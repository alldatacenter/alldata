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

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.impl.MissingDependencyException;
import com.qlangtech.tis.extension.impl.PluginManifest;
import com.qlangtech.tis.extension.model.UpdateCenter;
import com.qlangtech.tis.extension.model.UpdateSite;
import com.qlangtech.tis.extension.util.VersionNumber;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import com.qlangtech.tis.util.PluginMeta;
import com.qlangtech.tis.util.YesNoMaybe;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

/**
 * port org.kohsuke.stapler.export.Exported;
 * Represents a Jenkins plug-in and associated control information
 * for Jenkins to control
 * <p>
 * A plug-in is packaged into a jar file whose extension is <tt>".jpi"</tt> (or <tt>".hpi"</tt> for backward compatibility),
 * A plugin needs to have a special manifest entry to identify what it is.
 * <p>
 * At the runtime, a plugin has two distinct state axis.
 * <ol>
 *  <li>Enabled/Disabled. If enabled, Jenkins is going to use it
 *      next time Jenkins runs. Otherwise the next run will ignore it.
 *  <li>Activated/Deactivated. If activated, that means Jenkins is using
 *      the plugin in this session. Otherwise it's not.
 * </ol>
 * <p>
 * For example, an activated but import javax.annotation.CheckForNull;
 * import javax.annotation.Nonnull;disabled plugin is still running but the next
 * time it won't.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class PluginWrapper implements Comparable<PluginWrapper>, ModelObject, ITPIArtifact {

    /**
     * List of plugins that depend on this plugin.
     */
    //   private Set<String> dependents = Collections.emptySet();
    /**
     * List of plugins that depend on this plugin.
     */
    private Set<String> dependents = Collections.emptySet();

    /**
     * List of plugins that depend optionally on this plugin.
     */
    private Set<String> optionalDependents = Collections.emptySet();

    /**
     * {@link PluginManager} to which this belongs to.
     */
    public final PluginManager parent;

    /**
     * Plugin manifest.
     * Contains description of the plugin.
     */
    private final PluginManifest manifest;

    /**
     * {@link ClassLoader} for loading classes from this plugin.
     * Null if disabled.
     */
    public final ClassLoader classLoader;

    /**
     * Base URL for loading static resources from this plugin.
     * Null if disabled. The static resources are mapped under
     * <tt>CONTEXTPATH/plugin/SHORTNAME/</tt>.
     */
    public final URL baseResourceURL;

    /**
     * Used to control enable/disable setting of the plugin.
     * If this file exists, plugin will be disabled.
     */
    private final File disableFile;

    /**
     * A .jpi file, an exploded plugin directory, or a .jpl file.
     */
    private final File archive;

    /**
     * Short name of the plugin. The artifact Id of the plugin.
     * This is also used in the URL within Jenkins, so it needs
     * to remain stable even when the *.jpi file name is changed
     * (like Maven does.)
     */
    private final String shortName;

    /**
     * True if this plugin is activated for this session.
     * The snapshot of <tt>disableFile.exists()</tt> as of the start up.
     */
    private final boolean active;

    private boolean hasCycleDependency = false;

    private final List<Dependency> dependencies;

    private final List<Dependency> optionalDependencies;

    private final Optional<PluginClassifier> classifier;

    /**
     * Is this plugin bundled in jenkins.war?
     */
    /*package*/
    boolean isBundled;

    @Override
    public String getIdentityName() {
        return this.shortName;
    }

    /**
     * The core can depend on a plugin if it is bundled. Sometimes it's the only thing that
     * depends on the plugin e.g. UI support library bundle plugin.
     */
    // .unmodifiableSet(Arrays.());
    private static Set<String> CORE_ONLY_DEPENDANT = Collections.singleton("jenkins-core");

    /**
     * Set the list of components that depend on this plugin.
     *
     * @param dependents The list of components that depend on this plugin.
     */
    public void setDependents(Set<String> dependents) {
        this.dependents = dependents;
    }

    /**
     * Get the list of components that depend on this plugin.
     *
     * @return The list of components that depend on this plugin.
     */
    public Set<String> getDependents() {
        if (isBundled && dependents.isEmpty()) {
            return CORE_ONLY_DEPENDANT;
        } else {
            return dependents;
        }
    }

    /**
     * Does this plugin have anything that depends on it.
     *
     * @return {@code true} if something (Jenkins core, or another plugin) depends on this
     * plugin, otherwise {@code false}.
     */
    public boolean hasDependants() {
        return (isBundled || !dependents.isEmpty());
    }

    /**
     * Does this plugin depend on any other plugins.
     *
     * @return {@code true} if this plugin depends on other plugins, otherwise {@code false}.
     */
    public boolean hasDependencies() {
        return (dependencies != null && !dependencies.isEmpty());
    }

    public YesNoMaybe supportsDynamicLoad() {
//        String v = manifest.getMainAttributes().getValue("Support-Dynamic-Loading");
//        if (v == null) { return YesNoMaybe.MAYBE;}
//        return Boolean.parseBoolean(v) ? YesNoMaybe.YES : YesNoMaybe.NO;
        return manifest.supportsDynamicLoad();
    }


    /**
     * Set the list of components that depend optionally on this plugin.
     *
     * @param optionalDependents The list of components that depend optionally on this plugin.
     */
    public void setOptionalDependents(Set<String> optionalDependents) {
        this.optionalDependents = optionalDependents;
    }


    // @ExportedBean
    public static final class Dependency {

        // @Exported
        public final String shortName;

        // @Exported
        public final String version;

        // @Exported
        public final boolean optional;


        public static Dependency parse(String s) {
            int idx = s.indexOf(':');
            if (idx == -1) {
                throw new IllegalArgumentException("Illegal dependency specifier " + s);
            }
            String shortName = s.substring(0, idx);
            String version = s.substring(idx + 1);
            boolean isOptional = false;
            String[] osgiProperties = s.split(";");
            for (int i = 1; i < osgiProperties.length; i++) {
                String osgiProperty = osgiProperties[i].trim();
                if (osgiProperty.equalsIgnoreCase("resolution:=optional")) {
                    isOptional = true;
                }
            }
            return new Dependency(shortName, version, isOptional);
        }


        public Dependency(String shortName, String version, boolean optional) {
            this.shortName = shortName;
            this.version = version;
            this.optional = optional;
        }

        @Override
        public String toString() {
            return shortName + " (" + version + ")";
        }
    }

    /**
     * @param archive              A .jpi archive file jar file, or a .jpl linked plugin.
     * @param manifest             The manifest for the plugin
     * @param baseResourceURL      A URL pointing to the resources for this plugin
     * @param classLoader          a classloader that loads classes from this plugin and its dependencies
     * @param disableFile          if this file exists on startup, the plugin will not be activated
     * @param dependencies         a list of mandatory dependencies
     * @param optionalDependencies a list of optional dependencies
     */
    public PluginWrapper(PluginManager parent, File archive, PluginManifest manifest, URL baseResourceURL, ClassLoader classLoader
            , File disableFile, List<Dependency> dependencies, List<Dependency> optionalDependencies) {
        this.parent = parent;
        this.manifest = manifest;
        this.shortName = manifest.computeShortName(archive.getName());
        this.baseResourceURL = baseResourceURL;
        this.classLoader = classLoader;
        this.disableFile = disableFile;
        this.active = !disableFile.exists();
        this.dependencies = dependencies;
        this.optionalDependencies = optionalDependencies;
        this.archive = archive;
        // String attrClazzier = manifest.getMainAttributes().getValue(PluginManager.PACAKGE_CLASSIFIER);
        this.classifier = manifest.parseClassifier(); //Optional.ofNullable(StringUtils.isEmpty(attrClazzier) ? null : new PluginClassifier(attrClazzier));
    }

    @Override
    public final Optional<PluginClassifier> getClassifier() {
        return this.classifier;
    }

    public String getDisplayName() {
        return StringUtils.removeStart(getLongName(), "Jenkins ");
    }

    /**
     * Gets the instance of {@link Plugin} contributed by this plugin.
     *
     * @throws Exception no plugin in the {@link 'PluginInstanceStore'}
     */
    public Plugin getPluginOrFail() throws Exception {
        Plugin plugin = getPlugin();
        if (plugin == null) {
            throw new Exception("Cannot find the plugin instance: " + shortName);
        }
        return plugin;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public List<Dependency> getOptionalDependencies() {
        return optionalDependencies;
    }

    /**
     * Returns the short name suitable for URL.
     */
    public String getShortName() {
        return shortName;
    }

    // /**
    // * Gets the instance of {@link Plugin} contributed by this plugin.
    // */
    // public @CheckForNull
    public Plugin getPlugin() {
        // TIS.get().getPluginInstanceStore(PluginManager.PluginInstanceStore.class);
        PluginManager.PluginInstanceStore pis = parent.pluginInstanceStore;
        return pis.store.get(this);
        // throw new UnsupportedOperationException();
    }

    // /**
    // * Gets the URL that shows more information about this plugin.
    // *
    // * @return null if this information is unavailable.
    // * @since 1.283
    // */
    // @Exported
    public String getUrl() {
        // first look for the manifest entry. This is new in maven-hpi-plugin 1.30
        String url = manifest.getURL();//.getValue("Url");
        if (url != null) {
            return url;
        }
        // fallback to update center metadata
//        UpdateSite.Plugin ui = getInfo();
//        if (ui != null) return ui.wiki;

        return null;
    }

    @Override
    public String toString() {
        return "Plugin:" + getShortName();
    }

    /**
     * Returns a one-line descriptive name of this plugin.
     */
    // @Exported
    public String getLongName() {
        return StringUtils.defaultIfBlank(manifest.getLongName(), shortName);//.getMainAttributes().getValue("Long-Name");
//        if (name != null)
//            return name;
//        return shortName;
    }

    public String getGroupId() {
        return manifest.getGroupId();//.getMainAttributes().getValue("Group-Id");
    }

    public long getLastModfiyTime() {
        return manifest.getLastModfiyTime();// Long.parseLong(manifest.getMainAttributes().getValue(PluginStrategy.KEY_LAST_MODIFY_TIME));
    }

//    public long getArchiveFileSize() {
//        //  this.archive.getAbsolutePath()
//        return FileUtils.sizeOf(this.archive);
//    }

    public File getArchive() {
        return this.archive;
    }

    /**
     * Does this plugin supports dynamic loading?
     */
    // @Exported
    // public YesNoMaybe supportsDynamicLoad() {
    // String v = manifest.getMainAttributes().getValue("Support-Dynamic-Loading");
    // if (v == null) return YesNoMaybe.MAYBE;
    // return Boolean.parseBoolean(v) ? YesNoMaybe.YES : YesNoMaybe.NO;
    // }

    /**
     * Returns the version number of this plugin
     */
    // @Exported
    public String getVersion() {
        return manifest.getVersionOf();// getVersionOf(manifest);
    }


    /**
     * Returns the version number of this plugin
     */
    public VersionNumber getVersionNumber() {
        return new VersionNumber(getVersion());
    }

    /**
     * Returns true if the version of this plugin is older than the given version.
     */
    public boolean isOlderThan(VersionNumber v) {
        try {
            return getVersionNumber().compareTo(v) < 0;
        } catch (IllegalArgumentException e) {
            // since the version information is missing only from the very old plugins
            return true;
        }
    }

    /**
     * Terminates the plugin.
     */
    public void stop() {
        Plugin plugin = getPlugin();
        if (plugin != null) {
            try {
                LOGGER.log(Level.FINE, "Stopping {0}", shortName);
                plugin.stop();
            } catch (Throwable t) {
                LOGGER.log(WARNING, "Failed to shut down " + shortName, t);
            }
        } else {
            LOGGER.log(Level.FINE, "Could not find Plugin instance to stop for {0}", shortName);
        }
        // Work around a bug in commons-logging.
        // See http://www.szegedi.org/articles/memleak.html
        // LogFactory.release(classLoader);
    }

    public void releaseClassLoader() {
        if (classLoader instanceof Closeable)
            try {
                ((Closeable) classLoader).close();
            } catch (IOException e) {
                LOGGER.log(WARNING, "Failed to shut down classloader", e);
            }
    }

    /**
     * Enables this plugin next time Jenkins runs.
     */
    public void enable() throws IOException {
        if (!disableFile.exists()) {
            LOGGER.log(Level.FINEST, "Plugin {0} has been already enabled. Skipping the enable() operation", getShortName());
            return;
        }
        if (!disableFile.delete()) {
            throw new IOException("Failed to delete " + disableFile);
        }
    }

    /**
     * Disables this plugin next time Jenkins runs.
     */
    public void disable() throws IOException {
        // creates an empty file
        FileUtils.touch(disableFile);
//        OutputStream os = new FileOutputStream(disableFile);
//        os.close();
    }

    /**
     * Returns true if this plugin is enabled for this session.
     */
    // @Exported
    public boolean isActive() {
        return active && !hasCycleDependency();
    }

    public boolean hasCycleDependency() {
        return hasCycleDependency;
    }

    public void setHasCycleDependency(boolean hasCycle) {
        hasCycleDependency = hasCycle;
    }

    // @Exported
    public boolean isBundled() {
        return isBundled;
    }

    /**
     * If true, the plugin is going to be activated next time
     * Jenkins runs.
     */
    // @Exported
    public boolean isEnabled() {
        return !disableFile.exists();
    }

//    public Manifest getManifest() {
//        return manifest;
//    }

    public void setPlugin(Plugin plugin) {
        this.parent.pluginInstanceStore.store.put(this, plugin);
        plugin.wrapper = this;
    }

    public String getPluginClass() {
        return manifest.getPluginClass(); //manifest.getMainAttributes().getValue("Plugin-Class");
    }

    public boolean hasLicensesXml() {
        try {
            new URL(baseResourceURL, "WEB-INF/licenses.xml").openStream().close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Makes sure that all the dependencies exist, and then accept optional dependencies
     * as real dependencies.
     *
     * @throws IOException thrown if one or several mandatory dependencies doesn't exists.
     */
    /*package*/
    void resolvePluginDependencies() throws IOException {
        List<Pair<Dependency, ITPIArtifactMatch>> missingDependencies = new ArrayList<>();
        //  ITPIArtifactMatch match = ITPIArtifact.match(this.classifier);

        ITPIArtifact.matchDependency(parent, dependencies, this, (p) -> {
        }, (missDep) -> {
            missingDependencies.add(missDep);
        });
        // make sure dependencies exist
//        for (Dependency d : dependencies) {
//            match.setIdentityName(d.shortName);
//            if (parent.getPlugin(match) == null) {
//                missingDependencies.add(d);
//            }
//        }
        if (!missingDependencies.isEmpty()) {
            throw new MissingDependencyException(this.shortName, missingDependencies);
        }
        // add the optional dependencies that exists

        ITPIArtifact.matchDependency(parent, optionalDependencies, this, (p) -> {
            dependencies.add(p.getRight());
        });

//        for (Dependency d : optionalDependencies) {
//            match.setIdentityName(d.shortName);
//            if (parent.getPlugin(match) != null) {
//                dependencies.add(d);
//            }
//        }
    }

    // /**
    // * If the plugin has {@link #getUpdateInfo() an update},
    // * returns the {@link hudson.model.UpdateSite.Plugin} object.
    // *
    // * @return
    // *      This method may return null &mdash; for example,
    // *      the user may have installed a plugin locally developed.
    // */
    // public UpdateSite.Plugin getUpdateInfo() {
    // UpdateCenter uc = Jenkins.getInstance().getUpdateCenter();
    // UpdateSite.Plugin p = uc.getPlugin(getShortName());
    // if(p!=null && p.isNewerThan(getVersion())) return p;
    // return null;
    // }

    /**
     * returns the {@link com.qlangtech.tis.extension.model.UpdateSite.Plugin} object, or null.
     */
    public UpdateSite.Plugin getInfo() {
        UpdateCenter uc = TIS.get().getUpdateCenter();
        return uc.getPlugin(getShortName());
    }

    /**
     * Returns true if this plugin has update in the update center.
     *
     * <p>
     * This method is conservative in the sense that if the version number is incomprehensible,
     * it always returns false.
     */
    // @Exported
    public boolean hasUpdate() {
        return getUpdateInfo() != null;
    }

    public UpdateSite.Plugin getUpdateInfo() {
        //  if(p!=null && p.isNewerThan(getVersion())) return p;
        return null;
    }

    // @Exported
    // See https://groups.google.com/d/msg/jenkinsci-dev/kRobm-cxFw8/6V66uhibAwAJ
    @Deprecated
    public boolean isPinned() {
        return false;
    }

    /**
     * Returns true if this plugin is deleted.
     * <p>
     * The plugin continues to function in this session, but in the next session it'll disappear.
     */
    // @Exported
    public boolean isDeleted() {
        return !archive.exists();
    }

    /**
     * Sort by short name.
     */
    public int compareTo(PluginWrapper pw) {
        return shortName.compareToIgnoreCase(pw.shortName);
    }

    /**
     * returns true if backup of previous version of plugin exists
     */
    public boolean isDowngradable() {
        return getBackupFile().exists();
    }

    /**
     * Where is the backup file?
     */
    public File getBackupFile() {
        return new File(TIS.pluginCfgRoot, "plugins/" + getShortName() + ".bak");
    }

    /**
     * returns the version of the backed up plugin,
     * or null if there's no back up.
     */
    public String getBackupVersion() {
        File backup = getBackupFile();
        if (backup.exists()) {
            try {
                JarFile backupPlugin = new JarFile(backup);
                try {
                    return backupPlugin.getManifest().getMainAttributes().getValue("Plugin-Version");
                } finally {
                    backupPlugin.close();
                }
            } catch (IOException e) {
                LOGGER.log(WARNING, "Failed to get backup version from " + backup, e);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Plugin 描述信息
     *
     * @return
     */
    public PluginMeta getDesc() {

        return new PluginMeta(this.getShortName(), trimVersion(this.getVersion()), this.getClassifier());
        // return this.getShortName() + XStream2.PluginMeta.NAME_VER_SPLIT + trimVersion(this.getVersion()); // : null;
    }

    static String trimVersion(String version) {
        // TODO seems like there should be some trick with VersionNumber to do this
        return version.replaceFirst(" .+$", "");
    }

    /**
     * Checks if this plugin is pinned and that's forcing us to use an older version than the bundled one.
     */
    // See https://groups.google.com/d/msg/jenkinsci-dev/kRobm-cxFw8/6V66uhibAwAJ
    @Deprecated
    public boolean isPinningForcingOldVersion() {
        return false;
    }

    //
    //
    // Action methods
    //
    //
    // @RequirePOST
    // public HttpResponse doMakeEnabled() throws IOException {
    // Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
    // enable();
    // return HttpResponses.ok();
    // }
    // @RequirePOST
    // public HttpResponse doMakeDisabled() throws IOException {
    // Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
    // disable();
    // return HttpResponses.ok();
    // }
    // @RequirePOST
    // @Deprecated
    // public HttpResponse doPin() throws IOException {
    // Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
    // // See https://groups.google.com/d/msg/jenkinsci-dev/kRobm-cxFw8/6V66uhibAwAJ
    // LOGGER.log(WARNING, "Call to pin plugin has been ignored. Plugin name: " + shortName);
    // return HttpResponses.ok();
    // }
    // @RequirePOST
    // @Deprecated
    // public HttpResponse doUnpin() throws IOException {
    // Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
    // // See https://groups.google.com/d/msg/jenkinsci-dev/kRobm-cxFw8/6V66uhibAwAJ
    // LOGGER.log(WARNING, "Call to unpin plugin has been ignored. Plugin name: " + shortName);
    // return HttpResponses.ok();
    // }
    // @RequirePOST
    // public HttpResponse doDoUninstall() throws IOException {
    // Jenkins jenkins = Jenkins.getActiveInstance();
    //
    // jenkins.checkPermission(Jenkins.ADMINISTER);
    // archive.delete();
    //
    // // Redo who depends on who.
    // jenkins.getPluginManager().resolveDependantPlugins();
    //
    // return HttpResponses.redirectViaContextPath("/pluginManager/installed");   // send back to plugin manager
    // }
    private static final Logger LOGGER = Logger.getLogger(PluginWrapper.class.getName());

    /**
     * Name of the plugin manifest file (to help find where we parse them.)
     */
    public static final String MANIFEST_FILENAME = "META-INF/MANIFEST.MF";
}
