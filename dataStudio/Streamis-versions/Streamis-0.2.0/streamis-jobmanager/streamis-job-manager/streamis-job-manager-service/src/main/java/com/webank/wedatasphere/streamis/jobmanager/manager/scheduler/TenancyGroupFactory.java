/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.scheduler;

import org.apache.commons.lang3.StringUtils;
import org.apache.linkis.scheduler.queue.AbstractGroup;
import org.apache.linkis.scheduler.queue.SchedulerEvent;
import org.apache.linkis.scheduler.queue.fifoqueue.FIFOGroup;
import org.apache.linkis.scheduler.queue.fifoqueue.FIFOGroupFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tenancy group factory e
 */
public class TenancyGroupFactory extends FIFOGroupFactory {

    public static final String GROUP_NAME_PREFIX = "Tenancy-Group-";

    public static final String DEFAULT_TENANCY = "default";

    private static final Pattern TENANCY_IN_GROUP_PATTERN = Pattern.compile("^" + GROUP_NAME_PREFIX + "([-_\\w\\W]+)$");

    private List<String> tenancies = new ArrayList<>();

    public List<String> getTenancies() {
        return tenancies;
    }

    public void setTenancies(List<String> tenancies) {
        this.tenancies = tenancies;
    }

    @Override
    public AbstractGroup createGroup(String groupName) {
        return new FIFOGroup(groupName, getInitCapacity(groupName), getMaxCapacity(groupName));
    }

    @Override
    public String getGroupNameByEvent(SchedulerEvent event) {
        String tenancy = DEFAULT_TENANCY;
        if (Objects.nonNull(event) && event instanceof StreamisSchedulerEvent){
            tenancy = ((StreamisSchedulerEvent) event).getTenancy();
        }
        return StringUtils.isNotBlank(tenancy)?GROUP_NAME_PREFIX + tenancy : GROUP_NAME_PREFIX + DEFAULT_TENANCY;
    }

    public String getTenancyByGroupName(String groupName){
        String tenancy = DEFAULT_TENANCY;
        if (StringUtils.isNotBlank(groupName)){
            Matcher matcher = TENANCY_IN_GROUP_PATTERN.matcher(groupName);
            if (matcher.find()){
                tenancy = matcher.group(1);
            }
        }
        return tenancy;
    }
}
