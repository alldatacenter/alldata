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
(function () {
  "use strict";

  var bodyParser = require('body-parser'),
    express = require('express'),
    mockData = require('./mockData.js'),
    server = express(),
    PORT = process.env.PORT || 11000;

  server.use('/', express.static(__dirname + '/dist'));
  server.use(bodyParser());
  server.use(function (req, res, next) {
    if (req.is('text/*')) {
      req.text = '';
      req.setEncoding('utf8');
      req.on('data', function (chunk) { req.text += chunk; });
      req.on('end', next);
    } else {
      next();
    }
  });

  server.get('/oozie/v2/jobs', function(req, res) {
    res.json(200, mockData.getJobs());
  });
  
   server.get('/oozie/v2/job/:id', function(req, res) {
    res.json(200, mockData.getJobById());
  });

  server.listen(PORT, function () {
    console.log('Dev server listening on port ' + PORT);
  });

}());
