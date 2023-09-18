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

import com.qlangtech.tis.manage.common.PropertyPlaceholderHelper;
import org.apache.commons.lang.StringUtils;

import java.util.Properties;

import static com.qlangtech.tis.hive.HdfsFormat.KEY_FIELD_DELIM;
import static com.qlangtech.tis.hive.HdfsFormat.KEY_LINE_DELIM;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-28 12:38
 **/
public enum HdfsFileType {

    TEXTFILE("text", "ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe' with SERDEPROPERTIES ('serialization.null.format'='', 'line.delim' ='${" + KEY_LINE_DELIM + "}','field.delim'='${" + KEY_FIELD_DELIM + "}')") //
    , ORC("orc", StringUtils.EMPTY);
    private final String type;
    private final String rowFormatTemplate;


    private static final PropertyPlaceholderHelper PlaceholderHelper = new PropertyPlaceholderHelper("${", "}");

    public String getType() {
        return this.name();
    }

    public String getRowFormat(HdfsFormat format) {
        Properties props = new Properties();
        props.put(KEY_LINE_DELIM, String.valueOf(format.getLineDelimiter()));
        props.put(KEY_FIELD_DELIM, format.getFieldDelimiter());
        return PlaceholderHelper.replacePlaceholders(this.rowFormatTemplate, props);
    }

    private HdfsFileType(String format, String rowFormatTemplate) {
        this.type = format;
        this.rowFormatTemplate = rowFormatTemplate;
    }

    public static HdfsFileType parse(String format) {
        for (HdfsFileType f : HdfsFileType.values()) {
            if (f.type.equals(StringUtils.lowerCase(format))) {
                return f;
            }
        }
        throw new IllegalStateException("format:" + format + " is illegal");
    }
}
