/* ===========================================================
 * bootstrap-checkbox - v.1.0.1
 * ===========================================================
 * Copyright 2014 Roberto Montresor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================== */

!function($) {
  var Checkbox = function(element, options, e) {
    if (e) {
      e.stopPropagation();
      e.preventDefault();
    }
    this.$element = $(element);
    this.$newElement = null;
    this.button = null;
    this.label = null;
    this.labelPrepend = null;
    this.options = $.extend({}, $.fn.checkbox.defaults, this.$element.data(), typeof options == 'object' && options);
    this.init();
  };

  Checkbox.prototype = {

    constructor: Checkbox,

    init: function (e) {
      this.$element.hide();
      this.$element.attr('autocomplete', 'off');

      this._createButtons();
    },

    _createButtons: function(){
      var classList = this.$element.attr('class') !== undefined ? this.$element.attr('class').split(/\s+/) : '';
      var template = this.getTemplate();
      this.$element.after(template);
      this.$newElement = this.$element.next('.bootstrap-checkbox');
      this.button = this.$newElement.find('button');
      this.label = this.$newElement.find('span.label-checkbox');
      this.labelPrepend = this.$newElement.find('span.label-prepend-checkbox');
      for (var i = 0; i < classList.length; i++) {
        if(classList[i] != 'checkbox') {
          this.$newElement.addClass(classList[i]);
        }
      }
      this.button.addClass(this.options.buttonStyle);

      if (this.$element.data('default-state') != undefined){
        this.options.defaultState = this.$element.data('default-state');
      }
      if (this.$element.data('default-enabled') != undefined){
        this.options.defaultEnabled = this.$element.data('default-enabled');
      }
      if (this.$element.data('display-as-button') != undefined){
        this.options.displayAsButton = this.$element.data('display-as-button');
      }

      if (this.options.indeterminate)
        this.$element.prop('indeterminate', true);

      this.checkEnabled();
      this.checkChecked();
      this.checkTabIndex();
      this.clickListener();
    },

    getTemplate: function() {
      var additionalButtonStyle = this.options.displayAsButton ? ' displayAsButton' : '',
        label = this.$element.data('label') ? '<span class="label-checkbox">'+this.$element.data('label')+'</span>' : '',
        labelPrepend = this.$element.data('label-prepend') ? '<span class="label-prepend-checkbox">'+this.$element.data('label-prepend')+'</span>' : '';

      var template =
        '<span class="button-checkbox bootstrap-checkbox">' +
          '<button type="button" class="btn clearfix'+additionalButtonStyle+'">' +
          ((this.$element.data('label-prepend') && this.options.displayAsButton) ? labelPrepend : '')+
          '<span class="icon '+this.options.checkedClass+'" style="display:none;"></span>' +
          '<span class="icon '+this.options.uncheckedClass+'"></span>' +
          '<span class="icon '+this.options.indeterminateClass+'" style="display:none;"></span>' +
          ((this.$element.data('label') && this.options.displayAsButton) ? label : '')+
          '</button>' +
          '</span>';

      if (!this.options.displayAsButton && (this.$element.data('label') || this.$element.data('label-prepend'))) {
        template =
          '<label class="'+this.options.labelClass+'">' +
            labelPrepend + template + label+
            '</label>';
      }
      return template;
    },

    checkEnabled: function() {
      this.button.attr('disabled', this.$element.is(':disabled'));
      this.$newElement.toggleClass('disabled', this.$element.is(':disabled'));
    },

    checkTabIndex: function() {
      if (this.$element.is('[tabindex]')) {
        var tabindex = this.$element.attr("tabindex");
        this.button.attr('tabindex', tabindex);
      }
    },

    checkChecked: function() {
      var whitePattern = /\s/g, replaceChar = '.';
      if (this.$element.prop('indeterminate') == true){
        this.button.find('span.'+this.options.checkedClass.replace(whitePattern, replaceChar)).hide();
        this.button.find('span.'+this.options.uncheckedClass.replace(whitePattern, replaceChar)).hide();
        this.button.find('span.'+this.options.indeterminateClass.replace(whitePattern, replaceChar)).show();
      } else {
        if (this.$element.is(':checked')) {
          this.button.find('span.'+this.options.checkedClass.replace(whitePattern, replaceChar)).show();
          this.button.find('span.'+this.options.uncheckedClass.replace(whitePattern, replaceChar)).hide();
        } else {
          this.button.find('span.'+this.options.checkedClass.replace(whitePattern, replaceChar)).hide();
          this.button.find('span.'+this.options.uncheckedClass.replace(whitePattern, replaceChar)).show();
        }
        this.button.find('span.'+this.options.indeterminateClass.replace(whitePattern, replaceChar)).hide();
      }

      if (this.$element.is(':checked')) {
        if (this.options.buttonStyleChecked){
          this.button.removeClass(this.options.buttonStyle);
          this.button.addClass(this.options.buttonStyleChecked);
        }
      } else {
        if (this.options.buttonStyleChecked){
          this.button.removeClass(this.options.buttonStyleChecked);
          this.button.addClass(this.options.buttonStyle);
        }
      }

      if (this.$element.is(':checked')) {
        if (this.options.labelClassChecked){
          $(this.$element).next("label").addClass(this.options.labelClassChecked);
        }
      } else {
        if (this.options.labelClassChecked){
          $(this.$element).next("label").removeClass(this.options.labelClassChecked);
        }
      }
    },

    clickListener: function() {
      var _this = this;
      this.button.on('click', function(e){
        e.preventDefault();
        _this.$element.prop("indeterminate", false);
        _this.$element[0].click();
        _this.checkChecked();
      });
      this.$element.on('change', function(e) {
        _this.checkChecked();
      });
      this.$element.parents('form').on('reset', function(e) {
        if (_this.options.defaultState == null){
          _this.$element.prop('indeterminate', true);
        } else {
          _this.$element.prop('checked', _this.options.defaultState);
        }
        _this.$element.prop('disabled', !_this.options.defaultEnabled);
        _this.checkEnabled();
        _this.checkChecked();
        e.preventDefault();
      });
    },

    setOptions: function(option, event){
      if (option.checked != undefined) {
        this.setChecked(option.checked);
      }
      if (option.enabled != undefined) {
        this.setEnabled(option.enabled);
      }
      if (option.indeterminate != undefined) {
        this.setIndeterminate(option.indeterminate);
      }
    },

    setChecked: function(checked){
      this.$element.prop("checked", checked);
      this.$element.prop("indeterminate", false);
      this.checkChecked();
    },

    setIndeterminate: function(indeterminate){
      this.$element.prop("indeterminate", indeterminate);
      this.checkChecked();
    },


    click: function(event){
      this.$element.prop("indeterminate", false);
      this.$element[0].click();
      this.checkChecked();
    },

    change: function(event){
      this.$element.change();
    },

    setEnabled: function(enabled){
      this.$element.attr('disabled', !enabled);
      this.checkEnabled();
    },

    toggleEnabled: function(event){
      this.$element.attr('disabled', !this.$element.is(':disabled'));
      this.checkEnabled();
    },

    refresh: function(event){
      this.checkEnabled();
      this.checkChecked();
    },

    update: function(options){
      if (!this.$element.next().find('.bootstrap-checkbox'))
        return;

      this.options = $.extend({}, this.options, options);
      this.$element.next().remove();
      this._createButtons();
    }
  };

  $.fn.checkbox = function(option, event) {
    return this.each(function () {
      var $this = $(this),
        data = $this.data('checkbox'),
        options = typeof option == 'object' && option;
      if (!data) {
        $this.data('checkbox', (data = new Checkbox(this, options, event)));
        if (data.options.constructorCallback != undefined){
          data.options.constructorCallback(data.$element, data.button, data.label, data.labelPrepend);
        }
      } else {
        if (typeof option == 'string') {
          data[option](event);
        } else if (typeof option != 'undefined') {
          data.setOptions(option, event);
        }
      }
    });
  };

  $.fn.checkbox.defaults = {
    displayAsButton: false,
    indeterminate: false,
    buttonStyle: 'btn-link',
    buttonStyleChecked: null,
    checkedClass: 'cb-icon-check',
    uncheckedClass: 'cb-icon-check-empty',
    indeterminateClass: 'cb-icon-check-indeterminate',
    defaultState: false,
    defaultEnabled: true,
    constructorCallback: null,
    labelClass: "checkbox bootstrap-checkbox",
    labelClassChecked: "active"
  };

}(window.jQuery);