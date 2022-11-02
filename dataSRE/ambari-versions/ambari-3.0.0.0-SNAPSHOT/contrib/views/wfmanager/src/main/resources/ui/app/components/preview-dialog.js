
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
import CommonUtils from '../utils/common-utils';

export default Ember.Component.extend({
  decodedXml : Ember.computed('previewXml', function(){
    return CommonUtils.decodeXml(this.get('previewXml'));
  }),
  elementsInserted :function(){
    this.$('#previewModal').modal({
      backdrop: 'static',
      keyboard: false
    });
    this.$('#previewModal').modal('show');
  	var self = this;
	  this.$('#previewModal').on('shown.bs.modal', function (e) {
	    self.$('.CodeMirror')[0].CodeMirror.refresh();
	  });
    this.$('#previewModal').on('hidden.bs.modal', function () {
      this.sendAction("closePreview");
	  }.bind(this));
  }.on('didInsertElement')
});
