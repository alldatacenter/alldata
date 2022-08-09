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
	saveWorkflow(url, jobxml){
		var deferred = Ember.RSVP.defer();
	    Ember.$.ajax({
	      url: url,
	      method: "POST",
	      dataType: "text",
	      contentType: "text/plain;charset=utf-8",
	      beforeSend: function(request) {
	        request.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
	        request.setRequestHeader("X-Requested-By", "workflow-designer");
	      },
	      data: jobxml,
	      success: function(response) {
	      	deferred.resolve(response);
	      }.bind(this),
	      error: function(response) {
	      	deferred.reject(response);
	      }.bind(this)
	    });
        return deferred;
	}
});
