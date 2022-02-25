<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
# Workflow Manager-UI

This is the repository for the Workflow Manager web UI . This has Dashboard where Oozie jobs can be monitored, run, stip etc..It also has a desiger which allows
you to develop a oozie work flow in a graphical interface.

Technologies Used.
it uses Ember as underline JS framework.
JsPlbumb for designer.
Dagre for layout in designer.
bootstrap for css.

## Prerequisites

If you are not building using maven, you will need the following things properly installed on your computer. Maven build would download and setup the prerequisites for you.

* [Git](http://git-scm.com/)
* [Node.js](http://nodejs.org/) (with NPM)
* [Bower](http://bower.io/)
* [Ember CLI](http://www.ember-cli.com/)
* [PhantomJS](http://phantomjs.org/)

## Installation

* `git clone <repository-url>` this repository
* change into the new directory
* To build the deployable war file, run `mvn clean package`

in the main directory, just do mvn install.

For doing local development
==============================
Go to folder under wfmanager\src\main\resources\ui.
* `npm install`
* `bower install`


## Running / Development using node and ember-cli

In development mode (and in non Ambari View mode), you might want to connect the UI with already running remote/local oozie server. To do so you can run the proxy so that UI can route the requests through it.

* `node proxy.js <local-port> <remote-oozie-host> <remote-oozie-port>`

By default local port is `11002`, default oozie host is `localhost` with default port as `11000`

* `node proxy.js`
* `Proxy on: localhost:11002 -> localhost:11000`

If you want to use different local port (other than `11002`) then update the API_URL in `environment.js` accordingly.

* `ember server`
* Visit your app at [http://localhost:4300](http://localhost:4300).

### Building and Running through Maven

* `mvn clean package` (Build web ui and puts into /dist)
* `mvn test -Pproxy` (start proxy for the oozie API)
* In another console tab `mvn test -Pserver` (does ember server)
* Wheather you use maven or ember, your app would be accessible at [http://localhost:4300](http://localhost:4300).

### Setup and Run sample Oozie workflows, bundles and coordinators

* Get and start the latest Hortonworks Sandbox (if you haven't yet)
* From the host machine, `ssh root@127.0.0.1 -p 2222` to get into Sandbox
* Switch to a `guest` user using `su - guest`
* Inside the sandbox, `tar -xvf /usr/hdp/current/oozie-client/doc/oozie-examples.tar.gz`
* Correct job.properties `find ./examples/apps/ -iname "job.properties" | xargs sed -i 's/localhost/sandbox.hortonworks.com/g'`
* Set hadoop user: `export HADOOP_USER_NAME=guest`
* Move the examples into HDFS: `hdfs dfs -put ./examples/ /user/guest/examples`
* Set oozie user `export OOZIE_USER_NAME=guest`
* Submit and run all the jobs: `find ./examples/apps/ -iname "job.properties" | xargs -i oozie job -oozie http://localhost:11000/oozie -config  {} -run`

### Oozie Ambari View

This UI can be built and deployed as an Ambari view. Below are the steps to build the Ambari view.

* `cd oozie-ambari-view`
* `mvn clean package` - This builds `target/oozie-ambari-view-0.0.0.1-SNAPSHOT.jar`
* `cp target/oozie-ambari-view-0.0.0.1-SNAPSHOT.jar </var/lib/ambari-server/resources/views/>`
* restart your ambari server and Oozie Amabri View would be available in Ambari

### Code Generators

Make use of the many generators for code, try `ember help generate` for more details

### Running Tests

* `ember test`
* `ember test --server`

## Further Reading / Useful Links

* [ember.js](http://emberjs.com/)
* [ember-cli](http://www.ember-cli.com/)
* Development Browser Extensions
  * [ember inspector for chrome](https://chrome.google.com/webstore/detail/ember-inspector/bmdblncegkenkacieihfhpjfppoconhi)
  * [ember inspector for firefox](https://addons.mozilla.org/en-US/firefox/addon/ember-inspector/)

How the code is organized.
There are 2 main components- Dashboard and Designer.
1) Designer


2) Dashboard.
