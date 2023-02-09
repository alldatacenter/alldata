Version 3.22.12
New features:
1. Java SDK支持深度归档
2. Java SDK实现posix accesslable相关接口

Third-party dependence:
1. 使用 log4j2 2.18.0 替代 log4j2 2.17.1
2. 使用 okhttp 4.10.0 替代 okhttp 4.9.3
4. 使用 jackson-core 2.13.3 替代 jackson-core 2.13.0
5. 使用 jackson-databind 2.13.4.1 替代 jackson-databind 2.13.0
6. 使用 jackson-annotations 2.13.3 替代 jackson-annotations 2.13.0
-----------------------------------------------------------------------------------

Version 3.22.3
Third-party dependence:
1. 使用 log4j2 2.17.1 替代 log4j2 2.17.0
2. 使用 okhttp 4.9.3 替代 okhttp 4.9.1
3. 使用 okio 2.10.0 替代 okio 2.7.0
4. 使用 jackson-core 2.13.0 替代 jackson-core 2.12.5
5. 使用 jackson-databind 2.13.0 替代 jackson-databind 2.12.5
6. 使用 jackson-annotations 2.13.0 替代 jackson-annotations 2.12.5
-----------------------------------------------------------------------------------

Version 3.21.12
三方依赖:
1. 使用 log4j2 2.17.0 替代 log4j2 2.16.0
-----------------------------------------------------------------------------------

Version 3.21.11
新特性：
1. 用户可以在发送请求时添加任意自定义头域

三方依赖:   
1. 使用jackson-core 2.12.5 替代jackson-core 2.11.1
2. 使用 jackson-databind 2.12.5 替代jackson-databind 2.11.1
3. 使用 jackson-annotations 2.12.5 替代jackson-annotations 2.11.1
4. 使用 okhttp 4.9.1 替代okhttp 4.8.0
5. 使用 log4j2 2.16.0 替代 log4j2 2.14.1
-----------------------------------------------------------------------------------
Version 3.21.8

1. okhttp 由 3.14.9 升级至 4.8.0
2. Response 对象添加原始头域字段

-----------------------------------------------------------------------------------
Version 3.21.4

1. 修复了一个已知问题

-----------------------------------------------------------------------------------

Version 3.20.7
新特性：
1. 上传对象时，新增支持指定3个请求头：Content-Disposition，Cache-Control, Expires；

三方依赖:
1. 使用jackson-core 2.11.1 替代jackson-core 2.10.0
2. 使用 jackson-databind 2.11.1 替代jackson-databind 2.10.0
3. 使用 jackson-annotations 2.11.1 替代jackson-annotations 2.10.0
4. 使用java-xmlbuilder 1.3 替代java-xmlbuilder 1.2
5. 使用 okhttp 3.14.9 替代okhttp 3.14.4
5. 使用 okio 1.17.5 替代okio 1.17.2

-----------------------------------------------------------------------------------

Version 3.20.6
修复问题：
1. 修复日志级别无法动态刷新的问题；
2. 优化下载gzip文件时，默认不自动解压；

三方依赖:
1. 使用 okhttp 3.14.4 替代okhttp 3.14.2
2. 使用 log4j-core 2.13.2 替代 log4j-core 2.12.0
3. 使用 log4j-api 2.13.2 替代  log4j-api 2.12.0

-----------------------------------------------------------------------------------

Version 3.20.3
新特性：
1. 支持请求者付费
2. 去掉默认的log4j配置

-----------------------------------------------------------------------------------

Version 3.20.1
三方依赖:
1. 使用 okhttp 3.14.2 替代okhttp 3.11.0

-----------------------------------------------------------------------------------

Version 3.19.11
新特性：
1. 请求鉴权支持链式访问模式
2. 支持下载请求，返回302请求的情况下，重定向的时候不带鉴权信息

三方依赖:
1. 使用 log4j-core 2.12.0 替代 log4j-core 2.8.2
2. 使用 log4j-api 2.12.0 替代  log4j-api 2.8.2
3. 使用 java-xmlbuilder 1.2 替代 java-xmlbuilder 1.1

