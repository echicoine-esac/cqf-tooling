package org.opencds.cqf.tooling.parameter;

import org.junit.Test;
import org.opencds.cqf.tooling.processor.argument.RefreshIGArgumentProcessor;

public class RefreshIGParametersTest {

	 

	@Test
	public void testArgs() {
		
		
		RefreshIGArgumentProcessor rigp = new RefreshIGArgumentProcessor();
		
		String[] args = {"test", "test"};
		
		RefreshIGParameters params = rigp.parseAndConvert(args);
		
		
		
	}

}
