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

App.MainViewsDetailsView = Em.View.extend({

  name: "mainViewsDetailsView",

  tagName: "iframe",
  classNames: ["views_sizes"],
  attributeBindings: ['src','seamless','allowfullscreen'],
  seamless: "seamless",
  allowfullscreen: "true",
  interval: null,

  /**
   * Drop autoHeight timer
   */
  willDestroyElement: function() {
    var interval = this.get('interval');
    if (interval) {
      clearInterval(interval);
    }
  },

  /**
   * Updates iframe height every 5s
   */
  didInsertElement : function() {
    var interval, self = this;
    interval = setInterval(function() {
      self.resizeFunction();
    }, 5000);
    self.set('interval', interval);
    this.resizeFunction();
    App.router.get('mainController').monitorInactivity();
  },

  resizeFunction : function() {
    var body = $(document.body);
    var footer = $("footer", body);
    var header = $("#top-nav", body);
    var iframe = $("iframe", body);

    var bodyHeight = body.outerHeight();
    var footerHeight = footer != null ? footer.outerHeight() : 0;
    var headerHeight = header != null ? header.outerHeight() : 0;

    var defaultHeight = bodyHeight - footerHeight - headerHeight;

    if (iframe != null && iframe.length > 0) {
      var childrenHeight = 0;
      var iframeElement = iframe[0];
      var pageScrollTop = $(window).scrollTop();
      // set iframe height to 'auto' to get actual scrollHeight
      iframeElement.style.height = 'auto';
      if (iframeElement.contentWindow != null
          && iframeElement.contentWindow.document != null
          && iframeElement.contentWindow.document.body != null) {
        var iFrameContentBody = iframeElement.contentWindow.document.body;
        childrenHeight = iFrameContentBody.scrollHeight;
      }
      var iFrameHeight = Math.max(childrenHeight, defaultHeight);
      iframe.css('height', iFrameHeight);
      $(window).scrollTop(pageScrollTop);
    }
  },

  src: function() {
    // can't use window.location.origin because IE doesn't support it
    return window.location.protocol + '//' + window.location.host + this.get('controller.content.href') + this.get('controller.content.viewPath');
  }.property('controller.content')

});
