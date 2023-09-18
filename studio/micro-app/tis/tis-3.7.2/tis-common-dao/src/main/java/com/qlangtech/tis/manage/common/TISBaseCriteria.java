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

import com.qlangtech.tis.ibatis.BasicCriteria;

import java.text.MessageFormat;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-11 15:12
 */
public abstract class TISBaseCriteria extends BasicCriteria {

    private static final MessageFormat DB_DERBY_PAGINATION_FORMAT = new MessageFormat(" OFFSET {0} ROWS FETCH NEXT {1} ROWS ONLY");
    private static final MessageFormat DB_MYSQL_PAGINATION_FORMAT = new MessageFormat(" limit {0},{1}");

    public final String getPaginationScript() {

        Config.TisDbConfig dbCfg = Config.getDbCfg();
        if (Config.DB_TYPE_DERBY.equals(dbCfg.dbtype)) {
            return DB_DERBY_PAGINATION_FORMAT.format(new Object[]{this.getSkip(), this.getPageSize()});
        } else if (Config.DB_TYPE_MYSQL.equals(dbCfg.dbtype)) {
            return DB_MYSQL_PAGINATION_FORMAT.format(new Object[]{this.getSkip(), this.getPageSize()});
        } else {
            throw new IllegalStateException("illegal dbtype:" + dbCfg.dbtype);
        }
    }
}
