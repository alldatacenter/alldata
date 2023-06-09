/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.utils;

import com.netease.arctic.ams.api.AlreadyExistsException;
import com.netease.arctic.ams.api.MetaException;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.server.service.IMetaService;
import com.netease.arctic.table.TableIdentifier;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArcticMetaValidator {
  public static final Logger LOG = LoggerFactory.getLogger(ArcticMetaValidator.class);

  public static void metaValidator(com.netease.arctic.ams.api.TableIdentifier tableIdentifier)
      throws MetaException {
    if (StringUtils.isBlank(tableIdentifier.getTableName())) {
      throw new MetaException("table name is blank");
    }
    if (StringUtils.isBlank(tableIdentifier.getCatalog())) {
      throw new MetaException("catalog is blank");
    }
    if (StringUtils.isBlank(tableIdentifier.getDatabase())) {
      throw new MetaException("database is blank");
    }
  }

  public static void alreadyExistValidator(IMetaService metaService, TableIdentifier tableIdentifier)
      throws AlreadyExistsException {
    if (metaService.isExist(tableIdentifier)) {
      String exception = String.format("The table is existed, catalog: %s, db: %s, table:%s",
          tableIdentifier.getCatalog(),
          tableIdentifier.getDatabase(),
          tableIdentifier.getTableName());

      LOG.warn(exception);
      throw new AlreadyExistsException(exception);
    }
  }

  public static void nuSuchObjectValidator(IMetaService metaService, TableIdentifier tableIdentifier)
      throws MetaException, NoSuchObjectException {
    metaValidator(tableIdentifier.buildTableIdentifier());

    if (!metaService.isExist(tableIdentifier)) {
      String exception = String.format("The table is not existed, catalog: %s, db: %s, table:%s",
          tableIdentifier.getCatalog(),
          tableIdentifier.getDatabase(),
          tableIdentifier.getTableName());

      LOG.warn(exception);
      throw new NoSuchObjectException(exception);
    }
  }

}
