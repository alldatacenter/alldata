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

package datart.security.util;

import datart.security.base.Permission;
import datart.security.base.ResourceType;

public class PermissionHelper {


    public static Permission vizPermission(String orgId,String roleId, String vizId, int permission) {
        return Permission.builder()
                .orgId(orgId)
                .roleId(roleId)
                .resourceType(ResourceType.VIZ.name())
                .resourceId(vizId)
                .permission(permission)
                .build();
    }

    public static Permission sourcePermission(String orgId,String roleId, String sourceId, int permission) {
        return Permission.builder()
                .orgId(orgId)
                .roleId(roleId)
                .resourceType(ResourceType.SOURCE.name())
                .resourceId(sourceId)
                .permission(permission)
                .build();
    }

    public static Permission viewPermission(String orgId,String roleId, String viewId, int permission) {
        return Permission.builder()
                .orgId(orgId)
                .roleId(roleId)
                .resourceType(ResourceType.VIEW.name())
                .resourceId(viewId)
                .permission(permission)
                .build();
    }

    public static Permission rolePermission(String orgId, int permission) {
        return Permission.builder()
                .orgId(orgId)
                .roleId("*")
                .resourceType(ResourceType.ROLE.name())
                .resourceId("*")
                .permission(permission)
                .build();
    }

    public static Permission userPermission(String orgId, int permission) {
        return Permission.builder()
                .orgId(orgId)
                .roleId("*")
                .resourceType(ResourceType.USER.name())
                .resourceId("*")
                .permission(permission)
                .build();
    }

    public static Permission schedulePermission(String orgId,String roleId, String scheduleId, int permission) {
        return Permission.builder()
                .orgId(orgId)
                .roleId(roleId)
                .resourceType(ResourceType.SCHEDULE.name())
                .resourceId(scheduleId)
                .permission(permission)
                .build();
    }

}
