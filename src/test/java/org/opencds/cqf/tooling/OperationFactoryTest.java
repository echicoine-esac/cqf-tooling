package org.opencds.cqf.tooling;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Test;
import org.opencds.cqf.tooling.acceleratorkit.DTProcessor;
import org.opencds.cqf.tooling.acceleratorkit.Processor;
import org.opencds.cqf.tooling.library.r4.LibraryGenerator;
import org.opencds.cqf.tooling.measure.r4.RefreshR4MeasureOperation;
import org.opencds.cqf.tooling.measure.stu3.RefreshStu3MeasureOperation;
import org.opencds.cqf.tooling.modelinfo.StructureDefinitionToModelInfo;
import org.opencds.cqf.tooling.operation.BundleResources;
import org.opencds.cqf.tooling.operation.ExecuteMeasureTestOperation;
import org.opencds.cqf.tooling.operation.ExtractMatBundleOperation;
import org.opencds.cqf.tooling.operation.IgBundler;
import org.opencds.cqf.tooling.operation.PostBundlesInDirOperation;
import org.opencds.cqf.tooling.operation.RefreshIGOperation;
import org.opencds.cqf.tooling.operation.RefreshLibraryOperation;
import org.opencds.cqf.tooling.operation.ScaffoldOperation;
import org.opencds.cqf.tooling.operation.TestIGOperation;
import org.opencds.cqf.tooling.qdm.QdmToQiCore;
import org.opencds.cqf.tooling.quick.QuickPageGenerator;
import org.opencds.cqf.tooling.terminology.CMSFlatMultiValueSetGenerator;
import org.opencds.cqf.tooling.terminology.EnsureExecutableValueSetOperation;
import org.opencds.cqf.tooling.terminology.GenericValueSetGenerator;
import org.opencds.cqf.tooling.terminology.HEDISValueSetGenerator;
import org.opencds.cqf.tooling.terminology.RCKMSJurisdictionsGenerator;
import org.opencds.cqf.tooling.terminology.SpreadsheetToCQLOperation;
import org.opencds.cqf.tooling.terminology.ToJsonValueSetDbOperation;
import org.opencds.cqf.tooling.terminology.VSACBatchValueSetGenerator;
import org.opencds.cqf.tooling.terminology.VSACValueSetGenerator;
import org.opencds.cqf.tooling.terminology.distributable.DistributableValueSetGenerator;

public class OperationFactoryTest {

	@Test
	public void test_QdmToQiCore() {
		assertEquals(OperationFactory.createOperation(OperationConstants.QDM_TO_QI_CORE).getClass(), QdmToQiCore.class);
	}

	@Test
	public void test_QuickPageGenerator() {
		assertEquals(OperationFactory.createOperation(OperationConstants.QI_CORE_QUICK).getClass(),
				QuickPageGenerator.class);
	}

	@Test
	public void test_VSACValueSetGenerator() {
		assertEquals(OperationFactory.createOperation(OperationConstants.VSAC_XLSX_TO_VALUE_SET).getClass(),
				VSACValueSetGenerator.class);
	}

	@Test
	public void test_DistributableValueSetGenerator() {
		assertEquals(OperationFactory.createOperation(OperationConstants.DISTRIBUTABLE_XLSX_TO_VALUE_SET).getClass(),
				DistributableValueSetGenerator.class);
	}

	@Test
	public void test_CMSFlatMultiValueSetGenerator() {
		assertEquals(OperationFactory.createOperation(OperationConstants.VSAC_MULTI_XLSX_TO_VALUE_SET).getClass(),
				CMSFlatMultiValueSetGenerator.class);
	}

	@Test
	public void test_VSACBatchValueSetGenerator() {
		assertEquals(OperationFactory.createOperation(OperationConstants.VSAC_XLSX_TO_VALUE_SET_BATCH).getClass(),
				VSACBatchValueSetGenerator.class);
	}

	@Test
	public void test_HEDISValueSetGenerator() {
		assertEquals(OperationFactory.createOperation(OperationConstants.HEDIS_XLSX_TO_VALUE_SET).getClass(),
				HEDISValueSetGenerator.class);
	}

	@Test
	public void test_GenericValueSetGenerator() {
		assertEquals(OperationFactory.createOperation(OperationConstants.XLSX_TO_VALUE_SET).getClass(),
				GenericValueSetGenerator.class);
	}

