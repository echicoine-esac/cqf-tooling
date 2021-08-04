package org.opencds.cqf.tooling.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.hl7.fhir.Measure;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.Utilities;
import org.junit.Before;
import org.junit.Test;
import org.opencds.cqf.tooling.parameter.RefreshIGParameters;
import org.opencds.cqf.tooling.processor.IGBundleProcessor;
import org.opencds.cqf.tooling.processor.IGProcessor;
import org.opencds.cqf.tooling.processor.argument.RefreshIGArgumentProcessor;
import org.opencds.cqf.tooling.utilities.IOUtils;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import ca.uhn.fhir.context.FhirContext;

public class RefreshIGOperationTest {

	RefreshIGOperation refreshIGOp;

	private static final String BREAST_CANCER_SCREENING_FHIR = "BreastCancerScreeningFHIR";
	private final String BUNDLED_FILES_LOC = "src\\test\\java\\org\\opencds\\cqf\\tooling\\operation\\refreshIG_TestFiles\\bundles\\measure\\";
	private final String BCS_BUNDLED_FULL_LOC = System.getProperty("user.dir") + "\\" + BUNDLED_FILES_LOC
			+ BREAST_CANCER_SCREENING_FHIR + "\\" + BREAST_CANCER_SCREENING_FHIR + "-files";

	private final String ID = "gov.healthit.ecqi.ecqms";
	private final int CONTACT = 2147483647;

	private final String INI_LOC = "src/test/java/org/opencds/cqf/tooling/operation/refreshIG_TestFiles/ig.ini";
	private final String ARGS[] = { "-RefreshIG", "-ini=" + INI_LOC, "-t", "-d", "-p" };

	@Before
	public void setUp() throws Exception {
		refreshIGOp = new RefreshIGOperation();
	}

