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
package com.qlangtech.tis.manage.biz.dal.dao.impl;

import com.qlangtech.tis.manage.biz.dal.dao.IApplicationDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.biz.dal.pojo.ApplicationCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;
import org.apache.commons.lang3.StringUtils;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ApplicationDAOImpl extends BasicDAO<Application, ApplicationCriteria> implements IApplicationDAO {

    @Override
    public String getEntityName() {
        return "application";
    }

    public ApplicationDAOImpl() {
        super();
    }

    @Override
    public Application selectByName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("param name can not be null");
        }
        Application key = new Application();
        key.setProjectName(name);
        return this.load("application.ibatorgenerated_selectByName", key);
    // return record;
    }

    public int countByExample(ApplicationCriteria example) {
        Integer count = this.count("application.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(ApplicationCriteria example) {
        Integer count = this.countFromWriterDB("application.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(ApplicationCriteria criteria) {
        return this.deleteRecords("application.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer appId) {
        Application key = new Application();
        key.setAppId(appId);
        return this.deleteRecords("application.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(Application record) {
        Object newKey = this.insert("application.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(Application record) {
        Object newKey = this.insert("application.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    public List<Application> selectByExample(ApplicationCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<Application> selectByExample(ApplicationCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<Application> list = this.list("application.ibatorgenerated_selectByExample", example);
        return list;
    }

    public Application selectByPrimaryKey(Integer appId) {
        Application key = new Application();
        key.setAppId(appId);
        Application record = this.load("application.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(Application record, ApplicationCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("application.ibatorgenerated_updateByExampleSelective", parms);
    }

    @Override
    public int updateLastProcessTime(String appname) {
        if (StringUtils.isBlank(appname)) {
            throw new IllegalArgumentException("param appId can not be null");
        }
        ApplicationCriteria criteria = new ApplicationCriteria();
        criteria.createCriteria().andProjectNameEqualTo(appname);
        return this.updateRecords("application.ibatorgenerated_updateLastProcessTime", criteria);
    }

    public int updateByExample(Application record, ApplicationCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("application.ibatorgenerated_updateByExample", parms);
    }

    public Application loadFromWriteDB(Integer appId) {
        Application key = new Application();
        key.setAppId(appId);
        Application record = this.loadFromWriterDB("application.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends ApplicationCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, ApplicationCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
