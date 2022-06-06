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
var express = require('express');

var app = express();

var API = '/api/v1';

app.use(function(req, res, next) {
	res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Headers", "X-Requested-With");
  next();
});

app.get(API + '/users', function(req, res) {
	res.json({
		"href" : "http://server:8080/api/v1/users/",
		"items" : [
			{
				"href" : "http://server:8080/api/v1/users/admin",
				"Users" : {
					"user_name" : "adminx",
					"ldap_user": false,
					"active_user": true
				}
			},
			{
				"href" : "http://server:8080/api/v1/users/Joe",
				"Users" : {
					"user_name": "Joe",
					"ldap_user": true,
					"active_user": false
				}
			}
		]
	});
});


app.get(API + '/users/:id', function(req, res) {
	res.json({
		"href":"http://<server>:8080/api/v1/users/admin",
		"Users":{
			"user_name":"admin",
			"user_groups": ["sysadmins", "hadoopadmins"]
		}
	});
});

app.listen(3000, function() {
	console.log('Listening on port 3000');
});