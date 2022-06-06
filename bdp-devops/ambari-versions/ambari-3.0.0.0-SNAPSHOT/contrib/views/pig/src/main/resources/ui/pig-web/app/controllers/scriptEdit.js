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

App.ScriptEditController = Em.ObjectController.extend({
  needs:['script'],
  pigParams: Em.A(),
  isExec:false,
  isRenaming:false,
  titleWarn:false,
  tmpArgument:'',
  editor:null,
  fullscreen:false,

  handleRenaming:function () {
    if (this.get('content.title')) {
      this.set('titleWarn',false);
    }
  }.observes('content.title','isRenaming'),

  pigParamsMatch:function (controller) {
    var editorContent = this.get('content.pigScript.fileContent');
    if (editorContent) {
      var match_var = editorContent.match(/\%\w+\%/g);
      if (match_var) {
        var oldParams = controller.get('pigParams');
        controller.set('pigParams',[]);
        match_var.forEach(function (param) {
          var suchParams = controller.pigParams.filterProperty('param',param);
          if (suchParams.length == 0){
            var oldParam = oldParams.filterProperty('param',param);
            var oldValue = oldParam.get('firstObject.value');
            controller.pigParams.pushObject(Em.Object.create({param:param,value:oldValue,title:param.replace(/%/g,'')}));
          }
        });
      } else {
        controller.set('pigParams',[]);
      }
    } else {
      controller.set('pigParams',[]);
    }
  }.observes('content.pigScript.fileContent','content.id'),


  oldTitle:'',
  actions: {
    rename:function (opt) {
      var changedAttributes = this.get('content').changedAttributes();

      if (opt === 'ask') {
        this.setProperties({'oldTitle':this.get('content.title'),'isRenaming':true});
      }

      if (opt === 'cancel') {
        this.setProperties({'content.title':this.get('oldTitle'),'isRenaming':false,'oldTitle':''});
      }

      if (opt === this.get('content.title') && !Em.isBlank(this.get('content.title'))) {
        if (Em.isArray(changedAttributes.title)) {
          this.get('content').save().then(function () {
            this.send('showAlert', {message:Em.I18n.t('editor.title_updated'),status:'success'});
          }.bind(this));
        }
        this.setProperties({'oldTitle':'','isRenaming':false});
      }
    },
    addArgument:function (arg) {
      var settled = this.get('content.argumentsArray');
      if (!arg) {
        return false;
      }
      if (!settled.contains(arg)) {
        this.setProperties({'content.argumentsArray': settled.pushObject(arg) && settled,'tmpArgument':''});
      } else {
        this.send('showAlert', {'message': Em.I18n.t('scripts.alert.arg_present'), status:'info'});
      }
    },
    removeArgument:function (arg) {
      this.set('content.argumentsArray',this.get('content.argumentsArray').removeObject(arg));
    },
    execute: function (script, operation) {

      var isEmpltyScript =  Ember.isEmpty(script.get('pigScript').get('content').get('fileContent'));

      if( isEmpltyScript ){
        this.send('showAlert', {message:Em.I18n.t('scripts.modal.error_empty_scriptcontent'),status:'error',trace:null});
        return;
      }

      this.set('isExec',true);

      return Ember.RSVP.resolve(script.get('pigScript'))
        .then(function (file) {
          return Ember.RSVP.all([file.save(),script.save()]);
        })
        .then(function (data) {
          return this.prepareJob(operation,data);
        }.bind(this))
        .then(this.executeSuccess.bind(this), this.executeError.bind(this))
        .finally(Em.run.bind(this,this.set,'isExec',false));
    },
    toggleTez:function () {
      this.toggleProperty('executeOnTez');
    },
    fullscreen:function () {
      this.toggleProperty('fullscreen');
    }
  },

  executeSuccess:function (job) {
    this.send('showAlert', {message:Em.I18n.t('job.alert.job_started'),status:'success'});
    if (this.target.isActive('script.edit')) {
      Em.run.next(this,this.transitionToRoute,'script.job',job);
    }
  },

  executeError:function (error) {
    var trace = (error.responseJSON)?error.responseJSON.trace:null;
    this.send('showAlert', {message:Em.I18n.t('job.alert.start_filed'),status:'error',trace:trace});
  },

  executeOnTez:false,

  prepareJob:function (type, data) {
    var job, promise,
        exc = 'execute' == type,
        exp = 'explain' == type,
        chk = 'syntax_check' == type,
        file = data[0],
        script = data[1],
        pigParams = this.get('pigParams') || [],
        fileContent = file.get('fileContent'),
        args = script.get('templetonArguments'),
        title = script.get('title'),
        hasParams = pigParams.length > 0;

    pigParams.forEach(function (param) {
      var rgParam = new RegExp(param.param,'g');
      fileContent = fileContent.replace(rgParam,param.value);
    });

    if (this.get('executeOnTez') && args.indexOf('-x\ttez') < 0) {
      args = args + (args ? "\t" : "") + '-x\ttez';
    }

    job = this.store.createRecord('job', {

      /**
       * Link to script.
       * @type {String}
       */
      scriptId:script.get('id'),

      /**
       * Add '-check' argument for syntax check and remove all arguments for explain.
       * @type {String}
       */
      templetonArguments:(exc)?args:(chk)?(!args.match(/-check/g))?args+(args?"\t":"")+'-check':args:'',

      /**
       * Modify title for syntax check and operations.
       * @type {String}
       */
      title:(exc)?title:(chk)?'Syntax check: "%@"'.fmt(title):'Explain: "%@"'.fmt(title),

      /**
       * If the script has parameters, set this value to script with replaced ones.
       * And if operations is explain, set it to reference to sourceFile.
       * @type {String}
       */
      forcedContent:(exp)?'explain -script ${sourceFile}':(hasParams)?fileContent:null,

      /**
       * If other, than execute, need to set job type.
       * @type {String}
       */
      jobType:(!exc)?type:null,

      /**
       * If execute of syntax_check without params, just set App.File instance.
       * @type {App.File}
       */
      pigScript:(!exp && !hasParams)?file:null,

      /**
       * For explain job type need sourceFile ...
       * @type {String}
       */
      sourceFile:(exp && !hasParams)?file.get('id'):null,

      /**
       * ... or sourceFileContent if script has parameters.
       * @type {String}
       */
      sourceFileContent:(exp && hasParams)?fileContent:null
    });

    promise = job.save();

    Em.run.next(promise, promise.catch, Em.run.bind(job,job.deleteRecord));

    return promise;
  },

  /**
   * available UDFs
   * @return {App.Udf} promise
   */
  ufdsList:function () {
    return this.store.find('udf');
  }.property('udf')
});
