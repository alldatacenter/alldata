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

import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class BasicDAO<T, C> extends SqlMapClientDaoSupport {

    protected int count(String sqlmap, C param) {
        return (Integer) this.getSqlMapClientTemplate().queryForObject(sqlmap, param);
    }

    protected int countFromWriterDB(String sqlmap, C param) {
        return this.count(sqlmap, param);
    }

    protected int deleteRecords(String sqlmap, Object criteria) {
        return this.getSqlMapClientTemplate().delete(sqlmap, criteria);
    }

    protected Object insert(String sqlmap, T record) {
        return this.getSqlMapClientTemplate().insert(sqlmap, record);
    }

    @SuppressWarnings("unchecked")
    protected List<T> list(String sqlmap, C criteria) {
        return (List<T>) this.getSqlMapClientTemplate().queryForList(sqlmap, criteria);
    }

    @SuppressWarnings("unchecked")
    protected <TT> List<TT> listAnonymity(String sqlmap, Object criteria) {
        return (List<TT>) this.getSqlMapClientTemplate().queryForList(sqlmap, criteria);
    }

    @SuppressWarnings("unchecked")
    protected T load(String sqlmap, T query) {
        return (T) this.getSqlMapClientTemplate().queryForObject(sqlmap, query);
    }

    @SuppressWarnings("unchecked")
    protected T loadPojo(String sqlmap, Object query) {
        return (T) this.getSqlMapClientTemplate().queryForObject(sqlmap, query);
    }

    protected T loadFromWriterDB(String sqlmap, T query) {
        return this.load(sqlmap, query);
    }

    protected int updateRecords(String sqlmap, C criteria) {
        return this.getSqlMapClientTemplate().update(sqlmap, criteria);
    }
}
