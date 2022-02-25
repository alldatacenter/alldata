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

App.showLogsPopup = function (header, content) {
  return App.ModalPopup.show({
    header: header,
    primary: Em.I18n.t('ok'),
    secondary: null,
    bodyClass: Em.View.extend({
      templateName: require('templates/common/modal_popups/logs_popup'),

      /**
       * @type {string}
       */
      content: content,

      /**
       * Onclick handler for selected Task
       *
       * @method openTaskLogInDialog
       */
      openTaskLogInDialog: function () {
        if ($(".task-detail-log-clipboard").length) {
          this.destroyClipBoard();
        }
        var newWindow = window.open();
        var newDocument = newWindow.document;
        newDocument.write($(".task-detail-log-info").html());
        newDocument.close();
      },

      /**
       * Onclick event for copy to clipboard button
       *
       * @method textTrigger
       */
      textTrigger: function () {
        $(".task-detail-log-clipboard").length ? this.destroyClipBoard() : this.createClipBoard();
      },

      /**
       * Create Clip Board
       *
       * @method createClipBoard
       */
      createClipBoard: function () {
        var logElement = $(".task-detail-log-maintext"),
            logElementRect = logElement[0].getBoundingClientRect(),
            textarea = $('<textarea class="task-detail-log-clipboard"></textarea>');
        $(".task-detail-log-clipboard-wrap").append(textarea);
        textarea
        .text(logElement.text())
        .css('display', 'block')
        .width(logElementRect.width)
        .height(logElementRect.height)
        .select();

        logElement.css("display", "none");
      },

      /**
       * Destroy Clip Board
       *
       * @method destroyClipBoard
       */
      destroyClipBoard: function () {
        $(".task-detail-log-clipboard").remove();
        $(".task-detail-log-maintext").css("display", "block");
      }
    })
  });
};