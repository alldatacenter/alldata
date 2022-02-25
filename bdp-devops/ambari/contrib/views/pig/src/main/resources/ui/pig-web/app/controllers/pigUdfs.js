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

App.PigUdfsController = Em.ArrayController.extend(App.Pagination,{
  actions:{
    createUdfModal:function () {
      return this.send('openModal','createUdf',this.store.createRecord('udf'));
    },
    createUdf:function (udf) {
      return udf.save().then(this.onCreateSuccess.bind(this),this.onCreateFail.bind(this));
    },
    deleteUdfModal:function(udf){
      return this.send('openModal','deleteUdf',udf);
    },
    deleteUdf:function(udf){
      udf.deleteRecord();
      return udf.save().then(this.onDeleteSuccess.bind(this),this.onDeleteFail.bind(this));
    }
  },
  onCreateSuccess:function (model) {
    this.send('showAlert', {
      message: Em.I18n.t('udfs.alert.udf_created',{name : model.get('name')}),
      status:'success'
    });
  },
  onCreateFail:function (error) {
    var trace = (error && error.responseJSON.trace)?error.responseJSON.trace:null;
    this.send('showAlert', {
      message:Em.I18n.t('udfs.alert.create_failed'),
      status:'error',
      trace:trace
    });
  },
  onDeleteSuccess: function(model){
    this.send('showAlert', {
      message: Em.I18n.t('udfs.alert.udf_deleted',{name : model.get('name')}),
      status:'success'
    });
  },
  onDeleteFail:function(error){
    var trace = (error && error.responseJSON.trace)?error.responseJSON.trace:null;
    this.send('showAlert', {
      message: Em.I18n.t('udfs.alert.delete_failed'),
      status:'error',
      trace:trace
    });
  }
});
