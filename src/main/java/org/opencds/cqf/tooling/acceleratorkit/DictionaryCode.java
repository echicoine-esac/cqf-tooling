package org.opencds.cqf.tooling.acceleratorkit;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

/**
 * Created by Bryn on 8/18/2019.
 */
public class DictionaryCode {

	private String code;
	private String display;
	private String label;
	private String parent;
	private String system;
	private List<CodeableConcept> terminologies;

	public String getCode() {
		return this.code;
	}

	public String getDisplay() {
		return this.display;
	}

	public String getLabel() {
		return this.label;
	}

	public String getParent() {
		return this.parent;
	}

	public String getSystem() {
		return this.system;
	}

	public List<CodeableConcept> getTerminologies() {
		if (this.terminologies == null) {
			this.terminologies = new ArrayList<>();
		}
		return this.terminologies;
	}

	public void setCode(String code) {
		if (code == null) {
			this.code = null;
		}
		this.code = code.replace((char) 160, (char) 32).trim();
	}

	public void setDisplay(String display) {
		if (display == null) {
			this.display = null;
		}
		this.display = display.replace((char) 160, (char) 32).trim();
	}

	public void setLabel(String label) {
		if (label == null) {
			this.label = null;
		}
		this.label = label.replace((char) 160, (char) 32).trim();
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public CodeableConcept toCodeableConcept() {
		CodeableConcept cc = new CodeableConcept();
		// cc.setText(this.label);
		Coding coding = new Coding();
		coding.setCode(this.code);
		coding.setDisplay(this.display);
		// TODO: Support different systems here
		coding.setSystem(this.system);
		cc.addCoding(coding);
		return cc;
	}
}
