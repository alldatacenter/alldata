/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.commons.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GzipUtils
 */
public class GzipUtils {

    public static final Logger LOG = LoggerFactory.getLogger(GzipUtils.class);

    /**
     * compress
     * 
     * @param  data
     * @return      byte array
     */
    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = null;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(data);
        } catch (IOException e) {
            LOG.error("compress data error", e);
        } finally {
            if (gzip != null) {
                try {
                    gzip.close();
                } catch (IOException e) {
                    LOG.error(GzipUtils.class.getSimpleName(), e);
                }
            }
        }

        return out.toByteArray();
    }

    /**
     * decompress
     * 
     * @param  bytes
     * @return       byte array
     */
    public static byte[] decompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (IOException e) {
            LOG.error("Decompress data error", e);
        }

        return out.toByteArray();
    }
}
