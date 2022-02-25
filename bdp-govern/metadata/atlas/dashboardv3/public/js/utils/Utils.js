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

define(['require', 'utils/Globals', 'pnotify', 'utils/Messages', 'utils/Enums', 'moment', 'store', 'modules/Modal', 'moment-timezone', 'pnotify.buttons', 'pnotify.confirm'], function(require, Globals, pnotify, Messages, Enums, moment, store, Modal) {
    'use strict';

    var Utils = {};
    var prevNetworkErrorTime = 0;

    Utils.generatePopover = function(options) {
        if (options.el) {
            var defaultObj = {
                placement: 'auto bottom',
                html: true,
                animation: false,
                container: 'body',
                sanitize: false
            };
            if (options.viewFixedPopover || options.contentClass) {
                defaultObj.template = '<div class="popover ' + (options.viewFixedPopover ? 'fixed-popover' : '') + ' fade bottom"><div class="arrow"></div><h3 class="popover-title"></h3><div class="' + (options.contentClass ? options.contentClass : '') + ' popover-content"></div></div>';
            }
            return options.el.popover(_.extend(defaultObj, options.popoverOptions));
        }
    }

    Utils.getNumberSuffix = function(options) {
        if (options && options.number) {
            var n = options.number,
                s = ["th", "st", "nd", "rd"],
                v = n % 100,
                suffix = (s[(v - 20) % 10] || s[v] || s[0]);
            return n + (options.sup ? '<sup>' + suffix + '</sup>' : suffix);
        }
    }

    Utils.generateUUID = function() {
        var d = new Date().getTime();
        if (window.performance && typeof window.performance.now === "function") {
            d += performance.now(); //use high-precision timer if available
        }
        var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = (d + Math.random() * 16) % 16 | 0;
            d = Math.floor(d / 16);
            return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
        return uuid;
    };
    Utils.getBaseUrl = function(url, noPop) {
        var path = url.replace(/\/[\w-]+.(jsp|html)|\/+$/ig, ''),
            splitPath = path.split("/");
        if (noPop !== true && splitPath && splitPath[splitPath.length - 1] === "n") {
            splitPath.pop();
            return splitPath.join("/");
        }
        return path;
    };
    Utils.getEntityIconPath = function(options) {
        var entityData = options && options.entityData,
            serviceType,
            status,
            typeName,
            iconBasePath = Utils.getBaseUrl(window.location.pathname, true) + Globals.entityImgPath;
        if (entityData) {
            typeName = entityData.typeName;
            serviceType = entityData && entityData.serviceType;
            status = entityData && entityData.status;
        }

        function getImgPath(imageName) {
            return iconBasePath + (Enums.entityStateReadOnly[status] ? "disabled/" + imageName : imageName);
        }

        function getDefaultImgPath() {
            if (entityData.isProcess) {
                if (Enums.entityStateReadOnly[status]) {
                    return iconBasePath + 'disabled/process.png';
                } else {
                    return iconBasePath + 'process.png';
                }
            } else {
                if (Enums.entityStateReadOnly[status]) {
                    return iconBasePath + 'disabled/table.png';
                } else {
                    return iconBasePath + 'table.png';
                }
            }
        }

        if (entityData) {
            if (options.errorUrl) {
                var isErrorInTypeName = (options.errorUrl && options.errorUrl.match("entity-icon/" + typeName + ".png|disabled/" + typeName + ".png") ? true : false);
                if (serviceType && isErrorInTypeName) {
                    var imageName = serviceType + ".png";
                    return getImgPath(imageName);
                } else {
                    return getDefaultImgPath();
                }
            } else if (entityData.typeName) {
                var imageName = entityData.typeName + ".png";
                return getImgPath(imageName);
            } else {
                return getDefaultImgPath();
            }
        }
    }

    pnotify.prototype.options.styling = "fontawesome";
    var notify = function(options) {
        return new pnotify(_.extend({
            icon: true,
            hide: true,
            delay: 3000,
            remove: true,
            buttons: {
                classes: {
                    closer: 'fa fa-times',
                    pin_up: 'fa fa-pause',
                    pin_down: 'fa fa-play'
                }
            }
        }, options));
    }
    Utils.notifyInfo = function(options) {
        notify({
            type: "info",
            text: (options.html ? options.content : _.escape(options.content)) || "Info message."
        });
    };

    Utils.notifyWarn = function(options) {
        notify({
            type: "notice",
            text: (options.html ? options.content : _.escape(options.content)) || "Info message."
        });
    };

    Utils.notifyError = function(options) {
        notify({
            type: "error",
            text: (options.html ? options.content : _.escape(options.content)) || "Error occurred."
        });
    };

    Utils.notifySuccess = function(options) {
        notify({
            type: "success",
            text: (options.html ? options.content : _.escape(options.content)) || "Error occurred."
        });
    };

    Utils.notifyConfirm = function(options) {
        var modal = {};
        if (options && options.modal) {
            var myStack = { "dir1": "down", "dir2": "right", "push": "top", 'modal': true };
            modal['addclass'] = 'stack-modal ' + (options.modalClass ? modalClass : 'width-500');
            modal['stack'] = myStack;
        }
        notify(_.extend({
            title: 'Confirmation',
            hide: false,
            confirm: {
                confirm: true,
                buttons: [{
                        text: options.cancelText || 'Cancel',
                        addClass: 'btn-action btn-md cancel',
                        click: function(notice) {
                            options.cancel(notice);
                            notice.remove();
                        }
                    },
                    {
                        text: options.okText || 'Ok',
                        addClass: 'btn-atlas btn-md ok',
                        click: function(notice) {
                            if (options.ok) {
                                options.ok($.extend({}, notice, {
                                    hideButtonLoader: function() {
                                        notice.container.find("button.ok").hideButtonLoader();
                                    },
                                    showButtonLoader: function() {
                                        notice.container.find("button.ok").showButtonLoader();
                                    }
                                }));
                            }
                            if (options.okShowLoader) {
                                notice.container.find("button.ok").showButtonLoader();
                            }
                            if (options.okCloses !== false) {
                                notice.remove();
                            }
                        }
                    }
                ]
            },
            buttons: {
                closer: false,
                sticker: false
            },
            history: {
                history: false
            }

        }, modal, options)).get().on('pnotify.confirm', function() {
            if (options.ok) {
                options.ok();
            }
        }).on('pnotify.cancel', function() {
            if (options.cancel) {
                options.cancel();
            }
        });
    }
    Utils.defaultErrorHandler = function(model, error, options) {
        var skipDefaultError = null,
            defaultErrorMessage = null,
            isHtml = null;
        if (options) {
            skipDefaultError = options.skipDefaultError;
            defaultErrorMessage = options.defaultErrorMessage;
            isHtml = options.isHtml;
        }
        var redirectToLoginPage = function() {
            Utils.localStorage.setValue("last_ui_load", "v2");
            window.location = 'login.jsp';
        }
        if (error && error.status) {
            if (error.status == 401) {
                redirectToLoginPage();
            } else if (error.status == 419) {
                redirectToLoginPage();
            } else if (error.status == 403) {
                Utils.serverErrorHandler(error, "You are not authorized");
            } else if (error.status == "0" && error.statusText != "abort") {
                var diffTime = (new Date().getTime() - prevNetworkErrorTime);
                if (diffTime > 3000) {
                    prevNetworkErrorTime = new Date().getTime();
                    Utils.notifyError({
                        content: "Network Connection Failure : " +
                            "It seems you are not connected to the internet. Please check your internet connection and try again"
                    });
                }
            } else if (skipDefaultError !== true) {
                Utils.serverErrorHandler(error, defaultErrorMessage, isHtml);
            }
        } else if (skipDefaultError !== true) {
            Utils.serverErrorHandler(error, defaultErrorMessage);
        }
    };
    Utils.serverErrorHandler = function(response, defaultErrorMessage, isHtml) {
        var responseJSON = response ? response.responseJSON : response,
            message = defaultErrorMessage ? defaultErrorMessage : Messages.defaultErrorMessage
        if (response && responseJSON) {
            message = responseJSON.errorMessage || responseJSON.message || responseJSON.error || message
        }
        var existingError = $(".ui-pnotify-container.alert-danger .ui-pnotify-text").text();
        if (existingError !== message) {
            Utils.notifyError({
                html: isHtml,
                content: message
            });
        }
    };
    Utils.cookie = {
        setValue: function(cname, cvalue) {
            document.cookie = cname + "=" + cvalue + "; ";
        },
        getValue: function(findString) {
            var search = findString + "=";
            var ca = document.cookie.split(';');
            for (var i = 0; i < ca.length; i++) {
                var c = ca[i];
                while (c.charAt(0) == ' ') c = c.substring(1);
                if (c.indexOf(name) == 0) {
                    return c.substring(name.length, c.length);
                }
            }
            return "";
        }
    };
    Utils.localStorage = function() {
        this.setValue = function() {
            localStorage.setItem(arguments[0], arguments[1]);
        }
        this.getValue = function(key, value) {
            var keyValue = localStorage.getItem(key);
            if ((!keyValue || keyValue == "undefined") && (value != undefined)) {
                return this.setLocalStorage(key, value);
            } else {
                if (keyValue === "" || keyValue === "undefined" || keyValue === "null") {
                    return null;
                } else {
                    return keyValue;
                }

            }
        }
        this.removeValue = function() {
            localStorage.removeItem(arguments[0]);
        }
        if (typeof(Storage) === "undefined") {
            _.extend(this, Utils.cookie);
            console.log('Sorry! No Web Storage support');
        }
    }
    Utils.localStorage = new Utils.localStorage();

    Utils.setUrl = function(options) {
        if (options) {
            if (options.mergeBrowserUrl) {
                var param = Utils.getUrlState.getQueryParams();
                if (param) {
                    options.urlParams = $.extend(param, options.urlParams);
                }
            }
            if (options.urlParams) {
                var urlParams = "?";
                _.each(options.urlParams, function(value, key, obj) {
                    if (value) {
                        value = encodeURIComponent(String(value));
                        urlParams += key + "=" + value + "&";
                    }
                });
                urlParams = urlParams.slice(0, -1);
                options.url += urlParams;
            }
            if (options.updateTabState) {
                var urlUpdate = {
                    stateChanged: true
                };
                if (Utils.getUrlState.isTagTab(options.url)) {
                    urlUpdate['tagUrl'] = options.url;
                } else if (Utils.getUrlState.isSearchTab(options.url)) {
                    urlUpdate['searchUrl'] = options.url;
                } else if (Utils.getUrlState.isGlossaryTab(options.url)) {
                    urlUpdate['glossaryUrl'] = options.url;
                } else if (Utils.getUrlState.isBMDetailPage(options.url)) {
                    urlUpdate['bmDetailPageUrl'] = options.url;
                    urlUpdate['administratorUrl'] = options.url;
                } else if (Utils.getUrlState.isAdministratorTab(options.url)) {
                    urlUpdate['administratorUrl'] = options.url;
                } else if (Utils.getUrlState.isDebugMetricsTab(options.url)) {
                    urlUpdate['debugMetricsUrl'] = options.url;
                }
                $.extend(Globals.saveApplicationState.tabState, urlUpdate);
            }
            Backbone.history.navigate(options.url, { trigger: options.trigger != undefined ? options.trigger : true });
        }
    };

    Utils.getUrlState = {
        getQueryUrl: function(url) {
            var hashValue = window.location.hash;
            if (url) {
                hashValue = url;
            }
            return {
                firstValue: hashValue.split('/')[1],
                hash: hashValue,
                queyParams: hashValue.split("?"),
                lastValue: hashValue.split('/')[hashValue.split('/').length - 1]
            }
        },
        checkTabUrl: function(options) {
            var url = options && options.url,
                matchString = options && options.matchString,
                quey = this.getQueryUrl(url);
            return quey.firstValue == matchString || quey.queyParams[0] == "#!/" + matchString;
        },
        isInitial: function() {
            return this.getQueryUrl().firstValue == undefined;
        },
        isTagTab: function(url) {
            return this.checkTabUrl({
                url: url,
                matchString: "tag"
            });
        },
        isSearchTab: function(url) {
            return this.checkTabUrl({
                url: url,
                matchString: "search"
            });
        },
        isAdministratorTab: function(url) {
            return this.checkTabUrl({
                url: url,
                matchString: "administrator"
            });
        },
        isBMDetailPage: function(url) {
            var quey = this.getQueryUrl(url);
            return (quey.hash.indexOf("businessMetadata") > -1) ? true : false;
        },
        isDebugMetricsTab: function(url) {
            return this.checkTabUrl({
                url: url,
                matchString: "debugMetrics"
            });
        },
        isGlossaryTab: function(url) {
            return this.checkTabUrl({
                url: url,
                matchString: "glossary"
            });
        },
        isDetailPage: function(url) {
            return this.checkTabUrl({
                url: url,
                matchString: "detailPage"
            });
        },
        getLastValue: function() {
            return this.getQueryUrl().lastValue;
        },
        getFirstValue: function() {
            return this.getQueryUrl().firstValue;
        },
        getQueryParams: function(url) {
            var qs = this.getQueryUrl(url).queyParams[1];
            if (typeof qs == "string") {
                qs = qs.split('+').join(' ');
                var params = {},
                    tokens,
                    re = /[?&]?([^=]+)=([^&]*)/g;
                while (tokens = re.exec(qs)) {
                    params[decodeURIComponent(tokens[1])] = decodeURIComponent(tokens[2]);
                }
                return params;
            }
        },
        getKeyValue: function(key) {
            var paramsObj = this.getQueryParams();
            if (key.length) {
                var values = [];
                _.each(key, function(objKey) {
                    var obj = {};
                    obj[objKey] = paramsObj[objKey]
                    values.push(obj);
                    return values;
                })
            } else {
                return paramsObj[key];
            }
        }
    }
    Utils.getName = function() {
        return Utils.extractKeyValueFromEntity.apply(this, arguments).name;
    }
    Utils.getNameWithProperties = function() {
        return Utils.extractKeyValueFromEntity.apply(this, arguments);
    }
    Utils.extractKeyValueFromEntity = function() {
        var collectionJSON = arguments[0],
            priorityAttribute = arguments[1],
            skipAttribute = arguments[2],
            returnObj = {
                name: '-',
                found: true,
                key: null
            }
        if (collectionJSON) {
            if (collectionJSON.attributes && collectionJSON.attributes[priorityAttribute]) {
                returnObj.name = _.escape(collectionJSON.attributes[priorityAttribute]);
                returnObj.key = priorityAttribute;
                return returnObj;
            }
            if (collectionJSON[priorityAttribute]) {
                returnObj.name = _.escape(collectionJSON[priorityAttribute]);
                returnObj.key = priorityAttribute;
                return returnObj;
            }
            if (collectionJSON.attributes) {
                if (collectionJSON.attributes.name) {
                    returnObj.name = _.escape(collectionJSON.attributes.name);
                    returnObj.key = 'name';
                    return returnObj;
                }
                if (collectionJSON.attributes.displayName) {
                    returnObj.name = _.escape(collectionJSON.attributes.displayName);
                    returnObj.key = 'displayName';
                    return returnObj;
                }
                if (collectionJSON.attributes.qualifiedName) {
                    returnObj.name = _.escape(collectionJSON.attributes.qualifiedName);
                    returnObj.key = 'qualifiedName';
                    return returnObj;
                }
                if (collectionJSON.attributes.displayText) {
                    returnObj.name = _.escape(collectionJSON.attributes.displayText);
                    returnObj.key = 'displayText';
                    return returnObj;
                }
                if (collectionJSON.attributes.guid) {
                    returnObj.name = _.escape(collectionJSON.attributes.guid);
                    returnObj.key = 'guid';
                    return returnObj;
                }
                if (collectionJSON.attributes.id) {
                    if (_.isObject(collectionJSON.attributes.id)) {
                        if (collectionJSON.id.id) {
                            returnObj.name = _.escape(collectionJSON.attributes.id.id);
                        }
                    } else {
                        returnObj.name = _.escape(collectionJSON.attributes.id);
                    }
                    returnObj.key = 'id';
                    return returnObj;
                }
            }
            if (collectionJSON.name) {
                returnObj.name = _.escape(collectionJSON.name);
                returnObj.key = 'name';
                return returnObj;
            }
            if (collectionJSON.displayName) {
                returnObj.name = _.escape(collectionJSON.displayName);
                returnObj.key = 'displayName';
                return returnObj;
            }
            if (collectionJSON.qualifiedName) {
                returnObj.name = _.escape(collectionJSON.qualifiedName);
                returnObj.key = 'qualifiedName';
                return returnObj;
            }
            if (collectionJSON.displayText) {
                returnObj.name = _.escape(collectionJSON.displayText);
                returnObj.key = 'displayText';
                return returnObj;
            }
            if (collectionJSON.guid) {
                returnObj.name = _.escape(collectionJSON.guid);
                returnObj.key = 'guid';
                return returnObj;
            }
            if (collectionJSON.id) {
                if (_.isObject(collectionJSON.id)) {
                    if (collectionJSON.id.id) {
                        returnObj.name = _.escape(collectionJSON.id.id);
                    }
                } else {
                    returnObj.name = _.escape(collectionJSON.id);
                }
                returnObj.key = 'id';
                return returnObj;
            }
        }
        returnObj.found = false;
        if (skipAttribute && returnObj.key == skipAttribute) {
            return {
                name: '-',
                found: true,
                key: null
            }
        } else {
            return returnObj;
        }

    }

    Utils.backButtonClick = function() {
        var queryParams = Utils.getUrlState.getQueryParams(),
            urlPath = "searchUrl";
        if (queryParams && queryParams.from) {
            if (queryParams.from == "classification") {
                urlPath = "tagUrl";
            } else if (queryParams.from == "glossary") {
                urlPath = "glossaryUrl";
            } else if (queryParams.from == "bm") {
                urlPath = "administratorUrl";
            }
        }
        Utils.setUrl({
            url: Globals.saveApplicationState.tabState[urlPath],
            mergeBrowserUrl: false,
            trigger: true,
            updateTabState: true
        });
    }

    Utils.showTitleLoader = function(loaderEl, titleBoxEl) {
        loaderEl.css ? loaderEl.css({
            'display': 'block',
            'position': 'relative',
            'height': '85px',
            'marginTop': '85px',
            'marginLeft': '50%',
            'left': '0%'
        }) : null;
        titleBoxEl.hide ? titleBoxEl.hide() : null;
    }
    Utils.hideTitleLoader = function(loaderEl, titleBoxEl) {
        loaderEl.hide ? loaderEl.hide() : null;
        titleBoxEl.fadeIn ? titleBoxEl.fadeIn() : null;
    }
    Utils.findAndMergeRefEntity = function(options) {
        var attributeObject = options.attributeObject,
            referredEntities = options.referredEntities
        var mergeObject = function(obj) {
            if (obj) {
                if (obj.attributes) {
                    Utils.findAndMergeRefEntity({
                        "attributeObject": obj.attributes,
                        "referredEntities": referredEntities
                    });
                } else if (referredEntities[obj.guid]) {
                    _.extend(obj, referredEntities[obj.guid]);
                }
            }
        }
        if (attributeObject && referredEntities) {
            _.each(attributeObject, function(obj, key) {
                if (_.isObject(obj)) {
                    if (_.isArray(obj)) {
                        _.each(obj, function(value) {
                            mergeObject(value);
                        });
                    } else {
                        mergeObject(obj);
                    }
                }
            });
        }
    }

    Utils.findAndMergeRelationShipEntity = function(options) {
        var attributeObject = options.attributeObject,
            relationshipAttributes = options.relationshipAttributes;
        _.each(attributeObject, function(val, key) {
            var attributVal = val;
            if (relationshipAttributes && relationshipAttributes[key]) {
                var relationShipVal = relationshipAttributes[key];
                if (_.isObject(val)) {
                    if (_.isArray(val)) {
                        _.each(val, function(attr) {
                            if (attr && attr.attributes === undefined) {
                                var entityFound = _.find(relationShipVal, { guid: attr.guid });
                                if (entityFound) {
                                    attr.attributes = _.omit(entityFound, 'typeName', 'guid', 'entityStatus');
                                    attr.status = entityFound.entityStatus;
                                }
                            }
                        });
                    } else if (relationShipVal && val.attributes === undefined) {
                        val.attributes = _.omit(relationShipVal, 'typeName', 'guid', 'entityStatus');
                        val.status = relationShipVal.entityStatus;
                    }
                }
            }
        })
    }

    Utils.getNestedSuperTypes = function(options) {
        var data = options.data,
            collection = options.collection,
            superTypes = [];

        var getData = function(data, collection) {
            if (data) {
                superTypes = superTypes.concat(data.superTypes);
                if (data.superTypes && data.superTypes.length) {
                    _.each(data.superTypes, function(superTypeName) {
                        if (collection.fullCollection) {
                            var collectionData = collection.fullCollection.findWhere({ name: superTypeName }).toJSON();
                        } else {
                            var collectionData = collection.findWhere({ name: superTypeName }).toJSON();
                        }
                        getData(collectionData, collection);
                    });
                }
            }
        }
        getData(data, collection);
        return _.uniq(superTypes);
    }
    Utils.getNestedSuperTypeObj = function(options) {
        var mainData = options.data,
            collection = options.collection,
            attrMerge = options.attrMerge,
            seperateRelatioshipAttr = options.seperateRelatioshipAttr || false,
            mergeRelationAttributes = options.mergeRelationAttributes || (seperateRelatioshipAttr ? false : true);

        if (mergeRelationAttributes && seperateRelatioshipAttr) {
            throw "Both mergeRelationAttributes & seperateRelatioshipAttr cannot be true!"
        }
        var attributeDefs = {};
        if (attrMerge && !seperateRelatioshipAttr) {
            attributeDefs = [];
        } else if (options.attrMerge && seperateRelatioshipAttr) {
            attributeDefs = {
                attributeDefs: [],
                relationshipAttributeDefs: []
            }
        }
        var getRelationshipAttributeDef = function(data) {
            return _.filter(
                data.relationshipAttributeDefs,
                function(obj, key) {
                    return obj;
                })
        }
        var getData = function(data, collection) {
            if (options.attrMerge) {
                if (seperateRelatioshipAttr) {
                    attributeDefs.attributeDefs = attributeDefs.attributeDefs.concat(data.attributeDefs);
                    attributeDefs.relationshipAttributeDefs = attributeDefs.relationshipAttributeDefs.concat(getRelationshipAttributeDef(data));
                } else {
                    attributeDefs = attributeDefs.concat(data.attributeDefs);
                    if (mergeRelationAttributes) {
                        attributeDefs = attributeDefs.concat(getRelationshipAttributeDef(data));
                    }
                }
            } else {
                if (attributeDefs[data.name]) {
                    attributeDefs[data.name] = _.toArrayifObject(attributeDefs[data.name]).concat(data.attributeDefs);
                } else {
                    if (seperateRelatioshipAttr) {
                        attributeDefs[data.name] = {
                            attributeDefs: data.attributeDefs,
                            relationshipAttributeDefs: data.relationshipAttributeDefs
                        };
                    } else {
                        attributeDefs[data.name] = data.attributeDefs;
                        if (mergeRelationAttributes) {
                            attributeDefs[data.name] = _.toArrayifObject(attributeDefs[data.name]).concat(getRelationshipAttributeDef(data));
                        }
                    }
                }
            }
            if (data.superTypes && data.superTypes.length) {
                _.each(data.superTypes, function(superTypeName) {
                    if (collection.fullCollection) {
                        var collectionData = collection.fullCollection.findWhere({ name: superTypeName });
                    } else {
                        var collectionData = collection.findWhere({ name: superTypeName });
                    }
                    collectionData = collectionData && collectionData.toJSON ? collectionData.toJSON() : collectionData;
                    if (collectionData) {
                        return getData(collectionData, collection);
                    } else {
                        return;
                    }
                });
            }
        }
        getData(mainData, collection);
        if (attrMerge) {
            if (seperateRelatioshipAttr) {
                attributeDefs = {
                    attributeDefs: _.uniq(_.sortBy(attributeDefs.attributeDefs, 'name'), true, function(obj) {
                        return obj.name
                    }),
                    relationshipAttributeDefs: _.uniq(_.sortBy(attributeDefs.relationshipAttributeDefs, 'name'), true, function(obj) {
                        return (obj.name + obj.relationshipTypeName)
                    })
                }
            } else {
                attributeDefs = _.uniq(_.sortBy(attributeDefs, 'name'), true, function(obj) {
                    if (obj.relationshipTypeName) {
                        return (obj.name + obj.relationshipTypeName)
                    } else {
                        return (obj.name)
                    }
                });
            }
        }
        return attributeDefs;
    }

    Utils.getProfileTabType = function(profileData, skipData) {
        var parseData = profileData.distributionData;
        if (_.isString(parseData)) {
            parseData = JSON.parse(parseData);
        }
        var createData = function(type) {
            var orderValue = [],
                sort = false;
            if (type === "date") {
                var dateObj = {};
                _.keys(parseData).map(function(key) {
                    var splitValue = key.split(":");
                    if (!dateObj[splitValue[0]]) {
                        dateObj[splitValue[0]] = {
                            value: splitValue[0],
                            monthlyCounts: {},
                            totalCount: 0 // use when count is null
                        }
                    }
                    if (dateObj[splitValue[0]] && splitValue[1] == "count") {
                        dateObj[splitValue[0]].count = parseData[key];
                    }
                    if (dateObj[splitValue[0]] && splitValue[1] !== "count") {
                        dateObj[splitValue[0]].monthlyCounts[splitValue[1]] = parseData[key];
                        if (!dateObj[splitValue[0]].count) {
                            dateObj[splitValue[0]].totalCount += parseData[key]
                        }
                    }
                });
                return _.toArray(dateObj).map(function(obj) {
                    if (!obj.count && obj.totalCount) {
                        obj.count = obj.totalCount
                    }
                    return obj
                });
            } else {
                var data = [];
                if (profileData.distributionKeyOrder) {
                    orderValue = profileData.distributionKeyOrder;
                } else {
                    sort = true;
                    orderValue = _.keys(parseData);
                }
                _.each(orderValue, function(key) {
                    if (parseData[key]) {
                        data.push({
                            value: key,
                            count: parseData[key]
                        });
                    }
                });
                if (sort) {
                    data = _.sortBy(data, function(o) {
                        return o.value.toLowerCase()
                    });
                }
                return data;
            }
        }
        if (profileData && profileData.distributionType) {
            if (profileData.distributionType === "count-frequency") {
                return {
                    type: "string",
                    label: Enums.profileTabType[profileData.distributionType],
                    actualObj: !skipData ? createData("string") : null,
                    xAxisLabel: "FREQUENCY",
                    yAxisLabel: "COUNT"
                }
            } else if (profileData.distributionType === "decile-frequency") {
                return {
                    label: Enums.profileTabType[profileData.distributionType],
                    type: "numeric",
                    xAxisLabel: "DECILE RANGE",
                    actualObj: !skipData ? createData("numeric") : null,
                    yAxisLabel: "FREQUENCY"
                }
            } else if (profileData.distributionType === "annual") {
                return {
                    label: Enums.profileTabType[profileData.distributionType],
                    type: "date",
                    xAxisLabel: "",
                    actualObj: !skipData ? createData("date") : null,
                    yAxisLabel: "COUNT"
                }
            }
        }
    }

    Utils.isUrl = function(url) {
        var regexp = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/
        return regexp.test(url);
    }

    Utils.JSONPrettyPrint = function(obj, getValue) {
        var replacer = function(match, pIndent, pKey, pVal, pEnd) {
                var key = '<span class=json-key>';
                var val = '<span class=json-value>';
                var str = '<span class=json-string>';
                var r = pIndent || '';
                if (pKey)
                    r = r + key + pKey.replace(/[": ]/g, '') + '</span>: ';
                if (pVal)
                    r = r + (pVal[0] == '"' ? str : val) + getValue(pVal) + '</span>';
                return r + (pEnd || '');
            },
            jsonLine = /^( *)("[\w]+": )?("[^"]*"|[\w.+-]*)?([,[{])?$/mg;
        if (obj && _.isObject(obj)) {
            return JSON.stringify(obj, null, 3)
                .replace(/&/g, '&amp;').replace(/\\"/g, '&quot;')
                .replace(/</g, '&lt;').replace(/>/g, '&gt;')
                .replace(jsonLine, replacer);
        } else {
            return {};
        }
    };

    $.fn.toggleAttribute = function(attributeName, firstString, secondString) {
        if (this.attr(attributeName) == firstString) {
            this.attr(attributeName, secondString);
        } else {
            this.attr(attributeName, firstString);
        }
    }

    Utils.millisecondsToTime = function(duration) {
        var milliseconds = parseInt((duration % 1000) / 100),
            seconds = parseInt((duration / 1000) % 60),
            minutes = parseInt((duration / (1000 * 60)) % 60),
            hours = parseInt((duration / (1000 * 60 * 60)) % 24);

        hours = (hours < 10) ? "0" + hours : hours;
        minutes = (minutes < 10) ? "0" + minutes : minutes;
        seconds = (seconds < 10) ? "0" + seconds : seconds;

        return hours + ":" + minutes + ":" + seconds + "." + milliseconds;
    }
    Utils.togglePropertyRelationshipTableEmptyValues = function(object) {
        var inputSelector = object.inputType,
            tableEl = object.tableEl;
        if (inputSelector.prop('checked') == true) {
            tableEl.removeClass('hide-empty-value');
        } else {
            tableEl.addClass('hide-empty-value');
        }
    }
    $.fn.showButtonLoader = function() {
        $(this).attr("disabled", "true").addClass('button-loader');
        $(this).siblings("button.cancel").prop("disabled", true);
    }
    $.fn.hideButtonLoader = function() {
        $(this).removeClass('button-loader').removeAttr("disabled");
        $(this).siblings("button.cancel").prop("disabled", false);
    }
    Utils.formatDate = function(options) {
        var dateValue = null,
            dateFormat = Globals.dateTimeFormat,
            isValidDate = false;
        if (options && options.date) {
            dateValue = options.date;
            if (dateValue !== "-") {
                dateValue = parseInt(dateValue);
                if (_.isNaN(dateValue)) {
                    dateValue = options.date;
                }
                dateValue = moment(dateValue);
                if (dateValue._isValid) {
                    isValidDate = true;
                    dateValue = dateValue.format(dateFormat);
                }
            }
        }
        if (dateValue !== "-") {
            if (isValidDate === false && options && options.defaultDate !== false) {
                dateValue = moment().format(dateFormat);
            }
            if (Globals.isTimezoneFormatEnabled) {
                if (!options || options && options.zone !== false) {
                    dateValue += " (" + moment.tz(moment.tz.guess()).zoneAbbr() + ")";
                }
            }
        }
        return dateValue;
    }
    //------------------------------------------------idleTimeout-----------------------------
    $.fn.idleTimeout = function(userRuntimeConfig) {

        //##############################
        //## Public Configuration Variables
        //##############################
        var defaultConfig = {
                redirectUrl: Utils.getBaseUrl(window.location.pathname) + '/index.html?action=timeout', // redirect to this url on logout. Set to "redirectUrl: false" to disable redirect

                // idle settings
                idleTimeLimit: Globals.idealTimeoutSeconds, // 'No activity' time limit in seconds. 1200 = 20 Minutes
                idleCheckHeartbeat: 2, // Frequency to check for idle timeouts in seconds

                // optional custom callback to perform before logout
                customCallback: false, // set to false for no customCallback
                // customCallback:    function () {    // define optional custom js function
                // perform custom action before logout
                // },

                // configure which activity events to detect
                // http://www.quirksmode.org/dom/events/
                // https://developer.mozilla.org/en-US/docs/Web/Reference/Events
                activityEvents: 'click keypress scroll wheel mousewheel mousemove', // separate each event with a space

                // warning dialog box configuration
                enableDialog: true, // set to false for logout without warning dialog
                dialogDisplayLimit: 10, // Time to display the warning dialog before logout (and optional callback) in seconds. 180 = 3 Minutes
                dialogTitle: 'Your session is about to expire!', // also displays on browser title bar
                dialogText: 'Your session is about to expire.',
                dialogTimeRemaining: 'You will be logged out in ',
                dialogStayLoggedInButton: 'Stay Logged In',
                dialogLogOutNowButton: 'Logout',

                // error message if https://github.com/marcuswestin/store.js not enabled
                errorAlertMessage: 'Please disable "Private Mode", or upgrade to a modern browser. Or perhaps a dependent file missing. Please see: https://github.com/marcuswestin/store.js',

                // server-side session keep-alive timer
                sessionKeepAliveTimer: 600, // ping the server at this interval in seconds. 600 = 10 Minutes. Set to false to disable pings
                sessionKeepAliveUrl: window.location.href // set URL to ping - does not apply if sessionKeepAliveTimer: false
            },

            //##############################
            //## Private Variables
            //##############################
            currentConfig = $.extend(defaultConfig, userRuntimeConfig), // merge default and user runtime configuration
            origTitle = document.title, // save original browser title
            activityDetector,
            startKeepSessionAlive, stopKeepSessionAlive, keepSession, keepAlivePing, // session keep alive
            idleTimer, remainingTimer, checkIdleTimeout, checkIdleTimeoutLoop, startIdleTimer, stopIdleTimer, // idle timer
            openWarningDialog, dialogTimer, checkDialogTimeout, startDialogTimer, stopDialogTimer, isDialogOpen, destroyWarningDialog, countdownDisplay, // warning dialog
            logoutUser;

        //##############################
        //## Public Functions
        //##############################
        // trigger a manual user logout
        // use this code snippet on your site's Logout button: $.fn.idleTimeout().logout();
        this.logout = function() {
            store.set('idleTimerLoggedOut', true);
        };

        //##############################
        //## Private Functions
        //##############################

        //----------- KEEP SESSION ALIVE FUNCTIONS --------------//
        startKeepSessionAlive = function() {

            keepSession = function() {
                $.get(currentConfig.sessionKeepAliveUrl);
                startKeepSessionAlive();
            };

            keepAlivePing = setTimeout(keepSession, (currentConfig.sessionKeepAliveTimer * 1000));
        };

        stopKeepSessionAlive = function() {
            clearTimeout(keepAlivePing);
        };

        //----------- ACTIVITY DETECTION FUNCTION --------------//
        activityDetector = function() {

            $('body').on(currentConfig.activityEvents, function() {

                if (!currentConfig.enableDialog || (currentConfig.enableDialog && isDialogOpen() !== true)) {
                    startIdleTimer();
                    $('#activity').effect('shake'); // added for demonstration page
                }
            });
        };

        //----------- IDLE TIMER FUNCTIONS --------------//
        checkIdleTimeout = function() {

            var timeIdleTimeout = (store.get('idleTimerLastActivity') + (currentConfig.idleTimeLimit * 1000));

            if ($.now() > timeIdleTimeout) {

                if (!currentConfig.enableDialog) { // warning dialog is disabled
                    logoutUser(); // immediately log out user when user is idle for idleTimeLimit
                } else if (currentConfig.enableDialog && isDialogOpen() !== true) {
                    openWarningDialog();
                    startDialogTimer(); // start timing the warning dialog
                }
            } else if (store.get('idleTimerLoggedOut') === true) { //a 'manual' user logout?
                logoutUser();
            } else {

                if (currentConfig.enableDialog && isDialogOpen() === true) {
                    destroyWarningDialog();
                    stopDialogTimer();
                }
            }
        };

        startIdleTimer = function() {
            stopIdleTimer();
            store.set('idleTimerLastActivity', $.now());
            checkIdleTimeoutLoop();
        };

        checkIdleTimeoutLoop = function() {
            checkIdleTimeout();
            idleTimer = setTimeout(checkIdleTimeoutLoop, (currentConfig.idleCheckHeartbeat * 1000));
        };

        stopIdleTimer = function() {
            clearTimeout(idleTimer);
        };

        //----------- WARNING DIALOG FUNCTIONS --------------//
        openWarningDialog = function() {


            var dialogContent = "<div id='idletimer_warning_dialog'><p>" + currentConfig.dialogText + "</p><p style='display:inline'>" + currentConfig.dialogTimeRemaining + ": <div style='display:inline' id='countdownDisplay'></div> secs.</p></div>";

            var that = this,
                modalObj = {
                    title: currentConfig.dialogTitle,
                    htmlContent: dialogContent,
                    okText: "Stay Signed-in",
                    cancelText: 'Logout',
                    mainClass: 'modal-lg',
                    allowCancel: true,
                    okCloses: false,
                    escape: false,
                    cancellable: true,
                    width: "500px",
                    mainClass: "ideal-timeout"
                };
            var modal = new Modal(modalObj);
            modal.open();
            modal.on('ok', function() {
                if (userRuntimeConfig && userRuntimeConfig.onModalKeepAlive) {
                    userRuntimeConfig.onModalKeepAlive(); //hit session API
                }
                destroyWarningDialog();
                modal.close();
                stopDialogTimer();
                startIdleTimer();
            });
            modal.on('closeModal', function() {
                logoutUser();
            });

            countdownDisplay();

            // document.title = currentConfig.dialogTitle;

            if (currentConfig.sessionKeepAliveTimer) {
                stopKeepSessionAlive();
            }
        };

        checkDialogTimeout = function() {
            var timeDialogTimeout = (store.get('idleTimerLastActivity') + (currentConfig.idleTimeLimit * 1000) + (currentConfig.dialogDisplayLimit * 1000));

            if (($.now() > timeDialogTimeout) || (store.get('idleTimerLoggedOut') === true)) {
                logoutUser();
            }
        };

        startDialogTimer = function() {
            dialogTimer = setInterval(checkDialogTimeout, (currentConfig.idleCheckHeartbeat * 1000));
        };

        stopDialogTimer = function() {
            clearInterval(dialogTimer);
            clearInterval(remainingTimer);
        };

        isDialogOpen = function() {
            var dialogOpen = $("#idletimer_warning_dialog").is(":visible");

            if (dialogOpen === true) {
                return true;
            }
            return false;
        };

        destroyWarningDialog = function() {
            if (currentConfig.sessionKeepAliveTimer) {
                startKeepSessionAlive();
            }
        };

        countdownDisplay = function() {
            var dialogDisplaySeconds = currentConfig.dialogDisplayLimit,
                mins, secs;

            remainingTimer = setInterval(function() {
                mins = Math.floor(dialogDisplaySeconds / 60); // minutes
                if (mins < 10) { mins = '0' + mins; }
                secs = dialogDisplaySeconds - (mins * 60); // seconds
                if (secs < 10) { secs = '0' + secs; }
                $('#countdownDisplay').html(mins + ':' + secs);
                dialogDisplaySeconds -= 1;
            }, 1000);
        };

        //----------- LOGOUT USER FUNCTION --------------//
        logoutUser = function() {
            store.set('idleTimerLoggedOut', true);

            if (currentConfig.sessionKeepAliveTimer) {
                stopKeepSessionAlive();
            }

            if (currentConfig.customCallback) {
                currentConfig.customCallback();
            }

            if (currentConfig.redirectUrl) {
                window.location.href = currentConfig.redirectUrl;
            }
        };

        //###############################
        // Build & Return the instance of the item as a plugin
        // This is your construct.
        //###############################
        return this.each(function() {

            if (store.enabled) {

                store.set('idleTimerLastActivity', $.now());
                store.set('idleTimerLoggedOut', false);

                activityDetector();

                if (currentConfig.sessionKeepAliveTimer) {
                    startKeepSessionAlive();
                }

                startIdleTimer();

            } else {
                alert(currentConfig.errorAlertMessage);
            }

        });
    };

    //------------------------------------------------
    return Utils;
});