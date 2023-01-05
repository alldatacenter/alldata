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
package com.qlangtech.tis.config.module.action;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-01-05 17:39
 */
public class StubStreamHandlerFactory implements URLStreamHandlerFactory {
  public static URLStreamHandler streamHander;

  @Override
  public URLStreamHandler createURLStreamHandler(String protocol) {
    // System.out.println("=======================" + protocol);
    if ("http".equals(protocol)) {
      //return new StubHttpURLStreamHander();
      return streamHander;
    }
    return null;
  }

  public static class StubHttpURLStreamHander extends URLStreamHandler {
    @Override
    protected String toExternalForm(URL u) {
      return "mockURL"; //super.toExternalForm(u);
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
      return new StubHttpURLConnection(u);
    }

    @Override
    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
      return new StubHttpURLConnection(u);
    }
  }

  //
  private static class StubHttpURLConnection extends HttpURLConnection {
    public StubHttpURLConnection(URL u) {
      super(u);
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return super.getInputStream();
    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean usingProxy() {
      return false;
    }

    @Override
    public void connect() throws IOException {

    }
  }
}
