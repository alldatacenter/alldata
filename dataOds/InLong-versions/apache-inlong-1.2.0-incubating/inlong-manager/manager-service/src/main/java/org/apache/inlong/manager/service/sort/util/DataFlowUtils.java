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

package org.apache.inlong.manager.service.sort.util;

import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.group.GroupCheckService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Util for build data flow info.
 */
@Service
@Deprecated
public class DataFlowUtils {

    @Autowired
    private GroupCheckService groupCheckService;
    @Autowired
    private StreamSourceService streamSourceService;
    @Autowired
    private InlongStreamService streamService;

    /*
     * Create dataflow info for sort.
     */
    /*public DataFlowInfo createDataFlow(InlongGroupInfo groupInfo, StreamSink streamSink) {
        String groupId = streamSink.getInlongGroupId();
        String streamId = streamSink.getInlongStreamId();
        List<StreamSource> sourceList = streamSourceService.listSource(groupId, streamId);
        if (CollectionUtils.isEmpty(sourceList)) {
            throw new WorkflowListenerException(String.format("Source not found by groupId=%s and streamId=%s",
                    groupId, streamId));
        }

        // Get all field info
        List<FieldInfo> sourceFields = new ArrayList<>();
        List<FieldInfo> sinkFields = new ArrayList<>();

        // TODO Support more than one source and one sink
        final StreamSource streamSource = sourceList.get(0);
        boolean isAllMigration = SourceInfoUtils.isBinlogAllMigration(streamSource);

        List<FieldMappingUnit> mappingUnitList;
        InlongStreamInfo streamInfo = streamService.get(groupId, streamId);
        if (isAllMigration) {
            mappingUnitList = FieldInfoUtils.setAllMigrationFieldMapping(sourceFields, sinkFields);
        } else {
            mappingUnitList = FieldInfoUtils.createFieldInfo(streamInfo.getFieldList(),
                    streamSink.getFieldList(), sourceFields, sinkFields);
        }

        FieldMappingRule fieldMappingRule = new FieldMappingRule(mappingUnitList.toArray(new FieldMappingUnit[0]));

        // Get source info
        String masterAddress = commonOperateService.getSpecifiedParam(InlongGroupSettings.TUBE_MASTER_URL);
        PulsarClusterInfo pulsarCluster = commonOperateService.getPulsarClusterInfo(groupInfo.getMqType());
        org.apache.inlong.sort.protocol.source.SourceInfo sourceInfo = SourceInfoUtils.createSourceInfo(pulsarCluster,
                masterAddress, clusterBean,
                groupInfo, streamInfo, streamSource, sourceFields);

        // Get sink info
        SinkInfo sinkInfo = SinkInfoUtils.createSinkInfo(streamSource, streamSink, sinkFields);

        // Get transformation info
        TransformationInfo transInfo = new TransformationInfo(fieldMappingRule);

        // Get properties
        Map<String, Object> properties = new HashMap<>();
        if (MapUtils.isNotEmpty(streamSink.getProperties())) {
            properties.putAll(streamSink.getProperties());
        }
        properties.put(InlongGroupSettings.DATA_FLOW_GROUP_ID_KEY, groupId);

        return new DataFlowInfo(streamSink.getId(), sourceInfo, transInfo, sinkInfo, properties);
    }*/

}
