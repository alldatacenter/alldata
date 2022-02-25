---
name: Security Details
route: /Security
menu: Documentation
submenu: Security
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Security Features of Apache Atlas


## Overview

The following features are available for enhancing the security of the platform:
   * SSL
   * Service Authentication
   * SPNEGO-based HTTP Authentication

## SSL

Both SSL one-way (server authentication) and two-way (server and client authentication) are supported.  The following application properties (properties configured in the atlas-application.properties file) are available for configuring SSL:

   * `atlas.enableTLS` (false|true) [default: false] - enable/disable the SSL listener
   * `keystore.file` - the path to the keystore file leveraged by the server.  This file contains the server certificate.
   * `truststore.file` - the path to the truststore file. This file contains the certificates of other trusted entities (e.g. the certificates for client processes if two-way SSL is enabled).  In most instances this can be set to the same value as the keystore.file property (especially if one-way SSL is enabled).
   * `client.auth.enabled` (false|true) [default: false] - enable/disable client authentication.  If enabled, the client will have to authenticate to the server during the transport session key creation process (i.e. two-way SSL is in effect).
   * `cert.stores.credential.provider.path` - the path to the Credential Provider store file.  The passwords for the keystore, truststore, and server certificate are maintained in this secure file.  Utilize the cputil script in the 'bin' directoy (see below) to populate this file with the passwords required.
   * `atlas.ssl.exclude.cipher.suites` - the excluded Cipher Suites list -  *NULL.*,.*RC4.*,.*MD5.*,.*DES.*,.*DSS.* are weak and unsafe Cipher Suites that are excluded by default. If additional Ciphers need to be excluded, set this property with the default Cipher Suites such as atlas.ssl.exclude.cipher.suites=.*NULL.*, .*RC4.*, .*MD5.*, .*DES.*, .*DSS.*, and add the additional Ciper Suites to the list with a comma separator. They can be added with their full name or a regular expression. The Cipher Suites listed in the atlas.ssl.exclude.cipher.suites property will have precedence over the default Cipher Suites. One would keep the default Cipher Suites, and add additional ones to be safe.

####  Credential Provider Utility Script

