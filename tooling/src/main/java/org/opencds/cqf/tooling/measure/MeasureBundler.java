package org.opencds.cqf.tooling.measure;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.tooling.processor.AbstractBundler;
import org.opencds.cqf.tooling.utilities.BundleUtils;
import org.opencds.cqf.tooling.utilities.IOUtils;
import org.opencds.cqf.tooling.utilities.IOUtils.Encoding;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class MeasureBundler extends AbstractBundler {
    public static final String ResourcePrefix = "measure-";
    protected CopyOnWriteArrayList<Object> identifiers;

    public static String getId(String baseId) {
        return ResourcePrefix + baseId;
    }

    //abstract methods to override:
    @Override
    protected void persistTestFiles(String bundleDestPath, String libraryName, Encoding encoding, FhirContext fhirContext, String fhirUri) {

        String filesLoc = bundleDestPath + File.separator + libraryName + "-files";
        File directory = new File(filesLoc);
        if (directory.exists()) {

            File[] filesInDir = directory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().startsWith("tests-");
                }
            });

            if (!(filesInDir == null || filesInDir.length == 0)) {
                for (File file : filesInDir) {
                    if (file.getName().toLowerCase().startsWith("tests-")) {
                        try {
                            IBaseResource resource = IOUtils.readResource(file.getAbsolutePath(), fhirContext, true);
                            //ensure the resource can be posted
                            if (BundleUtils.resourceIsTransactionBundle(resource)) {
                                BundleUtils.postBundle(encoding, fhirContext, fhirUri, resource);
                            }
                        } catch (Exception e) {
                            //resource is likely not IBaseResource
                            logger.error("persistTestFiles", e);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected String getSourcePath(FhirContext fhirContext, Map.Entry<String, IBaseResource> resourceEntry) {
        return IOUtils.getMeasurePathMap(fhirContext).get(resourceEntry.getKey());
    }

    @Override
    protected Map<String, IBaseResource> getResources(FhirContext fhirContext) {
        return IOUtils.getMeasures(fhirContext);
    }

    @Override
    protected String getResourceProcessorType() {
        return TYPE_MEASURE;
    }

    @Override
    protected Set<String> getPaths(FhirContext fhirContext) {
        return IOUtils.getMeasurePaths(fhirContext);
    }
}
