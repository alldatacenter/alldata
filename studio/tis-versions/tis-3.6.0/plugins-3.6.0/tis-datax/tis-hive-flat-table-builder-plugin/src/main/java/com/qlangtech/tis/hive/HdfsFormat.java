/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.hive;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-28 12:53
 **/
public class HdfsFormat {

    static final String KEY_LINE_DELIM = "lineDelim";
    static final String KEY_FIELD_DELIM = "fieldDelim";

    public static final HdfsFormat DEFAULT_FORMAT = new HdfsFormat("\t", HdfsFileType.TEXTFILE);

    private String fieldDelimiter;
    private final char lineDelimiter = '\n';
    private HdfsFileType fileType;

    public HdfsFormat() {
    }

    public HdfsFormat(String fieldDelimiter, HdfsFileType fileType) {
        this.fieldDelimiter = fieldDelimiter;
        this.fileType = fileType;
    }

    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    public void setFieldDelimiter(String fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
    }

    public char getLineDelimiter() {
        return this.lineDelimiter;
    }

    public HdfsFileType getFileType() {
        return fileType;
    }

    public String getRowFormat() {
        return this.fileType.getRowFormat(this);
    }

    public void setFileType(HdfsFileType fileType) {
        this.fileType = fileType;
    }
}
