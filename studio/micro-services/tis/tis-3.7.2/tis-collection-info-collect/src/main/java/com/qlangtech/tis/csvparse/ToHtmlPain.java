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
package com.qlangtech.tis.csvparse;

import java.util.Formatter;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ToHtmlPain {

    // private StringBuffer out = new StringBuffer();
    // = new Formatter(output);
    private final Formatter out;

    public ToHtmlPain(StringBuffer buffer) {
        super();
        this.out = new Formatter(buffer);
    }

    // private void writeDateHeader(final HistoryAvarageResult historyRecord,
    // XSSFSheet sheet) {
    // int colIndex = 3;
    // XSSFRow row;
    // row = sheet.createRow(2);
    // ExcelRow erow = new ExcelRow(row, "");
    // // 写日期title
    // for (int i = 0; i < 2; i++) {
    // for (Date date : historyRecord.dates) {
    // erow.setString(colIndex++, datef.format(date));
    // }
    // }
    // }
    public void printHeader() {
        out.format("<thead>                                                             ");
        out.format(" <tr>                                                               ");
        out.format("    <th class=\"style_03\" rowspan=\"2\">业务线</th>                ");
        out.format("    <th class=\"style_09\" rowspan=\"2\">部门</th>                  ");
        out.format("    <th class=\"style_09\" rowspan=\"2\">索引名称</th>              ");
        out.format("    <th class=\"style_07\" colspan=\"7\">QP</th>                    ");
        out.format("                                                                    ");
        out.format("    <th class=\"style_0b\" colspan=\"7\">索引条目</th>              ");
        out.format("                                                                    ");
        out.format("    <th class=\"style_05\" rowspan=\"2\">机器数</th>                ");
        out.format("</tr>                                                               ");
        out.format("<tr>                                                                ");
        out.format("                                                                    ");
        // region 1
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">04/26</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">04/27</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">04/28</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">04/29</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">04/30</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">05/01</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">05/02</th>   ");
        // region 2
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">04/26</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">04/27</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">04/28</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">04/29</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">04/30</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">05/01</th>   ");
        out.format("    <th class=\"style_00\" style=\"text-align: left;\">05/02</th>   ");
        out.format("</tr>                                                               ");
        out.format("</thead>                                                            ");
    }
}
