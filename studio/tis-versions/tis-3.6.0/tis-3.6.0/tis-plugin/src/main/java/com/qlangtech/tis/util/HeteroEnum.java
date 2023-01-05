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

import com.qlangtech.tis.IPluginEnum;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.datax.job.DataXJobWorker;
import com.qlangtech.tis.extension.*;
import com.qlangtech.tis.extension.impl.BaseSubFormProperties;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.offline.FlatTableBuilder;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.IPluginStoreSave;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.credentials.ParamsConfigPluginStore;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.PostedDSProp;
import com.qlangtech.tis.plugin.incr.IncrStreamFactory;
import com.qlangtech.tis.plugin.k8s.K8sImage;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

//import com.qlangtech.tis.plugin.incr.IncrStreamFactory;

/**
 * 表明一种插件的类型
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class HeteroEnum<T extends Describable<T>> implements IPluginEnum<T> {

    @TISExtension
    public final static HeteroEnum<FlatTableBuilder> FLAT_TABLE_BUILDER = new HeteroEnum(//
            FlatTableBuilder.class, //
            "flat_table_builder", "宽表构建", Selectable.Single);
    // ////////////////////////////////////////////////////////
//    @TISExtension
//    public static final HeteroEnum<IndexBuilderTriggerFactory> INDEX_BUILD_CONTAINER = new HeteroEnum<IndexBuilderTriggerFactory>(//
//            IndexBuilderTriggerFactory.class, //
//            "index_build_container", // },
//            "索引构建容器", Selectable.Single);
//    // ////////////////////////////////////////////////////////
//    @TISExtension
//    public static final HeteroEnum<TableDumpFactory> DS_DUMP = new HeteroEnum<TableDumpFactory>(//
//            TableDumpFactory.class, //
//            "ds_dump", // },
//            "数据导出", Selectable.Single);
    // ////////////////////////////////////////////////////////
    @TISExtension
    public static final HeteroEnum<FileSystemFactory> FS = new HeteroEnum<FileSystemFactory>(//
            FileSystemFactory.class, //
            "fs", "存储");
    // ////////////////////////////////////////////////////////
    @TISExtension
    public static final HeteroEnum<MQListenerFactory> MQ = new HeteroEnum<MQListenerFactory>(//
            MQListenerFactory.class, //
            "mq", "Source Factory", Selectable.Multi, true) {
        @Override
        public IPluginStore getPluginStore(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
            return super.getPluginStore(pluginContext, pluginMeta);
        }
    };
    // ////////////////////////////////////////////////////////
    @TISExtension
    public static final HeteroEnum<ParamsConfig> PARAMS_CONFIG = new HeteroEnum<ParamsConfig>(//
            ParamsConfig.class, //
            "params-cfg", // },//
            "基础配置", Selectable.Multi, false) {
        @Override
        public IPluginStore getPluginStore(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
            return new ParamsConfigPluginStore(pluginMeta);
        }
    };
    // ////////////////////////////////////////////////////////
    @TISExtension
    public static final HeteroEnum<K8sImage> K8S_IMAGES = new HeteroEnum<K8sImage>(//
            K8sImage.class, //
            "k8s-images", // },//
            "K8S-Images", Selectable.Multi, false);
    // ////////////////////////////////////////////////////////

    @TISExtension
    public static final HeteroEnum<DataXJobWorker> DATAX_WORKER = new HeteroEnum<DataXJobWorker>(//
            DataXJobWorker.class, //
            "datax-worker", // },//
            "DataX Worker", Selectable.Single, true) {
        @Override
        public IPluginStore getPluginStore(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {

            if (!pluginContext.isCollectionAware()) {
                throw new IllegalStateException("must be collection aware");
            }
            return DataXJobWorker.getJobWorkerStore(new TargetResName(pluginContext.getCollectionName()));

            //return super.getPluginStore(pluginContext, pluginMeta);
        }
    };
    // ////////////////////////////////////////////////////////

    @TISExtension
    public static final HeteroEnum<IncrStreamFactory> INCR_STREAM_CONFIG
            = new HeteroEnum<>(//
            IncrStreamFactory.class, //
            "incr-config", // },
            "增量引擎配置", Selectable.Single, true);

    @TISExtension
    public static final HeteroEnum<DataSourceFactory> DATASOURCE = new HeteroEnum<DataSourceFactory>(//
            DataSourceFactory.class, //
            "datasource", //
            "数据源", //
            Selectable.Single, true) {
        @Override
        public IPluginStore getPluginStore(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
            //return super.getPluginStore(pluginContext, pluginMeta);

            if (!pluginContext.isDataSourceAware()) {
                throw new IllegalArgumentException("pluginContext must be dataSourceAware");
            }

            PostedDSProp dsProp = PostedDSProp.parse(pluginMeta);
            if (!dsProp.getDbname().isPresent()) {
                return null;
            }
            return TIS.getDataBasePluginStore(dsProp);
        }
    };
    //    @TISExtension
//    public static final HeteroEnum<FieldTypeFactory> SOLR_FIELD_TYPE = new HeteroEnum<FieldTypeFactory>(//
//            FieldTypeFactory.class, //
//            "field-type", //
//            "字段类型", //
//            Selectable.Multi);
//    //  @TISExtension
//    public static final HeteroEnum<QueryParserFactory> SOLR_QP = new HeteroEnum<QueryParserFactory>(//
//            QueryParserFactory.class, //
//            "qp", //
//            "QueryParser", //
//            Selectable.Multi);
//    //@TISExtension
//    public static final HeteroEnum<SearchComponentFactory> SOLR_SEARCH_COMPONENT = new HeteroEnum<SearchComponentFactory>(//
//            SearchComponentFactory.class, //
//            "searchComponent", //
//            "SearchComponent", //
//            Selectable.Multi);
//    //@TISExtension
//    public static final HeteroEnum<TISTransformerFactory> SOLR_TRANSFORMER = new HeteroEnum<TISTransformerFactory>(//
//            TISTransformerFactory.class, //
//            "transformer", //
//            "Transformer", //
//            Selectable.Multi);
    @TISExtension
    public static final HeteroEnum<DataxReader> DATAX_READER = new HeteroEnum<DataxReader>(//
            DataxReader.class, //
            "dataxReader", //
            "DataX Reader", //
            Selectable.Multi, true) {
        @Override
        public IPluginStore getPluginStore(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
            //   return super.getPluginStore(pluginContext, pluginMeta);
            return getDataXReaderAndWriterStore(pluginContext, true, pluginMeta, Optional.empty());
        }
    };
    @TISExtension
    public static final HeteroEnum<DataxWriter> DATAX_WRITER = new HeteroEnum<DataxWriter>(//
            DataxWriter.class, //
            "dataxWriter", //
            "DataX Writer", //
            Selectable.Multi, true) {
        @Override
        public IPluginStore getPluginStore(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
            return getDataXReaderAndWriterStore(pluginContext, false, pluginMeta);
        }
    };

    @TISExtension
    public static final HeteroEnum<IAppSource> APP_SOURCE = new HeteroEnum<IAppSource>(//
            IAppSource.class, //
            "appSource", //
            "App Source", //
            Selectable.Multi, true) {
        @Override
        public IPluginStore getPluginStore(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
            final String dataxName = pluginMeta.getDataXName();// (pluginMeta.getExtraParam(DataxUtils.DATAX_NAME));
//            if (StringUtils.isEmpty(dataxName)) {
//                throw new IllegalArgumentException(
//                        "plugin extra param 'DataxUtils.DATAX_NAME'" + DataxUtils.DATAX_NAME + " can not be null");
//            }
            return com.qlangtech.tis.manage.IAppSource.getPluginStore(pluginContext, dataxName);
        }
    };

    public final String caption;

    public final String identity;

    public final Class<? extends Describable> extensionPoint;

    // public final IDescriptorsGetter descriptorsGetter;
    // private final IItemGetter itemGetter;
    public final Selectable selectable;
    private final boolean appNameAware;

    public HeteroEnum(
            Class<T> extensionPoint,
            String identity, String caption, Selectable selectable) {
        this(extensionPoint, identity, caption, selectable, false);
    }

    public static MQListenerFactory getIncrSourceListenerFactory(String dataXName) {
        IPluginContext pluginContext = IPluginContext.namedContext(dataXName);
        List<MQListenerFactory> mqFactories = MQ.getPlugins(pluginContext, null);
        MQListenerFactory mqFactory = null;
        for (MQListenerFactory factory : mqFactories) {
            mqFactory = factory;
        }
        Objects.requireNonNull(mqFactory, "mqFactory can not be null, dataXName:" + dataXName + " mqFactories size:" + mqFactories.size());
        return mqFactory;
    }

    public static IncrStreamFactory getIncrStreamFactory(String dataxName) {
        IPluginContext pluginContext = IPluginContext.namedContext(dataxName);
        List<IncrStreamFactory> streamFactories = HeteroEnum.INCR_STREAM_CONFIG.getPlugins(pluginContext, null);
        for (IncrStreamFactory factory : streamFactories) {
            return factory;
        }
        throw new IllegalStateException("stream app:" + dataxName + " incrController can not not be null");
    }

    public static IPluginStore<?> getDataXReaderAndWriterStore(IPluginContext pluginContext, boolean getReader, UploadPluginMeta pluginMeta) {
        return getDataXReaderAndWriterStore(pluginContext, getReader, pluginMeta, Optional.empty());
    }

    public static IPluginStore<?> getDataXReaderAndWriterStore(IPluginContext pluginContext, boolean getReader, UploadPluginMeta pluginMeta,
                                                               Optional<IPropertyType.SubFormFilter> subFormFilter
    ) {
        IPluginStore<?> store = null;

        if (subFormFilter.isPresent()) {
            IPropertyType.SubFormFilter filter = subFormFilter.get();
            Descriptor targetDescriptor = filter.getTargetDescriptor();
            final Class<Describable> clazz = targetDescriptor.getT();
//            Optional<Descriptor> firstDesc = heteroEnum.descriptors().stream()
//                    .filter((des) -> filter.match((Descriptor) des)).map((des) -> (Descriptor) des).findFirst();
//            if (!firstDesc.isPresent()) {
//                throw new IllegalStateException("can not find relevant descriptor:" + filter.uploadPluginMeta.toString());
//            }

            PluginFormProperties pluginProps = targetDescriptor.getPluginFormPropertyTypes(subFormFilter);

            store = pluginProps.accept(new PluginFormProperties.IVisitor() {
                @Override
                public IPluginStoreSave<?> visit(BaseSubFormProperties props) {
                    // 为了在更新插件时候不把plugin上的@SubForm标记的属性覆盖掉，需要先将老的plugin上的值覆盖到新http post过来的反序列化之后的plugin上
                    //   Class<Describable> clazz = (Class<Describable>) heteroEnum.getExtensionPoint();


                    DataxReader.SubFieldFormAppKey<Describable> key = HeteroEnum.createDataXReaderAndWriterRelevant(pluginContext, pluginMeta
                            , new HeteroEnum.DataXReaderAndWriterRelevantCreator<DataxReader.SubFieldFormAppKey<Describable>>() {
                                @Override
                                public DataxReader.SubFieldFormAppKey<Describable> dbRelevant(IPluginContext pluginContext, String saveDbName) {
                                    return new DataxReader.SubFieldFormAppKey<>(pluginContext, true, saveDbName, props, clazz);
                                }

                                @Override
                                public DataxReader.SubFieldFormAppKey<Describable> appRelevant(IPluginContext pluginContext, String dataxName) {
                                    return new DataxReader.SubFieldFormAppKey<>(pluginContext, false, dataxName, props, clazz);
                                }
                            });

                    return KeyedPluginStore.getPluginStore(key);
                }
            });
        } else {
            store = createDataXReaderAndWriterRelevant(pluginContext, pluginMeta
                    , new DataXReaderAndWriterRelevantCreator<IPluginStore<?>>() {
                        @Override
                        public IPluginStore<?> dbRelevant(IPluginContext pluginContext, String saveDbName) {
                            if (!getReader) {
                                throw new IllegalStateException("getReader must be true");
                            }
                            return DataxReader.getPluginStore(pluginContext, true, saveDbName);
                        }

                        @Override
                        public IPluginStore<?> appRelevant(IPluginContext pluginContext, String dataxName) {
                            KeyedPluginStore<?> keyStore = (getReader)
                                    ? DataxReader.getPluginStore(pluginContext, dataxName) : DataxWriter.getPluginStore(pluginContext, dataxName);
                            return keyStore;
                        }
                    });
        }
        return store;
    }

    public static <T> T createDataXReaderAndWriterRelevant(
            IPluginContext pluginContext, UploadPluginMeta pluginMeta, DataXReaderAndWriterRelevantCreator<T> creator) {
        final String dataxName = pluginMeta.getDataXName();//.getExtraParam(DataxUtils.DATAX_NAME);

        if (StringUtils.isEmpty(dataxName)) {
            String saveDbName = pluginMeta.getExtraParam(DataxUtils.DATAX_DB_NAME);
            if (StringUtils.isNotBlank(saveDbName)) {

                // return DataxReader.getPluginStore(pluginContext, true, saveDbName);
                return creator.dbRelevant(pluginContext, saveDbName);
            } else {
                throw new IllegalArgumentException("plugin extra param " + DataxUtils.DATAX_NAME + " can not be null");
            }
        } else {
//            KeyedPluginStore<?> keyStore = (getReader)
//                    ? DataxReader.getPluginStore(pluginContext, dataxName) : DataxWriter.getPluginStore(pluginContext, dataxName);
//            return keyStore;
            return creator.appRelevant(pluginContext, dataxName);
        }
    }

    public interface DataXReaderAndWriterRelevantCreator<T> {
        public T dbRelevant(IPluginContext pluginContext, String saveDbName);

        public T appRelevant(IPluginContext pluginContext, String dataxName);

    }

    @Override
    public boolean isAppNameAware() {
        return this.appNameAware;
    }

    public HeteroEnum(
            Class<T> extensionPoint,
            String identity, String caption, Selectable selectable, boolean appNameAware) {
        this.extensionPoint = extensionPoint;
        this.caption = caption;
        this.identity = identity;
        this.selectable = selectable;
        this.appNameAware = appNameAware;
    }

    /**
     * 判断实例是否是应该名称唯一的
     *
     * @return
     */
    public boolean isIdentityUnique() {
        return IdentityName.class.isAssignableFrom(this.extensionPoint);
    }


    HeteroEnum(
            Class<T> extensionPoint, String identity, String caption) {
        this(extensionPoint, identity, caption, Selectable.Multi);
    }

    public <T> T getPlugin() {
        if (this.selectable != Selectable.Single) {
            throw new IllegalStateException(this.extensionPoint + " selectable is:" + this.selectable);
        }
        IPluginStore store = TIS.getPluginStore(this.extensionPoint);
        return (T) store.getPlugin();
    }

    /**
     * ref: PluginItems.save()
     *
     * @param pluginContext
     * @param pluginMeta
     * @param
     * @return
     */
    public List<T> getPlugins(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
        IPluginStore store = getPluginStore(pluginContext, pluginMeta);
        if (store == null) {
            return Collections.emptyList();
        }
        List<T> plugins = store.getPlugins();
        UploadPluginMeta.TargetDesc targetDesc = null;
        if (pluginMeta != null && (targetDesc = pluginMeta.getTargetDesc()).shallMatchTargetDesc()) {
            final UploadPluginMeta.TargetDesc finalDesc = targetDesc;
            return plugins.stream()
                    .filter((p) -> finalDesc.isNameMatch(p.getDescriptor().getDisplayName()))
                    .collect(Collectors.toList());
        }

        return plugins;
    }

    @Override
    public IPluginStore getPluginStore(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
        IPluginStore store = null;
        if (this.isAppNameAware()) {
            if (!pluginContext.isCollectionAware()) {
                throw new IllegalStateException(this.getExtensionPoint().getName() + " must be collection aware");
            }
            store = TIS.getPluginStore(pluginContext.getCollectionName(), this.extensionPoint);
        } else {
            store = TIS.getPluginStore(this.extensionPoint);
        }
        //}
        Objects.requireNonNull(store, "plugin store can not be null");
        return store;
    }

    public <T extends Describable<T>> List<Descriptor<T>> descriptors() {
        IPluginStore pluginStore = TIS.getPluginStore(this.extensionPoint);
        return pluginStore.allDescriptor();
    }

    public static <T extends Describable<T>> IPluginEnum<T> of(String identity) {

        ExtensionList<IPluginEnum> pluginEnums = TIS.get().getExtensionList(IPluginEnum.class);

        for (IPluginEnum he : pluginEnums) {
            if (StringUtils.equals(he.getIdentity(), identity)) {
                return he;
            }
        }
        throw new IllegalStateException("identity:" + identity + " is illegal,exist:"
                + pluginEnums.stream().map((h) -> "'" + h.getIdentity() + "'").collect(Collectors.joining(",")));
    }


    @Override
    public Class getExtensionPoint() {
        return this.extensionPoint;
    }

    @Override
    public String getIdentity() {
        return this.identity;
    }

    @Override
    public String getCaption() {
        return this.caption;
    }

    @Override
    public Selectable getSelectable() {
        return this.selectable;
    }
}
