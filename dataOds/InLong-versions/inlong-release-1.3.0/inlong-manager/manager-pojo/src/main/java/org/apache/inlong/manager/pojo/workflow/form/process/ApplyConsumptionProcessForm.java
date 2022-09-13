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

package org.apache.inlong.manager.pojo.workflow.form.process;

import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.inlong.manager.common.exceptions.FormValidateException;
import org.apache.inlong.manager.pojo.consumption.ConsumptionInfo;
import org.apache.inlong.manager.common.util.Preconditions;

import java.util.Map;

/**
 * New data consumption form
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApplyConsumptionProcessForm extends BaseProcessForm {

    public static final String FORM_NAME = "ApplyConsumptionProcessForm";

    @ApiModelProperty(value = "Data consumption information")
    private ConsumptionInfo consumptionInfo;

    @Override
    public void validate() throws FormValidateException {
        Preconditions.checkNotNull(consumptionInfo, "Data consumption information cannot be empty");
    }

    @Override
    public String getFormName() {
        return FORM_NAME;
    }

    @Override
    public String getInlongGroupId() {
        return consumptionInfo.getConsumerGroup();
    }

    @Override
    public Map<String, Object> showInList() {
        Map<String, Object> show = Maps.newHashMap();
        if (consumptionInfo != null) {
            show.put("inlongGroupId", consumptionInfo.getInlongGroupId());
        }
        return show;
    }
}
