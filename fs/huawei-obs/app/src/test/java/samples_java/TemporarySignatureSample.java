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
package samples_java;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.ObsConvertor;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.BucketCors;
import com.obs.services.model.BucketCorsRule;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.SpecialParamEnum;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This sample demonstrates how to do common operations in temporary signature
 * way on OBS using the OBS SDK for Java.
 */
public class TemporarySignatureSample {
	private static final String endPoint = "http://your-endpoint";

	private static final String ak = "*** Provide your Access Key ***";

	private static final String sk = "*** Provide your Secret Key ***";

	private static ObsClient obsClient;

	private static String bucketName = "my-obs-bucket-demo";

	private static String objectKey = "my-obs-object-key-demo";

	private static OkHttpClient httpClient = new OkHttpClient.Builder().followRedirects(false)
			.retryOnConnectionFailure(false).cache(null).build();


	public static void main(String[] args) {
		ObsConfiguration config = new ObsConfiguration();
		config.setEndPoint(endPoint);
		config.setAuthType(AuthTypeEnum.OBS);
		try {
			/*
			 * Constructs a obs client instance with your account for accessing OBS
			 */
			obsClient = new ObsClient(ak, sk, config);

			/*
			 * Create bucket
			 */
			doCreateBucket();

			/*
			 * Set/Get/Delete bucket cors
			 */
			doBucketCorsOperations();

			/*
			 * Create object
			 */
			doCreateObject();

			/*
			 * Get object
			 */
			doGetObject();

			/*
			 * Set/Get object acl
			 */
			doObjectAclOperations();

			/*
			 * Delete object
			 */
			doDeleteObject();

			/*
			 * Delete bucket
			 */
			doDeleteBucket();

		} catch (Exception ex) {
			if (ex instanceof ObsException) {
				ObsException e = (ObsException) ex;
				System.out.println("Response Code: " + e.getResponseCode());
				System.out.println("Error Message: " + e.getErrorMessage());
				System.out.println("Error Code:       " + e.getErrorCode());
				System.out.println("Request ID:      " + e.getErrorRequestId());
				System.out.println("Host ID:           " + e.getErrorHostId());
			} else {
				ex.printStackTrace();
			}
		} finally {
			if (obsClient != null) {
				try {
					/*
					 * Close obs client
					 */
					obsClient.close();
				} catch (IOException e) {
				}
			}
		}

	}

	private static void getResponse(Request request) throws IOException {
		Call c = httpClient.newCall(request);
		Response res = c.execute();
		System.out.println("\tStatus:" + res.code());
		if (res.body() != null) {
			String content = res.body().string();
			if (content == null || content.trim().equals("")) {
				System.out.println("\n");
			} else {
				System.out.println("\tContent:" + content + "\n\n");
			}
		} else {
			System.out.println("\n");
		}
		res.close();
	}

	private static Request.Builder getBuilder(TemporarySignatureResponse res) {
		Request.Builder builder = new Request.Builder();
		for (Map.Entry<String, String> entry : res.getActualSignedRequestHeaders().entrySet()) {
			builder.header(entry.getKey(), entry.getValue());
		}
		return builder.url(res.getSignedUrl());
	}

	private static void doDeleteObject() throws IOException {
		TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.DELETE, 300);
		req.setBucketName(bucketName);
		req.setObjectKey(objectKey);
		TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
		System.out.println("Deleting object using temporary signature url:");
		System.out.println("\t" + res.getSignedUrl());

