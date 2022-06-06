<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->


how to build
--------------------

```
docker build -t ambari/build ./dev-support/docker/docker
```

how to run
--------------------

```
# bash
docker run --privileged -t -i -p 80:80 -p 5005:5005 -p 8080:8080 -h node1.mydomain.com --name ambari1 -v ${AMBARI_SRC:-$(pwd)}:/tmp/ambari ambari/build bash
# where 5005 is java debug port and 8080 is the default http port, if no --privileged ambari-server start fails due to access to /proc/??/exe
# -t is required otherwise, sudo commands do not run

# build, install ambari and deploy hadoop in container
cd {ambari src}
docker rm ambari1
docker run --privileged -t -p 80:80 -p 5005:5005 -p 8080:8080 -h node1.mydomain.com --name ambari1 -v ${AMBARI_SRC:-$(pwd)}:/tmp/ambari ambari/build /tmp/ambari-build-docker/bin/ambaribuild.py [test|server|agent|deploy] [-b] [-s [HDP|BIGTOP|PHD]] [-d] [-c]
where
test: mvn test
server: install and run ambari-server
agent: install and run ambari-server and ambari-agent
deploy: install and run ambari-server and ambari-agent, and deploy a hadoop
-b option to rebuild ambari
-d option to start ambari-server with --debug option
-c option to clean local git repo. "git clean -xdf"
```

how to run unit test
--------------------
```
cd dev-support/docker/docker
python -m bin.test.ambaribuild_test

```

