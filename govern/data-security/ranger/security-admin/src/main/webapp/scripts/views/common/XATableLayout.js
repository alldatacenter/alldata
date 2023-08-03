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
	var XAEnums 		= require('utils/XAEnums');
	var XALinks 		= require('modules/XALinks');
	var XAGlobals 		= require('utils/XAGlobals');
	var localization	= require('utils/XALangSupport');
	
	var XATablelayoutTmpl = require('hbs!tmpl/common/XATableLayout_tmpl');
	var Spinner		= require('views/common/Spinner');

	require('backgrid-filter');
	require('backgrid-paginator');
	require('Backgrid.ColumnManager');

	var XATableLayout = Backbone.Marionette.Layout.extend(
	/** @lends XATableLayout */
	{
		_viewName : 'XATableLayout',
		
    	template: XATablelayoutTmpl,

		/** Layout sub regions */
    	regions: {
			'rTableList'	: 'div[data-id="r_tableList"]',
			'rTableSpinner'	: 'div[data-id="r_tableSpinner"]',
			'rPagination'	: 'div[data-id="r_pagination"]'
		},

    	/** ui selector cache */
    	ui: {},

		gridOpts : {
			className: 'table table-bordered table-condensed backgrid',
			emptyText : 'No Records found!'
		},

		columnManagerOpts: {
            opts: {
                // State settings
                saveState: true,
                loadStateOnInit: true
            },
            visibilityControlOpts: {},
            el: null
        },

		filterOpts : {
			placeholder: localization.tt('plcHldr.searchByResourcePath'),
			wait: 150
		},

		includePagination : true,

		includeFilter : false,

		includeHeaderSearch : false,

		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			return events;
		},

    	/**
		* intialize a new HDXATableLayout Layout 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a XATableLayout Layout");
			/*
			 * mandatory :
			 * collection
			 * columns,
			 *
			 * optional :
			 * includePagination (true),
			 * includeFilter (false),
			 * includeHeaderSearch (false),
			 * gridOpts : {
			 * 	className: 'table table-bordered table-condensed backgrid',
			 *  row: TableRow,
			 *  header : XABackgrid,
			 *  emptyText : 'No Records found!',
			 * }
			 * filterOpts : {
			 *	name: ['name'],
			 *  placeholder: localization.tt('plcHldr.searchByResourcePath'),
			 *  wait: 150
			 * }
			 *
			 */
			_.extend(this, _.pick(options,	'collection', 'columns', 'includePagination', 'columnManagerOpts',
											'includeHeaderSearch', 'includeFilter' ,'scrollToTop', 'paginationCache', 'backgridColumnManager'));

			_.extend(this.gridOpts, options.gridOpts, { collection : this.collection, columns : this.columns } );
			_.extend(this.filterOpts, options.filterOpts);
			
			this.scrollToTop = _.has(options, 'scrollToTop') ? options.scrollToTop : true;
			this.paginationCache = _.has(options, 'paginationCache') ? options.paginationCache : true;
			
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/

			this.listenTo(this.collection, "sync reset", this.showHidePager);
			
            this.listenTo(this.collection, 'request', function(){
				this.$el.find(this.rTableSpinner.el).addClass('loading');
			},this);
            this.listenTo(this.collection, 'sync error', function(){
				this.$el.find(this.rTableSpinner.el).removeClass('loading');
			},this);
		},

		/** on render callback */
		onRender: function() {
			this.initializePlugins();
			
			this.renderTable();
			if(this.includePagination) {
				this.renderPagination();
			}
		},

		/** all post render plugin initialization */
		initializePlugins: function(){
			if (this.backgridColumnManager) {
				this.includeColumnManager()
			}
		},

		includeColumnManager : function() {
			var that = this;
			var $el = this.columnManagerOpts.el
			// Create column collection
			this.columns = new Backgrid.Columns(this.columns);
			var colManager = new Backgrid.Extension.ColumnManager(this.columns, this.columnManagerOpts.opts);
			var colVisibilityControl = new Backgrid.Extension.ColumnManagerVisibilityControl(_.extend({
                columnManager: colManager,
            }, this.columnManagerOpts.visibilityControlOpts));
            if (!$el.jquery) {
                $el = $($el)
            }
            if (this.columnManagerOpts.el) {
                $el.html(colVisibilityControl.render().el);
            } else {
                $el.append(colVisibilityControl.render().el);
            }
			this.gridOpts.columns = this.columns;
		},

        getGridObj : function(){
			if (this.rTableList.currentView){
                return this.rTableList.currentView;
			}
			return null;
		},

		renderTable : function(){
			var that = this;
			this.grid = new Backgrid.Grid(this.gridOpts);
			this.rTableList.show(this.grid);
		},
		renderPagination : function(){
			this.rPagination.show(new Backgrid.Extension.Paginator({
				collection: this.collection,
				scrollToTop : this.scrollToTop,
				paginationCache : this.paginationCache
				
			}));
		},
		showHidePager : function(){
			if (!_.isUndefined($('.latestResponse')) && $('.latestResponse').length > 0) {
				$('.latestResponse').html('<b>Last Response Time : </b>' + Globalize.format(new Date(),  "MM/dd/yyyy hh:mm:ss tt"));
			}
			$('.popover').remove();
			if(this.collection.state && this.collection.state.totalRecords > XAGlobals.settings.PAGE_SIZE)	{
				this.$el.find(this.rPagination.el).show()
			} else {
				this.$el.find(this.rPagination.el).hide();
			}
		},
		renderFilter : function(){
			this.rFilter.show(new Backgrid.Extension.ServerSideFilter({
				  collection: this.collection,
				  name: ['name'],
				  placeholder: localization.tt('plcHldr.searchByResourcePath'),
				  wait: 150
			}));
			
			setTimeout(function(){
				that.$('table').colResizable({liveDrag :true});
			},0);
		},

		/** on close */
		onClose: function(){
		}

	});

	return XATableLayout; 
});
