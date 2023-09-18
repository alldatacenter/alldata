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