		getResponse(getBuilder(res).delete().build());
	}

	private static void doObjectAclOperations() throws ObsException, IOException {

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("x-obs-acl", "public-read");

		TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.PUT, 300);
		req.setBucketName(bucketName);
		req.setObjectKey(objectKey);
		req.setHeaders(headers);
		req.setSpecialParam(SpecialParamEnum.ACL);
		TemporarySignatureResponse res = obsClient.createTemporarySignature(req);

		System.out.println("Setting object ACL to public-read using temporary signature url:");
		System.out.println("\t" + res.getSignedUrl());
		Request.Builder builder = getBuilder(res);
		builder.url(res.getSignedUrl()).put(RequestBody.create(null, "".getBytes("UTF-8")));
		getResponse(builder.build());

		TemporarySignatureRequest req2 = new TemporarySignatureRequest(HttpMethodEnum.GET, 300);
		req2.setBucketName(bucketName);
		req2.setObjectKey(objectKey);
		req2.setSpecialParam(SpecialParamEnum.ACL);
		TemporarySignatureResponse res2 = obsClient.createTemporarySignature(req2);
		System.out.println("Getting object ACL using temporary signature url:");
		System.out.println("\t" + res2.getSignedUrl());
		getResponse(getBuilder(res2).get().build());
	}

	private static void doGetObject() throws ObsException, IOException {
		TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.GET, 300);
		req.setBucketName(bucketName);
		req.setObjectKey(objectKey);
		TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
		System.out.println("Getting object using temporary signature url:");
		System.out.println("\t" + res.getSignedUrl());
		getResponse(getBuilder(res).get().build());
	}

	private static void doCreateObject() throws ObsException, IOException {
		Map<String, String> headers = new HashMap<String, String>();
		String contentType = "text/plain";
		headers.put("Content-Type", contentType);

		TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.PUT, 300);
		req.setBucketName(bucketName);
		req.setObjectKey(objectKey);
		req.setHeaders(headers);

		TemporarySignatureResponse res = obsClient.createTemporarySignature(req);

		System.out.println("Createing object using temporary signature url:");
		System.out.println("\t" + res.getSignedUrl());
		Request.Builder builder = getBuilder(res);
		builder.put(RequestBody.create(MediaType.parse(contentType), "Hello OBS".getBytes("UTF-8")));
		getResponse(builder.build());
	}

	private static void doBucketCorsOperations() throws ObsException, IOException, ServiceException {
		BucketCors bucketCors = new BucketCors();
		BucketCorsRule rule = new BucketCorsRule();
		rule.getAllowedHeader().add("Authorization");
		rule.getAllowedOrigin().add("http://www.a.com");
		rule.getAllowedOrigin().add("http://www.b.com");
		rule.getExposeHeader().add("x-obs-test1");
		rule.getExposeHeader().add("x-obs-test2");
		rule.setMaxAgeSecond(100);
		rule.getAllowedMethod().add("HEAD");
		rule.getAllowedMethod().add("GET");
		rule.getAllowedMethod().add("PUT");
		bucketCors.getRules().add(rule);

		String requestXml = ObsConvertor.getInstance().transBucketCors(bucketCors);
		String requestXmlMd5 = ServiceUtils.computeMD5(requestXml);

		Map<String, String> headers = new HashMap<String, String>();

		headers.put("Content-Type", "application/xml");
		headers.put("Content-MD5", requestXmlMd5);

		TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.PUT, 300);
		req.setBucketName(bucketName);
		req.setHeaders(headers);
		req.setSpecialParam(SpecialParamEnum.CORS);
		TemporarySignatureResponse res = obsClient.createTemporarySignature(req);

		System.out.println("Setting bucket CORS using temporary signature url:");
		System.out.println("\t" + res.getSignedUrl());

		Request.Builder builder = getBuilder(res);
		builder.put(RequestBody.create(MediaType.parse("application/xml"), requestXml.getBytes("UTF-8")));
		getResponse(builder.build());

		TemporarySignatureRequest req2 = new TemporarySignatureRequest(HttpMethodEnum.GET, 300);
		req2.setBucketName(bucketName);
		req2.setSpecialParam(SpecialParamEnum.CORS);
		TemporarySignatureResponse res2 = obsClient.createTemporarySignature(req2);
		System.out.println("Getting bucket CORS using temporary signature url:");
		System.out.println("\t" + res2.getSignedUrl());
		getResponse(getBuilder(res2).get().build());
	}

	private static void doDeleteBucket() throws ObsException, IOException {
		TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.DELETE, 300);
		req.setBucketName(bucketName);
		TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
		System.out.println("Deleting bucket using temporary signature url:");
		System.out.println("\t" + res.getSignedUrl());
		getResponse(getBuilder(res).delete().build());
	}

	private static void doCreateBucket() throws ObsException, IOException {
		TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.PUT, 300);
		req.setBucketName(bucketName);
		TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
		System.out.println("Creating bucket using temporary signature url:");
		System.out.println("\t" + res.getSignedUrl());
		String location = "your-location";
		String content =  "<CreateBucketConfiguration><Location>" +location+ "</Location></CreateBucketConfiguration>";
		getResponse(getBuilder(res).put(RequestBody.create(null, content.getBytes("UTF-8"))).build());
	}

}
