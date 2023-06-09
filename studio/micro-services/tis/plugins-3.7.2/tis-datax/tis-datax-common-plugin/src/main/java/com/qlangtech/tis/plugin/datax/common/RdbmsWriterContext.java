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

package com.qlangtech.tis.plugin.datax.common;

import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-23 12:44
 **/
public abstract class RdbmsWriterContext<WRITER extends BasicDataXRdbmsWriter, DS extends BasicDataSourceFactory>
        extends BasicRdbmsContext<WRITER, DS> implements IDataxContext {
    private final String tableName;

    public RdbmsWriterContext(WRITER writer, IDataxProcessor.TableMap tabMapper) {
        super(writer, (DS) writer.getDataSourceFactory());
        this.tableName = tabMapper.getTo();
        this.setCols(tabMapper.getSourceCols().stream().map((c) -> c.getName()).collect(Collectors.toList()));
    }

    public String getTableName() {
        return this.tableName;
    }

    public boolean isContainPreSql() {
        return StringUtils.isNotBlank(this.plugin.preSql);
    }

    public boolean isContainPostSql() {
        return StringUtils.isNotBlank(this.plugin.postSql);
    }

    public String getPreSql() {
        return this.plugin.preSql;
    }

    public String getPostSql() {
        return this.plugin.postSql;
    }

    public boolean isContainSession() {
        return StringUtils.isNotEmpty(this.plugin.session);
    }

    public final String getSession() {
        return this.plugin.session;
    }

    public boolean isContainBatchSize() {
        return this.plugin.batchSize != null;
    }

    public int getBatchSize() {
        return this.plugin.batchSize;
    }


    public String getUserName() {
        return this.dsFactory.getUserName();
    }

    public String getPassword() {
        return StringUtils.trimToEmpty(this.dsFactory.getPassword());
    }

    public String getJdbcUrl() {
        List<String> jdbcUrls = this.dsFactory.getJdbcUrls();
        Optional<String> firstURL = jdbcUrls.stream().findFirst();
        if (!firstURL.isPresent()) {
            throw new IllegalStateException("can not find jdbc url");
        }
        return firstURL.get();
    }


}
