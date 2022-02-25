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
var pigHelpers = require('./helpers-data');

App.PigHelperComponent = Em.Component.extend({
  layoutName: 'components/pigHelper',
  helpers: pigHelpers,
  editor: null,
  actions:{
    putToEditor:function (helper) {
      var editor = this.get('editor');
      var cursor = editor.getCursor();
      var pos = this.findPosition(helper);

      editor.replaceRange(helper, cursor, cursor);
      editor.focus();

      if (pos.length>1) {
        editor.setSelection(
          {line:cursor.line, ch:cursor.ch + pos[0]},
          {line:cursor.line, ch:cursor.ch + pos[1]+1}
        );
      }

      return false;
    }
  },
  findPosition: function (curLine){
    var pos = curLine.indexOf("%");
    var posArr = [];
    while(pos > -1) {
      posArr.push(pos);
      pos = curLine.indexOf("%", pos+1);
    }
    return posArr;
  }
});
