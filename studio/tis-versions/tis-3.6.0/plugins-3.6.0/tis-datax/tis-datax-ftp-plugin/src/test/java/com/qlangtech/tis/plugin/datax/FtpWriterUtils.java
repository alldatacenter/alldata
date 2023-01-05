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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.plugin.datax.format.CSVFormat;
import com.qlangtech.tis.plugin.datax.format.TextFormat;
import com.qlangtech.tis.plugin.datax.server.FTPServer;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-15 23:05
 **/
public class FtpWriterUtils {

    public static TextFormat createTextFormat() {
        TextFormat tformat = new TextFormat();
        tformat.header = true;
        tformat.fieldDelimiter = "comma";
        return tformat;
    }

    public static CSVFormat createCsvFormat() {
        CSVFormat format = new CSVFormat();
        format.csvReaderConfig = "{\n" +
                "        \"safetySwitch\": false,\n" +
                "        \"skipEmptyRecords\": false,\n" +
                "        \"useTextQualifier\": false\n" +
                "}";
        return format;
    }

    public static FTPServer createFtpServer() {
        FTPServer ftpServer = new FTPServer();
        ftpServer.protocol = "ftp";
        ftpServer.host = "192.168.28.201";
        ftpServer.port = 21;
        ftpServer.timeout = 33333;
        ftpServer.username = "test";
        ftpServer.password = "test";
        ftpServer.connectPattern = "PASV";
        return ftpServer;
    }
}
