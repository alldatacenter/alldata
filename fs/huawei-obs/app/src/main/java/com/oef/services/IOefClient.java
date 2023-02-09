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

package com.oef.services;

import java.io.IOException;

import com.obs.services.exception.ObsException;
import com.obs.services.model.HeaderResponse;
import com.oef.services.model.CreateAsyncFetchJobsRequest;
import com.oef.services.model.CreateAsynchFetchJobsResult;
import com.oef.services.model.PutExtensionPolicyRequest;
import com.oef.services.model.QueryExtensionPolicyResult;
import com.oef.services.model.QueryAsynchFetchJobsResult;

/**
 * OEF value-added service interface
 *
 */
public interface IOefClient {
    /**
     * Close OEF client and release connection resources.
     * 
     * @throws IOException
     *             I/O exception. This exception is thrown when resources fail
     *             to be closed.
     */
    void close() throws IOException;

    /**
     * Configure an asynchronous policy.
     * 
     * @param bucketName
     *            Bucket name
     * @param request
     *            Asynchronous policy
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse putExtensionPolicy(final String bucketName, final PutExtensionPolicyRequest request)
            throws ObsException;

    /**
     * Query an asynchronous policy
     * 
     * @param bucketName
     *            Bucket name
     * @return ExtensionPolicyResult
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    QueryExtensionPolicyResult queryExtensionPolicy(final String bucketName) throws ObsException;

    /**
     * Delete an asynchronous policy
     * 
     * @param bucketName
     *            Bucket name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse deleteExtensionPolicy(final String bucketName) throws ObsException;

    /**
     * Create an asynchronous fetch job
     * 
     * @param request
     *            Asynchronous fetch job
     * @return CreateAsynchFetchJobsResult
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    CreateAsynchFetchJobsResult createFetchJob(final CreateAsyncFetchJobsRequest request) throws ObsException;

    /**
     * Query an asynchronous fetch job
     * 
     * @param bucketName
     *            Bucket name
     * @param jobId
     *            Job ID
     * @return QueryAsynchFetchJobsResult
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    QueryAsynchFetchJobsResult queryFetchJob(final String bucketName, final String jobId) throws ObsException;
}
