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
import FilePicker from 'ember-cli-file-picker/components/file-picker';

export default FilePicker.extend({
  filesAreValid: function(files) {
   // do something with the files and add errors:
   // this.get('errors').addObject('wrong file type');
   //return false;
    return true;
  },
  click : function(event){
      this.$('.file-picker__input').val(null);//this is done so that opening the same file would call the callbacks.s
      this._super(event);
  }
});
