/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.type;

import static org.apache.atlas.type.AtlasStructType.AtlasAttribute.encodePropertyKey;

/**
 * Intg Constants.
 */
public final class Constants {

    public static final String INTERNAL_PROPERTY_KEY_PREFIX        = "__";

    /**
     * Shared System Attributes
     */
    public static final String TYPE_NAME_PROPERTY_KEY               = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "typeName");
    public static final String STATE_PROPERTY_KEY                   = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "state");
    public static final String CREATED_BY_KEY                       = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "createdBy");
    public static final String MODIFIED_BY_KEY                      = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "modifiedBy");
    public static final String TIMESTAMP_PROPERTY_KEY               = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "timestamp");
    public static final String MODIFICATION_TIMESTAMP_PROPERTY_KEY  = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "modificationTimestamp");

    /**
     * Entity-Only System Attributes
     */
    public static final String GUID_PROPERTY_KEY                    = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "guid");
    public static final String HISTORICAL_GUID_PROPERTY_KEY         = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "historicalGuids");
    public static final String LABELS_PROPERTY_KEY                  = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "labels");
    public static final String CUSTOM_ATTRIBUTES_PROPERTY_KEY       = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "customAttributes");
    public static final String CLASSIFICATION_TEXT_KEY              = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "classificationsText");
    public static final String CLASSIFICATION_NAMES_KEY             = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "classificationNames");
    public static final String PROPAGATED_CLASSIFICATION_NAMES_KEY  = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "propagatedClassificationNames");
    public static final String IS_INCOMPLETE_PROPERTY_KEY           = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "isIncomplete");
    public static final String PENDING_TASKS_PROPERTY_KEY           = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "pendingTasks");

    //Classification-Only System Attributes
    public static final String CLASSIFICATION_ENTITY_STATUS_PROPERTY_KEY = encodePropertyKey(INTERNAL_PROPERTY_KEY_PREFIX + "entityStatus");

    private Constants() {}
}