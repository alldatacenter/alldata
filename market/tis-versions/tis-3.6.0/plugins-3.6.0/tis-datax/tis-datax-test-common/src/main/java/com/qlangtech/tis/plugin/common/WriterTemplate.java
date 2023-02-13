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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.*;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.datax.MockDataxReaderContext;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataXReaderColType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.test.BasicTest;
import com.qlangtech.tis.trigger.util.JsonUtil;
import org.easymock.EasyMock;
import org.junit.Assert;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-29 16:28
 **/
public class WriterTemplate {
    public final static String customerregisterId = "customerregister_id";
    public final static String lastVer = "last_ver";
    public final static String kind = "kind";

    public final static String TAB_customer_order_relation = "customer_order_relation";


//    public static final Field jarLoaderCenterField;
//
//    static {
//        try {
//            jarLoaderCenterField = LoadUtil.class.getDeclaredField("jarLoaderCenter");
//            jarLoaderCenterField.setAccessible(true);
//        } catch (NoSuchFieldException e) {
//            throw new RuntimeException("can not get field 'jarLoaderCenter' of LoadUtil", e);
//        }
//    }

    public static String cfgGenerate(DataxWriter dataXWriter, IDataxProcessor.TableMap tableMap) throws Exception {

        IDataxProcessor processor = EasyMock.mock("dataxProcessor", IDataxProcessor.class);

        IDataxGlobalCfg dataxGlobalCfg = EasyMock.mock("dataxGlobalCfg", IDataxGlobalCfg.class);
        EasyMock.expect(processor.getDataXGlobalCfg()).andReturn(dataxGlobalCfg).anyTimes();
        //EasyMock.expect(processor.getWriter(null)).andReturn(dataXWriter);

        IDataxReader dataXReader = EasyMock.createMock("dataXReader", IDataxReader.class);

        //EasyMock.expect(processor.getReader(null)).andReturn(dataXReader);


        EasyMock.replay(processor, dataxGlobalCfg, dataXReader);

        String writerCfg = generateWriterCfg(dataXWriter, tableMap, processor, dataXReader);
        Assert.assertNotNull(writerCfg);
        System.out.println(writerCfg);
        EasyMock.verify(processor, dataxGlobalCfg, dataXReader);
        return writerCfg;
    }

    public static void valiateCfgGenerate(String assertFileName, DataxWriter dataXWriter, IDataxProcessor.TableMap tableMap) throws Exception {

//        IDataxProcessor processor = EasyMock.mock("dataxProcessor", IDataxProcessor.class);
//
//        IDataxGlobalCfg dataxGlobalCfg = EasyMock.mock("dataxGlobalCfg", IDataxGlobalCfg.class);
//        EasyMock.expect(processor.getDataXGlobalCfg()).andReturn(dataxGlobalCfg).anyTimes();
//        //EasyMock.expect(processor.getWriter(null)).andReturn(dataXWriter);
//
//        IDataxReader dataXReader = EasyMock.createMock("dataXReader", IDataxReader.class);
//
//        //EasyMock.expect(processor.getReader(null)).andReturn(dataXReader);
//
//
//        EasyMock.replay(processor, dataxGlobalCfg, dataXReader);

        String writerCfg = cfgGenerate(dataXWriter, tableMap);// generateWriterCfg(dataXWriter, tableMap, processor, dataXReader);
        Assert.assertNotNull(writerCfg);
        System.out.println(writerCfg);
        JsonUtil.assertJSONEqual(dataXWriter.getClass(), assertFileName, writerCfg, (message, expected, actual) -> {
            Assert.assertEquals(message, expected, actual);
        });
        JSONObject writer = JSON.parseObject(writerCfg);

        Assert.assertEquals(dataXWriter.getDataxMeta().getName(), writer.getString("name"));
    }

