/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.utils;

import org.apache.inlong.tubemq.corebase.TBaseConstants;

public class SettingValidUtils {

    public static int validAndGetMsgSizeInMB(int inMaxMsgSizeInMB) {
        return MixedUtils.mid(inMaxMsgSizeInMB,
                TBaseConstants.META_MIN_ALLOWED_MESSAGE_SIZE_MB,
                TBaseConstants.META_MAX_ALLOWED_MESSAGE_SIZE_MB);
    }

    public static int validAndGetMsgSizeBtoMB(int inMaxMsgSizeInB) {
        return MixedUtils.mid(inMaxMsgSizeInB,
                TBaseConstants.META_MAX_MESSAGE_DATA_SIZE,
                TBaseConstants.META_MAX_MESSAGE_DATA_SIZE_UPPER_LIMIT)
                / TBaseConstants.META_MB_UNIT_SIZE;
    }

    public static int validAndXfeMaxMsgSizeFromMBtoB(int inMaxMsgSizeInMB) {
        return MixedUtils.mid(inMaxMsgSizeInMB,
                TBaseConstants.META_MIN_ALLOWED_MESSAGE_SIZE_MB,
                TBaseConstants.META_MAX_ALLOWED_MESSAGE_SIZE_MB)
                * TBaseConstants.META_MB_UNIT_SIZE;
    }

    public static int validAndGetMaxMsgSizeInB(int inMaxMsgSizeInB) {
        return MixedUtils.mid(inMaxMsgSizeInB,
                TBaseConstants.META_MAX_MESSAGE_DATA_SIZE,
                TBaseConstants.META_MAX_MESSAGE_DATA_SIZE_UPPER_LIMIT);
    }
}
