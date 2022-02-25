<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Ambari Server Configuration
---
- [Introduction](#introduction)
- [Configuration Properties](#configuration-properties)
- [Baseline Values](#baseline-values)
- [Database And Persistence](#db-persistence)

<a name="introduction"></a>
## Introduction
Ambari Server is configured using a flat text properties file. The file is read once when starting Ambari. Any changes made to the properties file are only reflected after restarting Ambari. 

```
/etc/ambari-server/conf/ambari.properties
```

<a name="configuration-properties"></a>
## Configuration Properties
The following are the properties which can be used to configure Ambari. 

| Property Name | Description | Default |
| --- | --- | --- |
| active.instance | Indicates whether the current ambari server instance is active or not. |`true` | 
| addservice.hostgroup.strategy | Fully qualified class name of the strategy used to form host groups for add service request layout recommendation. |`org.apache.ambari.server.topology.addservice.GroupByComponentsStrategy` | 
| agent.api.acceptor.count | Count of acceptors to configure for the jetty connector used for Ambari agent. | | 
| agent.api.gzip.compression.enabled | Determiens whether communication with the Ambari Agents should have the JSON payloads compressed with GZIP. |`true` | 
| agent.auto.cache.update | Determines whether the agents will automatically attempt to download updates to stack resources from the Ambari Server. |`true` | 
| agent.check.mounts.timeout | The timeout, used by the `timeout` command in linux, when checking mounts for free capacity. |`0` | 
| agent.check.remote.mounts | Determines whether the Ambari Agents will use the `df` or `df -l` command when checking disk mounts for capacity issues. Auto-mounted remote directories can cause long delays. |`false` | 
| agent.package.install.task.timeout | The time, in seconds, before package installation commands are killed. |`1800` | 
| agent.package.parallel.commands.limit | The maximum number of tasks which can run within a single operational request. If there are more tasks, then they will be broken up between multiple operations. |`100` | 
| agent.service.check.task.timeout | The time, in seconds, before agent service check commands are killed. |`0` | 
| agent.ssl | Determines whether SSL is used to communicate between Ambari Server and Ambari Agents. |`true` | 
| agent.stack.retry.on_repo_unavailability | Determines whether agents should retrying installation commands when the repository is not available. This can prevent false installation errors with repositories that are sporadically inaccessible. |`false` | 
| agent.stack.retry.tries | The number of times an Ambari Agent should retry package installation when it fails due to a repository error. <br/><br/> This property is related to `agent.stack.retry.on_repo_unavailability`. |`5` | 
| agent.task.timeout | The time, in seconds, before agent commands are killed. This does not include package installation commands. |`900` | 
| agent.threadpool.size.max | The size of the Jetty connection pool used for handling incoming Ambari Agent requests. |`25` | 
| agents.registration.queue.size | Queue size for agents in registration. |`200` | 
| agents.reports.processing.period | Period in seconds with agents reports will be processed. |`1` | 
| agents.reports.processing.start.timeout | Timeout in seconds before start processing of agents' reports. |`5` | 
| agents.reports.thread.pool.size | Thread pool size for agents reports processing. |`10` | 
| alerts.ambari.snmp.dispatcher.udp.port | The UDP port to use when binding the Ambari SNMP dispatcher on Ambari Server startup. If no port is specified, then a random port will be used. | | 
| alerts.cache.enabled | Determines whether current alerts should be cached. Enabling this can increase performance on large cluster, but can also result in lost alert data if the cache is not flushed frequently. |`false` | 
| alerts.cache.flush.interval | The time, in minutes, after which cached alert information is flushed to the database<br/><br/> This property is related to `alerts.cache.enabled`. |`10` | 
| alerts.cache.size | The size of the alert cache.<br/><br/> This property is related to `alerts.cache.enabled`. |`50000` | 
| alerts.execution.scheduler.threadpool.size.core | The core number of threads used to process incoming alert events. The value should be increased as the size of the cluster increases. |`2` | 
| alerts.execution.scheduler.threadpool.size.max | The number of threads used to handle alerts received from the Ambari Agents. The value should be increased as the size of the cluster increases. |`2` | 
| alerts.execution.scheduler.threadpool.worker.size | The number of queued alerts allowed before discarding old alerts which have not been handled. The value should be increased as the size of the cluster increases. |`2000` | 
| alerts.server.side.scheduler.threadpool.size.core | The core pool size of the executor service that runs server side alerts. |`4` | 
| alerts.snmp.dispatcher.udp.port | The UDP port to use when binding the SNMP dispatcher on Ambari Server startup. If no port is specified, then a random port will be used. | | 
| alerts.template.file | The full path to the XML file that describes the different alert templates. | | 
| ambari.display.url | The URL to use when creating messages which should include the Ambari Server URL.<br/><br/>The following are examples of valid values:<ul><li>`http://ambari.apache.org:8080`</ul> | | 
| ambari.post.user.creation.hook | The location of the post user creation hook on the ambari server hosting machine. |`/var/lib/ambari-server/resources/scripts/post-user-creation-hook.sh` | 
| ambari.post.user.creation.hook.enabled | Indicates whether the post user creation is enabled or not. By default is false. |`false` | 
| ambari.python.wrap | The name of the shell script used to wrap all invocations of Python by Ambari.  |`ambari-python-wrap` | 
| ambariserver.metrics.disable | Global disable flag for AmbariServer Metrics. |`false` | 
| anonymous.audit.name | The name of the user given to requests which are executed without any credentials. |`_anonymous` | 
| api.authenticated.user | The username of the default user assumed to be executing API calls. When set, authentication is not required in order to login to Ambari or use the REST APIs.   | | 
| api.csrfPrevention.enabled | Determines whether Cross-Site Request Forgery attacks are prevented by looking for the `X-Requested-By` header. |`true` | 
| api.gzip.compression.enabled | Determines whether data sent to and from the Ambari service should be compressed. |`true` | 
| api.gzip.compression.min.size | Used in conjunction with `api.gzip.compression.enabled`, determines the mininum size that an HTTP request must be before it should be compressed. This is measured in bytes. |`10240` | 
| api.heartbeat.interval | Server to API STOMP endpoint heartbeat interval in milliseconds. |`10000` | 
| api.ssl | Determines whether SSL is used in for secure connections to Ambari. When enabled, ambari-server setup-https must be run in order to properly configure keystores. |`false` | 
| auditlog.enabled | Determines whether audit logging is enabled. |`true` | 
| auditlog.logger.capacity | The size of the worker queue for audit logger events.<br/><br/> This property is related to `auditlog.enabled`. |`10000` | 
| authentication.kerberos.auth_to_local.rules | The auth-to-local rules set to use when translating a user's principal name to a local user name during authentication via SPNEGO. |`DEFAULT` | 
| authentication.kerberos.enabled | Determines whether to use Kerberos (SPNEGO) authentication when connecting Ambari. |`false` | 
| authentication.kerberos.spnego.keytab.file | The Kerberos keytab file to use when verifying user-supplied Kerberos tokens for authentication via SPNEGO |`/etc/security/keytabs/spnego.service.keytab` | 
| authentication.kerberos.spnego.principal | The Kerberos principal name to use when verifying user-supplied Kerberos tokens for authentication via SPNEGO |`HTTP/_HOST` | 
| authentication.local.max.failures | The maximum number of authentication attempts permitted to a local user. Once the number of failures reaches this limit the user will be locked out. 0 indicates unlimited failures. |`0` | 
| authentication.local.show.locked.account.messages | Show or hide whether the user account is disabled or locked out, if relevant, when an authentication attempt fails. |`false` | 
| auto.group.creation | The auto group creation by Ambari |`false` | 
| bootstrap.dir | The directory on the Ambari Server file system used for storing Ambari Agent bootstrap information such as request responses. |`/var/run/ambari-server/bootstrap` | 
| bootstrap.master_host_name | The host name of the Ambari Server which will be used by the Ambari Agents for communication. | | 
| bootstrap.script | The location and name of the Python script used to bootstrap new Ambari Agent hosts. |`/usr/lib/ambari-server/lib/ambari_server/bootstrap.py` | 
| bootstrap.setup_agent.password | The password to set on the `AMBARI_PASSPHRASE` environment variable before invoking the bootstrap script. |`password` | 
| bootstrap.setup_agent.script | The location and name of the Python script executed on the Ambari Agent host during the bootstrap process. |`/usr/lib/ambari-server/lib/ambari_server/setupAgent.py` | 
| client.api.acceptor.count | Count of acceptors to configure for the jetty connector used for Ambari API. | | 
| client.api.port | The port that client connections will use with the REST API. The Ambari Web client runs on this port. |`8080` | 
| client.api.ssl.cert_pass_file | The filename which contains the password for the keystores, truststores, and certificates for the REST API when it's protected by SSL. |`https.pass.txt` | 
| client.api.ssl.crt_pass | The password for the keystores, truststores, and certificates for the REST API when it's protected by SSL. If not specified, then `client.api.ssl.cert_pass_file` should be used. | | 
| client.api.ssl.keys_dir | The location on the Ambari server where the REST API keystore and password files are stored if using SSL. | | 
| client.api.ssl.keystore_name | The name of the keystore used when the Ambari Server REST API is protected by SSL. |`https.keystore.p12` | 
| client.api.ssl.keystore_type | The type of the keystore file specified in `client.api.ssl.keystore_name`. Self-signed certificates can be `PKCS12` while CA signed certificates are `JKS` |`PKCS12` | 
| client.api.ssl.port | The port that client connections will use with the REST API when using SSL. The Ambari Web client runs on this port if SSL is enabled. |`8443` | 
| client.api.ssl.truststore_name | The name of the truststore used when the Ambari Server REST API is protected by SSL. |`https.keystore.p12` | 
| client.api.ssl.truststore_type | The type of the keystore file specified in `client.api.ssl.truststore_name`. Self-signed certificates can be `PKCS12` while CA signed certificates are `JKS` |`PKCS12` | 
| client.security | The type of authentication mechanism used by Ambari.<br/><br/>The following are examples of valid values:<ul><li>`local`<li>`ldap`<li>`pam`</ul> | | 
| client.threadpool.size.max | The size of the Jetty connection pool used for handling incoming REST API requests. This should be large enough to handle requests from both web browsers and embedded Views. |`25` | 
| common.services.path | The location on the Ambari Server where common service resources exist. Stack services share the common service files.<br/><br/>The following are examples of valid values:<ul><li>`/var/lib/ambari-server/resources/common-services`</ul> | | 
| custom.action.definitions | The location on the Ambari Server where custom actions are defined. |`/var/lib/ambari-server/resources/custom_action_definitions` | 
| db.mysql.jdbc.name | The name of the MySQL JDBC JAR connector. |`mysql-connector-java.jar` | 
| db.oracle.jdbc.name | The name of the Oracle JDBC JAR connector. |`ojdbc6.jar` | 
| default.kdcserver.port | The port used to communicate with the Kerberos Key Distribution Center. |`88` | 
| execution.command.retry.count | The number of attempts to emit execution command message to agent. Default is 4 |`4` | 
| execution.command.retry.interval | The interval in seconds between attempts to emit execution command message to agent. Default is 15 |`15` | 
| extensions.path | The location on the Ambari Server where stack extensions exist.<br/><br/>The following are examples of valid values:<ul><li>`/var/lib/ambari-server/resources/extensions`</ul> | | 
| gpl.license.accepted | Whether user accepted GPL license. |`false` | 
| gzip.handler.jetty.enabled | Determines whether jetty Gzip compression is enabled or not. |`true` | 
| http.cache-control | The value that will be used to set the `Cache-Control` HTTP response header. |`no-store` | 
| http.charset | The value that will be used to set the Character encoding to HTTP response header. |`utf-8` | 
| http.pragma | The value that will be used to set the `PRAGMA` HTTP response header. |`no-cache` | 
| http.strict-transport-security | When using SSL, this will be used to set the `Strict-Transport-Security` response header. |`max-age=31536000` | 
| http.x-content-type-options | The value that will be used to set the `X-CONTENT-TYPE` HTTP response header. |`nosniff` | 
| http.x-frame-options | The value that will be used to set the `X-Frame-Options` HTTP response header. |`DENY` | 
| http.x-xss-protection | The value that will be used to set the `X-XSS-Protection` HTTP response header. |`1; mode=block` | 
| java.home | The location of the JDK on the Ambari Agent hosts. If stack.java.home exists, that is only used by Ambari Server (or you can find that as ambari_java_home in the commandParams on the agent side)<br/><br/>The following are examples of valid values:<ul><li>`/usr/jdk64/jdk1.8.0_112`</ul> | | 
| jce.name | The name of the JCE policy ZIP file. If stack.jce.name exists, that is only used by Ambari Server (or you can find that as ambari_jce_name in the commandParams on the agent side)<br/><br/>The following are examples of valid values:<ul><li>`UnlimitedJCEPolicyJDK8.zip`</ul> | | 
| jdk.name | The name of the JDK installation binary. If stack.jdk.name exists, that is only used by Ambari Server (or you can find that as ambari_jdk_name in the commandParams on the agent side)<br/><br/>The following are examples of valid values:<ul><li>`jdk-8u112-linux-x64.tar.gz`</ul> | | 
| kdcserver.connection.check.timeout | The timeout, in milliseconds, to wait when communicating with a Kerberos Key Distribution Center. |`10000` | 
| kerberos.check.jaas.configuration | Determines whether Kerberos-enabled Ambari deployments should use JAAS to validate login credentials. |`false` | 
| kerberos.keytab.cache.dir | The location on the Ambari Server where Kerberos keytabs are cached. |`/var/lib/ambari-server/data/cache` | 
| kerberos.operation.retries | The number of times failed Kerberos operations should be retried to execute. |`3` | 
| kerberos.operation.retry.timeout | The time to wait (in seconds) between failed Kerberos operations retries. |`10` | 
| kerberos.operation.verify.kdc.trust | Validate the trust of the SSL certificate provided by the KDC when performing Kerberos operations over SSL. |`true` | 
| ldap.sync.username.collision.behavior | Determines how to handle username collision while updating from LDAP.<br/><br/>The following are examples of valid values:<ul><li>`skip`<li>`convert`<li>`add`</ul> |`add` | 
| log4j.monitor.delay | Indicates the delay, in milliseconds, for the log4j monitor to check for changes |`300000` | 
| logsearch.metadata.cache.expire.timeout | The time, in hours, that the Ambari Server will hold Log File metadata in its internal cache before making a request to the LogSearch Portal to get the latest metadata. |`24` | 
| logsearch.portal.connect.timeout | The time, in milliseconds, that the Ambari Server will wait while attempting to connect to the LogSearch Portal service. |`5000` | 
| logsearch.portal.external.address | Address of an external LogSearch Portal service. (managed outside of Ambari) Using Ambari Credential store is required for this feature (credential: 'logsearch.admin.credential') | | 
| logsearch.portal.read.timeout | The time, in milliseconds, that the Ambari Server will wait while attempting to read a response from the LogSearch Portal service. |`5000` | 
| messaging.threadpool.size | Thread pool size for spring messaging |`10` | 
| metadata.path | The location on the Ambari Server where the stack resources exist.<br/><br/>The following are examples of valid values:<ul><li>`/var/lib/ambari-server/resources/stacks`</ul> | | 
| metrics.retrieval-service.cache.timeout | The amount of time, in minutes, that JMX and REST metrics retrieved directly can remain in the cache. |`30` | 
| metrics.retrieval-service.request.ttl | The number of seconds to wait between issuing JMX or REST metric requests to the same endpoint. This property is used to throttle requests to the same URL being made too close together<br/><br/> This property is related to `metrics.retrieval-service.request.ttl.enabled`. |`5` | 
| metrics.retrieval-service.request.ttl.enabled | Enables throttling requests to the same endpoint within a fixed amount of time. This property will prevent Ambari from making new metric requests to update the cache for URLs which have been recently retrieved.<br/><br/> This property is related to `metrics.retrieval-service.request.ttl`. |`true` | 
| mpacks-v2.staging.path | The Ambari Management Pack version-2 staging directory on the Ambari Server.<br/><br/>The following are examples of valid values:<ul><li>`/var/lib/ambari-server/resources/mpacks-v2`</ul> | | 
| mpacks.staging.path | The Ambari Management Pack staging directory on the Ambari Server.<br/><br/>The following are examples of valid values:<ul><li>`/var/lib/ambari-server/resources/mpacks`</ul> | | 
| notification.dispatch.alert.script.directory | The directory for scripts which are used by the alert notification dispatcher. |`/var/lib/ambari-server/resources/scripts` | 
| packages.pre.installed | Determines whether Ambari Agent instances have already have the necessary stack software installed |`false` | 
| pam.configuration | The PAM configuration file. | | 
| property.mask.file | The path of the file which lists the properties that should be masked from the api that returns ambari.properties | | 
| proxy.allowed.hostports | A comma-separated whitelist of host and port values which Ambari Server can use to determine if a proxy value is valid. |`*:*` | 
| recommendations.artifacts.lifetime | The amount of time that Recommendation API data is kept on the Ambari Server file system. This is specified using a `hdwmy` syntax for pairing the value with a time unit (hours, days, weeks, months, years)<br/><br/>The following are examples of valid values:<ul><li>`8h`<li>`2w`<li>`1m`</ul> |`1w` | 
| recommendations.artifacts.rollover.max | Maximum number of recommendations artifacts at a given time<br/><br/>The following are examples of valid values:<ul><li>`50`<li>`10`<li>`100`</ul> |`100` | 
| recommendations.dir | The directory on the Ambari Server file system used for storing Recommendation API artifacts. |`/var/run/ambari-server/stack-recommendations` | 
| recovery.disabled_components | A comma-separated list of component names which are not included in automatic recovery attempts.<br/><br/>The following are examples of valid values:<ul><li>`NAMENODE,ZOOKEEPER_SERVER`</ul> | | 
| recovery.enabled_components | A comma-separated list of component names which are included in automatic recovery attempts.<br/><br/>The following are examples of valid values:<ul><li>`NAMENODE,ZOOKEEPER_SERVER`</ul> | | 
| recovery.lifetime_max_count | The maximum number of recovery attempts of a failed component during the lifetime of an Ambari Agent instance. This is reset when the Ambari Agent is restarted. | | 
| recovery.max_count | The maximum number of recovery attempts of a failed component during a specified recovery window. | | 
| recovery.retry_interval | The delay, in minutes, between automatic retry windows. | | 
| recovery.type | The type of automatic recovery of failed services and components to use.<br/><br/>The following are examples of valid values:<ul><li>`DEFAULT`<li>`AUTO_START`<li>`FULL`</ul> | | 
| recovery.window_in_minutes | The length of a recovery window, in minutes, in which recovery attempts can be retried.<br/><br/> This property is related to `recovery.max_count`. | | 
| registration.threadpool.size | Thread pool size for agents registration |`10` | 
| repo.validation.suffixes.default | The suffixes to use when validating most types of repositories. |`/repodata/repomd.xml` | 
| repo.validation.suffixes.ubuntu | The suffixes to use when validating Ubuntu repositories. |`/dists/%s/Release` | 
| repositories.legacy-override.enabled | This property is used in specific testing circumstances only. Its use otherwise will lead to very unpredictable results with repository management and package installation |`false` | 
| resources.dir | The location on the Ambari Server where all resources exist, including common services, stacks, and scripts. |`/var/lib/ambari-server/resources/` | 
| rolling.upgrade.skip.packages.prefixes | A comma-separated list of packages which will be skipped during a stack upgrade. | | 
| security.agent.hostname.validate | Determines whether the Ambari Agent host names should be validated against a regular expression to ensure that they are well-formed.<br><br>WARNING: By setting this value to false, host names will not be validated, allowing a possible security vulnerability as described in CVE-2014-3582. See https://cwiki.apache.org/confluence/display/AMBARI/Ambari+Vulnerabilities for more information. |`true` | 
| security.master.key.location | The location on the Ambari Server of the master key file. This is the key to the master keystore. | | 
| security.master.keystore.location | The location on the Ambari Server of the master keystore file. | | 
| security.passwords.encryption.enabled | Whether security password encryption is enabled or not. In case it is we store passwords in their own file(s); otherwise we store passwords in the Ambari credential store. |`false` | 
| security.server.cert_chain_name | The name of the file located in the `security.server.keys_dir` directory containing the CA certificate chain used to verify certificates during 2-way SSL communications. |`ca_chain.pem` | 
| security.server.cert_name | The name of the file located in the `security.server.keys_dir` directory where certificates will be generated when Ambari uses the `openssl ca` command. |`ca.crt` | 
| security.server.crt_pass | The password for the keystores, truststores, and certificates. If not specified, then `security.server.crt_pass_file` should be used | | 
| security.server.crt_pass.len | The length of the randomly generated password for keystores and truststores.  |`50` | 
| security.server.crt_pass_file | The filename which contains the password for the keystores, truststores, and certificates. |`pass.txt` | 
| security.server.csr_name | The name of the certificate request file used when generating certificates. |`ca.csr` | 
| security.server.disabled.ciphers | A list of cipher suites which are not strong enough to use and will be excluded when creating SSL connections.<br/><br/>The following are examples of valid values:<ul><li>`SSL_RSA_WITH_RC4_128_MD5\|SSL_RSA_WITH_RC4_12‌​8_MD5`</ul> | | 
| security.server.disabled.protocols | The list of protocols which should not be used when creating SSL connections.<br/><br/>The following are examples of valid values:<ul><li>`TLSv1.1\|TLSv1.2`</ul> | | 
| security.server.encrypt_sensitive_data | Whether to encrypt sensitive data (at rest) on service level configuration. |`false` | 
| security.server.key_name | The name of the private key used to sign requests. |`ca.key` | 
| security.server.keys_dir | The directory on the Ambari Server where keystores are kept. |`.` | 
| security.server.keystore_name | The name of the keystore file, located in `security.server.keys_dir` |`keystore.p12` | 
| security.server.keystore_type | The type of the keystore file specified in `security.server.key_name`. Self-signed certificates can be `PKCS12` while CA signed certificates are `JKS` |`PKCS12` | 
| security.server.one_way_ssl.port | The port that the Ambari Agents will use to communicate with the Ambari Server over SSL. |`8440` | 
| security.server.passphrase | The password to the Ambari Server to supply to new Ambari Agent hosts being bootstrapped. |`AMBARI_PASSPHRASE` | 
| security.server.passphrase_env_var | An environment variable which can be used to supply the Ambari Server password when bootstrapping new Ambari Agents. |`AMBARI_PASSPHRASE` | 
| security.server.tls.ephemeral_dh_key_size | The Ephemeral TLS Diffie-Hellman (DH) key size. Supported from Java 8. |`2048` | 
| security.server.truststore_name | The name of the truststore file ambari uses to store trusted certificates. Located in `security.server.keys_dir` |`keystore.p12` | 
| security.server.truststore_type | The type of the truststore file specified in `security.server.truststore_name`. Self-signed certificates can be `PKCS12` while CA signed certificates are `JKS` |`PKCS12` | 
| security.server.two_way_ssl | Determines whether two-way SSL should be used between Ambari Server and Ambari Agents so that the agents must also use SSL. |`false` | 
| security.server.two_way_ssl.port | The port that the Ambari Server will use to communicate with the agents over SSL. |`8441` | 
| security.temporary.keystore.actibely.purge | Determines whether the temporary keystore should have keys actively purged on a fixed internal. or only when requested after expiration. |`true` | 
| security.temporary.keystore.retention.minutes | The time, in minutes, that the temporary, in-memory credential store retains values. |`90` | 
| server.cache.isStale.enabled | Determines when the stale configuration cache is enabled. If disabled, then queries to determine if components need to be restarted will query the database directly. |`true` | 
| server.cache.isStale.expiration | The expiration time, in {@link TimeUnit#MINUTES}, that stale configuration information is cached.<br/><br/> This property is related to `server.cache.isStale.enabled`. |`600` | 
| server.connection.max.idle.millis | The time, in milliseconds, that Ambari Agent connections can remain open and idle. |`900000` | 
| server.ecCacheSize | The size of the cache which is used to hold current operations in memory until they complete. |`10000` | 
| server.execution.scheduler.isClustered | Determines whether Quartz will use a clustered job scheduled when performing scheduled actions like rolling restarts. |`false` | 
| server.execution.scheduler.maxDbConnections | The number of concurrent database connections that the Quartz job scheduler can use. |`5` | 
| server.execution.scheduler.maxStatementsPerConnection | The maximum number of prepared statements cached per database connection. |`120` | 
| server.execution.scheduler.maxThreads | The number of threads that the Quartz job scheduler will use when executing scheduled jobs. |`5` | 
| server.execution.scheduler.misfire.toleration.minutes | The time, in minutes, that a scheduled job can be run after its missed scheduled execution time. |`480` | 
| server.execution.scheduler.start.delay.seconds | The delay, in seconds, that a Quartz job must wait before it starts. |`120` | 
| server.execution.scheduler.wait | The time, in seconds, that the Quartz execution scheduler will wait before checking for new commands to schedule, such as rolling restarts. |`1` | 
| server.hosts.mapping | The location on the Ambari Server of the file which is used for mapping host names. | | 
| server.hrcStatusSummary.cache.enabled | Determines whether an existing request's status is cached. This is enabled by default to prevent increases in database access when there are long running operations in progress. |`true` | 
| server.hrcStatusSummary.cache.expiryDuration | The expiration time, in minutes, of the request status cache.<br/><br/> This property is related to `server.hrcStatusSummary.cache.enabled`. |`30` | 
| server.hrcStatusSummary.cache.size | The size of the cache which is used to hold a status of every operation in a request.<br/><br/> This property is related to `server.hrcStatusSummary.cache.enabled`. |`10000` | 
| server.http.request.header.size | The size of the buffer to use, in bytes, for REST API HTTP header requests. |`65536` | 
| server.http.response.header.size | The size of the buffer to use, in bytes, for REST API HTTP header responses. |`65536` | 
| server.http.session.inactive_timeout | The time, in seconds, that open HTTP sessions will remain valid while they are inactive. |`1800` | 
| server.jdbc.connection-pool | The connection pool manager to use for database connections. If using MySQL, then `c3p0` is automatically chosen.<br/><br/>The following are examples of valid values:<ul><li>`internal`<li>`c3p0`</ul> |`internal` | 
| server.jdbc.connection-pool.acquisition-retry-attempts | The number of times connections should be retried to be acquired from the database before giving up. Only used with c3p0.<br/><br/> This property is related to `server.jdbc.connection-pool`. |`30` | 
| server.jdbc.connection-pool.acquisition-retry-delay | The delay, in milliseconds, between connection acquisition attempts. Only used with c3p0.<br/><br/> This property is related to `server.jdbc.connection-pool`. |`1000` | 
| server.jdbc.connection-pool.acquisition-size | The number of connections that should be retrieved when the pool size must increase. This should be set higher than 1 since the assumption is that a pool that needs to grow should probably grow by more than 1. Only used with c3p0.<br/><br/> This property is related to `server.jdbc.connection-pool`. |`5` | 
| server.jdbc.connection-pool.idle-test-interval | The number of seconds in between testing each idle connection in the connection pool for validity. Only used with c3p0.<br/><br/> This property is related to `server.jdbc.connection-pool`. |`7200` | 
| server.jdbc.connection-pool.max-age |  The maximum amount of time, in seconds, any connection, whether its been idle or active, should remain in the pool. This will terminate the connection after the expiration age and force new connections to be opened. Only used with c3p0.<br/><br/> This property is related to `server.jdbc.connection-pool`. |`0` | 
| server.jdbc.connection-pool.max-idle-time | The maximum amount of time, in seconds, that an idle connection can remain in the pool. This should always be greater than the value returned from `server.jdbc.connection-pool.max-idle-time-excess`. Only used with c3p0.<br/><br/> This property is related to `server.jdbc.connection-pool`. |`14400` | 
| server.jdbc.connection-pool.max-idle-time-excess | The maximum amount of time, in seconds, that connections beyond the minimum pool size should remain in the pool. This should always be less than than the value returned from `server.jdbc.connection-pool.max-idle-time`. Only used with c3p0.<br/><br/> This property is related to `server.jdbc.connection-pool`. |`0` | 
| server.jdbc.connection-pool.max-size | The maximum number of connections that should exist in the database connection pool. Only used with c3p0.<br/><br/> This property is related to `server.jdbc.connection-pool`. |`32` | 
| server.jdbc.connection-pool.min-size | The minimum number of connections that should always exist in the database connection pool. Only used with c3p0.<br/><br/> This property is related to `server.jdbc.connection-pool`. |`5` | 
| server.jdbc.database_name | The name of the database. |`ambari` | 
| server.jdbc.driver | The name of the PostgresSQL JDBC JAR connector. |`org.postgresql.Driver` | 
| server.jdbc.generateTables | The table generation strategy to use when initializing JPA. |`NONE` | 
| server.jdbc.postgres.schema | The schema within a named PostgreSQL database where Ambari's tables, users, and constraints are stored.  | | 
| server.jdbc.rca.driver | The PostgresSQL driver name for the RCA database. |`org.postgresql.Driver` | 
| server.jdbc.rca.url | The full JDBC URL for connecting to the RCA database. |`jdbc:postgresql://{hostname}/ambarirca` | 
| server.jdbc.rca.user.name | The user name for connecting to the database which stores RCA information. |`mapred` | 
| server.jdbc.rca.user.passwd | The password for the user when connecting to the database which stores RCA information. |`mapred` | 
| server.jdbc.user.name | The user name used to login to the database. |`ambari` | 
| server.jdbc.user.passwd | The password for the user when logging into the database. |`bigdata` | 
| server.locks.profiling | Enable the profiling of internal locks. |`false` | 
| server.metrics.retrieval-service.thread.priority | The priority of threads used by the service which retrieves JMX and REST metrics directly from their respective endpoints. |`5` | 
| server.metrics.retrieval-service.threadpool.size.core | The core number of threads used to retrieve JMX and REST metrics directly from their respective endpoints. |`16` | 
| server.metrics.retrieval-service.threadpool.size.max | The maximum number of threads used to retrieve JMX and REST metrics directly from their respective endpoints. |`32` | 
| server.metrics.retrieval-service.threadpool.worker.size | The number of queued requests allowed for JMX and REST metrics before discarding old requests which have not been fullfilled. |`320` | 
| server.operations.retry-attempts | The number of retry attempts for failed API and blueprint operations. |`0` | 
| server.os_family | The operating system family for all hosts in the cluster. This is used when bootstrapping agents and when enabling Kerberos.<br/><br/>The following are examples of valid values:<ul><li>`redhat`<li>`ubuntu`</ul> | | 
| server.os_type | The operating system version for all hosts in the cluster. This is used when bootstrapping agents and when enabling Kerberos.<br/><br/>The following are examples of valid values:<ul><li>`6`<li>`7`</ul> | | 
| server.persistence.type | The type of database connection being used. Unless using an embedded PostgresSQL server, then this should be `remote`.<br/><br/>The following are examples of valid values:<ul><li>`local`<li>`remote`</ul> |`local` | 
| server.property-provider.threadpool.completion.timeout | The maximum time, in milliseconds, that federated requests for data can execute before being terminated. Increasing this value could result in degraded performanc from the REST APIs. |`5000` | 
| server.property-provider.threadpool.size.core | The core number of threads that will be used to retrieve data from federated datasources, such as remote JMX endpoints. |`16` | 
| server.property-provider.threadpool.size.max | The maximum number of threads that will be used to retrieve data from federated datasources, such as remote JMX endpoints. |`32` | 
| server.property-provider.threadpool.worker.size | The maximum size of pending federated datasource requests, such as those to JMX endpoints, which can be queued before rejecting new requests. |`2147483647` | 
| server.requestlogs.namepattern | The pattern of request log file name |`ambari-access-yyyy_mm_dd.log` | 
| server.requestlogs.path | The location on the Ambari Server where request logs can be created. | | 
| server.requestlogs.retaindays | The number of days that request log would be retained. |`15` | 
| server.script.threads | The number of threads that should be allocated to run external script. |`20` | 
| server.script.timeout | The time, in milliseconds, until an external script is killed. |`10000` | 
| server.stage.command.execution_type | How to execute commands in one stage |`STAGE` | 
| server.stages.parallel | Determines whether operations in different execution requests can be run concurrently. |`true` | 
| server.startup.web.timeout | The time, in seconds, that the ambari-server Python script will wait for Jetty to startup before returning an error code. |`50` | 
| server.task.timeout | The time, in seconds, before a server-side operation is terminated. |`1200` | 
| server.timeline.metrics.cache.catchup.interval | The time, in milliseconds, that Ambari Metrics intervals should use when extending the boundaries of the original request.<br/><br/> This property is related to `server.timeline.metrics.cache.disabled`. |`300000` | 
| server.timeline.metrics.cache.connect.timeout.millis | The time, in milliseconds, to wait while attempting to connect to Ambari Metrics.<br/><br/> This property is related to `server.timeline.metrics.cache.disabled`. |`5000` | 
| server.timeline.metrics.cache.disabled | Determines whether Ambari Metric data is cached. |`false` | 
| server.timeline.metrics.cache.entry.idle.seconds | The time, in seconds, that Ambari Metric data can remain in the cache without being accessed.<br/><br/> This property is related to `server.timeline.metrics.cache.disabled`. |`1800` | 
| server.timeline.metrics.cache.entry.ttl.seconds | The time, in seconds, that Ambari Metric timeline data is cached by Ambari Server.<br/><br/> This property is related to `server.timeline.metrics.cache.disabled`. |`3600` | 
| server.timeline.metrics.cache.heap.percent | The amount of heap on the Ambari Server dedicated to the caching values from Ambari Metrics. Measured as part of the total heap of Ambari Server.<br/><br/> This property is related to `server.timeline.metrics.cache.disabled`. |`15%` | 
| server.timeline.metrics.cache.interval.read.timeout.millis | The time, in milliseconds, that requests to update stale metric data will wait while reading from Ambari Metrics. This allows for greater control by allowing stale values to be returned instead of waiting for Ambari Metrics to always populate responses with the latest data.<br/><br/> This property is related to `server.timeline.metrics.cache.disabled`. |`10000` | 
| server.timeline.metrics.cache.read.timeout.millis | The time, in milliseconds, that initial requests to populate metric data will wait while reading from Ambari Metrics.<br/><br/> This property is related to `server.timeline.metrics.cache.disabled`. |`10000` | 
| server.timeline.metrics.cache.use.custom.sizing.engine | Determines if a custom engine should be used to increase performance of calculating the current size of the cache for Ambari Metric data.<br/><br/> This property is related to `server.timeline.metrics.cache.disabled`. |`true` | 
| server.timeline.metrics.https.enabled | Determines whether to use to SSL to connect to Ambari Metrics when retrieving metric data. |`false` | 
| server.tmp.dir | The location on the Ambari Server where temporary artifacts can be created. |`/var/lib/ambari-server/tmp` | 
| server.version.file | The full path to the file which contains the Ambari Server version. This is used to ensure that there is not a version mismatch between Ambari Agents and Ambari Server.<br/><br/>The following are examples of valid values:<ul><li>`/var/lib/ambari-server/resources/version`</ul> | | 
| server.version_definition.allow_from_filesystem | Controls whether VDF can be read from the filesystem. |`false` | 
| server.version_definition.connect.timeout.millis | The time, in milliseconds, that requests to connect to a URL to retrieve Version Definition Files (VDF) will wait before being terminated. |`5000` | 
| server.version_definition.read.timeout.millis | The time, in milliseconds, that requests to read from a connected URL to retrieve Version Definition Files (VDF) will wait before being terminated. |`5000` | 
| shared.resources.dir | The location on the Ambari Server where resources are stored. This is exposed via HTTP in order for Ambari Agents to access them. |`/usr/lib/ambari-server/lib/ambari_commons/resources` | 
| ssl.trustStore.password | The password to use when setting the `javax.net.ssl.trustStorePassword` property | | 
| ssl.trustStore.path | The location of the truststore to use when setting the `javax.net.ssl.trustStore` property. | | 
| ssl.trustStore.type | The type of truststore used by the `javax.net.ssl.trustStoreType` property. | | 
| stack.hooks.folder | A location of hooks folder relative to resources folder. |`stack-hooks` | 
| stack.java.home | The location of the JDK on the Ambari Agent hosts for stack services.<br/><br/>The following are examples of valid values:<ul><li>`/usr/jdk64/jdk1.7.0_45`</ul> | | 
| stack.java.version | JDK version of the stack, use in case of it differs from Ambari JDK version.<br/><br/>The following are examples of valid values:<ul><li>`1.7`</ul> | | 
| stack.jce.name | The name of the JCE policy ZIP file for stack services.<br/><br/>The following are examples of valid values:<ul><li>`UnlimitedJCEPolicyJDK7.zip`</ul> | | 
| stack.jdk.name | The name of the JDK installation binary for stack services.<br/><br/>The following are examples of valid values:<ul><li>`jdk-7u45-linux-x64.tar.gz`</ul> | | 
| stack.upgrade.auto.retry.check.interval.secs | The amount of time to wait, in seconds, between checking for upgrade tasks to be retried. This value is only applicable if `stack.upgrade.auto.retry.timeout.mins` is positive.<br/><br/> This property is related to `stack.upgrade.auto.retry.timeout.mins`. |`20` | 
| stack.upgrade.auto.retry.command.details.to.ignore | A comma-separate list of upgrade tasks details to skip when retrying failed commands automatically. |`"Execute HDFS Finalize"` | 
| stack.upgrade.auto.retry.command.names.to.ignore | A comma-separate list of upgrade tasks names to skip when retrying failed commands automatically. |`"ComponentVersionCheckAction","FinalizeUpgradeAction"` | 
| stack.upgrade.auto.retry.timeout.mins | The amount of time to wait in order to retry a command during a stack upgrade when an agent loses communication. This value must be greater than the `agent.task.timeout` value. |`0` | 
| stack.upgrade.bypass.prechecks | Determines whether pre-upgrade checks will be skipped when performing a rolling or express stack upgrade. |`false` | 
| stack.upgrade.default.parallelism | Default value of max number of tasks to schedule in parallel for upgrades. Upgrade packs can override this value. |`100` | 
| stackadvisor.script | The location and name of the Python stack advisor script executed when configuring services. |`/var/lib/ambari-server/resources/scripts/stack_advisor.py` | 
| stomp.max_buffer.message.size | The maximum size of a buffer for stomp message sending. Default is 5 MB. |`5242880` | 
| stomp.max_incoming.message.size | The maximum size of an incoming stomp text message. Default is 2 MB. |`2097152` | 
| subscription.registry.cache.size | Maximal cache size for spring subscription registry. |`1500` | 
| task.query.parameterlist.size | The maximum number of tasks which can be queried by ID from the database. |`999` | 
| topology.task.creation.parallel | Indicates whether parallel topology task creation is enabled |`false` | 
| topology.task.creation.parallel.threads | The number of threads to use for parallel topology task creation if enabled |`10` | 
| view.extract-after-cluster-config | Drives view extraction in case of blueprint deployments; non-system views are deployed when cluster configuration is successful |`false` | 
| view.extraction.threadpool.size.core | The number of threads used to extract Ambari Views when Ambari Server is starting up. |`10` | 
| view.extraction.threadpool.size.max | The maximum number of threads used to extract Ambari Views when Ambari Server is starting up. |`20` | 
| view.extraction.threadpool.timeout | The time, in milliseconds, that non-core threads will live when extraction views on Ambari Server startup. |`100000` | 
| view.request.threadpool.size.max | The maximum number of threads which will be allocated to handling REST API requests from embedded views. This value should be smaller than `agent.threadpool.size.max`<br/><br/> This property is related to `agent.threadpool.size.max`. |`0` | 
| view.request.threadpool.timeout | The time, milliseconds, that REST API requests from embedded views can wait if there are no threads available to service the view's request. Setting this too low can cause views to timeout. |`2000` | 
| views.additional.classpath | Additional class path added to each Ambari View. Comma separated jars or directories | | 
| views.ambari.request.connect.timeout.millis | The amount of time, in milliseconds, that a view will wait when trying to connect on HTTP(S) operations to the Ambari REST API. |`30000` | 
| views.ambari.request.read.timeout.millis | The amount of time, in milliseconds, that a view will wait before terminating an HTTP(S) read request to the Ambari REST API. |`45000` | 
| views.dir | The directory on the Ambari Server file system used for expanding Views and storing webapp work. |`/var/lib/ambari-server/resources/views` | 
| views.directory.watcher.disable | Determines whether the view directory watcher service should be disabled. |`false` | 
| views.http.cache-control | The value that will be used to set the `Cache-Control` HTTP response header for Ambari View requests. |`no-store` | 
| views.http.charset | The value that will be used to set the Character encoding to HTTP response header for Ambari View requests. |`utf-8` | 
| views.http.pragma | The value that will be used to set the `PRAGMA` HTTP response header for Ambari View requests. |`no-cache` | 
| views.http.strict-transport-security | The value that will be used to set the `Strict-Transport-Security` HTTP response header for Ambari View requests. |`max-age=31536000` | 
| views.http.x-content-type-options | The value that will be used to set the `X-CONTENT-TYPE` HTTP response header for Ambari View requests. |`nosniff` | 
| views.http.x-frame-options | The value that will be used to set the `X-Frame-Options` HTTP response header for Ambari View requests. |`SAMEORIGIN` | 
| views.http.x-xss-protection | The value that will be used to set the `X-XSS-Protection` HTTP response header for Ambari View requests. |`1; mode=block` | 
| views.remove.undeployed | Determines whether remove undeployed views from the Ambari database. |`false` | 
| views.request.connect.timeout.millis | The amount of time, in milliseconds, that a view will wait when trying to connect on HTTP(S) operations to a remote resource. |`5000` | 
| views.request.read.timeout.millis | The amount of time, in milliseconds, that a view will wait before terminating an HTTP(S) read request. |`10000` | 
| views.validate | Determines whether to validate a View's configuration XML file against an XSD. |`false` | 
| webapp.dir | The Ambari Server webapp root directory. |`web` | 


<a name="baseline-values"></a>
## Baseline Values
As the size of a cluster grows, some of the default property values may no longer be sufficient. The below tables offer recommendations on the values of some configuration properties based on the size and usage of the cluster.

#### Alerts & Notifications
| Property Name | 10 Hosts | ~50 Hosts | ~100 Hosts | 500+ Hosts | 
| --- | --- | --- | --- | --- |
| alerts.execution.scheduler.threadpool.size.core | 2 | 2 | 4 | 4 | 
| alerts.execution.scheduler.threadpool.size.max | 2 | 2 | 8 | 8 | 
| alerts.execution.scheduler.threadpool.worker.size | 400 | 2000 | 4000 | 20000 | 
| alerts.cache.enabled | false | false | false | true | 
| alerts.cache.flush.interval | 10 | 10 | 10 | 10 | 
| alerts.cache.size | 50000 | 50000 | 100000 | 100000 | 

#### Jetty API & Agent Thread Pools
| Property Name | 10 Hosts | ~50 Hosts | ~100 Hosts | 500+ Hosts | 
| --- | --- | --- | --- | --- |
| client.threadpool.size.max | 25 | 35 | 50 | 65 | 
| agent.threadpool.size.max | 25 | 35 | 75 | 100 | 



<a name="db-persistence"></a>
## Database And Persistence
In addition to the static database connection properties, it's possible to supply custom properties for both EclipseLink and the database driver through `ambari.properties`. 

### Database Driver
Using the `server.jdbc.properties.` prefix, it's possible to supply specific properties to the database driver being used by Ambari. These properties do not affect JPA or any of the persistence-unit operations. They are solely to govern the behavior of the database driver. 

```
server.jdbc.properties.lockTimeout=15000
server.jdbc.properties.loginTimeout=15000
```

### Persistence Unit
EclipseLink properties can also be configured using a prefix of `server.persistence.properties.`. The EclipseLink properties should be defined in their entirety with the prefix prepended in front of them.

```
server.persistence.properties.eclipselink.jdbc.batch-writing.size=25
server.persistence.properties.eclipselink.profiler=QueryMonitor
```