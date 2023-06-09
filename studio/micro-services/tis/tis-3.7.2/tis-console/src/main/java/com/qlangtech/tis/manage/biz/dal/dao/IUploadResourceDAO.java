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
package com.qlangtech.tis.manage.biz.dal.dao;

import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;
import com.qlangtech.tis.manage.biz.dal.pojo.UploadResourceCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IUploadResourceDAO {

    int countByExample(UploadResourceCriteria example);

    int countFromWriteDB(UploadResourceCriteria example);

    int deleteByExample(UploadResourceCriteria criteria);

    int deleteByPrimaryKey(Long urId);

    Integer insert(UploadResource record);

    Integer insertSelective(UploadResource record);

    List<UploadResource> selectByExampleWithBLOBs(UploadResourceCriteria example);

    List<UploadResource> selectByExample(UploadResourceCriteria criteria);

    List<UploadResource> selectByExampleWithoutBLOBs(UploadResourceCriteria example, int page, int pageSize);

    UploadResource selectByPrimaryKey(Long urId);

    int updateByExampleSelective(UploadResource record, UploadResourceCriteria example);

    int updateByExampleWithBLOBs(UploadResource record, UploadResourceCriteria example);

    int updateByExampleWithoutBLOBs(UploadResource record, UploadResourceCriteria example);

    UploadResource loadFromWriteDB(Long urId);
}
