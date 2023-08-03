define(['jquery', 'underscore', 'backbone', 'backbone-forms', 'backbone-forms.list'], function($, _, Backbone) {

	Backbone.Form.editors.List.Modal.ModalAdapter = Backbone.BootstrapModal;
	var Form = Backbone.Form;
	
	Form.editors.Datepicker = Form.editors.Base.extend({
		tagName: 'input',
		initialize: function(options) {
			//this.pluginAttr = _.extend( {'type': 'text'}, options.schema.pluginAttr || {}); //TODO FIXME 

			var allowFuture = true;
			Form.editors.Base.prototype.initialize.call(this, options);
			if(! _.isEmpty(options.schema.pluginAttr)){
				allowFuture = options.schema.pluginAttr.allowFuture;
			}
			this.$el.attr({'type': 'text'});
			if(allowFuture){
				this.$el.attr('data-date-enddate', new Date); //TODO 
			}
		},
		render: function() {

			this.$el.datepicker({
				autoclose : true
			});
			if(this.value)
				this.setValue(this.value);
			return this;
		},

		getValue: function() {
			var value = this.$el.val();
			if (value) {
				return this.$el.data('datepicker').date;    //bhavir
				//return value.date;
			} else {
				return '';
			}
			//return this.$el.data('datepicker').date;        //original
		},
		setValue: function(val) {
			if(!val)
				return;
			var d =  new Date(val);
			this.$el.val((d.getMonth()+1) +'/' +d.getDate()+'/'+d.getFullYear());
			this.$el.data('datepicker').date = d;
		}
	});

	Form.editors.Range = Form.editors.Text.extend({
		events: _.extend({}, Form.editors.Text.prototype.events, {
			'change': function(event) {
				this.trigger('change', this);
			}
		}),

		initialize: function(options) {
			Form.editors.Text.prototype.initialize.call(this, options);

			this.$el.attr('type', 'range');

			if (this.schema.appendToLabel) {
				this.updateLabel();
				this.on('change', this.updateLabel, this);
			}
		},
		getValue: function() {
			var val = Form.editors.Text.prototype.getValue.call(this);
			return parseInt(val, 10);
		},

		updateLabel: function() {
			_(_(function() {
				var $label = this.$el.parents('.bbf-field').find('label');
				$label.text(this.schema.title + ': ' + this.getValue() + (this.schema.valueSuffix || ''));
			}).bind(this)).defer();
		}
	});

	// like 'Select' editor, but will always return a boolean (true or false)
	Form.editors.BooleanSelect = Form.editors.Select.extend({
		initialize: function(options) {
			options.schema.options = [
				{ val: '1', label: 'Yes' },
				{ val: '', label: 'No' }
			];
			Form.editors.Select.prototype.initialize.call(this, options);
		},
		getValue: function() {
			return !!Form.editors.Select.prototype.getValue.call(this);
		},
		setValue: function(value) {
			value = value ? '1' : '';
			Form.editors.Select.prototype.setValue.call(this, value);
		}
	});

	// like the 'Select' editor, except will always return a number (int or float)
	Form.editors.NumberSelect = Form.editors.Select.extend({
		getValue: function() {
			return parseFloat(Form.editors.Select.prototype.getValue.call(this));
		},
		setValue: function(value) {
			Form.editors.Select.prototype.setValue.call(this, parseFloat(value));
		}
	});

	/**
	 * SELECT2
	 *
	 * Renders Select2 - jQuery based replacement for select boxes
	 *
	 * Requires an 'options.values' value on the schema.
	 *  Can be an array of options, a function that calls back with the array of options, a string of HTML
	 *  or a Backbone collection. If a collection, the models must implement a toString() method
	 */
	Form.editors.Select2Remote = Form.editors.Text.extend({		 
		initialize : function(options){
			this.onFocusOpen = !_.isUndefined(options.schema.onFocusOpen) ? options.schema.onFocusOpen : true;
			
			this.pluginAttr = _.extend( {'width' : 'resolve'}, options.schema.pluginAttr || {});
			Form.editors.Text.prototype.initialize.call(this,options);
		},

		render: function() {
			var self = this;
			
			self.setValue(self.value);

			//this.setOptions(this.schema.options);
			setTimeout(function () {
				if(!self.onFocusOpen)
					self.$el.select2(self.pluginAttr);
				else
					self.$el.select2(self.pluginAttr).on('select2-focus',  function(){$(this).select2('open');});
			},0);			

			return this;
		},
		getValue: function() {
		    return this.$el.val();
		  },

		  setValue: function(value) {
		    this.$el.val(value);
		  }


	});

	/**
	 * SELECT2
	 *
	 * Renders Select2 - jQuery based replacement for select boxes
	 *
	 * Requires an 'options.values' value on the schema.
	 *  Can be an array of options, a function that calls back with the array of options, a string of HTML
	 *  or a Backbone collection. If a collection, the models must implement a toString() method
	 */
	Form.editors.Select2 = Form.editors.Select.extend({		 
		initialize : function(options){
			this.pluginAttr = _.extend( {'width' : 'resolve'}, options.schema.pluginAttr || {});
			Form.editors.Select.prototype.initialize.call(this,options);
		},

		render: function() {
			var self = this;
			this.setOptions(this.schema.options);
			setTimeout(function () {
				self.$el.select2(self.pluginAttr);
			},0);			

			return this;
		}

	});


	/**
	 * Bootstrap-tag
	 */

	Form.editors.Tag = Form.editors.Text.extend({		 
		events: {
			'change': function() {
				this.trigger('change', this);
			},
			'focus': function() {
				this.trigger('focus', this);
			},
			'blur': function() {
				this.trigger('blur', this);
			}
		},

		initialize: function(options) {
			Form.editors.Text.prototype.initialize.call(this, options);

			this.pluginAttr = _.extend({}, options.schema.pluginAttr || {});
		},
		render: function() {
			var self = this;
			if(this.schema.options){
				// convert options to source for the plugin
				this.pluginAttr['source'] = this.getSource(this.schema.options);
			}

			this.setValue(this.value);

			setTimeout(function () {
				self.$el.tag(self.pluginAttr);
			},0);	

			return this;
		},

		/**
		 * Set the source param.
		 */
		getSource: function(options) {
			var self = this;
			var source;

			//If a collection was passed, check if it needs fetching
			if (options instanceof Backbone.Collection) {
				var collection = options;

				//Don't do the fetch if it's already populated
				if (collection.length > 0) {
					source= this._toSource(options);
				} else {
					collection.fetch({
						success: function(collection) {
							source = self._toSource(options);
						}
					});
				}
			}

			//If a function was passed, run it to get the options
			else if (_.isFunction(options)) {
				options(function(result) {
					source = self._toSource(result);
				}, self);
			}

			//Otherwise, ready to go straight to renderOptions
			else {
				source = this._toSource(options);
			}

			return source;
		},

		_toSource : function(options) {
			var source;
			//Accept string
			if (_.isString(options)) {
				source = [options];
			}

			//Or array
			else if (_.isArray(options)) {
				source = options;
			}

			//Or Backbone collection
			else if (options instanceof Backbone.Collection) {
				source = new Array();
				options.each(function(model) {
					source.push(model.toString());
				});
			}

			else if (_.isFunction(options)) {
				options(function(opts) {
					source = opts;
				}, this);
			}
			return source;
		},
		getValue: function() {
			if (this.schema.getValue) {
				return this.schema.getValue(this.$el.val(),this.model);
			} else {
				return this.$el.val();
			}
		},

		setValue: function(values) {
			if (!_.isArray(values)) values = [values];
			if (this.schema.setValue) {
				this.$el.val(this.schema.setValue(values));
			} else {
				this.$el.val(values);
			}
		},

		focus: function() {
			if (this.hasFocus) return;
			this.$el.focus();
		},

		blur: function() {
			if (!this.hasFocus) return;
			this.$el.blur();
		}

	});


	Form.editors.MultiSelect = Form.editors.Base.extend({
		initialize : function(options){
			this.options = options;
			//Form.editors.Base.prototype.initialize.call(this, options);

		},
		render: function() {
			// remove the id;
			//this.$el.attr('id',null);
			this.FormSelect = new Form.editors.Select(this.options);
			this.$el.append(this.FormSelect.render().el);
			this.$el.find('select2').attr('multiple','multiple');
			this.$el.find('select2').multiselect({
				maxHeight: 100,	
				includeSelectAllOption: true
			});//{enableFiltering : true}

			//this.$el.find('select option').prop('selected',null);
			/*_.each(this.$el.find('select option'),function(e){
			  $(e).prop('selected',false);
			  });*/
			//this.setValue(this.value);
			//this.createMultiSelect();

			return this;
		},
		createMultiSelect : function(){
			this.$el.find('select2').attr('multiple','multiple');
			this.$el.find('select2').multiselect({
				maxHeight: 100,	
				includeSelectAllOption: true
			});//{enableFiltering : true}

		},
		setValue : function(val){
			if(_.isArray(val)){
				_.each(this.$el.find('select option'),function(e){
					$(e).prop('selected',false);
					if($.inArray(e.value, val) > -1){
						//$(e).prop('selected','selected');
					}
				});
			}
		},
		getValue : function(){
			var arr = [];
			this.$el.find('select :selected').each(function(i,el) {
				if(el.value != "multiselect-all") 
					arr.push($(el).val());
			});
			return arr;
		}

	});

});
