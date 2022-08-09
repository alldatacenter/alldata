/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';
var sass = require('node-sass');
module.exports = function(grunt) {
    var buildTime = new Date().getTime(),
        distPath = './dist',
        libPath = distPath + '/js/libs/',
        isDashboardDirectory = grunt.file.isDir('public'),
        nodeModulePath = './node_modules/',
        modulesPath = 'public/';
    if (!isDashboardDirectory) {
        modulesPath = '../public/'
    }

    grunt.initConfig({
        watch: {
            js: {
                files: ['public/**/*.js'],
                tasks: ['copy:build']
            },
            html: {
                files: ['public/**/*.html'],
                tasks: ['copy:build']
            },
            css: {
                files: ['public/**/*.scss', 'public/**/*.css'],
                tasks: ['copy:build', 'sass']
            },
            image: {
                files: ['public/**/*.{ico,gif,png}'],
                tasks: ['copy:build']
            }
        },
        connect: {
            server: {
                options: {
                    port: 9999,
                    base: distPath,
                    // change this to '0.0.0.0' to access the server from outside
                    hostname: '0.0.0.0',
                    middleware: function(connect, options, middlewares) {
                        middlewares.unshift(require('grunt-middleware-proxy/lib/Utils').getProxyMiddleware());
                        return middlewares;
                    }
                },
                proxies: [{
                    context: '/api', // the context of the data service
                    host: '127.0.0.1',
                    auth: "admin:admin",
                    port: 21000, // the port that the data service is running on
                    https: false
                }],
            },
        },
        npmcopy: {
            js: {
                options: {
                    destPrefix: libPath,
                    srcPrefix: nodeModulePath
                },
                files: {
                    // FileName : {"src":"dest"}
                    'jquery.min.js': { 'jquery/dist': 'jquery/js' },
                    'require.js': { 'requirejs': 'requirejs' },
                    'text.js': { 'requirejs-text': 'requirejs-text' },
                    'underscore-min.js': { 'underscore': 'underscore' },
                    'bootstrap.min.js': { 'bootstrap/dist/js': 'bootstrap/js' },
                    'backbone-min.js': { 'backbone': 'backbone' },
                    'backbone.babysitter.min.js': { 'backbone.babysitter/lib': 'backbone-babysitter' },
                    'backbone.marionette.min.js': { 'backbone.marionette/lib': 'backbone-marionette' },
                    'backbone.paginator.min.js': { 'backbone.paginator/lib': 'backbone-paginator' },
                    'backbone.wreqr.min.js': { 'backbone.wreqr/lib': 'backbone-wreqr' },
                    'backgrid.js': { 'backgrid/lib': 'backgrid/js' },
                    'backgrid-filter.min.js': { 'backgrid-filter': 'backgrid-filter/js' },
                    'backgrid-orderable-columns.js': { 'backgrid-orderable-columns': 'backgrid-orderable-columns/js' },
                    'backgrid-paginator.min.js': { 'backgrid-paginator': 'backgrid-paginator/js' },
                    'backgrid-sizeable-columns.js': { 'backgrid-sizeable-columns': 'backgrid-sizeable-columns/js' },
                    'Backgrid.ColumnManager.js': { 'backgrid-columnmanager/src': 'backgrid-columnmanager/js' },
                    'jquery-asBreadcrumbs.min.js': { 'jquery-asBreadcrumbs/dist': 'jquery-asBreadcrumbs/js' },
                    'd3.min.js': { 'd3/dist': 'd3' },
                    'index.js': { 'd3-tip': 'd3/' },
                    'dagre-d3.min.js': { 'dagre-d3/dist': 'dagre-d3' },
                    'select2.full.min.js': { 'select2/dist/js': 'select2' },
                    'backgrid-select-all.min.js': { 'backgrid-select-all': 'backgrid-select-all' },
                    'moment.min.js': { 'moment/min': 'moment/js' },
                    'moment-timezone-with-data.min.js': { 'moment-timezone/builds': 'moment-timezone' },
                    'jquery.placeholder.js': { 'jquery-placeholder': 'jquery-placeholder/js' },
                    'platform.js': { 'platform': 'platform' },
                    'query-builder.standalone.min.js': { 'jQuery-QueryBuilder/dist/js': 'jQueryQueryBuilder/js' },
                    'daterangepicker.js': { 'bootstrap-daterangepicker': 'bootstrap-daterangepicker/js' },
                    'jquery.sparkline.min.js': { 'jquery-sparkline': 'sparkline' },
                    'table-dragger.js': { 'table-dragger/dist': 'table-dragger' },
                    'jstree.min.js': { 'jstree/dist': 'jstree' },
                    'jquery.steps.min.js': { 'jquery-steps/build': 'jquery-steps' },
                    'dropzone-amd-module.js': { 'dropzone/dist': "dropzone/js" },
                    'lossless-json.js': { 'lossless-json/dist': 'lossless-json' }
                }

            },
            css: {
                options: {
                    destPrefix: libPath,
                    srcPrefix: nodeModulePath
                },
                files: {
                    'bootstrap.min.css': { 'bootstrap/dist/css': 'bootstrap/css' },
                    'glyphicons-halflings-regular.woff2': { 'bootstrap/fonts': 'bootstrap/fonts' },
                    'backgrid.css': { 'backgrid/lib': 'backgrid/css' },
                    'backgrid-filter.min.css': { 'backgrid-filter': 'backgrid-filter/css' },
                    'backgrid-orderable-columns.css': { 'backgrid-orderable-columns': 'backgrid-orderable-columns/css' },
                    'backgrid-paginator.css': { 'backgrid-paginator': 'backgrid-paginator/css' },
                    'backgrid-sizeable-columns.css': { 'backgrid-sizeable-columns': 'backgrid-sizeable-columns/css' },
                    'Backgrid.ColumnManager.css': { 'backgrid-columnmanager/lib': 'backgrid-columnmanager/css' },
                    'asBreadcrumbs.min.css': { 'jquery-asBreadcrumbs/dist/css': 'jquery-asBreadcrumbs/css' },
                    'select2.min.css': { 'select2/dist/css': 'select2/css' },
                    'backgrid-select-all.min.css': { 'backgrid-select-all': 'backgrid-select-all' },
                    'font-awesome.min.css': { 'font-awesome/css': 'font-awesome/css' },
                    '*': [{
                        'expand': true,
                        'dot': true,
                        'cwd': nodeModulePath + 'font-awesome',
                        'src': ['fonts/*.*'],
                        'dest': libPath + 'font-awesome/'
                    }, {
                        'expand': true,
                        'dot': true,
                        'cwd': nodeModulePath + 'jstree/dist/themes/',
                        'src': ['**'],
                        'dest': libPath + 'jstree/css/'
                    }],
                    'query-builder.default.min.css': { 'jQuery-QueryBuilder/dist/css': 'jQueryQueryBuilder/css' },
                    'daterangepicker.css': { 'bootstrap-daterangepicker': 'bootstrap-daterangepicker/css' },
                    'pretty-checkbox.min.css': { 'pretty-checkbox/dist': 'pretty-checkbox/css' },
                    'dropzone.css': { 'dropzone/dist': "dropzone/css" }
                }

            },
            license: {
                options: {
                    destPrefix: libPath,
                    srcPrefix: nodeModulePath
                },
                files: {
                    'LICENSE.txt': [
                        { 'jquery': 'jquery' },
                        { 'jquery-placeholder': 'jquery-placeholder' }
                    ],
                    'LICENSE': [{ 'requirejs-text': 'requirejs-text' },
                        { 'underscore': 'underscore' },
                        { 'bootstrap': 'bootstrap' },
                        { 'backgrid-columnmanager': 'backgrid-columnmanager' },
                        { 'jquery-asBreadcrumbs': 'jquery-asBreadcrumbs' },
                        { 'd3': 'd3' },
                        { 'd3-tip': 'd3/' },
                        { 'dagre-d3': 'dagre-d3' },
                        { 'platform': 'platform/' },
                        { 'jQuery-QueryBuilder': 'jQueryQueryBuilder/' },
                        { 'moment-timezone': 'moment-timezone' },
                        { 'pretty-checkbox': 'pretty-checkbox' }
                    ],
                    'LICENSE.md': [{ 'backbone.babysitter': 'backbone-babysitter' },
                        { 'backbone.wreqr': 'backbone-wreqr' },
                        { 'lossless-json': 'lossless-json' }
                    ],
                    'license.txt': [{ 'backbone.marionette': 'backbone-marionette' }],
                    'license': [{ 'table-dragger': 'table-dragger' }],
                    'LICENSE-MIT': [{ 'backbone.paginator': 'backbone-paginator' },
                        { 'backgrid': 'backgrid' },
                        { 'backgrid-filter': 'backgrid-filter' },
                        { 'backgrid-orderable-columns': 'backgrid-orderable-columns' },
                        { 'backgrid-paginator': 'backgrid-paginator' },
                        { 'backgrid-sizeable-columns': 'backgrid-sizeable-columns' },
                        { 'backgrid-select-all': 'backgrid-select-all' }
                    ]
                }
            }
        },
        rename: {
            main: {
                files: [
                    { src: [libPath + 'jstree/css/default/style.min.css'], dest: libPath + 'jstree/css/default/default-theme.min.css' },
                    { src: [libPath + 'jstree/css/default-dark/style.min.css'], dest: libPath + 'jstree/css/default-dark/default-dark-theme.min.css' },
                ]
            }
        },
        sass: {
            options: {
                implementation: sass,
                sourceMap: false
            },
            build: {
                files: {
                    [distPath + '/css/style.css']: modulesPath + 'css/scss/style.scss',
                    [distPath + '/css/migration-style.css']: modulesPath + 'css/scss/migration-style.scss',
                    [distPath + '/css/login.css']: modulesPath + 'css/scss/login.scss'
                }
            }
        },
        copy: {
            build: {
                expand: true,
                cwd: modulesPath,
                src: ['**', '!**/scss/**', "!**/atlas-lineage/**", "**/atlas-lineage/dist/**", "!index.html.tpl"],
                dest: distPath
            }
        },
        clean: {
            build: [distPath, libPath],
            options: {
                force: true
            }
        },
        uglify: {
            buildlibs: {
                options: {
                    mangle: true,
                    compress: true,
                    beautify: false
                },
                files: [{
                    expand: true,
                    cwd: distPath + '/js',
                    src: ['external_lib/**/*.js', 'libs/**/*.js'],
                    dest: distPath + '/js'
                }]
            },
            buildjs: {
                options: {
                    mangle: false,
                    compress: true,
                    beautify: true
                },
                files: [{
                    expand: true,
                    cwd: distPath + '/js',
                    src: ['**/*.js', '!libs/**', '!external_lib/**'],
                    dest: distPath + '/js'
                }]
            }
        },
        cssmin: {
            build: {
                files: [{
                    expand: true,
                    cwd: distPath + '/css',
                    src: '*.css',
                    dest: distPath + '/css'
                }]
            }
        },
        htmlmin: {
            build: {
                options: {
                    removeComments: true,
                    collapseWhitespace: true
                },
                files: [{
                    expand: true,
                    cwd: distPath + '/js/templates',
                    src: '**/*.html',
                    dest: distPath + '/js/templates'
                }]
            }
        },
        template: {
            build: {
                options: {
                    data: {
                        'bust': buildTime
                    }
                },
                files: {
                    [distPath + '/index.html']: [modulesPath + 'index.html.tpl'],
                    [distPath + '/migration-status.html']: [modulesPath + 'migration-status.html.tpl']
                }
            }
        }
    });

    // Dynamically add copy-task using npmcopy
    var npmCopy = grunt.config.get('npmcopy'),
        libFiles = [],
        createPath = function(options) {
            var obj = options.obj,
                fileName = options.fileName,
                pathPrefix = options.pathPrefix;
            if (obj.length) {
                for (var i in obj) {
                    createPath({
                        'obj': obj[i],
                        'libFiles': options.libFiles,
                        'pathPrefix': pathPrefix,
                        'fileName': fileName
                    });
                }
            } else {
                if (fileName == "*") {
                    options.libFiles.push(obj);
                } else {
                    key = Object.keys(obj);
                    options.libFiles.push({ 'src': pathPrefix.srcPrefix + key + "/" + fileName, 'dest': pathPrefix.destPrefix + obj[key] + "/" + fileName });
                }
            }
        };

    for (var key in npmCopy) {
        var options = npmCopy[key].options,
            files = npmCopy[key].files;
        for (var fileName in files) {
            createPath({
                'obj': files[fileName],
                'libFiles': libFiles,
                'pathPrefix': options,
                'fileName': fileName
            });
        }
    };
    grunt.config.set('copy.libs', { files: libFiles });

    grunt.loadNpmTasks('grunt-contrib-connect');
    grunt.loadNpmTasks('grunt-middleware-proxy');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-contrib-htmlmin');
    grunt.loadNpmTasks('grunt-template');
    grunt.loadNpmTasks('grunt-contrib-rename');

    require('load-grunt-tasks')(grunt);

    grunt.registerTask('dev', [
        'clean',
        'copy:libs',
        'copy:build',
        'rename',
        'sass:build',
        'template',
        'setupProxies:server',
        'connect:server',
        'watch'
    ]);

    grunt.registerTask('build', [
        'clean',
        'copy:libs',
        'copy:build',
        'rename',
        'sass:build',
        'template'
    ]);

    grunt.registerTask('dev-minify', [
        'clean',
        'copy:libs',
        'copy:build',
        'rename',
        'sass:build',
        'uglify',
        'cssmin',
        'template',
        'setupProxies:server',
        'connect:server',
        'watch'
    ]);

    grunt.registerTask('build-minify', [
        'clean',
        'copy:libs',
        'copy:build',
        'rename',
        'sass:build',
        'uglify',
        'cssmin',
        'template'
    ]);
};