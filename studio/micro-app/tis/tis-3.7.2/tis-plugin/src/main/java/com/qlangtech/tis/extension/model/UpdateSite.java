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
package com.qlangtech.tis.extension.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.ITPIArtifact;
import com.qlangtech.tis.extension.ITPIArtifactMatch;
import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.extension.PluginWrapper;
import com.qlangtech.tis.extension.plugins.DetachedPluginsUtil;
import com.qlangtech.tis.extension.util.TextFile;
import com.qlangtech.tis.extension.util.VersionNumber;
import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import com.qlangtech.tis.plugin.IPluginTaggable;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.Util;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.qlangtech.tis.util.MemoryReductionUtil.*;

//import edu.umd.cs.findbugs.annotations.CheckForNull;
//import edu.umd.cs.findbugs.annotations.NonNull;
//import edu.umd.cs.findbugs.annotations.Nullable;

//import edu.umd.cs.findbugs.annotations.CheckForNull;
//import edu.umd.cs.findbugs.annotations.NonNull;
//import edu.umd.cs.findbugs.annotations.Nullable;


//import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class UpdateSite {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateSite.class);
    /**
     * What's the time stamp of data file?
     * 0 means never.
     */
    private transient volatile long dataTimestamp;

    /**
     * When was the last time we asked a browser to check the data for us?
     * 0 means never.
     *
     * <p>
     * There's normally some delay between when we send HTML that includes the check code,
     * until we get the data back, so this variable is used to avoid asking too many browsers
     * all at once.
     */
    private transient volatile long lastAttempt;

    /**
     * If the attempt to fetch data fails, we progressively use longer time out before retrying,
     * to avoid overloading the server.
     */
    private transient volatile long retryWindow;

    /**
     * Latest data as read from the data file.
     */
    private transient Data data;
    /**
     * ID string for this update source.
     */
    private final String id;

    /**
     * Path to {@code update-center.json}, like {@code http://jenkins-ci.org/update-center.json}.
     */
    private final String url;

    public UpdateSite(String id, String url) {
        this.id = id;
        this.url = url;
    }


    /**
     * Update the data file from the given URL if the file
     * does not exist, or is otherwise due for update.
     * Accepted formats are JSONP or HTML with {@code postMessage}, not raw JSON.
     *
     * @return null if no updates are necessary, or the future result
     * @since 2.222
     */
    public Future<FormValidation> updateDirectly() {
        return updateDirectly(false);
    }


    /**
     * Update the data file from the given URL if the file
     * does not exist, or is otherwise due for update.
     * Accepted formats are JSONP or HTML with {@code postMessage}, not raw JSON.
     *
     * @param signatureCheck whether to enforce the signature (may be off only for testing!)
     * @return null if no updates are necessary, or the future result
     * @since 1.502
     * @deprecated use {@linkplain #updateDirectly()}
     */
    public Future<FormValidation> updateDirectly(final boolean signatureCheck) {
        if (!getDataFile().exists() || isDue()) {
            return UpdateCenter.updateService.submit(new Callable<FormValidation>() {
                @Override
                public FormValidation call() throws Exception {
                    TextFile faildSignal = getDataLoadFaildFile();
                    boolean faild = false;
                    try {
                        return updateDirectlyNow(signatureCheck);
                    } catch (Exception e) {
                        faild = true;
                        faildSignal.write(e.getMessage());
                        throw e;
                    } finally {
                        if (!faild) {
                            try {
                                faildSignal.delete();
                            } catch (Throwable e) {
                            }
                        }
                    }
                }
            });
        } else {
            return null;
        }
    }

    public Plugin getPlugin(String artifactId) {
        Data dt = getData();
        if (dt == null) return null;
        return dt.plugins.get(artifactId);
    }

    public String getUrl() {
        return this.url;
    }

    private FormValidation updateDirectlyNow(boolean signatureCheck) throws IOException {
        String siteCfgUrl = getUrl() + "?id=" + URLEncoder.encode(getId(), "UTF-8") + "&version=" + URLEncoder.encode(TIS.VERSION, "UTF-8");
        LOGGER.info("siteCfgUrl:{}", siteCfgUrl);
        return updateData(HttpUtils.get( //
                new URL(siteCfgUrl)//
                , new ConfigFileContext.StreamProcess<String>() {
                    @Override
                    public String p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                        try {
                            return IOUtils.toString(stream, TisUTF8.get());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }), signatureCheck);
    }

    private FormValidation updateData(String json, boolean signatureCheck)
            throws IOException {

        dataTimestamp = System.currentTimeMillis();

        JSONObject o = JSONObject.parseObject(json);

        try {
            int v = o.getIntValue("updateCenterVersion");
            if (v != 1) {
                throw new IllegalArgumentException("Unrecognized update center version: " + v);
            }
        } catch (JSONException x) {
            throw new IllegalArgumentException("Could not find (numeric) updateCenterVersion in " + json, x);
        }

        if (signatureCheck) {
//            FormValidation e = verifySignatureInternal(o);
//            if (e.kind!=Kind.OK) {
//                LOGGER.severe(e.toString());
//                return e;
//            }
        }

        LOGGER.info("Obtained the latest update center data file for UpdateSource " + id);
        retryWindow = 0;
        getDataFile().write(json);
        data = new Data(o);
        return FormValidation.ok();
    }

    /**
     * Returns true if it's time for us to check for new version.
     */
    public synchronized boolean isDue() {
//        if(neverUpdate)     return false;
//        if(dataTimestamp == 0)
//            dataTimestamp = getDataFile().file.lastModified();
//        long now = System.currentTimeMillis();
//
//        retryWindow = Math.max(retryWindow,SECONDS.toMillis(15));
//
//        boolean due = now - dataTimestamp > DAY && now - lastAttempt > retryWindow;
//        if(due) {
//            lastAttempt = now;
//            retryWindow = Math.min(retryWindow*2, HOURS.toMillis(1)); // exponential back off but at most 1 hour
//        }
//        return due;
        return false;
    }


    /**
     * Get ID string.
     */

    public String getId() {
        return this.id;
    }

    public String getConnectionCheckUrl() {
        Data dt = getData();
        if (dt == null) return "http://www.google.com/";
        return dt.connectionCheckUrl;
    }

    public static class Entry {
        public final String sourceId;
        /**
         * Artifact ID.
         */
        public final String name;
        /**
         * The version.
         */
        public final String version;


        //  private final Optional<PluginClassifier> classifier;

        Entry(String sourceId //, Optional<PluginClassifier> classifier
                , JSONObject o, String baseURL) {
            this.sourceId = sourceId;
            // this.classifier = Objects.requireNonNull(classifier, "classifier can not be null");
            this.name = Util.intern(o.getString("name"));
            this.version = Util.intern(o.getString("version"));
        }

//        @Override
//        public String getIdentityName() {
//            return this.name;
//        }

//        @Override
//        public Optional<PluginClassifier> getClassifier() {
//            return this.classifier;
//        }

        /**
         * Checks if the specified "current version" is older than the version of this entry.
         *
         * @param currentVersion The string that represents the version number to be compared.
         * @return true if the version listed in this entry is newer.
         * false otherwise, including the situation where the strings couldn't be parsed as version numbers.
         */
        public boolean isNewerThan(String currentVersion) {
            try {
                return new VersionNumber(currentVersion).compareTo(new VersionNumber(version)) < 0;
            } catch (IllegalArgumentException e) {
                // couldn't parse as the version number.
                return false;
            }
        }
    }

    /**
     * A version range for {@code Warning}s indicates which versions of a given plugin are affected
     * by it.
     * <p>
     * {@link #name}, {@link #firstVersion} and {@link #lastVersion} fields are only used for administrator notices.
     * <p>
     * The {@link #pattern} is used to determine whether a given warning applies to the current installation.
     *
     * @since 2.40
     */
    public static final class WarningVersionRange {
        /**
         * Human-readable English name for this version range, e.g. 'regular', 'LTS', '2.6 line'.
         */
        //@Nullable
        public final String name;

        /**
         * First version in this version range to be subject to the warning.
         */
        // @Nullable
        public final String firstVersion;

        /**
         * Last version in this version range to be subject to the warning.
         */
        // @Nullable
        public final String lastVersion;

        /**
         * Regular expression pattern for this version range that matches all included version numbers.
         */
        // @NonNull
        private final Pattern pattern;

        public WarningVersionRange(JSONObject o) {
            this.name = Util.fixEmpty(o.getString("name"));
            this.firstVersion = Util.intern(Util.fixEmpty(o.getString("firstVersion")));
            this.lastVersion = Util.intern(Util.fixEmpty(o.getString("lastVersion")));
            Pattern p;
            try {
                p = Pattern.compile(o.getString("pattern"));
            } catch (PatternSyntaxException ex) {
                LOGGER.info("Failed to compile pattern '" + o.getString("pattern") + "', using '.*' instead", ex);
                p = Pattern.compile(".*");
            }
            this.pattern = p;
        }

        public boolean includes(VersionNumber number) {
            return pattern.matcher(number.toString()).matches();
        }
    }

    static final Predicate<Object> IS_DEP_PREDICATE = x -> x instanceof JSONObject && get(((JSONObject) x), "name") != null;
    static final Predicate<Object> IS_NOT_OPTIONAL = x -> "false".equals(get(((JSONObject) x), "optional"));

    public abstract class Plugin extends Entry {

        /**
         * Optional URL to the Wiki page that discusses this plugin.
         */
        public final String wiki;
        /**
         * Human readable title of the plugin, taken from Wiki page.
         * Can be null.
         *
         * <p>
         * beware of XSS vulnerability since this data comes from Wiki
         */
        public final String title;
        /**
         * Optional excerpt string.
         */
        public final String excerpt;
        /**
         * Optional version # from which this plugin release is configuration-compatible.
         */
        public final String compatibleSinceVersion;
        /**
         * Version of Jenkins core this plugin was compiled against.
         */
        public final String requiredCore;
        /**
         * Version of Java this plugin requires to run.
         *
         * @since 2.158
         */
        public final String minimumJavaVersion;
        /**
         * Categories for grouping plugins, taken from labels assigned to wiki page.
         * Can be {@code null} if the update center does not return categories.
         */
        public final String[] categories;

        /**
         * Dependencies of this plugin, a name -&gt; version mapping.
         */
        private final Map<String, String> dependencies;


        public List<Option> getDependencies() {
            Option opt = null;
            List<Option> opts = Lists.newArrayList();
            for (Map.Entry<String, String> entry : dependencies.entrySet()) {
                opt = new Option(entry.getKey(), entry.getValue());
                opts.add(opt);
            }
            return opts;
        }

        public abstract boolean isMultiClassifier();

        public abstract List<IPluginCoord> getArts();

        /**
         * Optional dependencies of this plugin.
         */
        public final Map<String, String> optionalDependencies;

        /**
         * Set of plugins, this plugin is a incompatible dependency to.
         */
        private Set<Plugin> incompatibleParentPlugins;

        /**
         * Date when this plugin was released.
         *
         * @since 2.224
         */
        public final long releaseTimestamp;

        /**
         * Popularity of this plugin.
         *
         * @since 2.233
         */
        public final Double popularity;

        public final Map<String, List<String>> extendPoints;

        public final Set<String> endTypes;
        public final Set<IPluginTaggable.PluginTag> pluginTags;


        /**
         * The latest existing version of this plugin. May be different from the version being offered by the
         * update site, which will result in a notice on the UI.
         */
        public String latest;

        public Plugin(String sourceId, JSONObject o) {
            super(sourceId, o, UpdateSite.this.url);

            this.wiki = get(o, "wiki");
            this.title = get(o, "title");
            this.excerpt = StringUtils.trimToNull(get(o, "excerpt"));
            this.compatibleSinceVersion = Util.intern(get(o, "compatibleSinceVersion"));
            this.minimumJavaVersion = Util.intern(get(o, "minimumJavaVersion"));
            this.latest = get(o, "latest");
            this.requiredCore = Util.intern(get(o, "requiredCore"));
            this.releaseTimestamp = o.getLongValue("buildDate");

            JSONArray pluginTags = o.getJSONArray("pluginTags");
            if (pluginTags == null) {
                throw new IllegalStateException(JsonUtil.toString(o) + " lack relevant property pluginTags");
            }
            this.pluginTags = pluginTags.stream()
                    .map((t) -> IPluginTaggable.PluginTag.parse((String) t)).collect(Collectors.toSet());
            JSONArray endTypes = o.getJSONArray("endTypes");
            if (endTypes == null) {
                throw new IllegalStateException(JsonUtil.toString(o) + " lack relevant property endTypes");
            }
            this.endTypes = endTypes.stream().map((e) -> (String) e).collect(Collectors.toSet());

            JSONObject extendPoints = o.getJSONObject("extendPoints");
            this.extendPoints = Maps.newHashMap();
            if (extendPoints != null) {
                extendPoints.forEach((key, val) -> {
                    String extendpoint = key;
                    JSONArray impls = (JSONArray) val;
                    this.extendPoints.put(extendpoint, impls.toJavaList(String.class));
                });
            }
            // final String releaseTimestamp = get(o, "releaseTimestamp");
//            Date date = null;
//            if (releaseTimestamp != null) {
//                try {
//                    date = Date.from(Instant.parse(releaseTimestamp));
//                } catch (Exception ex) {
//                    LOGGER.info("Failed to parse releaseTimestamp for " + title + " from " + sourceId, ex);
//                }
//            }
            final String popularityFromJson = get(o, "popularity");
            Double popularity = 0.0;
            if (popularityFromJson != null) {
                try {
                    popularity = Double.parseDouble(popularityFromJson);
                } catch (NumberFormatException nfe) {
                    LOGGER.info("Failed to parse popularity: '" + popularityFromJson + "' for plugin " + this.title);
                }
            }
            this.popularity = popularity;
            // this.releaseTimestamp = date;

            JSONArray labels = o.getJSONArray("labels");
            if (labels != null) {
                this.categories = internInPlace((String[]) labels.toArray(EMPTY_STRING_ARRAY));
            } else {
                this.categories = null;
            }


            JSONArray ja = o.getJSONArray("dependencies");
            int depCount = (int) (ja.stream().filter(IS_DEP_PREDICATE.and(IS_NOT_OPTIONAL)).count());
            int optionalDepCount = (int) (ja.stream().filter(IS_DEP_PREDICATE.and(IS_NOT_OPTIONAL.negate())).count());
            dependencies = getPresizedMutableMap(depCount);
            optionalDependencies = getPresizedMutableMap(optionalDepCount);

            for (Object jo : ja) {
                JSONObject depObj = (JSONObject) jo;
                // Make sure there's a name attribute and that the optional value isn't true.
                String depName = Util.intern(get(depObj, "name"));
                if (depName != null) {
                    if (get(depObj, "optional").equals("false")) {
                        dependencies.put(depName, Util.intern(get(depObj, "version")));
                    } else {
                        optionalDependencies.put(depName, Util.intern(get(depObj, "version")));
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "{" +
                    "title='" + title + '\'' +
                    '}';
        }

        public Future<UpdateCenter.UpdateCenterJob> deploy(Optional<PluginClassifier> classifier) {
            return deploy(false, classifier);
        }

        public Future<UpdateCenter.UpdateCenterJob> deploy(boolean dynamicLoad, Optional<PluginClassifier> classifier) {
            return deploy(dynamicLoad, null, classifier, null);
        }

        /**
         * 安装插件
         * Schedules the installation of this plugin.
         *
         * <p>
         * This is mainly intended to be called from the UI. The actual installation work happens
         * asynchronously in another thread.
         *
         * @param dynamicLoad   If true, the plugin will be dynamically loaded into this Jenkins. If false,
         *                      the plugin will only take effect after the reboot.
         *                      See {@link 'UpdateCenter#isRestartRequiredForCompletion()'}
         * @param correlationId A correlation ID to be set on the job.
         * @param batch         if defined, a list of plugins to add to, which will be started later
         */
        public Future<UpdateCenter.UpdateCenterJob> deploy(boolean dynamicLoad, UUID correlationId
                , Optional<PluginClassifier> classifier, List<PluginWrapper> batch) {

            UpdateCenter uc = TIS.get().getUpdateCenter();
            for (Plugin dep : getNeededDependencies()) {
                UpdateCenter.InstallationJob job = uc.getJob(dep);
                if (job == null || job.status instanceof UpdateCenter.DownloadJob.Failure) {
                    LOGGER.info("Adding dependent install of " + dep.name + " for plugin " + name);
                    dep.deploy(dynamicLoad
                            , /* UpdateCenterPluginInstallTest.test_installKnownPlugins specifically asks that these not be correlated */ null
                            , classifier, batch);
                } else {
                    LOGGER.info("Dependent install of {} for plugin {} already added, skipping", dep.name, name);
                }
            }
            IPluginCoord coord = getTargetCoord(this, classifier);
            PluginWrapper pw = getInstalled();
            if (pw != null) { // JENKINS-34494 - check for this plugin being disabled
                Future<UpdateCenter.UpdateCenterJob> enableJob = null;
                if (!pw.isEnabled()) {
                    UpdateCenter.EnableJob job = uc.new EnableJob(UpdateSite.this, this, coord, dynamicLoad);
                    job.setCorrelationId(correlationId);
                    enableJob = uc.addJob(job);
                }
                if (pw.getVersionNumber().equals(new VersionNumber(version))) {
                    return enableJob != null ? enableJob : uc.addJob(uc.new NoOpJob(UpdateSite.this, this, coord));
                }
            }
            UpdateCenter.InstallationJob job = createInstallationJob(this, coord, uc, dynamicLoad);
            job.setCorrelationId(correlationId);
            job.setBatch(batch);
            return uc.addJob(job);
        }


        @JSONField(serialize = false)
        public List<Pair<Plugin, VersionNumber>> getDependencyPlugins() {
            List<Pair<Plugin, VersionNumber>> deps = new ArrayList<>();
            for (Map.Entry<String, String> e : dependencies.entrySet()) {
                VersionNumber requiredVersion = e.getValue() != null ? new VersionNumber(e.getValue()) : null;
                Plugin depPlugin = TIS.get().getUpdateCenter().getPlugin(e.getKey(), requiredVersion);
                if (depPlugin == null) {
                    LOGGER.warn("Could not find dependency {} of {}", e.getKey(), name);
                    continue;
                }

                // Is the plugin installed already? If not, add it.
                deps.add(Pair.of(depPlugin, requiredVersion));
//                if (depPlugin.needInstall(requiredVersion)) {
//                    deps.add(depPlugin);
//                }

//                PluginWrapper current = depPlugin.getInstalled();
//
//                if (current == null) {
//                    deps.add(depPlugin);
//                }
//                // If the dependency plugin is installed, is the version we depend on newer than
//                // what's installed? If so, upgrade.
//                else if (current.isOlderThan(requiredVersion)) {
//                    deps.add(depPlugin);
//                }
//                // JENKINS-34494 - or if the plugin is disabled, this will allow us to enable it
//                else if (!current.isEnabled()) {
//                    deps.add(depPlugin);
//                }
            }
            return deps;
        }

        /**
         * Returns a list of dependent plugins which need to be installed or upgraded for this plugin to work.
         */
        public List<Plugin> getNeededDependencies() {
            List<Plugin> deps = new ArrayList<>();


            for (Pair<Plugin, VersionNumber> depPlugin : getDependencyPlugins()) {


//                VersionNumber requiredVersion = e.getValue() != null ? new VersionNumber(e.getValue()) : null;
//                Plugin depPlugin = TIS.get().getUpdateCenter().getPlugin(e.getKey(), requiredVersion);
//                if (depPlugin == null) {
//                    LOGGER.warn("Could not find dependency {} of {}", e.getKey(), name);
//                    continue;
//                }

                // Is the plugin installed already? If not, add it.

                if (depPlugin.getLeft().needInstall(depPlugin.getRight())) {
                    deps.add(depPlugin.getLeft());
                }

//                PluginWrapper current = depPlugin.getInstalled();
//
//                if (current == null) {
//                    deps.add(depPlugin);
//                }
//                // If the dependency plugin is installed, is the version we depend on newer than
//                // what's installed? If so, upgrade.
//                else if (current.isOlderThan(requiredVersion)) {
//                    deps.add(depPlugin);
//                }
//                // JENKINS-34494 - or if the plugin is disabled, this will allow us to enable it
//                else if (!current.isEnabled()) {
//                    deps.add(depPlugin);
//                }
            }

            for (Map.Entry<String, String> e : optionalDependencies.entrySet()) {
                VersionNumber requiredVersion = e.getValue() != null ? new VersionNumber(e.getValue()) : null;
                Plugin depPlugin = TIS.get().getUpdateCenter().getPlugin(e.getKey(), requiredVersion);
                if (depPlugin == null) {
                    continue;
                }

                PluginWrapper current = depPlugin.getInstalled();

                // If the optional dependency plugin is installed, is the version we depend on newer than
                // what's installed? If so, upgrade.
                if (current != null && current.isOlderThan(requiredVersion)) {
                    deps.add(depPlugin);
                }
            }

            return deps;
        }

        /**
         * Checks whether a plugin has a desired category
         *
         * @since 2.272
         */
        public boolean hasCategory(String category) {
            if (categories == null) {
                return false;
            }
            // TODO: cache it in a hashset for performance improvements
            return Arrays.asList(categories).contains(category);
        }

        @JSONField(serialize = false)
        public PluginWrapper getInstalled() {
            PluginManager pm = TIS.get().getPluginManager();
            PluginWrapper plugin = null;
            for (ITPIArtifact art : this.getArts()) {
                plugin = pm.getPlugin(ITPIArtifact.matchh(art));
                if (plugin != null) {
                    return plugin;
                }
            }

            return null;
        }

        private boolean needInstall(VersionNumber requiredVersion) {
            PluginWrapper current = this.getInstalled();

            if (current == null) {
                return true;
            }
            // If the dependency plugin is installed, is the version we depend on newer than
            // what's installed? If so, upgrade.
            else if (current.isOlderThan(requiredVersion)) {
                return true;
            }
            // JENKINS-34494 - or if the plugin is disabled, this will allow us to enable it
            else if (!current.isEnabled()) {
                return true;
            }

            return false;
        }


        public String getDisplayName() {
            String displayName;
            if (title != null) {
                displayName = title;
            } else {
                displayName = name;
            }
            return StringUtils.removeStart(displayName, "TIS ");
        }
    }


    protected UpdateCenter.InstallationJob createInstallationJob(Plugin plugin
            , IPluginCoord coord, UpdateCenter uc, boolean dynamicLoad) {
        // IPluginCoord coord = getTargetCoord(plugin, classifier);
        return uc.new InstallationJob(plugin, coord, this, dynamicLoad);
    }

    private IPluginCoord getTargetCoord(Plugin plugin, Optional<PluginClassifier> classifier) {
        List<IPluginCoord> arts = plugin.getArts();
        IPluginCoord coord = null;
        if (classifier.isPresent()) {
            ITPIArtifactMatch match = ITPIArtifact.matchh(plugin.getDisplayName(), classifier);
            match.setIdentityName(plugin.getDisplayName());
            for (IPluginCoord c : arts) {
                if (ITPIArtifact.isEquals(c, match)) {
                    coord = c;
                    break;
                }
            }
        } else {
            for (IPluginCoord c : arts) {
                coord = c;
                break;
            }
        }
        Objects.requireNonNull(coord, "plugin:" + plugin.getDisplayName()
                + (classifier.isPresent() ? ",for classifier:" + classifier.get().getClassifier() : StringUtils.EMPTY) + ",coord can not be null");
        return coord;
    }

    private static String get(JSONObject o, String prop) {

        return o.getString(prop);
//            if(o.has(prop))
//                return o.getString(prop);
//            else
//                return null;
    }

    public Plugin createPlugin(final String sourceId, boolean supportMultiClassifier
            , JSONObject pluginMeta, List<DftCoord> classifiers) {

        if (supportMultiClassifier) {
            return new MultiClassifierPlugin(sourceId, classifiers, pluginMeta);
        } else {
            return new NoneClassifierPlugin(sourceId, pluginMeta);
        }
    }

    private static class DftCoord implements IPluginCoord {
        private final URL downloadUrl;
        private final String sha1;
        private final String sha256;
        private final long size;

        private final PluginClassifier classifier;
        private final String pluginName;
        private final String sizeLiteral;

        public DftCoord(String pluginName, JSONObject meta) {

//            {
//                "classifier":"hadoop_2.7.3",
//                    "gav":"com.qlangtech.tis.plugins:tis-datax-hdfs-plugin_hadoop_2.7.3:3.6.0",
//                    "sha1":"",
//                    "sha256":"",
//                    "size":49873753,
//                    "url":"http://mirror.qlangtech.com/3.6.0/tis-plugin/tis-datax-hdfs-plugin/tis-datax-hdfs-plugin_hadoop_2.7.3.tpi"
//            },
            this.pluginName = pluginName;
            this.classifier = PluginClassifier.create(meta.getString("classifier"));
            this.sha1 = meta.getString("sha1");
            this.sha256 = meta.getString("sha256");
            this.size = meta.getLongValue("size");

            this.sizeLiteral = FileUtils.byteCountToDisplaySize(this.size);

            try {
                this.downloadUrl = new URL(meta.getString("downloadUrl"));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        public String getSizeLiteral() {
            return this.sizeLiteral;
        }

        @Override
        @JSONField(serialize = false)
        public String getIdentityName() {
            //  throw new UnsupportedOperationException();
            return this.pluginName;
        }

        @Override
        @JSONField(serialize = false)
        public Optional<PluginClassifier> getClassifier() {
            return Optional.of(this.classifier);
        }

        public String getClassifierName() {
            return this.classifier.getClassifier();
        }


        @Override
        public URL getDownloadUrl() throws MalformedURLException {
            return this.downloadUrl;
        }

        @Override
        public String getSha1() throws IOException {
            return this.sha1;
        }

        @Override
        public String getSha256() throws IOException {
            return this.sha256;
        }

        @Override
        public long getSize() throws IOException {
            return this.size;
        }

        @Override
        public String getGav() {
            return getClassifierName();
        }
    }

    private class NoneClassifierPlugin extends Plugin implements IPluginCoord {

        /**
         * Download URL.
         */
        public final URL url;
        // non-private, non-final for test
        /* final */ String sha1;

        /* final */ String sha256;

        /* final */ String sha512;

        public final String sizeLiteral;
        public final long size;

        public NoneClassifierPlugin(String sourceId, JSONObject o) {
            super(sourceId, o);
            String baseURL = UpdateSite.this.url;
            // Trim this to prevent issues when the other end used Base64.encodeBase64String that added newlines
            // to the end in old commons-codec. Not the case on updates.jenkins-ci.org, but let's be safe.
            this.sha1 = Util.fixEmptyAndTrim(o.getString("sha1"));
            this.sha256 = Util.fixEmptyAndTrim(o.getString("sha256"));
            this.sha512 = Util.fixEmptyAndTrim(o.getString("sha512"));

            try {
                this.url = new URL(o.getString("url"));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            this.size = o.getLongValue("size");
            this.sizeLiteral = FileUtils.byteCountToDisplaySize(this.size);
//            if (!URI.create(url).isAbsolute()) {
//                if (baseURL == null) {
//                    throw new IllegalArgumentException("Cannot resolve " + url + " without a base URL");
//                }
//                url = URI.create(baseURL).resolve(url).toString();
//            }
            // this.url = url;
        }

        @Override
        @JSONField(serialize = false)
        public List<IPluginCoord> getArts() {
            return Collections.singletonList(this);
        }

        @Override
        public boolean isMultiClassifier() {
            return false;
        }

        @Override
        public String getIdentityName() {
            //throw new UnsupportedOperationException();
            return this.name;
        }

        @Override
        public URL getDownloadUrl() throws MalformedURLException {
            return this.url; //new URL(this.url);
        }

        @Override
        public String getSha1() throws IOException {
            return this.sha1;
        }

        @Override
        public String getSha256() throws IOException {
            return this.sha256;
        }

        @Override
        public long getSize() throws IOException {
            return size;
        }

        @Override
        public String getGav() {
            return null;
        }
    }

    public static ThreadLocal<ConcurrentHashMap<String, List<IPluginCoord>>> pluginArts = new ThreadLocal<ConcurrentHashMap<String, List<IPluginCoord>>>() {
        @Override
        protected ConcurrentHashMap<String, List<IPluginCoord>> initialValue() {
            return new ConcurrentHashMap<>();
        }
    };

    private class MultiClassifierPlugin extends Plugin {

        private final List<DftCoord> coords;


        public MultiClassifierPlugin(String sourceId, List<DftCoord> coords, JSONObject o) {
            super(sourceId, o);
            if (CollectionUtils.isEmpty(coords)) {
                throw new IllegalArgumentException("param coords can not be empty");
            }
            this.coords = coords;
        }

        @Override
        public boolean isMultiClassifier() {
            return true;
        }


        @Override
        @JSONField(serialize = true)
        public List<IPluginCoord> getArts() {
            return pluginArts.get().computeIfAbsent(this.getDisplayName(), (key) -> {
                Plugin plugin = null;
                // 如果被依赖的插件已经先安装上上了，那么，新加的差价必须要和被依赖的插件的 classifier相一致
                // PluginWrapper installed = null;
                List<ITPIArtifact> installCoords = Lists.newArrayList();
                getDepInstallCoords(installCoords, MultiClassifierPlugin.this);

                Optional<ITPIArtifact> containClassifier
                        = installCoords.stream().filter((c) -> c.getClassifier().isPresent()).findAny();

                if (CollectionUtils.isEmpty(installCoords) || !containClassifier.isPresent()) {
                    return coords.stream().collect(Collectors.toList());
                } else {
                    final String id = "merge";
                    Optional<PluginClassifier> depClassifier = null;
                    PluginClassifier c = null;
                    Map<String, String> dimension = Maps.newHashMap();
                    for (ITPIArtifact dep : installCoords) {
                        depClassifier = dep.getClassifier();
                        if (depClassifier.isPresent()) {
                            c = depClassifier.get();
                            dimension.putAll(c.dimensionMap());
                        }
                    }
                    PluginClassifier.validateDimension(dimension);

                    ITPIArtifact merge = new ITPIArtifact() {
                        @Override
                        public String getIdentityName() {
                            return id;
                        }

                        @Override
                        public Optional<PluginClassifier> getClassifier() {
                            return Optional.of(new PluginClassifier(dimension));
                        }
                    };

                    return coords.stream().filter((coord) -> {
                        ITPIArtifactMatch matchh = ITPIArtifact.matchh(this.getDisplayName(), coord.getClassifier());
                        matchh.setIdentityName(id);
                        return ITPIArtifact.isEquals(merge, matchh);
                    }).collect(Collectors.toList());
                }
            });


        }

        private void getDepInstallCoords(List<ITPIArtifact> installCoords, Plugin p) {
            Plugin plugin = null;
            for (Pair<Plugin, VersionNumber> depPlugin : p.getDependencyPlugins()) {
                // Is the plugin installed already? If not, add it.
                plugin = depPlugin.getLeft();
                if (!plugin.needInstall(depPlugin.getRight())) {
                    installCoords.add(plugin.getInstalled());
                }
                getDepInstallCoords(installCoords, plugin);
            }
        }
    }

    /**
     * In-memory representation of the update center data.
     */
    public final class Data {
        /**
         * The {@link UpdateSite} ID.
         */
        public final String sourceId;

        /**
         * The latest jenkins.war.
         */
        public final Entry core;
        /**
         * Plugins in the repository, keyed by their artifact IDs.
         */
        public final Map<String, Plugin> plugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        /**
         * List of warnings (mostly security) published with the update site.
         *
         * @since 2.40
         */
        private final Set<Warning> warnings = new HashSet<>();

        /**
         * Mapping of plugin IDs to deprecation notices
         *
         * @since 2.246
         */
        private final Map<String, Deprecation> deprecations = new HashMap<>();

        /**
         * If this is non-null, Jenkins is going to check the connectivity to this URL to make sure
         * the network connection is up. Null to skip the check.
         */
        public final String connectionCheckUrl;

        Data(JSONObject o) {
            this.sourceId = Util.intern((String) o.get("id"));
            JSONObject c = o.getJSONObject("core");
            if (c != null) {
                core = new Entry(sourceId, c, url);
            } else {
                core = null;
            }

            JSONArray w = o.getJSONArray("warnings");
            if (w != null) {
                for (int i = 0; i < w.size(); i++) {
                    try {
                        warnings.add(new Warning(w.getJSONObject(i)));
                    } catch (Exception ex) {
                        LOGGER.warn("Failed to parse JSON for warning", ex);
                    }
                }
            }

            JSONObject deprecations = o.getJSONObject("deprecations");
            if (deprecations != null) {
                for (String pluginId : deprecations.keySet()) {
                    try {

                        JSONObject entry = deprecations.getJSONObject(pluginId); // additional level of indirection to support future extensibility
                        if (entry != null) {
                            String referenceUrl = entry.getString("url");
                            if (referenceUrl != null) {
                                this.deprecations.put(pluginId, new Deprecation(referenceUrl));
                            }
                        }
                    } catch (RuntimeException ex) {
                        LOGGER.info("Failed to parse JSON for deprecation", ex);
                    }
                }
            }

            JSONObject plugins = o.getJSONObject("plugins");
            JSONObject pluginMeta = null;
            List<DftCoord> classifiers = null;
            boolean supportMultiClassifier;
            for (Map.Entry<String, Object> e : plugins.entrySet()) {
                pluginMeta = (JSONObject) e.getValue();
                classifiers = Lists.newArrayList();

                supportMultiClassifier = pluginMeta.getBooleanValue("supportMultiClassifier");
                JSONArray classifierInfo = pluginMeta.getJSONArray(PluginManager.PACAKGE_CLASSIFIER);
                if (supportMultiClassifier && classifierInfo != null && classifierInfo.size() > 0) {
                    classifiers = classifierInfo.stream()
                            .map((i) -> {
                                return (new DftCoord(e.getKey(), (JSONObject) i));
                            }).collect(Collectors.toList());
                }

                // for (Optional<PluginClassifier> classifier : classifiers) {
                // new Plugin(sourceId, classifier, pluginMeta);
                Plugin p = createPlugin(sourceId, supportMultiClassifier, pluginMeta, classifiers);
                // JENKINS-33308 - include implied dependencies for older plugins that may need them
                List<PluginWrapper.Dependency> implicitDeps = DetachedPluginsUtil.getImpliedDependencies(p.name, p.requiredCore);
                if (!implicitDeps.isEmpty()) {
                    for (PluginWrapper.Dependency dep : implicitDeps) {
                        if (!p.dependencies.containsKey(dep.shortName)) {
                            p.dependencies.put(dep.shortName, dep.version);
                        }
                    }
                }
                this.plugins.put(Util.intern(e.getKey()), p);

                // compatibility with update sites that have no separate 'deprecated' top-level entry.
                // Also do this even if there are deprecations to potentially allow limiting the top-level entry to overridden URLs.
                if (p.hasCategory("deprecated")) {
                    if (!this.deprecations.containsKey(p.name)) {
                        this.deprecations.put(p.name, new Deprecation(p.wiki));
                    }
                }
                //}
            }

            connectionCheckUrl = (String) o.get("connectionCheckUrl");
        }


        /**
         * Returns the set of warnings
         *
         * @return the set of warnings
         * @since 2.40
         */

        public Set<Warning> getWarnings() {
            return this.warnings;
        }

        /**
         * Returns the deprecations provided by the update site
         *
         * @return the deprecations provided by the update site
         * @since 2.246
         */
        public Map<String, Deprecation> getDeprecations() {
            return this.deprecations;
        }

        /**
         * Is there a new version of the core?
         */
        public boolean hasCoreUpdates() {
            return core != null && core.isNewerThan(TIS.VERSION);
        }

        /**
         * Do we support upgrade?
         */
        public boolean canUpgrade() {
            return true;//Lifecycle.get().canRewriteHudsonWar();
        }
    }

    /**
     * Represents a warning about a certain component, mostly related to known security issues.
     *
     * @see 'UpdateSiteWarningsConfiguration'
     * @see 'jenkins.security.UpdateSiteWarningsMonitor'
     * @since 2.40
     */
    public static final class Warning {

        public enum Type {
            CORE,
            PLUGIN,
            UNKNOWN
        }

        /**
         * The type classifier for this warning.
         */

        public /* final */ Type type;

        /**
         * The globally unique ID of this warning.
         *
         * <p>This is typically the CVE identifier or SECURITY issue (Jenkins project);
         * possibly with a unique suffix (e.g. artifactId) if either applies to multiple components.</p>
         */
        public final String id;

        /**
         * The name of the affected component.
         * <ul>
         *   <li>If type is 'core', this is 'core' by convention.
         *   <li>If type is 'plugin', this is the artifactId of the affected plugin
         * </ul>
         */
        public final String component;

        /**
         * A short, English language explanation for this warning.
         */
        public final String message;

        /**
         * A URL with more information about this, typically a security advisory. For use in administrator notices
         * only, so
         */
        public final String url;

        /**
         * A list of named version ranges specifying which versions of the named component this warning applies to.
         * <p>
         * If this list is empty, all versions of the component are considered to be affected by this warning.
         */
        public final List<WarningVersionRange> versionRanges;

        /**
         * @param o the {@link JSONObject} representing the warning
         * @throws if the argument does not match the expected format
         */
        public Warning(JSONObject o) {
            try {
                this.type = Type.valueOf(o.getString("type").toUpperCase(Locale.US));
            } catch (IllegalArgumentException ex) {
                this.type = Type.UNKNOWN;
            }
            this.id = o.getString("id");
            this.component = Util.intern(o.getString("name"));
            this.message = o.getString("message");
            this.url = o.getString("url");

            JSONArray versions = o.getJSONArray("versions");
            if (versions != null) {
//                JSONArray versions = o.getJSONArray("versions");
                List<WarningVersionRange> ranges = new ArrayList<>(versions.size());
                for (int i = 0; i < versions.size(); i++) {
                    WarningVersionRange range = new WarningVersionRange(versions.getJSONObject(i));
                    ranges.add(range);
                }
                this.versionRanges = Collections.unmodifiableList(ranges);
            } else {
                this.versionRanges = Collections.emptyList();
            }
        }

        /**
         * Two objects are considered equal if they are the same type and have the same ID.
         *
         * @param o the other object
         * @return true iff this object and the argument are considered equal
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Warning)) return false;

            Warning warning = (Warning) o;

            return id.equals(warning.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        public boolean isPluginWarning(String pluginName) {
            return type == Type.PLUGIN && pluginName.equals(this.component);
        }

        /**
         * Returns true if this warning is relevant to the current configuration
         *
         * @return true if this warning is relevant to the current configuration
         */
        public boolean isRelevant() {
            switch (this.type) {
                case CORE:
                    VersionNumber current = TIS.getVersion();

                    if (!isRelevantToVersion(current)) {
                        return false;
                    }
                    return true;
                case PLUGIN:

                    // check whether plugin is installed
                    PluginWrapper plugin = com.qlangtech.tis.TIS.get().getPluginManager().getPlugin(ITPIArtifact.create(this.component));
                    if (plugin == null) {
                        return false;
                    }

                    // check whether warning is relevant to installed version
                    VersionNumber currentCore = plugin.getVersionNumber();
                    if (!isRelevantToVersion(currentCore)) {
                        return false;
                    }
                    return true;
                case UNKNOWN:
                default:
                    return false;
            }
        }

        public boolean isRelevantToVersion(VersionNumber version) {
            if (this.versionRanges.isEmpty()) {
                // no version ranges specified, so all versions are affected
                return true;
            }

            for (UpdateSite.WarningVersionRange range : this.versionRanges) {
                if (range.includes(version)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * This is where we store the update center data.
     */
    private TextFile getDataFile() {
        return new TextFile(getFile(getId() + ".json"));
    }

    private static File getFile(String name) {
        return new File(TIS.pluginCfgRoot, "updates/" + name);
    }

    public TextFile getDataLoadFaildFile() {
        return new TextFile(getFile(getId() + ".error"));
    }

    public boolean existLocal() {
        return getDataFile().exists();
    }

    public JSONObject getJSONObject() {
        TextFile df = getDataFile();
        if (df.exists()) {
            long start = System.nanoTime();
            try {
                JSONObject o = JSONObject.parseObject(df.read());
                LOGGER.info(String.format("Loaded and parsed %s in %.01fs", df, (System.nanoTime() - start) / 1_000_000_000.0));
                return o;
            } catch (JSONException | IOException e) {
                LOGGER.error("Failed to parse " + df, e);
                df.delete(); // if we keep this file, it will cause repeated failures
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Loads the update center data, if any.
     *
     * @return null if no data is available.
     */

    public Data getData() {
        if (data == null) {
            JSONObject o = getJSONObject();
            if (o != null) {
                data = new Data(o);
            }
        }
        return data;
    }

    /**
     * Returns a list of plugins that should be shown in the "available" tab.
     * These are "all plugins - installed plugins".
     */

    public List<Plugin> getAvailables() {
        List<Plugin> r = new ArrayList<>();
        Data data = getData();
        if (data == null) return Collections.emptyList();
        for (Plugin p : data.plugins.values()) {
            if (p.getInstalled() == null) {
                r.add(p);
            }
        }
        r.sort((plugin, t1) -> {
            final int pop = t1.popularity.compareTo(plugin.popularity);
            if (pop != 0) {
                return pop; // highest popularity first
            }
            return plugin.getDisplayName().compareTo(t1.getDisplayName());
        });
        return r;
    }

    /**
     * Represents a deprecation of a certain component. Jenkins project policy determines exactly what it means.
     *
     * @since 2.246
     */
    public static final class Deprecation {
        /**
         * URL for this deprecation.
         * <p>
         * Jenkins will show a link to this URL when displaying the deprecation message.
         */
        public final String url;

        public Deprecation(String url) {
            this.url = url;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Deprecation that = (Deprecation) o;
            return Objects.equals(url, that.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url);
        }
    }

    /**
     * Is this the legacy default update center site?
     */
    public boolean isLegacyDefault() {
        return true;//isHudsonCI() || isUpdatesFromHudsonLabs();
    }
}
