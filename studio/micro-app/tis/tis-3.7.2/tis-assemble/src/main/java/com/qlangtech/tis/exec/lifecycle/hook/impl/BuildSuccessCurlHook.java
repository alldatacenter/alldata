///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.exec.lifecycle.hook.impl;
//
//import com.qlangtech.tis.exec.impl.DefaultChainContext;
//import com.qlangtech.tis.manage.common.ConfigFileContext.StreamProcess;
//import com.qlangtech.tis.manage.common.HttpUtils;
//import com.qlangtech.tis.manage.common.PropertyPlaceholderHelper;
//import com.qlangtech.tis.manage.common.PropertyPlaceholderHelper.PlaceholderResolver;
//import com.qlangtech.tis.order.center.IParamContext;
//import com.qlangtech.tis.pubhook.common.RunEnvironment;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import java.io.InputStream;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.List;
//import java.util.Map;
//
///**
// * 构建完成触发curl命令
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2019年1月17日
// */
//public class BuildSuccessCurlHook extends AdapterIndexBuildLifeCycleHook {
//
//    private static final Logger logger = LoggerFactory.getLogger(BuildSuccessCurlHook.class);
//
//    private String url;
//
//    @Override
//    public void init(Map<String, String> params) {
//        this.url = params.get("url");
//        if (StringUtils.isEmpty(this.url)) {
//            throw new IllegalArgumentException("param url can not be null");
//        }
//    }
//
//    // http://localhost:8080/trigger?component.start=indexBackflow&ps=20160623001000&appname=search4_fat_instance
//    @Override
//    public void buildSuccess(IParamContext ctx) {
//        if (RunEnvironment.getSysRuntime() != RunEnvironment.ONLINE) {
//            logger.info("runtime:" + RunEnvironment.getSysRuntime() + " will skip this step");
//            return;
//        }
//        URL applyUrl = null;
//        try {
//            applyUrl = new URL(PropertyPlaceholderHelper.replace(url, new PlaceholderResolver() {
//
//                @Override
//                public String resolvePlaceholder(String placeholderName) {
//                    if (DefaultChainContext.KEY_PARTITION.equals(placeholderName)) {
//                        return ctx.getPartitionTimestampWithMillis();
//                    }
//                    throw new IllegalStateException("placeholderName:" + placeholderName + " is not illegal,shall be '" + DefaultChainContext.KEY_PARTITION + "'");
//                }
//            }));
//            logger.info("send buildSuccess hook apply url:" + applyUrl);
//            HttpUtils.processContent(applyUrl, new StreamProcess<Void>() {
//
//                @Override
//                public Void p(int status, InputStream stream, Map<String, List<String>> headerFields) {
//                    return null;
//                }
//            });
//        } catch (MalformedURLException e) {
//            logger.error(String.valueOf(applyUrl), e);
//        }
//    }
//}
