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

module.exports = function(config) {
  config.set({

    // base path, that will be used to resolve files and exclude
    basePath: '',

    plugins: [
      'karma-mocha',
      'karma-chai',
      'karma-sinon',
      'karma-phantomjs-launcher',
      'karma-coverage',
      'karma-ember-precompiler-brunch',
      'karma-commonjs-require',
      'karma-babel-preprocessor'
    ],

    // frameworks to use
    frameworks: ['mocha', 'chai', 'sinon'],


    // list of files / patterns to load in the browser
    files: [

//      'public/javascripts/vendor.js',
//      'public/javascripts/app.js',
//      'public/test/karma_setup.js',
//      'public/test/javascripts/test.js',
//      'public/test/tests.js'

      'node_modules/karma-commonjs-require/node_modules/commonjs-require-definition/require.js',
      'vendor/scripts/console-helper.js',
      'vendor/scripts/jquery-1.9.1.js',
      'vendor/scripts/handlebars-1.0.0.beta.6.js',
      'vendor/scripts/ember-latest.js',
      'vendor/scripts/ember-data-latest.js',
      'vendor/scripts/ember-i18n-1.4.1.js',
      'vendor/scripts/bootstrap.js',
      'vendor/scripts/bootstrap-combobox.js',
      'vendor/scripts/bootstrap-switch.min.js',
      'vendor/scripts/d3.v2.js',
      'vendor/scripts/cubism.v1.js',
      'vendor/scripts/jquery.ui.core.js',
      'vendor/scripts/jquery.ui.position.js',
      'vendor/scripts/jquery.ui.widget.js',
      'vendor/scripts/jquery.ui.autocomplete.js',
      'vendor/scripts/jquery.ui.mouse.js',
      'vendor/scripts/jquery.ui.datepicker.js',
      'vendor/scripts/jquery-ui-timepicker-addon.js',
      'vendor/scripts/jquery.ui.slider.js',
      'vendor/scripts/jquery.ui.sortable.js',
      'vendor/scripts/jquery.ui.custom-effects.js',
      'vendor/scripts/jquery.timeago.js',
      'vendor/scripts/jquery.ajax-retry.js',
      'vendor/scripts/difflib.js',
      'vendor/scripts/diffview.js',
      'vendor/scripts/underscore.js',
      'vendor/scripts/backbone.js',
      'vendor/scripts/visualsearch.js',
      'vendor/scripts/workflow_visualization.js',
      'vendor/scripts/rickshaw.js',
      'vendor/scripts/spin.js',
      'vendor/scripts/jquery.flexibleArea.js',
      'vendor/scripts/FileSaver.js',
      'vendor/scripts/Blob.js',
      'vendor/scripts/moment.min.js',
      'vendor/scripts/moment-timezone-with-data-2010-2020.js',
      'vendor/**/*.js',
      'app/templates/**/*.hbs',
      'app!(assets)/**/!(karma_setup|tests).js',
      'app/assets/test/karma_setup.js',
      {
        pattern: 'app/assets/data/**',
        served: true,
        included: false,
        watched: true
      },
      'test/**/*.js',
      'app/assets/test/tests.js'
    ],

    emberPrecompilerBrunchPreprocessor: {
      jqueryPath: 'vendor/scripts/jquery-1.9.1.js',
      emberPath: 'vendor/scripts/ember-latest.js',
      handlebarsPath: 'vendor/scripts/handlebars-1.0.0.beta.6.js'
    },

    commonRequirePreprocessor: {
      appDir: 'app'
    },

    coverageReporter: {
      type: 'html',
      dir: 'public/coverage/'
    },

    preprocessors: {
      '!(vendor|node_modules|test)/**/!(karma_setup|tests).js': 'coverage',
      'app/templates/**/*.hbs': ['ember-precompiler-brunch', 'common-require'],
      'app!(assets)/**/!(karma_setup|tests).js': ['common-require', 'babel'],
      'test/**/*.js': ['common-require']
    },

    babelPreprocessor: {
      options: {
        presets: ['es2015']
      },
      filename: function (file) {
        return file.originalPath.replace(/\.js$/, '.js');
      },
      sourceFileName: function (file) {
        return file.originalPath;
      }
    },

    // list of files to exclude
    exclude: [

    ],


    // test results reporter to use
    // possible values: 'dots', 'progress', 'junit', 'growl', 'coverage'
//    reporters: ['progress', 'coverage'],
    reporters: ['progress', 'coverage'],


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,


    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera (has to be installed with `npm install karma-opera-launcher`)
    // - Safari (only Mac; has to be installed with `npm install karma-safari-launcher`)
    // - PhantomJS
    // - IE (only Windows; has to be installed with `npm install karma-ie-launcher`)
    browsers: ['PhantomJS'],

    // If browser does not capture in given timeout [ms], kill it
    captureTimeout: 60000,

    browserNoActivityTimeout: 30000,

    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    singleRun: true
  });
};
