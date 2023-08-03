/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

//"use strict";

// Initializes client-side handling of cross-site request forgery (CSRF)
// protection by figuring out the custom HTTP headers that need to be sent in
// requests and which HTTP methods are ignored because they do not require CSRF
// protection.
define(function(require) {
	"use strict";
	require('jquery');
	var restCsrfCustomHeader = null;
	var restCsrfMethodsToIgnore = null;
  var restCsrfToken = null;

	if(!window.location.origin){
		window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
	}
    // Proxy URL for Ranger UI doesn't work without trailing slash so add slash
    var pathName = /\/[\w-]+.(jsp|html)/;
    if(!pathName.test(window.location.pathname) && window.location.pathname.slice(-1) !== "/"){
        history.pushState({}, null, window.location.pathname + "/");
    }
	var baseUrl = window.location.origin + window.location.pathname.substr(0, window.location.pathname.lastIndexOf("/"));
	
	if(baseUrl.slice(-1) == "/") {
	  baseUrl = baseUrl.slice(0,-1);
	}
	var url = baseUrl + "/service/plugins/csrfconf";

  $.ajax({'url': url, 'dataType': 'json', 'async': false}).done(
    function(data) {
    	function getTrimmedStringArrayValue(element) {
    		var str = element, array = [];
    		if (str) {
    			var splitStr = str.split(',');
    			for (var i = 0; i < splitStr.length; i++) {
    				array.push(splitStr[i].trim());
    			}
    		}
    		return array;
      }

      // Get all relevant configuration properties.
      var $xml = $(data);
      var csrfEnabled = false;
      var header = null;
      var headerToken = null;
      var methods = [];
      $xml.each(function(indx,element){
    	  if(element['ranger.rest-csrf.enabled']) {
    		  var str = "" + element['ranger.rest-csrf.enabled'];
    		  csrfEnabled = (str.toLowerCase() == 'true');
    	  }
    	  if (element['ranger.rest-csrf.custom-header']) {
    		  header = element['ranger.rest-csrf.custom-header'].trim();
    	  }
    	  if (element['ranger.rest-csrf.methods-to-ignore']) {
    		  methods = getTrimmedStringArrayValue(element['ranger.rest-csrf.methods-to-ignore']);
    	  }
        if (element['_csrfToken']) {
          headerToken = element['_csrfToken'];
        }
      });

      // If enabled, set up all subsequent AJAX calls with a pre-send callback
      // that adds the custom headers if necessary.
      if (csrfEnabled) {
        restCsrfCustomHeader = header;
        restCsrfMethodsToIgnore = {};
        restCsrfToken = headerToken;
        localStorage.setItem("csrfToken" , headerToken)
        methods.map(function(method) { restCsrfMethodsToIgnore[method] = true; });
        $.ajaxSetup({
          beforeSend: addRestCsrfCustomHeader
        });
      }
    });

  // Adds custom headers to request if necessary.  This is done only for WebHDFS
  // URLs, and only if it's not an ignored method.
  function addRestCsrfCustomHeader(xhr, settings) {
//    if (settings.url == null || !settings.url.startsWith('/webhdfs/')) {
	  if (settings.url == null ) {
      return;
    }
    var method = settings.type;
    if (restCsrfCustomHeader != null && !restCsrfMethodsToIgnore[method]) {
      // The value of the header is unimportant.  Only its presence matters.
      if (restCsrfToken != null) {
        xhr.setRequestHeader(restCsrfCustomHeader, restCsrfToken);
      } else {
        xhr.setRequestHeader(restCsrfCustomHeader, '""');
      }
    }
  }
});