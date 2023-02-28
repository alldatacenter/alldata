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
// */
package com.qlangtech.tis.runtime.module.action;

import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.DefaultFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public class HdfsAction {


    public static HttpServletResponse getDownloadResponse(String pathName) {
        HttpServletResponse response = (HttpServletResponse) DefaultFilter.getRespone();
        response.setContentType("application/text");
        response.addHeader("Content-Disposition", "attachment; filename=\"" + pathName + "\"");
        return response;
    }

    public static HttpServletResponse getDownloadResponse(File file, boolean hasContent) {
        HttpServletResponse response = (HttpServletResponse) DefaultFilter.getRespone();
        response.setContentType("application/text");
        response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
        response.addHeader(ConfigFileContext.KEY_HEAD_LAST_UPDATE, String.valueOf(file.lastModified()));
        response.addHeader(ConfigFileContext.KEY_HEAD_FILE_SIZE, String.valueOf(file.length()));
        response.addHeader(ConfigFileContext.KEY_HEAD_FILE_DOWNLOAD, String.valueOf(hasContent));
        return response;
    }

    public static HttpServletResponse getDownloadResponse(File file) {
        return getDownloadResponse(file, true);
    }
}
