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
    'hbs!tmpl/glossary/AssignTermLayoutView_tmpl',
    'utils/Utils',
    'utils/Enums',
    'utils/Messages',
    'utils/UrlLinks',
    'modules/Modal',
    'jquery-steps'
], function(require, Backbone, AssignTermLayoutViewTmpl, Utils, Enums, Messages, UrlLinks, Modal) {

    var AssignTermLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends AssignTermLayoutView */
        {
            _viewName: 'AssignTermLayoutView',

            template: AssignTermLayoutViewTmpl,

            templateHelpers: function() {
                return {
                    isAttributeRelationView: this.isAttributeRelationView,
                    selectedTermAttributeList: Enums.termRelationAttributeList[this.selectedTermAttribute]
                };
            },

            /** Layout sub regions */
            regions: {
                RGlossaryTree: "#r_glossaryTree"
            },

            /** ui selector cache */
            ui: {
                wizard: '[data-id="wizard"]'
            },
            /** ui events hash */
            events: function() {
                var events = {};
                return events;
            },
            /**
             * intialize a new AssignTermLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'glossaryCollection', 'guid', 'callback', 'hideLoader', 'isCategoryView', 'categoryData', 'isTermView', 'termData', 'isAttributeRelationView', 'selectedTermAttribute', 'associatedTerms', 'multiple'));
                var that = this;
                this.options = options;
                if (!this.isCategoryView && !this.isTermView && !this.isAttributeRelationView) {
                    this.isEntityView = true;
                }
                this.glossary = {
                    selectedItem: {}
                }
                var title = "";
                if (this.isCategoryView || this.isEntityView) {
                    title = ("Assign term to " + (this.isCategoryView ? "Category" : "entity"))
                } else if (this.isAttributeRelationView) {
                    title = "Assign term to " + this.selectedTermAttribute;
                } else {
                    title = "Assign Category to term";
                }
                this.modal = new Modal({
                    "title": title,
                    "content": this,
                    "cancelText": "Cancel",
                    "okText": "Assign",
                    "allowCancel": true,
                    "showFooter": this.isAttributeRelationView ? false : true,
                    "mainClass": "wizard-modal",
                    "okCloses": false
                });
                this.modal.open();
                this.modal.$el.find('button.ok').attr("disabled", true);
                this.modal.on('closeModal', function() {
                    that.modal.trigger('cancel');
                    if (that.assignTermError && that.hideLoader) {
                        that.hideLoader();
                    }
                    if (options.onModalClose) {
                        options.onModalClose()
                    }
                });
                this.modal.on('ok', function() {
                    that.assignTerm();
                });
                this.bindEvents();
            },
            bindEvents: function() {
                this.listenTo(this.glossaryCollection, "node_selected", function(skip) {
                    this.modal.$el.find('button.ok').attr("disabled", false);
                }, this);
            },
            onRender: function() {
                this.renderGlossaryTree();
                var that = this;
                if (this.isAttributeRelationView) {
                    this.ui.wizard.steps({
                        headerTag: "h3",
                        bodyTag: "section",
                        transitionEffect: "slideLeft",
                        autoFocus: true,
                        enableCancelButton: true,
                        transitionEffect: $.fn.steps.transitionEffect.none,
                        transitionEffectSpeed: 200,
                        labels: {
                            cancel: "Cancel",
                            finish: "Assign",
                            next: "Next",
                            previous: "Previous",
                            loading: "Loading ..."
                        },
                        onStepChanging: function(event, currentIndex, newIndex) {
                            var isMatch = that.glossary.selectedItem.type == "GlossaryTerm";
                            if (!isMatch) {
                                Utils.notifyWarn({
                                    content: "Please select Term for association"
                                });
                            }
                            return isMatch
                        },
                        onFinished: function(event, currentIndex) {
                            var $assignBtn = $(this).find('a[href="#finish"]');
                            if (!$assignBtn.attr('disabled')) {
                                $assignBtn.attr('disabled', true).showButtonLoader();
                                $assignBtn.parent().attr('aria-disabled', 'true').addClass('disabled');
                                that.assignTerm();
                            }
                        },
                        onCanceled: function(event) {
                            that.modal.trigger('cancel');
                        },
                    });
                }
            },
            assignTerm: function() {
                this.assignTermError = false;
                var that = this,
                    data = [],
                    termAttributeFormData = [],
                    selectedItem = this.glossary.selectedItem,
                    selectedGuid = selectedItem.guid,
                    termName = selectedItem.text,
                    ajaxOptions = {
                        success: function(rModel, response) {
                            Utils.notifySuccess({
                                content: (that.isCategoryView || that.isEntityView || that.isAttributeRelationView ? "Term" : "Category") + " is associated successfully "
                            });
                            if (that.callback) {
                                that.callback();
                            }
                        },
                        cust_error: function() {
                            var $assignBtn = that.$el.find('a[href="#finish"]');
                            $assignBtn.removeAttr('disabled').hideButtonLoader();
                            $assignBtn.parent().attr('aria-disabled', 'false').removeClass('disabled');
                            that.assignTermError = true;
                        },
                        complete: function() {
                            that.modal.trigger('closeModal');
                        }
                    },
                    model = new this.glossaryCollection.model();
                if (this.isCategoryView) {
                    data = $.extend(true, {}, this.categoryData);
                    if (data.terms) {
                        data.terms.push({ "termGuid": selectedGuid });
                    } else {
                        data.terms = [{ "termGuid": selectedGuid }];
                    }
                    model.assignTermToCategory(_.extend(ajaxOptions, { data: JSON.stringify(data), guid: data.guid }));
                } else if (this.isTermView) {
                    data = $.extend(true, {}, this.termData);
                    if (data.categories) {
                        data.categories.push({ "categoryGuid": selectedGuid });
                    } else {
                        data.categories = [{ "categoryGuid": selectedGuid }];
                    }
                    model.assignCategoryToTerm(_.extend(ajaxOptions, { data: JSON.stringify(data), guid: data.guid }));
                } else if (this.isAttributeRelationView) {
                    termAttributeFormData = this.$('[data-id="termAttributeForm"]').serializeArray().reduce(function(obj, item) {
                            obj[item.name] = item.value;
                            return obj;
                        }, {}),
                        data = $.extend(true, {}, this.termData);
                    if (data[this.selectedTermAttribute]) {
                        data[this.selectedTermAttribute].push(_.extend({ "termGuid": selectedGuid }, termAttributeFormData));
                    } else {
                        data[this.selectedTermAttribute] = [_.extend({ "termGuid": selectedGuid }, termAttributeFormData)];
                    }
                    model.assignTermToAttributes(_.extend(ajaxOptions, { data: JSON.stringify(data), guid: data.guid }));
                } else {
                    var deletedEntity = [],
                        skipEntity = [];

                    if (this.multiple) {
                        _.each(that.multiple, function(entity, i) {
                            var name = Utils.getName(entity.model);
                            if (Enums.entityStateReadOnly[entity.model.status]) {
                                deletedEntity.push(name);
                            } else {
                                if (_.indexOf((entity.model.meaningNames || _.pluck(entity.model.meanings, 'displayText')), termName) === -1) {
                                    data.push({ guid: entity.model.guid })
                                } else {
                                    skipEntity.push(name);
                                }
                            }
                        });
                        if (deletedEntity.length) {
                            Utils.notifyError({
                                html: true,
                                content: "<b>" + deletedEntity.join(', ') +
                                    "</b> " + (deletedEntity.length === 1 ? "entity " : "entities ") +
                                    Messages.assignTermDeletedEntity
                            });
                            that.modal.close();
                        }
                    } else {
                        data.push({ "guid": that.guid });
                    }
                    if (skipEntity.length) {
                        var text = "<b>" + skipEntity.length + " of " + that.multiple.length +
                            "</b> entities selected have already been associated with <b>" + termName +
                            "</b> term, Do you want to associate the term with other entities ?",
                            removeCancelButton = false;
                        if ((skipEntity.length + deletedEntity.length) === that.multiple.length) {
                            text = (skipEntity.length > 1 ? "All selected" : "Selected") + " entities have already been associated with <b>" + termName + "</b> term";
                            removeCancelButton = true;
                        }
                        var notifyObj = {
                            text: text,
                            modal: true,
                            ok: function(argument) {
                                if (data.length) {
                                    model.assignTermToEntity(selectedGuid, _.extend(ajaxOptions, { data: JSON.stringify(data) }));
                                }
                            },
                            cancel: function(argument) {}
                        }
                        if (removeCancelButton) {
                            notifyObj['confirm'] = {
                                confirm: true,
                                buttons: [{
                                        text: 'Ok',
                                        addClass: 'btn-atlas btn-md',
                                        click: function(notice) {
                                            notice.remove();
                                        }
                                    },
                                    null
                                ]
                            }
                        }
                        Utils.notifyConfirm(notifyObj);
                    } else if (data.length) {
                        model.assignTermToEntity(selectedGuid, _.extend(ajaxOptions, { data: JSON.stringify(data) }));
                    }
                }
            },
            renderGlossaryTree: function() {
                var that = this;
                require(['views/glossary/GlossaryLayoutView'], function(GlossaryLayoutView) {
                    that.RGlossaryTree.show(new GlossaryLayoutView(_.extend({
                        "isAssignTermView": that.isCategoryView,
                        "isAssignCategoryView": that.isTermView,
                        "isAssignEntityView": that.isEntityView,
                        "isAssignAttributeRelationView": that.isAttributeRelationView,
                        "glossary": that.glossary,
                        "associatedTerms": that.associatedTerms
                    }, that.options)));
                });
            },
        });
    return AssignTermLayoutView;
});