package org.example;

import java.beans.Introspector;
import java.util.*;

import static org.apache.commons.lang3.reflect.TypeUtils.isArrayType;
import static org.example.version.isComplexType;

public class FieldNode {
	String fieldName;
	String dataType;
	String parent;
	String occurrence;
	String fieldDescription;
	List<FieldNode> children = new ArrayList<>();

	public FieldNode(String fieldName, String dataType, String parent, String occurrence, String fieldDescription) {
		super();
		this.fieldName = fieldName;
		this.dataType = dataType;
		this.parent = parent;
		this.occurrence = occurrence;
		this.fieldDescription = fieldDescription;
	}

	public FieldNode() {}
	public void addChild(FieldNode child) {
		children.add(child);
	}

	public List<FieldNode> getChildren() {
		return children;
	}


	// getters

	public String getFieldName() { return fieldName; }
	public String getDataType() { return dataType; }
	public String getOccurrence() { return occurrence; }
	public String getFieldDescription() { return fieldDescription; }

	@Override
	public String toString() {
		return fieldName + " [" + parent + "] ";//+ " (" + fieldDescription + ")";
	}

	public String getJsonFieldName() {
		return Introspector.decapitalize(fieldName);
	}

	public String getJsonType() {
		if (isComplexType(dataType)) {
			return "object";
		} else if (isArrayType()) {
			return "array";
		}
		return "string";
	}

	public boolean isArrayType() {
		return fieldName.toLowerCase().endsWith("list") ||
				fieldName.toLowerCase().endsWith("array");
	}
}