	@Test
	public void test_EnsureExecutableValueSetOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.ENSURE_EXECUTABLE_VALUE_SET).getClass(),
				EnsureExecutableValueSetOperation.class);
	}

	@Test
	public void test_ToJsonValueSetDbOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.TO_JSON_VALUE_SET_DB).getClass(),
				ToJsonValueSetDbOperation.class);
	}

	@Test
	public void test_CqlToSTU3Library() {
		assertEquals(OperationFactory.createOperation(OperationConstants.CQL_TO_STU3_LIBRARY).getClass(),
				org.opencds.cqf.tooling.library.stu3.LibraryGenerator.class);
	}

	@Test
	public void test_CqlToR4Library() {
		assertEquals(OperationFactory.createOperation(OperationConstants.CQL_TO_R4_LIBRARY).getClass(),
				LibraryGenerator.class);
	}

	@Test
	public void test_UpdateSTU3Cql() {
		assertEquals(OperationFactory.createOperation(OperationConstants.UPDATE_STU3_CQL).getClass(),
				org.opencds.cqf.tooling.library.stu3.LibraryGenerator.class);
	}

	@Test
	public void test_UpdateR4Cql() {
		assertEquals(OperationFactory.createOperation(OperationConstants.UPDATE_R4_CQL).getClass(),
				LibraryGenerator.class);
	}

	@Test(expected = NotImplementedException.class)
	public void test_JsonSchemaGenerator() {
		OperationFactory.createOperation(OperationConstants.JSON_SCHEMA_GENERATOR);
	}

	@Test
	public void test_BundleIg() {
		assertEquals(OperationFactory.createOperation(OperationConstants.BUNDLE_IG).getClass(), IgBundler.class);
	}

	@Test
	public void test_RefreshIGOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.REFRESH_IG).getClass(),
				RefreshIGOperation.class);
	}

	@Test
	public void test_RefreshLibraryOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.REFRESH_LIBRARY).getClass(),
				RefreshLibraryOperation.class);
	}

	@Test
	public void test_RefreshStu3MeasureOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.REFRESH_STU3_MEASURE).getClass(),
				RefreshStu3MeasureOperation.class);
	}

	@Test
	public void test_RefreshR4MeasureOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.REFRESH_R4_MEASURE).getClass(),
				RefreshR4MeasureOperation.class);
	}

	@Test
	public void test_ScaffoldOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.SCAFFOLD_IG).getClass(),
				ScaffoldOperation.class);
	}

	@Test
	public void test_TestIGOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.TEST_IG).getClass(), TestIGOperation.class);
	}

	@Test(expected = NotImplementedException.class)
	public void test_CqlToMeasure() {
		OperationFactory.createOperation(OperationConstants.CQL_TO_MEASURE);
	}

	@Test(expected = NotImplementedException.class)
	public void test_BundlesToBundle() {
		OperationFactory.createOperation(OperationConstants.BUNDLES_TO_BUNDLE);
	}

	@Test(expected = NotImplementedException.class)
	public void test_BundleToResources() {
		OperationFactory.createOperation(OperationConstants.BUNDLE_TO_RESOURCES);
	}

	@Test
	public void test_ExtractMatBundleOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.EXTRACT_MAT_BUNDLE).getClass(),
				ExtractMatBundleOperation.class);
	}

	@Test
	public void test_StructureDefinitionToModelInfo() {
		assertEquals(OperationFactory.createOperation(OperationConstants.GENERATE_M_IS).getClass(),
				StructureDefinitionToModelInfo.class);
	}

	@Test
	public void test_Processor() {
		assertEquals(OperationFactory.createOperation(OperationConstants.PROCESS_ACCELERATOR_KIT).getClass(),
				Processor.class);
	}

	@Test
	public void test_DTProcessor() {
		assertEquals(OperationFactory.createOperation(OperationConstants.PROCESS_DECISION_TABLES).getClass(),
				DTProcessor.class);
	}

	@Test
	public void test_BundleResources() {
		assertEquals(OperationFactory.createOperation(OperationConstants.BUNDLE_RESOURCES).getClass(),
				BundleResources.class);
	}

	@Test
	public void test_PostBundlesInDirOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.POST_BUNDLES_IN_DIR).getClass(),
				PostBundlesInDirOperation.class);
	}

	@Test
	public void test_RCKMSJurisdictionsGenerator() {
		assertEquals(OperationFactory.createOperation(OperationConstants.JURISDICTIONS_XLSX_TO_CODE_SYSTEM).getClass(),
				RCKMSJurisdictionsGenerator.class);
	}

	@Test
	public void test_ExecuteMeasureTestOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.EXECUTE_MEASURE_TEST).getClass(),
				ExecuteMeasureTestOperation.class);
	}

	@Test
	public void test_SpreadsheetToCQLOperation() {
		assertEquals(OperationFactory.createOperation(OperationConstants.SPREADSHEET_TO_CQL).getClass(),
				SpreadsheetToCQLOperation.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_default() {
		OperationFactory.createOperation("test");
	}
}
