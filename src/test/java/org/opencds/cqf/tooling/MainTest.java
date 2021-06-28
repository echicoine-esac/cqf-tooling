package org.opencds.cqf.tooling;

import org.junit.Test;

public class MainTest {

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidArgs() {
		String[] args = { "" };
		Main.main(args);
	}

}
