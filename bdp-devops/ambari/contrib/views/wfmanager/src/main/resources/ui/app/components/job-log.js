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

export default Ember.Component.extend({
  logFilter :"",
  logActionList : "",
  initialize : function(){
    this.$('[data-toggle="popover"]').popover();
    /*jshint multistr: true */
    var filterHelpContent = 'You can filter using search filters as opt1=val1;opt2=val1;opt3=val1. Available options are recent,start,end,loglevel,text,limit,debug\
    Visit <a href="https://oozie.apache.org/docs/4.2.0/DG_CommandLineTool.html#Filtering_the_server_logs_with_logfilter_options" target="_blank">here</a> for more details';
    this.$('#logFilterHelp [data-toggle="popover"]').attr('data-content', filterHelpContent);
    var actionListHelpContent = 'Enter list of actions in the format similiar to 1,3-4,7-40 to get logs for specific coordinator actions.';
    this.$('#actionlogHelp [data-toggle="popover"]').attr('data-content', actionListHelpContent);
  }.on('didInsertElement'),
  actions : {
    onGetJobLog (){
      var params = {};
      if(this.get('logFilter')){
        params.logFilter = this.get('logFilter');
      }
      if(this.get('logActionList')){
        params.logActionList = this.get('logActionList');
      }
      this.sendAction('getJobLog', params);
    }
  }
});
