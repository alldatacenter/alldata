/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.impala.hook;

import java.net.InetAddress;
import java.net.UnknownHostException;
import com.google.common.collect.Sets;
import java.io.IOException;
import org.apache.atlas.hook.AtlasHook;
import org.apache.atlas.impala.hook.events.BaseImpalaEvent;
import org.apache.atlas.impala.hook.events.CreateImpalaProcess;
import org.apache.atlas.impala.model.ImpalaOperationType;
import org.apache.atlas.impala.model.ImpalaQuery;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.util.HashSet;

import static org.apache.atlas.repository.Constants.IMPALA_SOURCE;

public class ImpalaLineageHook extends AtlasHook {
    private static final Logger LOG = LoggerFactory.getLogger(ImpalaLineageHook.class);
    public static final String ATLAS_ENDPOINT                      = "atlas.rest.address";
    public static final String REALM_SEPARATOR                     = "@";
    public static final String CONF_PREFIX                         = "atlas.hook.impala.";
    public static final String CONF_REALM_NAME                     = "atlas.realm.name";
    public static final String HDFS_PATH_CONVERT_TO_LOWER_CASE     = CONF_PREFIX + "hdfs_path.convert_to_lowercase";
    public static final String DEFAULT_HOST_NAME                   = "localhost";

    private static final String  realm;
    private static final boolean convertHdfsPathToLowerCase;
    private static       String  hostName;

    static {
        realm                      = atlasProperties.getString(CONF_REALM_NAME, DEFAULT_CLUSTER_NAME);  // what should default be ??
        convertHdfsPathToLowerCase = atlasProperties.getBoolean(HDFS_PATH_CONVERT_TO_LOWER_CASE, false);

        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.warn("No hostname found. Setting the hostname to default value {}", DEFAULT_HOST_NAME, e);
            hostName = DEFAULT_HOST_NAME;
        }
    }

    public ImpalaLineageHook() {

    }

    public String getMessageSource() {
        return IMPALA_SOURCE;
    }

    public void process(String impalaQueryString) throws Exception {
        if (StringUtils.isEmpty(impalaQueryString)) {
            LOG.warn("==> ImpalaLineageHook.process skips because the impalaQueryString is empty <==");
            return;
        }

        ImpalaQuery lineageQuery = AtlasType.fromJson(impalaQueryString, ImpalaQuery.class);
        process(lineageQuery);
    }

    public void process(ImpalaQuery lineageQuery) throws Exception {
        if (lineageQuery == null) {
            LOG.warn("==> ImpalaLineageHook.process skips because the query object is null <==");
            return;
        }

        if (StringUtils.isEmpty(lineageQuery.getQueryText())) {
            LOG.warn("==> ImpalaLineageHook.process skips because the query text is empty <==");
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> ImpalaLineageHook.process({})", lineageQuery.getQueryText());
        }

        try {
            ImpalaOperationType operationType = ImpalaOperationParser.getImpalaOperationType(lineageQuery.getQueryText());
            AtlasImpalaHookContext context =
                new AtlasImpalaHookContext(this, operationType, lineageQuery);
            BaseImpalaEvent event = null;

            switch (operationType) {
                    case CREATEVIEW:
                    case CREATETABLE_AS_SELECT:
                    case ALTERVIEW_AS:
                    case QUERY:
                        event = new CreateImpalaProcess(context);
                        break;
                default:
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("HiveHook.run({}): operation ignored", lineageQuery.getQueryText());
                    }
                    break;
            }

            if (event != null) {
                LOG.debug("Processing event: " + lineageQuery.getQueryText());

                final UserGroupInformation ugi = getUgiFromUserName(lineageQuery.getUser());

                super.notifyEntities(event.getNotificationMessages(), ugi);
            }
        } catch (Throwable t) {

            LOG.error("ImpalaLineageHook.process(): failed to process query {}",
                AtlasType.toJson(lineageQuery), t);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== ImpalaLineageHook.process({})", lineageQuery.getQueryText());
        }
    }

    public String getHostName() {
        return hostName;
    }

    private UserGroupInformation getUgiFromUserName(String userName)  throws IOException {
        String userPrincipal = userName.contains(REALM_SEPARATOR)? userName : userName + "@" + getRealm();
        Subject userSubject = new Subject(false, Sets.newHashSet(
            new KerberosPrincipal(userPrincipal)), new HashSet<Object>(),new HashSet<Object>());
        return UserGroupInformation.getUGIFromSubject(userSubject);
    }

    public String getRealm() {
        return realm;
    }

    public boolean isConvertHdfsPathToLowerCase() {
        return convertHdfsPathToLowerCase;
    }
}