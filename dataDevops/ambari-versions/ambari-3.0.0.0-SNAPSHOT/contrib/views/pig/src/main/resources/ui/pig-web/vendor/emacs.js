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

(function() {
  String.prototype.regexIndexOf = function(regex, startpos) {
    var indexOf = this.substring(startpos || 0).search(regex);
    return (indexOf >= 0) ? (indexOf + (startpos || 0)) : indexOf;
  }

  function regFindPosition(curLine){
    var pos= curLine.regexIndexOf(/\%\w*\%/);
    var posArr=[];
    while(pos > -1) {
      posArr.push(pos);
      pos = curLine.regexIndexOf(/\%\w*\%/,pos+1);
    }
    return posArr;
  }

  CodeMirror.keyMap.emacs = {
    fallthrough: ["basic", "default"],
    "Tab": function(cm) {
      var cursor = cm.getCursor();
      var curLine = cm.getLine(cursor.line);
      var vArr = regFindPosition(curLine);
      var paramRanges = [];

      for (var i = 0; i < vArr.length; i++) {
        closeparam = curLine.regexIndexOf(/\%/, vArr[i]+1);
        paramRanges.push({s:vArr[i],e:closeparam});
      };

      if(vArr.length>1){
        if (cursor.ch < paramRanges[0].s) {
          CodeMirror.commands.defaultTab(cm);
        } else {
          var thisrange, nextrange;
          for (var i = 0; i < paramRanges.length; i++) {
            thisrange = paramRanges[i];
            nextrange = paramRanges[i+1] || paramRanges[0];
            if (cursor.ch > thisrange.s && (cursor.ch < nextrange.s || nextrange.s < thisrange.s)){
              cm.setSelection({line:cursor.line, ch:nextrange.s},{line:cursor.line, ch:nextrange.e+1});
            } else if (cursor.ch == thisrange.s){
              cm.setSelection({line:cursor.line, ch:thisrange.s},{line:cursor.line, ch:thisrange.e+1});
            }
          }
        }
      } else {
        CodeMirror.commands.defaultTab(cm);
      }
    }
  };
})();
