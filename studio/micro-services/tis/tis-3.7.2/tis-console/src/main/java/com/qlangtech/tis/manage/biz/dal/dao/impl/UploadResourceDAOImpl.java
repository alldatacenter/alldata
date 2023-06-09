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

// import com.koubei.persistence.BaseIbatisDAO;
import java.util.List;
import com.qlangtech.tis.manage.biz.dal.dao.IUploadResourceDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;
import com.qlangtech.tis.manage.biz.dal.pojo.UploadResourceCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class UploadResourceDAOImpl extends BasicDAO<UploadResource, UploadResourceCriteria> implements IUploadResourceDAO {

    @Override
    public String getEntityName() {
        return "upload_resource";
    }

    public UploadResourceDAOImpl() {
        super();
    }

    public int countByExample(UploadResourceCriteria example) {
        Integer count = this.count("upload_resource.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(UploadResourceCriteria example) {
        Integer count = this.countFromWriterDB("upload_resource.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(UploadResourceCriteria criteria) {
        return this.deleteRecords("upload_resource.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Long urId) {
        UploadResource key = new UploadResource();
        key.setUrId(urId);
        return this.deleteRecords("upload_resource.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(UploadResource record) {
        return (Integer) this.insert("upload_resource.ibatorgenerated_insert", record);
    }

    public Integer insertSelective(UploadResource record) {
        return (Integer) this.insert("upload_resource.ibatorgenerated_insertSelective", record);
    }

    // @SuppressWarnings("unchecked")
    public List<UploadResource> selectByExampleWithBLOBs(UploadResourceCriteria example) {
        List<UploadResource> list = this.list("upload_resource.ibatorgenerated_selectByExampleWithBLOBs", example);
        return list;
    }

    public List<UploadResource> selectByExample(UploadResourceCriteria criteria) {
        return this.selectByExampleWithoutBLOBs(criteria, 1, 100);
    }

    // @SuppressWarnings("unchecked")
    public List<UploadResource> selectByExampleWithoutBLOBs(UploadResourceCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<UploadResource> list = this.list("upload_resource.ibatorgenerated_selectByExample", example);
        return list;
    }

    public UploadResource selectByPrimaryKey(Long urId) {
        UploadResource key = new UploadResource();
        key.setUrId(urId);
        UploadResource record = this.load("upload_resource.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(UploadResource record, UploadResourceCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("upload_resource.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExampleWithBLOBs(UploadResource record, UploadResourceCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        int rows = this.updateRecords("upload_resource.ibatorgenerated_updateByExampleWithBLOBs", parms);
        return rows;
    }

    public int updateByExampleWithoutBLOBs(UploadResource record, UploadResourceCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("upload_resource.ibatorgenerated_updateByExample", parms);
    }

    public UploadResource loadFromWriteDB(Long urId) {
        UploadResource key = new UploadResource();
        key.setUrId(urId);
        UploadResource record = this.loadFromWriterDB("upload_resource.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends UploadResourceCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, UploadResourceCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
