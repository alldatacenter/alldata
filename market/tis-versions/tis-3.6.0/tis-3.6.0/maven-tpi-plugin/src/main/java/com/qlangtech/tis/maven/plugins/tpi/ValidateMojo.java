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

import com.qlangtech.tis.manage.common.Config;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.util.Optional;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-23 10:21
 **/
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidateMojo extends AbstractJenkinsMojo {

    @Override
    public void execute() throws MojoExecutionException {
        this.getLog().info("start to validate tpi");
        Optional<PluginClassifier> classifier = TpiMojo.getPluginClassifier(this.project);
        Optional<PluginClassifier> dptClassifier = null;
        MavenArtifact tpiDep = null;
        MavenProject mavenProject = null;
        Set<Artifact> depedencies = this.project.getDependencyArtifacts();
        // this.project.getDependencies();
        try {
            for (Artifact art : depedencies) {

                if (!StringUtils.startsWith(art.getGroupId(), Config.QLANGTECH_PACKAGE)) {
                    continue;
                }
                //if (PluginClassifier.PACAKGE_TPI_EXTENSION_NAME.equals(art.getType())) {
                tpiDep = this.wrap(art);
                mavenProject = tpiDep.resolvePom();
                if (!PluginClassifier.PACAKGE_TPI_EXTENSION_NAME.equals(mavenProject.getPackaging())) {
                    continue;
                }

                dptClassifier = TpiMojo.getPluginClassifier(mavenProject);

                PluginClassifier.validate(this.project.getArtifactId(), art.getArtifactId(), dptClassifier, classifier);

                if (classifier.isPresent()
                        && dptClassifier.isPresent()) {
                    if (!classifier.get().match(this.project.getArtifactId(), dptClassifier.get())) {
                        throw new MojoExecutionException("plugin:" + this.project.getArtifactId()
                                + "[" + classifier.get().getClassifier() + "] dependency for "
                                + art.getArtifactId() + "[" + dptClassifier.get().getClassifier() + "] is not match ");
                    }
                }
                //}
            }
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }


//        JavaSpecificationVersion javaVersion = getMinimumJavaVersion();
//        if (JavaSpecificationVersion.forCurrentJVM().isOlderThan(javaVersion)) {
//            throw new MojoExecutionException("Java " + javaVersion + " or later is necessary to build this plugin.");
//        }
//
//        if (new VersionNumber(findJenkinsVersion()).compareTo(new VersionNumber("2.204")) < 0) {
//            throw new MojoExecutionException("This version of maven-hpi-plugin requires Jenkins 2.204 or later");
//        }
//
//        MavenProject parent = project.getParent();
//        if (parent != null
//                && parent.getGroupId().equals("org.jenkins-ci.plugins")
//                && parent.getArtifactId().equals("plugin")
//                && !parent.getProperties().containsKey("java.level")
//                && project.getProperties().containsKey("java.level")) {
//            getLog().warn("Ignoring deprecated java.level property."
//                    + " This property should be removed from your plugin's POM."
//                    + " In the future this warning will be changed to an error and will break the build.");
//        }
    }
}
