package org.opencds.cqf.tooling.processor;

import org.cqframework.cql.cql2elm.*;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider;
import org.cqframework.cql.elm.requirements.fhir.DataRequirementsProcessor;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.fhir.ucum.UcumService;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.context.IWorkerContext.ILoggingService;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.ParameterDefinition;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.opencds.cqf.tooling.npm.ILibraryReader;
import org.opencds.cqf.tooling.npm.NpmLibrarySourceProvider;
import org.opencds.cqf.tooling.npm.NpmModelInfoProvider;
import org.opencds.cqf.tooling.utilities.ResourceUtils;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class CqlProcessor {

    /**
     * information about a cql file
     */
    public class CqlSourceFileInformation {
        private VersionedIdentifier identifier;
        private byte[] elm;
        private byte[] jsonElm;
        private List<ValidationMessage> errors = new ArrayList<>();
        private List<RelatedArtifact> relatedArtifacts = new ArrayList<>();
        private List<DataRequirement> dataRequirements = new ArrayList<>();
        private List<ParameterDefinition> parameters = new ArrayList<>();

        public VersionedIdentifier getIdentifier() {
            return identifier;
        }

        public void setIdentifier(VersionedIdentifier identifier) {
            this.identifier = identifier;
        }

        public byte[] getElm() {
            return elm;
        }

        public void setElm(byte[] elm) {
            this.elm = elm;
        }

        public byte[] getJsonElm() {
            return jsonElm;
        }

        public void setJsonElm(byte[] jsonElm) {
            this.jsonElm = jsonElm;
        }

        public List<ValidationMessage> getErrors() {
            return errors;
        }

        public List<RelatedArtifact> getRelatedArtifacts() {
            return relatedArtifacts;
        }

        public List<DataRequirement> getDataRequirements() {
            return dataRequirements;
        }

        public List<ParameterDefinition> getParameters() {
            return parameters;
        }
    }

    /**
     * all the NPM packages this IG depends on (including base).
     * This list is in a maintained order such that you can just
     * do for (NpmPackage p : packages) and that will resolve the
     * library in the right order
     */
    private final List<NpmPackage> packages;

    /**
     * All the file paths cql files might be found in (absolute local file paths)
     * <p>
     * will be at least one error
     */
    private final List<String> folders;

    /**
     * Version indepedent reader
     */
    private ILibraryReader reader;

    /**
     * use this to write to the standard IG log
     */
    private ILoggingService logger;

    /**
     * UcumService used by the translator to validate UCUM units
     */
    private UcumService ucumService;

    /**
     * Map of translated files by fully qualified file name.
     * ConcurrentHashMap for thread safe access
     * Populated during execute
     */
    private Map<String, CqlSourceFileInformation> fileMap;

    /**
     * The packageId for the implementation guide, used to construct a NamespaceInfo for the CQL translator
     * Libraries that don't specify a namespace will be built in this namespace
     * Libraries can specify a namespace, but must use this name to do it
     */
    @SuppressWarnings("unused")
    private String packageId;

    /**
     * The canonical base of the IG, used to construct a NamespaceInfo for the CQL translator
     * Libraries translated in this IG will have this namespaceUri as their system
     * Library resources published in this IG will then have URLs of [canonicalBase]/Library/[libraryName]
     */
    @SuppressWarnings("unused")
    private String canonicalBase;

    private Boolean includeErrors;

    private NamespaceInfo namespaceInfo;

    public CqlProcessor(List<NpmPackage> packages, List<String> folders, ILibraryReader reader, ILoggingService logger,
                        UcumService ucumService, String packageId, String canonicalBase, Boolean includeErrors) {
        super();
        this.packages = packages;
        this.folders = folders;
        this.reader = reader;
        this.logger = logger;
        this.ucumService = ucumService;
        this.packageId = packageId;
        this.canonicalBase = canonicalBase;
        if (packageId != null && !packageId.isEmpty() && canonicalBase != null && !canonicalBase.isEmpty()) {
            this.namespaceInfo = new NamespaceInfo(packageId, canonicalBase);
        }
        this.includeErrors = includeErrors;
    }

    /**
     * Do the compile. Do not return any exceptions related to content; only throw exceptions for infrastructural issues
     * <p>
     * note that it's not an error if there's no .cql files - this is called without checking for their existence
     * <p>
     * Any exception will stop the build cold.
     */
    public void execute() throws FHIRException {
        try {
            System.out.println("\r\n[Translating CQL source files]\r\n");
            fileMap = new HashMap<>();

            // foreach folder
            for (String folder : folders) {
                translateFolder(folder);
            }
        } catch (Exception E) {
            logger.logDebugMessage(ILoggingService.LogCategory.PROGRESS, String.format("Errors occurred attempting to translate CQL content: %s", E.getMessage()));
        }
    }

    /**
     * Return CqlSourceFileInformation for the given filename
     *
     * @param filename Fully qualified name of the source file
     * @return
     */
    public CqlSourceFileInformation getFileInformation(String filename) {
        if (fileMap == null) {
            throw new IllegalStateException("CQL File map is not available, execute has not been called");
        }

        if (!fileMap.containsKey(filename)) {
            for (Map.Entry<String, CqlSourceFileInformation> entry : fileMap.entrySet()) {
                if (filename.equalsIgnoreCase(entry.getKey())) {
                    logger.logDebugMessage(ILoggingService.LogCategory.PROGRESS, String.format("File with a similar name but different casing was found. File found: '%s'", entry.getKey()));
                }
            }
            return null;
        }

        return this.fileMap.remove(filename);
    }

    public Collection<CqlSourceFileInformation> getAllFileInformation() {
        if (fileMap == null) {
            throw new IllegalStateException("CQL File map is not available, execute has not been called");
        }

        return this.fileMap.values();
    }

    /**
     * Called at the end after all getFileInformation have been called
     * return any errors that didn't have any particular home, and also
     * errors for any files that were linked but haven't been accessed using
     * getFileInformation - these have been omitted from the IG, and that's
     * an error
     *
     * @return
     */
    public List<ValidationMessage> getGeneralErrors() {
        List<ValidationMessage> result = new ArrayList<>();

        if (fileMap != null) {
            for (Map.Entry<String, CqlSourceFileInformation> entry : fileMap.entrySet()) {
                result.add(new ValidationMessage(ValidationMessage.Source.Publisher, ValidationMessage.IssueType.PROCESSING, entry.getKey(), "CQL source was not associated with a library resource in the IG.", ValidationMessage.IssueSeverity.ERROR));
            }
        }

        return result;
    }

    private void checkCachedManager() {
        if (cachedOptions == null) {
            if (hasMultipleBinaryPaths) {
                throw new RuntimeException("CqlProcessor has been used with multiple Cql paths, ambiguous options and manager");
            } else {
                throw new RuntimeException("CqlProcessor has not been executed, no cached options or manager");
            }
        }
    }

    private boolean hasMultipleBinaryPaths = false;
    private CqlTranslatorOptions cachedOptions;

    public CqlTranslatorOptions getCqlTranslatorOptions() {
        checkCachedManager();
        return cachedOptions;
    }

    private LibraryManager cachedLibraryManager;

    public LibraryManager getLibraryManager() {
        checkCachedManager();
        return cachedLibraryManager;
    }

    private void translateFolder(String folder) {

        String separator = System.getProperty("file.separator");

        logger.logMessage(String.format("Translating CQL source in folder %s", folder));

        CqlTranslatorOptions options = ResourceUtils.getTranslatorOptions(folder);

        // Setup
        // Construct DefaultLibrarySourceProvider
        // Construct FhirLibrarySourceProvider
        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new LibraryManager(modelManager, options.getCqlCompilerOptions());
        if (options.getCqlCompilerOptions().getValidateUnits()) {
            libraryManager.setUcumService(ucumService);
        }
        if (packages != null) {
            modelManager.getModelInfoLoader().registerModelInfoProvider(new NpmModelInfoProvider(packages, reader, logger), true);
            libraryManager.getLibrarySourceLoader().registerProvider(new NpmLibrarySourceProvider(packages, reader, logger));
        }
        libraryManager.getLibrarySourceLoader().registerProvider(new DefaultLibrarySourceProvider(Paths.get(folder)));
        libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
        modelManager.getModelInfoLoader().registerModelInfoProvider(new DefaultModelInfoProvider(Paths.get(folder)));

        loadNamespaces(libraryManager);


        File directory = new File(folder);
        if (directory.isDirectory()) {
            FilenameFilter filter = (dir, name) -> name.endsWith(".cql");
            String[] cqlFiles = directory.list(filter);
            boolean hasCqlFiles = cqlFiles != null && cqlFiles.length > 0;
            if (hasCqlFiles) {

                for (String fileName : cqlFiles) {
                    final String fileLocation = directory.getAbsolutePath() + separator + fileName;
                    translateFile(libraryManager, fileLocation, options.getCqlCompilerOptions());
                }


                if (cachedOptions == null) {
                    if (!hasMultipleBinaryPaths) {
                        cachedOptions = options;
                        cachedLibraryManager = libraryManager;
                    }
                } else {
                    if (!hasMultipleBinaryPaths) {
                        hasMultipleBinaryPaths = true;
                        cachedOptions = null;
                        cachedLibraryManager = null;
                    }
                }
            } else {
                System.out.println("No .cql files in the directory.");
            }
        }

    }

    private void loadNamespaces(LibraryManager libraryManager) {
        if (namespaceInfo != null) {
            libraryManager.getNamespaceManager().addNamespace(namespaceInfo);
        }

        if (packages != null) {
            for (NpmPackage p : packages) {
                if (p.name() != null && !p.name().isEmpty() && p.canonical() != null && !p.canonical().isEmpty()) {
                    NamespaceInfo ni = new NamespaceInfo(p.name(), p.canonical());
                    libraryManager.getNamespaceManager().addNamespace(ni);
                }
            }
        }
    }

    public static ValidationMessage.IssueType severityToIssueType(CqlCompilerException.ErrorSeverity severity) {
        switch (severity) {
            case Info:
                return ValidationMessage.IssueType.INFORMATIONAL;
            case Warning:
            case Error:
                return ValidationMessage.IssueType.PROCESSING;
            default:
                return ValidationMessage.IssueType.UNKNOWN;
        }
    }

    public static ValidationMessage.IssueSeverity severityToIssueSeverity(CqlCompilerException.ErrorSeverity severity) {
        switch (severity) {
            case Info:
                return ValidationMessage.IssueSeverity.INFORMATION;
            case Warning:
                return ValidationMessage.IssueSeverity.WARNING;
            case Error:
                return ValidationMessage.IssueSeverity.ERROR;
            default:
                return ValidationMessage.IssueSeverity.NULL;
        }
    }

    public static ValidationMessage exceptionToValidationMessage(String fileLocation, CqlCompilerException exception) {
        TrackBack tb = exception.getLocator();
        if (tb != null) {
            return new ValidationMessage(ValidationMessage.Source.Publisher, severityToIssueType(exception.getSeverity()),
                    tb.getStartLine(), tb.getStartChar(), tb.getLibrary().getId(), exception.getMessage(),
                    severityToIssueSeverity(exception.getSeverity()));
        } else {
            return new ValidationMessage(ValidationMessage.Source.Publisher, severityToIssueType(exception.getSeverity()),
                    fileLocation, exception.getMessage(), severityToIssueSeverity(exception.getSeverity()));
        }
    }

    public static List<String> listTranslatorErrors(CqlTranslator translator) {
        List<String> errors = new ArrayList<>();

        for (CqlCompilerException error : translator.getErrors()) {
            errors.add((error.getLocator() == null) ? "[n/a]" : String.format("[%d:%d, %d:%d] ",
                    error.getLocator().getStartLine(),
                    error.getLocator().getStartChar(),
                    error.getLocator().getEndLine(),
                    error.getLocator().getEndChar())
                    + error.getMessage());
        }

        return errors;
    }

    private void translateFile(LibraryManager libraryManager, String fileLocation, CqlCompilerOptions options) {
        CqlSourceFileInformation result = new CqlSourceFileInformation();

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(fileLocation))) {
            // translate toXML

            CqlTranslator translator = CqlTranslator.fromStream(namespaceInfo, inputStream, libraryManager);

            // record errors and warnings
            for (CqlCompilerException exception : translator.getExceptions()) {
                result.getErrors().add(exceptionToValidationMessage(fileLocation, exception));
            }

            if (!translator.getErrors().isEmpty()) {
                result.getErrors().add(new ValidationMessage(ValidationMessage.Source.Publisher, IssueType.EXCEPTION, fileLocation,
                        String.format("CQL Processing failed with (%d) errors.", translator.getErrors().size()), IssueSeverity.ERROR));

                //clean reporting of errors with file name :
                System.out.printf("[FAIL] CQL Processing of %s failed with %d Error(s) %s%n",
                        fileLocation, translator.getErrors().size(),

                        (includeErrors ?
                                listTranslatorErrors(translator).stream()
                                        .map(error -> "\n\t" + error)
                                        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                                : "")
                );

            } else {
                try {
                    // convert to base64 bytes
                    // NOTE: Publication tooling requires XML content
                    result.setElm(translator.toXml().getBytes());
                    result.setIdentifier(translator.toELM().getIdentifier());
                    result.setJsonElm(translator.toJson().getBytes());

                    // Add the translated library to the library manager (NOTE: This should be a "cacheLibrary" call on the LibraryManager, available in 1.5.3+)
                    // Without this, the data requirements processor will try to load the current library, resulting in a re-translation
                    CompiledLibrary compiledLibrary = translator.getTranslatedLibrary();
                    libraryManager.getCompiledLibraries().put(compiledLibrary.getIdentifier(), compiledLibrary);

                    DataRequirementsProcessor drp = new DataRequirementsProcessor();
                    org.hl7.fhir.r5.model.Library requirementsLibrary =
                            drp.gatherDataRequirements(libraryManager, translator.getTranslatedLibrary(), options, null, false);

                    // TODO: Report context, requires 1.5 translator (ContextDef)
                    // NOTE: In STU3, only Patient context is supported

                    // TODO: Extract direct reference code data
                    //result.extension.addAll(requirementsLibrary.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-directReferenceCode"));

                    // Extract relatedArtifact data (models, libraries, code systems, and value sets)
                    result.relatedArtifacts.addAll(requirementsLibrary.getRelatedArtifact());

                    // Extract parameter data and validate result types are supported types
                    result.parameters.addAll(requirementsLibrary.getParameter());
                    for (ValidationMessage paramMessage : drp.getValidationMessages()) {
                        result.getErrors().add(new ValidationMessage(paramMessage.getSource(), paramMessage.getType(), fileLocation,
                                paramMessage.getMessage(), paramMessage.getLevel()));
                    }

                    // Extract dataRequirement data
                    result.dataRequirements.addAll(requirementsLibrary.getDataRequirement());

                    System.out.printf("[SUCCESS] CQL Processing of %s completed successfully.%n",
                            fileLocation);
                } catch (Exception ex) {
                    logger.logMessage(String.format("CQL Translation succeeded for file: '%s', but ELM generation failed with the following error: %s", fileLocation, ex.getMessage()));
                }
            }
        } catch (Exception e) {
            result.getErrors().add(new ValidationMessage(ValidationMessage.Source.Publisher, IssueType.EXCEPTION, fileLocation, "CQL Processing failed with exception: " + e.getMessage(), IssueSeverity.ERROR));
        }

        fileMap.put(fileLocation, result);
    }

    private FilenameFilter getCqlFilenameFilter() {
        return new FilenameFilter() {
            @Override
            public boolean accept(File path, String name) {
                return name.endsWith(".cql");
            }
        };
    }
}
