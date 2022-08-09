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


module.exports = {
	200: function () {
		console.log("Status code 200: Success.");
  },
  202: function () {
    console.log("Status code 202: Success for creation.");
  },
	400: function () {
		console.log("Error code 400: Bad Request.");
	},
	401: function () {
		console.log("Error code 401: Unauthorized.");
	},
	402: function () {
		console.log("Error code 402: Payment Required.");
	},
	403: function () {
		console.log("Error code 403: Forbidden.");
    App.router.logOff();
	},
	404: function () {
		console.log("Error code 404: URI not found.");
	},
	500: function () {
		console.log("Error code 500: Internal Error on server side.");
	},
	501: function () {
		console.log("Error code 501: Not implemented yet.");
	},
	502: function () {
		console.log("Error code 502: Services temporarily overloaded.");
	},
	503: function () {
		console.log("Error code 503: Gateway timeout.");
	}
};
