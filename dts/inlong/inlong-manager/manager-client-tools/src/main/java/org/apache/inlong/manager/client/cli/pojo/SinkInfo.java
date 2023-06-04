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

package org.apache.inlong.manager.client.cli.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.inlong.manager.client.cli.util.ParseStatus;
import org.apache.inlong.manager.common.enums.SimpleGroupStatus;

import java.util.Date;

/**
 * Sink info, including sink type, sink name, etc.
 */
@Data
public class SinkInfo {

    private Integer id;
    private String inlongGroupId;
    private String inlongStreamId;
    private String sinkType;
    private String sinkName;

    @ParseStatus(clazz = SimpleGroupStatus.class)
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date modifyTime;
}
