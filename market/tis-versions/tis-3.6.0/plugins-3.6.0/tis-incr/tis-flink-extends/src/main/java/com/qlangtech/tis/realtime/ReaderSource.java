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

import com.qlangtech.tis.async.message.client.consumer.Tab2OutputTag;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.data.RowData;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-11-15 18:03
 **/
public abstract class ReaderSource<T> {
    // public final SourceFunction<T> sourceFunc;
    public final String tokenName;

    public final Class<T> rowType;

    private ReaderSource(String tokenName, // SourceFunction<T> sourceFunc,
                         Class<T> rowType) {
        if (StringUtils.isEmpty(tokenName)) {
            throw new IllegalArgumentException("param tokenName can not be empty");
        }
        // this.sourceFunc = sourceFunc;
        this.tokenName = tokenName;
        this.rowType = rowType;
    }

    protected void afterSourceStreamGetter(Tab2OutputTag<DTOStream> tab2OutputStream, SingleOutputStreamOperator<T> operator) {
        //return operator;
    }

    public void getSourceStream(
            StreamExecutionEnvironment env, Tab2OutputTag<DTOStream> tab2OutputStream) {

        SingleOutputStreamOperator<T> operator = addAsSource(env)
                .name(this.tokenName)
                .setParallelism(1);

        afterSourceStreamGetter(tab2OutputStream, operator);

//        if (rowType == DTO.class) {
//
//            return (SingleOutputStreamOperator<T>) (((SingleOutputStreamOperator<DTO>) operator)
//                    .process(new SourceProcessFunction(tab2OutputStream.entrySet().stream()
//                    .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue().outputTag)))));
//
//        } else if (rowType == RowData.class) {
//            return operator;
//        } else {
//
//        }
//        return env.addSource(this.sourceFunc)
//                .name(this.tokenName)
//                .setParallelism(1)
//                .;
    }

    protected abstract DataStreamSource<T> addAsSource(StreamExecutionEnvironment env);// {
    //  return env.addSource(this.sourceFunc);
    //}


    public static ReaderSource<DTO> createDTOSource(String tokenName, SourceFunction<DTO> sourceFunc) {
        return new DTOSideOutputReaderSource(tokenName) {
            @Override
            protected DataStreamSource<DTO> addAsSource(StreamExecutionEnvironment env) {
                return env.addSource(sourceFunc);
            }
        };
    }

    public static ReaderSource<DTO> createDTOSource(String tokenName, final DataStreamSource<DTO> source) {
        return new DTOSideOutputReaderSource(tokenName) {
            @Override
            protected DataStreamSource<DTO> addAsSource(StreamExecutionEnvironment env) {
                return source;
            }
        };
    }

    private static abstract class DTOSideOutputReaderSource extends ReaderSource<DTO> {

        public DTOSideOutputReaderSource(String tokenName) {
            super(tokenName, DTO.class);
        }

        @Override
        protected final void afterSourceStreamGetter(
                Tab2OutputTag<DTOStream> tab2OutputStream, SingleOutputStreamOperator<DTO> operator) {
            SingleOutputStreamOperator<DTO> mainStream
                    = operator.process(new SourceProcessFunction(tab2OutputStream.entrySet().stream()
                    .collect(Collectors.toMap((e) -> e.getKey().getFrom(), (e) -> ((DTOStream.DispatchedDTOStream) e.getValue()).outputTag))));
            for (Map.Entry<TableAlias, DTOStream> e : tab2OutputStream.entrySet()) {
                e.getValue().addStream(mainStream);
            }
           // return mainStream;
        }
    }


    public static ReaderSource<RowData> createRowDataSource(String tokenName, ISelectedTab tab, SourceFunction<RowData> sourceFunc) {
        return new RowDataOutputReaderSource(tokenName, RowData.class, tab) {
            @Override
            protected DataStreamSource<RowData> addAsSource(StreamExecutionEnvironment env) {
                return env.addSource(sourceFunc);
            }
        };
    }

    public static ReaderSource<RowData> createRowDataSource(String tokenName, ISelectedTab tab, DataStreamSource<RowData> streamSource) {
        return new RowDataOutputReaderSource(tokenName, RowData.class, tab) {
            @Override
            protected DataStreamSource<RowData> addAsSource(StreamExecutionEnvironment env) {
                return streamSource;
            }
        };
    }

    private static abstract class RowDataOutputReaderSource extends ReaderSource<RowData> {
        private ISelectedTab tab;

        public RowDataOutputReaderSource(String tokenName, Class rowType, ISelectedTab tab) {
            super(tokenName, rowType);
            this.tab = tab;
        }

        @Override
        protected final void afterSourceStreamGetter(
                Tab2OutputTag<DTOStream> tab2OutputStream, SingleOutputStreamOperator<RowData> operator) {
            DTOStream dtoStream = tab2OutputStream.get(tab);
            dtoStream.addStream(operator);
           // return operator;
        }
    }
}