In order to prevent the use of clear-text passwords, the Atlas platofrm makes use of the Credential Provider facility for secure password storage (see [Hadoop Credential Command Reference](http://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/CommandsManual.html#credentia) for more information about this facility).  The cputil script in the `bin` directory can be leveraged to create the password store required.

To create the credential provdier for Atlas:

* cd to the `bin` directory
* type `./cputil.py`
* Enter the path for the generated credential provider.  The format for the path is:
   * jceks://file/local/file/path/file.jceks or jceks://hdfs@namenodehost:port/path/in/hdfs/to/file.jceks.  The files generally use the ".jceks" extension (e.g. test.jceks)
* Enter the passwords for the keystore, truststore, and server key (these passwords need to match the ones utilized for actually creating the associated certificate store files).

The credential provider will be generated and saved to the path provided.

## Service Authentication

The Atlas platform, upon startup, is associated to an authenticated identity.  By default, in an insecure environment, that identity is the same as the OS authenticated user launching the server.  However, in a secure cluster leveraging kerberos, it is considered a best practice to configure a keytab and principal in order for the platform to authenticate to the KDC. This allows the service to subsequently interact with other secure cluster services (e.g. HDFS).

The properties for configuring service authentication are:

   * `atlas.authentication.method` (simple|kerberos) [default: simple] - the authentication method to utilize.  Simple will leverage the OS authenticated identity and is the default mechanism.  'kerberos' indicates that the service is required to authenticate to the KDC leveraging the configured keytab and principal.
   * `atlas.authentication.keytab` - the path to the keytab file.
   * `atlas.authentication.principal` - the principal to use for authenticating to the KDC.  The principal is generally of the form "user/host@realm".  You may use the '_HOST' token for the hostname and the local hostname will be substituted in by the runtime (e.g. "Atlas/_HOST@EXAMPLE.COM")

> Note that when Atlas is configured with HBase as the storage backend in a secure cluster, the graph db (JanusGraph) needs sufficient user permissions to be able to create and access an HBase table.  To grant the appropriate permissions see [Graph persistence engine - Hbase](#/Configuration).

### JAAS configuration

In a secure cluster, some of the components (such as Kafka) that Atlas interacts with, require Atlas to authenticate itself to them using JAAS. The following properties are used to set up appropriate JAAS Configuration.

   * `atlas.jaas.client-id.loginModuleName` - the authentication method used by the component (for example, com.sun.security.auth.module.Krb5LoginModule)
   * `atlas.jaas.client-id.loginModuleControlFlag` (required|requisite|sufficient|optional) [default: required]
   * `atlas.jaas.client-id.option.useKeyTab` (true|false)
   * `atlas.jaas.client-id.option.storeKey` (true | false)
   * `atlas.jaas.client-id.option.serviceName` - service name of server component
   * `atlas.jaas.client-id.option.keyTab` = `<atlas keytab>`
   * `atlas.jaas.client-id.option.principal` = `<atlas principal>`

For example, the following property settings in jaas-application.properties file


<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.jaas.KafkaClient.loginModuleName = com.sun.security.auth.module.Krb5LoginModule
atlas.jaas.KafkaClient.loginModuleControlFlag = required
atlas.jaas.KafkaClient.option.useKeyTab = true
atlas.jaas.KafkaClient.option.storeKey = true
atlas.jaas.KafkaClient.option.serviceName = kafka
atlas.jaas.KafkaClient.option.keyTab = /etc/security/keytabs/kafka_client.keytab
atlas.jaas.KafkaClient.option.principal = kafka-client-1@EXAMPLE.COM
atlas.jaas.MyClient.0.loginModuleName = com.sun.security.auth.module.Krb5LoginModule
atlas.jaas.MyClient.0.loginModuleControlFlag = required
atlas.jaas.MyClient.0.option.useKeyTab = true
atlas.jaas.MyClient.0.option.storeKey = true
atlas.jaas.MyClient.0.option.serviceName = kafka
atlas.jaas.MyClient.0.option.keyTab = /etc/security/keytabs/kafka_client.keytab
atlas.jaas.MyClient.0.option.principal = kafka-client-1@EXAMPLE.COM
atlas.jaas.MyClient.1.loginModuleName = com.sun.security.auth.module.Krb5LoginModule
atlas.jaas.MyClient.1.loginModuleControlFlag = optional
atlas.jaas.MyClient.1.option.useKeyTab = true
atlas.jaas.MyClient.1.option.storeKey = true
atlas.jaas.MyClient.1.option.serviceName = kafka
atlas.jaas.MyClient.1.option.keyTab = /etc/security/keytabs/kafka_client.keytab
atlas.jaas.MyClient.1.option.principal = kafka-client-1@EXAMPLE.COM`}
</SyntaxHighlighter>

will set the JAAS configuration that is equivalent to the following jaas.conf file entries.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`KafkaClient {
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   storeKey=true
   serviceName=kafka
   keyTab="/etc/security/keytabs/kafka_client.keytab"
   principal="kafka-client-1@EXAMPLE.COM";
};
MyClient {
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   storeKey=true
   serviceName=kafka keyTab="/etc/security/keytabs/kafka_client.keytab"
   principal="kafka-client-1@EXAMPLE.COM";
};
MyClient {
   com.sun.security.auth.module.Krb5LoginModule optional
   useKeyTab=true
   storeKey=true
   serviceName=kafka
   keyTab="/etc/security/keytabs/kafka_client.keytab"
   principal="kafka-client-1@EXAMPLE.COM";
};`}
</SyntaxHighlighter>


## SPNEGO-based HTTP Authentication

HTTP access to the Atlas platform can be secured by enabling the platform's SPNEGO support.  There are currently two supported authentication mechanisms:

   * `simple` - authentication is performed via a provided user name
   * `kerberos` - the KDC authenticated identity of the client is leveraged to authenticate to the server

The kerberos support requires the client accessing the server to first authenticate to the KDC (usually this is done via the 'kinit' command).  Once authenticated, the user may access the server (the authenticated identity will be related to the server via the SPNEGO negotiation mechanism).

The properties for configuring the SPNEGO support are:

   * `atlas.http.authentication.enabled` (true|false) [default: false] - a property indicating whether to enable HTTP authentication
   * `atlas.http.authentication.type` (simple|kerberos) [default: simple] - the authentication type
   * `atlas.http.authentication.kerberos.principal` - the web-application Kerberos principal name. The Kerberos principal name must start with "HTTP/...". For example: "HTTP/localhost@LOCALHOST". There is no default value.
   * `atlas.http.authentication.kerberos.keytab` - the path to the keytab file containing the credentials for the kerberos principal.
   * `atlas.rest.address` - `<http/https>://<atlas-fqdn>:<atlas port>`

For a more detailed discussion of the HTTP authentication mechanism refer to [Hadoop Auth, Java HTTP SPNEGO 2.6.0 - Server Side Configuration](http://hadoop.apache.org/docs/stable/hadoop-auth/Configuration.html).  The prefix that document references is "atlas.http.authentication" in the case of the Atlas authentication implementation.

### Client security configuration

When leveraging Atlas client code to communicate with an Atlas server configured for SSL transport and/or Kerberos authentication, there is a requirement to provide the Atlas client configuration file that provides the security properties that allow for communication with, or authenticating to, the server.

Update the atlas-application.properties file with the appropriate settings (see below) and copy it to the client's classpath or to the directory specified by the "atlas.conf" system property.

The client properties for SSL communication are:

   * `atlas.enableTLS` (false|true) [default: false] - enable/disable the SSL client communication infrastructure.
   * `keystore.file` - the path to the keystore file leveraged by the client.  This file is only required if 2-Way SSL is enabled at the server and contains the client certificate.
   * `truststore.file` - the path to the truststore file. This file contains the certificates of trusted entities (e.g. the certificates for the server or a shared certification authority). This file is required for both one-way or two-way SSL.
   * `cert.stores.credential.provider.path` - the path to the Credential Provider store file.  The passwords for the keystore, truststore, and client certificate are maintained in this secure file.

The property required for authenticating to the server (if authentication is enabled):

   * `atlas.http.authentication.type` (simple|kerberos) [default: simple] - the authentication type

### SOLR Kerberos configuration
If the authentication type specified is 'kerberos', then the kerberos ticket cache will be accessed for authenticating to the server (Therefore the client is required to authenticate to the KDC prior to communication with the server using 'kinit' or a similar mechanism).

See [the Apache SOLR Kerberos configuration](https://cwiki.apache.org/confluence/display/RANGER/How+to+configure+Solr+Cloud+with+Kerberos+for+Ranger+0.5).

   * Add principal and generate the keytab file for solr.  Create a keytab per host for each host where Solr is going to run and use the principal name with the host (e.g. addprinc -randkey solr/${HOST1}@EXAMPLE.COM. Replace ${HOST1} with the actual host names).



<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`kadmin.local
kadmin.local:  addprinc -randkey solr/<hostname>@EXAMPLE.COM
kadmin.local:  xst -k solr.keytab solr/<hostname>@EXAMPLE.COM
kadmin.local:  quit`}
</SyntaxHighlighter>

   * Add principal and generate the keytab file for authenticating HTTP request. (Note that if Ambari is used to Kerberize the cluster, the keytab /etc/security/keytabs/spnego.service.keytab can be used)


<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`kadmin.local
kadmin.local:  addprinc -randkey HTTP/<hostname>@EXAMPLE.COM
kadmin.local:  xst -k HTTP.keytab HTTP/<hostname>@EXAMPLE.COM
kadmin.local:  quit`}
</SyntaxHighlighter>

   * Copy the keytab file to all the hosts running Solr.


<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`cp solr.keytab /etc/security/keytabs/
chmod 400 /etc/security/keytabs/solr.keytab
cp HTTP.keytab /etc/security/keytabs/
chmod 400 /etc/security/keytabs/HTTP.keytab`}
</SyntaxHighlighter>



   * Create path in Zookeeper for storing the Solr configs and other parameters.


<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`$SOLR_INSTALL_HOME/server/scripts/cloud-scripts/zkcli.sh -zkhost $ZK_HOST:2181 -cmd makepath solr`}
</SyntaxHighlighter>


   * Upload the configuration to Zookeeper.


<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`$SOLR_INSTALL_HOME/server/scripts/cloud-scripts/zkcli.sh -cmd upconfig  -zkhost $ZK_HOST:2181/solr -confname basic_configs -confdir $SOLR_INSTALL_HOME/server/solr/configsets/_default/conf`}
</SyntaxHighlighter>



   * Create the JAAS configuration.


