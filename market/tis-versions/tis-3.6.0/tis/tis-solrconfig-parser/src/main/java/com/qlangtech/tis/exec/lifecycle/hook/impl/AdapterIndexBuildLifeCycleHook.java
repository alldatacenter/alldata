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
package com.qlangtech.tis.exec.lifecycle.hook.impl;

import com.qlangtech.tis.exec.lifecycle.hook.IIndexBuildLifeCycleHook;
import com.qlangtech.tis.order.center.IParamContext;
import com.qlangtech.tis.solrdao.impl.ParseResult;
import com.qlangtech.tis.solrdao.extend.IndexBuildHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 适配一下下
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年1月17日
 */
public abstract class AdapterIndexBuildLifeCycleHook implements IIndexBuildLifeCycleHook {

    private static final Logger logger = LoggerFactory.getLogger(AdapterIndexBuildLifeCycleHook.class);

    public static IIndexBuildLifeCycleHook create(ParseResult schemaParse) {
        try {
            List<IIndexBuildLifeCycleHook> indexBuildLifeCycleHooks = new ArrayList<>();
            List<IndexBuildHook> indexBuildHooks = schemaParse.getIndexBuildHooks();
            for (IndexBuildHook buildHook : indexBuildHooks) {
                Class<?> clazz = Class.forName(buildHook.getFullClassName());
                IIndexBuildLifeCycleHook indexBuildHook = (IIndexBuildLifeCycleHook) clazz.newInstance();
                if (indexBuildHook instanceof AdapterIndexBuildLifeCycleHook) {
                    ((AdapterIndexBuildLifeCycleHook) indexBuildHook).init(buildHook.getParams());
                }
                indexBuildLifeCycleHooks.add(indexBuildHook);
            }
            return new IIndexBuildLifeCycleHook() {

                @Override
                public void start(IParamContext ctx) {
                    try {
                        indexBuildLifeCycleHooks.forEach((e) -> {
                            e.start(ctx);
                        });
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                @Override
                public void buildFaild(IParamContext ctx) {
                    try {
                        indexBuildLifeCycleHooks.forEach((e) -> {
                            e.buildFaild(ctx);
                        });
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                @Override
                public void buildSuccess(IParamContext ctx) {
                    try {
                        indexBuildLifeCycleHooks.forEach((e) -> {
                            e.buildSuccess(ctx);
                        });
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AdapterIndexBuildLifeCycleHook() {
        super();
    }

    public void init(Map<String, String> params) {
    }

    @Override
    public void start(IParamContext ctx) {
    }

    @Override
    public void buildFaild(IParamContext ctx) {
    }

    @Override
    public void buildSuccess(IParamContext ctx) {
    }
}
