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
 */

package com.obs.services.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.obs.services.internal.Constants;
import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.utils.ServiceUtils;

/**
 * Request parameters for uploading objects in a batch.
 *
 */
public class PutObjectsRequest extends AbstractBulkRequest {
    private String folderPath;

    private String prefix;

    private List<String> filePaths;

    private TaskCallback<PutObjectResult, PutObjectBasicRequest> callback;

    // Part size, in bytes, 5 MB by default.
    private long partSize = 1024 * 1024 * 5L;

    // Threshold size of a file for multipart upload, in bytes, 100 MB by
    // default.
    private long bigfileThreshold = 1024 * 1024 * 100L;

    // Number of threads for multipart upload, 1 by default.
    private int taskNum = 1;

    private UploadObjectsProgressListener listener;

    // Interval for updating the detailed information, 500 KB by default.
    private long taskProgressInterval = 5 * ObsConstraint.DEFAULT_PROGRESS_INTERVAL;

    // Callback threshold of the data transfer listener of each object, 100 KB
    // by default
    private long detailProgressInterval = ObsConstraint.DEFAULT_PROGRESS_INTERVAL;

    private Map<ExtensionObjectPermissionEnum, Set<String>> extensionPermissionMap;

    private AccessControlList acl;

    private String successRedirectLocation;

    private SseKmsHeader sseKmsHeader;

