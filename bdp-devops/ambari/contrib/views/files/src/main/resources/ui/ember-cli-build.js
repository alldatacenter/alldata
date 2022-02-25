/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*jshint node:true*/
/* global require, module */
var EmberApp = require('ember-cli/lib/broccoli/ember-app');

module.exports = function(defaults) {
  var app = new EmberApp(defaults, {
    // Add options here
    lessOptions: {
      paths: [
        'bower_components/bootstrap/less'
      ]
    },
    fingerprint: {
      enabled: false
    }
  });

  // Use `app.import` to add additional libraries to the generated
  // output files.
  //
  // If you need to use different assets in different
  // environments, specify an object as the first parameter. That
  // object's keys should be the environment name and the values
  // should be the asset to use in that environment.
  //
  // If the library that you are including contains AMD or ES6
  // modules that you would like to import into your application
  // please specify an object with the list of modules as keys
  // along with the exports of each module as its value.

  app.import(app.bowerDirectory + '/bootstrap/dist/js/bootstrap.js');
  app.import(app.bowerDirectory + '/bootstrap-treeview/src/js/bootstrap-treeview.js');
  app.import(app.bowerDirectory + '/bootstrap-treeview/dist/bootstrap-treeview.min.css');

  /*
  app.import(app.bowerDirectory + '/bootstrap/fonts/glyphicons-halflings-regular.eot', {
    destDir: 'fonts'
  });
  app.import(app.bowerDirectory + '/bootstrap/fonts/glyphicons-halflings-regular.svg', {
    destDir: 'fonts'
  });
  app.import(app.bowerDirectory + '/bootstrap/fonts/glyphicons-halflings-regular.ttf', {
    destDir: 'fonts'
  });
  app.import(app.bowerDirectory + '/bootstrap/fonts/glyphicons-halflings-regular.woff', {
    destDir: 'fonts'
  });*/

  app.import('bower_components/moment/moment.js');
  app.import('bower_components/ember-uploader/dist/ember-uploader.named-amd.js');

  return app.toTree();
};
