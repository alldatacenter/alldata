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

define(['require', 'utils/Utils', 'lossless-json', 'marionette', 'backgrid', 'asBreadcrumbs', 'jquery-placeholder'], function(require, Utils, LosslessJSON) {
    'use strict';

    Backbone.$.ajaxSetup({
        cache: false
    });

    var oldBackboneSync = Backbone.sync;
    Backbone.sync = function(method, model, options) {
        var that = this;
        if (options.queryParam) {
            var generateQueryParam = $.param(options.queryParam);
            if (options.url.indexOf('?') !== -1) {
                options.url = options.url + "&" + generateQueryParam;
            } else {
                options.url = options.url + "?" + generateQueryParam;
            }
        }
        return oldBackboneSync.apply(this, [method, model,
            _.extend(options, {
                error: function(response) {
                    Utils.defaultErrorHandler(that, response, options);
                    that.trigger("error", that, response);
                    if (options.cust_error) {
                        options.cust_error(that, response);
                    }
                },
                converters: _.extend($.ajaxSettings.converters, {
                    "text json": function(data) {
                        try {
                            return LosslessJSON.parse(data, function(k, v) { try { return (v.isLosslessNumber) ? v.valueOf() : v } catch (err) { return v.value } });
                        } catch (err) {
                            if (err.name.toLowerCase() === "syntaxerror" && data.length > 0 && data.indexOf("<html") > -1) { // to handel logout for multile windows
                                var redirectUrl = window.location.origin + window.location.pathname;
                                window.location = redirectUrl.substring(0, redirectUrl.lastIndexOf("/"));
                            } else {
                                return $.parseJSON(data);
                            }
                        }
                    }
                })
            })
        ]);
    }

    String.prototype.trunc = String.prototype.trunc ||
        function(n) {
            return (this.length > n) ? this.substr(0, n - 1) + '...' : this;
        };
    String.prototype.capitalize = function() {
        return this.charAt(0).toUpperCase() + this.slice(1);
    }

    /*
     * Overriding default sortType
     */
    Backgrid.Column.prototype.defaults.sortType = "toggle";

    /*
     * Overriding Cell for adding custom className to Cell i.e <td>
     */
    var cellInit = Backgrid.Cell.prototype.initialize;
    Backgrid.Cell.prototype.initialize = function() {
        cellInit.apply(this, arguments);
        var className = this.column.get('className');
        var rowClassName = this.column.get('rowClassName');
        if (rowClassName) this.$el.addClass(rowClassName);
        if (className) this.$el.addClass(className);
    }
    /*
     * Overriding Cell for adding custom width to Cell i.e <td>
     */
    Backgrid.HeaderRow = Backgrid.HeaderRow.extend({
        render: function() {
            var that = this;
            Backgrid.HeaderRow.__super__.render.apply(this, arguments);
            _.each(this.columns.models, function(modelValue) {
                var elAttr = modelValue.get('elAttr'),
                    elAttrObj = null;
                if (elAttr) {
                    if (_.isFunction(elAttr)) {
                        elAttrObj = elAttr(modelValue);
                    } else if (_.isObject(elAttr)) {
                        if (!_.isArray(elAttr)) {
                            elAttrObj = [elAttr];
                        } else {
                            elAttrObj = elAttr;
                        }
                    }
                    _.each(elAttrObj, function(val) {
                        that.$el.find('.' + modelValue.get('name')).data(val);
                    });
                }
                if (modelValue.get('width')) that.$el.find('.' + modelValue.get('name')).css('min-width', modelValue.get('width') + 'px');
                if (modelValue.get('fixWidth')) that.$el.find('.' + modelValue.get('name')).css('width', modelValue.get('fixWidth') + 'px');
                if (modelValue.get('toolTip')) that.$el.find('.' + modelValue.get('name')).attr('title', modelValue.get('toolTip'));
                if (modelValue.get('headerClassName')) that.$el.find('.' + modelValue.get('name').replace(".", "\\.")).addClass(modelValue.get('headerClassName'));
            });
            return this;
        }
    });
    /*
     * HtmlCell renders any html code
     * @class Backgrid.HtmlCell
     * @extends Backgrid.Cell
     */
    var HtmlCell = Backgrid.HtmlCell = Backgrid.Cell.extend({

        /** @property */
        className: "html-cell",

        render: function() {
            this.$el.empty();
            var rawValue = this.model.get(this.column.get("name"));
            var formattedValue = this.formatter.fromRaw(rawValue, this.model);
            this.$el.append(formattedValue);
            this.delegateEvents();
            return this;
        }
    });


    /*
     * Backgrid Header render listener when resize or re-ordered
     */
    var BackgridHeaderInitializeMethod = function(options) {
        this.columns = options.columns;
        if (!(this.columns instanceof Backbone.Collection)) {
            this.columns = new Backgrid.Columns(this.columns);
        }
        this.createHeaderRow();

        this.listenTo(this.columns, "sort", _.bind(function() {
            this.createHeaderRow();
            this.render();
        }, this));
    };

    /**
     * Sets up a new headerRow and attaches it to the view
     * Tested with backgrid 0.3.5
     */
    var BackgridHeaderCreateHeaderRowMethod = function() {
        this.row = new Backgrid.HeaderRow({
            columns: this.columns,
            collection: this.collection
        });
    };

    /**
     * Tested with backgrid 0.3.5
     */
    var BackgridHeaderRenderMethod = function() {
        this.$el.empty();
        this.$el.append(this.row.render().$el);
        this.delegateEvents();

        // Trigger event
        this.trigger("backgrid:header:rendered", this);

        return this;
    };
    /*
          backgrid-expandable-cell
          https://github.com/cime/backgrid-expandable-cell

          Copyright (c) 2014 Andrej Cimperšek
          Licensed under the MIT @license.
        */
    Backgrid.ExpandableCell = Backgrid.Cell.extend({
        accordion: true,
        toggle: '<i style="cursor: pointer;" class="glyphicon toggle pull-left"></i>',
        toggleClass: 'toggle',
        toggleExpandedClass: 'fa fa-angle-down',
        toggleCollapsedClass: 'fa fa-angle-right',
        trClass: 'expandable',
        tdClass: 'expandable-content',
        events: {
            'click': 'setToggle'
        },
        initialize: function(options) {
            if (options.accordion) {
                this.accordion = options.accordion;
            }

            this.column = options.column;
            if (!(this.column instanceof Backgrid.Column)) {
                this.column = new Backgrid.Column(this.column);
            }

            var column = this.column,
                model = this.model,
                $el = this.$el;

            if (Backgrid.callByNeed(column.renderable(), column, model)) $el.addClass("renderable");
        },

        render: function() {
            /* follow along with the original render really... */
            this.$el.empty();
            var isExpand = true;
            if (this.column.get('isExpandVisible')) {
                isExpand = this.column.get('isExpandVisible')(this.$el, this.model);
            }
            this.$toggleEl = $(this.toggle).addClass(this.toggleClass).addClass(this.toggleCollapsedClass);
            this.$toggleEl = isExpand ? this.$toggleEl : this.$toggleEl.addClass("noToggle");

            this.$el.append(this.$toggleEl);

            this.delegateEvents();

            return this;
        },

        setToggle: function() {
            var detailsRow = this.$el.data('details');
            var toggle = this.$toggleEl;
            /* if there's details data is not there/undefined and $toggleEl having noToggle class, no need to expand */
            if (!detailsRow && this.$toggleEl.hasClass('noToggle')) {
                return false;
            }

            /* if there's details data already stored, then we'll remove it */
            if (detailsRow) {
                $(detailsRow).remove();
                this.$el.data('details', null);
                toggle.removeClass(this.toggleExpandedClass).addClass(this.toggleCollapsedClass);
            } else {
                if (this.accordion) {
                    var table = this.$el.closest('table');
                    $('.' + this.toggleClass, table).filter('.' + this.toggleExpandedClass).click();
                }

                var renderableColumns = this.$el.closest('table').find('th.renderable').length;
                var isRenderable = false;
                var cellClass = this.tdClass;

                if (Backgrid.callByNeed(this.column.renderable(), this.column, this.model)) {
                    isRenderable = true;
                    cellClass += ' renderable';
                }

                /* build a jquery object for the new row... */
                detailsRow = $('<tr class="' + this.trClass + '"></td><td class="' + cellClass + '" colspan="' + (renderableColumns - 1) + '"></td></tr>');

                /* Inject new row */
                this.$el.closest('tr').after(detailsRow);

                /* Call expand function */
                this.column.get('expand')(detailsRow.find('td.' + this.tdClass), this.model);

                this.$el.data('details', detailsRow);

                toggle.removeClass(this.toggleCollapsedClass).addClass(this.toggleExpandedClass);
            }

            return this;
        }
    });

    // Backgrid patch
    Backgrid.Header.prototype.initialize = BackgridHeaderInitializeMethod;
    Backgrid.Header.prototype.createHeaderRow = BackgridHeaderCreateHeaderRowMethod;
    Backgrid.Header.prototype.render = BackgridHeaderRenderMethod;

    /* End: Backgrid Header render listener when resize or re-ordered */

    var UriCell = Backgrid.UriCell = Backgrid.Cell.extend({
        className: "uri-cell",
        title: null,
        target: "_blank",

        initialize: function(options) {
            UriCell.__super__.initialize.apply(this, arguments);
            this.title = options.title || this.title;
            this.target = options.target || this.target;
        },

        render: function() {
            this.$el.empty();
            var rawValue = this.model.get(this.column.get("name"));
            var href = _.isFunction(this.column.get("href")) ? this.column.get('href')(this.model) : this.column.get('href');
            var klass = this.column.get("klass");
            var formattedValue = this.formatter.fromRaw(rawValue, this.model);
            this.$el.append($("<a>", {
                tabIndex: -1,
                href: href,
                title: this.title || formattedValue,
                'class': klass
            }).text(formattedValue));

            if (this.column.has("iconKlass")) {
                var iconKlass = this.column.get("iconKlass");
                var iconTitle = this.column.get("iconTitle");
                this.$el.find('a').append('<i class="' + iconKlass + '" title="' + iconTitle + '"></i>');
            }
            this.delegateEvents();
            return this;
        }
    });

    var HeaderDecodeCell = Backgrid.HeaderHTMLDecodeCell = Backgrid.HeaderCell.extend({
        initialize: function(options) {
            Backgrid.HeaderCell.prototype.initialize.apply(this, arguments);
            this.name = _.unescape(this.column.get("label"))
            // Add class
            this.$el.addClass(this.name);
        },
        render: function() {
            this.$el.empty();

            // Add to header
            this.$el.text(this.name);

            this.delegateEvents();
            return this;
        }
    });
});