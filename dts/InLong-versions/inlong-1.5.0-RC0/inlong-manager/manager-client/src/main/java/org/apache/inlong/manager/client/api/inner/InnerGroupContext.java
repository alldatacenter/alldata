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

package org.apache.inlong.manager.client.api.inner;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.MapUtils;
import org.apache.inlong.manager.client.api.InlongStream;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm;

import java.util.Map;

/**
 * InnerGroupContext.
 */
@Data
@NoArgsConstructor
public class InnerGroupContext {

    private InlongGroupInfo groupInfo;

    private Map<String, InnerStreamContext> streamContextMap = Maps.newHashMap();

    private Map<String, InlongStream> streamMap = Maps.newHashMap();

    private ApplyGroupProcessForm initMsg;

    public String getGroupId() {
        Preconditions.checkNotNull(groupInfo, "inlong group info was not init");
        return groupInfo.getInlongGroupId();
    }

    public void setStreamContext(InnerStreamContext streamContext) {
        Preconditions.checkTrue(streamContext != null && streamContext.getStreamInfo() != null,
                "stream context cannot be null");
        if (MapUtils.isEmpty(streamContextMap)) {
            streamContextMap = Maps.newHashMap();
        }
        streamContextMap.put(streamContext.getStreamInfo().getName(), streamContext);
    }

    public void setStream(InlongStream stream) {
        Preconditions.checkTrue(stream != null, "stream cannot be null");
        if (MapUtils.isEmpty(streamMap)) {
            streamMap = Maps.newHashMap();
        }
        streamMap.put(stream.getInlongStreamId(), stream);
    }

}
