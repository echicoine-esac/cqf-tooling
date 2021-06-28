package org.opencds.cqf.tooling.operation;

import org.junit.Before;
import org.junit.Test;

public class RefreshIGOperationTest {

	RefreshIGOperation r;

	@Before
	public void setUp() throws Exception {
		r = new RefreshIGOperation();
	}

//	@Test(expected = IllegalArgumentException.class)
//	public void testBadExecute() {
//
//		String args[] = { "-ini=refreshIGTestFiles/ig.ini", "-t", "-d", "-p" };
//		r.execute(args);
//	}
	
	@Test
	public void testExecute() {
		
		String args[] = {"-RefreshIG", "-ini=src/test/java/org/opencds/cqf/tooling/operation/refreshIGTestFiles/ig.ini", "-t", "-d", "-p"};
		try {
			r.execute(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
