package org.opencds.cqf.tooling.measure;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.FilenameUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Measure;
import org.opencds.cqf.tooling.library.LibraryProcessor;
import org.opencds.cqf.tooling.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.tooling.measure.stu3.STU3MeasureProcessor;
import org.opencds.cqf.tooling.parameter.RefreshMeasureParameters;
import org.opencds.cqf.tooling.processor.*;
import org.opencds.cqf.tooling.utilities.*;
import org.opencds.cqf.tooling.utilities.IOUtils.Encoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MeasureProcessor extends BaseProcessor {
    public static final String ResourcePrefix = "measure-";
    public static final String MeasureTestGroupName = "measure";
    protected List<Object> identifiers;

    public static String getId(String baseId) {
        return ResourcePrefix + baseId;
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<String> refreshIgMeasureContent(BaseProcessor parentContext, Encoding outputEncoding, Boolean versioned, FhirContext fhirContext, String measureToRefreshPath, Boolean shouldApplySoftwareSystemStamp) {
        return refreshIgMeasureContent(parentContext, outputEncoding, null, versioned, fhirContext, measureToRefreshPath, shouldApplySoftwareSystemStamp);
    }

    public List<String> refreshIgMeasureContent(BaseProcessor parentContext, Encoding outputEncoding, String measureOutputDirectory, Boolean versioned, FhirContext fhirContext, String measureToRefreshPath, Boolean shouldApplySoftwareSystemStamp) {

        System.out.println("Refreshing measures...");

        MeasureProcessor measureProcessor;
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                measureProcessor = new STU3MeasureProcessor();
                break;
            case R4:
                measureProcessor = new R4MeasureProcessor();
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown fhir version: " + fhirContext.getVersion().getVersion().getFhirVersionString());
        }

        String measurePath = FilenameUtils.concat(parentContext.getRootDir(), IGProcessor.measurePathElement);
        RefreshMeasureParameters params = new RefreshMeasureParameters();
        params.measurePath = measurePath;
        params.parentContext = parentContext;
        params.fhirContext = fhirContext;
        params.encoding = outputEncoding;
        params.versioned = versioned;
        params.measureOutputDirectory = measureOutputDirectory;
        List<String> contentList = measureProcessor.refreshMeasureContent(params);

        if (!measureProcessor.getIdentifiers().isEmpty()) {
            this.getIdentifiers().addAll(measureProcessor.getIdentifiers());
        }
        return contentList;
    }

    protected List<Object> getIdentifiers() {
        if (identifiers == null) {
            identifiers = new ArrayList<>();
        }
        return identifiers;
    }

    public void bundleMeasures(ArrayList<String> refreshedLibraryNames, String igPath, List<String> binaryPaths, Boolean includeDependencies,
                               Boolean includeTerminology, Boolean includePatientScenarios, Boolean includeVersion, Boolean addBundleTimestamp, FhirContext fhirContext, String fhirUri,
                               Encoding encoding) {
        Map<String, IBaseResource> measures = IOUtils.getMeasures(fhirContext);
        //Map<String, IBaseResource> libraries = IOUtils.getLibraries(fhirContext);
        LogUtils.info("bundleMeasures: Process starting");
        List<String> bundledMeasures = new ArrayList<String>();

        List<Future<?>> futures = new ArrayList<>();

        for (Map.Entry<String, IBaseResource> measureEntry : measures.entrySet()) {

            ExecutorService executor = Executors.newSingleThreadExecutor();

            Future<?> future = executor.submit(() -> {
                try {
                    LogUtils.info("bundleMeasures: 1");
                    String measureSourcePath = IOUtils.getMeasurePathMap(fhirContext).get(measureEntry.getKey());
                    // Assumption - File name matches measure.name
                    String measureName = FilenameUtils.getBaseName(measureSourcePath).replace(MeasureProcessor.ResourcePrefix, "");
                    try {
                        Map<String, IBaseResource> resources = new HashMap<String, IBaseResource>();
                        LogUtils.info("bundleMeasures: 2");
                        Boolean shouldPersist = ResourceUtils.safeAddResource(measureSourcePath, resources, fhirContext);
                        if (!resources.containsKey("Measure/" + measureEntry.getKey())) {
                            throw new IllegalArgumentException(String.format("Could not retrieve base resource for measure %s", measureName));
                        }
                        LogUtils.info("bundleMeasures: 3");
                        IBaseResource measure = resources.get("Measure/" + measureEntry.getKey());
                        String primaryLibraryUrl = ResourceUtils.getPrimaryLibraryUrl(measure, fhirContext);
                        IBaseResource primaryLibrary;
                        if (primaryLibraryUrl.startsWith("http")) {
                            primaryLibrary = IOUtils.getLibraryUrlMap(fhirContext).get(primaryLibraryUrl);
                        } else {
                            primaryLibrary = IOUtils.getLibraries(fhirContext).get(primaryLibraryUrl);
                        }
                        LogUtils.info("bundleMeasures: 4");
                        if (primaryLibrary == null)
                            throw new IllegalArgumentException(String.format("Could not resolve library url %s", primaryLibraryUrl));

                        String primaryLibrarySourcePath = IOUtils.getLibraryPathMap(fhirContext).get(primaryLibrary.getIdElement().getIdPart());
                        String primaryLibraryName = ResourceUtils.getName(primaryLibrary, fhirContext);
                        if (includeVersion) {
                            primaryLibraryName = primaryLibraryName + "-" +
                                    fhirContext.newFhirPath().evaluateFirst(primaryLibrary, "version", IBase.class).get().toString();
                        }
                        LogUtils.info("bundleMeasures: 5");
                        shouldPersist = shouldPersist
                                & ResourceUtils.safeAddResource(primaryLibrarySourcePath, resources, fhirContext);

                        String cqlFileName = IOUtils.formatFileName(primaryLibraryName, Encoding.CQL, fhirContext);

                        String cqlLibrarySourcePath = IOUtils.getCqlLibrarySourcePath(primaryLibraryName, cqlFileName, binaryPaths);
                        LogUtils.info("bundleMeasures: 6");
                        if (cqlLibrarySourcePath == null) {
                            throw new IllegalArgumentException(String.format("Could not determine CqlLibrarySource path for library %s", primaryLibraryName));
                        }
                        LogUtils.info("bundleMeasures: 7");
                        if (includeTerminology) {
                            boolean result = ValueSetsProcessor.bundleValueSets(cqlLibrarySourcePath, igPath, fhirContext, resources, encoding, includeDependencies, includeVersion);
                            if (shouldPersist && !result) {
                                LogUtils.info("Measure will not be bundled because ValueSet bundling failed.");
                            }
                            shouldPersist = shouldPersist & result;
                        }
                        LogUtils.info("bundleMeasures: 8");
                        if (includeDependencies) {
                            LibraryProcessor libraryProcessor = new LibraryProcessor();
                            boolean result = libraryProcessor.bundleLibraryDependencies(primaryLibrarySourcePath, fhirContext, resources, encoding, includeVersion);
                            if (shouldPersist && !result) {
                                LogUtils.info("Measure will not be bundled because Library Dependency bundling failed.");
                            }
                            shouldPersist = shouldPersist & result;
                        }
                        LogUtils.info("bundleMeasures: 9");
                        if (includePatientScenarios) {
                            boolean result = TestCaseProcessor.bundleTestCases(igPath, MeasureTestGroupName, primaryLibraryName, fhirContext, resources);
                            if (shouldPersist && !result) {
                                LogUtils.info("Measure will not be bundled because Test Case bundling failed.");
                            }
                            shouldPersist = shouldPersist & result;
                        }
                        LogUtils.info("bundleMeasures: 10");
                        if (shouldPersist) {
                            String bundleDestPath = FilenameUtils.concat(FilenameUtils.concat(IGProcessor.getBundlesPath(igPath), MeasureTestGroupName), measureName);

                            persistBundle(igPath, bundleDestPath, measureName, encoding, fhirContext, new ArrayList<IBaseResource>(resources.values()), fhirUri, addBundleTimestamp);
                            bundleFiles(igPath, bundleDestPath, measureName, binaryPaths, measureSourcePath, primaryLibrarySourcePath, fhirContext, encoding, includeTerminology, includeDependencies, includePatientScenarios, includeVersion, addBundleTimestamp);
                            bundledMeasures.add(measureSourcePath);
                        }
                        LogUtils.info("bundleMeasures: 11");
                    } catch (Exception e) {
                        LogUtils.putException(measureName, e);
                    } finally {
                        LogUtils.warn(measureName);
                    }


                    //end for

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            futures.add(future);


        }

        //wait for all tasks to complete:
        for (Future<?> future : futures) {
            try {
                future.get(); // This will block until the task is complete
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        String message = "\r\n" + bundledMeasures.size() + " Measures successfully bundled:";
        for (String bundledMeasure : bundledMeasures) {
            message += "\r\n     " + bundledMeasure + " BUNDLED";
        }

        List<String> measurePathLibraryNames = new ArrayList<>(IOUtils.getMeasurePaths(fhirContext));
        ArrayList<String> failedMeasures = new ArrayList<>(measurePathLibraryNames);
        measurePathLibraryNames.removeAll(bundledMeasures);
        measurePathLibraryNames.retainAll(refreshedLibraryNames);
        message += "\r\n" + measurePathLibraryNames.size() + " Measures refreshed, but not bundled (due to issues):";
        for (String notBundled : measurePathLibraryNames) {
            message += "\r\n     " + notBundled + " REFRESHED";
        }

        failedMeasures.removeAll(bundledMeasures);
        failedMeasures.removeAll(measurePathLibraryNames);
        message += "\r\n" + failedMeasures.size() + " Measures failed refresh:";
        for (String failed : failedMeasures) {
            message += "\r\n     " + failed + " FAILED";
        }

        LogUtils.info(message);
    }

    private void persistBundle(String igPath, String bundleDestPath, String libraryName, Encoding encoding, FhirContext fhirContext, List<IBaseResource> resources, String fhirUri, Boolean addBundleTimestamp) {
        //Check for test files in bundleDestPath + "-files", loop through if exists,
        // find all files that start with "tests-", post to fhir server following same folder structure:
        persistTestFiles(bundleDestPath, libraryName, encoding, fhirContext, fhirUri);

        IOUtils.initializeDirectory(bundleDestPath);
        Object bundle = BundleUtils.bundleArtifacts(libraryName, resources, fhirContext, addBundleTimestamp, this.getIdentifiers());
        IOUtils.writeBundle(bundle, bundleDestPath, encoding, fhirContext);

        BundleUtils.postBundle(encoding, fhirContext, fhirUri, (IBaseResource) bundle);
    }

    private void persistTestFiles(String bundleDestPath, String libraryName, Encoding encoding, FhirContext fhirContext, String fhirUri) {

        String filesLoc = bundleDestPath + File.separator + libraryName + "-files";
        File directory = new File(filesLoc);
        if (directory.exists()) {
            String filesPostLoc = fhirUri + "/" + libraryName + "-files";
            File[] filesInDir = directory.listFiles();
            if (!(filesInDir == null || filesInDir.length == 0)) {
                for (File file : filesInDir) {
                    if (file.getName().toLowerCase().startsWith("tests-")) {
                        IBaseResource resource = IOUtils.readResource(file.getAbsolutePath(), fhirContext, true);
                        BundleUtils.postBundle(encoding, fhirContext, fhirUri, resource);
                    }
                }
            }

        }
    }

    private void bundleFiles(String igPath, String bundleDestPath, String libraryName, List<String> binaryPaths, String resourceFocusSourcePath,
                             String librarySourcePath, FhirContext fhirContext, Encoding encoding, Boolean includeTerminology, Boolean includeDependencies, Boolean includePatientScenarios,
                             Boolean includeVersion, Boolean addBundleTimestamp) {
        String bundleDestFilesPath = FilenameUtils.concat(bundleDestPath, libraryName + "-" + IGBundleProcessor.bundleFilesPathElement);
        IOUtils.initializeDirectory(bundleDestFilesPath);

        IOUtils.copyFile(resourceFocusSourcePath, FilenameUtils.concat(bundleDestFilesPath, FilenameUtils.getName(resourceFocusSourcePath)));
        IOUtils.copyFile(librarySourcePath, FilenameUtils.concat(bundleDestFilesPath, FilenameUtils.getName(librarySourcePath)));

        String cqlFileName = IOUtils.formatFileName(FilenameUtils.getBaseName(librarySourcePath), Encoding.CQL, fhirContext);
        if (cqlFileName.toLowerCase().startsWith("library-")) {
            cqlFileName = cqlFileName.substring(8);
        }
        String cqlLibrarySourcePath = IOUtils.getCqlLibrarySourcePath(libraryName, cqlFileName, binaryPaths);
        String cqlDestPath = FilenameUtils.concat(bundleDestFilesPath, cqlFileName);
        IOUtils.copyFile(cqlLibrarySourcePath, cqlDestPath);

        if (includeTerminology) {
            try {
                Map<String, IBaseResource> valuesets = ResourceUtils.getDepValueSetResources(cqlLibrarySourcePath, igPath, fhirContext, includeDependencies, includeVersion);
                if (!valuesets.isEmpty()) {
                    Object bundle = BundleUtils.bundleArtifacts(ValueSetsProcessor.getId(libraryName), new ArrayList<IBaseResource>(valuesets.values()), fhirContext, addBundleTimestamp, this.getIdentifiers());
                    IOUtils.writeBundle(bundle, bundleDestFilesPath, encoding, fhirContext);
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.putException(libraryName, e.getMessage());
            }
        }

        if (includeDependencies) {
            Map<String, IBaseResource> depLibraries = ResourceUtils.getDepLibraryResources(librarySourcePath, fhirContext, encoding, includeVersion, logger);
            if (!depLibraries.isEmpty()) {
                String depLibrariesID = "library-deps-" + libraryName;
                Object bundle = BundleUtils.bundleArtifacts(depLibrariesID, new ArrayList<IBaseResource>(depLibraries.values()), fhirContext, addBundleTimestamp, this.getIdentifiers());
                IOUtils.writeBundle(bundle, bundleDestFilesPath, encoding, fhirContext);
            }
        }

        if (includePatientScenarios) {
            TestCaseProcessor.bundleTestCaseFiles(igPath, "measure", libraryName, bundleDestFilesPath, fhirContext);
        }
    }

    protected boolean versioned;
    protected FhirContext fhirContext;

    public List<String> refreshMeasureContent(RefreshMeasureParameters params) {
        return new ArrayList<String>();
    }

    protected List<Measure> refreshGeneratedContent(List<Measure> sourceMeasures) {
        return internalRefreshGeneratedContent(sourceMeasures);
    }

    private List<Measure> internalRefreshGeneratedContent(List<Measure> sourceMeasures) {
        // for each Measure, refresh the measure based on the primary measure library
        List<Measure> resources = new ArrayList<Measure>();
        for (Measure measure : sourceMeasures) {
            resources.add(refreshGeneratedContent(measure));
        }
        return resources;
    }

    private Measure refreshGeneratedContent(Measure measure) {
        MeasureRefreshProcessor processor = new MeasureRefreshProcessor();
        LibraryManager libraryManager = getCqlProcessor().getLibraryManager();
        CqlTranslatorOptions cqlTranslatorOptions = getCqlProcessor().getCqlTranslatorOptions();
        // Do not attempt to refresh if the measure does not have a library
        if (measure.hasLibrary()) {
            String libraryUrl = ResourceUtils.getPrimaryLibraryUrl(measure, fhirContext);
            VersionedIdentifier primaryLibraryIdentifier = CanonicalUtils.toVersionedIdentifier(libraryUrl);
            List<CqlCompilerException> errors = new ArrayList<CqlCompilerException>();
            CompiledLibrary CompiledLibrary = libraryManager.resolveLibrary(primaryLibraryIdentifier, cqlTranslatorOptions, errors);
            boolean hasErrors = false;
            if (errors.size() > 0) {
                for (CqlCompilerException e : errors) {
                    if (e.getSeverity() == CqlCompilerException.ErrorSeverity.Error) {
                        hasErrors = true;
                    }
                    logMessage(e.getMessage());
                }
            }
            if (!hasErrors) {
                return processor.refreshMeasure(measure, libraryManager, CompiledLibrary, cqlTranslatorOptions);
            }
        }
        return measure;
    }
}
