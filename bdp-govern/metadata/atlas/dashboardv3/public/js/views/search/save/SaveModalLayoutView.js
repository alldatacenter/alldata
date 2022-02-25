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
    'hbs!tmpl/search/save/SaveModalLayoutView_tmpl',
    'utils/Utils',
    'modules/Modal',
    'utils/UrlLinks',
    'platform',
    'models/VSearch',
    "collection/VSearchList",
    'utils/CommonViewFunction',
    'utils/Messages'
], function(require, Backbone, SaveModalLayoutViewTmpl, Utils, Modal, UrlLinks, platform, VSearch, VSearchList, CommonViewFunction, Messages) {


    var SaveModalLayoutView = Backbone.Marionette.LayoutView.extend({
        _viewName: 'SaveModalLayoutView',
        template: SaveModalLayoutViewTmpl,
        regions: {},
        ui: {
            saveAsName: "[data-id='saveAsName']"
        },
        templateHelpers: function() {
            return {
                selectedModel: this.selectedModel ? this.selectedModel.toJSON() : null,
                rename: this.rename
            };
        },
        events: function() {
            var events = {};
            return events;
        },
        initialize: function(options) {
            var that = this;
            _.extend(this, _.pick(options, 'rename', 'selectedModel', 'collection', 'getValue', 'isBasic', 'saveObj'));
            this.model = new VSearch();
            this.saveSearchCollection = new VSearchList();
            this.saveSearchCollection.url = UrlLinks.saveSearchApiUrl();
            this.saveSearchCollection.fullCollection.comparator = function(model) {
                return getModelName(model);
            }

            function getModelName(model) {
                if (model.get('name')) {
                    return model.get('name').toLowerCase();
                }
            };
            if (this.saveObj) {
                this.onCreateButton();
            } else {
                this.modal = modal = new Modal({
                    titleHtml: true,
                    title: '<span>' + (this.selectedModel && this.rename ? 'Rename' : 'Save') + (this.isBasic ? " Basic" : " Advanced") + ' Custom Filter</span>',
                    content: this,
                    cancelText: "Cancel",
                    okCloses: false,
                    okText: this.selectedModel ? 'Update' : 'Save',
                    allowCancel: true
                });
                this.modal.open();
                modal.$el.find('button.ok').attr("disabled", "true");
                modal.on('ok', function() {
                    modal.$el.find('button.ok').attr("disabled", "true");
                    that.onCreateButton();
                });
                modal.on('closeModal', function() {
                    modal.trigger('cancel');
                });
            }
        },
        hideLoader: function() {
            this.$el.find("form").removeClass("hide");
            this.$el.find(".fontLoader").removeClass("show");
        },
        onRender: function() {
            if (this.rename == true) {
                this.hideLoader();
            } else {
                var that = this;
                this.saveSearchCollection.fetch({
                    success: function(collection, data) {
                        that.saveSearchCollection.fullCollection.reset(_.where(data, { searchType: that.isBasic ? "BASIC" : "ADVANCED" }));
                        var options = "";
                        that.saveSearchCollection.fullCollection.each(function(model) {
                            options += '<option value="' + model.get("name") + '">' + model.get("name") + '</option>';
                        })
                        that.ui.saveAsName.append(options);
                        that.ui.saveAsName.val("");
                        that.ui.saveAsName.select2({
                            placeholder: "Enter filter name ",
                            allowClear: false,
                            tags: true,
                            multiple: false,
                            templateResult: function(state) {
                                if (!state.id) {
                                    return state.text;
                                }
                                if (!state.element) {
                                    return $("<span><span class='option-title-light'>New:</span> <strong>" + _.escape(state.text) + "</strong></span>");
                                } else {
                                    return $("<span><span class='option-title-light'>Update:</span> <strong>" + _.escape(state.text) + "</strong></span>");
                                }
                            }
                        }).on("change", function() {
                            var val = that.ui.saveAsName.val();
                            if (val.length) {
                                that.selectedModel = that.saveSearchCollection.fullCollection.find({ name: val });
                                if (that.selectedModel) {
                                    that.modal.$el.find('button.ok').text("Save As");
                                } else {
                                    that.modal.$el.find('button.ok').text("Save");
                                }
                                that.modal.$el.find('button.ok').removeAttr("disabled");
                            } else {
                                that.modal.$el.find('button.ok').attr("disabled", "true");
                                that.selectedModel = null;
                            }
                        });
                    },
                    silent: true
                });
                this.hideLoader();
            }
        },
        onCreateButton: function() {
            var that = this,
                obj = { name: this.ui.saveAsName.val() || null, value: this.getValue() };
            if (this.saveObj) {
                // Save search Filter
                _.extend(obj, this.saveObj);
            }
            var saveObj = CommonViewFunction.generateObjectForSaveSearchApi(obj);
            if (this.selectedModel) {
                // Update Name only.
                var selectedModel = this.selectedModel.toJSON();
                if (this.rename !== true) {
                    _.extend(selectedModel.searchParameters, saveObj.searchParameters);
                }
                selectedModel.name = obj.name;
                saveObj = selectedModel;
            } else {
                if (this.isBasic) {
                    saveObj['searchType'] = "BASIC";
                } else {
                    saveObj['searchType'] = "ADVANCED";
                }
            }
            this.model.urlRoot = UrlLinks.saveSearchApiUrl();
            this.model.save(saveObj, {
                type: (saveObj.guid ? 'PUT' : 'POST'),
                success: function(model, data) {
                    if (that.collection) {
                        if (saveObj.guid) {
                            var collectionRef = that.collection.find({ guid: data.guid });
                            if (collectionRef) {
                                collectionRef.set(data);
                            }
                            Utils.notifySuccess({
                                content: obj.name + Messages.getAbbreviationMsg(false, 'editSuccessMessage')
                            });
                        } else {
                            that.collection.add(data);
                            Utils.notifySuccess({
                                content: obj.name + Messages.getAbbreviationMsg(false, 'addSuccessMessage')
                            });
                        }
                    }
                    if (that.callback) {
                        that.callback();
                    }

                }
            });
            if (this.modal) {
                this.modal.trigger('cancel');
            }
        }
    });
    return SaveModalLayoutView;
});