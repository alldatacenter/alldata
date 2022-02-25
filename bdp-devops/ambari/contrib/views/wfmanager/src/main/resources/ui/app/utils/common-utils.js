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
import Constants from '../utils/constants';
export default Ember.Object.create({
  extractSchemaVersion(xmlns){
    return xmlns.substring(xmlns.lastIndexOf(":")+1);
  },
  extractSchema(xmlns){
    return xmlns.substr(0,xmlns.lastIndexOf(":"));
  },
  setTestContext(context){
    window.flowDesignerTestContext=context;
  },
  isSupportedAction(actionType){
    return Constants.actions.findBy('name', actionType)? true : false;
  },
  decodeXml(xml){
    return xml && xml.length > 0 ? xml.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '\"').replace(/&apos;/g, '\'') : xml;
  },

  toArray(map){
    var entries = map.entries();
    var mapArray = [];
    while(true){
      var entry = entries.next()
      if(entry.done){
        break;
      }
      mapArray.push(entry.value);
    }
    return mapArray;
  },

  startsWith (string,searchString, position){
    position = position || 0;
    return string.substr(position, searchString.length) === searchString;
  }
});
