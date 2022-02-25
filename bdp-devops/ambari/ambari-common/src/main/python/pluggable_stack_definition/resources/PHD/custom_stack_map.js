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

/*
sign will be compared like: <clusterSelectedStackVersion> sign <stackVersionNumber> if true use this baseStackFolder
Example:
 {
 "stackName": "PHD",
 "stackVersionNumber": "3.0",
 "sign": "<",
 "baseStackFolder": "HDP2"
 }
 This rule will work for any PHD version number that is lower than 3.0 (2.9,2.8, e.t.c)
*/

module.exports = [
  {
    "stackName": "PHD",
    "stackVersionNumber": "3.3",
    "sign": "=",
    "baseStackFolder": "HDP2.3"
  },
  {
    "stackName": "PHD",
    "stackVersionNumber": "3.0",
    "sign": "=",
    "baseStackFolder": "HDP2.2"
  },
  {
    "stackName": "PHD",
    "stackVersionNumber": "3.0",
    "sign": "<",
    "baseStackFolder": "HDP2"
  }
];