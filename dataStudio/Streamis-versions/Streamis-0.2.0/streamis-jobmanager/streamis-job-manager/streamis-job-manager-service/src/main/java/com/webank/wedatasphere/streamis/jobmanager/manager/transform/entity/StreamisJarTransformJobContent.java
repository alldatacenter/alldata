/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity;

import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamisFile;
import java.util.List;

/**
 * Created by enjoyyin on 2021/9/23.
 */
public class StreamisJarTransformJobContent implements StreamisTransformJobContent {

    private StreamisFile mainClassJar;
    private String mainClass;
    private List<String> args;

    private List<StreamisFile> dependencyJars;
    private List<String> hdfsJars;
    private List<StreamisFile> resources;

    public StreamisFile getMainClassJar() {
        return mainClassJar;
    }

    public void setMainClassJar(StreamisFile mainClassJar) {
        this.mainClassJar = mainClassJar;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public List<StreamisFile> getDependencyJars() {
        return dependencyJars;
    }

    public void setDependencyJars(List<StreamisFile> dependencyJars) {
        this.dependencyJars = dependencyJars;
    }

    public List<String> getHdfsJars() {
        return hdfsJars;
    }

    public void setHdfsJars(List<String> hdfsJars) {
        this.hdfsJars = hdfsJars;
    }

    public List<StreamisFile> getResources() {
        return resources;
    }

    public void setResources(List<StreamisFile> resources) {
        this.resources = resources;
    }
}
