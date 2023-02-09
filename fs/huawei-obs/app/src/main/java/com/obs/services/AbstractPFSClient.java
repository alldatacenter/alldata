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


package com.obs.services;

import java.io.File;
import java.io.InputStream;

import com.obs.services.exception.ObsException;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.DeleteObjectRequest;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.ModifyObjectRequest;
import com.obs.services.model.ModifyObjectResult;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.RenameObjectRequest;
import com.obs.services.model.RenameObjectResult;
import com.obs.services.model.TruncateObjectRequest;
import com.obs.services.model.TruncateObjectResult;
import com.obs.services.model.fs.DropFileRequest;
import com.obs.services.model.fs.DropFileResult;
import com.obs.services.model.fs.GetAttributeRequest;
import com.obs.services.model.fs.ListContentSummaryRequest;
import com.obs.services.model.fs.ListContentSummaryResult;
import com.obs.services.model.fs.ObsFSAttribute;
import com.obs.services.model.fs.ObsFSFile;
import com.obs.services.model.fs.ReadFileRequest;
import com.obs.services.model.fs.ReadFileResult;
import com.obs.services.model.fs.RenameRequest;
import com.obs.services.model.fs.RenameResult;
import com.obs.services.model.fs.TruncateFileRequest;
import com.obs.services.model.fs.TruncateFileResult;
import com.obs.services.model.fs.WriteFileRequest;
import com.obs.services.model.fs.ListContentSummaryFsResult;
import com.obs.services.model.fs.ListContentSummaryFsRequest;
import com.obs.services.model.fs.ContentSummaryFsRequest;
import com.obs.services.model.fs.ContentSummaryFsResult;
import com.obs.services.model.fs.accesslabel.DeleteAccessLabelRequest;
import com.obs.services.model.fs.accesslabel.DeleteAccessLabelResult;
import com.obs.services.model.fs.accesslabel.GetAccessLabelRequest;
import com.obs.services.model.fs.accesslabel.GetAccessLabelResult;
import com.obs.services.model.fs.accesslabel.SetAccessLabelRequest;
import com.obs.services.model.fs.accesslabel.SetAccessLabelResult;

public abstract class AbstractPFSClient extends AbstractMultipartObjectClient {
    
