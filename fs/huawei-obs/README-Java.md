Version 3.22.12
New features:
1. Added the Deep Archive storage class in the Java SDK.
2. Implemented the APIs related to posix accesslable int the Java SDK.

Third-party dependence:
1. Replace log4j2 2.17.1 with log4j2 2.18.0
2. Replace okhttp 4.9.3 with okhttp 4.10.0
4. Replace jackson-core 2.13.0 with jackson-core 2.13.3
5. Replace jackson-databind 2.13.0 with jackson-databind 2.13.4.1
6. Replace jackson-annotations 2.13.0 with jackson-annotations 2.13.3
-----------------------------------------------------------------------------------

Version 3.22.3
Third-party dependence:
1. Replace log4j2 2.17.0 with log4j2 2.17.1
2. Replace okhttp 4.9.1 with okhttp 4.9.3
3. Replace okio 2.7.0 with okio 2.10.0
4. Replace jackson-core 2.12.5 with jackson-core 2.13.0
5. Replace jackson-databind 2.12.5 with jackson-databind 2.13.0
6. Replace jackson-annotations 2.12.5 with jackson-annotations 2.13.0
-----------------------------------------------------------------------------------

Version 3.21.12
Third-party dependence:
1. Replace log4j2 2.16.0 with log4j2 2.17.0
-----------------------------------------------------------------------------------

Version 3.21.11
New features:
1. Allowed you to add any custom header field in a request.
Third-party dependence:
1. Replace jackson-core 2.11.1 with jackson-core 2.12.5
2. Replace jackson-databind 2.11.1 with jackson-databind 2.12.5
3. Replace jackson-annotations 2.11.1 with jackson-annotations 2.12.5
4. Replace okhttp 4.8.0 with okhttp 4.9.1
5. Replace log4j2 2.14.1 with log4j2 2.16.0
-----------------------------------------------------------------------------------
Version 3.21.8

1. Replace okhttp 3.14.9 with okhttp 4.8.0.
2. Adding original headers to Response object

-----------------------------------------------------------------------------------

Version 3.21.4

1. Fixed a known issue

-----------------------------------------------------------------------------------

Version 3.20.6
Fixed issues:
1. Fixed the issue that logs cannot be dynamically updated.
2. Fixed the issue that automatic depression is not executed when gzip files are being downloaded. 

Third-party dependencies:
1. Replaced okhttp 3.14.2 with okhttp 3.14.4.
2. Replaced log4j-core 2.12.0 with log4j-core 2.13.2.
3. Replaced log4j-api 2.12.0 with log4j-api 2.13.2.

-----------------------------------------------------------------------------------

Version 3.20.3
New Features:
1. Supports Requester Pays.
2. Deleted the default log4j configuration.

-----------------------------------------------------------------------------------

Version 3.20.1
Third-party dependence:
1. Replace okhttp 3.11.0 with okhttp 3.14.2.

-----------------------------------------------------------------------------------

Version 3.19.11
New features:
1. Request authentication supports obtaining access keys by searching in sequence.
2. Download requests are supported. If status code 302 Found is returned, authentication information is not required for redirection.

Third-party dependence:
1. Replace log4j-core 2.8.2 with log4j-core 2.12.0.
2. Replace jackson-databind 2.8.2 with log4j-api 2.12.0.
3. Replace java-xmlbuilder 1.1 with java-xmlbuilder 1.2.

-----------------------------------------------------------------------------------

Version 3.19.9

Third-party dependencies:
1. replace jackson-core 2.9.9 with jackson-core 2.9.10
2. replace jackson-databind 2.9.9 with jackson-databind 2.9.10
3. replace jackson-annotations 2.9.9 with jackson-core 2.9.10

-----------------------------------------------------------------------------------

Version 3.19.7.1

New features:
1. Added the IObsCredentialsProvider interface to provide methods for obtaining AK/SK methods and actively refreshing AK/SK.
2. Added three implementations of the IObsCredentialsProvider interface:
   a. User provide AK/SK: BasicObsCredentialsProvider;
   b. Get AK/SK from the environment variable: EnvironmentVariableObsCredentialsProvider;
   c. Get AK/SK from the ECS service: EcsObsCredentialsProvider.
3. Support the content-type setting of the svp format file, and set the value to image/svg+xml.

-----------------------------------------------------------------------------------

Version 3.19.5.x

Third-party dependencies:
1. replace okhttp 3.10.0 with okhttp 3.11.0
2. replace jackson-core 2.9.8 with jackson-core 2.9.9
3. replace jackson-databind 2.9.8 with jackson-databind 2.9.9
4. replace jackson-annotations 2.9.8 with jackson-core 2.9.9

-----------------------------------------------------------------------------------

Version 3.19.5

New features:
1. Added the project ID parameter to SSE-KMS.
2. Added the detailed error flag returned by the OBS server when an exception occurs on ObsException.getErrorIndicator.

Resolved issues:
1. [Function] Fixed the issue that upload and download progress bar cannot be displayed when content-length is not set.
2. [Function] Fixed the issue that content-type is automatically set to video/mp4 for uploaded files with file name extension of .mp4.
3. [Function] Fixed the issue of object download failure in the data retrieval scenario.

-----------------------------------------------------------------------------------

Version 3.1.3
New features:
1. Added bucket encryption APIs: ObsClient.setBucketEncryption, ObsClient.getBucketEncryption, and ObsClient.deleteBucketEncryption. Currently, only the SSE-KMS encryption is supported.
2. Added the enumerated type (SSEAlgorithmEnum) for server-side encryption. The server-side encryption model ServerAlgorithm and ServerEncryption are marked as Deprecated.

Documentation & Demo
1. Modified the encryption sample code in the section describing server-side encryption in the Developer Guide.

Resolved issues:
1. Optimized the logging when exceptions occur.
2. Fixed the issue that errors may occur when the ByteArrayInputStream data flow is transferred during object upload.
3. Optimized the level of access logs to avoid ambiguity.
4. Changed the lower limit of resumable upload API on a part size from 5 MB to 100KB.

-----------------------------------------------------------------------------------

Version 3.1.2.1

Resolved issues:
1. Modified the default value of maxIdleConnections in ObsConfiguration to 1000.

-----------------------------------------------------------------------------------

Version 3.1.2

New features:
1. FunctionGraph configuration and query are supported in the bucket event notification APIs: ObsClient.setBucketNotification and ObsClient.getBucketNotification.

Documentation & Demo
1. Added the description of FunctionGraph configuration in the section about event notification in the Developer Guide.

Resolved issues:
1. Fixed the issue that the error information reported by the bucket creation API ObsClient.createBucket is incorrect due to protocol negotiation.
2. Fix the bugs at the bottom layer of okhttp3.Dispatcher. This bug causes the issue that the maximum number of concurrent requests exceeds the upper limit.

-----------------------------------------------------------------------------------

Version 3.1.1

New features:
1. Integrated log4j 1.x as the log component.
2. Added the temporary authentication access API (ObsClient.createGetTemporarySignature) that supports policy setting.
3. The API for object upload (ObsClient.putObject) can automatically identify a wider MIME type.

Resolved issues:
1. Fixed the issue that multiple TopicConfigurations cannot be set for the bucket event notification API (ObsClient.setBucketNotification).
2. Fixed the issue that the SDK is incompatible with JDK 9 or later versions.





