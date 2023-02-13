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

package org.apache.inlong.manager.common.validation;

import javax.validation.groups.Default;

/**
 * Used for validate update request fields group
 *
 * <p/>
 * In general, the request body of save and update can be shared,
 * but we need to verify the parameters of the two requests separately
 *
 * <p/>
 * For example, the request body save and update only have the difference in keys,
 * and this keys must be carried when updating, we can use it like this
 * <code>org.apache.inlong.manager.pojo.node.DataNodeRequest</code>
 */
public interface UpdateByKeyValidation extends Default {

}
