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

package org.apache.inlong.manager.client.cli.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import org.apache.commons.lang3.EnumUtils;
import org.apache.inlong.manager.common.enums.SimpleGroupStatus;

/**
 * Class for inlong group status verification.
 */
public class GroupStatus implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (!EnumUtils.isValidEnum(SimpleGroupStatus.class, value)) {
            String msg = "should be one of the following values:\n"
                    + "\tCREATE, REJECTED, INITIALIZING, OPERATING, STARTED, FAILED, STOPPED, FINISHED, DELETED";
            throw new ParameterException("Parameter " + name + msg);
        }
    }
}
