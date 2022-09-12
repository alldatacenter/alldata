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

package org.apache.inlong.manager.common.pojo.workflow.form;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.exceptions.FormValidateException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;

import java.util.List;
import java.util.Map;

/**
 * Form of create inlong group resource
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GroupResourceProcessForm extends BaseProcessForm {

    public static final String FORM_NAME = "GroupResourceProcessForm";

    private InlongGroupInfo groupInfo;

    @Getter
    @Setter
    private GroupOperateType groupOperateType = GroupOperateType.INIT;

    @Deprecated
    private String streamId;

    @Deprecated
    private List<InlongStreamInfo> streamInfos;

    public InlongGroupInfo getGroupInfo() {
        return groupInfo;
    }

    public void setGroupInfo(InlongGroupInfo groupInfo) {
        this.groupInfo = groupInfo;
    }

    @Override
    public void validate() throws FormValidateException {
    }

    @Override
    public String getFormName() {
        return FORM_NAME;
    }

    @Override
    public String getInlongGroupId() {
        return groupInfo.getInlongGroupId();
    }

    @Deprecated
    public String getInlongStreamId() {
        return streamId;
    }

    @Deprecated
    public void setInlongStreamId(String streamId) {
        this.streamId = streamId;
    }

    @Override
    public Map<String, Object> showInList() {
        Map<String, Object> show = Maps.newHashMap();
        show.put("inlongGroupId", groupInfo.getInlongGroupId());
        show.put("groupOperateType", this.groupOperateType);
        return show;
    }
}
