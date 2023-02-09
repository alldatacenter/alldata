/**
* Copyright 2019 Huawei Technologies Co.,Ltd.
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use
* this file except in compliance with the License.  You may obtain a copy of the
* License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software distributed
* under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations under the License.
**/

package com.obs.services.model;

import com.obs.services.internal.Constants;

/**
 * Permissions in the ACL
 */
public final class Permission {
    /**
     * Full control permission
     */
    public static final Permission PERMISSION_FULL_CONTROL = new Permission(Constants.PERMISSION_FULL_CONTROL);

    /**
     * Read permission
     */
    public static final Permission PERMISSION_READ = new Permission(Constants.PERMISSION_READ);

    /**
     * Write permission
     */
    public static final Permission PERMISSION_WRITE = new Permission(Constants.PERMISSION_WRITE);

    /**
     * ACL read permission
     */
    public static final Permission PERMISSION_READ_ACP = new Permission(Constants.PERMISSION_READ_ACP);

    /**
     * ACL write permission
     */
    public static final Permission PERMISSION_WRITE_ACP = new Permission(Constants.PERMISSION_WRITE_ACP);

    /**
     * Read permission on objects in the bucket
     */
    @Deprecated
    public static final Permission PERMISSION_READ_OBJECT = new Permission(Constants.PERMISSION_READ_OBJECT);

    /**
     * Full control permission on objects in the bucket
     */
    @Deprecated
    public static final Permission PERMISSION_FULL_CONTROL_OBJECT = new Permission(
            Constants.PERMISSION_FULL_CONTROL_OBJECT);

    private String permissionString = "";

    private Permission(String permissionString) {
        this.permissionString = permissionString;
    }

    public String getPermissionString() {
        return permissionString;
    }

    /**
     * Obtain a permission object based on the string.
     * 
     * @param str
     *            Permission name
     * @return Permission object corresponding to the permission name
     */
    public static Permission parsePermission(String str) {
        Permission permission = null;

        if (str.equals(PERMISSION_FULL_CONTROL.toString())) {
            permission = PERMISSION_FULL_CONTROL;
        } else if (str.equals(PERMISSION_READ.toString())) {
            permission = PERMISSION_READ;
        } else if (str.equals(PERMISSION_WRITE.toString())) {
            permission = PERMISSION_WRITE;
        } else if (str.equals(PERMISSION_READ_ACP.toString())) {
            permission = PERMISSION_READ_ACP;
        } else if (str.equals(PERMISSION_WRITE_ACP.toString())) {
            permission = PERMISSION_WRITE_ACP;
        } else if (str.equals(PERMISSION_READ_OBJECT.toString())) {
            permission = PERMISSION_READ_OBJECT;
        } else if (str.equals(PERMISSION_FULL_CONTROL_OBJECT.toString())) {
            permission = PERMISSION_FULL_CONTROL_OBJECT;
        } else {
            permission = new Permission(str);
        }
        return permission;
    }

    public String toString() {
        return permissionString;
    }

    public boolean equals(Object obj) {
        return (obj instanceof Permission) && toString().equals(obj.toString());
    }

    public int hashCode() {
        return permissionString.hashCode();
    }
}
