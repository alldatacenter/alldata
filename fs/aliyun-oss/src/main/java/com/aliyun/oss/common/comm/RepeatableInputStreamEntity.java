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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.BasicHttpEntity;

import com.aliyun.oss.common.utils.HttpHeaders;

public class RepeatableInputStreamEntity extends BasicHttpEntity {

    private boolean firstAttempt = true;

    private NoAutoClosedInputStreamEntity innerEntity;

    private InputStream content;

    public RepeatableInputStreamEntity(ServiceClient.Request request) {
        setChunked(false);

        String contentType = request.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        content = request.getContent();
        long contentLength = request.getContentLength();

        innerEntity = new NoAutoClosedInputStreamEntity(content, contentLength);
        innerEntity.setContentType(contentType);

        setContent(content);
        setContentType(contentType);
        setContentLength(request.getContentLength());
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    @Override
    public boolean isRepeatable() {
        return content.markSupported() || innerEntity.isRepeatable();
    }

    @Override
    public void writeTo(OutputStream output) throws IOException {
        if (!firstAttempt && isRepeatable()) {
            content.reset();
        }

        firstAttempt = false;
        innerEntity.writeTo(output);
    }

    /**
     * The default entity org.apache.http.entity.InputStreamEntity will close
     * input stream after wirteTo was called. To avoid this, we custom a entity
     * that will not close stream automatically.
     * 
     * @author chao.wangchaowc
     */
    public static class NoAutoClosedInputStreamEntity extends AbstractHttpEntity {
        private final static int BUFFER_SIZE = 2048;

        private final InputStream content;
        private final long length;

        public NoAutoClosedInputStreamEntity(final InputStream instream, long length) {
            super();
            if (instream == null) {
                throw new IllegalArgumentException("Source input stream may not be null");
            }
            this.content = instream;
            this.length = length;
        }

        public boolean isRepeatable() {
            return false;
        }

        public long getContentLength() {
            return this.length;
        }

        public InputStream getContent() throws IOException {
            return this.content;
        }

        public void writeTo(final OutputStream outstream) throws IOException {
            if (outstream == null) {
                throw new IllegalArgumentException("Output stream may not be null");
            }
            InputStream instream = this.content;

            byte[] buffer = new byte[BUFFER_SIZE];
            int l;
            if (this.length < 0) {
                // consume until EOF
                while ((l = instream.read(buffer)) != -1) {
                    outstream.write(buffer, 0, l);
                }
            } else {
                // consume no more than length
                long remaining = this.length;
                while (remaining > 0) {
                    l = instream.read(buffer, 0, (int) Math.min(BUFFER_SIZE, remaining));
                    if (l == -1) {
                        break;
                    }
                    outstream.write(buffer, 0, l);
                    remaining -= l;
                }
            }

        }

        public boolean isStreaming() {
            return true;
        }
    }
}
