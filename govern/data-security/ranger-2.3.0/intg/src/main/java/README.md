<!---
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Ranger - Java client

Java library for Apache Ranger.

## Installation

#### Prerequisites
- Java 8
- Maven 3.6.3

Verify installation of java using the below command.

```
$ java -version
java version "1.8.0_281"
Java(TM) SE Runtime Environment (build 1.8.0_281-b09)
Java HotSpot(TM) 64-Bit Server VM (build 25.281-b09, mixed mode)
```
Libraries
```
import org.apache.ranger.*;  
import org.apache.ranger.plugin.model.*;
```

Add the following dependency to pom.xml
```xml
<dependency>
    <groupId>org.apache.ranger</groupId>
    <artifactId>ranger-intg</artifactId>
    <version>3.0.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```
## Usage

```
mvn clean package
mvn install dependency:copy-dependencies
java -cp "target/<module name>-3.0.0-SNAPSHOT.jar:target/dependency/*" testRanger
```

```java
// testRanger
import java.util.*;
import org.apache.ranger.*;
import org.apache.ranger.plugin.model.*;

public class testRanger {

    public static void main(String[] args) throws RangerServiceException {
        // create a client to connect to Apache Ranger admin
        String rangerUrl = "http://localhost:6080";
        String username  = "admin";
        String password  = "rangerR0cks!";

        /* for kerberos authentication:
           authType = "kerberos"
           username = principal
           password = path of the keytab file */

        // For SSL enabled ranger admin use SSL config file (see: ranger-examples/sample-client/conf/ssl-client.xml)
        RangerClient rangerClient = new RangerClient(rangerUrl, "simple", username, password, null);
        String serviceDefName     = "hive";
        String serviceName        = "testHive";
        String policyName         = "testPolicy";

        /*
        Create a new Service
         */
        RangerService service = new RangerService();
        service.setType(serviceDefName);
        service.setName(serviceName);
        Map<String, String> config = new HashMap<>();
        config.put("username", "hive");
        config.put("password", "hive");
        config.put("jdbc.driverClassName", "org.apache.hive.jdbc.HiveDriver");
        config.put("jdbc.url", "jdbc:hive2://ranger-hadoop:10000");
        config.put("hadoop.security.authorization", "true");
        service.setConfigs(config);
        RangerService createdService = rangerClient.createService(service);
        System.out.println("New Service created with id: " + createdService.getId());


        /*
        Create a new Policy
         */
        Map<String, RangerPolicy.RangerPolicyResource> resource = new HashMap<>();
        resource.put("database", new RangerPolicy.RangerPolicyResource("test_db"));
        resource.put("table", new RangerPolicy.RangerPolicyResource("test_table"));
        resource.put("column", new RangerPolicy.RangerPolicyResource("*"));
        RangerPolicy policy = new RangerPolicy();
        policy.setService(serviceName);
        policy.setName(policyName);
        policy.setResources(resource);

        RangerPolicy createdPolicy = rangerClient.createPolicy(policy);
        System.out.println("New Policy created with id: " + createdPolicy.getId());


        /*
        Delete a policy
         */
        rangerClient.deletePolicy(serviceName, policyName);
        System.out.println("Policy with name: " + policyName + " deleted successfully");


        /*
        Delete a Service
         */
        rangerClient.deleteService(serviceName);
        System.out.println("Service with name: " + serviceName + " deleted successfully");

    }
}
```

For more examples, checkout `SampleClient` implementation in [ranger-examples](https://github.com/apache/ranger/blob/master/ranger-examples/sample-client/src/main/java/org/apache/ranger/examples/sampleclient/SampleClient.java) module.
