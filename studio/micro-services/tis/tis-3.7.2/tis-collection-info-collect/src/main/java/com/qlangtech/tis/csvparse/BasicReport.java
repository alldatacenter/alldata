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

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class BasicReport {

    private final Date parseDate;

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    protected BasicReport(Date parseDate) {
        super();
        this.parseDate = parseDate;
    }

    public String getFormatDate() {
        return dateFormat.format(parseDate);
    }

    public Date getParseDate() {
        return parseDate;
    }

    public File createReportFileName(Date date) {
        return new File(dateFormat.format(date) + "core_report.xls");
    }
    // public static final void processExcel(final InputStream tempReader,
    // WokbookProcess wokbookProcess) throws Exception {
    // POIFSFileSystem reader = null;
    // try {
    // //	reader = new POIFSFileSystem();
    // XSSFWorkbook workbook = new XSSFWorkbook(tempReader);
    // wokbookProcess.start(workbook);
    //
    // } finally {
    // try {
    // tempReader.close();
    // } catch (Throwable e) {
    //
    // }
    // }
    // }
}
