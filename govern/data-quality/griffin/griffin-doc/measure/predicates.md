<!--
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

# About predicates

## Overview
The purpose of predicates is obligate Griffin to check certain conditions before starting SparkSubmitJob. 
Depending on these conditions Griffin need to start or not start the measurement.

## Configure predicates
For configuring predicates need add property to measure json:
```
{
    ...
     "data.sources": [
        ...
         "connector": {
                   "predicates": [
                       {
                         "type": "file.exist",
                         "config": {
                           "root.path": "/path/to/",
                           "path": "file.ext,file2.txt"
                         }
                       }
                   ],
         ...
         }
}
```

Possible values for predicates.type:
- "file.exist" - in this case creates predicate with class org.apache.griffin.core.job.FileExistPredicator. This predicate checks existence of files before starting Spark jobs.
 ```
                     {
                         "type": "file.exist",
                         "config": {
                           "root.path": "/path/to/",
                           "path": "file.ext,file2.txt"
                         }
                       }
```

- "custom" - in this case required transmit class name in the property "class" in config. 
This example creates same predicate like in previous example
```
                     {
                         "type": "custom",
                         "config": {
                           "class": "org.apache.griffin.core.job.FileExistPredicator",
                           "root.path": "/path/to/",
                           "path": "file.ext,file2.txt"
                         }
                       }
```
It important to notice that predicate class must satisfy follow conditions:
- implement interface **org.apache.griffin.core.job.Predicator**
- have constructor with argument of type **org.apache.griffin.core.job.entity.SegmentPredicate**

## Deployment custom predicates
For the creating custom predicate you need 
1. Build the Griffin service using command
As a result, two artifacts will be built,  
- **service-VERSION.jar** - executable Spring-Boot application
- **service-VERSION-lib.jar** - jar, which we can use as a dependency
This step is necessary because we can't use executable Spring-Boot application as a dependency in our plugin. 
2. Create module and add dependency that was built in previous step
```
         <dependency>
             <groupId>org.apache.griffin</groupId>
             <artifactId>service</artifactId>
             <classifier>lib</classifier>
             <version>${griffin.version}</version>
             <scope>provided</scope>
         </dependency>
```
3. Create a Predicate class, which should, as mentioned earlier, implement the Predicator interface and have a constructor with an argument of type SegmentPredicate
4. Build the module into a jar file and put it in any folder (for example /path-to-jar)
5. Start the Griffin service application using command 
```
java -cp target/service-VERSION.jar -Dloader.path=/path-to-jar/ org.springframework.boot.loader.PropertiesLauncher
```