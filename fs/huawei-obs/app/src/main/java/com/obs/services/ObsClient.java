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

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.obs.services.exception.ObsException;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.ReadAheadQueryResult;
import com.obs.services.model.ReadAheadRequest;
import com.obs.services.model.ReadAheadResult;
import com.obs.services.model.fs.NewBucketRequest;
import com.obs.services.model.fs.NewFileRequest;
import com.obs.services.model.fs.NewFolderRequest;
import com.obs.services.model.fs.ObsFSBucket;
import com.obs.services.model.fs.ObsFSFile;
import com.obs.services.model.fs.ObsFSFolder;
import com.obs.services.model.fs.WriteFileRequest;

/**
 * ObsClient
 */
public class ObsClient extends AbstractBatchClient {

    private static Map<String, Field> fields = new ConcurrentHashMap<String, Field>();
    
    /**
     * Constructor
     *
     * @param endPoint
     *            OBS endpoint
     *
     */
    public ObsClient(String endPoint) {
        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(endPoint);
        this.init("", "", null, config);
    }

    /**
     * Constructor
     *
     * @param config
     *            Configuration parameters of ObsClient
     *
     */
    public ObsClient(ObsConfiguration config) {
        if (config == null) {
            config = new ObsConfiguration();
        }
        this.init("", "", null, config);
    }

    /**
     * Constructor
     *
     * @param accessKey
     *            AK in the access key
     * @param secretKey
     *            SK in the access key
     * @param endPoint
     *            OBS endpoint
     *
     */
    public ObsClient(String accessKey, String secretKey, String endPoint) {
        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(endPoint);
        this.init(accessKey, secretKey, null, config);
    }

    /**
     * Constructor
     *
     * @param accessKey
     *            AK in the access key
     * @param secretKey
     *            SK in the access key
     * @param config
     *            Configuration parameters of ObsClient
     *
     */
    public ObsClient(String accessKey, String secretKey, ObsConfiguration config) {
        if (config == null) {
            config = new ObsConfiguration();
        }
        this.init(accessKey, secretKey, null, config);
    }

    /**
     * Constructor
     *
     * @param accessKey
     *            AK in the temporary access key
     * @param secretKey
     *            SK in the temporary access key
     * @param securityToken
     *            Security token
     * @param endPoint
     *            OBS endpoint
     *
     */
    public ObsClient(String accessKey, String secretKey, String securityToken, String endPoint) {
        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(endPoint);
        this.init(accessKey, secretKey, securityToken, config);
    }

    /**
     * Constructor
     *
     * @param accessKey
     *            AK in the temporary access key
     * @param secretKey
     *            SK in the temporary access key
     * @param securityToken
     *            Security token
     * @param config
     *            Configuration parameters of ObsClient
     *
     */
    public ObsClient(String accessKey, String secretKey, String securityToken, ObsConfiguration config) {
        if (config == null) {
            config = new ObsConfiguration();
        }
        this.init(accessKey, secretKey, securityToken, config);
    }

    public ObsClient(IObsCredentialsProvider provider, String endPoint) {
        ServiceUtils.assertParameterNotNull(provider, "ObsCredentialsProvider is null");
        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(endPoint);
        this.init(provider.getSecurityKey().getAccessKey(), provider.getSecurityKey().getSecretKey(),
                provider.getSecurityKey().getSecurityToken(), config);
        this.credentials.setObsCredentialsProvider(provider);
    }

    public ObsClient(IObsCredentialsProvider provider, ObsConfiguration config) {
        ServiceUtils.assertParameterNotNull(provider, "ObsCredentialsProvider is null");
        if (config == null) {
            config = new ObsConfiguration();
        }
        this.init(provider.getSecurityKey().getAccessKey(), provider.getSecurityKey().getSecretKey(),
                provider.getSecurityKey().getSecurityToken(), config);
        this.credentials.setObsCredentialsProvider(provider);
    }

