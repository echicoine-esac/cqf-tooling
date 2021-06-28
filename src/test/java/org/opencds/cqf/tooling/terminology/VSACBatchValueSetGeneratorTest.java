package org.opencds.cqf.tooling.terminology;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class VSACBatchValueSetGeneratorTest {

	private static final String BASE_URL_FLAG_IS_NOT_VALID_WITH_VALUE_SET_SOURCE_FLAG_SET_TO_CMS = "baseUrl flag is not valid with valueSetSource flag set to 'cms'";
	private static final String UNC_PATH_IS_MISSING_HOSTNAME = "UNC path is missing hostname: ";
	private static final String INVALID_ARGUMENT = "Invalid argument: ";
	private static final String THE_SPECIFIED_PATH_TO_VALUESET_FILES_IS_NOT_A_DIRECTORY = "The specified path to valueset files is not a directory";
	private static final String THE_SPECIFIED_PATH_TO_VALUESET_FILES_IS_EMPTY = "The specified path to valueset files is empty";

	private VSACBatchValueSetGenerator v;

	@Before
	public void setUp() {
		v = new VSACBatchValueSetGenerator();
	}

	@Test
	public void testExecute() {
	String[] args = { 
			"-VsacXlsxToValueSetBatch", 
			"-ptsd=src/test/resources/org/opencds/cqf/tooling/terminology",
			"-op=target/test/resources/org/opencds/cqf/tooling/terminology/output", 
			"-setname=true", 
			"-vssrc=cms" 
			};
		v.execute(args);
		int fileCount = new File("target/test/resources/org/opencds/cqf/tooling/terminology/output").list().length;
		assertEquals(586, fileCount);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidArgument() {
		String expectedException = UNC_PATH_IS_MISSING_HOSTNAME + "\\\\";
		
		String[] args = { 
				"-VsacXlsxToValueSetBatch", 
				"-ptsd=\\\\",
				"-op=\\\\", 
				"-setname=true", 
				"-vssrc=cms" 
				};
		try {
			v.execute(args);
		} catch (Exception e) {
			assertEquals(e.getMessage(), expectedException);
			throw e;
		}
		
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNullPathToSpreadsheetDirectory() {
		String expectedException = INVALID_ARGUMENT + "-ptsd=";
		
		String[] args = { 
				"-VsacXlsxToValueSetBatch", 
				"-ptsd=",
				"-op=target/test/resources/org/opencds/cqf/tooling/terminology/output", 
				"-setname=true", 
				"-vssrc=cms" 
				};
		
		try {
			v.execute(args);
		} catch (Exception e) {
			assertEquals(e.getMessage(), expectedException);
			throw e;
		}
	}
 
	
	@Test(expected = RuntimeException.class)
	public void testInvalidPathToSpreadsheetDirectory() {
		String expectedException = THE_SPECIFIED_PATH_TO_VALUESET_FILES_IS_NOT_A_DIRECTORY;
		
		String[] args = { 
				"-VsacXlsxToValueSetBatch", 
				"-ptsd=\\\\",
				"-op=target/test/resources/org/opencds/cqf/tooling/terminology/output", 
				"-setname=true", 
				"-vssrc=cms" 
				};
		
		try {
			v.execute(args);
		} catch (Exception e) {
			assertEquals(e.getMessage(), expectedException);
			throw e;
		}
	}
	
	@Test(expected = RuntimeException.class)
	public void testEmptyDirectory() {
		String expectedException = THE_SPECIFIED_PATH_TO_VALUESET_FILES_IS_EMPTY;
		String dir = "/testing/empty_directory";
		File theDir = new File(dir);
		try {
			//if the directory doesn't exist, make it.  If it already exists, 
			//delete it, make it again to ensure 0 file count.
			if (!theDir.exists()){
			    theDir.mkdirs();
			}else {
				theDir.delete();
				theDir.mkdirs();
			}
		
			//pass in the empty dir:
			String[] args = { 
					"-VsacXlsxToValueSetBatch", 
					"-ptsd="+dir,
					"-op=target/test/resources/org/opencds/cqf/tooling/terminology/output", 
					"-setname=true", 
					"-vssrc=cms" 
					};
			
			v.execute(args);
		} catch (Exception e) {
			assertEquals(e.getMessage(), expectedException);
			throw e;
		}
	}
	
	@Test(expected = RuntimeException.class)
	public void testBUrlAndVSSRCisCMS() {
		String expectedException = BASE_URL_FLAG_IS_NOT_VALID_WITH_VALUE_SET_SOURCE_FLAG_SET_TO_CMS;
		
		String[] args = { 
				"-VsacXlsxToValueSetBatch", 
				"-ptsd=src/test/resources/org/opencds/cqf/tooling/terminology",
				"-op=target/test/resources/org/opencds/cqf/tooling/terminology/output", 
				"-setname=true", 
				"-vssrc=cms",
				"-burl=test.com"
				};
		
		try {
			v.execute(args);
		} catch (Exception e) {
			assertEquals(e.getMessage(), expectedException);
			throw e;
		}
	}
	 
}
