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

package org.apache.inlong.manager.service.core;

import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupExtEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamExtEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.pojo.sort.standalone.SortFieldInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortSourceClusterInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortSourceGroupInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortSourceStreamInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortSourceStreamSinkInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortTaskInfo;

import java.util.List;

/**
 * Loader for sort service to load configs thought Cursor
 */
public interface SortConfigLoader {

    /**
     * Load all clusters by cursor
     *
     * @return list of clusters, including MQ cluster and DataProxy cluster
     */
    List<SortSourceClusterInfo> loadAllClusters();

    /**
     * Load stream sinks by cursor
     *
     * @return list of stream sinks
     */
    List<SortSourceStreamSinkInfo> loadAllStreamSinks();

    /**
     * Load groups by cursor
     *
     * @return list of group info
     */
    List<SortSourceGroupInfo> loadAllGroup();

    /**
     * Load backup group info by cursor
     *
     * # @param keyName key name
     *
     * @return list of backup group info
     */
    List<InlongGroupExtEntity> loadGroupBackupInfo(String keyName);

    /**
     * Load backup stream info by cursor
     *
     * @param keyName key name
     * @return list of backup stream info
     */
    List<InlongStreamExtEntity> loadStreamBackupInfo(String keyName);

    /**
     * Load all inlong stream info by cursor
     *
     * @return list of stream info
     */
    List<SortSourceStreamInfo> loadAllStreams();

    /**
     * Load all inlong stream sink entity by cursor
     *
     * @return List of stream sink entity
     */
    List<StreamSinkEntity> loadAllStreamSinkEntity();

    /**
     * Load all task info
     *
     * @return List of tasks
     */
    List<SortTaskInfo> loadAllTask();

    /**
     * Load all data node entity
     *
     * @return List of data node
     */
    List<DataNodeEntity> loadAllDataNodeEntity();

    /**
     * Load all fields info
     *
     * @return List of fields info
     */
    List<SortFieldInfo> loadAllFields();
}
