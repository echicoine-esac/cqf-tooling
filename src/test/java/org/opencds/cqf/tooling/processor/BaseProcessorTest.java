package org.opencds.cqf.tooling.processor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu2.model.ImplementationGuide;
import org.junit.Test;
import org.opencds.cqf.tooling.npm.NpmPackageManager;
import org.opencds.cqf.tooling.parameter.RefreshLibraryParameters;

public class BaseProcessorTest {

	@Test
	public void initializeTest() {
		BaseProcessor bp = new BaseProcessor();
		RefreshLibraryParameters params = null;
		// configure RefreshLibraryParameters
		bp.initialize(params.parentContext);

		assertEquals(bp.getRootDir(), "");

		ImplementationGuide ig = new ImplementationGuide();
		// configure ig
		assertEquals(bp.getSourceIg(), ig);
		assertEquals(bp.getFhirVersion(), "1.0");

		assertEquals(bp.getPackageId(), "");
		assertEquals(bp.getCanonicalBase(), "");

		List<String> ucum = new ArrayList<>();

		assertEquals(bp.getUcumService().validateUCUM(), ucum);

		NpmPackageManager npm = null;
		try {
			npm = new NpmPackageManager(bp.getSourceIg(), bp.getFhirVersion());
		} catch (IOException e) {
			e.printStackTrace();
		}

		assertEquals(bp.getPackageManager().getNpmList(), npm.getNpmList());

		assertEquals(bp.getRootDir(), "");
	}

	@Test
	public void initializeFromIniTest() {
		BaseProcessor bp = new BaseProcessor();
		RefreshLibraryParameters params = null;
		// configure RefreshLibraryParameters
		bp.initializeFromIni(params.ini);

		assertEquals(bp.getRootDir(), "");

		ImplementationGuide ig = new ImplementationGuide();
		// configure ig
		assertEquals(bp.getSourceIg(), ig);
		assertEquals(bp.getFhirVersion(), "1.0");

		assertEquals(bp.getPackageId(), "");
		assertEquals(bp.getCanonicalBase(), "");

		List<String> ucum = new ArrayList<>();

		assertEquals(bp.getUcumService().validateUCUM(), ucum);

		NpmPackageManager npm = null;
		try {
			npm = new NpmPackageManager(bp.getSourceIg(), bp.getFhirVersion());
		} catch (IOException e) {
			e.printStackTrace();
		}

		assertEquals(bp.getPackageManager().getNpmList(), npm.getNpmList());

		assertEquals(bp.getRootDir(), "");

	}

	// TODO
	@Test
	public void initializeFromIgTest() {
		BaseProcessor bp = new BaseProcessor();
		RefreshLibraryParameters params = null;
		// configure RefreshLibraryParameters
		bp.initializeFromIg("root", "path", "version");

		ImplementationGuide ig = new ImplementationGuide();
		// configure ig
		assertEquals(bp.getSourceIg(), ig);
		assertEquals(bp.getFhirVersion(), "1.0");

		assertEquals(bp.getPackageId(), "");
		assertEquals(bp.getCanonicalBase(), "");

		List<String> ucum = new ArrayList<>();

		assertEquals(bp.getUcumService().validateUCUM(), ucum);

		NpmPackageManager npm = null;
		try {
			npm = new NpmPackageManager(bp.getSourceIg(), bp.getFhirVersion());
		} catch (IOException e) {
			e.printStackTrace();
		}

		assertEquals(bp.getPackageManager().getNpmList(), npm.getNpmList());

		assertEquals(bp.getRootDir(), "");

	}
}
