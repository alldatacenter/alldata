/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.server.base.dto;

import datart.core.entity.Schedule;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

@NoArgsConstructor
@Data
public class ScheduleBaseInfo {

    private String id;

    private String name;

    private String orgId;

    private String type;

    private boolean active;

    private String parentId;

    private Boolean isFolder;

    private Double index;


    public ScheduleBaseInfo(Schedule schedule) {
        BeanUtils.copyProperties(schedule, this);
    }

}
