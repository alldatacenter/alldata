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
package com.qlangtech.tis.dataplatform.dao.impl;

import com.qlangtech.tis.dataplatform.dao.IDataplatformDAOFacade;
import com.qlangtech.tis.dataplatform.dao.IDsDatasourceDAO;
import com.qlangtech.tis.dataplatform.dao.IDsTableDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DataplatformDAOFacadeImpl implements IDataplatformDAOFacade {

  private final IDsTableDAO dsTableDAO;

  private final IDsDatasourceDAO dsDatasourceDAO;


  public DataplatformDAOFacadeImpl(IDsTableDAO dsTableDAO, IDsDatasourceDAO dsDatasourceDAO) {
    this.dsTableDAO = dsTableDAO;
    this.dsDatasourceDAO = dsDatasourceDAO;
  }


  public IDsTableDAO getDsTableDAO() {
    return this.dsTableDAO;
  }

  public IDsDatasourceDAO getDsDatasourceDAO() {
    return this.dsDatasourceDAO;
  }
}
