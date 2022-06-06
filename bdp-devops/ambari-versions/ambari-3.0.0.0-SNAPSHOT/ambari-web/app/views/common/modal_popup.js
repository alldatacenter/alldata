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

App.ModalPopup = Ember.View.extend({

  viewName: 'modalPopup',
  modalDialogClasses: [],
  templateName: require('templates/common/modal_popup'),
  header: '&nbsp;',
  body: '&nbsp;',
  encodeBody: true,
  // define bodyClass which extends Ember.View to use an arbitrary Handlebars template as the body
  primary: Em.I18n.t('ok'),
  secondary: Em.I18n.t('common.cancel'),
  third: null,
  autoHeight: true,
  marginBottom: 300,
  disablePrimary: false,
  disableSecondary: false,
  disableThird: false,
  primaryClass: 'btn-success',
  secondaryClass: 'btn-default',
  thirdClass: 'btn-default',
  modalDialogClassesStr: function () {
    var modalDialogClasses = this.get('modalDialogClasses');
    if (!Em.isArray(modalDialogClasses)) {
      return '';
    }
    return modalDialogClasses.join(' ');
  }.property('modalDialogClasses.[]'),
  primaryId: '',
  secondaryId: '',
  thirdId: '',
  'data-qa': 'modal',
  onPrimary: function () {
    this.hide();
  },

  onSecondary: function () {
    this.hide();
  },

  onThird: function () {
    this.hide();
  },

  onClose: function () {
    this.hide();
  },

  hide: function () {
    if (!$.mocho) {
      this.$('#modal').modal('hide');
    }
    this.destroy();
  },

  showFooter: true,

  /**
   * Hide or show 'X' button for closing popup
   */
  showCloseButton: true,

  didInsertElement: function () {
    this.$().find('#modal')
      .on('enter-key-pressed', this.enterKeyPressed.bind(this))
      .on('escape-key-pressed', this.escapeKeyPressed.bind(this));
    this.fitZIndex();
    this.handleBackDrop();
    var firstInputElement = this.$('#modal').find(':input').not(':disabled, .no-autofocus').first();
    if (!$.mocho) {
      this.$('#modal').modal({
        keyboard: false,
        backdrop: false
      });
    }
    this.focusElement(firstInputElement);
    this.subscribeResize();
  },

  handleBackDrop: function () {
    if (this.get('backdrop') === false) {
      $('.modal-backdrop').css('visibility', 'hidden');
    } else {
      $('.modal-backdrop').css('visibility', 'visible');
    }
  },

  subscribeResize: function() {
    if (this.get('autoHeight') && !$.mocho) {
      this.fitHeight();
      $(window).on('resize', this.fitHeight.bind(this));
    }
  },

  willDestroyElement: function() {
    this.$().find('#modal').off('enter-key-pressed').off('escape-key-pressed');
    if (this.get('autoHeight') && !$.mocho) {
      $(window).off('resize', this.fitHeight);
    }
  },

  escapeKeyPressed: function (event) {
    var closeButton = this.$().find('.modal-header > .close').last();
    if (closeButton.length > 0) {
      event.preventDefault();
      event.stopPropagation();
      closeButton.click();
      return false;
    }
  },

  enterKeyPressed: function (event) {
    var primaryButton = this.$().find('.modal-footer > .btn-success').last();
    if (!$("*:focus").is('textarea') && primaryButton.length > 0 && primaryButton.attr('disabled') !== 'disabled') {
      event.preventDefault();
      event.stopPropagation();
      primaryButton.click();
      return false;
    }
  },

  /**
   * If popup is opened from another popup it should be displayed above
   * @method fitZIndex
   */
  fitZIndex: function () {
    var existedPopups = $('.modal-backdrop');
    if (existedPopups && !$.mocho) {
      var maxZindex = 1;
      existedPopups.each(function(index, popup) {
        if ($(popup).css('z-index') > maxZindex) {
          maxZindex = $(popup).css('z-index');
        }
      });
      this.$().find('.modal-backdrop').css('z-index', maxZindex * 2);
      this.$().find('.modal').css('z-index', maxZindex * 2 + 1);
    }
  },

  focusElement: function(elem) {
    elem.focus();
  },

  fitHeight: function () {
    if (this.get('state') === 'destroyed') return;
    const popup = this.$().find('#modal'),
      wrapper = $(popup).find('.modal-dialog'),
      block = $(popup).find('.modal-body'),
      wh = $(window).height(),
      ww = $(window).width(),
      topNavPaddingTop = 19, // from ambari-web/app/styles/common.less
      topNavFontSize = 20, // from ambari-web/app/styles/common.less
      topNavLineHeight = 1.3, // from ambari-web/app/styles/common.less
      modalMarginTopDefault = 10, // from ambari-web/app/styles/common.less
      modalMarginTopWide = 30, // from ambari-web/app/styles/common.less
      modalMarginTop = ww < 768 ? modalMarginTopDefault : modalMarginTopWide, // from ambari-web/vendor/styles/bootstrap.css
      top = topNavPaddingTop + topNavFontSize * topNavLineHeight - modalMarginTop;
    let newMaxHeight = wh - top * 2 - (wrapper.height() - block.height());

    popup.css({
      'top': top + 'px',
      'marginTop': 0
    });

    newMaxHeight = Math.max(newMaxHeight, 500);
    block.css('max-height', newMaxHeight);
  }
});

App.ModalPopup.reopenClass({

  show: function (options) {
    var popup = this.create(options);
    popup.appendTo('#wrapper');
    return popup;
  }

});
