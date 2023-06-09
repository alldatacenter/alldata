/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.maven.plugins.tpi;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarFile;

import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;

/**
 * {@link Artifact} is a bare data structure without any behavior and therefore
 * hard to write OO programs around it.
 * <p>
 * This class wraps {@link Artifact} and adds behaviours.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public class MavenArtifact implements Comparable<MavenArtifact> {

    public final ArtifactFactory artifactFactory;

    public final MavenProjectBuilder builder;

    public final List<ArtifactRepository> remoteRepositories;

    public final ArtifactRepository localRepository;

    public final Artifact artifact;

    public final ArtifactResolver resolver;

    public MavenArtifact(Artifact artifact, ArtifactResolver resolver
            , ArtifactFactory artifactFactory, MavenProjectBuilder builder, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository) {
        this.artifact = artifact;
        this.resolver = resolver;
        this.artifactFactory = artifactFactory;
        this.builder = builder;
        this.remoteRepositories = remoteRepositories;
        // null check
        remoteRepositories.size();
        this.localRepository = localRepository;
    }

    public MavenProject resolvePom() throws ProjectBuildingException {
        return builder.buildFromRepository(artifact, remoteRepositories, localRepository);
    }

    /**
     * Is this a Jenkins plugin?
     */
    public boolean isPlugin() {
        try {
            String type = getResolvedType();
            return type.equals(PluginClassifier.PACAKGE_TPI_EXTENSION_NAME) || type.equals("jpi");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Like {@link #isPlugin} but will not throw an exception if the project model cannot be resolved.
     * Helpful for example when an indirect dependency has a bogus {@code systemPath} that is only rejected in some environments.
     */
    public boolean isPluginBestEffort(Log log) {
        //   try {
        return isPlugin();
//        } catch (IOException x) {
//            if (log.isDebugEnabled()) {
//                log.debug(x);
//            } else {
//                log.warn(x.getCause().getMessage());
//            }
//            return false;
//        }
    }

    public String getId() {
        return artifact.getId();
    }

    /**
     * Converts the filename of an artifact to artifactId-version.type format.
     *
     * @return converted filename of the artifact
     */
    public String getDefaultFinalName() {
        return artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getArtifactHandler().getExtension();
    }

    public boolean isOptional() {
        return artifact.isOptional();
    }

    public String getType() {
        return artifact.getType();
    }

    /**
     * Resolves to the jar file that contains the code of the plugin.
     */
    public File getFile() {
        if (artifact.getFile() == null)
            try {
                resolver.resolve(artifact, remoteRepositories, localRepository);
            } catch (AbstractArtifactResolutionException e) {
                throw new RuntimeException("Failed to resolve " + getId(), e);
            }
        return artifact.getFile();
    }

    /**
     * Returns {@link MavenArtifact} for the hpi variant of this artifact.
     */
    public MavenArtifact getHpi() throws IOException {
        Artifact a = artifactFactory.createArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), SCOPE_COMPILE, getResolvedType());
        return new MavenArtifact(a, resolver, artifactFactory, builder, remoteRepositories, localRepository);
    }

    public List<String> getDependencyTrail() {
        return artifact.getDependencyTrail();
    }

    public String getGroupId() {
        return artifact.getGroupId();
    }

    public String getScope() {
        return artifact.getScope();
    }

    /**
     * Returns true if the artifacts has one of the given scopes (including null.)
     */
    public boolean hasScope(String... scopes) {
        for (String s : scopes) {
            if (s == null && artifact.getScope() == null)
                return true;
            if (s != null && s.equals(artifact.getScope()))
                return true;
        }
        return false;
    }

    /**
     * Gets the {@code artifactId} as used in a dependency declaration. For a plugin artifact this need not be be a plugin short name.
     */
    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    /**
     * Gets the {@code version} as used in a dependency declaration. For a plugin artifact this need not be be a plugin version number.
     */
    public String getVersion() {
        return artifact.getVersion();
    }

    public String getClassifier() {
        return artifact.getClassifier();
    }

    /**
     * For a plugin artifact, unlike {@link #getArtifactId} this parses the plugin manifest.
     */
    public String getActualArtifactId() throws IOException {
        File file = getFile();
        if (file != null && file.isFile()) {
            try (JarFile jf = new JarFile(file)) {
                return jf.getManifest().getMainAttributes().getValue("Short-Name");
            }
        } else {
            return getArtifactId();
        }
    }

    /**
     * For a plugin artifact, unlike {@link #getVersion} this parses the plugin manifest.
     */
    public String getActualVersion() throws IOException {
        File file = getFile();
        if (file != null && file.isFile()) {
            try (JarFile jf = new JarFile(file)) {
                // e.g. " (private-abcd1234-username)"; Implementation-Version is clean but seems less portable
                return jf.getManifest().getMainAttributes().getValue("Plugin-Version").replaceFirst(" [(].+[)]$", "");
            }
        } else {
            return getVersion();
        }
    }

    public ArtifactVersion getVersionNumber() throws OverConstrainedVersionException {
        return artifact.getSelectedVersion();
    }

    /**
     * Returns true if this artifact has the same groupId and artifactId as the given project.
     */
    public boolean hasSameGAAs(MavenProject project) {
        return getGroupId().equals(project.getGroupId()) && getArtifactId().equals(project.getArtifactId());
    }

    public boolean isNewerThan(MavenArtifact rhs) {
        // return new VersionNumber(this.getVersion()).compareTo(new VersionNumber(rhs.getVersion())) > 0;
        return true;
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public int compareTo(MavenArtifact o) {
        return getId().compareTo(o.getId());
    }

    /**
     * Tries to detect the original packaging type by eventually resolving the POM.
     * This is necessary when a plugin depends on another plugin and it doesn't specify the type as hpi or jpi.
     */
    private String getResolvedType() throws IOException {
        try {
            String type = artifact.getType();
            if ("provided".equals(artifact.getScope())) {
                return type;
            }
            // only resolve the POM if the packaging type is jar, because that's the default if no type has been specified
            if (!type.equals("jar")) {
                return type;
            }
            // also ignore core-assets, tests, etc.
            if (!StringUtils.isEmpty(artifact.getClassifier())) {
                return type;
            }
            // when a plugin depends on another plugin, it doesn't specify the type as hpi or jpi, so we need to resolve its POM to see it
            return resolvePom().getPackaging();
        } catch (ProjectBuildingException e) {
            throw new IOException("Failed to open artifact " + artifact.toString() + " at " + artifact.getFile() + ": " + e, e);
        }
    }
}
