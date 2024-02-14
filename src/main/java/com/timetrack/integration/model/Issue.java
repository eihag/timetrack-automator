package com.timetrack.integration.model;

public class Issue extends AbstractDto {

    private Fields fields;

    private String key;

    public Fields getFields() {
        return fields;
    }

    public Issue setFields(Fields fields) {
        this.fields = fields;
        return this;
    }

    public String getKey() {
        return key;
    }

    public Issue setKey(String key) {
        this.key = key;
        return this;
    }
}
