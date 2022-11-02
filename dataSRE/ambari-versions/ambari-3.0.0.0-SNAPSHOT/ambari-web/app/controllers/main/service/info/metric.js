/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.MainServiceInfoMetricsController = Em.Controller.extend(App.WidgetSectionMixin, {
  name: 'mainServiceInfoMetricsController',

  layoutNameSuffix: "_dashboard",

  sectionNameSuffix: "_SUMMARY",

  /**
   * Some widget has type `GRAPH`
   *
   * @type {boolean}
   */
  someWidgetGraphExists: Em.computed.someBy('widgets', 'widgetType', 'GRAPH'),

  /**
   * @type {boolean}
   */
  showTimeRangeControl: Em.computed.or('!isServiceWithEnhancedWidgets', 'someWidgetGraphExists'),

  /**
   * @type {boolean}
   */
  isWidgetLayoutsLoaded: false,

  /**
   * @type {boolean}
   */
  isAllSharedWidgetsLoaded: false,

  /**
   * @type {boolean}
   */
  isMineWidgetsLoaded: false,

  /**
   * load widget layouts across all users in CLUSTER scope
   * @returns {$.ajax}
   */
  loadWidgetLayouts: function () {
    this.set('isWidgetLayoutsLoaded', false);
    return App.ajax.send({
      name: 'widgets.layouts.get',
      sender: this,
      data: {
        sectionName: this.get('sectionName')
      },
      success: 'loadWidgetLayoutsSuccessCallback'
    });
  },

  loadWidgetLayoutsSuccessCallback: function (data) {
    App.widgetLayoutMapper.map(data);
    this.set('isWidgetLayoutsLoaded', true);
  },


  /**
   * load all shared widgets to show on widget browser
   * @returns {$.ajax}
   */
  loadAllSharedWidgets: function () {
    this.set('isAllSharedWidgetsLoaded', false);
    return App.ajax.send({
      name: 'widgets.all.shared.get',
      sender: this,
      success: 'loadAllSharedWidgetsSuccessCallback'
    });
  },

  /**
   * success callback of <code>loadAllSharedWidgets</code>
   * @param {object|null} data
   */
  loadAllSharedWidgetsSuccessCallback: function (data) {
    var widgetIds = this.get('widgets').mapProperty('id');
    var activeNSWidgetLayouts = this.get('activeNSWidgetLayouts');
    if (data.items[0] && data.items.length) {
      this.set("allSharedWidgets",
        data.items.filter(function (widget) {
          return widget.WidgetInfo.widget_type != "HEATMAP";
        }).map(function (widget) {
          var widgetType = widget.WidgetInfo.widget_type;
          var widgetName = widget.WidgetInfo.widget_name;
          var widgetId = widget.WidgetInfo.id;
          var widgetTag = widget.WidgetInfo.tag;
          var inNSLayouts = false;
          if (widgetTag) {
            inNSLayouts = activeNSWidgetLayouts.findProperty('nameServiceId', widgetTag).get('widgets').someProperty('id', widgetId) &&
              activeNSWidgetLayouts.findProperty('nameServiceId', 'all').get('widgets').someProperty('id', widgetId);
          }
          return Em.Object.create({
            id: widgetId,
            widgetName: widgetName,
            tag: widgetTag,
            metrics: widget.WidgetInfo.metrics,
            description: widget.WidgetInfo.description,
            widgetType: widgetType,
            iconPath: "/img/widget-" + widgetType.toLowerCase() + ".png",
            serviceName: JSON.parse(widget.WidgetInfo.metrics).mapProperty('service_name').uniq().join('-'),
            added: widgetTag ? inNSLayouts : widgetIds.contains(widgetId),
            isShared: widget.WidgetInfo.scope == "CLUSTER"
          });
        })
      );
    }
    this.set('isAllSharedWidgetsLoaded', true);
  },

  allSharedWidgets: [],
  mineWidgets: [],

  /**
   * load all mine widgets of current user to show on widget browser
   * @returns {$.ajax}
   */
  loadMineWidgets: function () {
    this.set('isMineWidgetsLoaded', false);
    return App.ajax.send({
      name: 'widgets.all.mine.get',
      sender: this,
      data: {
        loginName: App.router.get('loginName')
      },
      success: 'loadMineWidgetsSuccessCallback'
    });
  },

  /**
   * success callback of <code>loadMineWidgets</code>
   * @param {object|null} data
   */
  loadMineWidgetsSuccessCallback: function (data) {
    var widgetIds = this.get('widgets').mapProperty('id');
    var activeNSWidgetLayouts = this.get('activeNSWidgetLayouts');
    if (data.items[0] && data.items.length) {
      this.set("mineWidgets",
        data.items.filter(function (widget) {
          return widget.WidgetInfo.widget_type != "HEATMAP";
        }).map(function (widget) {
          var widgetType = widget.WidgetInfo.widget_type;
          var widgetName = widget.WidgetInfo.widget_name;
          var widgetId = widget.WidgetInfo.id;
          var widgetTag = widget.WidgetInfo.tag;
          var inNSLayouts = false;
          if (widgetTag) {
            inNSLayouts = activeNSWidgetLayouts.findProperty('nameServiceId', widgetTag).get('widgets').someProperty('id', widgetId) &&
              activeNSWidgetLayouts.findProperty('nameServiceId', 'all').get('widgets').someProperty('id', widgetId);
          }
          return Em.Object.create({
            id: widget.WidgetInfo.id,
            widgetName: widgetName,
            tag: widgetTag,
            metrics: widget.WidgetInfo.metrics,
            description: widget.WidgetInfo.description,
            widgetType: widgetType,
            iconPath: "/img/widget-" + widgetType.toLowerCase() + ".png",
            serviceName: JSON.parse(widget.WidgetInfo.metrics).mapProperty('service_name').uniq().join('-'),
            added: widgetTag ? inNSLayouts : widgetIds.contains(widgetId),
            isShared: widget.WidgetInfo.scope == "CLUSTER"
          });
        })
      );
    } else {
      this.set("mineWidgets", []);
    }
    this.set('isMineWidgetsLoaded', true);
  },

  /**
   * add widgets, on click handler for "Add"
   */
  addWidget: function (event) {
    var self = this;
    var widgetToAdd = event.context;
    var activeLayouts = widgetToAdd.tag ? this.get('activeNSWidgetLayouts').filter(function (l) {
      return ['all', widgetToAdd.tag].contains(l.get('nameServiceId'));
    }) : [this.get('activeWidgetLayout')];
    activeLayouts.forEach(function (activeLayout) {
      var widgetIds = activeLayout.get('widgets').map(function(widget) {
        return {
          "id": widget.get("id")
        }
      });
      if (!widgetIds.mapProperty('id').contains(widgetToAdd.id)) {
        widgetIds.pushObject({
          "id": widgetToAdd.id
        });
        var data = {
          "WidgetLayoutInfo": {
            "display_name": activeLayout.get("displayName"),
            "id": activeLayout.get("id"),
            "layout_name": activeLayout.get("layoutName"),
            "scope": activeLayout.get("scope"),
            "section_name": activeLayout.get("sectionName"),
            "widgets": widgetIds
          }
        };

        return App.ajax.send({
          name: 'widget.layout.edit',
          sender: self,
          data: {
            layoutId: activeLayout.get("id"),
            data: data
          },
          success: 'updateActiveLayout'
        });
      }
    });

    widgetToAdd.set('added', true);
  },

  /**
   * find and hide widgets from all layouts
   * @param event
   */
  hideWidgetBrowser: function (event) {
    var activeLayouts = event.context.tag ? this.get('activeNSWidgetLayouts').filter(function (l) {
      return l.get('widgets').mapProperty('id').contains(event.context.id);
    }) : [this.get('activeWidgetLayout')];
    activeLayouts.forEach(function(layout) {
      event.context.nsLayout = layout;
      this.hideWidget(event)
    }, this);
  },

  /**
   * hide widgets, on click handler for "Added"
   */
  hideWidget: function (event) {
    var widgetToHide = event.context;
    var widgetLayout = event.context.nsLayout;
    var activeLayout = widgetLayout || this.get('activeWidgetLayout');
    var widgetIds = activeLayout.get('widgets').map(function (widget) {
      return {
        "id": widget.get("id")
      }
    });
    var data = {
      "WidgetLayoutInfo": {
        "display_name": activeLayout.get("displayName"),
        "id": activeLayout.get("id"),
        "layout_name": activeLayout.get("layoutName"),
        "scope": activeLayout.get("scope"),
        "section_name": activeLayout.get("sectionName"),
        "widgets": widgetIds.filter(function (widget) {
          return widget.id !== widgetToHide.id;
        })
      }
    };

    widgetToHide.set('added', false);
    return App.ajax.send({
      name: 'widget.layout.edit',
      sender: this,
      data: {
        layoutId: activeLayout.get("id"),
        data: data
      },
      success: 'hideWidgetSuccessCallback'
    });

  },

  /**
   * @param {object|null} data
   * @param {object} opt
   * @param {object} params
   */
  hideWidgetSuccessCallback: function (data, opt, params) {
    params.data.WidgetLayoutInfo.widgets = params.data.WidgetLayoutInfo.widgets.map(function (widget) {
      return {
        WidgetInfo: {
          id: widget.id
        }
      }
    });
    App.widgetLayoutMapper.map({items: [params.data]});
    this.propertyDidChange('widgets');
  },

  /**
   * update current active widget layout
   */
  updateActiveLayout: function () {
    this.getActiveWidgetLayout();
  },

  /**
   * delete widgets, on click handler for "Delete"
   */
  deleteWidget: function (event) {
    var widget = event.context;
    var self = this;
    var confirmMsg =  widget.get('isShared') ? Em.I18n.t('dashboard.widgets.browser.action.delete.shared.bodyMsg').format(widget.widgetName) :  Em.I18n.t('dashboard.widgets.browser.action.delete.mine.bodyMsg').format(widget.widgetName);
    var bodyMessage = Em.Object.create({
      confirmMsg: confirmMsg,
      confirmButton: Em.I18n.t('dashboard.widgets.browser.action.delete.btnMsg')
    });
    return App.showConfirmationFeedBackPopup(function (query) {
      return App.ajax.send({
        name: 'widget.action.delete',
        sender: self,
        data: {
          id: widget.id
        },
        success: 'updateWidgetBrowser'
      });

    }, bodyMessage);
  },

  /**
   * update widget browser content after deleted some widget
   */
  updateWidgetBrowser: function () {
    this.loadAllSharedWidgets();
    this.loadMineWidgets();
  },

  /**
   * Share widgets, on click handler for "Share"
   */
  shareWidget: function (event) {
    var widget = event.context;
    var self = this;
    var bodyMessage = Em.Object.create({
      confirmMsg: Em.I18n.t('dashboard.widgets.browser.action.share.confirmation'),
      confirmButton: Em.I18n.t('dashboard.widgets.browser.action.share')
    });
    return App.showConfirmationFeedBackPopup(function (query) {
      return App.ajax.send({
        name: 'widgets.wizard.edit',
        sender: self,
        data: {
          data: {
            "WidgetInfo": {
              "widget_name": widget.get("widgetName"),
              "scope": "CLUSTER"
            }
          },
          widgetId: widget.get("id")
        },
        success: 'updateWidgetBrowser'
      });
    }, bodyMessage);
  },

  /**
   * create widget
   */
  createWidget: function () {
    App.router.send('createServiceWidget', Em.Object.create({
      layout: this.get('activeWidgetLayout'),
      serviceName: this.get('content.serviceName')
    }));
  },

  /**
   * edit widget
   * @param {App.Widget} content
   */
  editWidget: function (content) {
    content.set('serviceName', this.get('content.serviceName'));
    App.router.send('editServiceWidget', content);
  },

  /**
   * launch Widgets Browser popup
   * @method showPopup
   * @return {App.ModalPopup}
   */
  goToWidgetsBrowser: function () {
    var self = this;

    return App.ModalPopup.show({
      header: Em.I18n.t('dashboard.widgets.browser.header'),

      classNames: ['common-modal-wrapper', 'widgets-browser-popup'],
      modalDialogClasses: ['modal-lg'],
      onPrimary: function () {
        this.hide();
        self.set('isAllSharedWidgetsLoaded', false);
        self.set('allSharedWidgets', []);
        self.set('isMineWidgetsLoaded', false);
        self.set('mineWidgets', []);
      },
      autoHeight: false,
      isHideBodyScroll: false,
      footerClass: Ember.View.extend({
        templateName: require('templates/common/modal_popups/widget_browser_footer'),
        isShowMineOnly: false,
        onPrimary: function() {
          this.get('parentView').onPrimary();
        }
      }),
      isShowMineOnly: false,
      showTopShadow: false,

      didInsertElement: function() {
        this._super();
        this.$().find('.modal-body').on('scroll', (event) => {
          const modalBody = $(event.currentTarget);
          if (modalBody.scrollTop() > 0) {
            modalBody.addClass('top-shadow');
          } else {
            modalBody.removeClass('top-shadow');
          }
        });
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/common/modal_popups/widget_browser_popup'),
        controller: self,
        willInsertElement: function () {
          this.get('controller').loadAllSharedWidgets();
          this.get('controller').loadMineWidgets();
        },

        isLoaded: Em.computed.and('controller.isAllSharedWidgetsLoaded', 'controller.isMineWidgetsLoaded'),

        isWidgetEmptyList: Em.computed.empty('filteredContent'),

        activeService: '',
        activeStatus: '',

        content: function () {
          var content = [];
          if (this.get('parentView.isShowMineOnly')) {
            content = this.get('controller.mineWidgets');
          } else {
            // merge my widgets and all shared widgets, no duplicated is allowed
            var widgetMap = {};
            var allWidgets = this.get('controller.allSharedWidgets').concat(this.get('controller.mineWidgets'));
            allWidgets.forEach(function(widget) {
              if (!widgetMap[widget.get("id")]) {
                content.pushObject(widget);
                widgetMap[widget.get("id")] = true;
              }
            });
          }

          //remove NameNode widgets with no tag if federation is enabled
          if (App.get('hasNameNodeFederation')) {
            content = content.filter(function (w) {
              var parsedMetric;
              try {
                parsedMetric = JSON.parse(w.metrics);
              } catch (e) {
              }
              return w.tag || !(parsedMetric && parsedMetric.someProperty('component_name', 'NAMENODE'));
            });
          }

          return content;
        }.property('controller.allSharedWidgets.length', 'controller.isAllSharedWidgetsLoaded',
          'controller.mineWidgets.length', 'controller.isMineWidgetsLoaded', 'parentView.isShowMineOnly'),

        /**
         * displaying content filtered by service name and status.
         */
        filteredContent: function () {
          var activeService = this.get('activeService') ? this.get('activeService') : this.get('controller.content.serviceName');
          var result = [];
          this.get('content').forEach(function (widget) {
            if (widget.get('serviceName').indexOf(activeService) >= 0) {
              result.pushObject(widget);
            }
          });
          return result;
        }.property('content', 'activeService', 'activeStatus'),

        /**
         * service name filter
         */
        services: function () {
          var view = this;
          var services = App.Service.find().filter(function(item){
            var stackService =  App.StackService.find().findProperty('serviceName', item.get('serviceName'));
            return stackService.get('isServiceWithWidgets');
          });
          return services.map(function (service) {
            return Em.Object.create({
              value: service.get('serviceName'),
              label: service.get('displayName'),
              isActive: function () {
                var activeService = view.get('activeService') ? view.get('activeService') : view.get('controller.content.serviceName');
                return this.get('value') == activeService;
              }.property('value', 'view.activeService')
            })
          });
        }.property('activeService'),

        filterByService: function (event) {
          this.set('activeService', event.context);
        },

        createWidget: function () {
          this.get('parentView').onPrimary();
          this.get('controller').createWidget();
        },

        ensureTooltip: function () {
          Em.run.later(this, function () {
            App.tooltip($("[rel='shared-icon-tooltip']"));
          }, 1000);
        }.observes('activeService', 'parentView.isShowMineOnly'),

        didInsertElement: function () {
          this.ensureTooltip();
        }
      })
    });
  }

});