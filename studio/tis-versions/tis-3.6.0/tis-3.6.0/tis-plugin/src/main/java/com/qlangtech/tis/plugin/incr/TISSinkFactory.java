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

package com.qlangtech.tis.plugin.incr;

import com.google.common.collect.Maps;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.datax.IDataXPluginMeta;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;
import com.qlangtech.tis.util.HeteroEnum;
import com.qlangtech.tis.util.IPluginContext;
import com.qlangtech.tis.util.Selectable;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-29 10:50
 **/
@Public
public abstract class TISSinkFactory implements Describable<TISSinkFactory>, KeyedPluginStore.IPluginKeyAware {
    // public static final String KEY_FLINK_STREAM_APP_NAME_PREFIX = "flink_stream_";
    public static final String KEY_PLUGIN_TPI_CHILD_PATH = "flink/";
    private static final Logger logger = LoggerFactory.getLogger(TISSinkFactory.class);

    public static Optional<Descriptor<IncrSelectedTabExtend>> getIncrSinkSelectedTabExtendDescriptor(String dataXName) {
        IPluginContext pluginContext = IPluginContext.namedContext(dataXName);
        List<TISSinkFactory> sinkFactories = sinkFactory.getPlugins(pluginContext, null);
        TISSinkFactory sinkFactory = null;
        for (TISSinkFactory factory : sinkFactories) {
            sinkFactory = factory;
            break;
        }
        Objects.requireNonNull(sinkFactory, "sinkFactory can not be null, dataXName:" + dataXName + " sinkFactories size:" + sinkFactories.size());
        Descriptor<TISSinkFactory> descriptor = sinkFactory.getDescriptor();
        if (!(descriptor instanceof IIncrSelectedTabExtendFactory)) {
            return Optional.empty();
        }

        Descriptor<IncrSelectedTabExtend> selectedTableExtendDesc
                = ((IIncrSelectedTabExtendFactory) descriptor).getSelectedTableExtendDescriptor();
        return Optional.ofNullable(selectedTableExtendDesc);
    }

    public static void main(String[] args) throws Exception {
        URL url = new URL("jar:file:/opt/data/tis/libs/plugins/flink/hudi/WEB-INF/lib/../../WEB-INF/lib/hudi-incr.jar!/META-INF/annotations/com.qlangtech.tis.extension.TISExtension");
        System.out.println(IOUtils.toString(url, TisUTF8.get()));

    }

    @TISExtension
    public static final HeteroEnum<TISSinkFactory> sinkFactory = new HeteroEnum<TISSinkFactory>(//
            TISSinkFactory.class, //
            "sinkFactory", //
            "Incr Sink Factory", //
            Selectable.Single, true);

    public static TISSinkFactory getIncrSinKFactory(String dataXName) {
        IPluginContext pluginContext = IPluginContext.namedContext(dataXName);
        return getIncrSinKFactory(pluginContext);
    }

    public static Function<IPluginContext, TISSinkFactory> stubGetter;

    public static TISSinkFactory getIncrSinKFactory(IPluginContext pluginContext) {

        if (stubGetter != null) {
            // for Test
            return stubGetter.apply(pluginContext);
        }

        List<TISSinkFactory> sinkFactories = sinkFactory.getPlugins(pluginContext, null);
        TISSinkFactory sinkFactory = null;
        logger.info("sinkFactories size:" + sinkFactories.size());
        for (TISSinkFactory factory : sinkFactories) {
            sinkFactory = factory;
            break;
        }
        Objects.requireNonNull(sinkFactory, "sinkFactories.size():" + sinkFactories.size());
        return sinkFactory;
    }

    public transient String dataXName;

    /**
     * 取得增量执行单元，脚本编译器
     *
     * @return
     */
    public abstract ICompileAndPackage getCompileAndPackageManager();

    @Override
    public void setKey(KeyedPluginStore.Key key) {
        this.dataXName = key.keyVal.getVal();
    }

    /**
     * Map< IDataxProcessor.TableAlias, <SinkFunction<DTO> >
     *
     * @param dataxProcessor
     * @return
     */
    public abstract <SinkFunc> Map<TableAlias, SinkFunc> createSinkFunction(IDataxProcessor dataxProcessor);


    @Override
    public final Descriptor<TISSinkFactory> getDescriptor() {
        Descriptor<TISSinkFactory> descriptor = TIS.get().getDescriptor(this.getClass());
        Class<BaseSinkFunctionDescriptor> expectClazz = getExpectDescClass();
        if (!(expectClazz.isAssignableFrom(descriptor.getClass()))) {
            throw new IllegalStateException(descriptor.getClass() + " must implement the Descriptor of " + expectClazz.getName());
        }
        return descriptor;
    }

    protected <TT extends BaseSinkFunctionDescriptor> Class<TT> getExpectDescClass() {
        return (Class<TT>) BaseSinkFunctionDescriptor.class;
    }


    public static abstract class BaseSinkFunctionDescriptor extends Descriptor<TISSinkFactory> implements IEndTypeGetter {
        @Override
        public Map<String, Object> getExtractProps() {
            Map<String, Object> vals = Maps.newHashMap();
            EndType targetType = this.getTargetType();
            vals.put(IDataXPluginMeta.END_TARGET_TYPE, targetType.getVal());
            vals.put(IIncrSelectedTabExtendFactory.KEY_EXTEND_SELECTED_TAB_PROP
                    , (this instanceof IIncrSelectedTabExtendFactory)
                            && (((IIncrSelectedTabExtendFactory) this).getSelectedTableExtendDescriptor() != null));
            return vals;
        }

        @Override
        public final EndType getEndType() {
            return Objects.requireNonNull(this.getTargetType(), "targetType can not be null");
        }

        protected abstract EndType getTargetType();
    }
}
