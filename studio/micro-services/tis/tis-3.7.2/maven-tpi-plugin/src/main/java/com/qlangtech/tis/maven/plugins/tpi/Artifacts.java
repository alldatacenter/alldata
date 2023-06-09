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
package com.qlangtech.tis.maven.plugins.tpi;

import com.google.common.base.Predicate;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import java.util.*;

/**
 * Collection filter operations on a set of {@link Artifact}s.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public class Artifacts extends ArrayList<Artifact> {

    public Artifacts() {
    }

    public Artifacts(Collection<? extends Artifact> c) {
        super(c);
    }

    /**
     * Return the {@link Artifact}s representing dependencies of the given project.
     *
     * A thin-wrapper of p.getArtifacts()
     */
    public static Artifacts of(MavenProject p) {
        return new Artifacts(p.getArtifacts());
    }

    public static Artifacts ofDirectDependencies(MavenProject p) {
        return new Artifacts(p.getDependencyArtifacts());
    }

    public Artifacts retainAll(Predicate<Artifact> filter) {
        for (Iterator<Artifact> itr = iterator(); itr.hasNext(); ) {
            if (!filter.apply(itr.next()))
                itr.remove();
        }
        return this;
    }

    public Artifacts removeAll(Predicate<Artifact> filter) {
        for (Iterator<Artifact> itr = iterator(); itr.hasNext(); ) {
            if (filter.apply(itr.next()))
                itr.remove();
        }
        return this;
    }

    public Artifacts scopeIs(String... scopes) {
        final List<String> s = Arrays.asList(scopes);
        return retainAll(new Predicate<Artifact>() {

            public boolean apply(Artifact a) {
                return s.contains(a.getScope());
            }
        });
    }

    public Artifacts scopeIsNot(String... scopes) {
        final List<String> s = Arrays.asList(scopes);
        return removeAll(new Predicate<Artifact>() {

            public boolean apply(Artifact a) {
                return s.contains(a.getScope());
            }
        });
    }

    public Artifacts typeIs(String... type) {
        final List<String> s = Arrays.asList(type);
        return retainAll(new Predicate<Artifact>() {

            public boolean apply(Artifact a) {
                return s.contains(a.getType());
            }
        });
    }

    public Artifacts typeIsNot(String... type) {
        final List<String> s = Arrays.asList(type);
        return removeAll(new Predicate<Artifact>() {

            public boolean apply(Artifact a) {
                return s.contains(a.getType());
            }
        });
    }

    public Artifacts groupIdIs(String... groupId) {
        final List<String> s = Arrays.asList(groupId);
        return retainAll(new Predicate<Artifact>() {

            public boolean apply(Artifact a) {
                return s.contains(a.getType());
            }
        });
    }

    public Artifacts groupIdIsNot(String... groupId) {
        final List<String> s = Arrays.asList(groupId);
        return removeAll(new Predicate<Artifact>() {

            public boolean apply(Artifact a) {
                return s.contains(a.getType());
            }
        });
    }

    public Artifacts artifactIdIs(String... artifactId) {
        final List<String> s = Arrays.asList(artifactId);
        return retainAll(new Predicate<Artifact>() {

            public boolean apply(Artifact a) {
                return s.contains(a.getType());
            }
        });
    }

    public Artifacts artifactIdIsNot(String... artifactId) {
        final List<String> s = Arrays.asList(artifactId);
        return removeAll(new Predicate<Artifact>() {

            public boolean apply(Artifact a) {
                return s.contains(a.getType());
            }
        });
    }
}
