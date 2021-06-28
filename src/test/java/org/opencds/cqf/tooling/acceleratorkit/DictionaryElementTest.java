package org.opencds.cqf.tooling.acceleratorkit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class DictionaryElementTest {
	DictionaryElement d;
	
	@Before
	public void setUp() throws Exception {
		d = new DictionaryElement("id123", "name123");
	}

	@Test
	public void testEquals() {
		DictionaryElement d2 = new DictionaryElement("id123", "name123");
		assertTrue(d.equals(d2));
	}

	@Test
	public void testEmptyGetChoices() {
		//should return empty list instead of null
		assertEquals(0, d.getChoices().size());
	}

	
}
