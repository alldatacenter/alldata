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

import com.qlangtech.tis.manage.biz.dal.pojo.BizFuncAuthority;
import com.qlangtech.tis.manage.biz.dal.pojo.BizFuncAuthorityCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IBizFuncAuthorityDAO {

    List<BizFuncAuthority> selectWithGroupByFuncidAppid(BizFuncAuthorityCriteria criteria);

    int countByExample(BizFuncAuthorityCriteria example);

    int countFromWriteDB(BizFuncAuthorityCriteria example);

    int deleteByExample(BizFuncAuthorityCriteria criteria);

    int deleteByPrimaryKey(Integer bfId);

    Integer insert(BizFuncAuthority record);

    Integer insertSelective(BizFuncAuthority record);

    List<BizFuncAuthority> selectByExample(BizFuncAuthorityCriteria criteria);

    /**
     * 查找用户的定时任务
     * @param criteria
     * @return
     */
    List<BizFuncAuthority> selectAppDumpJob(BizFuncAuthorityCriteria criteria);

    List<BizFuncAuthority> selectByExample(BizFuncAuthorityCriteria example, int page, int pageSize);

    BizFuncAuthority selectByPrimaryKey(Integer bfId);

    int updateByExampleSelective(BizFuncAuthority record, BizFuncAuthorityCriteria example);

    int updateByExample(BizFuncAuthority record, BizFuncAuthorityCriteria example);

    BizFuncAuthority loadFromWriteDB(Integer bfId);
}
