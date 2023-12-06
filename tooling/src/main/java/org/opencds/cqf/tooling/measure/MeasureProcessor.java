package org.opencds.cqf.tooling.measure;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.FilenameUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r5.model.Measure;
import org.opencds.cqf.tooling.common.ThreadUtils;
import org.opencds.cqf.tooling.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.tooling.measure.stu3.STU3MeasureProcessor;
import org.opencds.cqf.tooling.parameter.RefreshMeasureParameters;
import org.opencds.cqf.tooling.processor.BaseProcessor;
import org.opencds.cqf.tooling.processor.IGProcessor;
import org.opencds.cqf.tooling.utilities.*;
import org.opencds.cqf.tooling.utilities.IOUtils.Encoding;

import java.util.ArrayList;
import java.util.List;

public class MeasureProcessor extends BaseProcessor {
    public static volatile String ResourcePrefix = "measure-";
    protected volatile List<Object> identifiers;

    public static String getId(String baseId) {
        return ResourcePrefix + baseId;
    }

    public List<String> refreshIgMeasureContent(BaseProcessor parentContext, Encoding outputEncoding, Boolean versioned, FhirContext fhirContext,
                                                String measureToRefreshPath, Boolean shouldApplySoftwareSystemStamp) {

        return refreshIgMeasureContent(parentContext, outputEncoding, null, versioned, fhirContext, measureToRefreshPath,
                shouldApplySoftwareSystemStamp);
    }

    public List<String> refreshIgMeasureContent(BaseProcessor parentContext, Encoding outputEncoding, String measureOutputDirectory,
                                                Boolean versioned, FhirContext fhirContext, String measureToRefreshPath,
                                                Boolean shouldApplySoftwareSystemStamp) {

        System.out.println("\r\n[Refreshing Measures]\r\n");

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

        String measurePath = FilenameUtils.concat(parentContext.getRootDir(), IGProcessor.MEASURE_PATH_ELEMENT);
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

    protected boolean versioned;
    protected FhirContext fhirContext;

    public List<String> refreshMeasureContent(RefreshMeasureParameters params) {
        return new ArrayList<>();
    }

    protected List<Measure> refreshGeneratedContent(List<Measure> sourceMeasures) {
        return internalRefreshGeneratedContent(sourceMeasures);
    }

    private List<Measure> internalRefreshGeneratedContent(List<Measure> sourceMeasures) {
        // for each Measure, refresh the measure based on the primary measure library
        List<Measure> resources = new ArrayList<>();

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
            CompiledLibrary CompiledLibrary = libraryManager.resolveLibrary(primaryLibraryIdentifier, errors);
            boolean hasErrors = false;
            if (!errors.isEmpty()) {
                StringBuilder errorMessage = new StringBuilder();
                for (CqlCompilerException e : errors) {
                    errorMessage.append("\n\t").append(e.getMessage());
                    if (e.getSeverity() == CqlCompilerException.ErrorSeverity.Error) {
                        hasErrors = true;
                    }
                }
                System.out.printf("[FAIL] CQL Processing of %s failed with %d Error(s): %s%n",
                        measure.getName(), errors.size(),
                        (includeErrors ?
                                errorMessage.toString()
                                : "")
                );
            }
            if (!hasErrors) {
                System.out.printf("[SUCCESS] CQL Processing of %s completed successfully.%n",
                        measure.getName());
                return processor.refreshMeasure(measure, libraryManager, CompiledLibrary, cqlTranslatorOptions.getCqlCompilerOptions());
            }
        }
        return measure;
    }
}