    /* (non-Javadoc)
     * @see com.obs.services.IFSClient#listContentSummary(com.obs.services.model.fs.listContentSummaryRequest)
     */
    @Override
    public ListContentSummaryResult listContentSummary(final ListContentSummaryRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "ListContentSummaryRequest is null");
        return this.doActionWithResult("listcontentsummary", request.getBucketName(),
            new ActionCallbackWithResult<ListContentSummaryResult>() {
                @Override
                public ListContentSummaryResult action() throws ServiceException {
                    return AbstractPFSClient.this.listContentSummaryImpl(request);
                }
            });
    }
    
    @Override
    public RenameObjectResult renameObject(final String bucketName, final String objectKey, final String newObjectKey)
            throws ObsException {
        RenameObjectRequest request = new RenameObjectRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setNewObjectKey(newObjectKey);
        return this.renameObject(request);
    }

    @Override
    public RenameObjectResult renameObject(final RenameObjectRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "RenameObjectRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "ObjectKey is null");
        ServiceUtils.assertParameterNotNull2(request.getNewObjectKey(), "NewObjectKey is null");

        return this.doActionWithResult("renameObject", request.getBucketName(),
                new ActionCallbackWithResult<RenameObjectResult>() {
                    @Override
                    public RenameObjectResult action() throws ServiceException {
                        return AbstractPFSClient.this.renameObjectImpl(request);
                    }
                });
    }

    @Override
    public TruncateObjectResult truncateObject(final String bucketName, final String objectKey, final long newLength)
            throws ObsException {
        TruncateObjectRequest request = new TruncateObjectRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setNewLength(newLength);
        return this.truncateObject(request);
    }

    @Override
    public TruncateObjectResult truncateObject(final TruncateObjectRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "TruncateObjectRequest is null");
        ServiceUtils.assertParameterNotNull(request.getNewLength(), "NewLength is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "ObjectKey is null");
        return this.doActionWithResult("truncateObject", request.getBucketName(),
                new ActionCallbackWithResult<TruncateObjectResult>() {

                    @Override
                    public TruncateObjectResult action() throws ServiceException {
                        return AbstractPFSClient.this.truncateObjectImpl(request);
                    }
                });
    }

    @Override
    public ModifyObjectResult modifyObject(final String bucketName, final String objectKey, final long position,
            File file) throws ObsException {
        ModifyObjectRequest request = new ModifyObjectRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setPosition(position);
        request.setFile(file);
        return this.modifyObject(request);
    }

    @Override
    public ModifyObjectResult modifyObject(final String bucketName, final String objectKey, final long position,
            InputStream input) throws ObsException {
        ModifyObjectRequest request = new ModifyObjectRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setPosition(position);
        request.setInput(input);
        return this.modifyObject(request);
    }

    @Override
    public ModifyObjectResult modifyObject(final ModifyObjectRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "ModifyObjectRequest is null");
        ServiceUtils.assertParameterNotNull(request.getPosition(), "position is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        return this.doActionWithResult("modifyObject", request.getBucketName(),
                new ActionCallbackWithResult<ModifyObjectResult>() {

                    @Override
                    public ModifyObjectResult action() throws ServiceException {
                        return AbstractPFSClient.this.modifyObjectImpl(request);
                    }
                });
    }

    @Override
    public ObsFSAttribute getAttribute(GetAttributeRequest request) throws ObsException {
        return (ObsFSAttribute) this.getObjectMetadata(request);
    }

    @Override
    public ReadFileResult readFile(ReadFileRequest request) throws ObsException {
        return (ReadFileResult) this.getObject(request);
    }

    @Override
    public ObsFSFile appendFile(WriteFileRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "WriteFileRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        ObjectMetadata metadata = this
                .getObjectMetadata(new GetObjectMetadataRequest(request.getBucketName(), request.getObjectKey()));
        if (request.getPosition() >= 0L && request.getPosition() != metadata.getNextPosition()) {
            throw new IllegalArgumentException("Where you proposed append to is not equal to length");
        }
        request.setPosition(metadata.getNextPosition());
        return this.writeFile(request);
    }

    @Override
    public RenameResult renameFile(final RenameRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "RenameRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "ObjectKey is null");
        ServiceUtils.assertParameterNotNull2(request.getNewObjectKey(), "NewObjectKey is null");

        return this.doActionWithResult("rename", request.getBucketName(), new ActionCallbackWithResult<RenameResult>() {
            @Override
            public RenameResult action() throws ServiceException {
                return AbstractPFSClient.this.renameFileImpl(request);
            }
        });
    }

    @Override
    public RenameResult renameFolder(RenameRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "RenameRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "ObjectKey is null");
        ServiceUtils.assertParameterNotNull2(request.getNewObjectKey(), "NewObjectKey is null");
        
        String delimiter = this.getFileSystemDelimiter();
        if (!request.getObjectKey().endsWith(delimiter)) {
            request.setObjectKey(request.getObjectKey() + delimiter);
        }

        if (!request.getNewObjectKey().endsWith(delimiter)) {
            request.setNewObjectKey(request.getNewObjectKey() + delimiter);
        }
        
        return this.renameFile(request);
    }

    @Override
    public TruncateFileResult truncateFile(final TruncateFileRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "TruncateFileRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "ObjectKey is null");
        return this.doActionWithResult("truncateFile", request.getBucketName(),
                new ActionCallbackWithResult<TruncateFileResult>() {

                    @Override
                    public TruncateFileResult action() throws ServiceException {
                        return AbstractPFSClient.this.truncateFileImpl(request);
                    }
                });
    }

    @Override
    public DropFileResult dropFile(final DropFileRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "DropFileRequest is null");
        return this.doActionWithResult("dropFile", request.getBucketName(),
                new ActionCallbackWithResult<DropFileResult>() {

                    @Override
                    public DropFileResult action() throws ServiceException {
                        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
                        return (DropFileResult) AbstractPFSClient.this.deleteObjectImpl(new DeleteObjectRequest(
                                request.getBucketName(), request.getObjectKey(), request.getVersionId()));
                    }
                });
    }

    @Override
    public ListContentSummaryFsResult listContentSummaryFs(final ListContentSummaryFsRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "ListContentSummaryFsRequest is null");
        return this.doActionWithResult("listcontentsummaryfs", request.getBucketName(),
                new ActionCallbackWithResult<ListContentSummaryFsResult>() {
                    @Override
                    public ListContentSummaryFsResult action() throws ServiceException {
                        return AbstractPFSClient.this.listContentSummaryFsImpl(request);
                    }
                });
    }

    @Override
    public ContentSummaryFsResult getContentSummaryFs(final ContentSummaryFsRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "ContentSummaryFsRequest is null");
        ServiceUtils.assertParameterNotNull(request.getDirName(), "DirName is null");
        return this.doActionWithResult("contentsummaryfs", request.getBucketName(),
                new ActionCallbackWithResult<ContentSummaryFsResult>() {
                    @Override
                    public ContentSummaryFsResult action() throws ServiceException {
                        return AbstractPFSClient.this.getContentSummaryFsImpl(request);
                    }
                });
    }

    @Override
    public SetAccessLabelResult setAccessLabelFs(SetAccessLabelRequest request) throws ObsException {
        return this.doActionWithResult("setaccesslabel", request.getBucketName(),
                new ActionCallbackWithResult<SetAccessLabelResult>() {
                    @Override
                    public SetAccessLabelResult action() throws ServiceException {
                        return AbstractPFSClient.this.setAccessLabelFsImpl(request);
                    }
                });
    }

    @Override
    public GetAccessLabelResult getAccessLabelFs(GetAccessLabelRequest request) throws ObsException {
        return this.doActionWithResult("getaccesslabel", request.getBucketName(),
                new ActionCallbackWithResult<GetAccessLabelResult>() {
                    @Override
                    public GetAccessLabelResult action() throws ServiceException {
                        return AbstractPFSClient.this.getAccessLabelFsImpl(request);
                    }
                });
    }

    @Override
    public DeleteAccessLabelResult deleteAccessLabelFs(DeleteAccessLabelRequest request) throws ObsException {
        return this.doActionWithResult("deleteaccesslabel", request.getBucketName(),
                new ActionCallbackWithResult<DeleteAccessLabelResult>() {
                    @Override
                    public DeleteAccessLabelResult action() throws ServiceException {
                        return AbstractPFSClient.this.deleteAccessLabelFsImpl(request);
                    }
                });
    }
}
