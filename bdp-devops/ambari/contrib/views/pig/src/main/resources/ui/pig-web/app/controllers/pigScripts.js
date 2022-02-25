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

var App = require('app');

App.PigScriptsController = Em.ArrayController.extend(App.Pagination,{
  sortProperties: ['dateCreatedUnix'],
  sortAscending: false,
  needs:['pig'],
  actions:{
    createScript:function () {
      return this.send('openModal', 'createScript', this.store.createRecord('script'));
    },
    confirmcreate:function (script,filePath) {
      if (filePath) {
        return Em.RSVP.Promise.all([Em.RSVP.resolve(script), this.getOrCreateFile(filePath,script)])
          .then(this.setFileAndSave.bind(this), Em.run.bind(this,'createScriptError', script));
      } else {
        script.save().then(this.createScriptSuccess.bind(this), Em.run.bind(this,'createScriptError',script));
      }
    },
    deletescript:function (script) {
      this.get('controllers.pig').send('deletescript',script);
    },
    copyScript:function (script) {
      this.get('controllers.pig').send('copyScript',script);
    }
  },

  getOrCreateFile: function (path,script) {
    var store = this.get('store');
    var createNewFilePath = function(path,title){
       var cleanedTitle = title.replace("[^a-zA-Z0-9 ]+", "").replace(" ", "_").toLowerCase();
       var formattedDate = moment().format("YYYY-MM-DD_hh-mm-ss_SSSS");
       var finalScriptName = cleanedTitle + "-" + formattedDate + ".pig";
      return path + "/" + finalScriptName;
    };
    return new Em.RSVP.Promise(function (resolve, reject) {
      store.find('file',path).then(function (file) {
          resolve(file);
        }, function (error) {
          if (error.status === 404) {
            store.recordForId('file', path).unloadRecord();
            var newPath = createNewFilePath(path,script.get('title'));
            var newFile = store.createRecord('file',{
              id:newPath,
              fileContent:''
            });

            newFile.save().then(function (file) {
              resolve(file);
            }, function (error) {
              reject(error);
            });
          } else {
            reject(error);
          }
        });
    });
  },



  setFileAndSave: function (data) {
    var script = data.objectAt(0),
        file = data.objectAt(1);

    var pr = script.set('pigScript',file).save();

    return pr.then(this.createScriptSuccess.bind(this),Em.run.bind(this,'createScriptError',script));
  },

  createScriptSuccess:function (script) {
    this.transitionToRoute('script.edit',script);
    this.send('showAlert', {
      'message': Em.I18n.t('scripts.alert.script_created', { 'title' : script.get('title') } ),
      'status': 'success'
    });
  },

  createScriptError: function (script, error) {
    script.deleteRecord();
    this.send('showAlert', {
      'message': Em.I18n.t('scripts.alert.create_failed'),
      'status': 'error',
      'trace': (error.responseJSON) ? error.responseJSON.trace : error.message
    });
  },

  jobs:function () {
    return this.store.find('job');
  }.property()
});
