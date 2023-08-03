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

 define([
	'backbone',
	'App',
	'RegionManager',
	'routers/Router',
	'controllers/Controller',
	'controllers/NController',
	'modules/XAOverrides',
	'modules/RestCsrf',
	'utils/XAUtils',
	'hbs!tmpl/common/loading_tmpl',
	'backbone-fetch-cache'
],
function ( Backbone, App, RegionManager, AppRouter, AppController, NewAppController, XAOverrides,RestCSRF, XAUtils, loadingHTML ) {
    'use strict';

	function startApp(){
		if(localStorage.getItem('setOldUI') == null) {
			localStorage.setItem('setOldUI', true)
		}
		var controller;
		if(localStorage.getItem('setOldUI') == "true") {
			controller = new AppController()
		} else {
			controller = new NewAppController()
		}
		//deny some routes access for normal users
		controller = XAUtils.filterAllowedActions(controller);
		App.appRouter = new AppRouter({
			controller: controller
		});
		App.appRouter.on('beforeroute', function(event) {
			if(!window._preventNavigation)
				$(App.rContent.$el).html(loadingHTML);
		});
		// Start Marionette Application in desktop mode (default)
		Backbone.fetchCache._cache = {};
		App.start();
	}

	//export an object so that we can assert that at least RequireJS loading works or not
	var mainModule={
		name: 'Main',
		startApp : startApp
	};

	try{
		window.Backbone = Backbone;
		startApp();
		mainModule.success = true;
	}
	catch(e){
		mainModule.success = false;
		mainModule.error = e;
	}
	return mainModule;
});
