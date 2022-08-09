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
    'utils/Utils',
    'd3',
    'marionette',
    'jquery-ui',
    'jstree'
], function(require, Utils, d3) {
    'use strict';
    _.mixin({
        numberFormatWithComma: function(number) {
            return d3.format(',')(number);
        },
        numberFormatWithBytes: function(number) {
            if (number > -1) {
                if (number === 0) {
                    return "0 Bytes";
                }
                var i = number == 0 ? 0 : Math.floor(Math.log(number) / Math.log(1024));
                if (i > 8) {
                    return _.numberFormatWithComma(number);
                }
                return Number((number / Math.pow(1024, i)).toFixed(2)) + " " + ["Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"][i];
            } else {
                return number;
            }
        },
        isEmptyArray: function(val) {
            if (val && _.isArray(val)) {
                return _.isEmpty(val);
            } else {
                return false;
            }
        },
        toArrayifObject: function(val) {
            return _.isObject(val) ? [val] : val;
        },
        startsWith: function(str, matchStr) {
            if (str && matchStr && _.isString(str) && _.isString(matchStr)) {
                return str.lastIndexOf(matchStr, 0) === 0
            } else {
                return;
            }
        },
        isUndefinedNull: function(val) {
            if (_.isUndefined(val) || _.isNull(val)) {
                return true
            } else {
                return false;
            }
        },
        trim: function(val) {
            if (val && val.trim) {
                return val.trim();
            } else {
                return val;
            }
        },
        isTypePrimitive: function(type) {
            if (type === "int" || type === "byte" || type === "short" || type === "long" || type === "float" || type === "double" || type === "string" || type === "boolean" || type === "date") {
                return true;
            }
            return false;
        }
    });
    var getPopoverEl = function(e) {
        return $(e.target).parent().data("bs.popover") || $(e.target).data("bs.popover") || $(e.target).parents('.popover').length;
    }
    $('body').on('click DOMMouseScroll mousewheel', function(e) {
        if (e.target) {
            // Do action if it is triggered by a human.
            //e.isImmediatePropagationStopped();
            var isPopOverEl = getPopoverEl(e)
            if (!isPopOverEl) {
                $('.popover').popover('hide');
            } else if (isPopOverEl.$tip) {
                $('.popover').not(isPopOverEl.$tip).popover('hide');
            }
            $(".tree-tooltip").removeClass("show");
            $(".tooltip").tooltip("hide");
        }
    });
    $('body').on('hidden.bs.popover', function(e) {
        $(e.target).data("bs.popover").inState = { click: false, hover: false, focus: false }
    });
    $('body').on('show.bs.popover', '[data-js="popover"]', function() {
        $('.popover').not(this).popover('hide');
    });
    $('body').on('keypress', 'input.number-input,.number-input .select2-search__field', function(e) {
        if (e.which != 8 && e.which != 0 && (e.which < 48 || e.which > 57)) {
            return false;
        }
    });
    $('body').on('keypress', 'input.number-input-negative,.number-input-negative .select2-search__field', function(e) {
        if (e.which != 8 && e.which != 0 && (e.which < 48 || e.which > 57)) {
            if (e.which == 45) {
                if (this.value.length) {
                    return false;
                }
            } else {
                return false;
            }
        }
    });
    $('body').on('keypress', 'input.number-input-exponential,.number-input-exponential .select2-search__field', function(e) {
        if ((e.which != 8 && e.which != 0) && (e.which < 48 || e.which > 57) && (e.which != 69 && e.which != 101 && e.which != 43 && e.which != 45 && e.which != 46 && e.which != 190)) {
            return false;
        }
    });
    $("body").on('click', '.dropdown-menu.dropdown-changetitle li a', function() {
        $(this).parents('li').find(".btn:first-child").html($(this).text() + ' <span class="caret"></span>');
    });
    $("body").on('click', '.dropdown-menu.multi-level .dropdown-submenu>a', function(e) {
        e.stopPropagation();
        e.preventDefault();
    });
    $("body").on('click', '.btn', function() {
        $(this).blur();
    });
    $('body').on('keyup input', '.modal-body', function(e) {
        var target = e.target,
            isGlossary = (e.target.dataset.id === "searchTerm" || e.target.dataset.id === "searchCategory") ? true : false; // assign term/category modal
        if ((target.type === "text" || target.type === "textarea") && !isGlossary) {
            var $this = $(this),
                $footerButton = $this.parents(".modal").find('.modal-footer button.ok'),
                requiredInputField = _.filter($this.find('input'), function($e) {
                    if ($e.getAttribute('placeholder') && $e.getAttribute('placeholder').indexOf('require') >= 0) {
                        return ($e.value.trim() == "");
                    }
                });
            if (requiredInputField.length > 0) {
                $footerButton.attr("disabled", "true");
            } else {
                $footerButton.removeAttr("disabled");
            }
        }
    });
    $.fn.select2.amd.define("TagHideDeleteButtonAdapter", [
            "select2/utils",
            "select2/selection/multiple",
            "select2/selection/placeholder",
            "select2/selection/eventRelay",
            "select2/selection/search",
        ],
        function(Utils, MultipleSelection, Placeholder, EventRelay, SelectionSearch) {

            // Decorates MultipleSelection with Placeholder

            var adapter = Utils.Decorate(MultipleSelection, Placeholder);
            adapter = Utils.Decorate(adapter, SelectionSearch);
            adapter = Utils.Decorate(adapter, EventRelay);

            adapter.prototype.render = function() {
                // Use selection-box from SingleSelection adapter
                // This implementation overrides the default implementation
                var $search = $(
                    '<li class="select2-search select2-search--inline">' +
                    '<input class="select2-search__field" type="search" tabindex="-1"' +
                    ' autocomplete="off" autocorrect="off" autocapitalize="none"' +
                    ' spellcheck="false" role="textbox" aria-autocomplete="list" />' +
                    '</li>'
                );

                this.$searchContainer = $search;
                this.$search = $search.find('input');
                var $selection = MultipleSelection.prototype.render.call(this);
                this._transferTabIndex();
                return $selection;
            };

            adapter.prototype.update = function(data) {
                // copy and modify SingleSelection adapter
                var that = this;
                this.clear();
                if (data.length === 0) {
                    this.$selection.find('.select2-selection__rendered')
                        .append(this.$searchContainer);
                    this.$search.attr('placeholder', this.options.get("placeholder"));
                    return;
                }
                this.$search.attr('placeholder', '');
                var $rendered = this.$selection.find('.select2-selection__rendered'),
                    $selectionContainer = [];
                if (data.length > 0) {
                    _.each(data, function(obj) {
                        var $container = $('<li class="select2-selection__choice"></li>'),
                            formatted = that.display(obj, $rendered),
                            $remove = $('<span class="select2-selection__choice__remove" role="presentation">&times;</span>'),
                            allowRemoveAttr = $(obj.element).data("allowremove"),
                            allowRemove = obj.allowRemove === undefined ? allowRemoveAttr : obj.allowRemove;
                        if (allowRemove === undefined || allowRemove !== false) {
                            $container.append($remove);
                        }
                        $container.data("data", obj);
                        $container.append(formatted);
                        $selectionContainer.push($container);
                    });
                    Utils.appendMany($rendered, $selectionContainer);
                }


                var searchHadFocus = this.$search[0] == document.activeElement;
                this.$search.attr('placeholder', '');
                this.$selection.find('.select2-selection__rendered')
                    .append(this.$searchContainer);
                this.resizeSearch();
                if (searchHadFocus) {
                    this.$search.focus();
                }
            };
            return adapter;
        });

    $.fn.select2.amd.define("ServiceTypeFilterDropdownAdapter", [
            "select2/utils",
            "select2/dropdown",
            "select2/dropdown/attachBody",
            "select2/dropdown/attachContainer",
            "select2/dropdown/search",
            "select2/dropdown/minimumResultsForSearch",
            "select2/dropdown/closeOnSelect",
        ],
        function(Utils, Dropdown, AttachBody, AttachContainer, Search, MinimumResultsForSearch, CloseOnSelect) {

            // Decorate Dropdown with Search functionalities
            var dropdownWithSearch = Utils.Decorate(Utils.Decorate(Dropdown, CloseOnSelect), Search);

            dropdownWithSearch.prototype.render = function() {
                // Copy and modify default search render method
                var $rendered = Dropdown.prototype.render.call(this),
                    dropdownCssClass = this.options.get("dropdownCssClass")
                if (dropdownCssClass) {
                    $rendered.addClass(dropdownCssClass);
                }

                // Add ability for a placeholder in the search box
                var placeholder = this.options.get("placeholderForSearch") || "";
                var $search = $(
                    '<span class="select2-search select2-search--dropdown"><div class="clearfix">' +
                    '<div class="col-md-10 no-padding" style="width: calc(100% - 30px);"><input class="select2-search__field" placeholder="' + placeholder + '" type="search"' +
                    ' tabindex="-1" autocomplete="off" autocorrect="off" autocapitalize="off"' +
                    ' spellcheck="false" role="textbox" /></div>' +
                    '<div class="col-md-2 no-padding" style="width: 30px;"><button type="button" style="padding: 3px 6px;margin: 0px 4px;" class="btn btn-action btn-sm filter " title="Type Filter"><i class="fa fa-filter"></i></button></div>' +
                    '</div></span>'
                );
                if (!this.options.options.getFilterBox) {
                    throw "In order to render the filter options adapter needed getFilterBox function"
                }
                var $Filter = $('<ul class="type-filter-ul"></ul>');
                this.$Filter = $Filter;
                this.$Filter.append(this.options.options.getFilterBox());
                this.$Filter.hide();

                this.$searchContainer = $search;
                if ($Filter.find('input[type="checkbox"]:checked').length) {
                    $search.find('button.filter').addClass('active');
                } else {
                    $search.find('button.filter').removeClass('active');
                }
                this.$search = $search.find('input');

                $rendered.prepend($search);
                $rendered.append($Filter);
                return $rendered;
            };
            var oldDropdownWithSearchBindRef = dropdownWithSearch.prototype.bind;
            dropdownWithSearch.prototype.bind = function(container, $container) {
                var self = this;
                oldDropdownWithSearchBindRef.call(this, container, $container);
                var self = this;
                this.$Filter.on('click', 'li', function() {
                    var itemCallback = self.options.options.onFilterItemSelect;
                    itemCallback && itemCallback(this);
                })

                this.$searchContainer.find('button.filter').click(function() {
                    container.$dropdown.find('.select2-search').hide(150);
                    container.$dropdown.find('.select2-results').hide(150);
                    self.$Filter.html(self.options.options.getFilterBox());
                    self.$Filter.show();
                });
                this.$Filter.on('click', 'button.filterDone', function() {
                    container.$dropdown.find('.select2-search').show(150);
                    container.$dropdown.find('.select2-results').show(150);
                    self.$Filter.hide();
                    var filterSubmitCallback = self.options.options.onFilterSubmit;
                    filterSubmitCallback && filterSubmitCallback({
                        filterVal: _.map(self.$Filter.find('input[type="checkbox"]:checked'), function(item) {
                            return $(item).data('value')
                        })
                    });
                });
                container.$element.on('hideFilter', function() {
                    container.$dropdown.find('.select2-search').show();
                    container.$dropdown.find('.select2-results').show();
                    self.$Filter.hide();
                });

            }
            // Decorate the dropdown+search with necessary containers
            var adapter = Utils.Decorate(dropdownWithSearch, AttachContainer);
            adapter = Utils.Decorate(adapter, AttachBody);

            return adapter;
        });

    $.jstree.plugins.node_customize = function(options, parent) {
        this.redraw_node = function(obj, deep, callback, force_draw) {
            var node_id = obj;
            var el = parent.redraw_node.apply(this, arguments);
            if (el) {
                var node = this._model.data[node_id];
                var cfg = this.settings.node_customize;
                var key = cfg.key;
                var type = (node && node.original && node.original[key]);
                var customizer = (type && cfg.switch[type]) || cfg.default;
                if (customizer)
                    customizer(el, node);
            }
            return el;
        };
    }

    $.widget("custom.atlasAutoComplete", $.ui.autocomplete, {
        _create: function() {
            this._super();
            this.widget().menu("option", "items", "> :not(.ui-autocomplete-category,.empty)");
        },
        _renderMenu: function(ul, items) {
            var that = this,
                currentCategory = "";
            items = _.sortBy(items, 'order');
            $.each(items, function(index, item) {
                var li;
                if (item.category != currentCategory) {
                    ul.append("<li class='ui-autocomplete-category'>" + item.category + "</li>");
                    currentCategory = item.category;
                }
                that._renderItemData(ul, item);
            });
        },
        _renderItemData: function(ul, item) {
            return this._renderItem(ul, item);
        }
    });

    // For placeholder support 
    if (!('placeholder' in HTMLInputElement.prototype)) {
        var originalRender = Backbone.Marionette.LayoutView.prototype.render;
        Backbone.Marionette.LayoutView.prototype.render = function() {
            originalRender.apply(this, arguments);
            this.$('input, textarea').placeholder();
        }
    }
    $('body').on('click', 'pre.code-block .expand-collapse-button', function(e) {
        var $el = $(this).parents('.code-block');
        if ($el.hasClass('shrink')) {
            $el.removeClass('shrink');
        } else {
            $el.addClass('shrink');
        }
    });

    // For adding tooltip globally
    $("body").on('mouseenter', '.select2-selection__choice', function() {
        $(this).attr("title", "");
    });
    if ($('body').tooltip) {
        $('body').tooltip({
            selector: '[title]:not(".select2-selection__choice,.select2-selection__rendered")',
            placement: function() {
                return this.$element.attr("data-placement") || "bottom";
            },
            container: 'body'
        });
    }
    //For closing the modal on browsers navigation
    $(window).on('popstate', function() {
        $('body').find('.modal-dialog .close').click();
    });
})