package org.opencds.cqf.tooling.library.stu3;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.base.Strings;

import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_30_50;
import org.hl7.fhir.convertors.conv30_50.VersionConvertor_30_50;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.Resource;
import org.opencds.cqf.tooling.common.ThreadUtils;
import org.opencds.cqf.tooling.common.stu3.CqfmSoftwareSystemHelper;
import org.opencds.cqf.tooling.library.LibraryProcessor;
import org.opencds.cqf.tooling.parameter.RefreshLibraryParameters;
import org.opencds.cqf.tooling.utilities.IOUtils;

import ca.uhn.fhir.context.FhirContext;

public class STU3LibraryProcessor extends LibraryProcessor {
    private FhirContext fhirContext;
    private static CqfmSoftwareSystemHelper cqfmHelper;

    /*
    Refresh all library resources in the given libraryPath
     */
    protected CopyOnWriteArrayList<String> refreshLibraries(String libraryPath, Boolean shouldApplySoftwareSystemStamp) {
        return refreshLibraries(libraryPath, null, shouldApplySoftwareSystemStamp);
    }
    
    /*
    Refresh all library resources in the given libraryPath and write to the given outputDirectory
     */
    protected CopyOnWriteArrayList<String> refreshLibraries(String libraryPath, String libraryOutputDirectory, Boolean shouldApplySoftwareSystemStamp) {
        File file = new File(libraryPath);
        ConcurrentHashMap<String, String> fileMap = new ConcurrentHashMap<String, String>();
        CopyOnWriteArrayList<org.hl7.fhir.r5.model.Library> libraries = new CopyOnWriteArrayList<>();

        if (file.isDirectory()) {
            ArrayList<Callable<Void>> tasks = new ArrayList<>();
            File[] fileList = file.listFiles();
            if (fileList != null && fileList.length > 0) {
                for (File libraryFile : fileList) {
                    tasks.add(() -> {
                        if (IOUtils.isXMLOrJson(libraryPath, libraryFile.getName())) {
                            loadLibrary(fileMap, libraries, libraryFile);
                        }
                        //task requires return statement
                        return null;
                    });
                }

                ThreadUtils.executeTasks(tasks);
            }
        }else {
            loadLibrary(fileMap, libraries, file);
        }


        CopyOnWriteArrayList<String> refreshedLibraryNames = new CopyOnWriteArrayList<String>();
        CopyOnWriteArrayList<org.hl7.fhir.r5.model.Library> refreshedLibraries = super.refreshGeneratedContent(libraries);
        VersionConvertor_30_50 versionConvertor_30_50 = new VersionConvertor_30_50(new BaseAdvisor_30_50());
        for (org.hl7.fhir.r5.model.Library refreshedLibrary : refreshedLibraries) {
            String filePath = fileMap.get(refreshedLibrary.getId());
            if(null != filePath) {
                org.hl7.fhir.dstu3.model.Library library = (org.hl7.fhir.dstu3.model.Library) versionConvertor_30_50.convertResource(refreshedLibrary);

                cleanseRelatedArtifactReferences(library);

                if (shouldApplySoftwareSystemStamp) {
                    cqfmHelper.ensureCQFToolingExtensionAndDevice(library, fhirContext);
                }

                String outputPath = filePath;
                if (libraryOutputDirectory != null) {
                    File libraryDirectory = new File(libraryOutputDirectory);
                    if (!libraryDirectory.exists()) {
                        //TODO: add logger and log non existant directory for writing
                    } else {
                        outputPath = libraryDirectory.getAbsolutePath();
                    }
                }
                IOUtils.writeResource(library, outputPath, IOUtils.getEncoding(outputPath), fhirContext);
                IOUtils.updateCachedResource(library, outputPath);

                String refreshedLibraryName;
                if (this.versioned && refreshedLibrary.getVersion() != null) {
                    refreshedLibraryName = refreshedLibrary.getName() + "-" + refreshedLibrary.getVersion();
                } else {
                    refreshedLibraryName = refreshedLibrary.getName();
                }
                refreshedLibraryNames.add(refreshedLibraryName);
            }
        }

        return refreshedLibraryNames;
    }

    private void cleanseRelatedArtifactReferences(Library library) {
        CopyOnWriteArrayList<String> unresolvableCodeSystems = new CopyOnWriteArrayList<>(Arrays.asList("http://loinc.org", "http://snomed.info/sct"));
        CopyOnWriteArrayList<RelatedArtifact> relatedArtifacts = new CopyOnWriteArrayList<>(library.getRelatedArtifact());
        relatedArtifacts.removeIf(ra -> ra.hasResource() && ra.getResource().hasReference() && unresolvableCodeSystems.contains(ra.getResource().getReference()));

        for (RelatedArtifact relatedArtifact : relatedArtifacts) {
            if ((relatedArtifact.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON) && relatedArtifact.hasResource()) {
                String resourceReference = relatedArtifact.getResource().getReference();
                resourceReference = resourceReference.replace("_", "-");
                if (resourceReference.contains("Library/")) {
                    resourceReference = resourceReference.substring(resourceReference.lastIndexOf("Library/"));
                }
      
                if (resourceReference.contains("|")) {
                    if (this.versioned) {
                        String curatedResourceReference = resourceReference.replace("|", "-");
                        relatedArtifact.getResource().setReference(curatedResourceReference);
                    }
                    else {
                        String curatedResourceReference = resourceReference.substring(0, resourceReference.indexOf("|"));
                        relatedArtifact.getResource().setReference(curatedResourceReference);
                    }

                }
            }
        }
    }

    private void loadLibrary(ConcurrentHashMap<String, String> fileMap, CopyOnWriteArrayList<org.hl7.fhir.r5.model.Library> libraries, File libraryFile) {
        try {
            Resource resource = (Resource) IOUtils.readResource(libraryFile.getAbsolutePath(), fhirContext);
            VersionConvertor_30_50 versionConvertor_30_50 = new VersionConvertor_30_50(new BaseAdvisor_30_50());
            org.hl7.fhir.r5.model.Library library = (org.hl7.fhir.r5.model.Library) versionConvertor_30_50.convertResource(resource);
            fileMap.put(library.getId(), libraryFile.getAbsolutePath());
            libraries.add(library);
        } catch (Exception ex) {
            logMessage(String.format("Error reading library: %s. Error: %s", libraryFile.getAbsolutePath(), ex.getMessage()));
        }
    }

    @Override
    public CopyOnWriteArrayList<String> refreshLibraryContent(RefreshLibraryParameters params) {
        if (params.parentContext != null) {
            initialize(params.parentContext);
        }
        else {
            initializeFromIni(params.ini);
        }

        String libraryPath = params.libraryPath;
        fhirContext = params.fhirContext;
        versioned = params.versioned;

        STU3LibraryProcessor.cqfmHelper = new CqfmSoftwareSystemHelper(rootDir);

        if (!Strings.isNullOrEmpty(params.libraryOutputDirectory)) {
            return refreshLibraries(libraryPath, params.libraryOutputDirectory, params.shouldApplySoftwareSystemStamp);
        } else {
            return refreshLibraries(libraryPath, params.shouldApplySoftwareSystemStamp);
        }
    }
}