-----------------------------------------------------------------------------------

Version 3.19.9

三方依赖:
1. 使用 jackson-core 2.9.10 替代 jackson-core 2.9.9
2. 使用 jackson-databind 2.9.10 替代 jackson-databind 2.9.9
3. 使用 jackson-annotations 2.9.10 替代 jackson-core 2.9.9

-----------------------------------------------------------------------------------

Version 3.19.7.1

新特性：
1. 添加IObsCredentialsProvider接口，提供获取AK/SK方法和主动刷新AK/SK的方法；
2. 添加IObsCredentialsProvider接口的三种实现方式：
   a. 用户传入AK/SK：BasicObsCredentialsProvider；
   b. 从环境变量读取AK/SK：EnvironmentVariableObsCredentialsProvider；
   c. 从ECS服务获取AK/SK：EcsObsCredentialsProvider。
3. 支持svp格式文件的content-type设置，设置值为image/svg+xml。

-----------------------------------------------------------------------------------
Version 3.19.5.x

三方依赖:
1. 使用okhttp 3.11.0替代okhttp 3.10.0
2. 使用 jackson-core 2.9.9 替代 jackson-core 2.9.8
3. 使用 jackson-databind 2.9.9 替代 jackson-databind 2.9.8
4. 使用 jackson-annotations 2.9.9 替代 jackson-core 2.9.8

-----------------------------------------------------------------------------------

Version 3.19.5

新特性：
1. sse-kms加密方式新增支持project id；
2. 新增ObsException.getErrorIndicator获取异常时OBS服务端返回的详细错误标识；

修复问题：
1. 【功能】修复上传下载进度条在未设置content-length时无法显示的问题；
2. 【功能】修正以.mp4后缀上传的文件自动设置的content-type为video/mp4；
3. 【功能】修复在数据回源情况下的对象下载失败的问题；

-----------------------------------------------------------------------------------

Version 3.1.3
新特性：
1. 新增桶加密接口（ObsClient.setBucketEncryption/ObsClient.getBucketEncryption/ObsClient.deleteBucketEncryption），目前仅支持SSE-KMS的服务端加密方式；
2. 新增服务端加密方式枚举类型（SSEAlgorithmEnum），将服务端加密相关模型 ServerAlgorithm，ServerEncryption 标记为 Deprecated；

资料&demo:
1. 开发指南服务端加密章节，修改加密示例代码；

修复问题：
1. 优化异常情况下的日志记录；
2. 修复上传对象时，传入ByteArrayInputStream数据流可能导致报错的问题；
3. 优化access日志的级别，避免产生歧义；
4. 修改断点续传上传接口对段大小限制，从最小5MB改为最小100KB；

-----------------------------------------------------------------------------------

Version 3.1.2.1

修复问题：
1. 修改ObsConfiguration中maxIdleConnections参数的默认值为1000；

-----------------------------------------------------------------------------------

Version 3.1.2

新特性：
1. 桶事件通知接口（ObsClient.setBucketNotification/ObsClient.getBucketNotification）新增对函数工作流服务配置和查询的支持；

资料&demo:
1. 开发指南事件通知章节，新增对函数工作流服务配置的介绍；

修复问题：
1. 修复创建桶接口（ObsClient.createBucket）由于协议协商导致报错信息不准确的问题；
2. 修复okhttp3.Dispatcher底层的BUG，该BUG会导致最大并发数超限；

-----------------------------------------------------------------------------------

Version 3.1.1

新特性：
1. 支持集成 log4j 1.x作为日志组件;
2. 新增支持以Policy设置权限的临时鉴权访问接口（ObsClient.createGetTemporarySignature）；
3. 上传对象（ObsClient.putObject）支持自动识别更广泛的MIME类型；

修复问题：
1. 修复设置桶事件通知接口（ObsClient.setBucketNotification）无法设置多个TopicConfiguration的问题；
2. 修复SDK对JDK 9 及以上版本不兼容的问题；





