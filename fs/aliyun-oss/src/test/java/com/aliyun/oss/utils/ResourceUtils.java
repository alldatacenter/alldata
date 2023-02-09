/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.utils.CodingUtils;

public class ResourceUtils {

    public static ResponseMessage loadResponseFromResource(String resourceName)
            throws FileNotFoundException {
        ResponseMessage response = new ResponseMessage(null);
        if (resourceName != null) {
            String filename = getTestFilename(resourceName);
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            response.setContent(fis);
            response.setContentLength(file.length());
        }

        return response;
    }

    public static InputStream getTestInputStream(String resourceName)
            throws FileNotFoundException {
        assert (!CodingUtils.isNullOrEmpty(resourceName));

        return new FileInputStream(getTestFilename(resourceName));
    }

    public static String getTestFilename(String resourceName) {
        assert (!CodingUtils.isNullOrEmpty(resourceName));

        return ResourceUtils.class.getClassLoader().getResource(resourceName).getFile();
    }

}
