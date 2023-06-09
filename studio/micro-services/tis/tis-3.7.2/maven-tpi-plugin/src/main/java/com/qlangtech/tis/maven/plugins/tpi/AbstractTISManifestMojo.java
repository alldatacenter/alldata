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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.IOUtil;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class for Mojo implementations, which produce Jenkins-style manifests.
 * The Mojo may be used to not only package plugins, but also JAR files like Jenkins modules
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 * @see TpiMojo
 * @since 3.0
 */
public abstract class AbstractTISManifestMojo extends AbstractHpiMojo {

    private static final Logger LOGGER = Logger.getLogger(AbstractTISManifestMojo.class.getName());

    /**
     * Optional - the oldest version of this plugin which the current version is
     * configuration-compatible with.
     */
    @Parameter(property = "hpi.compatibleSinceVersion")
    private String compatibleSinceVersion;

    /**
     * Optional - sandbox status of this plugin.
     */
    @Parameter
    private String sandboxStatus;

    /**
     * Specify the minimum version of Java that this plugin requires.
     */
    @Parameter
    protected String minimumJavaVersion = "8";

    /**
     * Generates a manifest file to be included in the .hpi file
     */
    protected void generateManifest(MavenArchiveConfiguration archive, File manifestFile) throws MojoExecutionException {
        // create directory if it doesn't exist yet
        if (!manifestFile.getParentFile().exists())
            manifestFile.getParentFile().mkdirs();
        getLog().info("Generating " + manifestFile);
        MavenArchiver ma = new MavenArchiver();
        ma.setOutputFile(manifestFile);
        PrintWriter printWriter = null;
        try {
            Manifest mf = ma.getManifest(project, archive.getManifest());
            Manifest.Section mainSection = mf.getMainSection();
            setAttributes(mainSection);
            printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(manifestFile), "UTF-8"));
            mf.write(printWriter);
        } catch (ManifestException e) {
            throw new MojoExecutionException("Error preparing the manifest: " + e.getMessage(), e);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error preparing the manifest: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error preparing the manifest: " + e.getMessage(), e);
        } finally {
            IOUtil.close(printWriter);
        }
    }

    protected void setAttributes(Manifest.Section mainSection) throws MojoExecutionException, ManifestException, IOException {
        File pluginImpl = new File(project.getBuild().getOutputDirectory(), "META-INF/services/hudson.Plugin");
        if (pluginImpl.exists()) {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(pluginImpl), "UTF-8"));
            String pluginClassName = in.readLine();
            in.close();
            mainSection.addAttributeAndCheck(new Manifest.Attribute("Plugin-Class", pluginClassName));
        }
        mainSection.addAttributeAndCheck(new Manifest.Attribute("Group-Id", project.getGroupId()));
        mainSection.addAttributeAndCheck(new Manifest.Attribute("Short-Name", project.getArtifactId()));
        mainSection.addAttributeAndCheck(new Manifest.Attribute("Long-Name", pluginName));
        mainSection.addAttributeAndCheck(new Manifest.Attribute("Last-Modify-Time", String.valueOf((new Date()).getTime())));


        Optional<PluginClassifier> pluginClassifier = TpiMojo.getPluginClassifier(this.project);
        if (pluginClassifier.isPresent()) {
            mainSection.addAttributeAndCheck(
                    new Manifest.Attribute("classifier", pluginClassifier.get().getClassifier()));
        }


        String url = project.getUrl();
        if (url != null) {
            mainSection.addAttributeAndCheck(new Manifest.Attribute("Url", url));
        }
        if (compatibleSinceVersion != null) {
            mainSection.addAttributeAndCheck(new Manifest.Attribute("Compatible-Since-Version", compatibleSinceVersion));
        }
        if (this.minimumJavaVersion == null) {
            throw new MojoExecutionException("minimumJavaVersion attribute must be set starting from version 2.8");
        }
        try {
            int res = Integer.parseInt(this.minimumJavaVersion);
            LOGGER.log(Level.INFO, "Minimum Java version for the plugin: {0}", this.minimumJavaVersion);
        } catch (NumberFormatException ex) {
            if (this.minimumJavaVersion.equals("1.6") || this.minimumJavaVersion.equals("1.7") || this.minimumJavaVersion.equals("1.8")) {
                // okay
            } else {
                throw new MojoExecutionException("Unsupported Java version string: `" + this.minimumJavaVersion + "`. If you use Java 9 or above, see https://openjdk.java.net/jeps/223");
            }
        }
        mainSection.addAttributeAndCheck(new Manifest.Attribute("Minimum-Java-Version", this.minimumJavaVersion));
        if (sandboxStatus != null)
            mainSection.addAttributeAndCheck(new Manifest.Attribute("Sandbox-Status", sandboxStatus));
        String v = project.getVersion();
        if (v.endsWith("-SNAPSHOT") && snapshotPluginVersionOverride != null) {
            String nonSnapshotVersion = v.substring(0, v.length() - "-SNAPSHOT".length());
            if (!snapshotPluginVersionOverride.startsWith(nonSnapshotVersion)) {
                String message = "The snapshotPluginVersionOverride of " + snapshotPluginVersionOverride + " does not start with the current target release version " + v;
                // but we provide this escape hatch just in case.
                if (failOnVersionOverrideToDifferentRelease) {
                    throw new MojoExecutionException(message);
                }
                getLog().warn(message);
            }
            getLog().info("Snapshot Plugin Version Override enabled. Using " + snapshotPluginVersionOverride + " in place of " + v);
            v = snapshotPluginVersionOverride;
        }
        if (v.endsWith("-SNAPSHOT") && pluginVersionDescription == null) {
            String dt = getGitHeadSha1();
            if (// if SHA1 isn't available, fall back to timestamp
                    dt == null)
                dt = new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date());
            pluginVersionDescription = "private-" + dt + "-" + System.getProperty("user.name");
        }
        if (pluginVersionDescription != null)
            v += " (" + pluginVersionDescription + ")";
        if (!project.getPackaging().equals("jenkins-module")) {
            // Earlier maven-hpi-plugin used to look for this attribute to determine if a jar file is a Jenkins plugin.
            // While that's fixed, people out there might be still using it, so as a precaution when building a module
            // don't put this information in there.
            // The "Implementation-Version" baked by Maven should serve the same purpose if someone needs to know the version.
            mainSection.addAttributeAndCheck(new Manifest.Attribute("Plugin-Version", v));
        }
        String jv = findTISVersion();
        mainSection.addAttributeAndCheck(new Manifest.Attribute("tis-Version", jv));
        if (maskClasses != null)
            mainSection.addAttributeAndCheck(new Manifest.Attribute("Mask-Classes", maskClasses));
        if (globalMaskClasses != null)
            mainSection.addAttributeAndCheck(new Manifest.Attribute("Global-Mask-Classes", globalMaskClasses));
        if (pluginFirstClassLoader)
            mainSection.addAttributeAndCheck(new Manifest.Attribute("PluginFirstClassLoader", "true"));
        String dep = findDependencyPlugins();
        if (dep.length() > 0)
            mainSection.addAttributeAndCheck(new Manifest.Attribute("Plugin-Dependencies", dep));
        if (project.getDevelopers() != null) {
            mainSection.addAttributeAndCheck(new Manifest.Attribute("Plugin-Developers", getDevelopersForManifest()));
        }
        Boolean b = isSupportDynamicLoading();
        if (b != null)
            mainSection.addAttributeAndCheck(new Manifest.Attribute("Support-Dynamic-Loading", b.toString()));
        // Extra info attributes
        addLicenseAttributesForManifest(mainSection);
        addPropertyAttributeIfNotNull(mainSection, "Plugin-ChangelogUrl", "hpi.pluginChangelogUrl");
        addPropertyAttributeIfNotNull(mainSection, "Plugin-LogoUrl", "hpi.pluginLogoUrl");
        addAttributeIfNotNull(mainSection, "Plugin-ScmUrl", getScmUrl());
    }

    /**
     * Finds and lists dependency plugins.
     */
    private String findDependencyPlugins() throws IOException, MojoExecutionException {
        StringBuilder buf = new StringBuilder();
        for (MavenArtifact a : getDirectDependencyArtfacts()) {
            if (a.isPlugin() && scopeFilter.include(a.artifact) && !a.hasSameGAAs(project)) {
                if (buf.length() > 0)
                    buf.append(',');
                buf.append(a.getActualArtifactId());
                buf.append(':');
                buf.append(a.getActualVersion());
                if (a.isOptional()) {
                    buf.append(";resolution:=optional");
                }
            }
        }
        // see http://jenkins-ci.361315.n4.nabble.com/Classloading-problem-when-referencing-classes-from-another-plugin-during-the-initialization-phase-of-td394967.html
        for (Artifact a : (Collection<Artifact>) project.getDependencyArtifacts())
            if ("provided".equals(a.getScope()) && wrap(a).isPlugin())
                throw new MojoExecutionException(a.getId() + " is marked as 'provided' scope dependency, but it should be the 'compile' scope.");
        return buf.toString();
    }

    /**
     * Finds and lists developers specified in POM.
     */
    private String getDevelopersForManifest() throws IOException {
        StringBuilder buf = new StringBuilder();
        for (Object o : project.getDevelopers()) {
            Developer d = (Developer) o;
            if (buf.length() > 0) {
                buf.append(',');
            }
            buf.append(d.getName() != null ? d.getName() : "");
            buf.append(':');
            buf.append(d.getId() != null ? d.getId() : "");
            buf.append(':');
            buf.append(d.getEmail() != null ? d.getEmail() : "");
        }
        return buf.toString();
    }

    protected Manifest loadManifest(File f) throws IOException, ManifestException {
        InputStreamReader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
        try {
            return new Manifest(r);
        } finally {
            IOUtil.close(r);
        }
    }

    private void addLicenseAttributesForManifest(Manifest.Section target) throws ManifestException {
        final List<License> licenses = project.getLicenses();
        int licenseCounter = 1;
        for (License lic : licenses) {
            String licenseSuffix = licenseCounter == 1 ? "" : ("-" + licenseCounter);
            addAttributeIfNotNull(target, "Plugin-License-Name" + licenseSuffix, lic.getName());
            addAttributeIfNotNull(target, "Plugin-License-Url" + licenseSuffix, lic.getUrl());
            // TODO(oleg_nenashev): Can be enabled later if needed
            // addAttributeIfNotNull(target, "Plugin-License-Distribution" + licenseSuffix, lic.getDistribution());
            // addAttributeIfNotNull(target, "Plugin-License-Comments" + licenseSuffix, lic.getComments());
            licenseCounter++;
        }
    }

    private String getScmUrl() {
        Scm scm = project.getScm();

        if (scm == null || StringUtils.isEmpty(scm.getUrl())) {
            MavenProject parent = project.getParent();
            scm = parent.getScm();
            if (scm != null && StringUtils.isNotBlank(scm.getUrl())) {
                boolean endWithSlash = StringUtils.endsWith(scm.getUrl(), "/");
                return scm.getUrl() + (endWithSlash ? StringUtils.EMPTY : "/") + project.getArtifactId();
            }
        } else {
            return scm.getUrl();
        }

        return null;
    }

    private void addAttributeIfNotNull(Manifest.Section target, String attributeName, String propertyValue) throws ManifestException {
        if (propertyValue != null) {
            target.addAttributeAndCheck(new Manifest.Attribute(attributeName, propertyValue));
        }
    }

    private void addPropertyAttributeIfNotNull(Manifest.Section target, String attributeName, String propertyName) throws ManifestException {
        String propertyValue = project.getProperties().getProperty(propertyName);
        if (propertyValue != null) {
            target.addAttributeAndCheck(new Manifest.Attribute(attributeName, propertyValue));
        }
    }
}
