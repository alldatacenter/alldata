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

package com.qlangtech.tis.plugins.flink.client;

import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-15 11:19
 **/
public class FlinkStreamDemo {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        SourceFunction<DTO> sourceFunc = createSourceFunction();

        DataStreamSource<DTO> dtoDataStreamSource = env.addSource(sourceFunc);

        SinkFunction<DTO> sinkFunction = createSink();

        dtoDataStreamSource.addSink(sinkFunction);

        env.execute("flink-example");
    }

    private static SinkFunction<DTO> createSink() {
        URL[] urls = new URL[]{};
        ClassLoader classLoader = new URLClassLoader(urls);
        ServiceLoader<ISinkFunctionFactory> loaders = ServiceLoader.load(ISinkFunctionFactory.class, classLoader);
        Iterator<ISinkFunctionFactory> it = loaders.iterator();
        if (it.hasNext()) {
            return it.next().create();
        }
        throw new IllegalStateException();
    }

    private static SourceFunction<DTO> createSourceFunction() {
        URL[] urls = new URL[]{};
        ClassLoader classLoader = new URLClassLoader(urls);
        ServiceLoader<ISourceFunctionFactory> loaders = ServiceLoader.load(ISourceFunctionFactory.class, classLoader);
        Iterator<ISourceFunctionFactory> it = loaders.iterator();
        if (it.hasNext()) {
            return it.next().create();
        }
        throw new IllegalStateException();
    }

    public interface ISinkFunctionFactory {
        SinkFunction<DTO> create();
    }

    public interface ISourceFunctionFactory {
        SourceFunction<DTO> create();
    }
}
