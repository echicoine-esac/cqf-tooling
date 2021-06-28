package org.opencds.cqf.tooling.measure.r4;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class CodeTerminologyRefTest {
	CodeTerminologyRef c;
	String EXPECTED_DEF = "code \"name\" : 'id' from \"codeSystemName\" display 'displayName'";
	@Before
	public void setUp() throws Exception {
		c = new CodeTerminologyRef("name", "id", "codeSystemName", "codeSystemId", "displayName");
	}

	@Test
	public void test() {
		assertEquals(c.getDefinition(), EXPECTED_DEF);
	}

}