    private SseCHeader sseCHeader;

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param folderPath
     *            Local path from which the folder is uploaded
     */
    public PutObjectsRequest(String bucketName, String folderPath) {
        this.bucketName = bucketName;
        this.folderPath = folderPath;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param filePaths
     *            List of local paths from which a batch of files are uploaded
     */
    public PutObjectsRequest(String bucketName, List<String> filePaths) {
        this.bucketName = bucketName;
        this.filePaths = filePaths;
    }

    /**
     * Obtain the local path of the uploaded folder.
     * 
     * @return folderPath Local path from which the folder is uploaded
     */
    public String getFolderPath() {
        return folderPath;
    }

    /**
     * Obtain the list of local paths of the batch uploaded files.
     * 
     * @return filePaths List of local paths from which a batch of files are
     *         uploaded
     */
    public List<String> getFilePaths() {
        return filePaths;
    }

    /**
     * Obtain the callback object of an upload task.
     * 
     * @return callback Callback object
     */
    public TaskCallback<PutObjectResult, PutObjectBasicRequest> getCallback() {
        return callback;
    }

    /**
     * Set the callback object of an upload task.
     * 
     * @param callback
     *            Callback object
     */
    public void setCallback(TaskCallback<PutObjectResult, PutObjectBasicRequest> callback) {
        this.callback = callback;
    }

    /**
     * Obtain the part size set for uploading an object.
     * 
     * @return partSize Part size
     */
    public long getPartSize() {
        return partSize;
    }

    /**
     * Set the part size for uploading the object.
     * 
     * @param partSize
     *            Part size
     */
    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    /**
     * Obtain the threshold size of a file for starting multipart upload.
     * 
     * @return bigfileThreshold Threshold size of a file for multipart upload
     */
    public long getBigfileThreshold() {
        return bigfileThreshold;
    }

    /**
     * Set the threshold size of a file for starting multipart upload.
     * 
     * @param bigfileThreshold
     *            Threshold size of a file for multipart upload
     */
    public void setBigfileThreshold(long bigfileThreshold) {
        if (bigfileThreshold < Constants.MIN_PART_SIZE) {
            this.bigfileThreshold = Constants.MIN_PART_SIZE;
        } else {
            this.bigfileThreshold = Math.min(bigfileThreshold, Constants.MAX_PART_SIZE);
        }
    }

    /**
     * Obtain the maximum number of threads used for processing upload tasks
     * concurrently.
     * 
     * @return Maximum number of threads used for processing upload tasks
     *         concurrently
     */
    public int getTaskNum() {
        return taskNum;
    }

    /**
     * Set the maximum number of threads used for processing upload tasks
     * concurrently.
     * 
     * @param taskNum
     *            Maximum number of threads used for processing upload tasks
     *            concurrently
     */
    public void setTaskNum(int taskNum) {
        if (taskNum < 1) {
            this.taskNum = 1;
        } else {
            this.taskNum = Math.min(taskNum, 1000);
        }
    }

    /**
     * Obtain the specific folder to which the file is uploaded.
     * 
     * @return prefix Folder
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Set the specific folder to which the file is uploaded.
     * 
     * @param prefix
     *            Folder
     */
    public void setPrefix(String prefix) {
        if (null == prefix) {
            return;
        } else if (prefix.endsWith("/")) {
            this.prefix = prefix;
        } else {
            this.prefix = prefix + "/";
        }

    }

    /**
     * Obtain the progress listener of the bulk task.
     * 
     * @return Progress listener
     */
    public UploadObjectsProgressListener getUploadObjectsProgressListener() {
        return listener;
    }

    /**
     * Set the progress listener of the bulk task.
     * 
     * @param listener
     *            Progress listener
     */
    public void setUploadObjectsProgressListener(UploadObjectsProgressListener listener) {
        this.listener = listener;
    }

    /**
     * Obtain the interval for updating the task progress information.
     * 
     * @return taskProgressInterval Interval for updating the task progress
     */
    public long getTaskProgressInterval() {
        return taskProgressInterval;
    }

    /**
     * Set the interval for updating the task progress information.
     * 
     * @param taskProgressInterval
     *            Interval for updating the task progress
     */
    public void setTaskProgressInterval(long taskProgressInterval) {
        this.taskProgressInterval = Math.max(taskProgressInterval, this.detailProgressInterval);
    }

    /**
     * Obtain the callback threshold of the data transfer listener. The default
     * value is 100 KB.
     * 
     * @return Callback threshold of the data transfer listener
     */
    public long getDetailProgressInterval() {
        return detailProgressInterval;
    }

    /**
     * Set the callback threshold of the data transfer listener. The default
     * value is 100 KB.
     * 
     * @param detailProgressInterval
     *            Callback threshold of the data transfer listener
     */
    public void setDetailProgressInterval(long detailProgressInterval) {
        this.detailProgressInterval = detailProgressInterval;
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
     *            Object ACL
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
     * Grant the OBS extension permissions to a user.
     * 
     * @param domainId
     *            User's domain ID
     * @param extensionPermissionEnum
     *            OBS extension permissions
     */
    public void grantExtensionPermission(String domainId, ExtensionObjectPermissionEnum extensionPermissionEnum) {
        if (extensionPermissionEnum == null || !ServiceUtils.isValid(domainId)) {
            return;
        }
        Set<String> users = getExtensionPermissionMap().computeIfAbsent(extensionPermissionEnum, k -> new HashSet<>());
        users.add(domainId.trim());
    }

    /**
     * Withdraw the OBS extension permissions from a user.
     * 
     * @param domainId
     *            User's domain ID
     * @param extensionPermissionEnum
     *            OBS extension permissions
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
     * Withdraw all OBS extension permissions from a user.
     * 
     * @param domainId
     *            User's domain ID
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

    /**
     * Obtain all OBS extended permissions.
     * 
     * @return OBS List of extension permissions
     */
    public Set<ExtensionObjectPermissionEnum> getAllGrantPermissions() {
        return this.getExtensionPermissionMap().keySet();
    }

    /**
     * Obtain the list of user IDs with the specified OBS extension permissions.
     * 
     * @param extensionPermissionEnum
     *            OBS extension permissions
     * @return List of user IDs
     */
    public Set<String> getDomainIdsByGrantPermission(ExtensionObjectPermissionEnum extensionPermissionEnum) {
        Set<String> domainIds = getExtensionPermissionMap().get(extensionPermissionEnum);
        if (domainIds == null) {
            domainIds = new HashSet<String>();
        }
        return domainIds;
    }

    /**
     * Obtain the OBS extension permissions of a specified user.
     * 
     * @param domainId
     *            User ID
     * @return OBS Extension permission set
     */
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

    /**
     * Obtain the set of relationships between users and OBS extension
     * permissions.
     * 
     * @return Set of relationships between users and OBS extended permissions
     */
    public Map<ExtensionObjectPermissionEnum, Set<String>> getExtensionPermissionMap() {
        if (extensionPermissionMap == null) {
            extensionPermissionMap = new HashMap<ExtensionObjectPermissionEnum, Set<String>>();
        }
        return extensionPermissionMap;
    }

    /**
     * Set the set of relationships between users and OBS extension permissions.
     * 
     * @param extensionPermissionMap
     *            Set of relationships between users and OBS extended
     *            permissions
     */
    public void setExtensionPermissionMap(Map<ExtensionObjectPermissionEnum, Set<String>> extensionPermissionMap) {
        if (extensionPermissionMap == null) {
            return;
        }
        this.extensionPermissionMap = extensionPermissionMap;
    }

    @Override
    public String toString() {
        return "PutObjectsRequest [folderPath=" + folderPath + ", prefix=" + prefix + ", filePaths=" + filePaths
                + ", callback=" + callback + ", partSize=" + partSize + ", bigfileThreshold=" + bigfileThreshold
                + ", taskNum=" + taskNum + ", listener=" + listener + ", taskProgressInterval=" + taskProgressInterval
                + ", detailProgressInterval=" + detailProgressInterval + ", extensionPermissionMap="
                + extensionPermissionMap + ", acl=" + acl + ", getBucketName()=" + getBucketName()
                + ", isRequesterPays()=" + isRequesterPays() + "]";
    }
}
