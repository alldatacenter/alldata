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

/**
 * Grantee or grantee group and permission information,
 * {@link AccessControlList}
 */
public class GrantAndPermission {

    private GranteeInterface grantee;

    private Permission permission;

    private boolean delivered;

    /**
     * Constructor
     * 
     * @param grantee
     *            Grantee (group) name
     * @param permission
     *            Permission information
     */
    public GrantAndPermission(GranteeInterface grantee, Permission permission) {
        this.grantee = grantee;
        this.permission = permission;
    }

    /**
     * Obtain the grantee (group) information.
     * 
     * @return Grantee (group) information
     */
    public GranteeInterface getGrantee() {
        return grantee;
    }

    /**
     * Obtain the permission information.
     * 
     * @return Permission information
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * Check whether the bucket ACL is deliverable.
     * 
     * @return Identifier specifying whether the ACL is delivered
     */
    public boolean isDelivered() {
        return delivered;
    }

    /**
     * Specify whether to deliver the bucket ACL. (This is only applicable to
     * bucket ACLs.)
     * 
     * @param delivered
     *            Whether to deliver the bucket ACL
     */
    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    @Override
    public String toString() {
        return "GrantAndPermission [grantee=" + grantee + ", permission=" + permission + ", delivered=" + delivered
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (delivered ? 1231 : 1237);
        result = prime * result + ((grantee == null) ? 0 : grantee.hashCode());
        result = prime * result + ((permission == null) ? 0 : permission.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GrantAndPermission other = (GrantAndPermission) obj;
        if (delivered != other.delivered) {
            return false;
        }
        if (grantee == null) {
            if (other.grantee != null) {
                return false;
            }
        } else if (!grantee.equals(other.grantee)) {
            return false;
        }
        if (permission == null) {
            if (other.permission != null) {
                return false;
            }
        } else if (!permission.equals(other.permission)) {
            return false;
        }
        return true;
    }

}