<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`vi /etc/solr/conf/solr_jaas.conf
Client {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  keyTab="/etc/security/keytabs/solr.keytab"
  storeKey=true
  useTicketCache=true
  debug=true
  principal="solr/<hostname>@EXAMPLE.COM";
};`}
</SyntaxHighlighter>


   * Copy /etc/solr/conf/solr_jaas.conf to all hosts running Solr.
   * Edit solr.in.sh in $SOLR_INSTALL_HOME/bin/


<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`vi $SOLR_INSTALL_HOME/bin/solr.in.sh
SOLR_JAAS_FILE=/etc/solr/conf/solr_jaas.conf
SOLR_HOST='hostname -f'
ZK_HOST="$ZK_HOST1:2181,$ZK_HOST2:2181,$ZK_HOST3:2181/solr"
KERBEROS_REALM="EXAMPLE.COM"
SOLR_KEYTAB=/etc/solr/conf/solr.keytab
SOLR_KERB_PRINCIPAL=HTTP@{KERBEROS_REALM}
SOLR_KERB_KEYTAB=/etc/solr/conf/HTTP.keytab
SOLR_AUTHENTICATION_CLIENT_CONFIGURER="org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer"
SOLR_AUTHENTICATION_OPTS=" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config={SOLR_JAAS_FILE} -Dsolr.kerberos.principal={SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab={SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain={SOLR_HOST} -Dhost={SOLR_HOST} -Dsolr.kerberos.name.rules=DEFAULT"`}
</SyntaxHighlighter>

   * Copy solr.in.sh to all hosts running Solr.
   * Set up Solr to use the Kerberos plugin by uploading the security.json.


<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`$SOLR_INSTALL_HOME/server/scripts/cloud-scripts/zkcli.sh -zkhost <zk host>:2181 -cmd put /security.json '{"authentication":{"class": "org.apache.solr.security.KerberosPlugin"}}'`}
</SyntaxHighlighter>


   * Start Solr.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`$SOLR_INSTALL_HOME/bin/solr start -cloud -z $ZK_HOST1:2181,$ZK_HOST2:2181,$ZK_HOST3:2181 -noprompt`}
</SyntaxHighlighter>


   * Test Solr


<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`kinit -k -t /etc/security/keytabs/HTTP.keytab HTTP/<host>@EXAMPLE.COM
curl --negotiate -u : "http://<host>:8983/solr/"`}
</SyntaxHighlighter>

 * Create collections in Solr corresponding to the indexes that Atlas uses and change the Atlas configuration to point to the Solr instance setup as described in the [Install Steps](#/Installation)
