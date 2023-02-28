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

import com.qlangtech.tis.manage.common.BasicDAO;
import com.qlangtech.tis.manage.biz.dal.dao.IResourceParametersDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.ResourceParameters;
import com.qlangtech.tis.manage.biz.dal.pojo.ResourceParametersCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ResourceParametersDAOImpl extends BasicDAO<ResourceParameters, ResourceParametersCriteria> implements IResourceParametersDAO {

    @Override
    public String getEntityName() {
        return "config_resource_parameters";
    }

    public ResourceParametersDAOImpl() {
        super();
    }

    public int countByExample(ResourceParametersCriteria example) {
        Integer count = this.count("resource_parameters.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(ResourceParametersCriteria example) {
        Integer count = this.countFromWriterDB("resource_parameters.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(ResourceParametersCriteria criteria) {
        return this.deleteRecords("resource_parameters.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Long rpId) {
        ResourceParameters key = new ResourceParameters();
        key.setRpId(rpId);
        return this.deleteRecords("resource_parameters.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Long insert(ResourceParameters record) {
        Object newKey = this.insert("resource_parameters.ibatorgenerated_insert", record);
        return (Long) newKey;
    }

    public Long insertSelective(ResourceParameters record) {
        Object newKey = this.insert("resource_parameters.ibatorgenerated_insertSelective", record);
        return (Long) newKey;
    }

    public List<ResourceParameters> selectByExample(ResourceParametersCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<ResourceParameters> selectByExample(ResourceParametersCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<ResourceParameters> list = this.list("resource_parameters.ibatorgenerated_selectByExample", example);
        return list;
    }

    public ResourceParameters selectByPrimaryKey(Long rpId) {
        ResourceParameters key = new ResourceParameters();
        key.setRpId(rpId);
        ResourceParameters record = this.load("resource_parameters.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(ResourceParameters record, ResourceParametersCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("resource_parameters.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(ResourceParameters record, ResourceParametersCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("resource_parameters.ibatorgenerated_updateByExample", parms);
    }

    public ResourceParameters loadFromWriteDB(Long rpId) {
        ResourceParameters key = new ResourceParameters();
        key.setRpId(rpId);
        ResourceParameters record = this.loadFromWriterDB("resource_parameters.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends ResourceParametersCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, ResourceParametersCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
