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
package com.qlangtech.tis.plugin;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.impl.XmlFile;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.solr.common.DOMUtil;
import com.qlangtech.tis.util.IPluginContext;
import com.qlangtech.tis.util.PluginMeta;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class KeyedPluginStore<T extends Describable> extends PluginStore<T> {
    public static final String TMP_DIR_NAME = ".tmp/";
    private static final Pattern DATAX_UPDATE_PATH = Pattern.compile("/x/(" + ValidatorCommons.pattern_identity + ")/update");
    public transient final Key key;

    public static <TT extends Describable> KeyedPluginStore<TT> getPluginStore(
            DataxReader.SubFieldFormAppKey<TT> subFieldFormKey //, IPluginProcessCallback<TT>... pluginCreateCallback
    ) {
        //  return new KeyedPluginStore(subFieldFormKey, pluginCreateCallback);
        return (KeyedPluginStore<TT>) TIS.dataXReaderSubFormPluginStore.get(subFieldFormKey);
    }

    static File getLastModifyToken(Key appKey) {
        File appDir = getSubPathDir(appKey);
        File lastModify = new File(appDir, CenterResource.KEY_LAST_MODIFIED_EXTENDION);
        return lastModify;
    }

    /**
     * 取得某个应用下面相关的插件元数据信息用于分布式任务同步用
     *
     * @return
     */
    public static PluginMetas getAppAwarePluginMetas(boolean isDB, String name) {
        AppKey appKey = new AppKey(null, isDB, name, null);
        File appDir = getSubPathDir(appKey);
        File lastModify = getLastModifyToken(appKey);// new File(appDir, CenterResource.KEY_LAST_MODIFIED_EXTENDION);
        long lastModfiyTimeStamp = -1;
        Set<PluginMeta> metas = Collections.emptySet();


        try {
            if (appDir.exists()) {
                if (lastModify.exists()) {
                    //throw new IllegalStateException("lastModify is not exist ,path:" + lastModify.getAbsolutePath());
                    lastModfiyTimeStamp = Long.parseLong(FileUtils.readFileToString(lastModify, TisUTF8.get()));
                }
                Iterator<File> files = FileUtils.iterateFiles(appDir
                        , new String[]{DOMUtil.XML_RESERVED_PREFIX}, true);
                metas = ComponentMeta.loadPluginMeta(() -> {
                    return Lists.newArrayList(files);
                });
//                if (lastModfiyTimeStamp < 0) {
//                    throw new IllegalStateException("please check lastModify file:" + lastModify.getAbsolutePath());
//                }
            }
            return new PluginMetas(appDir, metas, lastModfiyTimeStamp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static class PluginMetas {
        // 全局配置文件对应的最近更新Key
        public static final String KEY_GLOBAL_PLUGIN_STORE = "globalPluginStore";
        //　plugin tpi 包的最近更新时间对应的Key
        public static final String KEY_PLUGIN_META = "pluginMetas";
        // app应用对应的最近更新时间
        public static final String KEY_APP_LAST_MODIFY_TIMESTAMP = "appLastModifyTimestamp";


        public final Set<PluginMeta> metas;
        public final long lastModifyTimestamp;

        public final File appDir;

        public PluginMetas(File appDir, Set<PluginMeta> metas, long lastModifyTimestamp) {
            this.metas = metas;
            this.lastModifyTimestamp = lastModifyTimestamp;
            this.appDir = appDir;
        }
    }

    private static File getSubPathDir(Key appKey) {
        return new File(TIS.pluginCfgRoot, appKey.getSubDirPath());
    }

    public KeyedPluginStore(Key key, IPluginProcessCallback<T>... pluginCreateCallback) {
        super(key.pluginClass, key.getSotreFile(), pluginCreateCallback);
        this.key = key;
    }

    public interface IPluginKeyAware {
        public void setKey(Key key);
    }

    @Override
    public List<T> getPlugins() {
        List<T> plugins = super.getPlugins();
        for (T plugin : plugins) {
            if (plugin instanceof IPluginKeyAware) {
                ((IPluginKeyAware) plugin).setKey(this.key);
            }
        }
        return plugins;
    }

    @Override
    public synchronized SetPluginsResult setPlugins(IPluginContext pluginContext
            , Optional<Context> context, List<Descriptor.ParseDescribable<T>> dlist, boolean update) {
        SetPluginsResult updateResult = super.setPlugins(pluginContext, context, dlist, update);
        if (updateResult.success && updateResult.cfgChanged) {
            // 本地写入时间戳文件，以备分布式文件同步之用
            updateResult.lastModifyTimeStamp
                    = writeLastModifyTimeStamp(getLastModifyToken(this.key));
        }
        return updateResult;
    }

    @Override
    public File getLastModifyTimeStampFile() {
        return new File(getSubPathDir(this.key), CenterResource.KEY_LAST_MODIFIED_EXTENDION);
    }

    @Override
    protected String getSerializeFileName() {
        return key.getSerializeFileName();
    }

    public static class Key<T extends Describable> {

        public final KeyVal keyVal;
        protected final String groupName;

        public final Class<T> pluginClass;

        public Key(String groupName, String keyVal, Class<T> pluginClass) {
            this(groupName, new KeyVal(keyVal), pluginClass);
        }

        public Key(String groupName, KeyVal keyVal, Class<T> pluginClass) {
            Objects.requireNonNull(keyVal, "keyVal can not be null");
            this.keyVal = keyVal;
            this.pluginClass = pluginClass;
            this.groupName = groupName;
        }

        public String getSerializeFileName() {
            return this.getSubDirPath() + File.separator + pluginClass.getName();
        }

        public final String getSubDirPath() {
            return groupName + File.separator + keyVal.getKeyVal();
        }

        public XmlFile getSotreFile() {
            return Descriptor.getConfigFile(getSerializeFileName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Key key = (Key) o;
            return this.hashCode() == key.hashCode();
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyVal.getKeyVal(), pluginClass);
        }
    }

    public static class KeyVal {
        private final String val;
        private final String suffix;

        public KeyVal(String val, String suffix) {
            if (StringUtils.isEmpty(val)) {
                throw new IllegalArgumentException("param 'key' can not be null");
            }
            this.val = val;
            this.suffix = suffix;
        }

        @Override
        public String toString() {
            return getKeyVal();
        }

        public String getKeyVal() {
            return StringUtils.isBlank(this.suffix) ? val : TMP_DIR_NAME + (val + "-" + this.suffix);
        }

        public KeyVal(String val) {
            this(val, StringUtils.EMPTY);
        }

        public String getVal() {
            return val;
        }

        public String getSuffix() {
            return suffix;
        }
    }

    public static class AppKey<TT extends Describable> extends Key<TT> {
        public final boolean isDB;

        public AppKey(IPluginContext pluginContext, boolean isDB, String appname, Class<TT> clazz) {
            super(isDB ? TIS.DB_GROUP_NAME : IFullBuildContext.NAME_APP_DIR, calAppName(pluginContext, appname), clazz);
            this.isDB = isDB;
        }

        private static KeyVal calAppName(IPluginContext pluginContext, String appname) {
            if (pluginContext == null) {
                return new KeyVal(appname);
            }
            String referer = pluginContext.getRequestHeader(DataxReader.HEAD_KEY_REFERER);
            Matcher configPathMatcher = DATAX_UPDATE_PATH.matcher(referer);
            boolean inUpdateProcess = configPathMatcher.find();
            if (inUpdateProcess && !pluginContext.isCollectionAware()) {
                throw new IllegalStateException("pluginContext.isCollectionAware() must be true");
            }
            return (pluginContext != null && inUpdateProcess)
                    ? new KeyVal(appname, pluginContext.getExecId()) : new KeyVal(appname);
        }
    }
}
