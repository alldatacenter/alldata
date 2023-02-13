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
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-22 10:06
 **/
public interface ITPIArtifact {

    static ITPIArtifactMatch matchh(String requiredFrom, Optional<PluginClassifier> classifier) {
        return new ITPIArtifactMatch(requiredFrom, classifier);
    }

    static ITPIArtifactMatch matchh(ITPIArtifact art) {
        ITPIArtifactMatch match = matchh(null, art.getClassifier());
        return match.setIdentityName(art.getIdentityName());
    }

    static void matchDependency(PluginManager pluginManager, List<PluginWrapper.Dependency> dependencies
            , PluginWrapper pw, Consumer<Pair<PluginWrapper, PluginWrapper.Dependency>> pluginConsumer
            , Consumer<Pair<PluginWrapper.Dependency, ITPIArtifactMatch>>... missConsumer) {
        matchDependency(pluginManager, dependencies, pw.getShortName(), pw.getClassifier(), pluginConsumer, missConsumer);
    }


    static void matchDependency(PluginManager pluginManager, List<PluginWrapper.Dependency> dependencies
            , String requiredFrom, Optional<PluginClassifier> classifier, Consumer<Pair<PluginWrapper, PluginWrapper.Dependency>> pluginConsumer
            , Consumer<Pair<PluginWrapper.Dependency, ITPIArtifactMatch>>... missConsumer) {
        //Optional<PluginClassifier> classifier = pw.getClassifier();
        ITPIArtifactMatch match = ITPIArtifact.matchh(requiredFrom, classifier);
        for (PluginWrapper.Dependency d : dependencies) {
            match.setIdentityName(d.shortName);
            PluginWrapper p = pluginManager.getPlugin(match);
            if (p != null) {
                pluginConsumer.accept(Pair.of(p, d));
            } else {
                for (Consumer<Pair<PluginWrapper.Dependency, ITPIArtifactMatch>> c : missConsumer) {
                    c.accept(Pair.of(d, match));
                }
            }
        }
    }

    public static ITPIArtifactMatch create(final String idName) {
//        return new ITPIArtifact() {
//            @Override
//            public String getIdentityName() {
//                return idName;
//            }
//        };
//        ITPIArtifactMatch match = matchh("", Optional.empty());
//        match.setIdentityName(idName);
//        return match;
        return create(idName, Optional.empty());
    }


    public static ITPIArtifactMatch create(final String idName, Optional<PluginClassifier> classifier) {
//        return new ITPIArtifact() {
//            @Override
//            public String getIdentityName() {
//                return idName;
//            }
//        };
        if (StringUtils.isEmpty(idName)) {
            throw new IllegalArgumentException("param idName can not be empty");
        }
        ITPIArtifactMatch match = matchh("", classifier);
        match.setIdentityName(idName);
        return match;
    }

    public static boolean isEquals(ITPIArtifact dptCandidate, ITPIArtifactMatch b) {
        if (!StringUtils.equals(dptCandidate.getIdentityName(), b.getIdentityName())) {
            return false;
        }
        Optional<PluginClassifier> ca = dptCandidate.getClassifier();
        Optional<PluginClassifier> ba = b.getClassifier();

        PluginClassifier.validate(b.getRequiredFrom(), b.getIdentityName(), ca, ba);

//        if (ca.isPresent() ^ ba.isPresent()) {
//            if (ca.isPresent()) {
//                throw new IllegalStateException("required from " + b.getRequiredFrom() + " for " + b.getIdentityName()
//                        + " of Candidate classifier present status:"
//                        + ca.isPresent() + " must same with Owner classifier prestent status:" + ba.isPresent());
//            }
//        }

        if (ca.isPresent()) {
            PluginClassifier candidateClassifier = ca.get();
            // PluginClassifier classifier = ba.get();
            return b.match(candidateClassifier);
            // return classifier.match(candidateClassifier);
        }

        return true;
    }

    public String getIdentityName();

    public default Optional<PluginClassifier> getClassifier() {
        return Optional.empty();
    }
}
