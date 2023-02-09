package com.aliyun.oss.model;

import java.io.Serializable;

public class CSVFormat implements Serializable {
    public static enum Header {
        None, // there is no csv header
        Ignore, // we should ignore csv header and should not use csv header in select sql
        Use // we can use csv header in select sql
    }
    //Define the first line of input. Valid values: None, Ignore, Use.
    private Header headerInfo = Header.None;

    //Define the delimiter for records
    private String recordDelimiter = "\n";

    //Define the comment char, this is a single char, so getter will return the first char
    private String commentChar = "#";

    //Define the delimiter for fields, this is a single char, so getter will return the first char
    private String fieldDelimiter = ",";

    //Define the quote char, this is a single char, so getter will return the first char
    private String quoteChar = "\"";

    private boolean allowQuotedRecordDelimiter = true;

    public boolean isAllowQuotedRecordDelimiter() {
        return allowQuotedRecordDelimiter;
    }

    public void setAllowQuotedRecordDelimiter(boolean allowQuotedRecordDelimiter) {
        this.allowQuotedRecordDelimiter = allowQuotedRecordDelimiter;
    }

    public CSVFormat withAllowQuotedRecordDelimiter(boolean allowQuotedRecordDelimiter) {
        setAllowQuotedRecordDelimiter(allowQuotedRecordDelimiter);
        return this;
    }

    public String getHeaderInfo() {
        return headerInfo.name();
    }

    public void setHeaderInfo(Header headerInfo) {
        this.headerInfo = headerInfo;
    }

    public CSVFormat withHeaderInfo(Header headerInfo) {
        setHeaderInfo(headerInfo);
        return this;
    }

    public String getRecordDelimiter() {
        return recordDelimiter;
    }

    public void setRecordDelimiter(String recordDelimiter) {
        this.recordDelimiter = recordDelimiter;
    }

    public CSVFormat withRecordDelimiter(String recordDelimiter) {
        setRecordDelimiter(recordDelimiter);
        return this;
    }

    public Character getCommentChar() {
        return commentChar == null || commentChar.isEmpty() ? null : commentChar.charAt(0);
    }

    public void setCommentChar(String commentChar) {
        this.commentChar = commentChar;
    }

    public CSVFormat withCommentChar(String commentChar) {
        setCommentChar(commentChar);
        return this;
    }

    public Character getFieldDelimiter() {
        return fieldDelimiter == null || fieldDelimiter.isEmpty() ? null : fieldDelimiter.charAt(0);
    }

    public void setFieldDelimiter(String fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
    }

    public CSVFormat withFieldDelimiter(String fieldDelimiter) {
        setFieldDelimiter(fieldDelimiter);
        return this;
    }

    public Character getQuoteChar() {
        return quoteChar == null || quoteChar.isEmpty() ? null : quoteChar.charAt(0);
    }

    public void setQuoteChar(String quoteChar) {
        this.quoteChar = quoteChar;
    }

    public CSVFormat withQuoteChar(String quoteChar) {
        setQuoteChar(quoteChar);
        return this;
    }
}
