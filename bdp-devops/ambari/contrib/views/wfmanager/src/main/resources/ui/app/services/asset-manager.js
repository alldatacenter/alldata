/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/
import Ember from 'ember';

export default Ember.Service.extend({
  saveAsset(assetConfig, wfDynamicProps) {
    var url = Ember.ENV.API_URL + "/assets";
    wfDynamicProps.forEach(function(property, index){
      url = url + ((index === 0) ? "?" : "&") + "config." + property.name + "=" + property.value;
    });
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: "POST",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "workflow-designer");
      },
      data: JSON.stringify(assetConfig),
    }).done(function(data){
      deferred.resolve(data);
    }).fail(function(data){
      deferred.reject(data);
    });
    return deferred;
  },
  fetchAssets() {
    var url = Ember.ENV.API_URL + "/assets";
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: "GET",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "workflow-designer");
      }
    }).done(function(data){
      deferred.resolve(data);
    }).fail(function(data){
      deferred.reject(data);
    });
    return deferred;
  },
  fetchMyAssets() {
    var url = Ember.ENV.API_URL + "/assets/mine";
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: "GET",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "workflow-designer");
      }
    }).done(function(data){
      deferred.resolve(data);
    }).fail(function(data){
      deferred.reject(data);
    });
    return deferred;
  },
  importAssetDefinition(assetDefinitionId) {
    var url = Ember.ENV.API_URL + "/assets/" + assetDefinitionId;
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: "GET",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "workflow-designer");
      }
    }).done(function(data){
      deferred.resolve(data);
    }).fail(function(data){
      deferred.reject(data);
    });
    return deferred;
  },
  deleteAsset(assetId) {
    var url = Ember.ENV.API_URL + "/assets/" + assetId;
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: "DELETE",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "workflow-designer");
      }
    }).done(function(data){
      deferred.resolve(data);
    }).fail(function(data){
      deferred.reject(data);
    });
    return deferred;
  },
  publishAsset(filePath, actionNodeXml, wfDynamicProps) {
    var url = Ember.ENV.API_URL + "/publishAsset?uploadPath="+filePath;
    wfDynamicProps.forEach(function(property){
      url = url + "&config." + property.name + "=" + property.value;
    });
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: "POST",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "Ambari");
      },
      data: actionNodeXml,
    }).done(function(data){
      deferred.resolve(data);
    }).fail(function(data){
      deferred.reject(data);
    });
    return deferred;
  },
  assetNameAvailable(assetName) {
    var url = Ember.ENV.API_URL + "/assets/assetNameAvailable?name=" + assetName;
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: "GET",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "workflow-designer");
      }
    }).done(function(data){
      deferred.resolve(data);
    }).fail(function(data){
      deferred.reject(data);
    });
    return deferred;
  }
});