	@Test
	public void testBreastCancerBundledFiles() {
//		clear out existing bundled files:
		try {
			File existingBCSfiles = new File(BCS_BUNDLED_FULL_LOC);
			FileUtils.cleanDirectory(existingBCSfiles);
			FileUtils.forceDelete(existingBCSfiles);
		} catch (IOException e) {
			e.printStackTrace();
		}

		refreshIGOp.execute(ARGS);

		List<String> espectedBundledFileList = new ArrayList<>(
				Arrays.asList("BreastCancerScreeningFHIR.cql", 
						"BreastCancerScreeningFHIR.json",
						"library-deps-BreastCancerScreeningFHIR-bundle.json", 
						"tests-denom-EXM125-bundle.json",
						"tests-numer-EXM125-bundle.json", 
						"valuesets-BreastCancerScreeningFHIR-bundle.json"));

		File bcsBundledFiles = new File(BCS_BUNDLED_FULL_LOC);

		List<String> resultingBundledFileList = Arrays.asList(bcsBundledFiles.listFiles()).stream()
				.map(file -> file.getName()).collect(Collectors.toList());

		assertEquals(espectedBundledFileList, resultingBundledFileList);

		Map<String, Map<?, ?>> jsonFiles = new HashMap<>();
		for (File file : bcsBundledFiles.listFiles()) {

			if (!file.getName().toLowerCase().endsWith(".json")) {
				continue;
			}

			try {
				Gson gson = new Gson();
				BufferedReader reader = new BufferedReader(new FileReader(file));
				Map<?, ?> map = gson.fromJson(reader, Map.class);
				jsonFiles.put(file.getName(), map);
				reader.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		Map<?, ?> json_BreastCancerScreeningFHIR = jsonFiles.get("BreastCancerScreeningFHIR.json");

		assertEquals(json_BreastCancerScreeningFHIR.get("resourceType"), "Library");
		assertEquals(json_BreastCancerScreeningFHIR.get("id"), "BreastCancerScreeningFHIR");
		assertEquals(json_BreastCancerScreeningFHIR.get("language"), "en");
		assertEquals(json_BreastCancerScreeningFHIR.get("url"), "http://ecqi.healthit.gov/ecqms/Library/BreastCancerScreeningFHIR");
		assertEquals(json_BreastCancerScreeningFHIR.get("version"), "2.0.003");
		assertEquals(json_BreastCancerScreeningFHIR.get("name"), "BreastCancerScreeningFHIR");
		assertEquals(json_BreastCancerScreeningFHIR.get("status"), "active");
		assertEquals(json_BreastCancerScreeningFHIR.get("experimental"), false);
		assertEquals(json_BreastCancerScreeningFHIR.get("date").toString(), "2021-01-15T16:56:33+00:00");
		assertEquals(json_BreastCancerScreeningFHIR.get("publisher"), "National Committee for Quality Assurance");
		assertEquals(json_BreastCancerScreeningFHIR.get("description"), "Breast Cancer Screening FHIR");

		
		@SuppressWarnings("unchecked")
		LinkedTreeMap<String, Object> meta = (LinkedTreeMap<String, Object>) json_BreastCancerScreeningFHIR.get("meta");
		assertEquals(meta.get("versionId"), "2");
		assertEquals(meta.get("lastUpdated").toString(), "2021-01-15T11:56:34.000-05:00");
		assertEquals(meta.get("source"), "#r5t67Nu6Bnm1gVb2");
		assertEquals(((ArrayList<?>)meta.get("profile")).get(0), "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/computable-library-cqfm");

		
		@SuppressWarnings("unchecked")
		LinkedTreeMap<String, Object> extArr = (LinkedTreeMap<String, Object>) ((ArrayList<?>) json_BreastCancerScreeningFHIR.get("extension")).get(0);
		assertEquals(extArr.get("url"), "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-softwaresystem");
		@SuppressWarnings("unchecked")
		LinkedTreeMap<String, Object> valRefLTM = (LinkedTreeMap<String, Object>) extArr.get("valueReference");
		assertEquals(valRefLTM.get("reference"), "cqf-tooling");
		
		
		@SuppressWarnings("unchecked")
		LinkedTreeMap<String, Object> type = (LinkedTreeMap<String, Object>)json_BreastCancerScreeningFHIR.get("type");
		@SuppressWarnings("unchecked")
		LinkedTreeMap<String, Object> coding = (LinkedTreeMap<String, Object>) ((ArrayList<?>) type.get("coding")).get(0);
		assertEquals(coding.get("system"), "http://terminology.hl7.org/CodeSystem/library-type");
		assertEquals(coding.get("code"), "logic-library");
		
	 
		@SuppressWarnings("unchecked")
		ArrayList<LinkedTreeMap<String, Object>> relatedArtifact = (ArrayList<LinkedTreeMap<String, Object>>) json_BreastCancerScreeningFHIR.get("relatedArtifact");
		assertEquals(relatedArtifact.size(), 51);

		@SuppressWarnings("unchecked")
		ArrayList<LinkedTreeMap<String, Object>> parameter = (ArrayList<LinkedTreeMap<String, Object>>) json_BreastCancerScreeningFHIR.get("parameter");
		assertEquals(parameter.size(), 34);

		@SuppressWarnings("unchecked")
		ArrayList<LinkedTreeMap<String, Object>> dataRequirement = (ArrayList<LinkedTreeMap<String, Object>>) json_BreastCancerScreeningFHIR.get("dataRequirement");
		assertEquals(dataRequirement.size(), 82);
		
		
		Map<?, ?> json_libdepsBCSFHIRbundle = jsonFiles.get("library-deps-BreastCancerScreeningFHIR-bundle.json");
		
		System.out.println(json_libdepsBCSFHIRbundle);
		
	}

	

	/**
	 * This test simply takes the list of measures and checks that bundled files match up.
	 * Also some verification of source IG, and UcumService after refreshIG. 
	 */
	@Test
	public void testInitializeFromINI() {
		IGProcessor processor = new IGProcessor();
		RefreshIGParameters params = new RefreshIGArgumentProcessor().parseAndConvert(ARGS);
		String args[] = { "-RefreshIG", "-ini=" + INI_LOC, "-t", "-d", "-p" };
		params = new RefreshIGArgumentProcessor().parseAndConvert(args);
		processor = new IGProcessor();
		processor.initializeFromIni(params.ini);

		IniFile ini = new IniFile(new File(INI_LOC).getAbsolutePath());
		String rootDir = Utilities.getDirectoryForFile(ini.getFileName());
		assertEquals(processor.getRootDir(), rootDir);
		assertEquals(processor.getCanonicalBase(), "http://ecqi.healthit.gov/ecqms");
		assertEquals(processor.getBinaryPaths().size(), 1);
		assertEquals(processor.getFhirVersion(), "4.0.1");
		assertEquals(processor.getPackageId(), ID);
		assertEquals(processor.getSourceIg().getName(), "eCQMContentR4");
		assertEquals(processor.getSourceIg().getCopyright(), null);
		assertEquals(processor.getSourceIg().getContactMax(), CONTACT);
		assertEquals(processor.getSourceIg().getCopyrightMax(), 1);
		assertEquals(processor.getSourceIg().getDateMax(), 1);
		assertEquals(processor.getSourceIg().getDescription(),
				"Draft electronic Clinical Quality Measure (eCQM) Implementation Guide");
		assertEquals(processor.getSourceIg().getDescriptionMax(), 1);
		assertEquals(processor.getSourceIg().getExperimental(), false);
		assertEquals(processor.getSourceIg().getExperimentalMax(), 1);
		assertEquals(processor.getSourceIg().getId(), ID);
		assertEquals(processor.getSourceIg().getIdBase(), ID);
		assertEquals(processor.getSourceIg().getIdentifierMax(), 0);
		assertEquals(processor.getSourceIg().getImplicitRules(), null);
		assertEquals(processor.getSourceIg().getJurisdictionMax(), CONTACT);
		assertEquals(processor.getSourceIg().getLanguage(), null);
		assertEquals(processor.getSourceIg().getNameMax(), 1);
		assertEquals(processor.getSourceIg().getPackageId(), ID);
		assertEquals(processor.getSourceIg().getPublisher(), "cqframework");
		assertEquals(processor.getSourceIg().getPublisherMax(), 1);
		assertEquals(processor.getSourceIg().getPurposeMax(), 0);
		assertEquals(processor.getUcumService(), null);
		assertEquals(processor.getSourceIg().getStatusMax(), 1);
		assertEquals(processor.getSourceIg().getTitle(), "eCQM FHIR R4 Content Implementation Guide");
		assertEquals(processor.getSourceIg().getTitleMax(), 1);
		assertEquals(processor.getSourceIg().getUrl(),
				"http://ecqi.healthit.gov/ecqms/ImplementationGuide/gov.healthit.ecqi.ecqms");
		assertEquals(processor.getSourceIg().getUrlMax(), 1);
		assertEquals(processor.getSourceIg().getUseContextMax(), 2147483647);
		assertEquals(processor.getSourceIg().getVersion(), "2021");
		assertEquals(processor.getSourceIg().getVersionMax(), 1);

		try {
			processor.getSourceIg().getPurpose();
		} catch (Error e) {
			assertEquals(e.getMessage(),
					"The resource type \"ImplementationGuide\" does not implement the property \"purpose\"");
		}

		processor.refreshIG(params);

		assertNotNull(processor.getUcumService());

		assertEquals(params.outputEncoding, IOUtils.Encoding.JSON);
		assertEquals(params.includeELM, false);
		assertEquals(params.includeDependencies, true);
		assertEquals(params.includeTerminology, true);
		assertEquals(params.includePatientScenarios, true);
		assertEquals(params.versioned, false);
		assertEquals(params.cdsHooksIg, false);
		assertNull(params.fhirUri);

		FhirContext fhirContext = IGProcessor.getIgFhirContext(processor.getFhirVersion());
		assertEquals(fhirContext.getVersion().getPathToSchemaDefinitions(), "/org/hl7/fhir/r4/model/schema");

		Map<String, IBaseResource> measures = IOUtils.getMeasures(fhirContext);
		
		IGBundleProcessor.bundleIg(processor.refreshedResourcesNames, rootDir, params.outputEncoding, params.includeELM,
				params.includeDependencies, params.includeTerminology, params.includePatientScenarios, params.versioned,
				params.cdsHooksIg, fhirContext, null);
		
		for (String measureName : measures.keySet()) {
			File existingBCSfiles = new File(System.getProperty("user.dir") + "\\" + BUNDLED_FILES_LOC + "\\"
					+ measureName + "\\" + measureName + "-files");
			assertTrue(existingBCSfiles.listFiles().length > 0);
		}

	}
 

}
