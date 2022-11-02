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

var App = require('app');

const WidgetObject = Em.Object.extend({
  id: '',
  threshold: '',
  viewClass: null,
  sourceName: '',
  title: '',
  checked: false,
  isVisible: true
});

const plusButtonFilterView = Ember.View.extend({
  tagName: 'ul',
  classNames: ['dropdown-menu'],
  templateName: require('templates/main/dashboard/plus_button_filter'),
  valueBinding: '',
  widgetCheckbox: App.CheckboxView.extend({
    didInsertElement: function () {
      $('.checkbox').click(function (event) {
        event.stopPropagation();
      });
    }
  }),
  applyFilter: function () {
    const parent = this.get('parentView'),
      hiddenWidgets = this.get('hiddenWidgets');
    hiddenWidgets.filterProperty('checked').setEach('isVisible', true);
    parent.saveWidgetsSettings();
  }
});

App.MainDashboardWidgetsView = Em.View.extend(App.Persist, App.LocalStorage, App.TimeRangeMixin, {
  name: 'mainDashboardWidgetsView',
  templateName: require('templates/main/dashboard/widgets'),

  widgetsDefinition: require('data/dashboard_widgets'),

  widgetsDefinitionMap: function () {
    return this.get('widgetsDefinition').toMapByProperty('id');
  }.property('widgetsDefinition.[]'),

  widgetGroups: [],

  widgetGroupsDeferred: $.Deferred(),

  displayedWidgetGroups: Em.computed.filterBy('widgetGroups', 'isDisplayed', true),

  setWidgetGroups: function () {
    if (App.get('router.clusterController.isHDFSNameSpacesLoaded')) {
      let groups = [];
      const hdfsService = App.HDFSService.find().objectAt(0),
        hdfsMasterGroups = hdfsService ? hdfsService.get('masterComponentGroups') : [];
      this.removeObserver('App.router.clusterController.isHDFSNameSpacesLoaded', this, 'setWidgetGroups');
      if (hdfsMasterGroups.length) {
        const nameSpacesListItems = hdfsMasterGroups.map(nameSpace => {
          const {name, title} = nameSpace;
          return {
            name,
            title,
            isActive: false
          };
        });
        groups.push(Em.Object.create({
          name: 'nn',
          title: Em.I18n.t('dashboard.widgets.nameSpace'),
          serviceName: 'HDFS',
          subGroups: [
            {
              name: '*',
              title: Em.I18n.t('common.all'),
              isActive: true
            },
            ...nameSpacesListItems
          ],
          activeSubGroup: Em.computed.findBy('subGroups', 'isActive', true),
          allWidgets: this.get('allNameNodeWidgets'),
          isDisplayed: App.get('hasNameNodeFederation')
        }));
      }
      this.set('widgetGroups', groups);
      this.get('widgetGroupsDeferred').resolve();
    }
  },

  /**
   * List of services
   * @type {Ember.Enumerable}
   */
  content: [],

  /**
   * Key-name to store data in Local Storage and Persist
   * @type {string}
   */
  persistKey: Em.computed.format('user-pref-{0}-dashboard', 'App.router.loginName'),

  /**
   * @type {boolean}
   */
  isDataLoaded: false,

  /**
   * Define if some widget is currently moving
   * @type {boolean}
   */
  isMoving: false,

  /**
   * @type {WidgetObject[]}
   */
  allWidgets: [],

  allNameNodeWidgets: [],

  /**
   * List of visible widgets
   *
   * @type {WidgetObject[]}
   */
  visibleWidgets: Em.computed.filterBy('allWidgets', 'isVisible', true),

  /**
   * List of hidden widgets
   *
   * @type {WidgetObject[]}
   */
  hiddenWidgets: Em.computed.filterBy('allWidgets', 'isVisible', false),

  timeRangeClassName: 'pull-left',

  /**
   * Example:
   * {
   *   visible: [1, 2, 4],
   *   hidden: [3, 5],
   *   threshold: {
   *     1: [80, 90],
   *     2: [],
   *     3: [1, 2]
   *   }
   * }
   * @type {Object|null}
   */
  userPreferences: null,

  didInsertElement: function () {
    this._super();
    if (App.get('router.clusterController.isHDFSNameSpacesLoaded')) {
      this.setWidgetGroups();
    } else {
      this.addObserver('App.router.clusterController.isHDFSNameSpacesLoaded', this, 'setWidgetGroups');
    }
    this.loadWidgetsSettings().complete(() => {
      this.get('widgetGroupsDeferred').done(() => {
        this.checkServicesChange();
        this.renderWidgets();
        this.set('isDataLoaded', true);
        App.loadTimer.finish('Dashboard Metrics Page');
        Em.run.next(this, 'makeSortable');
        if (this.get('displayedWidgetGroups.length')) {
          Em.run.next(this, 'makeGroupedWidgetsSortable');
        }
      });
    });
  },

  /**
   * Set visibility-status for widgets
   */
  loadWidgetsSettings: function () {
    return this.getUserPref(this.get('persistKey'));
  },

  /**
   * make POST call to save settings
   * format settings if they are not provided
   *
   * @param {object} [settings]
   */
  saveWidgetsSettings: function (settings) {
    let userPreferences = this.get('userPreferences');
    let newSettings = {
      visible: [],
      hidden: [],
      threshold: {},
      groups: {}
    };
    if (arguments.length === 1) {
      newSettings = settings;
    }
    else {
      newSettings.threshold = userPreferences.threshold;
      this.get('allWidgets').forEach(widget => {
        const key = widget.get('isVisible') ? 'visible' : 'hidden';
        newSettings[key].push(widget.get('id'));
      });
      this.get('widgetGroups').forEach(group => {
        const groupName = group.get('name');
        if (!newSettings.groups[groupName]) {
          newSettings.groups[groupName] = {};
        }
        group.get('allWidgets').forEach(widgetsSubGroup => {
          const subGroupName = widgetsSubGroup.get('subGroupName'),
            widgets = widgetsSubGroup.get('widgets'),
            subGroupSettings = {
              visible: [],
              hidden: [],
              threshold: {}
            };
          newSettings.groups[groupName][subGroupName] = subGroupSettings;
          widgets.forEach(widget => {
            const key = widget.get('isVisible') ? 'visible' : 'hidden',
              threshold = widget.get('threshold'),
              widgetId = widget.get('id'),
              id = parseInt(widgetId);
            if (subGroupName === '*') {
              const regExp = new RegExp(`\\d+\\-${groupName}\\-([\\W\\w]+)\\-\\*`),
                regExpMatch = widgetId.match(regExp),
                subGroup = regExpMatch && regExpMatch[1];
              if (subGroup) {
                subGroupSettings[key].push({
                  id,
                  subGroup
                });
                $.extend(true, subGroupSettings.threshold, {
                  [subGroup]: {
                    [id]: threshold
                  }
                });
              }
            } else {
              subGroupSettings[key].push(id);
              subGroupSettings.threshold[id] = threshold;
            }
          });
        });
      });
    }
    this.get('widgetGroups').forEach(group => {
      const groupName = group.get('name');
      group.get('allWidgets').forEach(widgetsSubGroup => {
        const subGroupName = widgetsSubGroup.get('subGroupName'),
          widgets = widgetsSubGroup.get('widgets'),
          arrayFromSettings = newSettings.groups[groupName][subGroupName].visible,
          isSubGroupForAll = subGroupName === '*',
          orderedArray = isSubGroupForAll
            ? arrayFromSettings.map(widget => `${widget.id}-${groupName}-${widget.subGroup}-*`)
            : arrayFromSettings;
        widgetsSubGroup.set('widgets', widgets.sort((widgetA, widgetB) => {
          const idA = isSubGroupForAll ? widgetA.get('id') : parseInt(widgetA.get('id')),
            idB = isSubGroupForAll ? widgetB.get('id') : parseInt(widgetB.get('id')),
            indexA = orderedArray.indexOf(idA),
            indexB = orderedArray.indexOf(idB);
          return indexA > -1 && indexB > -1 ? indexA - indexB : 0;
        }));
      });
    });
    this.set('userPreferences', newSettings);
    this.setDBProperty(this.get('persistKey'), newSettings);
    this.postUserPref(this.get('persistKey'), newSettings);
  },

  getUserPrefSuccessCallback: function (response) {
    if (response) {
      this.set('userPreferences', response);
    } else {
      this.getUserPrefErrorCallback();
    }
  },

  getUserPrefErrorCallback: function () {
    var userPreferences = this.generateDefaultUserPreferences();
    this.saveWidgetsSettings(userPreferences);
  },

  resolveConfigDependencies: function(widgetsDefinition) {
    var clusterEnv = App.router.get('clusterController.clusterEnv.properties') || {};
    if (clusterEnv.hide_yarn_memory_widget === 'true') {
      widgetsDefinition.findProperty('id', 20).isHiddenByDefault = true;
    }
    return widgetsDefinition;
  },

  getWidgetSubGroupsObject: function (subGroups) {
    return subGroups.reduce((current, subGroup) => {
      return Object.assign({}, current, {
        [subGroup.name]: {
          visible: [],
          hidden: [],
          threshold: {}
        }
      });
    }, {});
  },

  generateDefaultUserPreferences: function() {
    var widgetsDefinition = this.get('widgetsDefinition');
    var preferences = {
      visible: [],
      hidden: [],
      threshold: {},
      groups: {}
    };

    this.resolveConfigDependencies(widgetsDefinition);
    widgetsDefinition.forEach(widget => {
      const {sourceName, id, groupName} = widget,
        widgetGroups = this.get('displayedWidgetGroups');
      if (App.Service.find(sourceName).get('isLoaded') || sourceName === 'HOST_METRICS') {
        const state = widget.isHiddenByDefault ? 'hidden' : 'visible',
          {threshold} = widget,
          widgetGroup = widgetGroups.find(group => {
            return group.get('serviceName') === sourceName && group.get('name') === groupName;
          });
        if (widgetGroup) {
          const widgetGroupName = widgetGroup.get('name'),
            allSubGroups = widgetGroup.get('subGroups'),
            subGroupForAllItems = allSubGroups.findProperty('name', '*'),
            subGroups = allSubGroups.rejectProperty('name', '*'),
            existingEntry = preferences.groups[widgetGroupName],
            currentEntry = existingEntry || this.getWidgetSubGroupsObject(allSubGroups);
          subGroups.forEach(subGroup => {
            const {name} = subGroup;
            currentEntry[name][state].push(id);
            currentEntry[name].threshold[id] = threshold;
            if (subGroupForAllItems) {
              currentEntry['*'][state].push({
                id,
                subGroup: name
              });
              currentEntry['*'].threshold[name] = Object.assign({}, currentEntry['*'].threshold[name], {
                [id]: threshold
              });
            }
          });
          if (!existingEntry) {
            preferences.groups[widgetGroupName] = currentEntry;
          }
        } else {
          preferences[state].push(id);
        }
      }
      preferences.threshold[id] = widget.threshold;
    });

    return preferences;
  },

  /**
   * Don't show widget on the Dashboard
   *
   * @param {number|string} id
   * @param {string} [groupId]
   * @param {string} [subGroupId]
   * @param {boolean} [isAllItemsSubGroup]
   */
  hideWidget(id, groupId, subGroupId, isAllItemsSubGroup) {
    const idToNumber = Number(id);
    if (isNaN(idToNumber)) {
      const subGroupToFilter = isAllItemsSubGroup ? '*' : subGroupId,
        groupWidgets = this.get('displayedWidgetGroups').findProperty('name', groupId).get('allWidgets'),
        subGroupWidgets = groupWidgets && groupWidgets.findProperty('subGroupName', subGroupToFilter).get('widgets'),
        targetWidget = subGroupWidgets && subGroupWidgets.findProperty('id', id);
      if (targetWidget) {
        targetWidget.set('isVisible', false);
      }
    } else {
      this.get('allWidgets').findProperty('id', id).set('isVisible', false);
    }
    this.saveWidgetsSettings();
  },

  /**
   *
   * @param {number} id
   * @param {boolean} isVisible
   * @param {string} subGroupId
   * @returns {WidgetObject}
   * @private
   */
  _createWidgetObj(id, isVisible, subGroupId) {
    var widget = this.get('widgetsDefinitionMap')[id];
    return WidgetObject.create({
      id,
      threshold: this.get('userPreferences.threshold')[id],
      viewClass: App[widget.viewName].extend({
        subGroupId
      }),
      sourceName: widget.sourceName,
      title: widget.title,
      isVisible
    });
  },

  /**
   *
   * @param {number} id
   * @param {boolean} isVisible
   * @param {string} groupId
   * @param {string} subGroupId
   * @param {boolean} isAllSubGroupsDisplay
   * @returns {WidgetObject}
   * @private
   */
  _createGroupWidgetObj(id, isVisible, groupId, subGroupId, isAllSubGroupsDisplay = false) {
    const widget = this.get('widgetsDefinitionMap')[id];
    subGroupId = subGroupId || 'default';
    return WidgetObject.create({
      id: `${id}-${groupId}-${subGroupId}${isAllSubGroupsDisplay ? '-*' : ''}`,
      threshold: isAllSubGroupsDisplay ?
        this.get('userPreferences.groups')[groupId]['*'].threshold[subGroupId][id] :
        this.get('userPreferences.groups')[groupId][subGroupId].threshold[id],
      viewClass: App[widget.viewName].extend({
        subGroupId,
        isAllItemsSubGroup: isAllSubGroupsDisplay
      }),
      sourceName: widget.sourceName,
      title: `${widget.title} - ${subGroupId}`,
      isVisible
    });
  },

  findWidgetInAllItemsSubGroup: function (id, subGroup) {
    return widget => widget.id === id && widget.subGroup === subGroup;
  },

  /**
   * set widgets to view in order to render
   */
  renderWidgets: function () {
    const userPreferences = this.get('userPreferences'),
      widgetsDefinition = this.get('widgetsDefinition'),
      widgetGroups = this.get('widgetGroups');
    let newVisibleWidgets = [],
      newHiddenWidgets = [];
    widgetGroups.forEach(group => group.get('allWidgets').clear());
    widgetsDefinition.forEach(widget => {
      const {id, groupName} = widget,
        widgetGroup = widgetGroups.findProperty('name', groupName);
      if (groupName && widgetGroup && widgetGroup.get('isDisplayed')) {
        const groupPreferences = userPreferences.groups[groupName];
        if (groupPreferences) {
          const subGroupNames = widgetGroup.get('subGroups').mapProperty('name').without('*'),
            subGroupForAllItems = widgetGroup.get('subGroups').findProperty('name', '*');
          let allWidgets = widgetGroup.get('allWidgets');
          subGroupNames.forEach(subGroupName => {
            const subGroupPreferences = groupPreferences[subGroupName],
              existingSubGroup = allWidgets.findProperty('subGroupName', subGroupName),
              currentSubGroup = existingSubGroup || Em.Object.create({
                  subGroupName,
                  title: subGroupName,
                  parentGroup: widgetGroup,
                  isActive: Em.computed.equal('parentGroup.activeSubGroup.name', subGroupName),
                  widgets: [],
                  hiddenWidgets: Em.computed.filterBy('widgets', 'isVisible', false)
                }),
              visibleIndex = subGroupPreferences.visible.indexOf(id),
              hiddenIndex = subGroupPreferences.hidden.indexOf(id),
              visibleCount = subGroupPreferences.visible.length;
            if (!existingSubGroup) {
              allWidgets.pushObject(currentSubGroup);
            }
            if (visibleIndex > -1) {
              currentSubGroup.get('widgets')[visibleIndex] = this._createGroupWidgetObj(id, true, groupName, subGroupName);
            }
            if (hiddenIndex > -1) {
              currentSubGroup.get('widgets')[hiddenIndex + visibleCount] = this._createGroupWidgetObj(id, false, groupName, subGroupName);
            }
          });
          if (subGroupForAllItems) {
            const subGroupPreferences = groupPreferences['*'],
              existingSubGroup = allWidgets.findProperty('subGroupName', '*'),
              currentSubGroup = existingSubGroup || Em.Object.create({
                  subGroupName: '*',
                  title: Em.I18n.t('common.all'),
                  parentGroup: widgetGroup,
                  isActive: Em.computed.equal('parentGroup.activeSubGroup.name', '*'),
                  widgets: [],
                  hiddenWidgets: Em.computed.filterBy('widgets', 'isVisible', false)
                });
            if (!existingSubGroup) {
              allWidgets.pushObject(currentSubGroup);
            }
            const visibleItems = subGroupPreferences.visible.filterProperty('id', id),
              hiddenItems = subGroupPreferences.hidden.filterProperty('id', id),
              visibleCount = subGroupPreferences.visible.length;
            let widgets = [];
            visibleItems.forEach(widget => {
              const subgroupName = widget.subGroup,
                findFunction = this.findWidgetInAllItemsSubGroup(id, subgroupName),
                index = subGroupPreferences.visible.findIndex(findFunction);
              currentSubGroup.get('widgets')[index] = this._createGroupWidgetObj(id, true, groupName, subgroupName, true);
            });
            hiddenItems.forEach(widget => {
              const subgroupName = widget.subGroup,
                findFunction = this.findWidgetInAllItemsSubGroup(id, subgroupName),
                index = subGroupPreferences.hidden.findIndex(findFunction);
              currentSubGroup.get('widgets')[index + visibleCount] = this._createGroupWidgetObj(id, false, groupName, subgroupName, true);
            });
          }
          allWidgets.forEach(subGroup => {
            const widgets = subGroup.get('widgets');
          });
        }
      } else {
        const subGroupId = widgetGroup ? widgetGroup.get('subGroups.lastObject.name') : 'default',
          visibleIndex = userPreferences.visible.indexOf(id),
          hiddenIndex = userPreferences.hidden.indexOf(id);
        if (visibleIndex > -1) {
          newVisibleWidgets[visibleIndex] = this._createWidgetObj(id, true, subGroupId);
        }
        if (hiddenIndex > -1) {
          newHiddenWidgets[hiddenIndex] = this._createWidgetObj(id, false, subGroupId);
        }
      }
    });
    newVisibleWidgets = newVisibleWidgets.filter(widget => !Em.isNone(widget));
    newHiddenWidgets = newHiddenWidgets.filter(widget => !Em.isNone(widget));
    this.set('allWidgets', newVisibleWidgets.concat(newHiddenWidgets));
  },

  /**
   * Check if any services with widgets were added or deleted.
   * Update the value on server if true.
   */
  checkServicesChange: function () {
    const userPreferences = this.get('userPreferences'),
      defaultPreferences = this.generateDefaultUserPreferences();
    let newValue = {
        visible: userPreferences.visible.slice(0),
        hidden: userPreferences.hidden.slice(0),
        threshold: userPreferences.threshold,
        groups: $.extend(true, {}, userPreferences.groups)
      },
      isChanged = false;

    ['visible', 'hidden'].forEach(state => {
      defaultPreferences[state].forEach(id => {
        if (!userPreferences.visible.contains(id) && !userPreferences.hidden.contains(id)) {
          isChanged = true;
          newValue[state].push(id);
        }
      });
      userPreferences[state].forEach(id => {
        if (!defaultPreferences.visible.contains(id) && !defaultPreferences.hidden.contains(id)) {
          isChanged = true;
          newValue[state] = newValue[state].without(id);
        }
      });
      Object.keys(defaultPreferences.groups).forEach(groupName => {
        const groupPreferences = defaultPreferences.groups[groupName],
          subGroupForAllItems = groupPreferences['*'],
          subGroups = Object.keys(groupPreferences).without('*');
        subGroups.forEach(subGroupName => {
          groupPreferences[subGroupName][state].forEach(id => {
            if (!newValue.groups[groupName] || !newValue.groups[groupName][subGroupName]) {
              $.extend(true, newValue.groups, {
                [groupName]: {
                  [subGroupName]: {
                    visible: [],
                    hidden: [],
                    threshold: defaultPreferences.groups[groupName][subGroupName].threshold
                  }
                }
              });
            }
            const subGroupPreferences = newValue.groups[groupName][subGroupName];
            if (!subGroupPreferences.visible.contains(id) && !subGroupPreferences.hidden.contains(id)) {
              isChanged = true;
              subGroupPreferences[state].push(id);
            }
          });
        });
        if (subGroupForAllItems) {
          subGroupForAllItems[state].forEach(item => {
            const {id, subGroup} = item;
            if (!newValue.groups[groupName]['*']) {
              $.extend(true, newValue.groups, {
                [groupName]: {
                  '*': {
                    visible: [],
                    hidden: [],
                    threshold: defaultPreferences.groups[groupName]['*'].threshold
                  }
                }
              });
            }
            const preferences = newValue.groups[groupName]['*'],
              checkFunction = this.findWidgetInAllItemsSubGroup(id, subGroup);
            if (!preferences.visible.some(checkFunction) && !preferences.hidden.some(checkFunction)) {
              isChanged = true;
              preferences[state].push({
                id,
                subGroup
              });
            }
            if (!preferences.threshold[subGroup]) {
              isChanged = true;
              preferences.threshold[subGroup] = defaultPreferences.groups[groupName]['*'].threshold[subGroup];
            }
          });
        }
      });
    });
    if (isChanged) {
      this.saveWidgetsSettings(newValue);
    }
  },

  /**
   * Reset widgets visibility-status
   */
  resetAllWidgets: function () {
    App.showConfirmationPopup(() => {
      this.saveWidgetsSettings(this.generateDefaultUserPreferences());
      this.setProperties({
        currentTimeRangeIndex: 0,
        customStartTime: null,
        customEndTime: null
      });
      this.renderWidgets();
    });
  },

  /**
   * Make widgets' list sortable on New Dashboard style
   */
  makeSortable: function () {
    var self = this;
    return $("#sortable").sortable({
      items: "> div",
      cursor: "move",
      tolerance: "pointer",
      scroll: false,
      update: function () {
        var widgetsArray = $('#sortable div[viewid]');

        var userPreferences = self.get('userPreferences') || self.getDBProperty(self.get('persistKey'));
        var newValue = {
          visible: [],
          hidden: userPreferences.hidden,
          threshold: userPreferences.threshold,
          groups: userPreferences.groups
        };
        newValue.visible = userPreferences.visible.map((item, index) => {
          var viewID = widgetsArray.get(index).getAttribute('viewid');
          return Number(viewID.split('-')[1]);
        });
        self.saveWidgetsSettings(newValue);
      },
      activate: function () {
        self.set('isMoving', true);
      },
      deactivate: function () {
        self.set('isMoving', false);
      }
    }).disableSelection();
  },

  makeGroupedWidgetsSortable: function () {
    this.get('displayedWidgetGroups').forEach(widgetGroup => {
      const selector = `#${widgetGroup.get('name')}`;
      $(selector).sortable({
        items: '> div',
        cursor: 'move',
        tolerance: 'pointer',
        scroll: false,
        update: () => {
          let isSubGroupForAllItems = false;
          const widgetsArray = $(`${selector} div[viewid]`),
            userPreferences = this.get('userPreferences') || this.getDBProperty(self.get('persistKey')),
            currentWidgetsData = widgetsArray.toArray().map(widget => {
              const viewId = widget.getAttribute('viewid'),
                splittedViewId = viewId.split('-');
              if (splittedViewId.length > 4) {
                isSubGroupForAllItems = true;
              }
              return {
                id: Number(splittedViewId[1]),
                groupName: splittedViewId[2],
                subGroupName: splittedViewId.slice(3, isSubGroupForAllItems ?
                splittedViewId.length - 1 : splittedViewId.length).join('-')
              };
            }),
            {groupName} = currentWidgetsData[0],
            subGroupName = isSubGroupForAllItems ? '*' : currentWidgetsData[0].subGroupName,
            groupPreferences = userPreferences.groups[groupName][subGroupName],
            newSubGroupValue = {
              visible: groupPreferences.visible.map((item, index) => {
                const widget = currentWidgetsData[index];
                if (isSubGroupForAllItems) {
                  return {
                    id: widget.id,
                    subGroup: widget.subGroupName
                  };
                } else {
                  return widget.id;
                }
              })
            },
            newValue = {
              visible: userPreferences.visible,
              hidden: userPreferences.hidden,
              threshold: userPreferences.threshold,
              groups: $.extend(true, userPreferences.groups, {
                [groupName]: {
                  [subGroupName]: newSubGroupValue
                }
              })
            };
          this.saveWidgetsSettings(newValue);
        },
        activate: () => {
          this.set('isMoving', true);
        },
        deactivate: () => {
          this.set('isMoving', false);
        }
      }).disableSelection();
    });
  },

  /**
   * Submenu view for New Dashboard style
   * @type {Ember.View}
   * @class
   */
  plusButtonFilterView: plusButtonFilterView.extend({
    hiddenWidgetsBinding: 'parentView.hiddenWidgets'
  }),

  groupWidgetsFilterView: plusButtonFilterView.extend(),

  showAlertsPopup: Em.K,

  setActiveSubGroup: function (event) {
    const subGroups = event.contexts[0] || [];
    subGroups.forEach(subGroup => Em.set(subGroup, 'isActive', Em.get(subGroup, 'name') === event.contexts[1]));
  }

});
