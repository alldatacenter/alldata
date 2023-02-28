/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

/**
 * @author by TGspace
 * @Classname FileUtil
 * @Description
 * @Date 2022/6/16 19:13
 */
public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static void downloadToResp(String filePath, HttpServletResponse response){
        InputStream fis = null;
        OutputStream outputStream = null;
        try {
            fis = new BufferedInputStream(new FileInputStream(filePath));
            String ext = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            outputStream = new BufferedOutputStream(response.getOutputStream());
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + UUID.randomUUID().toString().concat(".").concat(ext));
            outputStream.write(buffer);
            outputStream.flush();
        } catch (IOException ex) {
            logger.error("download error ", ex);
        }finally{
            try {
                if(null != fis){
                    fis.close();
                }
                if(null != outputStream){
                    outputStream.close();
                }
            } catch (IOException e) {
                logger.error("close stream error ", e);
            }
        }
    }
}
