/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 
define(function(require){
    'use strict';

	var Backbone		= require('backbone');
	var Communicator	= require('communicator');
	var XAUtil			= require('utils/XAUtils');
	var XAGlobals 	= require('utils/XAGlobals');

	require('backgrid-filter');
	require('backgrid-paginator');
	require('select2');


	var HeaderSearchCell = Backbone.View.extend({

		tagName: "td",

		className: "backgrid-filter",

		template: _.template('<input type="search" <% if (placeholder) { %> placeholder="<%- placeholder %>" <% } %> name="<%- name %>" <% if (style) { %> style="<%- style %>" <% } %> />'),
							 //<a class="clear" href="#">&times;</a>'),

		placeholder: "",

		events: {
			"keyup input": "evKeyUp",
			"submit": "search"
		},

		initialize: function (options) {
			_.extend(this, _.pick(options, 'column'));
			this.name = this.column.get('name');
			if(this.column.get('reName') != undefined)
				this.name = this.column.get('reName');
				

			var collection = this.collection, self = this;
			if (Backbone.PageableCollection && collection instanceof Backbone.PageableCollection ) {
				collection.queryParams[this.name] = function () {
					return self.searchBox().val() || null;
				};
			}
		},

		render: function () {
			this.$el.empty().append(this.template({
				name: this.column.get('name'),
				placeholder: this.column.get('placeholder') || "Search",
				style : this.column.get('headerSearchStyle')
			}));
			this.$el.addClass('renderable');
			this.delegateEvents();
			return this;

		},

		evKeyUp: function (e) {
			var $clearButton = this.clearButton();
			var searchTerms = this.searchBox().val();

			if(!e.shiftKey)	this.search();
			
			if (searchTerms) {
				$clearButton.show();
			} else {
				$clearButton.hide();
			}
		},

		searchBox: function () {
			return this.$el.find("input[type=search]");
		},

		clearButton: function () {
			return this.$el.find(".clear");
		},

		search: function () {

			var data = {};

			// go back to the first page on search
			var collection = this.collection;
			if (Backbone.PageableCollection &&
				collection instanceof Backbone.PageableCollection &&
					collection.mode == "server") {
				collection.state.currentPage = collection.state.firstPage;
			}
			var query = this.searchBox().val();
			if (query) data[this.name] = query;
			if(collection.extraSearchParams){
				_.extend(data, collection.extraSearchParams);
			}
			collection.fetch({
				data: data,
				reset: true,
				success : function(){},
				error  : function(msResponse){
					XAUtil.notifyError('Error', 'Invalid input data!');
				}
			});
		},

		clear: function (e) {
			if (e) e.preventDefault();
			this.searchBox().val(null);
			this.collection.fetch({reset: true});
		}

	});
	
	var HeaderMultiSelectSearchCell = Backbone.View.extend({

		tagName: "td",

		className: "backgrid-filter",

		template: _.template('<input type="search" name="<%- name %>" />'),
							 //<a class="clear" href="#">&times;</a>'),

		placeholder: "",

		events: {
			"change input": "evKeyUp",
			"submit": "search"
		},

		initialize: function (options) {
			_.extend(this, _.pick(options, 'column'));
			this.name = this.column.get('name');
			if(this.column.get('reName') != undefined)
				this.name = this.column.get('reName');
				
			this.pluginAttr = this.column.has('optSelect2') ? this.column.get('optSelect2') : {};
			var collection = this.collection, self = this;
			if (Backbone.PageableCollection && collection instanceof Backbone.PageableCollection ) {
				collection.queryParams[this.name] = function () {
					return self.searchBox().val() || null;
				};
			}
		},

		render: function () {
			this.$el.empty().append(this.template({
				name: this.column.get('name'),
				style : this.column.get('headerSearchStyle')
			}));
			this.$el.find('input').select2(this.pluginAttr);
			this.$el.addClass('renderable');
			this.delegateEvents();
			return this;

		},

		evKeyUp: function (e) {
			var $clearButton = this.clearButton();
			var searchTerms = this.searchBox().val();

			if(!e.shiftKey)	this.search();
			
			if (searchTerms) {
				$clearButton.show();
			} else {
				$clearButton.hide();
			}
		},

		searchBox: function () {
			return this.$el.find("input[type=search]");
		},

		clearButton: function () {
			return this.$el.find(".clear");
		},

		search: function () {

			var data = {};

			// go back to the first page on search
			var collection = this.collection;
			if (Backbone.PageableCollection &&
				collection instanceof Backbone.PageableCollection &&
					collection.mode == "server") {
				collection.state.currentPage = collection.state.firstPage;
			}
			var query = this.searchBox().val();
			if (query) data[this.name] = query;
			if(collection.extraSearchParams){
				_.extend(data, collection.extraSearchParams);
			}
			collection.fetch({
				data: data,
				reset: true,
				success : function(){},
				error  : function(msResponse){
					XAUtil.notifyError('Error', 'Invalid input data!');
				}
			});
		},

		clear: function (e) {
			if (e) e.preventDefault();
			this.searchBox().val(null);
			this.collection.fetch({reset: true});
		}

	});
	
	
	var HeaderFilterCell = Backbone.View.extend({

		tagName: "td",

		className: "backgrid-filter",

		template: _.template('<select >  <option>ALL</option>\
				 				<% _.each(list, function(data) {\
				 						if(_.isObject(data)){ %>\
				 							<option value="<%= data.value %>"><%= data.label %></option>\
				 						<% }else{ %>\
												<option value="<%= data %>"><%= data %></option>\
				 						<% } %>\
				 				<% }); %></select>') ,
					 
		placeholder: "",

		events: {
			"click": function(){
			}
		},

		initialize: function (options) {
			_.extend(this, _.pick(options, 'column'));
			this.name = this.column.get('name');
			this.headerFilterOptions = this.column.get('headerFilterOptions');
		},

		render: function () {
			var that =this;
			this.$el.empty().append(this.template({
				name: this.column.get('name'),
				list : this.headerFilterOptions.filterList
			}));
			
			this.$el.find('select').select2({
				allowClear: true,
				closeOnSelect : false,
				width : this.headerFilterOptions.filterWidth || '100%' ,
				height : this.headerFilterOptions.filterHeight || '20px'
			});
			
			this.$el.addClass('renderable');
			
			this.$el.find('select').on('click',function(e){
				that.search(e.currentTarget.value);
			});
			return this;

		},

		

		search: function (selectedOptionValue) {

			var data = {};

			// go back to the first page on search
			var collection = this.collection;
			if (Backbone.PageableCollection &&
				collection instanceof Backbone.PageableCollection &&
					collection.mode == "server") {
				collection.state.currentPage = collection.state.firstPage;
			}
			if(selectedOptionValue != "ALL") var query = selectedOptionValue;
			if (query) data[this.name] = query;
			if(collection.extraSearchParams){
				_.extend(data, collection.extraSearchParams);
			}
			collection.fetch({data: data, reset: true});
		}

	});
	
	var HeaderRow = Backgrid.Row.extend({

		requiredOptions: ["columns", "collection"],

		initialize: function () {
			Backgrid.Row.prototype.initialize.apply(this, arguments);
		},

		makeCell: function (column, options) {
			var headerCell;
			switch(true){
				case (column.has('canHeaderSearch') && column.get('canHeaderSearch') === true) :
					headerCell = new HeaderSearchCell({
						column		: column,
						collection	: this.collection
					});
					break;
				case (column.has('canHeaderMultiSelectSearch') && column.get('canHeaderMultiSelectSearch') === true) :
					headerCell = new HeaderMultiSelectSearchCell({
						column		: column,
						collection	: this.collection
					});
					break;	
				case (column.has('canHeaderFilter') && column.get('canHeaderFilter') === true) :
					headerCell = new HeaderFilterCell({
						column		: column,
						collection	: this.collection
					});
					break;
				default :
					headerCell = new Backbone.View({
						tagName : 'td'
					});
			}
			return headerCell;
		}

	});



	var XAHeader = Backgrid.Header.extend({

		initialize: function (options) {
			var args = Array.prototype.slice.apply(arguments);
			Backgrid.Header.prototype.initialize.apply(this, args);

			this.searchRow = new HeaderRow({
				columns: this.columns,
				collection: this.collection
			});
		},

		/**
		  Renders this table head with a single row of header cells.
		  */
		render: function () {
			var that = this;
			var args = Array.prototype.slice.apply(arguments);
			Backgrid.Header.prototype.render.apply(this, args);

			this.$el.append(this.searchRow.render().$el);
			//Add title to table header cell
			this.columns.models.filter(function(m){
				if(m.has('fullDescription')){
					that.$el.find('.'+m.get('name')).attr('title', m.get("fullDescription"))
				}
			})
			return this;
		},

		remove: function () {
			var args = Array.prototype.slice.apply(arguments);
			Backgrid.Header.prototype.remove.apply(this, args);

			this.searchRow.remove.apply(this.searchRow, arguments);
			return Backbone.View.prototype.remove.apply(this, arguments);
		}

	});

	return XAHeader;
});
