package org.opencds.cqf.tooling.measure;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.tooling.processor.AbstractBundler;
import org.opencds.cqf.tooling.utilities.BundleUtils;
import org.opencds.cqf.tooling.utilities.IOUtils;
import org.opencds.cqf.tooling.utilities.IOUtils.Encoding;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MeasureBundler extends AbstractBundler {
    public static final String ResourcePrefix = "measure-";
    protected CopyOnWriteArrayList<Object> identifiers;

    public static String getId(String baseId) {
        return ResourcePrefix + baseId;
    }

    //abstract methods to override:
    @Override
    protected void persistOtherFiles(String bundleDestPath, String libraryName, Encoding encoding, FhirContext fhirContext, String fhirUri, String builtBundleDestPath) {
        // persistFilesWithFilter(bundleDestPath, libraryName, encoding, fhirContext, fhirUri, "tests-");
        // persistFilesWithFilter(bundleDestPath, libraryName, encoding, fhirContext, fhirUri, "group-");

        String filesLoc = bundleDestPath + File.separator + libraryName + "-files";

        findAndPrintDuplicateFiles(filesLoc);

        File directory = new File(filesLoc);



        if (directory.exists()) {

            //we want all files in -files directory that end in .xml, .json, or .cql
            File[] filesInDir = directory.listFiles((dir, name) ->
                    (name.toLowerCase().endsWith(".cql") ||
                            name.toLowerCase().endsWith(".json") ||
                            name.toLowerCase().endsWith(".xml"))
            );

            if (!(filesInDir == null || filesInDir.length == 0)) {
                for (File file : filesInDir) {
                    String failMsg = "Bundle Measures failed to persist resource " + file.getAbsolutePath();
                    try {
                        //ensure the resource can be posted
                        try {
                            IBaseResource resource = IOUtils.readResource(file.getAbsolutePath(), fhirContext, true);
                            if (resource != null) {
                                BundleUtils.postBundle(encoding, fhirContext, fhirUri, resource, file.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            System.out.println(failMsg);
                        }

                    } catch (Exception e) {
                        //resource is likely not IBaseResource
                        logger.error("persistTestFiles", e);
                    }
                }
            }
        }
    }
    public static void findAndPrintDuplicateFiles(String directoryPath) {
        try {
            Map<String, List<File>> fileHashes = new HashMap<>();

            Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String hash = calculateFileHash(file.toFile());
                            fileHashes.computeIfAbsent(hash, k -> new ArrayList<>()).add(file.toFile());
                        } catch (IOException | NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    });

            // Print duplicates
            fileHashes.forEach((hash, fileList) -> {
                if (fileList.size() > 1) {
                    System.out.println("Files with the same content (Hash: " + hash + "):");
                    fileList.forEach(System.out::println);
                    System.out.println();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String calculateFileHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");

        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                md5Digest.update(buffer, 0, read);
            }
        }

        byte[] hashBytes = md5Digest.digest();

        // Convert to hex
        StringBuilder hexHash = new StringBuilder();
        for (byte hashByte : hashBytes) {
            hexHash.append(String.format("%02x", hashByte));
        }

        return hexHash.toString();
    }
//    protected void persistFilesWithFilter(String bundleDestPath, String libraryName, Encoding encoding, FhirContext fhirContext, String fhirUri, String startsWithFilter) {
//
//        String filesLoc = bundleDestPath + File.separator + libraryName + "-files";
//        File directory = new File(filesLoc);
//        if (directory.exists()) {
//
//            File[] filesInDir = directory.listFiles((dir, name) -> name.toLowerCase().startsWith(startsWithFilter));
//
//            if (!(filesInDir == null || filesInDir.length == 0)) {
//                for (File file : filesInDir) {
//                    try {
//                        //ensure the resource can be posted
////                            if (BundleUtils.resourceIsTransactionBundle(resource)) {
//                        BundleUtils.postBundle(encoding, fhirContext, fhirUri, file.getAbsolutePath());
////                            }
//                    } catch (Exception e) {
//                        //resource is likely not IBaseResource
//                        logger.error("persistTestFiles", e);
//                    }
//                }
//            }
//        }
//    }

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
