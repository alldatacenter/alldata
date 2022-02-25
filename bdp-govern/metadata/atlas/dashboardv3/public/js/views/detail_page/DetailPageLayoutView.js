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
    'hbs!tmpl/detail_page/DetailPageLayoutView_tmpl',
    'utils/Utils',
    'utils/CommonViewFunction',
    'utils/Globals',
    'utils/Enums',
    'utils/Messages',
    'utils/UrlLinks',
    'collection/VEntityList'
], function(require, Backbone, DetailPageLayoutViewTmpl, Utils, CommonViewFunction, Globals, Enums, Messages, UrlLinks, VEntityList) {
    'use strict';

    var DetailPageLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends DetailPageLayoutView */
        {
            _viewName: 'DetailPageLayoutView',

            template: DetailPageLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                REntityDetailTableLayoutView: "#r_entityDetailTableLayoutView",
                RSchemaTableLayoutView: "#r_schemaTableLayoutView",
                RTagTableLayoutView: "#r_tagTableLayoutView",
                RLineageLayoutView: "#r_lineageLayoutView",
                RAuditTableLayoutView: "#r_auditTableLayoutView",
                RPendingTaskTableLayoutView: "#r_pendingTaskTableLayoutView",
                RReplicationAuditTableLayoutView: "#r_replicationAuditTableLayoutView",
                RProfileLayoutView: "#r_profileLayoutView",
                RRelationshipLayoutView: "#r_relationshipLayoutView",
                REntityUserDefineView: "#r_entityUserDefineView",
                REntityLabelDefineView: "#r_entityLabelDefineView",
                REntityBusinessMetadataView: "#r_entityBusinessMetadataView"
            },
            /** ui selector cache */
            ui: {
                tagClick: '[data-id="tagClick"]',
                pTagCountClick: '[data-id="pTagCountClick"]',
                termClick: '[data-id="termClick"]',
                propagatedTagDiv: '[data-id="propagatedTagDiv"]',
                title: '[data-id="title"]',
                description: '[data-id="description"]',
                editBox: '[data-id="editBox"]',
                deleteTag: '[data-id="deleteTag"]',
                deleteTerm: '[data-id="deleteTerm"]',
                addTag: '[data-id="addTag"]',
                addTerm: '[data-id="addTerm"]',
                tagList: '[data-id="tagList"]',
                termList: '[data-id="termList"]',
                propagatedTagList: '[data-id="propagatedTagList"]',
                tablist: '[data-id="tab-list"] li',
                entityIcon: '[data-id="entityIcon"]',
                backButton: '[data-id="backButton"]'
            },
            templateHelpers: function() {
                return {
                    entityUpdate: Globals.entityUpdate,
                    isTasksEnabled: Globals.isTasksEnabled
                };
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.tagClick] = function(e) {
                    if (e.target.nodeName.toLocaleLowerCase() != "i") {
                        Utils.setUrl({
                            url: '#!/tag/tagAttribute/' + e.currentTarget.textContent,
                            mergeBrowserUrl: false,
                            trigger: true
                        });
                    }
                };
                events["click " + this.ui.pTagCountClick] = function(e) {
                    var tag = $(e.currentTarget).parent().children().first().text();
                    Utils.setUrl({
                        url: '#!/detailPage/' + this.id + '?tabActive=classification&filter=' + tag,
                        mergeBrowserUrl: false,
                        trigger: true
                    });
                };
                events["click " + this.ui.termClick] = function(e) {
                    if (e.target.nodeName.toLocaleLowerCase() != "i") {
                        Utils.setUrl({
                            url: '#!/glossary/' + $(e.currentTarget).find('i').data('guid'),
                            mergeBrowserUrl: false,
                            urlParams: { gType: "term", viewType: "term", fromView: "entity" },
                            trigger: true
                        });
                    }
                };
                events["click " + this.ui.addTerm] = 'onClickAddTermBtn';
                events["click " + this.ui.deleteTag] = 'onClickTagCross';
                events["click " + this.ui.deleteTerm] = 'onClickTermCross';
                events["click " + this.ui.addTag] = 'onClickAddTagBtn';
                events["click " + this.ui.tablist] = function(e) {
                    var tabValue = $(e.currentTarget).attr('role');
                    Utils.setUrl({
                        url: Utils.getUrlState.getQueryUrl().queyParams[0],
                        urlParams: { tabActive: tabValue || 'properties' },
                        mergeBrowserUrl: false,
                        trigger: false,
                        updateTabState: true
                    });

                };
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
                _.extend(this, _.pick(options, 'value', 'collection', 'id', 'entityDefCollection', 'typeHeaders', 'enumDefCollection', 'classificationDefCollection', 'glossaryCollection', 'businessMetadataDefCollection', 'searchVent'));
                $('body').addClass("detail-page");
                this.collection = new VEntityList([], {});
                this.collection.url = UrlLinks.entitiesApiUrl({ guid: this.id, minExtInfo: true });
                this.fetchCollection();
            },
            bindEvents: function() {
                var that = this;
                this.listenTo(this.collection, 'reset', function() {
                    this.entityObject = this.collection.first().toJSON();
                    var collectionJSON = this.entityObject.entity;
                    this.activeEntityDef = this.entityDefCollection.fullCollection.find({ name: collectionJSON.typeName });
                    if (!this.activeEntityDef) {
                        Utils.backButtonClick();
                        Utils.notifyError({
                            content: "Unknown Entity-Type"
                        });
                        return true;
                    }
                    if (collectionJSON && _.startsWith(collectionJSON.typeName, "AtlasGlossary")) {
                        this.$(".termBox").hide();
                    }
                    // MergerRefEntity.
                    Utils.findAndMergeRefEntity({
                        attributeObject: collectionJSON.attributes,
                        referredEntities: this.entityObject.referredEntities
                    });

                    Utils.findAndMergeRefEntity({
                        attributeObject: collectionJSON.relationshipAttributes,
                        referredEntities: this.entityObject.referredEntities
                    });

                    Utils.findAndMergeRelationShipEntity({
                        attributeObject: collectionJSON.attributes,
                        relationshipAttributes: collectionJSON.relationshipAttributes
                    });

                    // check if entity is process
                    var isProcess = false,
                        typeName = Utils.getName(collectionJSON, 'typeName'),
                        superTypes = Utils.getNestedSuperTypes({ data: this.activeEntityDef.toJSON(), collection: this.entityDefCollection }),
                        isLineageRender = _.find(superTypes, function(type) {
                            if (type === "DataSet" || type === "Process") {
                                if (type === "Process") {
                                    isProcess = true;
                                }
                                return true;
                            }
                        });
                    if (!isLineageRender) {
                        isLineageRender = (typeName === "DataSet" || typeName === "Process") ? true : null;
                    }
                    if (collectionJSON && collectionJSON.guid) {
                        var tagGuid = collectionJSON.guid;
                        this.readOnly = Enums.entityStateReadOnly[collectionJSON.status];
                    } else {
                        var tagGuid = this.id;
                    }
                    if (this.readOnly) {
                        this.$el.addClass('readOnly');
                    } else {
                        this.$el.removeClass('readOnly');
                    }
                    if (collectionJSON) {
                        this.name = Utils.getName(collectionJSON);

                        if (collectionJSON.attributes) {
                            if (collectionJSON.typeName) {
                                collectionJSON.attributes.typeName = _.escape(collectionJSON.typeName);
                            }
                            if (this.name && collectionJSON.typeName) {
                                this.name = this.name + ' (' + _.escape(collectionJSON.typeName) + ')';
                            }
                            if (!this.name && collectionJSON.typeName) {
                                this.name = _.escape(collectionJSON.typeName);
                            }
                            this.description = collectionJSON.attributes.description;
                            if (this.name) {
                                this.ui.title.show();
                                var titleName = '<span>' + this.name + '</span>';
                                if (this.readOnly) {
                                    titleName += '<button title="Deleted" class="btn btn-action btn-md deleteBtn"><i class="fa fa-trash"></i> Deleted</button>';
                                }
                                this.ui.title.html(titleName);
                                if (collectionJSON.attributes.serviceType === undefined) {
                                    if (Globals.serviceTypeMap[collectionJSON.typeName] === undefined && this.activeEntityDef) {
                                        Globals.serviceTypeMap[collectionJSON.typeName] = this.activeEntityDef.get('serviceType');
                                    }
                                } else if (Globals.serviceTypeMap[collectionJSON.typeName] === undefined) {
                                    Globals.serviceTypeMap[collectionJSON.typeName] = collectionJSON.attributes.serviceType;
                                }
                                var entityData = _.extend({ "serviceType": Globals.serviceTypeMap[collectionJSON.typeName], "isProcess": isProcess }, collectionJSON);
                                if (this.readOnly) {
                                    this.ui.entityIcon.addClass('disabled');
                                } else {
                                    this.ui.entityIcon.removeClass('disabled');
                                }
                                this.ui.entityIcon.attr('title', _.escape(collectionJSON.typeName)).html('<img src="' + Utils.getEntityIconPath({ entityData: entityData }) + '"/>').find("img").on('error', function() {
                                    this.src = Utils.getEntityIconPath({ entityData: entityData, errorUrl: this.src });
                                });
                            } else {
                                this.ui.title.hide();
                            }
                            if (this.description) {
                                this.ui.description.show();
                                this.ui.description.html('<span>' + _.escape(this.description) + '</span>');
                            } else {
                                this.ui.description.hide();
                            }
                        }
                        var tags = {
                            'self': [],
                            'propagated': [],
                            'propagatedMap': {},
                            'combineMap': {}
                        };
                        if (collectionJSON.classifications) {
                            var tagObject = collectionJSON.classifications;
                            _.each(tagObject, function(val) {
                                var typeName = val.typeName;
                                if (val.entityGuid === that.id) {
                                    tags['self'].push(val)
                                } else {
                                    tags['propagated'].push(val);
                                    if (tags.propagatedMap[typeName]) {
                                        tags.propagatedMap[typeName]["count"]++;
                                    } else {
                                        tags.propagatedMap[typeName] = val;
                                        tags.propagatedMap[typeName]["count"] = 1;
                                    }
                                }
                                if (tags.combineMap[typeName] === undefined) {
                                    tags.combineMap[typeName] = val;
                                }
                            });
                            tags.self = _.sortBy(tags.self, "typeName");
                            tags.propagated = _.sortBy(tags.propagated, "typeName");
                            this.generateTag(tags);
                        } else {
                            this.generateTag([]);
                        }
                        if (collectionJSON.relationshipAttributes && collectionJSON.relationshipAttributes.meanings) {
                            this.generateTerm(collectionJSON.relationshipAttributes.meanings);
                        }
                        if (Globals.entityTypeConfList && _.isEmptyArray(Globals.entityTypeConfList)) {
                            this.editEntity = true;
                        } else {
                            if (_.contains(Globals.entityTypeConfList, collectionJSON.typeName)) {
                                this.editEntity = true;
                            }
                        }
                        if (collectionJSON.attributes && collectionJSON.attributes.columns) {
                            var valueSorted = _.sortBy(collectionJSON.attributes.columns, function(val) {
                                return val.attributes && val.attributes.position
                            });
                            collectionJSON.attributes.columns = valueSorted;
                        }
                    }
                    this.hideLoader();
                    var obj = {
                        entity: collectionJSON,
                        guid: this.id,
                        entityName: this.name,
                        typeHeaders: this.typeHeaders,
                        tags: tags,
                        entityDefCollection: this.entityDefCollection,
                        fetchCollection: this.fetchCollection.bind(that),
                        enumDefCollection: this.enumDefCollection,
                        classificationDefCollection: this.classificationDefCollection,
                        glossaryCollection: this.glossaryCollection,
                        businessMetadataCollection: this.activeEntityDef.get('businessAttributeDefs'),
                        searchVent: this.searchVent,
                        attributeDefs: (function() {
                            return that.getEntityDef(collectionJSON);
                        })(),
                        editEntity: this.editEntity || false
                    }
                    obj["renderAuditTableLayoutView"] = function() {
                        that.renderAuditTableLayoutView(obj);
                    };
                    this.renderEntityDetailTableLayoutView(obj);
                    this.renderEntityUserDefineView(obj);
                    this.renderEntityLabelDefineView(obj);
                    if (obj.businessMetadataCollection) {
                        this.renderEntityBusinessMetadataView(obj);
                    }
                    this.renderRelationshipLayoutView(obj);
                    this.renderAuditTableLayoutView(obj);
                    this.renderTagTableLayoutView(obj);
                    if (Globals.isTasksEnabled) { this.renderPendingTaskTableLayoutView(); }

                    // To render profile tab check for attribute "profileData" or typeName = "hive_db","hbase_namespace"
                    if (collectionJSON && (!_.isUndefined(collectionJSON.attributes['profileData']) || collectionJSON.typeName === "hive_db" || collectionJSON.typeName === "hbase_namespace")) {
                        if (collectionJSON.typeName === "hive_db" || collectionJSON.typeName === "hbase_namespace") {
                            this.$('.profileTab a').text("Tables")
                        }
                        this.$('.profileTab').show();
                        this.renderProfileLayoutView(_.extend({}, obj, {
                            entityDetail: collectionJSON.attributes,
                            profileData: collectionJSON.attributes.profileData,
                            typeName: collectionJSON.typeName,
                            value: that.value
                        }));
                    } else {
                        this.$('.profileTab').hide();
                        this.redirectToDefaultTab("profile");
                    }

                    if (this.activeEntityDef) {
                        //to display ReplicationAudit tab
                        if (collectionJSON && collectionJSON.typeName === "AtlasServer") {
                            this.$('.replicationTab').show();
                            this.renderReplicationAuditTableLayoutView(obj);
                        } else {
                            this.$('.replicationTab').hide();
                            this.redirectToDefaultTab("raudits");
                        }
                        // To render Schema check attribute "schemaElementsAttribute"
                        var schemaOptions = this.activeEntityDef.get('options');
                        var schemaElementsAttribute = schemaOptions && schemaOptions.schemaElementsAttribute;
                        if (!_.isEmpty(schemaElementsAttribute)) {
                            this.$('.schemaTable').show();
                            this.renderSchemaLayoutView(_.extend({}, obj, {
                                attribute: collectionJSON.relationshipAttributes[schemaElementsAttribute] || collectionJSON.attributes[schemaElementsAttribute]
                            }));
                        } else {
                            this.$('.schemaTable').hide();
                            this.redirectToDefaultTab("schema");
                        }

                        if (isLineageRender) {
                            this.$('.lineageGraph').show();
                            this.renderLineageLayoutView(_.extend(obj, {
                                processCheck: isProcess,
                                fetchCollection: this.fetchCollection.bind(this),
                            }));
                        } else {
                            this.$('.lineageGraph').hide();
                            this.redirectToDefaultTab("lineage");
                        }
                    }

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
                Utils.showTitleLoader(this.$('.page-title .fontLoader'), this.$('.entityDetail'));
                this.$('.fontLoader-relative').addClass('show'); // to show tab loader
            },
            redirectToDefaultTab: function(tabName) {
                var regionRef = null;
                switch (tabName) {
                    case "schema":
                        regionRef = this.RSchemaTableLayoutView;
                        break;
                    case "lineage":
                        regionRef = this.RLineageLayoutView;
                        break;
                    case "raudits":
                        regionRef = this.RReplicationAuditTableLayoutView;
                        break;
                    case "profile":
                        regionRef = this.RProfileLayoutView;
                        break;
                }
                if (regionRef) {
                    regionRef.destroy();
                    regionRef.$el.empty();
                }
                if (this.value && this.value.tabActive == tabName || this.$(".tab-content .tab-pane.active").attr("role") === tabName) {
                    Utils.setUrl({
                        url: Utils.getUrlState.getQueryUrl().queyParams[0],
                        urlParams: { tabActive: 'properties' },
                        mergeBrowserUrl: false,
                        trigger: true,
                        updateTabState: true
                    });
                }
            },
            manualRender: function(options) {
                if (options) {
                    var oldId = this.id;
                    _.extend(this, _.pick(options, 'value', 'id'));
                    if (this.id !== oldId) {
                        this.collection.url = UrlLinks.entitiesApiUrl({ guid: this.id, minExtInfo: true });
                        this.fetchCollection();
                    }
                    this.updateTab();
                }
            },
            updateTab: function() {
                if (this.value && this.value.tabActive) {
                    this.$('.nav.nav-tabs').find('[role="' + this.value.tabActive + '"]').addClass('active').siblings().removeClass('active');
                    this.$('.tab-content').find('[role="' + this.value.tabActive + '"]').addClass('active').siblings().removeClass('active');
                    $("html, body").animate({ scrollTop: (this.$('.tab-content').offset().top + 1200) }, 1000);
                }
            },
            onShow: function() {
                this.updateTab();
            },
            onDestroy: function() {
                if (!Utils.getUrlState.isDetailPage()) {
                    $('body').removeClass("detail-page");
                }
            },
            fetchCollection: function() {
                this.collection.fetch({ reset: true });
            },
            getEntityDef: function(entityObj) {
                if (this.activeEntityDef) {
                    var data = this.activeEntityDef.toJSON();
                    var attributeDefs = Utils.getNestedSuperTypeObj({
                        data: data,
                        attrMerge: true,
                        collection: this.entityDefCollection
                    });
                    return attributeDefs;
                } else {
                    return [];
                }
            },
            onClickTagCross: function(e) {
                var that = this,
                    tagName = $(e.currentTarget).parent().text(),
                    entityGuid = $(e.currentTarget).data("entityguid");
                CommonViewFunction.deleteTag(_.extend({}, {
                    guid: that.id,
                    associatedGuid: that.id != entityGuid ? entityGuid : null,
                    msg: "<div class='ellipsis-with-margin'>Remove: " + "<b>" + _.escape(tagName) + "</b> assignment from <b>" + this.name + "?</b></div>",
                    titleMessage: Messages.removeTag,
                    okText: "Remove",
                    showLoader: that.showLoader.bind(that),
                    hideLoader: that.hideLoader.bind(that),
                    tagName: tagName,
                    callback: function() {
                        if (that.searchVent) {
                            that.searchVent.trigger("Classification:Count:Update");
                        }
                        that.fetchCollection();
                    }
                }));
            },
            onClickTermCross: function(e) {
                var $el = $(e.currentTarget),
                    termGuid = $el.data('guid'),
                    termName = $el.text(),
                    that = this,
                    termObj = _.find(this.collection.first().get('entity').relationshipAttributes.meanings, { guid: termGuid });
                CommonViewFunction.removeCategoryTermAssociation({
                    termGuid: termGuid,
                    model: {
                        guid: that.id,
                        relationshipGuid: termObj.relationshipGuid
                    },
                    collection: that.glossaryCollection,
                    msg: "<div class='ellipsis-with-margin'>Remove: " + "<b>" + _.escape(termName) + "</b> assignment from <b>" + this.name + "?</b></div>",
                    titleMessage: Messages.glossary.removeTermfromEntity,
                    isEntityView: true,
                    buttonText: "Remove",
                    showLoader: that.showLoader.bind(that),
                    hideLoader: that.hideLoader.bind(that),
                    callback: function() {
                        that.fetchCollection();
                    }
                });
            },
            generateTag: function(tagObject) {
                var that = this,
                    tagData = "",
                    propagatedTagListData = "";
                _.each(tagObject.self, function(val) {
                    tagData += '<span class="btn btn-action btn-sm btn-icon btn-blue" data-id="tagClick"><span>' + val.typeName + '</span><i class="fa fa-close" data-id="deleteTag" data-type="tag" title="Remove Classification"></i></span>';
                });
                _.each(tagObject.propagatedMap, function(val, key) {
                    propagatedTagListData += '<span class="btn btn-action btn-sm btn-icon btn-blue"><span data-id="tagClick">' + val.typeName + '</span>' + (val.count > 1 ? '<span class="active" data-id="pTagCountClick">(' + val.count + ')</span>' : "") + '</span>';
                });
                propagatedTagListData !== "" ? this.ui.propagatedTagDiv.show() : this.ui.propagatedTagDiv.hide();
                this.ui.tagList.find("span.btn").remove();
                this.ui.propagatedTagList.find("span.btn").remove();
                this.ui.tagList.prepend(tagData);
                this.ui.propagatedTagList.html(propagatedTagListData);
            },
            generateTerm: function(data) {
                var that = this,
                    termData = "";
                _.each(data, function(val) {
                    termData += '<span class="btn btn-action btn-sm btn-icon btn-blue" data-id="termClick"><span>' + _.escape(val.displayText) + '</span><i class="' + (val.relationshipStatus == "ACTIVE" ? 'fa fa-close' : "") + '" data-id="deleteTerm" data-guid="' + val.guid + '" data-type="term" title="Remove Term"></i></span>';
                });
                this.ui.termList.find("span.btn").remove();
                this.ui.termList.prepend(termData);
            },
            hideLoader: function() {
                Utils.hideTitleLoader(this.$('.page-title .fontLoader'), this.$('.entityDetail'));
            },
            showLoader: function() {
                Utils.showTitleLoader(this.$('.page-title .fontLoader'), this.$('.entityDetail'));
            },
            onClickAddTagBtn: function(e) {
                var that = this;
                require(['views/tag/AddTagModalView'], function(AddTagModalView) {
                    var tagList = [];
                    _.map(that.entityObject.entity.classifications, function(obj) {
                        if (obj.entityGuid === that.id) {
                            tagList.push(obj.typeName);
                        }
                    });
                    var view = new AddTagModalView({
                        guid: that.id,
                        tagList: tagList,
                        callback: function() {
                            if (that.searchVent) {
                                that.searchVent.trigger("Classification:Count:Update");
                            }
                            that.fetchCollection();
                        },
                        showLoader: that.showLoader.bind(that),
                        hideLoader: that.hideLoader.bind(that),
                        collection: that.classificationDefCollection,
                        enumDefCollection: that.enumDefCollection
                    });
                    view.modal.on('ok', function() {
                        Utils.showTitleLoader(that.$('.page-title .fontLoader'), that.$('.entityDetail'));
                    });
                });
            },
            //This function checks for the lenght of Available terms and modal for adding terms is displayed accordingly.
            assignTermModalView: function(glossaryCollection, obj) {
                var that = this,
                    terms = 0;
                _.each(glossaryCollection.fullCollection.models, function(model) {
                    if (model.get('terms')) {
                        terms += model.get('terms').length;
                    };
                });
                if (terms) {
                    require(['views/glossary/AssignTermLayoutView'], function(AssignTermLayoutView) {
                        var view = new AssignTermLayoutView({
                            guid: obj.guid,
                            callback: function() {
                                that.fetchCollection();
                            },
                            associatedTerms: obj.associatedTerms,
                            showLoader: that.showLoader.bind(that),
                            hideLoader: that.hideLoader.bind(that),
                            glossaryCollection: glossaryCollection
                        });
                        view.modal.on('ok', function() {
                            Utils.showTitleLoader(that.$('.page-title .fontLoader'), that.$('.entityDetail'));
                        });
                    });
                } else {
                    Utils.notifyInfo({
                        content: "There are no available terms that can be associated with this entity"
                    });
                }
            },
            onClickAddTermBtn: function(e) {
                var that = this,
                    entityObj = this.collection.first().get('entity'),
                    glossaryData = null,
                    obj = {
                        guid: this.id,
                        associatedTerms: [],
                    };
                this.glossaryCollection.fetch({
                    success: function(glossaryCollection) {
                        that.assignTermModalView(glossaryCollection, obj);
                    },
                    reset: true,
                });
                if (entityObj && entityObj.relationshipAttributes && entityObj.relationshipAttributes.meanings) {
                    obj.associatedTerms = entityObj.relationshipAttributes.meanings;
                }
            },
            renderEntityDetailTableLayoutView: function(obj) {
                var that = this;
                require(['views/entity/EntityDetailTableLayoutView'], function(EntityDetailTableLayoutView) {
                    that.REntityDetailTableLayoutView.show(new EntityDetailTableLayoutView(obj));
                });
            },
            renderEntityUserDefineView: function(obj) {
                var that = this;
                require(['views/entity/EntityUserDefineView'], function(EntityUserDefineView) {
                    that.REntityUserDefineView.show(new EntityUserDefineView(obj));
                });
            },
            renderEntityLabelDefineView: function(obj) {
                var that = this;
                require(['views/entity/EntityLabelDefineView'], function(EntityLabelDefineView) {
                    that.REntityLabelDefineView.show(new EntityLabelDefineView(obj));
                });
            },
            renderEntityBusinessMetadataView: function(obj) {
                var that = this;
                require(['views/entity/EntityBusinessMetaDataView'], function(EntityBusinessMetaDataView) {
                    that.REntityBusinessMetadataView.show(new EntityBusinessMetaDataView(obj));
                });
            },
            renderTagTableLayoutView: function(obj) {
                var that = this;
                require(['views/tag/TagDetailTableLayoutView'], function(TagDetailTableLayoutView) {
                    that.RTagTableLayoutView.show(new TagDetailTableLayoutView(obj));
                });
            },
            renderPendingTaskTableLayoutView: function() {
                var that = this;
                require(['views/detail_page/PendingTaskTableLayoutView'], function(PendingTaskTableLayoutView) {
                    that.RPendingTaskTableLayoutView.show(new PendingTaskTableLayoutView());
                });
            },
            renderLineageLayoutView: function(obj) {
                var that = this;
                require(['views/graph/LineageLayoutView'], function(LineageLayoutView) {
                    that.RLineageLayoutView.show(new LineageLayoutView(obj));
                });
            },
            renderRelationshipLayoutView: function(obj) {
                var that = this;
                require(['views/graph/RelationshipLayoutView'], function(RelationshipLayoutView) {
                    that.RRelationshipLayoutView.show(new RelationshipLayoutView(obj));
                });
            },
            renderSchemaLayoutView: function(obj) {
                var that = this;
                require(['views/schema/SchemaLayoutView'], function(SchemaLayoutView) {
                    that.RSchemaTableLayoutView.show(new SchemaLayoutView(obj));
                });
            },
            renderAuditTableLayoutView: function(obj) {
                var that = this;
                require(['views/audit/AuditTableLayoutView'], function(AuditTableLayoutView) {
                    that.RAuditTableLayoutView.show(new AuditTableLayoutView(obj));
                });
            },
            renderReplicationAuditTableLayoutView: function(obj) {
                var that = this;
                require(['views/audit/ReplicationAuditTableLayoutView'], function(ReplicationAuditTableLayoutView) {
                    that.RReplicationAuditTableLayoutView.show(new ReplicationAuditTableLayoutView(obj));
                });
            },
            renderProfileLayoutView: function(obj) {
                var that = this;
                require(['views/profile/ProfileLayoutView'], function(ProfileLayoutView) {
                    that.RProfileLayoutView.show(new ProfileLayoutView(obj));
                });
            }
        });
    return DetailPageLayoutView;
});