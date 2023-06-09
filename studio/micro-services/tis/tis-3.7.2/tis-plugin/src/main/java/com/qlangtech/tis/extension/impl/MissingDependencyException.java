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
package com.qlangtech.tis.extension.impl;

import com.qlangtech.tis.extension.ITPIArtifactMatch;
import com.qlangtech.tis.extension.PluginWrapper;
import com.qlangtech.tis.util.Util;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception thrown if plugin resolution fails due to missing dependencies
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class MissingDependencyException extends IOException {

    private String pluginShortName;

    private List<Pair<PluginWrapper.Dependency, ITPIArtifactMatch>> missingDependencies;

    public MissingDependencyException(String pluginShortName, List<Pair<PluginWrapper.Dependency, ITPIArtifactMatch>> missingDependencies) {
        super("One or more dependencies could not be resolved for " + pluginShortName
                + " : " + Util.join(missingDependencies.stream().map((p) -> {
            return p.getLeft().toString() + ",matcher:" + p.getRight();
        }).collect(Collectors.toList()), ", "));
        this.pluginShortName = pluginShortName;
        this.missingDependencies = missingDependencies;
    }

    public List<PluginWrapper.Dependency> getMissingDependencies() {
        return missingDependencies.stream().map((p) -> p.getLeft()).collect(Collectors.toList());
    }

    public String getPluginShortName() {
        return pluginShortName;
    }
}
