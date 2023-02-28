/**
 * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
 * <p>
 * This program is free software: you can use, redistribute, and/or modify
 * it under the terms of the GNU Affero General Public License, version 3
 * or later ("AGPL"), as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
