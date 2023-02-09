/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;

import okhttp3.Response;

public class HttpMethodReleaseInputStream extends InputStream implements InputStreamWrapper {
    private static final ILogger log = LoggerBuilder.getLogger(HttpMethodReleaseInputStream.class);
    private InputStream inputStream = null;
    private Response httpResponse = null;
    private boolean flag = false;
    private boolean comsumed = false;

    public HttpMethodReleaseInputStream(Response response) {
        this.httpResponse = response;
        try {
            this.inputStream = new InterruptableInputStream(response.body().byteStream());
        } catch (Exception e) {
            try {
                response.close();
            } catch (Exception ee) {
                log.warn("io close failed.", ee);
            }
            this.inputStream = new ByteArrayInputStream(new byte[] {});
        }
    }

    public Response getHttpResponse() {
        return httpResponse;
    }

    protected void closeConnection() throws IOException {
        if (!flag) {
            if (!comsumed && httpResponse != null) {
                httpResponse.close();
            }
            flag = true;
        }
    }

    @Override
    public int read() throws IOException {
        try {
            int read = inputStream.read();
            if (read == -1) {
                comsumed = true;
                if (!flag) {
                    closeConnection();
                }
            }
            return read;
        } catch (IOException e) {
            try {
                closeConnection();
            } catch (IOException ignored) {
                log.warn("io close failed.", ignored);
            }
            throw e;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            int read = inputStream.read(b, off, len);
            if (read == -1) {
                comsumed = true;
                if (!flag) {
                    closeConnection();
                }
            }
            return read;
        } catch (IOException e) {
            try {
                closeConnection();
            } catch (IOException ignored) {
                log.warn("io close failed.", ignored);
            }
            throw e;
        }
    }

    @Override
    public int available() throws IOException {
        try {
            return inputStream.available();
        } catch (IOException e) {
            try {
                closeConnection();
            } catch (IOException ignored) {
                log.warn("io close failed.", ignored);
            }
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (!flag) {
            closeConnection();
        }
        inputStream.close();
    }

    public InputStream getWrappedInputStream() {
        return inputStream;
    }

}
