/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.db;

import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_JOB_VERSION;
import static org.apache.inlong.agent.constant.AgentConstants.JOB_VERSION;

import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.common.constant.Constants;
import org.apache.inlong.common.db.CommandEntity;

import java.util.List;

/**
 * Command for database
 */
public class CommandDb {

    private final Db db;

    public CommandDb(Db db) {
        this.db = db;
    }

    /**
     * store manager command to db
     */
    public void storeCommand(CommandEntity commandEntity) {
        db.putCommand(commandEntity);
    }

    /**
     * get those commands not ack to manager
     */
    public List<CommandEntity> getUnackedCommands() {
        return db.searchCommands(false);
    }

    /**
     * save normal command result for trigger
     */
    public void saveNormalCmds(TriggerProfile profile, boolean success) {
        CommandEntity entity = new CommandEntity();
        entity.setId(CommandEntity.generateCommandId(profile.getTriggerId(), profile.getOpType()));
        entity.setTaskId(Integer.parseInt(profile.getTriggerId()));
        entity.setCommandResult(success ? Constants.RESULT_SUCCESS : Constants.RESULT_FAIL);
        entity.setVersion(profile.getInt(JOB_VERSION, DEFAULT_JOB_VERSION));
        entity.setAcked(false);
        storeCommand(entity);
    }

    /**
     * save special command result for trigger (retry\makeup\check)
     */
    public void saveSpecialCmds(Integer id, Integer taskId, boolean success) {
        CommandEntity entity = new CommandEntity();
        entity.setId(String.valueOf(id));
        entity.setTaskId(taskId);
        entity.setAcked(false);
        entity.setCommandResult(success ? Constants.RESULT_SUCCESS : Constants.RESULT_FAIL);
        storeCommand(entity);
    }
}
