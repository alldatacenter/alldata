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
var validator = require('utils/validator');

App.GridFilterObject = Em.Object.extend({
  checked:false
});

App.GridFilter = Em.View.extend({
  tagName:"ul",
  classNames:['filter'],
  templateName:require('templates/common/grid/filter'),
  attributeBindings:['style'],
  getHeader:function () {
    return this.get('header')
  },
  filters:Em.computed.alias('header._filters')
});

App.GridHeader = Em.View.extend({
  templateName:require('templates/common/grid/header'),
  tagName:'th',
  filterable:true,
  showFilter:false,
  getGrid:function () {
    return this.get('grid');
  },
  _filters:[],
  doFilter:function () {
  },
  toggleFilter:function () {
    this.set('showFilter', 1 - this.get('showFilter'));
  },
  applyFilter:function () {

    var filters = this.get('_filters');
    var filterValues = [];
    $.each(filters, function(){
      if(this.get('checked')) {
        filterValues.push(this.get('value'));
      }
    });

    var grid = this.get('grid');
    grid.addFilters(this.get('name'), filterValues);
    this.set('showFilter', false);
  },
  init:function () {
    this._super();
    if (!this.get('_filters').length) {
      this.filterValues();
      var thisHeader = this;
      this.set('filter', App.GridFilter.extend({ header:thisHeader }));
    }
  },

  filterValues:function () {
    var gridFilters = this.get('grid._filters');
    if (gridFilters && gridFilters[this.get('name')]) {
      var filters = this.get('grid._filters')[this.get('name')];
      // there should be something like filter preparing
      var newFilters = [];
      $.each(filters, function (i, v) {
        newFilters.push(App.GridFilterObject.create({label:v, value:v}));
      });

      this.set('_filters', newFilters);
    }
  }.observes('grid._filters')
});

App.GridRow = Em.View.extend({
  tagName:'tr',
  init:function (options) {
    var object = this.get('object');
    var grid = this.get('grid');
    var fieldNames = grid.get('fieldNames');
    var template = '';

    if (fieldNames) {
      $.each(grid.get('fieldNames'), function (i, field) {
        template += "<td>" + object.get(field) + "</td>";
      });

      this.set('template', Em.Handlebars.compile(template));
    }
    return this._super();
  }
});

App.GridPage = Em.Object.extend({
  activeClass:Em.computed.ifThenElse('active', 'active', ''),
  active:function () {
    return parseInt(this.get('number')) == parseInt(this.get('pager.grid.currentPage'));
  }.property('pager.grid.currentPage')
});

App.GridPager = Em.View.extend({

  pages:[],
  templateName:require('templates/common/grid/pager'),
  classNames:['pagination'],

  activatePrevPage:function () {
    var current = this.get('grid.currentPage');
    if (current > 1) this.set('grid.currentPage', current - 1);
  },
  activateNextPage:function () {
    var current = this.get('grid.currentPage');
    if (current < this.get('pages').length) this.set('grid.currentPage', current + 1);
  },

  prevPageDisabled:function () {
    return this.get('grid.currentPage') > 1 ? false : "disabled";
  }.property('grid.currentPage'),

  nextPageDisabled:function () {
    return this.get('grid.currentPage') < this.get('pages').length ? false : "disabled";
  }.property('grid.currentPage'),

  init:function () {
    this._super();
    this.clearPages();
    this.pushPages();
  },

  activatePage:function (event) {
    var page = event.context;
    this.get('grid').set('currentPage', parseInt(event.context.get('number')));
  },

  clearPages:function () {
    this.set('pages', []);
  },

  pushPages:function () {
    var thisPager = this;
    var pages = this.get('grid._pager.pages');
    $.each(pages, function () {
      var thisNumber = this;
      thisPager.get('pages').push(App.GridPage.create({
        number:thisNumber,
        pager:thisPager
      }));
    })
  }.observes('grid._pager')
});

App.Grid = Em.View.extend({
  _columns:{}, // not used
  _filters:{}, // prepared filters from data values
  _pager:{pages:[1, 2, 3, 4, 5]}, // observed by pager to config it

  _collection:{className:false, staticOptions:{}}, // collection config
  currentPage:1,
  fieldNames:[],
  appliedFilters:{},
  filteredArray:[],
  columns:[],
  collection:[],
  initComleted:false,
  rows:[],
  templateName:require('templates/main/admin/audit'),

  init:function () {
    this._super();
    this.prepareColumns(); // should be the 1
    this.prepareCollection();
    this.preparePager();
  },

  preparePager:function () {
//    this.set('pager', App.GridPager.extend({ grid:this })); ask to hide
  },

  addFilters: function(field, values){
    var filters = this.get('appliedFilters');
    filters[field] = values;

    var collection = this.get('_collection.className');
    collection = collection.find();
    var arrayCollection = collection.filter(function(data) {
      var oneFilterFail = false;
      $.each(filters, function(fieldname, values){
        if(values.length && values.indexOf(data.get(fieldname)) == -1) {
          return oneFilterFail = true;
        }
      });
      return !oneFilterFail;
    });

    this.set('filteredArray', arrayCollection);
  },

  prepareCollection:function () {
    if (validator.empty(this.get('_collection.className'))) {
      throw "_collection.className field is not defined";
    }
    var collection = this.get('_collection.className');
    this.set('collection', collection.find(this.get('_collection.staticOptions')));
  },

  addColumn:function (options) {
    options.grid = this;
    if (validator.empty(options.name)) {
      throw "define column name";
    }

    if (this.get('_columns.' + options.name)) {
      throw "column with this '" + options.name + "' already exists";
    }

    var field = App.GridHeader.extend(options);
    this.columns.push(field);

    if (field.filterable || 1) { // .filterable - field not working :(
      this.fieldNames.push(options.name);
    }
  },

  clearColumns:function () {
    this.set('_columns', {});
    this.set('columns', []);
    this.set('fieldNames', []);
  },

  prepareColumns:function () {
    this.clearColumns();
  },

  prepareFilters:function () {
    var thisGrid = this;
    var collection = this.get('collection');
    var fieldNames = this.get('fieldNames');
    var options = {};

    if (collection && collection.content) {
      collection.forEach(function (object, i) {
        $.each(fieldNames, function (j, field) {
          if (!options[field]) {
            options[field] = [];
          }

          var filter = object.get(field);
          if (options[field].indexOf(filter) == -1) {
            options[field].push(filter);
          }
        });
      });

      thisGrid.set('_filters', options);
    }
  }.observes('collection.length'),


  clearRows:function () {
    this.set('rows', [])
  },

  prepareRows:function () {
    var collection = this.get('collection');
    var thisGrid = this;
    this.clearRows();
    var i=1;

    if (collection && collection.content) {
      collection.forEach(function (object, i) {
        var row = App.GridRow.extend({grid:thisGrid, object:object});
        thisGrid.rows.push(row);
      });
    }
  }.observes('collection.length'),

  filteredRows:function () {
    var collection = this.get('filteredArray');
    var thisGrid = this;
    this.clearRows();

    collection.forEach(function (object) {
      var row = App.GridRow.extend({grid:thisGrid, object:object});
      thisGrid.rows.push(row);
    });
  }.observes('filteredArray')
});