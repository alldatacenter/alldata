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
	require('backgrid');
/*
			SubgridCustomRow is a simple container view that takes a grid instance and renders a
			grid within the specific column.
			@extends Backbone.View
		   */
		  var SubgridCustomRow = Backgrid.SubgridCustomRow = Backbone.View.extend({

			  tagName: "tr",

			  className: "backgrid-subgrid-custom-row",

			  submodel: Backbone.Model.extend({}),

			  /**
			   * Initializes a row view instance.
			   * @param {Object} options
			   * @param {Backbone.Collection.<Backgrid.Column>|Array.<Backgrid.Column>|Array.<Object>} options.columns Column metadata.
			   * @param {Backbone.Model} options.model The model instance to render.
			   * @throws {TypeError} If options.columns or options.model is undefined.
			   */
			  //params: options.columnName, options.labelName , options.columnCollectionName
			  //define model.attributes.<columnName>Collection eg. model.attributes.colnameCollection
			  initialize: function (options) {
				  var thisView = this;
				  var GridColumnView = Backbone.View.extend({ tagName: "td" });
				  var SubCollection  = Backbone.Collection.extend({ model: this.submodel });
				  this.gridColumnView = new GridColumnView({});
				  this.sideColumnView = new GridColumnView({});
				  this.el.id = this.model.get("id");
				  // requireOptions(options, ["columns", "model"]);
				  this.columns = options.columns;
				  var subcolumns = this.subcolumns = options.model.get("subcolumns");
				  if (!(subcolumns instanceof Backgrid.Columns)) {
					  subcolumns = this.subcolumns = this.model.subcolumns = new Backgrid.Columns(subcolumns);
				  }

				  var customCollection = this[options.columnCollectionName] = options.model.get(options.columnCollectionName);
				  if(!(customCollection instanceof Backbone.Collection)) {
					  customCollection = this[options.columnCollectionName] = this.model.attributes[options.columnCollectionName] = new SubCollection(customCollection);
				  }
				  this.label = options.labelName;
				  this.subgrid = new Backgrid.Grid({
					  columns: this.subcolumns,
					  collection: this[options.columnCollectionName],
					  emptyText: "No records found"
				  });

				  this.listenTo(Backbone, "SubgridCustomCell:remove", this.render);
			  },
			  /**
			   * Renders a row containing a subgrid for this row's model.
			   * */
			  render: function () {
				  this.$el.empty();
				  this.$el.addClass("warning");// add class warning to subgrid row
				  this.gridColumnView.el.colSpan = (this.columns.length - 1);
				  // Appends the first  empty column
				  $(this.el).append(this.sideColumnView.render().$el);
				  // Appends the subgrid column that spans the rest of the table
				  $(this.el).append(this.gridColumnView.render().$el);
				  // Appends the Subgrid
				  this.gridColumnView.$el.append(this.subgrid.render().$el);
				  $(this.$el.children()[0]).html('<b>'+this.label+'</b>');
				  this.gridColumnView.$el.find('[class="backgrid"]').addClass("table-bordered");
				  return this;
			  }
		  });
		  /*
		  SubgridCustomCell is a cell class to expand and collaspe another grid within
		  the grid specific to each row.

		  @extends Backgrid.Cell
		   */

		  var SubgridCustomCell = Backgrid.SubgridCustomCell = Backgrid.Cell.extend({

			  className: "subgrid-custom-cell",
			  // define the icon within the cell
			  icon: function () {
				  var iconOptions = '<a href="javascript:void(0);" style="color:black;"><i class="fa-fw fa fa-plus"></i></a>';
				  if(this.state == "expanded")
					  iconOptions = '<a href="javascript:void(0);" style="color:black;"><i class="fa-fw fa fa-minus"></i></a>';
				  return (iconOptions);
			  },
			  optionValues: undefined,

			  /**
			   * Initializer.
			   * @param {Object} options
			   * @param {Backbone.Model} options.model
			   * @param {Backgrid.Column} options.column
			   * @param {Backgrid.Columns} options.column.attributes
			   * @throws {ReferenceError} If formatter is a string but a formatter class of
			   * said name cannot be found in the Backgrid module.
			   * */
			  initialize: function (options) {
				  this.state = "collasped";
				  //requireOptions(options, ["model", "column"]);
				  //requireOptions(options.column.attributes, ["optionValues"]);
				  this.model.set("subcolumns", options.column.get("optionValues"));
				  //requireTypeOrder(options.column, "subgrid", 0 );
				  this.column = options.column;
				  if (!(this.column instanceof Backgrid.Column)) {
					  this.column = new Backgrid.Column(this.column);
				  }
				  this.listenTo(Backbone, "backgrid:sort", this.clearSubgrid);
				  this.model.bind("remove", this.clearSubgrid, this);
			  },
			  /**
			   * Renders a collasped view.
			   */
			  render: function () {
				  //this.$el.empty().text(this.formatter.fromRaw(this.model.get(this.column.get("name"))));
				  $(this.el).append(this.icon());
				  return this;
			  },

			  events: {
				  "click": "stateConverter"
			  },
			  /**
			   * Checks the current state of the cell, either:
			   * appends another row for the subgrid and appends the grid to the row
			   * or removes the row from the parent grid,
			   * and saves the current data the model.
			   * */
			  stateConverter: function () {

				  var columnName = this.column.get('name');
				  var columnCollectionName = columnName+"Collection";
				  var labelName = this.column.attributes.label;
				  $(this.el).html("");
				  if (this.state == "collasped"){
//					  $(this.el).parent().addClass("warning");
//					 Add warning class to sub grid table
                                          $(this.el).addClass("warning");
					  this.state = "expanded";
					  this.subrow = new SubgridCustomRow({
						  columns: this.column.collection,
						  model: this.model,
						  columnName: columnName,
						  columnCollectionName: columnCollectionName,
						  labelName: labelName
					  });
					  $(this.el).parent("tr").after(this.subrow.render().$el);
				  } else {
					  if( $(this.el).parent().siblings('.warning').length <= 1 ){
//						  $(this.el).parent().removeClass("warning")
//						  Remove warning class from sub grid table
                                                  $(this.el).removeClass("warning")
					  }
					  this.state = "collasped";
					  this.subrow.remove();
				  }
				  //this.model.set("subgrid", this.subrow.subgrid);
				  this.model.set(columnCollectionName, this.subrow[columnCollectionName]);

				  $(this.el).append(this.icon());
			  },
			  /**
			   * Binds the remove function with the row when a model is removed.
			   */
			  clearSubgrid: function () {
				  var thisView = this;
				  // TO DO : Clean up code
				  $(".backgrid-subgrid-custom-row").filter(function() {
					  return ($(this).attr("id") == thisView.model.get('id'));
				  }).remove()
			  },
			  remove: function () {
				  if (this.subrow) {
					  this.subrow.remove.apply(this, arguments);
					  delete this.subrow;
				  }
				  return Backbone.View.prototype.remove.apply(this, arguments);
			  }
		  });
});
