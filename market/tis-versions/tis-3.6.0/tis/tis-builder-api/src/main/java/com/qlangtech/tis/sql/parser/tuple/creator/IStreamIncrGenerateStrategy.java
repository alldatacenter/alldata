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
package com.qlangtech.tis.sql.parser.tuple.creator;

import com.qlangtech.tis.order.center.IParamContext;
import com.qlangtech.tis.realtime.transfer.UnderlineUtils;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-06 09:49
 */
public interface IStreamIncrGenerateStrategy {

    String TEMPLATE_FLINK_TABLE_HANDLE_SCALA = "flink_table_handle_scala.vm";

    default boolean isExcludeFacadeDAOSupport() {
        return true;
    }

    default IStreamTemplateResource getFlinkStreamGenerateTplResource() {
        return IStreamTemplateResource.createClasspathResource("flink_source_handle_scala.vm", true);
    }

    public interface IStreamTemplateResource {
        public static IStreamTemplateResource createClasspathResource(String tplName, boolean relativePath) {
            return new ClasspathTemplateResource(relativePath ? ("/com/qlangtech/tis/classtpl/" + tplName) : tplName);
        }

        public static IStreamTemplateResource createStringContentResource(String content) {
            return new StringTemplateResource(content);
        }
    }

    static class ClasspathTemplateResource implements IStreamTemplateResource {
        private final String tplPath;

        public ClasspathTemplateResource(String tplName) {
            this.tplPath = tplName;
        }

        public String getTplPath() {
            return this.tplPath;
        }
    }

    static class StringTemplateResource implements IStreamTemplateResource {
        private final String tmpContent;

        public StringTemplateResource(String tmpContent) {
            this.tmpContent = tmpContent;
        }

        public Reader getContentReader() {
            return new StringReader(this.tmpContent);
        }
    }

    default IStreamTemplateData decorateMergeData(IStreamTemplateData mergeData) {
        return mergeData;
    }

//    Map<IEntityNameGetter, List<IValChain>> getTabTriggerLinker();
//
//    /**
//     * map<dbname,list<tables>>
//     *
//     * @return
//     */
//    Map<DBNode, List<String>> getDependencyTables(IDBTableNamesGetter dbTableNamesGetter);
//
//    IERRules getERRule();


    /**
     *
     **/
    interface IStreamTemplateData {

        String KEY_STREAM_SOURCE_TABLE_SUFFIX = "_source";

        /**
         * 该标签可以在生成的类上打印标签，该标签内容可以打印的日志输出内容之中用于比对 该份执行代码是否就是TIS中生成的那份脚本内容
         *
         * @return
         */
        public default String getCurrentTimestamp() {
            return IParamContext.getCurrentMillisecTimeStamp();
        }

        /**
         * TIS App 应用名称
         *
         * @return
         */
        public String getCollection();

        public default String getJavaName() {
            return UnderlineUtils.getJavaName(this.getCollection());
        }

        /**
         * TableAlias
         *
         * @param <T>
         * @return
         */
        public <T> List<T> getDumpTables();
    }

    public static abstract class AdapterStreamTemplateData implements IStreamTemplateData {
        private final IStreamTemplateData data;

        public AdapterStreamTemplateData(IStreamTemplateData data) {
            this.data = data;
        }

        @Override
        public String getCollection() {
            return data.getCollection();
        }

        @Override
        public String getJavaName() {
            return data.getJavaName();
        }

        @Override
        public List<EntityName> getDumpTables() {
            return data.getDumpTables();
        }
    }
}
