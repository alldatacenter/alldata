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

package com.qlangtech.tis.sql.parser;

import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.plugin.ds.ColMeta;
import com.qlangtech.tis.plugin.ds.IDBReservedKeys;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface ISqlTask {

    public String getId();

    String getExportName();

    public List<DependencyNode> getDependencies();

    String getSql();

    RewriteSql getRewriteSql(String taskName, TabPartitions dumpPartition, Supplier<IPrimaryTabFinder> erRules
            , IJoinTaskContext templateContext, boolean isFinalNode);

    class RewriteSql {
        private static final MessageFormat SQL_INSERT_TABLE
                = new MessageFormat("INSERT OVERWRITE TABLE {0} PARTITION (" + IDumpTable.PARTITION_PT + "," + IDumpTable.PARTITION_PMOD + ") \n {1}");

        public final String originSql;
        public final String rewriteSql;

        public final IAliasTable primaryTable;

        /**
         * @see com.qlangtech.tis.plugin.ds.ColMeta
         * @see com.qlangtech.tis.plugin.ds.ColumnMetaData
         */
        private final List<ColMeta> cols;

        public List<ColMeta> getCols() {
            if (CollectionUtils.isEmpty(this.cols)) {
                throw new IllegalStateException("cols can not be null");
            }
            return this.cols;
        }


        /**
         * 除去ps列
         */
        public List<ColMeta> getColsExcludePartitionCols() {
            return getCols().stream().filter((r) -> !IDumpTable.preservedPsCols.contains(r.getName())).collect(Collectors.toList());
        }

        /**
         * @param rewriteSql
         * @param cols         the finally output cols
         * @param primaryTable
         */
        public RewriteSql(String originSql, String rewriteSql, List<ColMeta> cols, IAliasTable primaryTable) {
            if (StringUtils.isEmpty(originSql)) {
                throw new IllegalArgumentException("param originSql can not be empty");
            }
            this.originSql = originSql;
            this.rewriteSql = rewriteSql;
            this.primaryTable = primaryTable;
            this.cols = cols;
        }


        public String convert2InsertIntoSQL(IDBReservedKeys dbReservedKeys, String exportTabName) {
            final EntityName newCreateTab = EntityName.parse(exportTabName);
            return SQL_INSERT_TABLE.format(
                    new Object[]{newCreateTab.getFullName(dbReservedKeys.getEscapeChar())
                            , this.rewriteSql});
        }

    }
}
