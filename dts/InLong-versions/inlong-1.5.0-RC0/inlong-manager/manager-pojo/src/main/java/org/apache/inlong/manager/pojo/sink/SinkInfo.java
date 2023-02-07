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

package org.apache.inlong.manager.pojo.sink;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Sink info - with stream
 */
@Data
@ApiModel("Sink info - with stream")
public class SinkInfo {

    private Integer id;
    private String inlongGroupId;
    private String inlongStreamId;
    private String sinkType;
    private String sinkName;
    private String dataNodeName;
    private String description;
    private Integer enableCreateResource;
    private String extParams;
    private Integer status;
    private String creator;

    // Inlong stream info
    private String mqResource;
    private String dataType;
    private String sourceSeparator; // Source separator configured in the stream info
    private String dataEscapeChar;

}
