package com.aliyun.oss.model;

import java.io.Serializable;

/**
 * Define input serialization of the select object operations.
 */
public class InputSerialization implements Serializable {
    // Default input format is csv
    private SelectContentFormat selectContentFormat = SelectContentFormat.CSV;
    private CSVFormat csvInputFormat = new CSVFormat();
    private JsonFormat jsonInputFormat = new JsonFormat();
    private String compressionType = CompressionType.NONE.name();

    public SelectContentFormat getSelectContentFormat() {
        return selectContentFormat;
    }

    public void setSelectContentFormat(SelectContentFormat selectContentFormat) {
        this.selectContentFormat = selectContentFormat;
    }

    public CSVFormat getCsvInputFormat() {
        return csvInputFormat;
    }

    public void setCsvInputFormat(CSVFormat csvInputFormat) {
        setSelectContentFormat(SelectContentFormat.CSV);
        this.csvInputFormat = csvInputFormat;
    }

    public InputSerialization withCsvInputFormat(CSVFormat csvFormat) {
        setCsvInputFormat(csvFormat);
        return this;
    }

    public JsonFormat getJsonInputFormat() {
        return jsonInputFormat;
    }

    public void setJsonInputFormat(JsonFormat jsonInputFormat) {
        if (jsonInputFormat.getJsonType() == null) {
            throw new IllegalArgumentException("Please set json type for this input, valid types are DOCUMENT and LINES");
        }
        setSelectContentFormat(SelectContentFormat.JSON);
        this.jsonInputFormat = jsonInputFormat;
    }

    public InputSerialization withJsonInputFormat(JsonFormat jsonInputFormat) {
        setJsonInputFormat(jsonInputFormat);
        return this;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(CompressionType compressionType) {
        this.compressionType = compressionType.name();
    }

    public InputSerialization withCompressionType(CompressionType compressionType) {
        setCompressionType(compressionType);
        return this;
    }
}
