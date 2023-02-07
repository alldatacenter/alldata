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

package org.apache.inlong.sort.standalone.sink.cls;

import com.tencentcloudapi.cls.producer.common.LogItem;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;

import java.util.List;

/**
 * Handler to pares profile event to CLS {@literal List<LogItem>} format.
 */
public interface IEvent2LogItemHandler {

    /**
     * Parse event into CLS {@literal List<LogItem>} format.
     *
     * @param context Context of CLS sink.
     * @param event Event to be pares to {@literal List<LogItem>}
     * @return {@literal List<LogItem>}
     */
    List<LogItem> parse(ClsSinkContext context, ProfileEvent event);
}
