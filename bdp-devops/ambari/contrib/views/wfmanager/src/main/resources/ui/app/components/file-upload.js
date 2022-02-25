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
import EmberUploader from 'ember-uploader';
export default EmberUploader.FileField.extend({
  filesDidChange: function(files) {
    const uploader = EmberUploader.Uploader.create({
      url: `${Ember.ENV.FILE_API_URL}/upload`,
      method: 'PUT',
      ajaxSettings: {
        headers: {
          'X-Requested-By': 'workflow designer'
        }
      }
    });
    uploader.on('progress', e => {
      this.sendAction("uploadProgress",e);
    });
    uploader.on('didUpload', (e) => {
      this.sendAction("uploadSuccess", e);
    });
    uploader.on('didError', (jqXHR, textStatus, errorThrown) => {
      var message = jqXHR.responseJSON.message.substr(0, jqXHR.responseJSON.message.indexOf("\n"));
      if(message.indexOf("already exists") > 0){
        message = `File ${message.substr(0, message.indexOf("for"))} exists`;
      }else if(message.indexOf('Permission denied') >= 0){
        message = `Permission Denied.`;
      }
      this.sendAction("uploadFailure", message, errorThrown);
    });
    if (!Ember.isEmpty(files)) {
      var path = Ember.isEmpty(this.get('selectedPath')) ? '/' : this.get('selectedPath');
      uploader.upload(files[0], { path: path});
    }
  }
});
