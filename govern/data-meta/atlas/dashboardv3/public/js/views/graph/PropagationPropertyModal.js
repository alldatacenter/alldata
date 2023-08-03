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
    'hbs!tmpl/graph/PropagationPropertyModalView_tmpl',
    'models/VRelationship',
    'models/VEntity',
    'modules/Modal',
    'utils/Utils',
    'utils/UrlLinks',
    'utils/Messages'
], function(require, PropagationPropertyModalViewTmpl, VRelationship, VEntity, Modal, Utils, UrlLinks, Messages) {
    'use strict';

    var PropogationPropertyModal = Backbone.Marionette.CompositeView.extend({
        template: PropagationPropertyModalViewTmpl,
        templateHelpers: function() {},
        regions: {},
        ui: {
            propagationOptions: '[data-id="propagationOptions"]',
            edgeDetailName: '[data-id="edgeDetailName"]',
            propagationState: "[data-id='propagationState']",
            entityClick: "[data-id='entityClick']",
            editPropagationType: 'input[name="editPropagationType"]',
            PropagatedClassificationTable: "[data-id='PropagatedClassificationTable']"

        },
        events: function() {
            var events = {},
                that = this;
            events["change " + this.ui.propagationOptions] = function() {
                this.modalEdited = true;
                this.modal.$el.find('button.ok').attr("disabled", false);
            };
            events["click " + this.ui.editPropagationType] = function(e) {
                if (this.modalEdited === true) {
                    e.preventDefault();
                    that.notifyModal();
                }
            };
            events["change " + this.ui.editPropagationType] = function(e) {
                if (e.target.checked) {
                    this.showPropagatedClassificationTable();
                    this.viewType = "table";
                } else {
                    this.showEditPropagation();
                    this.viewType = "flow";
                }
            };
            events["click " + this.ui.entityClick] = function(e) {
                var that = this,
                    url = "",
                    notifyObj = {
                        modal: true,
                        text: "Are you sure you want to navigate away from this page ?",
                        ok: function(argument) {
                            that.modal.trigger('cancel');
                            Utils.setUrl({
                                url: url,
                                mergeBrowserUrl: false,
                                trigger: true
                            });

                        },
                        cancel: function(argument) {}
                    },
                    $el = $(e.currentTarget),
                    guid = $el.parents('tr').data('entityguid');
                if ($el.hasClass('entityName')) {
                    url = '#!/detailPage/' + guid + '?tabActive=lineage';
                } else {
                    url = '#!/tag/tagAttribute/' + $el.data('name');
                }
                Utils.notifyConfirm(notifyObj);
            };
            events["change " + this.ui.propagationState] = function(e) {
                this.modalEdited = true;
                this.modal.$el.find('button.ok').attr("disabled", false);
                var $el = $(e.currentTarget).parents('tr'),
                    entityguid = $el.data("entityguid"),
                    classificationName = $el.find('[data-name]').data('name');
                if (e.target.checked) {
                    this.propagatedClassifications = _.reject(this.propagatedClassifications, function(val, key) {
                        if (val.entityGuid == entityguid && classificationName == val.typeName) {
                            that.blockedPropagatedClassifications.push(val);
                            return true;
                        }
                    });
                } else {
                    this.blockedPropagatedClassifications = _.reject(this.blockedPropagatedClassifications, function(val, key) {
                        if (val.entityGuid == entityguid && classificationName == val.typeName) {
                            that.propagatedClassifications.push(val);
                            return true;
                        }
                    });
                }
            };
            return events;
        },
        /**
         * intialize a new PropogationPropertyModal Layout
         * @constructs
         */
        initialize: function(options) {
            _.extend(this, _.pick(options, 'edgeInfo', 'relationshipId', 'lineageData', 'apiGuid', 'detailPageFetchCollection'));
            this.entityModel = new VRelationship();
            this.VEntityModel = new VEntity();
            this.modalEdited = false;
            this.viewType = 'flow';
            var that = this,
                modalObj = {
                    title: 'Enable/Disable Propagation',
                    content: this,
                    okText: 'Update',
                    okCloses: false,
                    cancelText: "Cancel",
                    mainClass: 'modal-lg',
                    allowCancel: true,
                };

            this.modal = new Modal(modalObj)
            this.modal.open();
            this.modal.$el.find('button.ok').attr("disabled", true);
            this.on('ok', function() {
                that.updateRelation();
            });
            this.on('closeModal', function() {
                this.modal.trigger('cancel');
            });
            this.updateEdgeView(this.edgeInfo);
        },

        onRender: function() {},
        updateEdgeView: function(options) {
            var obj = options,
                fromEntity = this.lineageData.guidEntityMap[obj.fromEntityId],
                toEntity = this.lineageData.guidEntityMap[obj.toEntityId];
            if (fromEntity && toEntity) {
                this.ui.edgeDetailName.html(_.escape(fromEntity.displayText) + " <span class='navigation-font'><i class='fa fa-long-arrow-right fa-color'></i></span> " + _.escape(toEntity.displayText));
            }
            if (obj && obj.relationshipId) {
                this.showLoader();
                this.getEdgeEntity({ id: obj.relationshipId, from: fromEntity, to: toEntity });
            }
        },
        getPropagationFlow: function(options) {
            var relationshipData = options.relationshipData,
                graphData = options.graphData,
                propagateTags = relationshipData.propagateTags;
            if (relationshipData.end1) {
                if (relationshipData.end1.guid == graphData.from.guid || propagateTags == "BOTH" || propagateTags == "NONE") {
                    return propagateTags;
                } else {
                    return propagateTags == "ONE_TO_TWO" ? "TWO_TO_ONE" : "ONE_TO_TWO";
                }
            } else {
                return propagateTags;
            }
        },
        getEdgeEntity: function(options) {
            var that = this,
                id = options.id,
                from = options.from,
                to = options.to,
                enableOtherFlow = function(relationshipObj) {
                    var isTwoToOne = false;
                    if (relationshipObj.propagateTags == "BOTH") {
                        that.ui.propagationOptions.find('.both').show();
                    } else {
                        that.ui.propagationOptions.find('.both').hide();
                        if (that.edgeInfo.fromEntityId != relationshipObj.end1.guid && relationshipObj.propagateTags == "ONE_TO_TWO") {
                            isTwoToOne = true;
                        } else if (that.edgeInfo.fromEntityId == relationshipObj.end1.guid && relationshipObj.propagateTags == "TWO_TO_ONE") {
                            isTwoToOne = true;
                        }
                        if (isTwoToOne) {
                            that.ui.propagationOptions.find('.TWO_TO_ONE').show();
                        } else {
                            that.ui.propagationOptions.find('.TWO_TO_ONE').hide();
                        }
                    }
                },
                updateValue = function(relationshipData) {
                    var relationshipObj = relationshipData.relationship;
                    if (relationshipObj) {
                        that.$("input[name='propagateRelation'][value=" + that.getPropagationFlow({
                            "relationshipData": relationshipObj,
                            "graphData": options
                        }) + "]").prop('checked', true);
                        enableOtherFlow(relationshipObj);
                        that.showBlockedClassificationTable(relationshipData);
                        that.hideLoader({ buttonDisabled: true });
                    }
                }
            this.ui.propagationOptions.find('li label>span.fromName').text(from.typeName);
            this.ui.propagationOptions.find('li label>span.toName').text(to.typeName);

            if (id === this.ui.propagationOptions.attr("entity-id")) {
                return;
            }
            this.ui.propagationOptions.attr("entity-id", id);
            if (this.apiGuid[id]) {
                updateValue(this.apiGuid[id]);
            } else {
                if (this.edgeCall && this.edgeCall.readyState != 4) {
                    this.edgeCall.abort();
                }
                this.edgeCall = this.entityModel.getRelationship(id, {
                    success: function(relationshipData) {
                        that.apiGuid[relationshipData.relationship.guid] = relationshipData;
                        updateValue(relationshipData);
                    },
                    cust_error: function() {
                        that.hideLoader();
                    }
                });
            }
        },
        updateRelation: function() {
            var that = this,
                entityId = that.ui.propagationOptions.attr('entity-id'),
                PropagationValue = this.$("input[name='propagateRelation']:checked").val(),
                relationshipProp = {};
            this.ui.propagationOptions.attr("propagation", PropagationValue);
            if (this.viewType == "flow") {
                relationshipProp = {
                    "propagateTags": that.getPropagationFlow({
                        "relationshipData": _.extend({}, this.apiGuid[entityId].relationship, { 'propagateTags': PropagationValue }),
                        "graphData": { from: { guid: this.edgeInfo.fromEntityId } }
                    })
                }
            } else {
                relationshipProp = {
                    "blockedPropagatedClassifications": this.blockedPropagatedClassifications,
                    "propagatedClassifications": this.propagatedClassifications
                };
            }
            this.showLoader();
            this.entityModel.saveRelationship({
                data: JSON.stringify(_.extend({}, that.apiGuid[entityId].relationship, relationshipProp)),
                success: function(relationshipData) {
                    if (relationshipData) {
                        that.hideLoader({ buttonDisabled: true });
                        that.modal.trigger('cancel');
                        that.apiGuid[relationshipData.guid] = relationshipData;
                        that.detailPageFetchCollection();
                        Utils.notifySuccess({
                            content: "Propagation flow updated succesfully."
                        });
                    }
                },
                cust_error: function() {
                    that.hideLoader();
                }
            });
        },
        showBlockedClassificationTable: function(options) {
            var that = this,
                propagationStringValue = "",
                classificationTableValue = "",
                relationship = options.relationship,
                referredEntities = options.referredEntities,
                getEntityName = function(guid) {
                    var entityObj = referredEntities[guid],
                        name = guid;
                    if (entityObj) {
                        name = Utils.getName(entityObj) + " (" + entityObj.typeName + ")";
                    }
                    return "<a class='entityName' data-id='entityClick'>" + name + "</a>";
                },
                getTableRow = function(options) {
                    var val = options.val,
                        fromBlockClassification = options.fromBlockClassification;
                    return "<tr data-entityguid=" + val.entityGuid + "><td class='text-center w30'><a class='classificationName' data-id='entityClick' title='" + val.typeName + "' data-name='" + val.typeName + "''>" + val.typeName + "</a></td><td class='text-center'>" + getEntityName(val.entityGuid) + "</td><td class='text-center w30'><input type='checkbox' " + (fromBlockClassification ? "checked" : "") + " data-id='propagationState' class='input'></td></tr>";
                };

            this.blockedPropagatedClassifications = _.isUndefined(relationship.blockedPropagatedClassifications) ? [] : _.clone(relationship.blockedPropagatedClassifications);
            this.propagatedClassifications = _.isUndefined(relationship.propagatedClassifications) ? [] : _.clone(relationship.propagatedClassifications);
            _.each(this.blockedPropagatedClassifications, function(val, key) {
                propagationStringValue += getTableRow({ "val": val, fromBlockClassification: true });
            });
            _.each(this.propagatedClassifications, function(val, key) {
                propagationStringValue += getTableRow({ "val": val, fromBlockClassification: false });
            });

            classificationTableValue = "<table class='attriTable'><tr><th class='w30'>Classification</th><th>Entity Name</th><th class='w30'>Block Propagatation</th>" + propagationStringValue + "</table>";

            this.ui.PropagatedClassificationTable.append(_.isEmpty(propagationStringValue) ? "No Records Found." : classificationTableValue);
        },
        showLoader: function() {
            this.modal.$el.find('button.ok').showButtonLoader();
            this.$('.overlay').removeClass('hide').addClass('show');
        },
        hideLoader: function(options) {
            var buttonDisabled = options && options.buttonDisabled;
            this.modal.$el.find('button.ok').hideButtonLoader();
            this.modal.$el.find('button.ok').attr("disabled", buttonDisabled ? buttonDisabled : false);
            this.$('.overlay').removeClass('show').addClass('hide');
        },
        notifyModal: function(options) {
            var that = this,
                notifyObj = {
                    modal: true,
                    text: "It looks like you have edited something. If you leave before saving, your changes will be lost.",
                    ok: function(argument) {
                        that.viewType = that.ui.editPropagationType.is(":checked") ? "flow" : "table";
                        that.ui.editPropagationType.prop("checked", that.viewType === "flow" ? false : true).trigger("change");
                        that.modal.$el.find('button.ok').attr("disabled", true);
                    },
                    cancel: function(argument) {
                        that.viewType = that.ui.editPropagationType.is(":checked") ? "table" : "flow";
                    }
                };
            Utils.notifyConfirm(notifyObj);
        },
        showEditPropagation: function() {
            this.$('.editPropagation').show();
            this.$('.propagatedClassificationTable').hide();
            this.modal.$el.find('.modal-title').text("Enable/Disable Propagation");
        },
        showPropagatedClassificationTable: function() {
            this.$('.editPropagation').hide();
            this.$('.propagatedClassificationTable').show();
            this.modal.$el.find('.modal-title').text("Select Classifications to Block Propagation");
        }

    });
    return PropogationPropertyModal;
});