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

package com.qlangtech.tis.plugin.common;

import com.alibaba.datax.common.element.ColumnCast;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.job.JobContainer;
import com.alibaba.datax.core.util.container.JarLoader;
import com.alibaba.datax.core.util.container.LoadUtil;
import com.alibaba.datax.plugin.writer.streamwriter.Key;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import com.qlangtech.tis.datax.*;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.datax.MockDataxReaderContext;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-23 17:06
 **/
public class ReaderTemplate {

    public static void validateDataXReader(String assertFileName, String dataXName, DataxReader dataxReader) throws IOException {
        IDataxProcessor processor = EasyMock.mock("dataxProcessor", IDataxProcessor.class);

        IDataxGlobalCfg dataxGlobalCfg = EasyMock.mock("dataxGlobalCfg", IDataxGlobalCfg.class);
        EasyMock.expect(processor.getDataXGlobalCfg()).andReturn(dataxGlobalCfg).anyTimes();
        IDataxWriter dataxWriter = EasyMock.mock("dataxWriter", IDataxWriter.class);
        EasyMock.expect(processor.getWriter(null, true)).andReturn(dataxWriter).anyTimes();
        IDataxContext dataxContext = EasyMock.mock("dataxWriterContext", IDataxContext.class);
        EasyMock.expect(dataxWriter.getSubTask(Optional.empty())).andReturn(dataxContext).anyTimes();

        //  EasyMock.expect(processor.getReader(null)).andReturn(dataxReader);

        EasyMock.replay(processor, dataxGlobalCfg, dataxWriter, dataxContext);

//        try (InputStream reader = this.getClass().getResourceAsStream("oss-datax-reader-assert.json")) {
//            JSONObject jsonObject = JSON.parseObject(IOUtils.toString(reader, TisUTF8.getName()));
//            System.out.println("nullFormat:" + jsonObject.getJSONObject("parameter").getString("nullFormat"));
//        }

        valiateReaderCfgGenerate(assertFileName, processor, dataxReader, dataxWriter, dataXName);


        EasyMock.verify(processor, dataxGlobalCfg, dataxWriter, dataxContext);
    }

    private static void valiateReaderCfgGenerate(String assertFileName, IDataxProcessor processor
            , DataxReader dataXReader, IDataxWriter dataxWriter, String dataXName) throws IOException {
        String readerCfg = generateReaderCfg(processor, dataXReader, dataxWriter, dataXName);
        TestCase.assertNotNull(readerCfg);
        System.out.println(readerCfg);
        com.qlangtech.tis.trigger.util.JsonUtil.assertJSONEqual(dataXReader.getClass(), assertFileName, readerCfg, (msg, expect, actual) -> {
            Assert.assertEquals(msg, expect, actual);
        });
        JSONObject reader = JSON.parseObject(readerCfg);
        Assert.assertEquals(dataXReader.getDataxMeta().getName(), reader.getString("name"));
    }

    public static String generateReaderCfg(IDataxProcessor processor, DataxReader dataXReader, String dataXName) throws IOException {

        IDataxWriter dataxWriter = new IDataxWriter() {
            @Override
            public String getTemplate() {
                return null;
            }

            @Override
            public DataxWriter.BaseDataxWriterDescriptor getWriterDescriptor() {
                return null;
            }

            @Override
            public IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap) {
                return null;
            }
        };
        return generateReaderCfg(processor, dataXReader, dataxWriter, dataXName);
    }

    private static String generateReaderCfg(IDataxProcessor processor, DataxReader dataXReader, IDataxWriter dataxWriter, String dataXName) throws IOException {

        IDataxReaderContext dataxReaderContext = null;
        Iterator<IDataxReaderContext> subTasks = dataXReader.getSubTasks();
        int dataxReaderContextCount = 0;
        while (subTasks.hasNext()) {
            dataxReaderContext = subTasks.next();
            dataxReaderContextCount++;
        }
        TestCase.assertEquals(1, dataxReaderContextCount);
        TestCase.assertNotNull(dataxReaderContext);
        DataXCfgGenerator dataProcessor = new DataXCfgGenerator(null, dataXName, processor) {
        };

        return dataProcessor.generateDataxConfig(dataxReaderContext, dataxWriter, dataXReader, Optional.empty());
    }

