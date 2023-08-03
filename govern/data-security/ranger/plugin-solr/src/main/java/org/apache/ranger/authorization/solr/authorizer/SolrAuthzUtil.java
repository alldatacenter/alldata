/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.authorization.solr.authorizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.services.solr.RangerSolrConstants;
import org.apache.solr.common.params.CollectionParams.CollectionAction;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.params.CollectionAdminParams;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.admin.ConfigSetsHandler;
import org.apache.solr.security.AuthorizationContext;
import org.apache.solr.security.AuthorizationContext.CollectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Solr authorization framework does not currently support fine-grained authorization.
 * Specifically it does not provide the resource names consistently. This utility class
 * extracts the resource names for specific APIs by introspecting the {@linkplain SolrParams}
 * passed as part of {@linkplain AuthorizationContext}. This class is taken from the Sentry implementation
 * to provide consistent behavior between Sentry-Solr and Ranger-Solr implementations.
 * It can possibly be removed once SOLR-11543 is available.
 */
class SolrAuthzUtil {
  private static final Logger LOG = LoggerFactory.getLogger(SolrAuthzUtil.class);

  /**
   * This method returns a list of Strings containing a name of Solr Config entities associated with the current
   * operation.
   */
  static List<String> getConfigAuthorizables (AuthorizationContext ctx) {
    List<String> result = new ArrayList<>(1);
    if (ctx.getHandler() instanceof ConfigSetsHandler) { // For Solr configset APIs
      String name = ctx.getParams().get(CommonParams.NAME);
      if (name != null) {
        result.add(name);
      }
    } else { // For Solr config APIs
      for (CollectionRequest r : ctx.getCollectionRequests()) {
        result.add(r.collectionName);
      }
    }
    if (result.isEmpty()) {
        LOG.debug("Missing collection name for the config operation with authorization context {}."
            + " Using * permissions for authorization check", toString(ctx));
      result.add("*");
    }

    return result;
  }

  /**
   * This method returns a List of Strings containing the names of schema entities associated with the current
   * operation.
   */
  static List<String> getSchemaAuthorizables (AuthorizationContext ctx) {
    List<String> result = new ArrayList<String>(1);
    for (CollectionRequest r : ctx.getCollectionRequests()) {
      result.add(r.collectionName);
    }
    if (result.isEmpty()) {
        LOG.debug("Missing collection name for the schema operation with authorization context {}."
            + " Using * permissions for authorization check", toString(ctx));
      result.add("*");
    }

    return result;
  }

  /**
   * This method extracts the Collection entity names
   * associated with this admin request and return a mapping of entity_name -> expected_auth_action.
   * This is used by Ranger-Solr authorization plugin to further restrict Solr admin operations.
   */
  static Map<String, RangerSolrConstants.ACCESS_TYPE> getCollectionsForAdminOp(AuthorizationContext ctx) {
    String actionName = ctx.getParams().get(CoreAdminParams.ACTION);
    CollectionAction action = CollectionAction.get(actionName);
    if (action != null) {
      switch (action) {
        case LISTSNAPSHOTS:
        case BACKUP: {
          String name = ctx.getParams().get(CollectionAdminParams.COLLECTION);
          return (name != null) ? Collections.singletonMap(name, RangerSolrConstants.ACCESS_TYPE.QUERY)
              : Collections.emptyMap();
        }

        case MIGRATE: {
          Map<String, RangerSolrConstants.ACCESS_TYPE> result = new HashMap<>();
          String source = ctx.getParams().get(CollectionAdminParams.COLLECTION);
          String target = ctx.getParams().get("target."+CollectionAdminParams.COLLECTION);
          if (source != null) {
            result.put (source,RangerSolrConstants.ACCESS_TYPE.QUERY);
          }
          if (target != null) {
            result.put (source, RangerSolrConstants.ACCESS_TYPE.UPDATE);
          }
          return result;
        }

        case DELETE:
        case CREATEALIAS:
        case DELETEALIAS:
        case CREATESHARD:
        case DELETESHARD:
        case SPLITSHARD:
        case RELOAD:
        case CREATE: {
          String name = ctx.getParams().get(CommonParams.NAME);
          return (name != null) ? Collections.singletonMap(name, RangerSolrConstants.ACCESS_TYPE.UPDATE)
              : Collections.emptyMap();
        }

        case DELETESNAPSHOT:
        case CREATESNAPSHOT:
        case SYNCSHARD:
        case MOVEREPLICA:
        case RESTORE:
        case MIGRATESTATEFORMAT:
        case FORCELEADER:
        case REBALANCELEADERS:
        case BALANCESHARDUNIQUE:
        case ADDREPLICAPROP:
        case DELETEREPLICAPROP:
        case ADDREPLICA:
        case DELETEREPLICA:
        case MODIFYCOLLECTION: {
          String name = ctx.getParams().get(CollectionAdminParams.COLLECTION);
          return (name != null) ? Collections.singletonMap(name, RangerSolrConstants.ACCESS_TYPE.UPDATE)
              : Collections.emptyMap();
        }

        case MOCK_COLL_TASK:
        case MOCK_REPLICA_TASK:
        case MOCK_SHARD_TASK:
        case REPLACENODE:
        case DELETENODE:
        case ADDROLE:
        case REMOVEROLE:
        case REQUESTSTATUS:
        case DELETESTATUS:
        case LIST:
        case LISTALIASES:
        case CLUSTERPROP:
        case OVERSEERSTATUS:
        case CLUSTERSTATUS: {
          return Collections.emptyMap();
        }
      }
    }

    return Collections.emptyMap();
  }

