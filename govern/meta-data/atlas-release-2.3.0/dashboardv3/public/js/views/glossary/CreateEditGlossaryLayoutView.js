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

define(['require',
    'backbone',
    'hbs!tmpl/glossary/CreateEditGlossaryLayoutView_tmpl',
    'utils/Utils',
    'utils/UrlLinks'
], function(require, Backbone, CreateEditGlossaryLayoutViewTmpl, Utils, UrlLinks) {

    var CreateEditGlossaryLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends CreateEditGlossaryLayoutView */
        {
            _viewName: 'CreateEditGlossaryLayoutView',

            template: CreateEditGlossaryLayoutViewTmpl,

            templateHelpers: function() {
                return {
                    create: this.create
                };
            },

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                "name": "[data-id='name']",
                "shortDescription": "[data-id='shortDescription']",
                "longDescription": "[data-id='longDescription']",
                "glossaryForm": "[data-id='glossaryForm']"
            },
            /** ui events hash */
            events: function() {
                var events = {};
                return events;
            },
            /**
             * intialize a new CreateEditGlossaryLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'glossaryCollection', 'model'));
                if (!this.model) {
                    this.create = true;
                }
            },
            bindEvents: function() {},
            onRender: function() {}
        });
    return CreateEditGlossaryLayoutView;
});