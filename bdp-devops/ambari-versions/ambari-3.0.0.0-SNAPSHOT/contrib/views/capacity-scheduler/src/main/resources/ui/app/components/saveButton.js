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

App.SaveButtonComponent = Em.Component.extend({
  canNotSave:false,
  needSave:true,
  refresh:false,
  restart:false,

  tagName:'button',
  layout:Em.Handlebars.compile('{{{icon}}} Actions <span class="caret"></span> '),
  attributeBindings: ['data-toggle'],
  classNames:['btn','dropdown-toggle'],
  classNameBindings:['color','disabled'],
  icon:function () {
    var tmpl = '<i class="fa fa-fw fa-%@"></i>',icon;
    if (this.get('refresh')) {
      icon = 'refresh';
    } else if (this.get('restart')) {
      icon = 'cogs';
    }
    return (icon)?tmpl.fmt(icon):'';
  }.property('refresh','restart'),
  color:function () {
    var className = 'btn-default';
    if (this.get('needSave')) {
      className = 'btn-warning';
    }
    if (this.get('canNotSave')) {
      className = 'btn-danger';
    }
    return className;
  }.property('canNotSave','needSave'),
  disabled:function () {
    return this.get('canNotSave');
  }.property('canNotSave','needSave')
});
