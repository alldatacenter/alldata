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

# Hdfs-directory-viewer

Ember Addon to view the HDFS file system.

Different Ambari views can use this in their view. Common code should be usable in every view.

# How to use it

### Including it in dependant project
Add the following code in package.json of the dependant view

```javascript
"name": "files",
"ember-addon": {
	"paths": [
	  "../../../../../commons/src/main/resources/ui/hdfs-directory-viewer"
	]
}
```

`paths` is an array which includes all the addons shares in ```commons``` library. The entries should be the relative path to the addon in this ```commons``` repository.

### Including the UI dependencies in the dependent project
As we are going to include the component using the `ember-addon` config in `package.json` and not by the `ember install` way, the UI dependencies also has to be included in the dependent projects `bower.json` file if not already added.

```
"bootstrap": "~3.3.6",
"bootstrap-treeview": "~1.2.0",
"font-awesome": "~4.5.0"
```

### Overriding configs in dependant project

Create a util object in `utils` directory using `ember generate util <object name>` and override it as follows:

```javascript
import ViewerConfig from 'hdfs-directory-viewer/utils/viewer-config';

export default ViewerConfig.extend({
  showOnlyDirectories: true,

  expandIcon: 'fa fa-chevron-right',
  collapseIcon: 'fa fa-chevron-down',

  listDirectoryUrl(pathParams) {
    return `/api/v1/views/FILES/versions/1.0.0/instances/files/resources/files/fileops/listdir?${pathParams}`;
  }
});
```

All the functions and attributes in `hdfs-directory-viewer/utils/viewer-config` can be overriden

### Passing the object to the view template

```javascript
import Ember from 'ember';
import MyViewerConfig from '../utils/my-viewer-config';

export default Ember.Controller.extend({
  config: MyViewerConfig.create(),
  actions: {
    viewerError: function() {
      console.log("Failed to fetch the content!!!");
    },
    viewerSelectedPath: function(data) {
      console.log(`User selected: path: ${data.path}, isDirectory: ${data.isDirectory}`);
    }
  }
});
```

```html
...
<div class="directory-viewer-wrap">
  {{directory-viewer
    config=config
    errorAction="viewerError"
    pathSelectAction="viewerSelectedPath"
  }}
</div>
...
```
