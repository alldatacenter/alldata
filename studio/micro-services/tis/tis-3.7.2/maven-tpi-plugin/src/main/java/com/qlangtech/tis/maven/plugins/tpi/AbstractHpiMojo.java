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

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public abstract class AbstractHpiMojo extends AbstractJenkinsMojo {

    /**
     * The directory for the generated WAR.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected String outputDirectory;

//    /**
//     * Single directory for extra files to include in the WAR.
//     */
    @Parameter(defaultValue = "${basedir}/tpis")
    protected File tpisDirectory;

    /**
     * The directory containing generated classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classesDirectory;

    /**
     * Name of the plugin that Jenkins uses for display purpose.
     * It should be one line text.
     */
    @Parameter(defaultValue = "${project.name}", readonly = true)
    protected String // TODO why is this read-only, surely I should be able to override
    pluginName;

    /**
     * Additional information that accompanies the version number of the plugin.
     * <p>
     * Useful to distinguish snapshot builds.
     * For example, if you are building snapshot plugins from Jenkins, you can
     * put the build number in here by running Maven with "-Dplugin.version.description=$BUILD_TAG"
     */
    @Parameter(defaultValue = "${plugin.version.description}")
    protected String pluginVersionDescription;

    /**
     * Optional - the version to use in place of the project version in the event that the project version is a
     * -SNAPSHOT. If specified, the value <strong>must</strong> start with the project version after removing the
     * terminal {@code -SNAPSHOT}, for example if the project version is {@code 1.2.3-SNAPSHOT} then the
     * {@code snapshotPluginVersionOverride} could be {@code 1.2.3-rc45.cafebabe-SNAPSHOT} or
     * {@code 1.2.3-20180430.123233-56}, etc, but it could not be {@code 1.2.4} as that does not start with the
     * project version.
     * <p>
     * When testing plugin builds on a locally hosted update centre, in order to be allowed to update the plugin,
     * the update centre must report the plugin version as greater than the currently installed plugin version.
     * If you are using a Continuous Delivery model for your plugin (i.e. where the master branch stays on a
     * version like {@code 1.x-SNAPSHOT} and releases are tagged but not merged back to master (in order to
     * prevent merge conflicts) then you will find that you cannot update the plugin when building locally as
     * {@code 1.x-SNAPSHOT} is never less than {@code 1.x-SNAPSHOT}. Thus in order to test plugin upgrade (in say
     * an acceptance test), you need to override the plugin version for non-releases. Typically you would use
     * something like <a href="https://github.com/stephenc/git-timestamp-maven-plugin">git-timestamp-maven-plugin</a>
     * to populate a property with the version and then use this configuration to provide the version.
     *
     * @see #failOnVersionOverrideToDifferentRelease
     * @since 2.4
     */
    @Parameter
    protected String snapshotPluginVersionOverride;

    /**
     * Controls the safety check that prevents a {@link #snapshotPluginVersionOverride} from switching to a different
     * release version.
     *
     * @since 2.4
     */
    @Parameter(defaultValue = "true")
    protected boolean failOnVersionOverrideToDifferentRelease = true;

    /**
     * The directory where the webapp is built.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
    private File webappDirectory;

    /**
     * Single directory for extra files to include in the WAR.
     */
    @Parameter(defaultValue = "${basedir}/src/main/webapp")
    protected File warSourceDirectory;

    /**
     * The list of webResources we want to transfer.
     */
    @Parameter
    private Resource[] webResources;

    @Parameter(defaultValue = "${project.build.filters}")
    private List<String> filters;

    /**
     * The path to the context.xml file to use.
     */
    @Parameter(defaultValue = "${maven.war.containerConfigXML}")
    private File containerConfigXML;

    /**
     * Directory to unpack dependent WARs into if needed
     */
    @Parameter(defaultValue = "${project.build.directory}/war/work")
    private File workDirectory;

    /**
     * To look up Archiver/UnArchiver implementations
     */
    @Component
    protected ArchiverManager archiverManager;

    private static final String WEB_INF = "WEB-INF";

    private static final String META_INF = "META-INF";

    private static final String[] DEFAULT_INCLUDES = { "**/**" };

    /**
     * The comma separated list of tokens to include in the WAR.
     */
    @Parameter(alias = "includes", defaultValue = "**")
    private String warSourceIncludes;

    /**
     * The comma separated list of tokens to exclude from the WAR.
     */
    @Parameter(alias = "excludes")
    private String warSourceExcludes;

    /**
     * The comma separated list of tokens to include when doing
     * a war overlay.
     */
    @Parameter(defaultValue = "**")
    private String dependentWarIncludes;

    /**
     * The comma separated list of tokens to exclude when doing
     * a way overlay.
     */
    @Parameter
    private String dependentWarExcludes;

    @Parameter
    private String classpathDependentExcludes;

    /**
     * [ws|tab|CR|LF]+ separated list of package prefixes that your plugin doesn't want to see
     * from the core.
     *
     * <p>
     * Tokens in this list is prefix-matched against the fully-qualified class name, so add
     * "." to the end of each package name, like "com.foo. com.bar."
     */
    @Parameter
    protected String maskClasses;

    /**
     * Like the {@code maskClasses} parameter, but it applies at the boundary between core and
     * all the plugins.
     *
     * <p>
     * This mechanism is intended for those plugins that bring JavaEE APIs (such as the database plugin,
     * which brings in the JPA API.) Other plugins that depend on the database plugin can still see
     * the JPA API through the container classloader, so to make them all resolve to the JPA API in the
     * database plugin, the database plugin needs to rely on this mechanism.
     *
     * @since 1.92
     */
    @Parameter
    protected String globalMaskClasses;

    /**
     * Change the classloader preference such that classes locally bundled in this plugin
     * will take precedence over those that are defined by the dependency plugins.
     *
     * <p>
     * This is useful if the plugins that you want to depend on exposes conflicting versions
     * of the libraries you are using, but enabling this switch makes your code
     * susceptible to classloader constraint violations.
     *
     * @since 1.53
     */
    @Parameter
    protected boolean pluginFirstClassLoader = false;

    /**
     * If true, test scope dependencies count as if they are normal dependencies.
     * This is only useful during hpi:run, so not exposing it as a configurable parameter.
     */
    ScopeArtifactFilter scopeFilter = new ScopeArtifactFilter("runtime");

    private static final String[] EMPTY_STRING_ARRAY = {};

    public File getClassesDirectory() {
        return classesDirectory;
    }

    public void setClassesDirectory(File classesDirectory) {
        this.classesDirectory = classesDirectory;
    }

    public File getWebappDirectory() {
        return webappDirectory;
    }

    public void setWebappDirectory(File webappDirectory) {
        this.webappDirectory = webappDirectory;
    }

    public void setWarSourceDirectory(File warSourceDirectory) {
        this.warSourceDirectory = warSourceDirectory;
    }

    public File getContainerConfigXML() {
        return containerConfigXML;
    }

    public void setContainerConfigXML(File containerConfigXML) {
        this.containerConfigXML = containerConfigXML;
    }

    /**
     * Returns a string array of the excludes to be used
     * when assembling/copying the war.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getExcludes() {
        List<String> excludeList = new ArrayList<String>();
        if (StringUtils.isNotEmpty(warSourceExcludes)) {
            excludeList.addAll(Arrays.asList(StringUtils.split(warSourceExcludes, ",")));
        }
        // if contextXML is specified, omit the one in the source directory
        if (containerConfigXML != null && StringUtils.isNotEmpty(containerConfigXML.getName())) {
            excludeList.add("**/" + META_INF + "/" + containerConfigXML.getName());
        }
        return excludeList.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * Returns a string array of the includes to be used
     * when assembling/copying the war.
     *
     * @return an array of tokens to include
     */
    protected String[] getIncludes() {
        return StringUtils.split(StringUtils.defaultString(warSourceIncludes), ",");
    }

    /**
     * Returns a string array of the excludes to be used
     * when adding dependent wars as an overlay onto this war.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getDependentWarExcludes() {
        String[] excludes;
        if (StringUtils.isNotEmpty(dependentWarExcludes)) {
            excludes = StringUtils.split(dependentWarExcludes, ",");
        } else {
            excludes = EMPTY_STRING_ARRAY;
        }
        return excludes;
    }

    /**
     * Returns a string array of the includes to be used
     * when adding dependent wars as an overlay onto this war.
     *
     * @return an array of tokens to include
     */
    protected String[] getDependentWarIncludes() {
        return StringUtils.split(StringUtils.defaultString(dependentWarIncludes), ",");
    }

    public void buildExplodedWebapp(File webappDirectory, File jarFile) throws MojoExecutionException {
        getLog().info("Exploding webapp...");
        webappDirectory.mkdirs();
        File webinfDir = new File(webappDirectory, WEB_INF);
        webinfDir.mkdirs();
        File metainfDir = new File(webappDirectory, META_INF);
        metainfDir.mkdirs();
        try {
            List<Resource> webResources = this.webResources != null ? Arrays.asList(this.webResources) : null;
            if (webResources != null && webResources.size() > 0) {
                Properties filterProperties = getBuildFilterProperties();
                for (Resource resource : webResources) {
                    copyResources(resource, webappDirectory, filterProperties);
                }
            }
            copyResources(warSourceDirectory, webappDirectory);
            if (containerConfigXML != null && StringUtils.isNotEmpty(containerConfigXML.getName())) {
                metainfDir = new File(webappDirectory, META_INF);
                String xmlFileName = containerConfigXML.getName();
                copyFileIfModified(containerConfigXML, new File(metainfDir, xmlFileName));
            }
            buildWebapp(project, webappDirectory);
            copyFileIfModified(jarFile, new File(getWebappDirectory(), "WEB-INF/lib/" + jarFile.getName()));
        } catch (IOException e) {
            throw new MojoExecutionException("Could not explode webapp...", e);
        }
    }

    private Properties getBuildFilterProperties() throws MojoExecutionException {
        // System properties
        Properties filterProperties = new Properties(System.getProperties());
        // Project properties
        filterProperties.putAll(project.getProperties());
        for (String filter : filters) {
            try {
                Properties properties = PropertyUtils.loadPropertyFile(new File(filter), true, true);
                filterProperties.putAll(properties);
            } catch (IOException e) {
                throw new MojoExecutionException("Error loading property file '" + filter + "'", e);
            }
        }
        return filterProperties;
    }

    /**
     * Copies webapp webResources from the specified directory.
     * <p>
     * Note that the {@code webXml} parameter could be null and may
     * specify a file which is not named {@code web.xml}. If the file
     * exists, it will be copied to the {@code META-INF} directory and
     * renamed accordingly.
     *
     * @param resource         the resource to copy
     * @param webappDirectory  the target directory
     * @param filterProperties
     * @throws IOException if an error occurred while copying webResources
     */
    public void copyResources(Resource resource, File webappDirectory, Properties filterProperties) throws IOException {
        if (!resource.getDirectory().equals(webappDirectory.getPath())) {
            getLog().info("Copy webapp webResources to " + webappDirectory.getAbsolutePath());
            if (webappDirectory.exists()) {
                String[] fileNames = getWarFiles(resource);
                for (String fileName : fileNames) {
                    if (resource.isFiltering()) {
                        copyFilteredFile(new File(resource.getDirectory(), fileName), new File(webappDirectory, fileName), null, getFilterWrappers(), filterProperties);
                    } else {
                        copyFileIfModified(new File(resource.getDirectory(), fileName), new File(webappDirectory, fileName));
                    }
                }
            }
        }
    }

    /**
     * Copies webapp webResources from the specified directory.
     * <p>
     * Note that the {@code webXml} parameter could be null and may
     * specify a file which is not named {@code web.xml}. If the file
     * exists, it will be copied to the {@code META-INF} directory and
     * renamed accordingly.
     *
     * @param sourceDirectory the source directory
     * @param webappDirectory the target directory
     * @throws IOException if an error occurred while copying webResources
     */
    public void copyResources(File sourceDirectory, File webappDirectory) throws IOException {
        if (!sourceDirectory.equals(webappDirectory)) {
            getLog().info("Copy webapp webResources to " + webappDirectory.getAbsolutePath());
            if (warSourceDirectory.exists()) {
                String[] fileNames = getWarFiles(sourceDirectory);
                for (String fileName : fileNames) {
                    copyFileIfModified(new File(sourceDirectory, fileName), new File(webappDirectory, fileName));
                }
            }
        }
    }

    /**
     * Returns all the transitive dependencies.
     */
    public Set<MavenArtifact> getProjectArtfacts() {
        return wrap(Artifacts.of(project));
    }

    /**
     * Returns just the direct dependencies.
     */
    public Set<MavenArtifact> getDirectDependencyArtfacts() {
        return wrap(Artifacts.ofDirectDependencies(project));
    }

    protected Set<MavenArtifact> wrap(Iterable<Artifact> artifacts) {
        Set<MavenArtifact> r = new TreeSet<>();
        for (Artifact a : artifacts) {
            r.add(wrap(a));
        }
        return r;
    }

    /**
     * Builds the webapp for the specified project.
     * <p>
     * Classes, libraries and tld files are copied to
     * the {@code webappDirectory} during this phase.
     *
     * @param project         the maven project
     * @param webappDirectory
     * @throws IOException if an error occurred while building the webapp
     */
    public void buildWebapp(MavenProject project, File webappDirectory) throws MojoExecutionException, IOException {
        getLog().info("Assembling webapp " + project.getArtifactId() + " in " + webappDirectory);
        // baisui add for specific dependency filter
        ArtifactFilter dependencyFilter = null;
        if (this.classpathDependentExcludes != null) {
            List<String> excludedDependencies = Arrays.asList(StringUtils.split(this.classpathDependentExcludes, ","));
            dependencyFilter = new PatternIncludesArtifactFilter(excludedDependencies);
        }
        File libDirectory = new File(webappDirectory, WEB_INF + "/lib");
        File tldDirectory = new File(webappDirectory, WEB_INF + "/tld");
        Set<MavenArtifact> artifacts = getProjectArtfacts();
        // Also capture test dependencies
        Set<MavenArtifact> dependencyArtifacts = getDirectDependencyArtfacts();
        List<String> duplicates = findDuplicates(artifacts);
        List<File> dependentWarDirectories = new ArrayList<File>();
        // List up IDs of Jenkins plugin dependencies
        Set<String> tisPlugins = new HashSet<>();
        Set<String> excludedArtifacts = new HashSet<>();
        for (MavenArtifact artifact : Sets.union(artifacts, dependencyArtifacts)) {
            if (artifact.isPluginBestEffort(getLog()))
                tisPlugins.add(artifact.getId());
            // Most likely a plugin transitive dependency but the trail through (test,provided) dep is shorter
            if (artifact.hasScope("test", "provided")) {
                excludedArtifacts.add(artifact.getId());
            }
        }
        OUTER: for (MavenArtifact artifact : artifacts) {
            // baisui add for specific
            if (dependencyFilter != null && dependencyFilter.include(artifact.artifact)) {
                this.getLog().info("dependency artifact:" + artifact.artifact + " skip");
                continue;
            }
            if (tisPlugins.contains(artifact.getId())) {
                // plugin dependency need not be WEB-INF/lib
                continue;
            }
            if (artifact.getDependencyTrail().size() >= 1) {
                if (tisPlugins.contains(artifact.getDependencyTrail().get(1))) {
                    // no need to have transitive dependencies through plugins in WEB-INF/lib.
                    continue;
                }
                if (excludedArtifacts.contains(artifact.getDependencyTrail().get(1))) {
                    // exclude artifacts resolved through a test or provided scope
                    continue;
                }
            }
            // dependencies into the artifact list.
            for (String trail : artifact.getDependencyTrail()) {
                if (trail.contains(":hudson-core:") || trail.contains(":jenkins-core:"))
                    continue OUTER;
            }
            String targetFileName = artifact.getDefaultFinalName();
            getLog().debug("Processing: " + targetFileName);
            if (duplicates.contains(targetFileName)) {
                getLog().debug("Duplicate found: " + targetFileName);
                targetFileName = artifact.getGroupId() + "-" + targetFileName;
                getLog().debug("Renamed to: " + targetFileName);
            }
            // TODO: utilise appropriate methods from project builder
            ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
            if (!artifact.isOptional() && filter.include(artifact.artifact)) {
                if (artifact.getDependencyTrail().size() > 2) {
                    getLog().warn("Bundling transitive dependency " + targetFileName + " (via " + artifact.getDependencyTrail().get(1).replaceAll("[^:]+:([^:]+):.+", "$1") + ")");
                } else {
                    getLog().info("Bundling direct dependency " + targetFileName);
                }
                String type = artifact.getType();
                if ("tld".equals(type)) {
                    copyFileIfModified(artifact.getFile(), new File(tldDirectory, targetFileName));
                } else {
                    if ("jar".equals(type) || "ejb".equals(type) || "ejb-client".equals(type)) {
                        copyFileIfModified(artifact.getFile(), new File(libDirectory, targetFileName));
                    } else {
                        if ("par".equals(type)) {
                            targetFileName = targetFileName.substring(0, targetFileName.lastIndexOf('.')) + ".jar";
                            getLog().debug("Copying " + artifact.getFile() + " to " + new File(libDirectory, targetFileName));
                            copyFileIfModified(artifact.getFile(), new File(libDirectory, targetFileName));
                        } else {
                            if ("war".equals(type)) {
                                dependentWarDirectories.add(unpackWarToTempDirectory(artifact));
                            } else {
                                getLog().debug("Skipping artifact of type " + type + " for WEB-INF/lib");
                            }
                        }
                    }
                }
            }
        }
        if (dependentWarDirectories.size() > 0) {
            getLog().info("Overlaying " + dependentWarDirectories.size() + " war(s).");
            // overlay dependent wars
            for (Iterator iter = dependentWarDirectories.iterator(); iter.hasNext(); ) {
                copyDependentWarContents((File) iter.next(), webappDirectory);
            }
        }
    }

    /**
     * Searches a set of artifacts for duplicate filenames and returns a list of duplicates.
     *
     * @param artifacts set of artifacts
     * @return List of duplicated artifacts
     */
    private List<String> findDuplicates(Set<MavenArtifact> artifacts) {
        List<String> duplicates = new ArrayList<String>();
        List<String> identifiers = new ArrayList<String>();
        for (MavenArtifact artifact : artifacts) {
            String candidate = artifact.getDefaultFinalName();
            if (identifiers.contains(candidate)) {
                duplicates.add(candidate);
            } else {
                identifiers.add(candidate);
            }
        }
        return duplicates;
    }

    /**
     * Unpacks war artifacts into a temporary directory inside {@code workDirectory}
     * named with the name of the war.
     *
     * @param artifact War artifact to unpack.
     * @return Directory containing the unpacked war.
     * @throws MojoExecutionException
     */
    private File unpackWarToTempDirectory(MavenArtifact artifact) throws MojoExecutionException {
        String name = artifact.getFile().getName();
        File tempLocation = new File(workDirectory, name.substring(0, name.length() - 4));
        boolean process = false;
        if (!tempLocation.exists()) {
            tempLocation.mkdirs();
            process = true;
        } else if (artifact.getFile().lastModified() > tempLocation.lastModified()) {
            process = true;
        }
        if (process) {
            File file = artifact.getFile();
            try {
                unpack(file, tempLocation);
            } catch (NoSuchArchiverException e) {
                this.getLog().info("Skip unpacking dependency file with unknown extension: " + file.getPath());
            }
        }
        return tempLocation;
    }

    /**
     * Unpacks the archive file.
     *
     * @param file     File to be unpacked.
     * @param location Location where to put the unpacked files.
     */
    private void unpack(File file, File location) throws MojoExecutionException, NoSuchArchiverException {
        String archiveExt = FileUtils.getExtension(file.getAbsolutePath()).toLowerCase();
        try {
            UnArchiver unArchiver = archiverManager.getUnArchiver(archiveExt);
            unArchiver.setSourceFile(file);
            unArchiver.setDestDirectory(location);
            unArchiver.extract();
        } catch (IOException e) {
            throw new MojoExecutionException("Error unpacking file: " + file + "to: " + location, e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Error unpacking file: " + file + "to: " + location, e);
        }
    }

    /**
     * Recursively copies contents of {@code srcDir} into {@code targetDir}.
     * This will not overwrite any existing files.
     *
     * @param srcDir    Directory containing unpacked dependent war contents
     * @param targetDir Directory to overlay srcDir into
     */
    private void copyDependentWarContents(File srcDir, File targetDir) throws MojoExecutionException {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(srcDir);
        scanner.setExcludes(getDependentWarExcludes());
        scanner.addDefaultExcludes();
        scanner.setIncludes(getDependentWarIncludes());
        scanner.scan();
        for (String dir : scanner.getIncludedDirectories()) {
            new File(targetDir, dir).mkdirs();
        }
        for (String file : scanner.getIncludedFiles()) {
            File targetFile = new File(targetDir, file);
            // Do not overwrite existing files.
            if (!targetFile.exists()) {
                try {
                    targetFile.getParentFile().mkdirs();
                    copyFileIfModified(new File(srcDir, file), targetFile);
                } catch (IOException e) {
                    throw new MojoExecutionException("Error copying file '" + file + "' to '" + targetFile + "'", e);
                }
            }
        }
    }

    /**
     * Returns a list of filenames that should be copied
     * over to the destination directory.
     *
     * @param sourceDir the directory to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getWarFiles(File sourceDir) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(sourceDir);
        scanner.setExcludes(getExcludes());
        scanner.addDefaultExcludes();
        scanner.setIncludes(getIncludes());
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    /**
     * Returns a list of filenames that should be copied
     * over to the destination directory.
     *
     * @param resource the resource to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getWarFiles(Resource resource) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(resource.getDirectory());
        if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
            scanner.setIncludes((String[]) resource.getIncludes().toArray(EMPTY_STRING_ARRAY));
        } else {
            scanner.setIncludes(DEFAULT_INCLUDES);
        }
        if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
            scanner.setExcludes((String[]) resource.getExcludes().toArray(EMPTY_STRING_ARRAY));
        }
        scanner.addDefaultExcludes();
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    /**
     * Copy file from source to destination only if source is newer than the target file.
     * If <code>destinationDirectory</code> does not exist, it
     * (and any parent directories) will be created. If a file <code>source</code> in
     * <code>destinationDirectory</code> exists, it will be overwritten.
     *
     * @param source               An existing <code>File</code> to copy.
     * @param destinationDirectory A directory to copy <code>source</code> into.
     * @throws FileNotFoundException    if <code>source</code> isn't a normal file.
     * @throws IllegalArgumentException if <code>destinationDirectory</code> isn't a directory.
     * @throws IOException              if <code>source</code> does not exist, the file in
     *                                  <code>destinationDirectory</code> cannot be written to, or an IO error occurs during copying.
     *                                  <p>
     *                                  TO DO: Remove this method when Maven moves to plexus-utils version 1.4
     */
    private static void copyFileToDirectoryIfModified(File source, File destinationDirectory) throws IOException {
        // to plexus-utils 1.2.
        if (destinationDirectory.exists() && !destinationDirectory.isDirectory()) {
            throw new IllegalArgumentException("Destination is not a directory");
        }
        copyFileIfModified(source, new File(destinationDirectory, source.getName()));
    }

    private FilterWrapper[] getFilterWrappers() {
        return new FilterWrapper[] { // support ${token}
        new FilterWrapper() {

            public Reader getReader(Reader fileReader, Properties filterProperties) {
                return new InterpolationFilterReader(fileReader, filterProperties, "${", "}");
            }
        }, // support @token@
        new FilterWrapper() {

            public Reader getReader(Reader fileReader, Properties filterProperties) {
                return new InterpolationFilterReader(fileReader, filterProperties, "@", "@");
            }
        } };
    }

    /**
     * @param from
     * @param to
     * @param encoding
     * @param wrappers
     * @param filterProperties
     * @throws IOException TO DO: Remove this method when Maven moves to plexus-utils version 1.4
     */
    private static void copyFilteredFile(File from, File to, String encoding, FilterWrapper[] wrappers, Properties filterProperties) throws IOException {
        // buffer so it isn't reading a byte at a time!
        Reader fileReader = null;
        Writer fileWriter = null;
        try {
            // fix for MWAR-36, ensures that the parent dir are created first
            to.getParentFile().mkdirs();
            if (encoding == null || encoding.length() < 1) {
                fileReader = new BufferedReader(new FileReader(from));
                fileWriter = new FileWriter(to);
            } else {
                FileInputStream instream = new FileInputStream(from);
                FileOutputStream outstream = new FileOutputStream(to);
                fileReader = new BufferedReader(new InputStreamReader(instream, encoding));
                fileWriter = new OutputStreamWriter(outstream, encoding);
            }
            Reader reader = fileReader;
            for (FilterWrapper wrapper : wrappers) {
                reader = wrapper.getReader(reader, filterProperties);
            }
            IOUtil.copy(reader, fileWriter);
        } finally {
            IOUtil.close(fileReader);
            IOUtil.close(fileWriter);
        }
    }

    /**
     * Copy file from source to destination only if source timestamp is later than the destination timestamp.
     * The directories up to <code>destination</code> will be created if they don't already exist.
     * <code>destination</code> will be overwritten if it already exists.
     *
     * @param source      An existing non-directory <code>File</code> to copy bytes from.
     * @param destination A non-directory <code>File</code> to write bytes to (possibly
     *                    overwriting).
     * @throws IOException           if <code>source</code> does not exist, <code>destination</code> cannot be
     *                               written to, or an IO error occurs during copying.
     * @throws FileNotFoundException if <code>destination</code> is a directory
     *                               <p>
     *                               TO DO: Remove this method when Maven moves to plexus-utils version 1.4
     */
    private static void copyFileIfModified(File source, File destination) throws IOException {
        // to plexus-utils 1.2.
        if (destination.lastModified() < source.lastModified()) {
            FileUtils.copyFile(source, destination);
        }
    }

    /**
     * Copies a entire directory structure but only source files with timestamp later than the destinations'.
     * <p>
     * Note:
     * <ul>
     * <li>It will include empty directories.
     * <li>The <code>sourceDirectory</code> must exists.
     * </ul>
     *
     * @param sourceDirectory
     * @param destinationDirectory
     * @throws IOException TO DO: Remove this method when Maven moves to plexus-utils version 1.4
     */
    private static void copyDirectoryStructureIfModified(File sourceDirectory, File destinationDirectory) throws IOException {
        if (!sourceDirectory.exists()) {
            throw new IOException("Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ").");
        }
        String sourcePath = sourceDirectory.getAbsolutePath();
        for (File file : sourceDirectory.listFiles()) {
            String dest = file.getAbsolutePath();
            dest = dest.substring(sourcePath.length() + 1);
            File destination = new File(destinationDirectory, dest);
            if (file.isFile()) {
                destination = destination.getParentFile();
                copyFileToDirectoryIfModified(file, destination);
            } else if (file.isDirectory()) {
                if (!destination.exists() && !destination.mkdirs()) {
                    throw new IOException("Could not create destination directory '" + destination.getAbsolutePath() + "'.");
                }
                copyDirectoryStructureIfModified(file, destination);
            } else {
                throw new IOException("Unknown file type: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * If the project is on Git, figure out Git SHA1.
     *
     * @return null if no git repository is found
     */
    public String getGitHeadSha1() {
        // we want to allow the plugin that's not sitting at the root (such as findbugs plugin),
        // but we don't want to go up too far and pick up unrelated repository.
        File git = new File(project.getBasedir(), ".git");
        if (!git.exists()) {
            git = new File(project.getBasedir(), "../.git");
            if (!git.exists())
                return null;
        }
        try {
            Process p = new ProcessBuilder("git", "rev-parse", "HEAD").redirectErrorStream(true).start();
            p.getOutputStream().close();
            String v = IOUtils.toString(p.getInputStream()).trim();
            if (p.waitFor() != 0)
                // git rev-parse failed to run
                return null;
            return v.trim().substring(0, 8);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to run git rev-parse HEAD", e);
            return null;
        } catch (InterruptedException e) {
            LOGGER.log(Level.FINE, "Failed to run git rev-parse HEAD", e);
            return null;
        }
    }

    /**
     * TO DO: Remove this interface when Maven moves to plexus-utils version 1.4
     */
    private interface FilterWrapper {

        Reader getReader(Reader fileReader, Properties filterProperties);
    }

    /**
     * Is the dynamic loading supported?
     * <p>
     * False, if the answer is known to be "No". Otherwise null, if there are some extensions
     * we don't know we can dynamic load. Otherwise, if everything is known to be dynamic loadable, return true.
     */
    protected Boolean isSupportDynamicLoading() throws IOException {
        // return true;
        return true;
    }

    private static final Logger LOGGER = Logger.getLogger(AbstractHpiMojo.class.getName());
}
