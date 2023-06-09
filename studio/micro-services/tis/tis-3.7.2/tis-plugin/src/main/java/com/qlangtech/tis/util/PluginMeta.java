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

package com.qlangtech.tis.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.ITPIArtifact;
import com.qlangtech.tis.extension.ITPIArtifactMatch;
import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.extension.PluginWrapper;
import com.qlangtech.tis.extension.impl.ClassicPluginStrategy;
import com.qlangtech.tis.extension.impl.PluginManifest;
import com.qlangtech.tis.extension.model.IPluginCoord;
import com.qlangtech.tis.extension.model.UpdateSite;
import com.qlangtech.tis.extension.util.VersionNumber;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-05 15:50
 **/
public class PluginMeta {

    public static final String NAME_VER_SPLIT = "@";
    public static final String ClASSIFIER_SPLIT = "!";

    private static final Pattern PATTERN_META
            = Pattern.compile("([^" + NAME_VER_SPLIT + "]+?)" + NAME_VER_SPLIT + "([^" + NAME_VER_SPLIT + "]+?)" + "(" + NAME_VER_SPLIT + "([^" + NAME_VER_SPLIT + "]+?))?" + "(" + ClASSIFIER_SPLIT + "(\\S+))?");

    private final String name;

    public final VersionNumber ver;

    protected Long lastModifyTimeStamp;

    public Optional<PluginClassifier> classifier;

    public String getPluginPackageName() {
        if (classifier.isPresent()) {
            return classifier.get().getTPIPluginName(this.name, PluginManager.PACAKGE_TPI_EXTENSION);
        }
        return this.name + PluginManager.PACAKGE_TPI_EXTENSION;
    }

    public void setClassifier(PluginClassifier classifier) {
        this.classifier = Optional.of(
                Objects.requireNonNull(classifier, "classifier can not be null"));
    }

    public File getPluginPackageFile() {
        return new File(TIS.pluginDirRoot, this.getPluginPackageName());
    }

    public PluginMeta(String name, String ver, Optional<PluginClassifier> classifier) {
        this(name, ver, classifier, null);
    }

    public PluginMeta(String name, String ver, Optional<PluginClassifier> classifier, Long lastModifyTimeStamp) {
        this.name = name;
        this.ver = new VersionNumber(ver);
        this.lastModifyTimeStamp = lastModifyTimeStamp;
        this.classifier = classifier;
    }

    @Override
    public boolean equals(Object o) {
        return this.hashCode() == o.hashCode();
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(getKey());
        this.getLastModifyTimeStamp();
        if (lastModifyTimeStamp != null) {
            buffer.append(NAME_VER_SPLIT).append(this.lastModifyTimeStamp);
        }
        if (this.classifier.isPresent()) {
            buffer.append(ClASSIFIER_SPLIT).append(this.classifier.get().getClassifier());
        }
        return buffer.toString();
    }

    public ITPIArtifactMatch createPluginMatcher() {
        ITPIArtifactMatch match = ITPIArtifact.create(this.getPluginName(), this.classifier);
        return match;
    }

    public String getKey() {
        return (new StringBuffer(name + NAME_VER_SPLIT + ver)).toString();
    }

