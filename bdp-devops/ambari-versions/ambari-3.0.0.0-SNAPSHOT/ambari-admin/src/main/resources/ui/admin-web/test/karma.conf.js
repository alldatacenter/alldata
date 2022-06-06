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

module.exports = function(config){
  config.set({

    basePath : '../',

    files : [
      'app/bower_components/jquery/dist/jquery.js',
      'app/bower_components/angular/angular.js',
      'app/bower_components/angular-animate/angular-animate.js',
      'app/bower_components/angular-bootstrap/ui-bootstrap.js',
      'app/bower_components/angular-bootstrap-toggle-switch/angular-toggle-switch.js',
      'app/bower_components/angular-route/angular-route.js',
      'app/bower_components/angular-translate/angular-translate.js',
      'app/bower_components/underscore/underscore.js',
      'app/bower_components/restangular/dist/restangular.js',
      'app/bower_components/mocha/mocha.js',
      'app/bower_components/chai/chai.js',
      'app/bower_components/sinon/lib/sinon.js',
      'app/bower_components/angular-mocks/angular-mocks.js',
      'app/scripts/**/*.js',
      'test/unit/**/*.js',
      'app/views/directives/*.html'
    ],

    autoWatch : true,

    frameworks: ['jasmine'],

    browsers: ['PhantomJS'],

    plugins : [
            'karma-jasmine',
            'karma-phantomjs-launcher',
            'karma-ng-html2js-preprocessor'
            ],

    junitReporter : {
      outputFile: 'test_out/unit.xml',
      suite: 'unit'
    },

    preprocessors: {
      'app/views/directives/*.html': ['ng-html2js']
    },
    ngHtml2JsPreprocessor: {
      'stripPrefix': 'app/'
    }

  });
};
