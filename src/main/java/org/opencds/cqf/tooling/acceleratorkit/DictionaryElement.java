package org.opencds.cqf.tooling.acceleratorkit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Bryn on 8/18/2019.
 *
 * Represents a WHO Accelerator Kit Data Dictionary Element
 */
public class DictionaryElement {
    private String activity;

    private String calculation;
    private List<DictionaryCode> choices;
    private DictionaryCode code;

    private ArrayList<String> codeSystemUrls;
    private String constraint;
    private String dataElementLabel;

    private String dataElementName;
    private String description;
    private String due;

    private String editable;
    private DictionaryFhirElementPath fhirElementPath;
    private String group;

    private String id;
    private String infoIcon;

    private String label;
    private String masterDataType;

    private String name;
    private String notes;
    private String page;

    private String relevance;
    private String required;
    private String scope;

    private String type; // Image, Note, QR Code, Text, Date, Checkbox, Calculation, Integer, MC (select one), MC (select multiple), Toaster message
    public DictionaryElement(String id, String name) {
        if (id == null || id.equals("")) {
            throw new IllegalArgumentException("id required");
        }
        this.id = id;
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("name required");
        }
        this.name = name;
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof DictionaryElement && ((DictionaryElement)obj).name.equals(name);
    }

    public String getActivity() {
        return this.activity;
    }
    public String getCalculation() {
        return this.calculation;
    }
    public List<DictionaryCode> getChoices() {
        if (this.choices == null) {
            this.choices = new ArrayList<>();
        }
        return this.choices;
    }

    public List<DictionaryCode> getChoicesForSystem(String system) {
        if (this.choices == null) {
            this.choices = new ArrayList<>();
        }
        List<DictionaryCode> codes = this.getValidChoices().stream()
                .filter((c) -> c.getSystem().equals(system))
                .collect(Collectors.toList());
        return codes;
    }
    public DictionaryCode getCode() {
        return this.code;
    }
    public List<String> getCodeSystemUrls() {
        if (this.codeSystemUrls == null) {
            this.codeSystemUrls = new ArrayList<>();
        }
        List<String> codeSystemUrls = this.getValidChoices().stream()
                .map((c) -> c.getSystem())
                .distinct()
                .collect(Collectors.toList());
        return codeSystemUrls;
    }

    public String getConstraint() {
        return this.constraint;
    }
    public String getDataElementLabel() {
        return this.dataElementLabel;
    }
    public String getDataElementName() {
        return this.dataElementName;
    }

    public String getDescription() {
        return this.description;
    }
    public String getDue() {
        return this.due;
    }
    public String getEditable() {
        return this.editable;
    }

    public DictionaryFhirElementPath getFhirElementPath() {
        return this.fhirElementPath;
    }
    public String getGroup() {
        return this.group;
    }
    public String getId() {
        return this.id;
    }

    public String getInfoIcon() {
        return this.infoIcon;
    }
    public String getLabel() {
        return this.label;
    }
    public String getMasterDataType() {
        return this.masterDataType;
    }

    public String getName() {
        return this.name;
    }
    public String getNotes() {
        return this.notes;
    }
    public String getPage() {
        return this.page;
    }
    public String getRelevance() {
        return this.relevance;
    }

    public String getRequired() {
        return this.required;
    }
    public String getScope() {
        return this.scope;
    }

    public String getType() {
        return this.type;
    }
    public List<DictionaryCode> getValidChoices() {
        return this.getChoices().stream()
                .filter((c) -> !c.getCode().trim().isEmpty())
                .collect(Collectors.toList());
    }
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }
    public void setCalculation(String calculation) {
        this.calculation = calculation;
    }
    public void setCode(DictionaryCode code) {
        this.code = code;
    }

    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }
    public void setDataElementLabel(String dataElementLabel) {
        this.dataElementLabel = dataElementLabel;
    }
    public void setDataElementName(String dataElementName) {
        this.dataElementName = dataElementName;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public void setDue(String due) {
        this.due = due;
    }
    public void setEditable(String editable) {
        this.editable = editable;
    }

    public void setFhirElementPath(DictionaryFhirElementPath fhirElementPath) {
        this.fhirElementPath = fhirElementPath;
    }
    public void setGroup(String group) {
        this.group = group;
    }
    public void setInfoIcon(String infoIcon) {
        this.infoIcon = infoIcon;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    public void setMasterDataType(String masterDataType) {
        this.masterDataType = masterDataType;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setPage(String page) {
        this.page = page;
    }
    public void setRelevance(String relevance) {
        this.relevance = relevance;
    }
    public void setRequired(String required) {
        this.required = required;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setType(String type) {
        this.type = type;
    }
}