//    public static void realExecute(final String readerJson, IDataXPluginMeta dataxReader) throws IllegalAccessException {
//        realExecute(readerJson, dataxReader);
//    }


    public static void realExecute(final String dataXName, final Configuration readerCfg, File writeFile, IDataXPluginMeta dataxReader) throws IllegalAccessException {
        Objects.requireNonNull(readerCfg);
        final JarLoader uberClassLoader = new JarLoader(new String[]{"."});

        DataxExecutor.initializeClassLoader(
                Sets.newHashSet("plugin.reader." + dataxReader.getDataxMeta().getName(), "plugin.writer.streamwriter"), uberClassLoader);

        Configuration allConf = IOUtils.loadResourceFromClasspath(MockDataxReaderContext.class //
                , "container.json", true, (input) -> {
                    Configuration cfg = Configuration.from(input);


                    cfg.set("plugin.writer.streamwriter.class"
                            , "com.alibaba.datax.plugin.writer.streamwriter.StreamWriter");

                    cfg.set("plugin.reader." + dataxReader.getDataxMeta().getName() + ".class"
                            , dataxReader.getDataxMeta().getImplClass());
                    cfg.set("job.content[0].reader" //
                            , readerCfg);
                    cfg.set("job.content[0].writer", Configuration.from("{\n" +
                            "    \"name\": \"streamwriter\",\n" +
                            "    \"parameter\": {\n" +
                            //"        \"print\": true\n" +
                            "        \"" + Key.PATH + "\": \"" + writeFile.getParentFile().getAbsolutePath() + "\",\n" +
                            "        \"" + Key.FILE_NAME + "\": \"" + writeFile.getName() + "\"\n" +
                            "    }\n" +
                            "}"));

                    DataxExecutor.setResType(cfg, StoreResourceType.DataApp);
                    return cfg;
                });


        // 绑定column转换信息
        ColumnCast.bind(allConf);
        LoadUtil.bind(allConf);

        JobContainer container = new JobContainer(allConf) {
            @Override
            public int getTaskSerializeNum() {
                return 999;
            }

            @Override
            public String getFormatTime(TimeFormat format) {
                return super.getFormatTime(format);
            }

            @Override
            public String getTISDataXName() {
                return dataXName;
            }
        };

        container.start();
    }

    /**
     * dataXWriter执行
     *
     * @param readerJson
     * @param dataxReader
     * @throws IllegalAccessException
     */
    public static void realExecute(String dataXName, final String readerJson, File writeFile, IDataXPluginMeta dataxReader) throws IllegalAccessException {
        // Configuration writeCfg = ;
        realExecute(dataXName, (Configuration) IOUtils.loadResourceFromClasspath(dataxReader.getClass(), readerJson, true, (writerJsonInput) -> {
            return Configuration.from(writerJsonInput);
        }), writeFile, dataxReader);
//        final JarLoader uberClassLoader = new JarLoader(new String[]{"."});
//
//        DataxExecutor.initializeClassLoader(
//                Sets.newHashSet("plugin.reader." + dataxReader.getDataxMeta().getName(), "plugin.writer.streamwriter"), uberClassLoader);
//
//        Configuration allConf = IOUtils.loadResourceFromClasspath(MockDataxReaderContext.class //
//                , "container.json", true, (input) -> {
//                    Configuration cfg = Configuration.from(input);
//
//
//                    cfg.set("plugin.writer.streamwriter.class"
//                            , "com.alibaba.datax.plugin.writer.streamwriter.StreamWriter");
//
//                    cfg.set("plugin.reader." + dataxReader.getDataxMeta().getName() + ".class"
//                            , dataxReader.getDataxMeta().getImplClass());
//                    cfg.set("job.content[0].reader" //
//                            , IOUtils.loadResourceFromClasspath(dataxReader.getClass(), readerJson, true, (writerJsonInput) -> {
//                                return Configuration.from(writerJsonInput);
//                            }));
//                    cfg.set("job.content[0].writer", Configuration.from("{\n" +
//                            "    \"name\": \"streamwriter\",\n" +
//                            "    \"parameter\": {\n" +
//                            "        \"print\": true\n" +
//                            "    }\n" +
//                            "}"));
//                    return cfg;
//                });
//
//
//        // 绑定column转换信息
//        ColumnCast.bind(allConf);
//        LoadUtil.bind(allConf);
//
//        JobContainer container = new JobContainer(allConf);
//
//        container.start();
    }

}
