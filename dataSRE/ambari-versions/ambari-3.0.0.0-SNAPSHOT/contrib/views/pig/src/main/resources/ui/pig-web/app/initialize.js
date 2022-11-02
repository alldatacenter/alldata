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

window.App = require('app');

App.ApplicationAdapter = DS.RESTAdapter.extend({
  init: function() {
    Ember.$.ajaxSetup({
      cache: false
    })
  },
  namespace: App.getNamespaceUrl(),
  headers: {
   'X-Requested-By': 'ambari'
  }
});

App.FileAdapter = App.ApplicationAdapter.extend({
  pathForType: function() {
    return 'resources/file';
  }
});

App.JobAdapter = App.ApplicationAdapter.extend({
  deleteRecord: function (store, type, record)  {
    var id = record.get('id');
    return this.ajax(this.buildURL(type.typeKey, id)+ '?remove=true', "DELETE");
  }
});

App.JobSerializer = DS.RESTSerializer.extend({
  normalizeHash: {
    jobs: function(hash) {
      delete hash.inProgress;
      return hash;
    },
    job: function(hash) {
      delete hash.inProgress;
      return hash;
    }
  }
});

App.FileSerializer = DS.RESTSerializer.extend({
  primaryKey:'filePath'
});

App.IsodateTransform = DS.Transform.extend({
  deserialize: function (serialized) {
    if (serialized) {
      return moment.unix(serialized).toDate();
    }
    return serialized;
  },
  serialize: function (deserialized) {
    if (deserialized) {
      return moment(deserialized).format('x');
    }
    return deserialized;
  }
});

App.ScriptdateTransform = DS.Transform.extend({
  deserialize: function (serialized) {
    if (serialized) {
      return moment(serialized).toDate();
    }
    return serialized;
  },
  serialize: function (deserialized) {
    if (deserialized) {
      return moment(deserialized).utc().format('YYYY-MM-DDTHH:mm:ss') + 'Z';
    }
    return deserialized;
  }
});

App.FileSaver = Ember.Object.extend({
  save: function(fileContents, mimeType, filename) {
    window.saveAs(new Blob([fileContents], {type: mimeType}), filename);
  }
});

App.register('lib:fileSaver', App.FileSaver);



Ember.Handlebars.registerBoundHelper('showDate', function(date,format) {
  return moment(date).format(format);
});

Em.TextField.reopen(Em.I18n.TranslateableAttributes);

require('translations');
require('router');


// mixins
require("mixins/fileHandler");
require("mixins/pagination");
require("mixins/routeError");

//routes
require("routes/pig");
require("routes/pigHistory");
require("routes/pigScripts");
require("routes/pigUdfs");
require("routes/script");
require("routes/scriptEdit");
require("routes/scriptHistory");
require("routes/scriptJob");
require("routes/splash");

//models
require("models/file");
require("models/pig_job");
require("models/pig_script");
require("models/udf");

//views
require("views/pig");
require("views/pig/alert");
require("views/pig/history");
require("views/pig/loading");
require("views/pig/scripts");
require("views/pig/udfs");
require("views/script/edit");
require("views/script/job");

//controllers
require("controllers/errorLog");
require("controllers/modal/confirmAway");
require("controllers/modal/confirmDelete");
require("controllers/modal/deleteJob");
require("controllers/modal/deleteUdf");
require("controllers/modal/createScript");
require("controllers/modal/createUdf");
require("controllers/modal/gotoCopy");
require("controllers/modal/logDownload");
require("controllers/modal/pigModal");
require("controllers/modal/resultsDownload");
require("controllers/page");
require("controllers/pig");
require("controllers/pigAlert");
require("controllers/pigHistory");
require("controllers/pigScripts");
require("controllers/pigUdfs");
require("controllers/script");
require("controllers/scriptEdit");
require("controllers/scriptHistory");
require("controllers/scriptJob");
require("controllers/splash");

//templates
require("templates/application");
require("templates/components/pigHelper");
require("templates/components/scriptListRow");
require("templates/loading");
require("templates/modal/confirmAway");
require("templates/modal/confirmDelete");
require("templates/modal/createScript");
require("templates/modal/deleteJob");
require("templates/modal/deleteUdf");
require("templates/modal/createUdf");
require("templates/modal/gotoCopy");
require("templates/modal/logDownload");
require("templates/modal/modalLayout");
require("templates/modal/resultsDownload");
require("templates/partials/alert-content");
require("templates/partials/paginationControls");
require("templates/pig");
require("templates/pig/alert");
require("templates/pig/errorLog");
require("templates/pig/history");
require("templates/pig/loading");
require("templates/pig/scripts");
require("templates/pig/udfs");
require("templates/script");
require("templates/script/edit");
require("templates/script/history");
require("templates/script/job");
require("templates/splash");
require('templates/error');

//components
require("components/codeMirror");
require("components/helpers-data");
require("components/jobProgress");
require("components/pigHelper");
require("components/scriptListRow");
require("components/tabControl");
require("components/pathInput");
require("components/highlightErrors");
