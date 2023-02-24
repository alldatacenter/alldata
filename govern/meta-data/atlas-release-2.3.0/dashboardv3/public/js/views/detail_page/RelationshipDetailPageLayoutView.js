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
    'hbs!tmpl/detail_page/RelationshipDetailPageLayoutView_tmpl',
    'utils/Utils',
    'utils/CommonViewFunction',
    'utils/Globals',
    'utils/Enums',
    'utils/Messages',
    'utils/UrlLinks',
    'collection/VEntityList'
], function(require, Backbone, RelationshipDetailPageLayoutView, Utils, CommonViewFunction, Globals, Enums, Messages, UrlLinks, VEntityList) {
    'use strict';

    var RelationshipDetailPageLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends DetailPageLayoutView */
        {
            _viewName: 'RelationshipDetailPageLayoutView',

            template: RelationshipDetailPageLayoutView,

            /** Layout sub regions */
            regions: {
                RRelationshipDetailTableLayoutView: "#r_relationshipDetailTableLayoutView"
            },
            /** ui selector cache */
            ui: {
                title: '[data-id="title"]',
                entityIcon: '[data-id="entityIcon"]',
                relationshipEnd1: '[data-id="relationshipEnd1"]',
                relationshipEnd2: '[data-id="relationshipEnd2"]',
                otherAttributes: '[data-id="otherAttributes"]',
                backButton: '[data-id="backButton"]'
            },
            templateHelpers: function() {
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.backButton] = function() {
                    Utils.backButtonClick();
                }
                return events;
            },
            /**
             * intialize a new DetailPageLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'value', 'collection', 'id', 'relationshipDefCollection', 'searchVent'));
                $('body').addClass("detail-page");
                this.collection = new VEntityList([], {});
                this.collection.url = UrlLinks.relationApiUrl({ guid: this.id, minExtInfo: true });
                this.fetchCollection();
            },
            bindEvents: function() {
                var that = this;
                this.listenTo(this.collection, 'reset', function() {
                    this.relationshipObject = this.collection.first().toJSON();
                    var collectionJSON = this.relationshipObject.relationship,
                        name = collectionJSON ? Utils.getName(collectionJSON) : "";
                    if (collectionJSON) {
                        this.readOnly = Enums.entityStateReadOnly[collectionJSON.status];
                        if (name && collectionJSON.typeName) {
                            name = name + ' (' + _.escape(collectionJSON.typeName) + ')';
                        }
                        if (this.readOnly) {
                            this.$el.addClass('readOnly');
                        } else {
                            this.$el.removeClass('readOnly');
                        }
                        if (name) {
                            this.ui.title.show();
                            var titleName = '<span>' + name + '</span>';
                            if (this.readOnly) {
                                titleName += '<button title="Deleted" class="btn btn-action btn-md deleteBtn"><i class="fa fa-trash"></i> Deleted</button>';
                            }
                            if (this.readOnly) {
                                this.ui.entityIcon.addClass('disabled');
                            } else {
                                this.ui.entityIcon.removeClass('disabled');
                            }
                            this.ui.title.html(titleName);
                            var entityData = _.extend({}, collectionJSON),
                                img = this.readOnly ? '<img src="/img/entity-icon/disabled/table.png"/>' : '<img src="/img/entity-icon/table.png"/>';
                            this.ui.entityIcon.attr('title', _.escape(collectionJSON.typeName)).html(img);
                        } else {
                            this.ui.title.hide();
                        }
                    }
                    this.hideLoader();
                    var obj = {
                        entity: collectionJSON,
                        guid: this.id,
                        entityName: name,
                        fetchCollection: this.fetchCollection.bind(that),
                        searchVent: this.searchVent,
                        attributeDefs: (function() {
                            return that.getEntityDef(collectionJSON);
                        })(),
                        isRelationshipDetailPage: true
                    }
                    this.renderRelationshipDetailTableLayoutView(obj);
                    this.ui.relationshipEnd1.empty().html(this.renderRelationAttributesDetails({
                        scope: this,
                        valueObject: collectionJSON.end1,
                        isRelationshipAttribute: true,
                        guidHyperLink: true
                    }));
                    this.ui.relationshipEnd2.empty().html(this.renderRelationAttributesDetails({
                        scope: this,
                        valueObject: collectionJSON.end2,
                        isRelationshipAttribute: true,
                        guidHyperLink: true
                    }));
                    this.ui.otherAttributes.empty().html(this.renderRelationAttributesDetails({
                        scope: this,
                        valueObject: _.pick(collectionJSON, 'createTime', 'createdBy', 'blockedPropagatedClassifications', 'guid', 'label', 'propagateTags', 'propagatedClassifications', 'provenanceType', 'status', 'updateTime', 'updatedBy', 'version'),
                        isRelationshipAttribute: true,
                        guidHyperLink: false
                    }));
                }, this);
                this.listenTo(this.collection, 'error', function(model, response) {
                    this.$('.fontLoader-relative').removeClass('show');
                    if (response.responseJSON) {
                        Utils.notifyError({
                            content: response.responseJSON.errorMessage || response.responseJSON.error
                        });
                    }
                }, this);
            },
            onRender: function() {
                var that = this;
                this.bindEvents();
                Utils.showTitleLoader(this.$('.page-title .fontLoader'), this.$('.relationshipDetail'));
                this.$('.fontLoader-relative').addClass('show'); // to show tab loader
            },
            manualRender: function(options) {
                if (options) {
                    var oldId = this.id;
                    _.extend(this, _.pick(options, 'value', 'id'));
                    if (this.id !== oldId) {
                        this.collection.url = UrlLinks.relationApiUrl({ guid: this.id, minExtInfo: true });
                        this.fetchCollection();
                    }
                }
            },
            onDestroy: function() {
                if (!Utils.getUrlState.isRelationshipDetailPage() && !Utils.getUrlState.isDetailPage()) {
                    $('body').removeClass("detail-page");
                }
            },
            fetchCollection: function() {
                this.collection.fetch({ reset: true });
                if (this.searchVent) {
                    this.searchVent.trigger('relationshipList:refresh');
                }
            },
            getEntityDef: function(entityObj) {
                if (this.activeEntityDef) {
                    var data = this.activeEntityDef.toJSON();
                    var attributeDefs = Utils.getNestedSuperTypeObj({
                        data: data,
                        attrMerge: true,
                        collection: this.relationshipDefCollection
                    });
                    return attributeDefs;
                } else {
                    return [];
                }
            },
            hideLoader: function() {
                Utils.hideTitleLoader(this.$('.page-title .fontLoader'), this.$('.relationshipDetail'));
            },
            showLoader: function() {
                Utils.showTitleLoader(this.$('.page-title .fontLoader'), this.$('.relationshipDetail'));
            },
            renderRelationshipDetailTableLayoutView: function(obj) {
                var that = this;
                require(['views/entity/EntityDetailTableLayoutView'], function(EntityDetailTableLayoutView) {
                    that.RRelationshipDetailTableLayoutView.show(new EntityDetailTableLayoutView(obj));
                });
            },
            renderRelationAttributesDetails: function(options) {
                var table = CommonViewFunction.propertyTable({
                    scope: options.scope,
                    valueObject: options.valueObject,
                    isRelationshipAttribute: options.isRelationshipAttribute,
                    guidHyperLink: options.guidHyperLink
                });
                return table;
            }
        });
    return RelationshipDetailPageLayoutView;
});