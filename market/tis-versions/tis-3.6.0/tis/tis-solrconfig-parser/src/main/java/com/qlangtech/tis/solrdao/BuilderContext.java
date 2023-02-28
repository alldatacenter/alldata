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
package com.qlangtech.tis.solrdao;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-6-7
 */
public class BuilderContext implements IBuilderContext {

    private String appName;

    private String pojoName;

    private String targetNameSpace;

    private String serverAddress;

    protected String targetDir;

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getAppName() {
        return this.appName;
    }

    public File getTargetPackageDir() {
        return new File(new File(this.getTargetDir()), StringUtils.replace(this.targetNameSpace, ".", "/"));
    }

    public String getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public String getTargetNameSpace() {
        return targetNameSpace;
    }

    public void setTargetNameSpace(String targetNameSpace) {
        this.targetNameSpace = targetNameSpace;
    }

    public String getPojoName() {
        if (StringUtils.isEmpty(this.pojoName)) {
            return StringUtils.capitalize(StringUtils.substringAfter(appName, "search4"));
        }
        return pojoName;
    }

    public void setPojoName(String pojoName) {
        this.pojoName = pojoName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public byte[] getResourceInputStream() {
        throw new UnsupportedOperationException();
    }

    // public InputStream getResourceInputStream() throws Exception {
    // URL url = new URL("http://" + getServerAddress() + ":8080/terminator-search/" + getAppName() + "-0/admin/file/?contentType=text/xml;charset=utf-8&file=schema.xml");
    // System.out.println(url);
    // InputStream reader = url.openStream();
    // return reader;
    // }
    /*
	 * (non-Javadoc)
	 *
	 * @see com.taobao.terminator.solrdao.IBuilderContext#getOutputStream()
	 */
    public Writer getOutputStream() throws Exception {
        return new OutputStreamWriter(FileUtils.openOutputStream(this.getNewFileName(), false));
    }

    private File getNewFileName() {
        File outputDir = this.getTargetPackageDir();
        return new File(outputDir, this.getPojoName() + ".java");
    }

    public void closeWriter(PrintWriter writer) {
        IOUtils.closeQuietly(writer);
    }
}
