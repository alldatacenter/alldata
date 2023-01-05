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
package com.qlangtech.tis.manage.common;

import com.qlangtech.tis.order.dump.task.ITableDumpConstant;
import org.apache.commons.lang.StringUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.qlangtech.tis.job.common.JobCommon;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年1月17日
 */
public class IndexBuildParam {

    public static final String JOB_TYPE = "job_type";

    // 需要使用的插件
    // public static final String JOB_PLUGINS = "job_plugins";
    // 远端取用ParamsConfig的key，例如：使用yarn容器触发本地使用yarn，那远端也需要使用yarn，则需要将yarn配置对应的name传到远端服务器上去
    // public static final String JOB_PARAM_CONFIG_NAME = "param_config_name";
    public static final String JOB_TYPE_INDEX_BUILD = "indexbuild";

    public static final String JOB_TYPE_DUMP = "tabledump";

    public static final String INDEXING_BUILD_TABLE_TITLE_ITEMS = "indexing_buildtabletitleitems";

    public static final String INDEXING_OUTPUT_PATH = "indexing_outputpath";

    // public static final String INDEXING_SOURCE_TYPE = "indexing_sourcetype";
    /**
     * IndexBuilderTriggerFactory的名称
     */
    public static final String INDEXING_BUILDER_TRIGGER_FACTORY = "indexing_builder_trigger_factory";

    public static final String INDEXING_SOURCE_PATH = "indexing_sourcepath";

    public static final String INDEXING_SCHEMA_PATH = "indexing_schemapath";

    public static final String INDEXING_SOLRCONFIG_PATH = "indexing_solrconfig_path";

    public static final String INDEXING_SERVICE_NAME = "indexing_servicename";

    public static final String INDEXING_CORE_NAME = "indexing_corename";

    public static final String INDEXING_USER_NAME = "indexing_username";

    public static final String INDEXING_INCR_TIME = "indexing_incrtime";

    public static final String INDEXING_MAX_NUM_SEGMENTS = "indexing_maxNumSegments";

    public static final String INDEXING_GROUP_NUM = "indexing_groupnum";

    public static final String INDEXING_DELIMITER = "indexing_delimiter";

    // public static final String INDEXING_SOLR_VERSION = "job_solrversion";
    public static final String INDEXING_RECORD_LIMIT = "indexing_recordlimit";

    // 记录数count
    public static final String INDEXING_ROW_COUNT = "indexing_row_count";

    // 构建索引最大错误上限，超过这个上限之后索引构建会失败
    public static final String INDEXING_MAX_DOC_FAILD_LIMIT = "indexing_maxfail_limit";

    private static final List<String> allfields;

    static {
        try {
            List<String> names = new ArrayList<>();
            Field[] fields = IndexBuildParam.class.getDeclaredFields();
            for (Field f : fields) {
                if (!((Modifier.STATIC & f.getModifiers()) > 0 && StringUtils.startsWith(f.getName(), "INDEXING_"))) {
                    continue;
                }
                names.add(String.valueOf(f.get(null)));
            }
            fields = ITableDumpConstant.class.getDeclaredFields();
            for (Field f : fields) {
                names.add(String.valueOf(f.get(null)));
            }
            //names.add(ITableDumpConstant.DUMP_TABLE_DUMP_FACTORY_NAME);
            names.add(JOB_TYPE);
            names.add(JobCommon.KEY_TASK_ID);
            // names.add(JOB_PLUGINS);
            allfields = Collections.unmodifiableList(names);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getAllFieldName() {
        return allfields;
    }

    public static void main(String[] args) {
        List<String> fields = getAllFieldName();
        for (String f : fields) {
            System.out.println(f);
        }
    }
}
