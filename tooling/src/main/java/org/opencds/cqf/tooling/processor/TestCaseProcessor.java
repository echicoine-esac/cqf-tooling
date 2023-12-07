package org.opencds.cqf.tooling.processor;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.tooling.common.ThreadUtils;
import org.opencds.cqf.tooling.utilities.BundleUtils;
import org.opencds.cqf.tooling.utilities.IOUtils;
import org.opencds.cqf.tooling.utilities.LogUtils;
import org.opencds.cqf.tooling.utilities.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IFhirVersion;

public class TestCaseProcessor
{
    private static final Logger logger = LoggerFactory.getLogger(TestCaseProcessor.class);

    public void refreshTestCases(String path, IOUtils.Encoding encoding, FhirContext fhirContext, Boolean includeErrors) {
        refreshTestCases(path, encoding, fhirContext, null, includeErrors);
    }

    public void refreshTestCases(String path, IOUtils.Encoding encoding, FhirContext fhirContext, @Nullable List<String> refreshedResourcesNames,
                                 Boolean includeErrors)
    {
        System.out.println("\r\n[Refreshing Tests]\r\n");

        List<String> resourceTypeTestGroups = IOUtils.getDirectoryPaths(path, false);
        IFhirVersion version = fhirContext.getVersion();

        //build list of tasks via for loop:
        List<Callable<Void>> resourceTypeTestGroupsTasks = new ArrayList<>();

        for (String group : resourceTypeTestGroups) {
            resourceTypeTestGroupsTasks.add(() -> {

                List<String> testArtifactPaths = IOUtils.getDirectoryPaths(group, false);

                //build list of tasks via for loop:
                List<Callable<Void>> testArtifactPathsTasks = new CopyOnWriteArrayList<>();

                for (String testArtifactPath : testArtifactPaths) {
                    testArtifactPathsTasks.add(() -> {

                        List<String> testCasePaths = IOUtils.getDirectoryPaths(testArtifactPath, false);

                        org.hl7.fhir.r4.model.Group testGroup;

                        if (version.getVersion() == FhirVersionEnum.R4) {
                            testGroup = new org.hl7.fhir.r4.model.Group();
                            testGroup.setActive(true);
                            testGroup.setType(Group.GroupType.PERSON);
                            testGroup.setActual(true);
                        } else {
                            testGroup = null;
                        }

                        // For each test case we need to create a group
                        if (!testCasePaths.isEmpty()) {
                            String measureName = IOUtils.getMeasureTestDirectory(testCasePaths.get(0));
                            if (testGroup != null) {
                                testGroup.setId(measureName);

                                testGroup.addExtension("http://hl7.org/fhir/StructureDefinition/artifact-testArtifact",
                                        new CanonicalType("http://ecqi.healthit.gov/ecqms/Measure/" + measureName));
                            }

                            List<Callable<Void>> testCasePathsTasks = new CopyOnWriteArrayList<>();

                            for (String testCasePath : testCasePaths) {
                                testCasePathsTasks.add(() -> {

                                    try {
                                        List<String> paths = IOUtils.getFilePaths(testCasePath, true);
                                        List<IBaseResource> resources = IOUtils.readResources(paths, fhirContext);
                                        ensureIds(testCasePath, resources);

                                        // Loop through resources and any that are patients need to be added to the test Group
                                        // Handle individual resources when they exist
                                        for (IBaseResource resource : resources) {
                                            if ((resource.fhirType() == "Patient") && (version.getVersion() == FhirVersionEnum.R4)) {
                                                org.hl7.fhir.r4.model.Patient patient = (org.hl7.fhir.r4.model.Patient) resource;
                                                if (testGroup != null) {
                                                    addPatientToGroupR4(testGroup, patient);
                                                }
                                            }

                                            // Handle bundled resources when that is how they are provided
                                            if ((resource.fhirType() == "Bundle") && (version.getVersion() == FhirVersionEnum.R4)) {
                                                org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) resource;
                                                var bundleResources =
                                                        BundleUtils.getR4ResourcesFromBundle(bundle);
                                                for (IBaseResource bundleResource : bundleResources) {
                                                    if (bundleResource.fhirType() == "Patient") {
                                                        org.hl7.fhir.r4.model.Patient patient = (org.hl7.fhir.r4.model.Patient) bundleResource;
                                                        if (testGroup != null) {
                                                            addPatientToGroupR4(testGroup, patient);
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // If the resource is a transaction bundle then don't bundle it again otherwise do
                                        String fileId = getId(FilenameUtils.getName(testCasePath));
                                        Object bundle;
                                        if ((resources.size() == 1) && (BundleUtils.resourceIsABundle(resources.get(0)))) {
                                            bundle = processTestBundle(fileId, resources.get(0), fhirContext);
                                        } else {
                                            bundle = BundleUtils.bundleArtifacts(fileId, resources, fhirContext, false);
                                        }
                                        IOUtils.writeBundle(bundle, testArtifactPath, encoding, fhirContext);

                                    } catch (Exception e) {
                                        //clean reporting of errors with file name:
                                        System.out.printf("Test Case refresh failed for %s: %s%n",
                                                testCasePath,
                                                (includeErrors ?
                                                        e.getMessage()
                                                        : "")
                                        );
                                    } finally {
                                        LogUtils.warn(testCasePath);
                                    }

                                    //clean reporting of status with file name:
                                    System.out.printf("Test Case refreshed: %s%n",
                                            testCasePath
                                    );

                                    //task requires return statement
                                    return null;
                                });
                            }//end for (String testCasePath : testCasePaths) {
                            ThreadUtils.executeTasks(testCasePathsTasks);

                            // Need to output the Group if it exists
                            if (testGroup != null) {
                                String groupFileName = "Group-" + measureName;
                                IOUtils.writeResource(testGroup, testArtifactPath, encoding, fhirContext, true,
                                        groupFileName);
                                System.out.printf("Group file created: %s%n",
                                        groupFileName
                                );
                            }
                        }
                        //task requires return statement
                        return null;
                    });
                }//
                ThreadUtils.executeTasks(testArtifactPathsTasks);

                //task requires return statement
                return null;
            });
        }//end for (String group : resourceTypeTestGroups) {
        ThreadUtils.executeTasks(resourceTypeTestGroupsTasks);
    }

    public static Object processTestBundle(String id, IBaseResource resource, FhirContext fhirContext) {
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                org.hl7.fhir.dstu3.model.Bundle dstu3Bundle = (org.hl7.fhir.dstu3.model.Bundle) resource;
                ResourceUtils.setIgId(id, dstu3Bundle, false);
                dstu3Bundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.TRANSACTION);

                for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent entry : dstu3Bundle.getEntry()) {
                    org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent request = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent();
                    request.setMethod(org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.PUT);
                    request.setUrl(entry.getResource().fhirType() + "/" + entry.getResource().getIdElement().getIdPart());
                    entry.setRequest(request);
                }

                return dstu3Bundle;

            case R4:
                org.hl7.fhir.r4.model.Bundle r4Bundle = (org.hl7.fhir.r4.model.Bundle)resource;
                ResourceUtils.setIgId(id, r4Bundle, false);
                r4Bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION);
                for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry : r4Bundle.getEntry()) {
                    org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent request = new org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent();
                    request.setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.PUT); // Adjust the HTTP method as needed
                    request.setUrl(entry.getResource().fhirType() + "/" + entry.getResource().getIdElement().getIdPart());
                    entry.setRequest(request);
                }

