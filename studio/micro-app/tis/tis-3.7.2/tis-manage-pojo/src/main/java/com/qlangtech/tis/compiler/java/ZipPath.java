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

package com.qlangtech.tis.compiler.java;

import org.apache.commons.lang.StringUtils;

import javax.tools.JavaFileObject;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-20 16:47
 **/
public class ZipPath {

    private final String parentPath;

    private final String entryName;

    private final JavaFileObject.Kind sourceKind;

    public ZipPath(String parentPath, String entryName, JavaFileObject.Kind sourceKind) {
        super();
        this.parentPath = parentPath;
        this.entryName = entryName;
        this.sourceKind = sourceKind;
    }

    public String getFullSourcePath() {
        // + JavaFileObject.Kind.CLASS.extension;
        StringBuffer result = new StringBuffer(getFullPath());
        if (sourceKind == JavaFileObject.Kind.CLASS) {
            result.append(JavaFileObject.Kind.CLASS.extension);
        } else if (sourceKind == JavaFileObject.Kind.SOURCE) {
            result.append(JavaFileObject.Kind.SOURCE.extension);
        } else if (sourceKind == JavaFileObject.Kind.OTHER) {
            result.append(".scala");
        } else {
            throw new IllegalStateException("source kind:" + this.sourceKind + " is illegal");
        }
        return result.toString();
    }

    public String getFullClassPath() {
        return getFullPath() + JavaFileObject.Kind.CLASS.extension;
    }

    public String getFullPath() {
        StringBuffer result = new StringBuffer(this.parentPath);
        if (!StringUtils.endsWith(parentPath, "/")) {
            result.append("/");
        }
        return result.append(this.entryName).toString();
    }

    public String getParentPath() {
        return this.parentPath;
    }

    public String getEntryName() {
        return this.entryName;
    }
}
