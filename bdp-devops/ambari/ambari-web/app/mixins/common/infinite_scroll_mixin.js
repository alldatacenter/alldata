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

 /**
 * @typedef {Object} InfiniteScrollMixinOptions
 * @property {String} appendHtml html to append when scroll ends and callback executed. It's common
 *   that root node has unique <code>id</code> or <code>class</code> attributes.
 * @property {Function} callback function to execute when scroll ends. This function should return
 *   <code>$.Deferred().promise()</code> instance
 * @property {Function} onReject function to execute when <code>callback</code> rejected.
 */

/**
 * @mixin App.InfiniteScrollMixin
 * This mixin provides methods to attach infinite scroll to specific scrollable element.
 *
 * Usage:
 * <code>
 *  // mix it
 *  var myView = Em.View.extend(App.InfiniteScrollMixin, {
 *    didInsertElement: function() {
 *    	// call method infiniteScrollInit
 *    	this.infiniteScrollInit($('.some-scrollable'), {
 *    		callback: this.someCallbacOnEndReached
 *    	});
 *    }
 *  });
 * </code>
 *
 */
App.InfiniteScrollMixin = Ember.Mixin.create({

  /**
   * Stores callback execution progress.
   *
   * @type {Boolean}
   */
  _infiniteScrollCallbackInProgress: false,

  /**
   * Stores HTMLElement infinite scroll initiated on.
   *
   * @type {HTMLElement}
   */
  _infiniteScrollEl: null,

  /**
   * Default options for infinite scroll.
   *
   * @type {InfiniteScrollMixinOptions}
   */
  _infiniteScrollDefaults: {
    appendHtml: '<div id="infinite-scroll-append"><i class="icon-spinner icon-spin"></i></div>',
    callback: function() { return $.Deferred().resolve().promise(); },
    onReject: function() {},
    onResolve: function() {}
  },

  /**
   * Determines that there is no data to load on next callback call.
   *
   */
  _infiniteScrollMoreData: true,

  /**
   * Initialize infinite scroll on specified HTMLElement.
   *
   * @param  {HTMLElement} el DOM element to attach infinite scroll.
   * @param  {InfiniteScrollMixinOptions} opts
   */
  infiniteScrollInit: function(el, opts) {
    var options = $.extend({}, this.get('_infiniteScrollDefaults'), opts || {});
    this.set('_infiniteScrollEl', el);
    this.get('_infiniteScrollEl').on('scroll', this._infiniteScrollHandler.bind(this));
    this.get('_infiniteScrollEl').on('infinite-scroll-end', this._infiniteScrollEndHandler(options).bind(this));
  },

  /**
   * Handler executed on scrolling.
   * @param  {jQuery.Event} e
   */
  _infiniteScrollHandler: function(e) {
    var el = $(e.target);
    var height = el.get(0).clientHeight;
    var scrollHeight = el.prop('scrollHeight');
    var endPoint = scrollHeight - height;
    if (endPoint === el.scrollTop() && !this.get('_infiniteScrollCallbackInProgress')) {
      el.trigger('infinite-scroll-end');
    }
  },

  /**
   * Handler called when scroll ends.
   *
   * @param  {InfiniteScrollMixinOptions} options
   * @return {Function}
   */
  _infiniteScrollEndHandler: function(options) {
    return function(e) {
      var self = this;
      if (this.get('_infiniteScrollCallbackInProgress') || !this.get('_infiniteScrollMoreData')) return;
      this._infiniteScrollAppendHtml(options.appendHtml);
      // always scroll to bottom
      this.get('_infiniteScrollEl').scrollTop(this.get('_infiniteScrollEl').get(0).scrollHeight);
      this.set('_infiniteScrollCallbackInProgress', true);
      options.callback().then(function() {
        options.onResolve();
      }, function() {
        options.onReject();
      }).always(function() {
        self.set('_infiniteScrollCallbackInProgress', false);
        self._infiniteScrollRemoveHtml(options.appendHtml);
      });
    }.bind(this);
  },

  /**
   * Helper function to append String as html node to.
   * @param  {String} htmlString string to append
   */
  _infiniteScrollAppendHtml: function(htmlString) {
    this.get('_infiniteScrollEl').append(htmlString);
  },

  /**
   * Remove HTMLElement by specified string that can be converted to html. HTMLElement root node
   * should have unique <code>id</code> or <code>class</code> attribute to avoid removing additional
   * elements.
   *
   * @param  {String} htmlString string to remove
   */
  _infiniteScrollRemoveHtml: function(htmlString) {
    this.get('_infiniteScrollEl').find(this._infiniteScrollGetSelector(htmlString)).remove();
  },

  /**
   * Get root node selector.
   * <code>id</code> attribute has higher priority and will return if found.
   * <code>class</code> if no <code>id</code> attribute found <code>class</code> attribute
   * will be used.
   *
   * @param  {String} htmlString string processed as HTML
   * @return {[type]}            [description]
   */
  _infiniteScrollGetSelector: function(htmlString) {
    var html = $(htmlString);
    var elId = html.attr('id');
    var elClass = (html.attr('class') || '').split(' ').join('.');
    html = null;
    return !!elId ? '#' + elId : '.' + elClass;
  },

  /**
   * Remove infinite scroll.
   * Unbind all listeners.
   */
  infiniteScrollDestroy: function() {
    this.get('_infiniteScrollEl').off('scroll', this._infiniteScrollHandler);
    this.get('_infiniteScrollEl').off('infinite-scroll-end', this._infiniteScrollHandler);
    this.set('_infiniteScrollEl', null);
  },

  /**
   * Set if there is more data to load on next scroll end event.
   * @param {boolean} isAvailable <code>true</code> when there are more data to fetch
   */
  infiniteScrollSetDataAvailable: function(isAvailable) {
    this.set('_infiniteScrollMoreData', isAvailable);
  }
});
