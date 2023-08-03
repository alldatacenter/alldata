/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.usergroupsync;

import org.apache.ranger.unixusersync.config.UserGroupSyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUserGroupSource {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractUserGroupSource.class);

    protected UserGroupSyncConfig config = UserGroupSyncConfig.getInstance();

    protected Mapper userNameRegExInst = null;
    protected Mapper groupNameRegExInst = null;


    public AbstractUserGroupSource() {
        String mappingUserNameHandler = config.getUserSyncMappingUserNameHandler();
        try {
            if (mappingUserNameHandler != null) {
                Class<Mapper> regExClass = (Class<Mapper>)Class.forName(mappingUserNameHandler);
                userNameRegExInst = regExClass.newInstance();
                if (userNameRegExInst != null) {
                    userNameRegExInst.init(UserGroupSyncConfig.SYNC_MAPPING_USERNAME);
                } else {
                    LOG.error("RegEx handler instance for username is null!");
                }
            }
        } catch (ClassNotFoundException cne) {
            LOG.error("Failed to load " + mappingUserNameHandler + " " + cne);
        } catch (Throwable te) {
            LOG.error("Failed to instantiate " + mappingUserNameHandler + " " + te);
        }

        String mappingGroupNameHandler = config.getUserSyncMappingGroupNameHandler();
        try {
            if (mappingGroupNameHandler != null) {
                Class<Mapper> regExClass = (Class<Mapper>)Class.forName(mappingGroupNameHandler);
                groupNameRegExInst = regExClass.newInstance();
                if (groupNameRegExInst != null) {
                    groupNameRegExInst.init(UserGroupSyncConfig.SYNC_MAPPING_GROUPNAME);
                } else {
                    LOG.error("RegEx handler instance for groupname is null!");
                }
            }
        } catch (ClassNotFoundException cne) {
            LOG.error("Failed to load " + mappingGroupNameHandler + " " + cne);
        } catch (Throwable te) {
            LOG.error("Failed to instantiate " + mappingGroupNameHandler + " " + te);
        }
    }

}
