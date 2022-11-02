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
'use strict';

angular.module('ambariAdminConsole')
.factory('Alert', [function() {
  
  var hideTimeout = null;
  var $boxContainer = null;
  var removingTimeout = null;

  function createAlertBox(innerHTML, moreInfo, type){
    if (!$boxContainer) {
      $boxContainer = angular.element('<div class="alert-container"/>').appendTo('body');
      $boxContainer
        .on('mouseenter', function() {
          clearTimeout(removingTimeout);
        })
        .on('mouseleave', function() {
          startRemovingTimeout();
        });
    }
    var elem = angular.element('<div><div class="icon-box"></div></div>').addClass('ambariAlert').addClass(type).addClass('invisible');

    elem.append('<div class="content">' + innerHTML + '</div>');
    if (moreInfo) {
      $(' <a href class="more-collapse"> more...</a>').appendTo(elem.find('.content'))
      .on('click', function() {
        elem.find('.more').show();
        $(this).remove();
        return false;
      });
      elem.append('<div class="more">'+moreInfo.replace(/\./g, '.<wbr />')+'</div>');
    }

    $('<button type="button" class="close"><span aria-hidden="true">&times;</span><span class="sr-only">{{"common.controls.close" | translate}}</span></button>')
      .appendTo(elem)
      .on('click', function() {
        var $box = $(this).closest('.ambariAlert');
        $box.remove();
      });

    var $icon = $('<span class="glyphicon"></span>');
    switch (type){
      case 'error':
        $icon.addClass('glyphicon-remove-sign');
        break;
      case 'success':
        $icon.addClass('glyphicon-ok-sign');
        break;
      case 'info':
        $icon.addClass('glyphicon-info-sign');
        break;
    }
    elem.find('.icon-box').append($icon);

    elem.appendTo($boxContainer);
    setTimeout(function() {
      elem.removeClass('invisible');
    }, 0);

    startRemovingTimeout();
  };

  function startRemovingTimeout(){
    clearTimeout(removingTimeout);
    removingTimeout = setTimeout(removeTopBox, 5000);
  }

  function removeTopBox(){
    $boxContainer.children().first().remove();
    if (!$boxContainer.children().length) {
      $boxContainer.remove();
      $boxContainer = null;
    } else {
      startRemovingTimeout();
    }
  }

  return {
    error: function(innerHTML, moreInfo) {
      createAlertBox(innerHTML, moreInfo, 'error');
    },
    success: function(innerHTML, moreInfo) {
      createAlertBox(innerHTML, moreInfo, 'success');
    },
    info: function(innerHTML, moreInfo) {
      createAlertBox(innerHTML, moreInfo, 'info');
    }
  };
}]);
