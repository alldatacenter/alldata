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
var modulesLoadCount = 0,
    showModuleLoader = function() {
        if (modulesLoadCount === 1) {
            document.querySelector(".module-loader").classList.add("show-loader");
        }
    },
    hideModuleLoader = function() {
        setTimeout(function() {
            if (modulesLoadCount === 0) {
                document.querySelector(".module-loader").className = "module-loader";
            }
        }, 1000);
    };
require.config({
    /* starting point for application */
    'hbs': {
        'disableI18n': true, // This disables the i18n helper and doesn't require the json i18n files (e.g. en_us.json)
        'helperPathCallback': // Callback to determine the path to look for helpers
            function(name) { // ('/template/helpers/'+name by default)
                return 'modules/Helpers';
            },
        'templateExtension': 'html', // Set the extension automatically appended to templates
        'compileOptions': {} // options object which is passed to Handlebars compiler
    },

    'urlArgs': "bust=" + getBustValue(),
    /**
     * Requested as soon as the loader has processed the configuration. It does
     * not block any other require() calls from starting their requests for
     * modules, it is just a way to specify some modules to load asynchronously
     * as part of a config block.
     * @type {Array} An array of dependencies to load.
     */
    'deps': ['marionette'],

    onNodeCreated: function(node, config, moduleName, url) {
        //console.log("module " + moduleName + " is about to be loaded");
        ++modulesLoadCount;
        showModuleLoader();
        node.addEventListener("load", function() {
            //console.log("module " + moduleName + " has been loaded");
            --modulesLoadCount;
            hideModuleLoader();
        });

        node.addEventListener("error", function() {
            //console.log("module " + moduleName + " could not be loaded");
            --modulesLoadCount;
            hideModuleLoader();
        });
    },

    /**
     * The number of seconds to wait before giving up on loading a script.
     * @default 7 seconds
     * @type {Number}
     */
    'waitSeconds': 0,

    'shim': {
        'backbone': {
            'deps': ['underscore', 'jquery'],
            'exports': 'Backbone'
        },
        'jquery-ui': {
            'deps': ['jquery']
        },
        'asBreadcrumbs': {
            'deps': ['jquery'],
            'exports': 'asBreadcrumbs'
        },
        'bootstrap': {
            'deps': ['jquery'],
            'exports': 'jquery'
        },
        'underscore': {
            'exports': '_'
        },
        'marionette': {
            'deps': ['backbone']
        },
        'backgrid': {
            'deps': ['backbone'],
            'exports': 'Backgrid'
        },
        'backgrid-paginator': {
            'deps': ['backbone', 'backgrid']
        },
        'backgrid-filter': {
            'deps': ['backbone', 'backgrid']
        },
        'backgrid-orderable': {
            'deps': ['backbone', 'backgrid'],
        },
        'backgrid-sizeable': {
            'deps': ['backbone', 'backgrid'],
        },
        'backgrid-select-all': {
            'deps': ['backbone', 'backgrid']
        },
        'backgrid-columnmanager': {
            'deps': ['backbone', 'backgrid'],
        },
        'hbs': {
            'deps': ['underscore', 'handlebars']
        },
        'd3': {
            'exports': ['d3']
        },
        'd3-tip': {
            'deps': ['d3'],
            'exports': ['d3-tip']
        },
        'LineageHelper': {
            'deps': ['d3'],
        },
        'dagreD3': {
            'deps': ['d3'],
            'exports': ['dagreD3']
        },
        'sparkline': {
            'deps': ['jquery'],
            'exports': ['sparkline']
        },
        'pnotify': {
            'exports': ['pnotify']
        },
        'jquery-placeholder': {
            'deps': ['jquery']
        },
        'query-builder': {
            'deps': ['jquery']
        },
        'daterangepicker': {
            'deps': ['jquery', 'moment']
        },
        'moment-timezone': {
            'deps': ['moment']
        },
        'moment': {
            'exports': ['moment']
        },
        'jstree': {
            'deps': ['jquery']
        },
        'jquery-steps': {
            'deps': ['jquery']
        }
    },

    paths: {
        'jquery': 'libs/jquery/js/jquery.min',
        'underscore': 'libs/underscore/underscore-min',
        'bootstrap': 'libs/bootstrap/js/bootstrap.min',
        'backbone': 'libs/backbone/backbone-min',
        'backbone.babysitter': 'libs/backbone.babysitter/lib/backbone.babysitter.min',
        'marionette': 'libs/backbone-marionette/backbone.marionette.min',
        'backbone.paginator': 'libs/backbone-paginator/backbone.paginator.min',
        'backbone.wreqr': 'libs/backbone-wreqr/backbone.wreqr.min',
        'backgrid': 'libs/backgrid/js/backgrid',
        'backgrid-filter': 'libs/backgrid-filter/js/backgrid-filter.min',
        'backgrid-orderable': 'libs/backgrid-orderable-columns/js/backgrid-orderable-columns',
        'backgrid-paginator': 'libs/backgrid-paginator/js/backgrid-paginator.min',
        'backgrid-sizeable': 'libs/backgrid-sizeable-columns/js/backgrid-sizeable-columns',
        'backgrid-columnmanager': 'external_lib/backgrid-columnmanager/js/Backgrid.ColumnManager',
        'asBreadcrumbs': 'libs/jquery-asBreadcrumbs/js/jquery-asBreadcrumbs.min',
        'd3': 'libs/d3/d3.min',
        'd3-tip': 'libs/d3/index',
        'LineageHelper': 'external_lib/atlas-lineage/dist/index',
        'dagreD3': 'libs/dagre-d3/dagre-d3.min',
        'sparkline': 'libs/sparkline/jquery.sparkline.min',
        'tmpl': 'templates',
        'requirejs.text': 'libs/requirejs-text/text',
        'handlebars': 'external_lib/require-handlebars-plugin/js/handlebars',
        'hbs': 'external_lib/require-handlebars-plugin/js/hbs',
        'i18nprecompile': 'external_lib/require-handlebars-plugin/js/i18nprecompile',
        'select2': 'libs/select2/select2.full.min',
        'backgrid-select-all': 'libs/backgrid-select-all/backgrid-select-all.min',
        'moment': 'libs/moment/js/moment.min',
        'moment-timezone': 'libs/moment-timezone/moment-timezone-with-data.min',
        'jquery-ui': 'external_lib/jquery-ui/jquery-ui.min',
        'pnotify': 'external_lib/pnotify/pnotify.custom.min',
        'pnotify.buttons': 'external_lib/pnotify/pnotify.custom.min',
        'pnotify.confirm': 'external_lib/pnotify/pnotify.custom.min',
        'jquery-placeholder': 'libs/jquery-placeholder/js/jquery.placeholder',
        'platform': 'libs/platform/platform',
        'query-builder': 'libs/jQueryQueryBuilder/js/query-builder.standalone.min',
        'daterangepicker': 'libs/bootstrap-daterangepicker/js/daterangepicker',
        'table-dragger': 'libs/table-dragger/table-dragger',
        'jstree': 'libs/jstree/jstree.min',
        'jquery-steps': 'libs/jquery-steps/jquery.steps.min',
        'dropzone': 'libs/dropzone/js/dropzone-amd-module',
        'lossless-json': 'libs/lossless-json/lossless-json',
        'store': 'external_lib/idealTimeout/store.min'
    },

    /**
     * If set to true, an error will be thrown if a script loads that does not
     * call define() or have a shim exports string value that can be checked.
     * To get timely, correct error triggers in IE, force a define/shim export.
     * @type {Boolean}
     */
    'enforceDefine': false
});