  /**
   * This method extracts the names of Solr Collection entities
   * associated with this admin request and return a mapping of entity_name -> expected_auth_action.
   * This is used by Ranger-Solr authorization plugin to further restrict Solr admin operations.
   */
  static Map<String, RangerSolrConstants.ACCESS_TYPE> getCoresForAdminOp(AuthorizationContext ctx) {
    String actionName = ctx.getParams().get(CoreAdminParams.ACTION);
    CoreAdminAction action = CoreAdminAction.get(actionName);
    if (action != null) {
      switch (action) {
        case REQUESTBUFFERUPDATES:
        case REQUESTAPPLYUPDATES:
        case CREATE: {
          String coreName = ctx.getParams().get(CoreAdminParams.NAME);
          return (coreName != null) ? Collections.singletonMap(coreName, RangerSolrConstants.ACCESS_TYPE.UPDATE)
              : Collections.emptyMap();
        }

        case REQUESTSTATUS:
        case OVERSEEROP:
        case INVOKE:
          // TODO - is this correct ?
        case DELETEALIAS: {
          return Collections.emptyMap();
        }

        case REQUESTSYNCSHARD:
        case REJOINLEADERELECTION:
        case PREPRECOVERY:
        case FORCEPREPAREFORLEADERSHIP:
        case CREATESNAPSHOT:
        case DELETESNAPSHOT:
        case RESTORECORE:
        case REQUESTRECOVERY:
        case SPLIT:
        case MERGEINDEXES:
        case UNLOAD:
        case RENAME:
        case RELOAD: {
          String coreName = ctx.getParams().get(CoreAdminParams.CORE);
          return (coreName != null) ? Collections.singletonMap(coreName, RangerSolrConstants.ACCESS_TYPE.UPDATE)
              : Collections.emptyMap();
        }

        case LISTSNAPSHOTS:
        case BACKUPCORE:
        case STATUS: {
          String coreName = ctx.getParams().get(CoreAdminParams.CORE);
          return (coreName != null) ? Collections.singletonMap(coreName, RangerSolrConstants.ACCESS_TYPE.QUERY)
              : Collections.emptyMap();
        }
        case SWAP: {
          Map<String, RangerSolrConstants.ACCESS_TYPE> result = new HashMap<>();
          String core1 = ctx.getParams().get(CoreAdminParams.CORE);
          String core2 = ctx.getParams().get("other");
          if (core1 != null) {
            result.put(core1, RangerSolrConstants.ACCESS_TYPE.UPDATE);
          }
          if (core2 != null) {
            result.put(core2, RangerSolrConstants.ACCESS_TYPE.UPDATE);
          }
          return result;
        }
      }
    }

    return Collections.emptyMap();
  }


  static String toString(AuthorizationContext ctx) {
    StringBuilder builder = new StringBuilder();
    builder.append("AuthorizationContext {");
    builder.append("userPrincipal : ");
    builder.append(ctx.getUserPrincipal().getName());
    // NOTE - comment out the code until SOLR-10814 is fixed.
    //builder.append(", userName : ");
    //builder.append(ctx.getUserName());
    builder.append(", collections : ");
    builder.append(ctx.getCollectionRequests());
    builder.append(", handler : ");
    builder.append(ctx.getHandler());
    builder.append(", HTTP method : ");
    builder.append(ctx.getHttpMethod());
    builder.append("}");

    return builder.toString();
  }


}
