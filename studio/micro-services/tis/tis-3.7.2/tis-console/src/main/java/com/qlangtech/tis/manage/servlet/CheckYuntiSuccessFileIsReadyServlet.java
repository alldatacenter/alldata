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
package com.qlangtech.tis.manage.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-11-1
 */
public class CheckYuntiSuccessFileIsReadyServlet extends BasicServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CheckYuntiSuccessFileIsReadyServlet.class);

    private static final Pattern PATTERN_HDFS_RESULT = Pattern.compile("2[0-9]{13}");

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        InputStream reader = null;
        final String yuntipath = req.getParameter("yuntipath");
        Assert.assertNotNull("parameter yuntipath can not be null" + yuntipath);
        try {
            final String execCommand = "/home/admin/hadoop-0.19.1/bin/hadoop fs -cat " + yuntipath;
            Process process = Runtime.getRuntime().exec(execCommand);
            reader = process.getInputStream();
            LineIterator lineiterator = IOUtils.lineIterator(reader, "utf8");
            final String hdfsExecuteResult = lineiterator.nextLine();
            log.info("command " + execCommand + " result first line:" + hdfsExecuteResult);
            final PrintWriter writer = resp.getWriter();
            Matcher matcher = null;
            if ((matcher = PATTERN_HDFS_RESULT.matcher(StringUtils.trimToEmpty(hdfsExecuteResult))).matches()) {
                log.info("matched");
                // return true;
                writer.println(String.valueOf(true));
            } else {
                writer.println(String.valueOf(false));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
