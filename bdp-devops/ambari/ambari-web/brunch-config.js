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


module.exports.config = {
  plugins: {
    babel: {
      ignore: [
        /^(vendor|app\/data|app\/assets|test)/
      ],
      pattern: /\.(js)$/,
      plugins: ['transform-object-assign']
    },
    assetsmanager: {
      copyTo: {
        'stylesheets/fonts' : ['vendor/theme/fonts/*'],
        'api-docs' : ['api-docs/*']
      }
    }
  },
  files: {
    javascripts: {
      joinTo: {
        'javascripts/app.js': /^app/,
        'javascripts/vendor.js': /^vendor/,
        'test/javascripts/test.js': /^test(\/|\\)(?!vendor)/,
        'test/javascripts/test-vendor.js': /^test(\/|\\)(?=vendor)/
      },
      order: {
        before: [
          'vendor/scripts/console-helper.js',
          'vendor/scripts/jquery-1.9.1.js',
          'vendor/scripts/jquery-migrate.js',
          'vendor/scripts/handlebars-1.0.0.beta.6.js',
          'vendor/scripts/ember-latest.js',
          'vendor/scripts/ember-data-latest.js',
          'vendor/scripts/ember-i18n-1.4.1.js',
          'vendor/scripts/bootstrap.js',
          'vendor/scripts/bootstrap-combobox.js',
          'vendor/scripts/bootstrap-slider.min.js',
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
          'vendor/scripts/jquery.sticky-kit.js',
          'vendor/scripts/jquery.typeahead.js',
          'vendor/scripts/underscore.js',
          'vendor/scripts/backbone.js',
          'vendor/scripts/difflib.js',
          'vendor/scripts/diffview.js',
          'vendor/scripts/visualsearch.js',
          'vendor/scripts/moment.min.js',
          'vendor/scripts/moment-timezone-with-data-2010-2020.js',
          'vendor/scripts/workflow_visualization.js',
          'vendor/scripts/rickshaw.js',
          'vendor/scripts/spin.js',
          'vendor/scripts/jquery.flexibleArea.js',
          'vendor/scripts/FileSaver.js',
          'vendor/scripts/Blob.js',
          'vendor/scripts/pluralize.js',
          'vendor/scripts/sockjs.min.js',
          'vendor/scripts/stomp.min.js',
          'vendor/scripts/theme/bootstrap-ambari.js',
          'vendor/scripts/theme/jszip.min.js'
        ]
      }
    },
    stylesheets: {
      defaultExtension: 'css',
      joinTo: {
        'stylesheets/app.css': /^app/,
        'stylesheets/vendor.css': /^vendor/
      },
      order: {
        before: [
          'app/styles/theme/bootstrap-ambari.css',
          'vendor/styles/bootstrap.css',
          'vendor/styles/font-awesome.css',
          'vendor/styles/font-awesome-ie7.css',
          'vendor/styles/font-awesome-4.css',
          'vendor/styles/cubism.css',
          'vendor/styles/rickshaw.css',
          'vendor/styles/bootstrap-combobox.css',
          'vendor/styles/bootstrap-slider.min.css',
          'vendor/styles/bootstrap-switch.min.css',
          'vendor/styles/diffview.css',
          'vendor/styles/visualsearch-datauri.css'
        ],
        after: [
          'app/styles/custom-ui.css'
        ]
      }
    },


    templates: {
      precompile: true,
      defaultExtensions: ['hbs'],
      joinTo: {'javascripts/app.js': /^app/},
      paths: {
        jquery: 'vendor/scripts/jquery-1.9.1.js',
        handlebars: 'vendor/scripts/handlebars-1.0.0.beta.6.js',
        ember: 'vendor/scripts/ember-latest.js'
      }
    }
  },

  server: {
    port: 3333,
    base: '/',
    run: 'no'
  },

  sourceMaps: false
};
