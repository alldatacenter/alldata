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

package com.aliyun.oss.common.comm;

import java.io.InputStream;
import java.util.zip.CheckedInputStream;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.CRC64;

public class ResponseChecksumHandler implements ResponseHandler {

    @Override
    public void handle(ResponseMessage response) throws OSSException, ClientException {
        InputStream originalInputStream = response.getContent();
        if (originalInputStream == null) {
            return;
        }

        CRC64 crc = new CRC64();
        CheckedInputStream checkedInputstream = new CheckedInputStream(originalInputStream, crc);
        response.setContent(checkedInputstream);
    }

}