require(['App',
    'router/Router',
    'utils/Helper',
    'utils/CommonViewFunction',
    'utils/Globals',
    'utils/UrlLinks',
    'collection/VEntityList',
    'collection/VTagList',
    'utils/Enums',
    'utils/Utils',
    'utils/Overrides',
    'bootstrap',
    'd3',
    'select2'
], function(App, Router, Helper, CommonViewFunction, Globals, UrlLinks, VEntityList, VTagList, Enums, Utils) {
    var that = this;
    this.asyncFetchCounter = 5 + (Enums.addOnEntities.length + 1);
    // entity
    this.entityDefCollection = new VEntityList();
    this.entityDefCollection.url = UrlLinks.entitiesDefApiUrl();
    // typeHeaders
    this.typeHeaders = new VTagList();
    this.typeHeaders.url = UrlLinks.typesApiUrl();
    // enum
    this.enumDefCollection = new VTagList();
    this.enumDefCollection.url = UrlLinks.enumDefApiUrl();
    this.enumDefCollection.modelAttrName = "enumDefs";
    // classfication
    this.classificationDefCollection = new VTagList();
    // metric
    this.metricCollection = new VTagList();
    this.metricCollection.url = UrlLinks.metricsApiUrl();
    this.metricCollection.modelAttrName = "data";
    this.classificationAndMetricEvent = new Backbone.Wreqr.EventAggregator();
    // businessMetadata
    this.businessMetadataDefCollection = new VEntityList();
    this.businessMetadataDefCollection.url = UrlLinks.businessMetadataDefApiUrl();
    this.businessMetadataDefCollection.modelAttrName = "businessMetadataDefs";

    App.appRouter = new Router({
        entityDefCollection: this.entityDefCollection,
        typeHeaders: this.typeHeaders,
        enumDefCollection: this.enumDefCollection,
        classificationDefCollection: this.classificationDefCollection,
        metricCollection: this.metricCollection,
        classificationAndMetricEvent: this.classificationAndMetricEvent,
        businessMetadataDefCollection: this.businessMetadataDefCollection
    });

    var startApp = function() {
        if (that.asyncFetchCounter === 0) {
            App.start();
        }
    };
    CommonViewFunction.userDataFetch({
        url: UrlLinks.sessionApiUrl(),
        callback: function(response) {
            if (response) {
                if (response.userName) {
                    Globals.userLogedIn.status = true;
                    Globals.userLogedIn.response = response;
                }
                if (response['atlas.entity.create.allowed'] !== undefined) {
                    Globals.entityCreate = response['atlas.entity.create.allowed'];
                }
                if (response['atlas.entity.update.allowed'] !== undefined) {
                    Globals.entityUpdate = response['atlas.entity.update.allowed'];
                }
                if (response['atlas.ui.editable.entity.types'] !== undefined) {
                    var entityTypeList = response['atlas.ui.editable.entity.types'].trim().split(",");
                    if (entityTypeList.length) {
                        if (entityTypeList[0] === "*") {
                            Globals.entityTypeConfList = [];
                        } else if (entityTypeList.length > 0) {
                            Globals.entityTypeConfList = entityTypeList;
                        }
                    }
                }
                if (response['atlas.ui.default.version'] !== undefined) {
                    Globals.DEFAULT_UI = response['atlas.ui.default.version'];
                }
                if (response['atlas.ui.date.format'] !== undefined) {
                    Globals.dateTimeFormat = response['atlas.ui.date.format'];
                    var dateFormatSeperated = Globals.dateTimeFormat.split(' ');
                    if (dateFormatSeperated[0]) {
                        Globals.dateFormat = dateFormatSeperated[0]; //date
                    }
                }
                if (response['atlas.ui.date.timezone.format.enabled'] !== undefined) {
                    Globals.isTimezoneFormatEnabled = response['atlas.ui.date.timezone.format.enabled'];
                }
                if (response['atlas.debug.metrics.enabled'] !== undefined) {
                    Globals.isDebugMetricsEnabled = response["atlas.debug.metrics.enabled"];
                }
                if (response['atlas.tasks.enabled'] !== undefined) {
                    Globals.isTasksEnabled = response['atlas.tasks.enabled'];
                }
                if (response['atlas.session.timeout.secs']) { Globals.idealTimeoutSeconds = response['atlas.session.timeout.secs']; }
                /*  Atlas idealTimeout 
       redirectUrl: url to redirect after timeout
       idealTimeLimit: timeout in seconds
       activityEvents: ideal keyboard mouse events
       dialogDisplayLimit: show popup before timeout in seconds
       */
                $(document).ready(function() {
                    $(document).idleTimeout({
                        redirectUrl: Utils.getBaseUrl(window.location.pathname) + '/index.html?action=timeout', // redirect to this url
                        idleTimeLimit: Globals.idealTimeoutSeconds, // 900 seconds
                        activityEvents: 'click keypress scroll wheel mousemove', // separate each event with a space
                        dialogDisplayLimit: 10, // Time to display the warning dialog before logout (and optional callback) in seconds
                        sessionKeepAliveTimer: false, // Set to false to disable pings.
                        onModalKeepAlive: function() {
                            CommonViewFunction.userDataFetch({
                                url: UrlLinks.sessionApiUrl()
                            })
                        }
                    });
                });
            }
            --that.asyncFetchCounter;
            startApp();
        }
    });
    this.entityDefCollection.fetch({
        complete: function() {
            that.entityDefCollection.fullCollection.comparator = function(model) {
                return model.get('name').toLowerCase();
            };
            that.entityDefCollection.fullCollection.sort({ silent: true });
            --that.asyncFetchCounter;
            startApp();
        }
    });
    this.typeHeaders.fetch({
        complete: function() {
            that.typeHeaders.fullCollection.comparator = function(model) {
                return model.get('name').toLowerCase();
            }
            that.typeHeaders.fullCollection.sort({ silent: true });
            --that.asyncFetchCounter;
            startApp();
        }
    });
    this.enumDefCollection.fetch({
        complete: function() {
            that.enumDefCollection.fullCollection.comparator = function(model) {
                return model.get('name').toLowerCase();
            };
            that.enumDefCollection.fullCollection.sort({ silent: true });
            --that.asyncFetchCounter;
            startApp();
        }
    });
    this.classificationDefCollection.fetch({
        async: true,
        complete: function() {
            that.classificationDefCollection.fullCollection.comparator = function(model) {
                return model.get('name').toLowerCase();
            };
            that.classificationDefCollection.fullCollection.sort({ silent: true });
            that.classificationAndMetricEvent.trigger("Classification:Count:Update");
            //--that.asyncFetchCounter;
            startApp();
        }
    });
    this.metricCollection.fetch({
        async: true,
        success: function() {
            // that.classificationAndMetricEvent.trigger("metricCollection:Update");
        },
        complete: function() {}
    });

    this.businessMetadataDefCollection.fetch({
        complete: function() {
            that.businessMetadataDefCollection.fullCollection.comparator = function(model) {
                return model.get('name').toLowerCase();
            };
            that.businessMetadataDefCollection.fullCollection.sort({ silent: true });
            --that.asyncFetchCounter;
            startApp();
        }
    });
    CommonViewFunction.fetchRootEntityAttributes({
        url: UrlLinks.rootEntityDefUrl(Enums.addOnEntities[0]),
        entity: Enums.addOnEntities,
        callback: function() {
            --that.asyncFetchCounter;
            startApp();
        }
    });
    CommonViewFunction.fetchRootClassificationAttributes({
        url: UrlLinks.rootClassificationDefUrl(Enums.addOnClassification[0]),
        classification: Enums.addOnClassification,
        callback: function() {
            --that.asyncFetchCounter;
            startApp();
        }
    });
});