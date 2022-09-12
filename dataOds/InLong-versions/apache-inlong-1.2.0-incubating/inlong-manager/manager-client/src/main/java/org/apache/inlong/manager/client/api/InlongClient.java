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

package org.apache.inlong.manager.client.api;

import org.apache.inlong.manager.client.api.enums.SimpleGroupStatus;
import org.apache.inlong.manager.client.api.impl.InlongClientImpl;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;

import java.util.List;
import java.util.Map;

/**
 * An interface to manipulate Inlong Cluster
 * <p/>
 * Example:
 *
 * <pre>
 * <code>
 *
 * ClientConfiguration configuration = ..
 * InlongClient client = InlongClient.create(${serviceUrl}, configuration);
 * InlongGroupInfo groupInfo = ..
 * InlongGroup group = client.createGroup(groupInfo);
 * InlongStreamInfo streamInfo = ..
 * InlongStreamBuilder builder = group.createStream(streamInfo);
 * StreamSource source = ..
 * StreamSink sink = ..
 * List StreamField fields = ..
 * InlongStream stream = builder.source(source).sink(sink).fields(fields).init();
 * group.init();
 * </code>
 * </pre>
 */
public interface InlongClient {

    /**
     * Create inlong client.
     *
     * @param serviceUrl the service url
     * @param configuration the configuration
     * @return the inlong client
     */
    static InlongClient create(String serviceUrl, ClientConfiguration configuration) {
        return new InlongClientImpl(serviceUrl, configuration);
    }

    /**
     * Create inlong group by the given group info
     *
     * @param groupInfo the group info
     * @return the inlong group
     * @throws Exception the exception
     */
    InlongGroup forGroup(InlongGroupInfo groupInfo) throws Exception;

    /**
     * List group list.
     *
     * @return the list
     * @throws Exception the exception
     */
    List<InlongGroup> listGroup(String expr, int status, int pageNum, int pageSize) throws Exception;

    /**
     * List group status
     *
     * @param groupIds inlong group id list
     * @return map of inlong group status list
     * @throws Exception the exception
     */
    Map<String, SimpleGroupStatus> listGroupStatus(List<String> groupIds) throws Exception;

    /**
     * Gets group.
     *
     * @param groupName the group name
     * @return the group
     * @throws Exception the exception
     */
    InlongGroup getGroup(String groupName) throws Exception;

}