    public String getPluginName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ver);
    }

    public static List<PluginMeta> parse(String attribute) {
        String[] metaArray = StringUtils.split(attribute, ",");
        return parse(metaArray, true);
    }

    public static List<PluginMeta> parse(String[] metaArray) {
        return parse(metaArray, false);
    }

    private static List<PluginMeta> parse(String[] metaArray, final boolean resolveDependencyFromPluginCenter) {
        if (metaArray == null) {
            throw new IllegalArgumentException("param metaArray can not be null");
        }
        final Map<String, PluginMeta> result = Maps.newHashMap();
        // String[] metaArray = StringUtils.split(attribute, ",");
        Optional<PluginClassifier> classifier = null;
        Matcher match = null;
        PluginWrapper pw = null;
        for (String meta : metaArray) {
            match = PATTERN_META.matcher(meta);
            if (match.matches()) {
                String pluginName = match.group(1);
                String ver = match.group(2);
                classifier = Optional.empty();
                String c, l = null;
                Long lastModify = null;
                if (match.groupCount() < 6) {
                    throw new IllegalStateException("groupCount must be 6:" + match + " but now is " + match.groupCount());
                }
                if (StringUtils.isNotEmpty(l = match.group(4))) {
                    lastModify = Long.parseLong(l);
                }
                if (StringUtils.isNotEmpty(c = match.group(6))) {
                    classifier = Optional.of(PluginClassifier.create(c));
                } else {
                    TIS tis = TIS.get();
                    // 可能TIS还没有初始化
                    Map<String, PluginWrapper> plugins
                            = (tis != null) ? tis.getPluginManager().getActivePluginsMap() : Collections.emptyMap();
                    if ((pw = plugins.get(pluginName)) != null) {
                        classifier = pw.getClassifier();
                    }
                }
                result.put(pluginName, new PluginMeta(pluginName, ver, classifier, lastModify) {
                    @Override
                    public List<PluginMeta> getMetaDependencies() {
                        if (resolveDependencyFromPluginCenter) {
                            return super.getMetaDependencies();
                        } else {
                            return processJarManifest((mfst) -> {
                                if (mfst == null) {
                                    return Collections.emptyList();
                                }
                                ClassicPluginStrategy.DependencyMeta dpts = mfst.getDependencyMeta();
                                return dpts.dependencies.stream()
                                        .map((d) -> {
                                            // PluginClassifier c = null;
                                            PluginMeta mt = result.get(d.shortName);
                                            //return Objects.requireNonNull(, "pluginName:" + d.shortName + " relevant pluginMeta shall not be empty");
                                            if (mt == null) {
                                                throw new IllegalStateException("pluginName:" + d.shortName
                                                        + " relevant pluginMeta shall not be empty, all:"
                                                        + result.keySet().stream().collect(Collectors.joining(",")));
                                            }
                                            return mt;
                                            //return new PluginMeta(d.shortName, d.version, dc);
                                        })
                                        .collect(Collectors.toList());
                            });
                        }
                    }
                });
                // result.add(new PluginMeta(pluginName, ver, classifier, lastModify));
            } else {
                throw new IllegalArgumentException("attri is invalid:" + Arrays.stream(metaArray).collect(Collectors.joining(",")));
            }
        }
        return Lists.newArrayList(result.values());
    }

    /**
     * plugin的最终打包时间
     *
     * @return
     */
    public long getLastModifyTimeStamp() {
        if (lastModifyTimeStamp != null) {
            return this.lastModifyTimeStamp;
        }
        return this.lastModifyTimeStamp = processJarManifest((mfst) ->
                (mfst == null) ? -1 : mfst.getLastModfiyTime()
        );
    }

    public boolean isLastModifyTimeStampNull() {
        return lastModifyTimeStamp == null;
    }

    public List<PluginMeta> getMetaDependencies() {

        return processJarManifest((mfst) -> {
            if (mfst == null) {
                return Collections.emptyList();
            }
            String pluginName = this.getPluginName();
            ITPIArtifactMatch from = ITPIArtifact.matchh(pluginName, classifier);
            ClassicPluginStrategy.DependencyMeta dpts = mfst.getDependencyMeta();
            return dpts.dependencies.stream()
                    .map((d) -> {
                        // PluginClassifier c = null;
                        Optional<PluginClassifier> dc = Optional.empty();
                        if (classifier.isPresent()) {
                            //   c = classifier.get();
                            from.setIdentityName(d.shortName);
                            UpdateSite.Plugin depPlugin = TIS.get().getUpdateCenter().getPlugin(d.shortName);
                            Objects.requireNonNull(depPlugin, "plugin:" + d.shortName
                                    + " from " + pluginName + ",classifier:" + classifier.get().getClassifier() + " relevant plugin meta can not be null");
                            if (depPlugin.isMultiClassifier()) {
                                boolean found = false;
                                for (IPluginCoord coord : depPlugin.getArts()) {
                                    if (ITPIArtifact.isEquals(coord, from)) {
                                        dc = coord.getClassifier();
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    throw new IllegalStateException(from.toString() + " can not find relevant classifer in " + depPlugin.getDisplayName());
                                }
                            }
                        }

                        return new PluginMeta(d.shortName, d.version, dc);
                    })
                    .collect(Collectors.toList());
        });
    }

    protected <R> R processJarManifest(Function<PluginManifest, R> manProcess) {
        File f = getPluginPackageFile();
//            if (!f.exists()) {
//                // throw new IllegalStateException("file:" + f.getPath() + " is not exist");
//                return manProcess.apply(null);
//            }
        PluginManifest manifest = PluginManifest.create(f);
        if (manifest == null) {
            return manProcess.apply(null);
        }

        return manProcess.apply(manifest);

//            try (JarInputStream tpiFIle = new JarInputStream(FileUtils.openInputStream(f), false)) {
//                Manifest mfst = tpiFIle.getManifest();
//                return manProcess.apply(mfst);
//            } catch (Exception e) {
//                throw new RuntimeException("tpi path:" + f.getAbsolutePath(), e);
//            }
    }

    public static void main(String[] args) throws Exception {
        File f = new File("/Users/mozhenghua/j2ee_solution/project/plugins/tis-datax/tis-datax-hudi-plugin/target/tis-datax-hudi-plugin.tpi");
        try (JarFile tpiFIle = new JarFile(f, false)) {
            tpiFIle.stream().forEach((e) -> System.out.println(e.getName()));
        }
    }


    public boolean copyFromRemote() {
        return copyFromRemote(Lists.newArrayList());
    }

    public boolean copyFromRemote(List<File> pluginFileCollector) {
        return copyFromRemote(pluginFileCollector, false, false);
    }

    /**
     * 将远端插件拷贝到本地
     */
    public boolean copyFromRemote(List<File> pluginFileCollector, boolean ignoreDependencies, boolean directDownload) {
        final URL url = CenterResource.getPathURL(Config.SUB_DIR_LIBS + "/" + TIS.KEY_TIS_PLUGIN_ROOT + "/" + this.getPluginPackageName());
        final File local = getPluginPackageFile();
        boolean updated = CenterResource.copyFromRemote2Local(url, local, directDownload);
        if (!ignoreDependencies && updated) {
            for (PluginMeta d : this.getMetaDependencies()) {
                d.copyFromRemote(pluginFileCollector);
            }
            pluginFileCollector.add(local);
        }
        return updated;
    }

//        public void install() {
//            try {
//                if (!TIS.permitInitialize) {
//                    return;
//                }
//                logger.info("dyanc install:{} to classloader ", this.toString());
//                PluginManager pluginManager = TIS.get().getPluginManager();
//                File pluginFile = getPluginPackageFile();
//                List<PluginWrapper> plugins = Lists.newArrayList(
//                        pluginManager.getPluginStrategy().createPluginWrapper(pluginFile));
//
//                pluginManager.dynamicLoad(pluginFile, false, null);
//                pluginManager.start(plugins);
//            } catch (Throwable e) {
//                throw new RuntimeException(e);
//            }
//        }
}
