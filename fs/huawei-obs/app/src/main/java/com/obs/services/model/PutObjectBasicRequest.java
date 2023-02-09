/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 **/

package com.obs.services.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.obs.services.internal.utils.ServiceUtils;

public abstract class PutObjectBasicRequest extends BaseObjectRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    protected Map<ExtensionObjectPermissionEnum, Set<String>> extensionPermissionMap;

    protected AccessControlList acl;

    protected String successRedirectLocation;

    protected SseKmsHeader sseKmsHeader;

    protected SseCHeader sseCHeader;

    public PutObjectBasicRequest() {
    }

    public PutObjectBasicRequest(String bucketName) {
        this.bucketName = bucketName;
    }


    /**
     * Obtain SSE-KMS encryption headers of the object.
     *
     * @return SSE-KMS encryption headers
     */
    public SseKmsHeader getSseKmsHeader() {
        return sseKmsHeader;
    }

    /**
     * Set SSE-KMS encryption headers of the object.
     *
     * @param sseKmsHeader
     *            SSE-KMS encryption headers
     */
    public void setSseKmsHeader(SseKmsHeader sseKmsHeader) {
        this.sseKmsHeader = sseKmsHeader;
    }

    /**
     * Obtain SSE-C encryption headers of the object.
     *
     * @return SSE-C encryption headers
     */
    public SseCHeader getSseCHeader() {
        return sseCHeader;
    }

    /**
     * Set SSE-C encryption headers of the object.
     *
     * @param sseCHeader
     *            SSE-C encryption headers
     */
    public void setSseCHeader(SseCHeader sseCHeader) {
        this.sseCHeader = sseCHeader;
    }

    /**
     * Obtain the ACL of the object.
     *
     * @return Object ACL
     */
    public AccessControlList getAcl() {
        return acl;
    }

    /**
     * Set the object ACL.
     *
     * @param acl
     *            Bucket ACL
     */
    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    /**
     * Obtain the redirection address after a successfully responded request.
     *
     * @return Redirection address
     */
    public String getSuccessRedirectLocation() {
        return successRedirectLocation;
    }

    /**
     * Set the redirection address after a successfully responded request.
     *
     * @param successRedirectLocation
     *            Redirection address
     */
    public void setSuccessRedirectLocation(String successRedirectLocation) {
        this.successRedirectLocation = successRedirectLocation;
    }

    /**
     * Grant the OBS extension permission to users.
     *
     * @param domainId
     *            ID of the domain to which the user belongs
     * @param extensionPermissionEnum
     *            OBS extension permission
     */
    public void grantExtensionPermission(String domainId, ExtensionObjectPermissionEnum extensionPermissionEnum) {
        if (extensionPermissionEnum == null || !ServiceUtils.isValid(domainId)) {
            return;
        }
        Set<String> users = getExtensionPermissionMap().computeIfAbsent(extensionPermissionEnum, k -> new HashSet<>());
        users.add(domainId.trim());
    }

    /**
     * Withdraw the OBS extension permission.
     *
     * @param domainId
     *            ID of the domain to which the user belongs
     * @param extensionPermissionEnum
     *            OBS extension permission
     */
    public void withdrawExtensionPermission(String domainId, ExtensionObjectPermissionEnum extensionPermissionEnum) {
        if (extensionPermissionEnum == null || !ServiceUtils.isValid(domainId)) {
            return;
        }
        domainId = domainId.trim();
        Set<String> domainIds = getExtensionPermissionMap().get(extensionPermissionEnum);
        if (domainIds != null) {
            domainIds.remove(domainId);
        }
    }

    /**
     * Withdraw all OBS extension permissions.
     *
     * @param domainId
     *            ID of the domain to which the user belongs
     */
    public void withdrawExtensionPermissions(String domainId) {
        if (ServiceUtils.isValid(domainId)) {
            for (Map.Entry<ExtensionObjectPermissionEnum, Set<String>> entry : this.getExtensionPermissionMap()
                    .entrySet()) {
                if (entry.getValue().contains(domainId.trim())) {
                    entry.getValue().remove(domainId);
                }
            }
        }
    }

    public Set<ExtensionObjectPermissionEnum> getAllGrantPermissions() {
        return this.getExtensionPermissionMap().keySet();
    }

    public Set<String> getDomainIdsByGrantPermission(ExtensionObjectPermissionEnum extensionPermissionEnum) {
        Set<String> domainIds = getExtensionPermissionMap().get(extensionPermissionEnum);
        if (domainIds == null) {
            domainIds = new HashSet<>();
        }
        return domainIds;
    }

    public Set<ExtensionObjectPermissionEnum> getGrantPermissionsByDomainId(String domainId) {
        Set<ExtensionObjectPermissionEnum> grantPermissions = new HashSet<ExtensionObjectPermissionEnum>();
        if (ServiceUtils.isValid(domainId)) {
            domainId = domainId.trim();
            for (Map.Entry<ExtensionObjectPermissionEnum, Set<String>> entry : this.getExtensionPermissionMap()
                    .entrySet()) {
                if (entry.getValue().contains(domainId)) {
                    grantPermissions.add(entry.getKey());
                }
            }
        }
        return grantPermissions;
    }

    public Map<ExtensionObjectPermissionEnum, Set<String>> getExtensionPermissionMap() {
        if (extensionPermissionMap == null) {
            extensionPermissionMap = new HashMap<>();
        }
        return extensionPermissionMap;
    }

    public void setExtensionPermissionMap(Map<ExtensionObjectPermissionEnum, Set<String>> extensionPermissionMap) {
        if (extensionPermissionMap == null) {
            return;
        }
        this.extensionPermissionMap = extensionPermissionMap;
    }

}