    public static String generateWriterCfg(DataxWriter dataXWriter
            , IDataxProcessor.TableMap tableMap
            , IDataxProcessor processor) throws IOException {

        IDataxReader dataxReader = new IDataxReader() {
            @Override
            public IStreamTableMeta getStreamTableMeta(String tableName) {
               throw new UnsupportedOperationException();
            }
            @Override
            public List<ISelectedTab> getSelectedTabs() {
                return null;
            }

            @Override
            public IGroupChildTaskIterator getSubTasks() {
                return null;
            }

            @Override
            public String getTemplate() {
                return null;
            }
        };
        return generateWriterCfg(dataXWriter, tableMap, processor, dataxReader);
    }

    private static String generateWriterCfg(DataxWriter dataXWriter
            , IDataxProcessor.TableMap tableMap
            , IDataxProcessor processor, IDataxReader dataXReader) throws IOException {
        MockDataxReaderContext mockReaderContext = new MockDataxReaderContext();
        DataXCfgGenerator dataProcessor = new DataXCfgGenerator(null, BasicTest.testDataXName, processor) {
            @Override
            public String getTemplateContent() {
                return dataXWriter.getTemplate();
            }
        };

        return dataProcessor.generateDataxConfig(mockReaderContext, dataXWriter, dataXReader, Optional.ofNullable(tableMap));
    }

    public static void realExecuteDump(final WriterJson writerJson, IDataXPluginMeta dataxWriter) throws IllegalAccessException {
        final IReaderPluginMeta readerMeta = new IReaderPluginMeta() {
            @Override
            public DataXMeta getDataxMeta() {
                DataXMeta randomStrMeta = new DataXMeta();
                randomStrMeta.setName("streamreader");
                randomStrMeta.setClass("com.alibaba.datax.plugin.reader.streamreader.StreamReader");
                return randomStrMeta;
            }

            @Override
            public String getReaderJsonCfgContent() {
                return "{\n" +
                        "          \"name\": \"streamreader\",\n" +
                        "          \"parameter\": {\n" +
                        "            \"column\": [\n" +
                        "              {\n" +
                        "                \"random\": \"3,4\",\n" +
                        "                \"type\": \"string\"\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"random\": \"10,10\",\n" +
                        "                \"type\": \"string\"\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"random\": \"1,2\",\n" +
                        "                \"type\": \"long\"\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"random\": \"2021050111,2021063111\",\n" +
                        "                \"type\": \"long\"\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"random\": \"4,999\",\n" +
                        "                \"type\": \"long\"\n" +
                        "              }\n" +
                        "            ],\n" +
                        "            \"sliceRecordCount\": 30\n" +
                        "          }\n" +
                        "        }";
            }
        };

        final IWriterPluginMeta writerMeta = new IWriterPluginMeta() {

            @Override
            public DataXMeta getDataxMeta() {
//                DataXMeta randomStrMeta = new DataXMeta();
//                randomStrMeta.setName("streamreader");
//                randomStrMeta.setClass("com.alibaba.datax.plugin.reader.streamreader.StreamReader");
//                return randomStrMeta;
                return dataxWriter.getDataxMeta();
            }

            @Override
            public Configuration getWriterJsonCfg() {
                Configuration cfg = null;
                if (writerJson.isPath()) {
                    cfg = IOUtils.loadResourceFromClasspath(
                            dataxWriter.getClass(), writerJson.getVal(), true
                            , (writerJsonInput) -> {
                                Configuration c = Configuration.from(writerJsonInput);
                                return c;
                            });
                } else {
                    cfg = Configuration.from(writerJson.getVal());
                }
                for (Function<Configuration, Configuration> setter : writerJson.cfgSetters) {
                    cfg = setter.apply(cfg);
                }
                return cfg;
            }
        };

        realExecuteDump(readerMeta, writerMeta);
    }