    @Override
    public ReadAheadResult readAheadObjects(final ReadAheadRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "request is null");
        return this.doActionWithResult("readAheadObjects", request.getBucketName(),
                new ActionCallbackWithResult<ReadAheadResult>() {
                    @Override
                    public ReadAheadResult action() throws ServiceException {
                        return ObsClient.this.readAheadObjectsImpl(request);

                    }
                });
    }

    @Override
    public ReadAheadResult deleteReadAheadObjects(final String bucketName, final String prefix) throws ObsException {
        ServiceUtils.assertParameterNotNull(bucketName, "bucketName is null");
        ServiceUtils.assertParameterNotNull(prefix, "prefix is null");
        return this.doActionWithResult("deleteReadAheadObjects", bucketName,
                new ActionCallbackWithResult<ReadAheadResult>() {
                    @Override
                    public ReadAheadResult action() throws ServiceException {
                        return ObsClient.this.deleteReadAheadObjectsImpl(bucketName, prefix);

                    }
                });
    }

    @Override
    public ReadAheadQueryResult queryReadAheadObjectsTask(final String bucketName, final String taskId)
            throws ObsException {
        ServiceUtils.assertParameterNotNull(bucketName, "bucketName is null");
        ServiceUtils.assertParameterNotNull(taskId, "taskId is null");
        return this.doActionWithResult("queryReadAheadObjectsTask", bucketName,
                new ActionCallbackWithResult<ReadAheadQueryResult>() {
                    @Override
                    public ReadAheadQueryResult action() throws ServiceException {
                        return ObsClient.this.queryReadAheadObjectsTaskImpl(bucketName, taskId);

                    }
                });
    }
    
    @Override
    public ObsFSBucket newBucket(NewBucketRequest request) throws ObsException {
        ObsBucket bucket = this.createBucket(request);
        ObsFSBucket fsBucket = new ObsFSBucket(bucket.getBucketName(), bucket.getLocation());
        setInnerClient(fsBucket, this);
        return fsBucket;
    }

    @Override
    public ObsFSFile newFile(NewFileRequest request) throws ObsException {
        ObsFSFile obsFile = (ObsFSFile) this.putObject(request);
        setInnerClient(obsFile, this);
        return obsFile;
    }

    @Override
    public ObsFSFolder newFolder(NewFolderRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "CreateFolderRequest is null");
        if (request.getObjectKey() != null) {
            String delimiter = this.getFileSystemDelimiter();
            if (!request.getObjectKey().endsWith(delimiter)) {
                request.setObjectKey(request.getObjectKey() + delimiter);
            }
        }

        ObsFSFolder obsFolder = (ObsFSFolder) this.putObject(new PutObjectRequest(request));
        setInnerClient(obsFolder, this);
        return obsFolder;
    }
    
    @Override
    public ObsFSFile writeFile(final WriteFileRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "WriteFileRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        ObsFSFile obsFile = this.doActionWithResult("writeFile", request.getBucketName(),
                new ActionCallbackWithResult<ObsFSFile>() {
                    @Override
                    public ObsFSFile action() throws ServiceException {
                        if (null != request.getInput() && null != request.getFile()) {
                            throw new ServiceException("Both input and file are set, only one is allowed");
                        }
                        return ObsClient.this.writeFileImpl(request);
                    }
                });
        setInnerClient(obsFile, this);
        return obsFile;
    }
    
    private static void setInnerClient(final Object obj, final ObsClient obsClient) {
        if (obj != null && obsClient != null) {
            final Class<?> clazz = obj.getClass();
            final String name = clazz.getName();
            
            // fix findbugs: DP_DO_INSIDE_DO_PRIVILEGED
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    Field f = fields.get(name);
                    try {
                        if (f == null) {
                            f = getFieldFromClass(clazz, "innerClient");
                            f.setAccessible(true);
                            fields.put(name, f);
                        }
                        f.set(obj, obsClient);
                    } catch (Exception e) {
                        throw new ObsException(e.getMessage(), e);
                    }
                    return null;
                }
                
            });
        }
    }

    private static Field getFieldFromClass(Class<?> clazz, String key) {
        do {
            try {
                return clazz.getDeclaredField(key);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        } while (clazz != null);
        return null;
    }
}
