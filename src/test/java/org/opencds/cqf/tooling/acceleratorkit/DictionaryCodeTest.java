package org.opencds.cqf.tooling.acceleratorkit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class DictionaryCodeTest {
	private DictionaryCode d;
	@Before
	public void setUp() throws Exception {
		this.d = new DictionaryCode();
		//leading space to test Trim, included char 160 to test replace
		d.setCode(" 123" + (char) 160 + "123");
		d.setDisplay(" display"  + (char) 160 + "display");
		d.setLabel(" label"  + (char) 160 + "label");
		d.setParent("parent");
		d.setSystem("system");		
	}

	@Test
	public void testBasics() {
		assertEquals(d.getCode(), "123" + (char) 32 + "123");
		assertEquals(d.getDisplay(), "display" + (char) 32 + "display");
		assertEquals(d.getLabel(), "label" + (char) 32 + "label");
		assertEquals(d.getParent(), "parent");
		assertEquals(d.getSystem(), "system");
		
		//should not be null
		assertEquals(d.getTerminologies().size(), 0);
		
		assertTrue(d.getTerminologies().size() == 0);
	}

}
