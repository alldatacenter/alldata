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

package org.apache.inlong.agent.constant;

/**
 *  Metadata type
 */
public class MetadataConstants {

    public static final String DATA_CONTENT = "__content__";
    public static final String DATA_CONTENT_TIME = "__LogTime__";

    // k8s metadata
    public static final String METADATA_CONTAINER_ID = "__container_id__";
    public static final String METADATA_CONTAINER_NAME = "__container_name__";
    public static final String METADATA_NAMESPACE = "__namespace__";
    public static final String METADATA_POD_UID = "__pod_uid__";
    public static final String METADATA_POD_NAME = "__pod_name__";
    public static final String METADATA_POD_LABEL = "__pod_label__";

    // cvm metadata
    public static final String ENV_CVM = "cvm";
    public static final String METADATA_HOST_NAME = "__HostName__";
    public static final String METADATA_SOURCE_IP = "__SourceIP__";
    public static final String METADATA_FILE_NAME = "__FileName__";
}
