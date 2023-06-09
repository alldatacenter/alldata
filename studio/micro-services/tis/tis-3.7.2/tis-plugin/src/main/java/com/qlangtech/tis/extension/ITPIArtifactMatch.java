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

package com.qlangtech.tis.extension;

import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-22 11:30
 **/
public class ITPIArtifactMatch implements ITPIArtifact {
    private Optional<PluginClassifier> classifier;
    private String identityName;

    private final String requiredFrom;

    ITPIArtifactMatch(String requiredFrom, Optional<PluginClassifier> classifier) {
        this.classifier = classifier;
        this.requiredFrom = requiredFrom;
    }

    public String getRequiredFrom() {
        return this.requiredFrom;
    }

    @Override
    public String getIdentityName() {
        if (StringUtils.isEmpty(this.identityName)) {
            throw new IllegalStateException("prop identityName can not be null");
        }
        return this.identityName;
    }

    public ITPIArtifactMatch setIdentityName(String identityName) {
        this.identityName = identityName;
        return this;
    }

    @Override
    public String toString() {
        return "{" +
                "classifier=" + (classifier.isPresent() ? classifier.get().getClassifier() : "not Present") +
                ", identityName='" + identityName + '\'' +
                ", requiredFrom='" + requiredFrom + '\'' +
                '}';
    }

    @Override
    public Optional<PluginClassifier> getClassifier() {
        return this.classifier;
    }

    public boolean match(PluginClassifier candidateClassifier) {
        if (!classifier.isPresent()) {
            throw new IllegalStateException("plugin:" + this.requiredFrom + " classifier must be present");
        }
        PluginClassifier c = classifier.get();
        return c.match(requiredFrom, candidateClassifier);

    }
}
