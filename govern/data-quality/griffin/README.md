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


# Apache Griffin  
[![Build Status](https://travis-ci.org/apache/griffin.svg?branch=master)](https://travis-ci.org/apache/griffin) [![License: Apache 2.0](https://camo.githubusercontent.com/8cb994f6c4a156c623fe057fccd7fb7d7d2e8c9b/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6c6963656e73652d417061636865253230322d3445423142412e737667)](https://www.apache.org/licenses/LICENSE-2.0.html)    

The data quality (DQ) is a key criteria for many data consumers like IoT, machine learning etc., however, there is no standard agreement on how to determine “good” data.
Apache Griffin is a model-driven data quality service platform where you can examine your data on-demand. It provides a standard process to define data quality measures, executions and reports, allowing those examinations across multiple data systems.
When you don't trust your data, or concern that poorly controlled data can negatively impact critical decision, you can utilize Apache Griffin to ensure data quality.


## Getting Started

### Quick Start

You can try running Griffin in docker following the [docker guide](griffin-doc/docker/griffin-docker-guide.md).

### Environment for Dev

Follow [Apache Griffin Development Environment Build Guide](griffin-doc/dev/dev-env-build.md) to set up development environment.
<br>If you want to contribute codes to Griffin, please follow [Apache Griffin Development Code Style Config Guide](griffin-doc/dev/code-style.md) to keep consistent code style.

### Deployment at Local

If you want to deploy Griffin in your local environment, please follow [Apache Griffin Deployment Guide](griffin-doc/deploy/deploy-guide.md).

## Community

For more information about Griffin, please visit our website at: [griffin home page](http://griffin.apache.org).

You can contact us via email:
- dev-list: <a href="mailto:dev@griffin.apache.org">dev@griffin.apache.org</a>
- user-list: <a href="mailto:users@griffin.apache.org">users@griffin.apache.org</a>

You can also subscribe the latest information by sending a email to [subscribe dev-list](mailto:dev-subscribe@griffin.apache.org) and [subscribe user-list](mailto:users-subscribe@griffin.apache.org).
You can also subscribe the latest information by sending a email to subscribe dev-list and user-list:
```
dev-subscribe@griffin.apache.org
users-subscribe@griffin.apache.org
```

You can access our issues on [JIRA page](https://issues.apache.org/jira/browse/GRIFFIN)

## Contributing

See [How to Contribute](http://griffin.apache.org/docs/contribute.html) for details on how to contribute code, documentation, etc.

Here's the most direct way to contribute your work merged into Apache Griffin.

* Fork the project from [github](https://github.com/apache/griffin)
* Clone down your fork
* Implement your feature or bug fix and commit changes
* Push the branch up to your fork
* Send a pull request to Apache Griffin master branch


## References
- [Home Page](http://griffin.apache.org/)
- [Wiki](https://cwiki.apache.org/confluence/display/GRIFFIN/Apache+Griffin)
- Documents:
	- [Measure](griffin-doc/measure)
	- [Service](griffin-doc/service)
	- [UI](griffin-doc/ui)
	- [Docker usage](griffin-doc/docker)
	- [Postman API](griffin-doc/service/postman)
