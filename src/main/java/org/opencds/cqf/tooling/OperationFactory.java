package org.opencds.cqf.tooling;

//import org.opencds.cqf.tooling.jsonschema.SchemaGenerator;
import org.apache.commons.lang.NotImplementedException;
import org.opencds.cqf.tooling.acceleratorkit.DTProcessor;
import org.opencds.cqf.tooling.acceleratorkit.Processor;
import org.opencds.cqf.tooling.library.r4.LibraryGenerator;
import org.opencds.cqf.tooling.measure.r4.RefreshR4MeasureOperation;
import org.opencds.cqf.tooling.measure.stu3.RefreshStu3MeasureOperation;
import org.opencds.cqf.tooling.modelinfo.StructureDefinitionToModelInfo;
import org.opencds.cqf.tooling.operation.*;
import org.opencds.cqf.tooling.qdm.QdmToQiCore;
import org.opencds.cqf.tooling.quick.QuickPageGenerator;
import org.opencds.cqf.tooling.terminology.*;
import org.opencds.cqf.tooling.terminology.distributable.DistributableValueSetGenerator;


class OperationFactory {

    static Operation createOperation(String operationName) {
        switch (operationName) {
            case OperationConstants.QDM_TO_QI_CORE:
                return new QdmToQiCore();
            case OperationConstants.QI_CORE_QUICK:
                return new QuickPageGenerator();
            case OperationConstants.VSAC_XLSX_TO_VALUE_SET:
                return new VSACValueSetGenerator();
            case OperationConstants.DISTRIBUTABLE_XLSX_TO_VALUE_SET:
                return new DistributableValueSetGenerator();
            case OperationConstants.VSAC_MULTI_XLSX_TO_VALUE_SET:
                return new CMSFlatMultiValueSetGenerator();
            case OperationConstants.VSAC_XLSX_TO_VALUE_SET_BATCH:
                return new VSACBatchValueSetGenerator();
            case OperationConstants.HEDIS_XLSX_TO_VALUE_SET:
                return new HEDISValueSetGenerator();
            case OperationConstants.XLSX_TO_VALUE_SET:
                return new GenericValueSetGenerator();
            case OperationConstants.ENSURE_EXECUTABLE_VALUE_SET:
                return new EnsureExecutableValueSetOperation();
            case OperationConstants.TO_JSON_VALUE_SET_DB:
                return new ToJsonValueSetDbOperation();
            case OperationConstants.CQL_TO_STU3_LIBRARY:
                return new org.opencds.cqf.tooling.library.stu3.LibraryGenerator();
            case OperationConstants.CQL_TO_R4_LIBRARY:
                return new LibraryGenerator();
            case OperationConstants.UPDATE_STU3_CQL:
                return new org.opencds.cqf.tooling.library.stu3.LibraryGenerator();
            case OperationConstants.UPDATE_R4_CQL:
                return new LibraryGenerator();
            case OperationConstants.JSON_SCHEMA_GENERATOR: 
//                return new SchemaGenerator();
            	throw new NotImplementedException("JsonSchemaGenerator");
            case OperationConstants.BUNDLE_IG:
                return new IgBundler();
//            case "PackageIG":
//                return new PackageOperation();
            case OperationConstants.REFRESH_IG:
                return new RefreshIGOperation();
            case OperationConstants.REFRESH_LIBRARY:
                return new RefreshLibraryOperation();
            case OperationConstants.REFRESH_STU3_MEASURE:
                return new RefreshStu3MeasureOperation();
            case OperationConstants.REFRESH_R4_MEASURE:
                return new RefreshR4MeasureOperation();
            case OperationConstants.SCAFFOLD_IG:
                return new ScaffoldOperation();
            case OperationConstants.TEST_IG:
                return new TestIGOperation();
            case OperationConstants.CQL_TO_MEASURE:
                throw new NotImplementedException("CqlToMeasure");
            case OperationConstants.BUNDLES_TO_BUNDLE:
                throw new NotImplementedException("BundlesToBundle");
            case OperationConstants.BUNDLE_TO_RESOURCES:
                throw new NotImplementedException("BundleToResources");
            case OperationConstants.EXTRACT_MAT_BUNDLE:
            	return new ExtractMatBundleOperation();
            case OperationConstants.GENERATE_M_IS:
                return new StructureDefinitionToModelInfo();
            case OperationConstants.PROCESS_ACCELERATOR_KIT:
                return new Processor();
            case OperationConstants.PROCESS_DECISION_TABLES:
                return new DTProcessor();
            case OperationConstants.BUNDLE_RESOURCES:
                return new BundleResources();
            case OperationConstants.POST_BUNDLES_IN_DIR:
                return new PostBundlesInDirOperation();
            case OperationConstants.JURISDICTIONS_XLSX_TO_CODE_SYSTEM:
                return new RCKMSJurisdictionsGenerator();
            case OperationConstants.EXECUTE_MEASURE_TEST:
                return new ExecuteMeasureTestOperation();
            case OperationConstants.SPREADSHEET_TO_CQL:
                return new SpreadsheetToCQLOperation();
            default:
                throw new IllegalArgumentException("Invalid operation: " + operationName);
        }
    }
}