    /**
     * dataXWriter执行
     *
     * @param
     * @param writerMeta
     * @throws IllegalAccessException
     */
    public static void realExecuteDump(IReaderPluginMeta readerPluginMeta, IWriterPluginMeta writerMeta
    ) throws IllegalAccessException {
        // final JarLoader uberClassLoader = new JarLoader(new String[]{"."});
        final JarLoader uberClassLoader = new TISJarLoader(TIS.get().getPluginManager());
        IDataXPluginMeta.DataXMeta readerMeta = readerPluginMeta.getDataxMeta();
        DataxExecutor.initializeClassLoader(
                Sets.newHashSet("plugin.reader." + readerMeta.getName()
                        , "plugin.writer." + writerMeta.getDataxMeta().getName()), uberClassLoader);

//        Map<String, JarLoader> jarLoaderCenter = (Map<String, JarLoader>) jarLoaderCenterField.get(null);
//        jarLoaderCenter.clear();
//        jarLoaderCenter.put("plugin.reader.streamreader", uberClassLoader);
//        jarLoaderCenter.put("plugin.writer." + dataxWriter.getDataxMeta().getName(), uberClassLoader);

        Configuration allConf = IOUtils.loadResourceFromClasspath(MockDataxReaderContext.class //
                , "container.json", true, (input) -> {
                    Configuration cfg = Configuration.from(input);

//                    "streamreader": {
//                        "class": "com.alibaba.datax.plugin.reader.streamreader.StreamReader"
//                    }

                    cfg.set("plugin.reader." + readerMeta.getName() + ".class", readerMeta.getImplClass());
                    //    , "com.alibaba.datax.plugin.reader.streamreader.StreamReader");

                    cfg.set("plugin.writer." + writerMeta.getDataxMeta().getName() + ".class"
                            , writerMeta.getDataxMeta().getImplClass());
                    cfg.set("job.content[0].writer" //
                            , writerMeta.getWriterJsonCfg());

                    cfg.set("job.content[0].reader", Configuration.from(readerPluginMeta.getReaderJsonCfgContent()));

                    return cfg;
                });


        // 绑定column转换信息
        ColumnCast.bind(allConf);
        LoadUtil.bind(allConf);

        JobContainer container = new JobContainer(allConf);

        container.start();
    }

    public static IDataxProcessor.TableMap createCustomer_order_relationTableMap() {
        return createCustomer_order_relationTableMap(Optional.empty());
    }

    public static IDataxProcessor.TableMap createCustomer_order_relationTableMap(Optional<ISelectedTab> tab) {
        IDataxProcessor.TableMap tableMap
                = new IDataxProcessor.TableMap(tab.isPresent() ? tab.get() : new ISelectedTab() {
            @Override
            public String getName() {
                return TAB_customer_order_relation;
            }

            @Override
            public List<CMeta> getCols() {
                return createColMetas();
            }
        });
        tableMap.setTo(TAB_customer_order_relation);
        //tableMap.setSourceCols(sourceCols);
        return tableMap;
    }

    public static List<CMeta> createColMetas() {
        CMeta colMeta = null;
        List<CMeta> sourceCols = Lists.newArrayList();
        colMeta = new CMeta();
        colMeta.setName(customerregisterId);
        colMeta.setType(DataXReaderColType.STRING.dataType);
        colMeta.setPk(true);
        sourceCols.add(colMeta);

        colMeta = new CMeta();
        colMeta.setName("waitingorder_id");
        colMeta.setType(DataXReaderColType.STRING.dataType);
        colMeta.setPk(true);
        sourceCols.add(colMeta);

        colMeta = new CMeta();
        colMeta.setName(kind);
        colMeta.setType(DataXReaderColType.INT.dataType);
        sourceCols.add(colMeta);

        colMeta = new CMeta();
        colMeta.setName("create_time");
        colMeta.setType(DataXReaderColType.Long.dataType);
        sourceCols.add(colMeta);

        colMeta = new CMeta();
        colMeta.setName(lastVer);
        colMeta.setType(DataXReaderColType.INT.dataType);
        sourceCols.add(colMeta);
        return sourceCols;
    }
}