                return r4Bundle;
            default:
                throw new IllegalArgumentException("Unknown fhir version: " + fhirContext.getVersion().getVersion().getFhirVersionString());
        }
    }


    private void addPatientToGroupR4(Group group, org.hl7.fhir.r4.model.Patient patient) {
        IdType idType = patient.getIdElement();
        org.hl7.fhir.r4.model.Group.GroupMemberComponent member = group.addMember();
        org.hl7.fhir.r4.model.Reference patientRef = new Reference();
        patientRef.setReference("Patient/" + idType.getIdPart());

        // Get name for display value
        org.hl7.fhir.r4.model.HumanName name = patient.getName().get(0);
        patientRef.setDisplay(name.getNameAsSingleString());

        member.setEntity(patientRef);
    }

    public static List<IBaseResource> getTestCaseResources(String path, FhirContext fhirContext) {
        List<IBaseResource> resources = new ArrayList<IBaseResource>();
        List<String> testCasePaths = IOUtils.getDirectoryPaths(path, false);
        for (String testCasePath : testCasePaths) {
            List<String> paths = IOUtils.getFilePaths(testCasePath, true);
            resources.addAll(ensureIds(testCasePath, IOUtils.readResources(paths, fhirContext)));
        }
        return resources;
    }

    private static List<IBaseResource> ensureIds(String baseId, List<IBaseResource> resources) {
        for (IBaseResource resource : resources) {
            if (resource.getIdElement().getIdPart() == null || resource.getIdElement().getIdPart().equals("")) {
                ResourceUtils.setIgId(FilenameUtils.getName(baseId), resource, false);
                resource.setId(resource.getClass().getSimpleName() + "/" + resource.getIdElement().getIdPart());
            }
        }
        return resources;
    }

    public static String getId(String baseId) {
        return "tests-" + baseId;
    }

    public static Boolean bundleTestCases(String igPath, String contextResourceType, String libraryName, FhirContext fhirContext,
                                          Map<String, IBaseResource> resources) {
        Boolean shouldPersist = true;
        String igTestCasePath = FilenameUtils.concat(FilenameUtils.concat(FilenameUtils.concat(igPath, IGProcessor.TEST_CASE_PATH_ELEMENT), contextResourceType), libraryName);

        // this is breaking for bundle of a bundle. Replace with individual resources
        // until we can figure it out.
        // List<String> testCaseSourcePaths = IOUtils.getFilePaths(igTestCasePath,
        // false);
        // for (String testCaseSourcePath : testCaseSourcePaths) {
        // shouldPersist = shouldPersist & safeAddResource(testCaseSourcePath,
        // resources, fhirContext);
        // }

        try {
            List<IBaseResource> testCaseResources = TestCaseProcessor.getTestCaseResources(igTestCasePath, fhirContext);
            for (IBaseResource resource : testCaseResources) {
                if ((!(resource instanceof org.hl7.fhir.dstu3.model.Bundle)) && (!(resource instanceof org.hl7.fhir.r4.model.Bundle))) {
                    resources.putIfAbsent(resource.getIdElement().getIdPart(), resource);
                }
            }
        } catch (Exception e) {
            shouldPersist = false;
            logger.error(igTestCasePath, e);
        }
        return shouldPersist;
    }


    static Set<String> copiedFilePaths = new HashSet<>();

    //TODO: the bundle needs to have -expectedresults added too
    /**
     * Bundles test case files from the specified path into a destination path.
     * The method copies relevant test case files, including expected results for MeasureReports,
     * and returns a summary message with the number of files copied.
     *
     * @param igPath             The path to the Implementation Guide (IG) containing test case files.
     * @param contextResourceType The resource type associated with the test cases.
     * @param libraryName        The name of the library associated with the test cases.
     * @param destPath           The destination path for the bundled test case files.
     * @param fhirContext        The FHIR context used for reading and processing resources.
     * @return A summary message indicating the number of files copied for the specified test case path.
     */
    public static String bundleTestCaseFiles(String igPath, String contextResourceType, String libraryName, String destPath, FhirContext fhirContext) {
        String igTestCasePath = FilenameUtils.concat(FilenameUtils.concat(FilenameUtils.concat(igPath, IGProcessor.TEST_CASE_PATH_ELEMENT), contextResourceType), libraryName);
        List<String> testCasePaths = IOUtils.getFilePaths(igTestCasePath, false);
        Set<String> measureReportPaths = IOUtils.getMeasureReportPaths(fhirContext);
        List<String> testCaseDirectories = IOUtils.getDirectoryPaths(igTestCasePath, false);

        int tracker = 0;
        for (String testPath : testCasePaths) {
            String bundleTestDestPath = FilenameUtils.concat(destPath, FilenameUtils.getName(testPath));
            if (IOUtils.copyFile(testPath, bundleTestDestPath)) {
                tracker++;
            }

            for (String testCaseDirectory : testCaseDirectories) {
                List<String> testContentPaths = IOUtils.getFilePaths(testCaseDirectory, false);
                for (String testContentPath : testContentPaths) {
                    // Copy the file if it hasn't been copied before (Set.add returns false if the Set already contains this entry)
                    if (copiedFilePaths.add(testContentPath)) {

                        Optional<String> matchingMeasureReportPath = measureReportPaths.stream()
                                .filter(path -> path.equals(testContentPath))
                                .findFirst();
                        if (matchingMeasureReportPath.isPresent()) {
                            IBaseResource measureReport = IOUtils.readResource(testContentPath, fhirContext);
                            if (!measureReport.getIdElement().getIdPart().startsWith("measurereport") || !measureReport.getIdElement().getIdPart().endsWith("-expectedresults")) {
                                Object measureReportStatus = ResourceUtils.resolveProperty(measureReport, "status", fhirContext);
                                String measureReportStatusValue = ResourceUtils.resolveProperty(measureReportStatus, "value", fhirContext).toString();
                                if (measureReportStatusValue.equals("COMPLETE")) {
                                    String expectedResultsId = FilenameUtils.getBaseName(testContentPath) + (FilenameUtils.getBaseName(testContentPath).endsWith("-expectedresults") ? "" : "-expectedresults");
                                    measureReport.setId(expectedResultsId);
                                }
                            }
                            IOUtils.writeResource(measureReport, destPath, IOUtils.Encoding.JSON, fhirContext);
                        } else {
                            String bundleTestContentDestPath = FilenameUtils.concat(destPath, FilenameUtils.getName(testContentPath));
                            if (IOUtils.copyFile(testContentPath, bundleTestContentDestPath)) {
                                tracker++;
                            }
                        }
                    }
                }
            }
        }
        return "\nBundle Test Case Files: " + tracker + " files copied for " + igTestCasePath;
    }

    public static void cleanUp() {
        copiedFilePaths = new HashSet<>();
    }
}