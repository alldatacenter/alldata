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

package com.qlangtech.tis.realtime;

import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.async.message.client.consumer.AsyncMsg;
import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
import com.qlangtech.tis.async.message.client.consumer.Tab2OutputTag;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IStreamTableMeataCreator;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.extension.TISExtensible;
import com.qlangtech.tis.plugin.incr.IncrStreamFactory;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.realtime.dto.DTOStream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-27 14:18
 **/
@Public
@TISExtensible
public abstract class BasicFlinkSourceHandle<SINK_TRANSFER_OBJ>
        implements IConsumerHandle<List<ReaderSource>, JobExecutionResult>, Serializable {

    private transient TISSinkFactory sinkFuncFactory;
    private transient IncrStreamFactory streamFactory;
    private transient IStreamTableMeataCreator.ISourceStreamMetaCreator metaCreator;

    protected String getDataXName() {
        String name = this.sinkFuncFactory.dataXName;
        if (StringUtils.isEmpty(name)) {
            throw new IllegalStateException("dataXName can not be empty");
        }
        return name;
    }

    public static IStreamTableMeataCreator.IStreamTableMeta getStreamTableMeta(TargetResName dataxName, String tabName) {
        TISSinkFactory sinKFactory = TISSinkFactory.getIncrSinKFactory(dataxName.getName());
        return getStreamTableMeta(sinKFactory, tabName);
    }

    public static IStreamTableMeataCreator.IStreamTableMeta getStreamTableMeta(TISSinkFactory sinKFactory, String tabName) {
        if (!(sinKFactory instanceof IStreamTableMeataCreator.ISinkStreamMetaCreator)) {
            throw new IllegalStateException("writer:"
                    + sinKFactory.getClass().getName() + " must be type of "
                    + IStreamTableMeataCreator.ISinkStreamMetaCreator.class.getSimpleName());
        }
        return ((IStreamTableMeataCreator.ISinkStreamMetaCreator) sinKFactory).getStreamTableMeta(tabName);
    }

    public static IStreamTableMeataCreator.IStreamTableMeta getStreamTableMeta(
            IStreamTableMeataCreator.ISourceStreamMetaCreator sourceFactory, String tabName) {
        return sourceFactory.getStreamTableMeta(tabName);
    }


    @Override
    public JobExecutionResult consume(TargetResName dataxName, AsyncMsg<List<ReaderSource>> asyncMsg
            , IDataxProcessor dataXProcessor) throws Exception {
        StreamExecutionEnvironment env = getFlinkExecutionEnvironment();

        if (CollectionUtils.isEmpty(asyncMsg.getFocusTabs())) {
            throw new IllegalArgumentException("focusTabs can not be empty");
        }

        Tab2OutputTag<DTOStream> tab2OutputTag = createTab2OutputTag(asyncMsg, env, dataxName);
        Map<TableAlias, TabSinkFunc<SINK_TRANSFER_OBJ>> sinks = createTabSinkFunc(dataXProcessor);
        CountDownLatch countDown = new CountDownLatch(1);

        this.processTableStream(env, tab2OutputTag, new SinkFuncs(sinks, countDown));


        countDown.await(60, TimeUnit.SECONDS);
        return executeFlinkJob(dataxName, env);
    }

    protected Map<TableAlias, TabSinkFunc<SINK_TRANSFER_OBJ>> createTabSinkFunc(IDataxProcessor dataXProcessor) {
        Map<TableAlias, TabSinkFunc<SINK_TRANSFER_OBJ>> sinks
                = this.getSinkFuncFactory().createSinkFunction(dataXProcessor);
        sinks.forEach((tab, func) -> {
            if (StringUtils.isEmpty(tab.getTo()) || StringUtils.isEmpty(tab.getFrom())) {
                throw new IllegalArgumentException("tab of "
                        + this.getSinkFuncFactory().getDescriptor().getDisplayName() + ":" + tab + " is illegal");
            }
        });
        return sinks;
    }


    /**
     * 处理各个表对应的数据流
     *
     * @param
     */
    protected abstract void processTableStream(StreamExecutionEnvironment env
            , Tab2OutputTag<DTOStream> tab2OutputTag, SinkFuncs<SINK_TRANSFER_OBJ> sinkFunction);


    private Tab2OutputTag<DTOStream> createTab2OutputTag(
            AsyncMsg<List<ReaderSource>> asyncMsg
            , StreamExecutionEnvironment env, TargetResName dataxName) throws java.io.IOException {
        Tab2OutputTag<DTOStream> tab2OutputTag = asyncMsg.getTab2OutputTag();
        for (ReaderSource sourceFunc : asyncMsg.getSource()) {

            sourceFunc.getSourceStream(env, tab2OutputTag);
        }
        return tab2OutputTag;
    }

    protected List<FlinkCol> getTabColMetas(TargetResName dataxName, String tabName) {
        return Collections.emptyList();
    }

    protected StreamExecutionEnvironment getFlinkExecutionEnvironment() {
        return streamFactory.createStreamExecutionEnvironment();
    }

    protected JobExecutionResult executeFlinkJob(TargetResName dataxName, StreamExecutionEnvironment env) throws Exception {
        return env.execute(dataxName.getName());
    }

    public TISSinkFactory getSinkFuncFactory() {
        return this.sinkFuncFactory;
    }

    public void setSinkFuncFactory(TISSinkFactory sinkFuncFactory) {
        if (sinkFuncFactory == null) {
            throw new IllegalArgumentException("param sinkFuncFactory can not be null");
        }
        this.sinkFuncFactory = sinkFuncFactory;
    }

    public void setStreamFactory(IncrStreamFactory streamFactory) {
        this.streamFactory = Objects.requireNonNull(streamFactory, "streamFactory can not be null");
    }

    /**
     * 源端Meta信息获取策略
     *
     * @param metaCreator
     */
    public void setSourceStreamTableMeta(IStreamTableMeataCreator.ISourceStreamMetaCreator metaCreator) {
        this.metaCreator = metaCreator;
    }

    public IStreamTableMeataCreator.ISourceStreamMetaCreator getSourceStreamTableMeta() {
        Objects.requireNonNull(this.metaCreator, "metaCreator can not be null");
        return metaCreator;
    }
}
