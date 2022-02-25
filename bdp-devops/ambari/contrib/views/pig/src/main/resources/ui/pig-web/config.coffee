#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#  
#      http://www.apache.org/licenses/LICENSE-2.0
#   
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

exports.config = 

  watcher:
    usePolling: true

  fileListInterval: 512

  files: 
    javascripts: 
      defaultExtension: 'js'
      joinTo: 
        'static/javascripts/app.js': /^app/
        'static/javascripts/vendor.js': /^bower_components|vendor/,
        'static/javascripts/test.js': /^test(\/|\\)(?!vendor)/
      order:
        before: [
          'bower_components/moment/moment.js',
          'bower_components/jquery/dist/jquery.js',
          'vendor/jquery-ui/core.js',
          'vendor/jquery-ui/widget.js',
          'vendor/jquery-ui/mouse.js',
          'vendor/jquery-ui/position.js',
        ]

    stylesheets:
      defaultExtension: 'css'
      joinTo: 'static/stylesheets/app.css'

    templates:
      precompile: true
      root: 'templates'
      defaultExtension: 'hbs'
      joinTo: 'static/javascripts/app.js' : /^app/
      paths:
        jquery: 'bower_components/jquery/dist/jquery.js'
        handlebars: 'bower_components/handlebars/handlebars.js'
        ember: 'bower_components/ember/ember.js'

  modules:
    addSourceURLs: true

  overrides:
    development:
      fileListInterval: 64
      paths:
        public: '/usr/lib/ambari-server/web/views-debug/PIG/0.1.0/MyPig/'
