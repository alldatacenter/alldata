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

#Ambari Views Commons Module

Have this as a dependency in the view which need the functionality.

This has common code for:

* HDFS access

**Note: More to be added later**

### How to Include it in dependant project

In the service class for the project use `commons` projects code in the way described in the below example for `files` view.

```java
package org.apache.ambari.view.filebrowser;

import javax.ws.rs.Path;

import org.apache.ambari.view.ViewContext;

import com.google.inject.Inject;
import org.apache.ambari.view.commons.hdfs.FileOperationService;
import org.apache.ambari.view.commons.hdfs.UploadService;
import org.apache.ambari.view.commons.hdfs.UserService;

/**
 * Root files service
 */
public class FileBrowserService {

  @Inject
  ViewContext context;

  /**
   * @see UploadService
   * @return service
   */
  @Path("/upload")
  public UploadService upload() {
    return new UploadService(context);
  }

  /**
   * @see org.apache.ambari.view.commons.hdfs.FileOperationService
   * @return service
   */
  @Path("/fileops")
  public FileOperationService fileOps() {
    return new FileOperationService(context);
  }

  /**
   * @see org.apache.ambari.view.commons.hdfs.UserService
   * @return service
   */
  @Path("/user")
  public UserService userService() { return new UserService(context); }

}
```


####Also, look into the various ember addons that are included in `src/main/resources/ui`.
Currently we have:

* hdfs-directory-viewer

**More to be added later**