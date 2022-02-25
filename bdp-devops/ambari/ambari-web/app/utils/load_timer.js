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



App.loadTimer = Em.Object.create({

  timeStampCache: {},

  /**
   * save start timestamp
   * @param {string} label
   */
  start: function(label) {
    $('.alert.attach-to-bottom-right').remove();
    if (App.get('supports.showPageLoadTime')) {
      this.get('timeStampCache')[label] = window.performance.now();
    }
  },

  /**
   * calculate time difference
   * @param {string} label
   * @returns {undefined|string}
   */
  finish: function(label) {
    var result;

    if (typeof(this.get('timeStampCache')[label]) === "number") {
      result = Number(window.performance.now() - this.get('timeStampCache')[label]).toFixed(2);
      console.debug(label + " loaded in: " + result + "ms");
      this.display(label + " loaded in: " + result + "ms");
      delete this.get('timeStampCache')[label];
    }
    return result;
  },

  /**
   * display time results on the screen
   * @param {string} result
   */
  display: function(result) {
    var alert = $("<div class='alert alert-warning attach-to-bottom-right'>" +  result + "</div>");
    var closeButton = $("<i class='glyphicon glyphicon-remove-circle'></i>").click(function () {
      $(this).remove();
      $(alert).remove();
    });
    alert.append("&nbsp;", closeButton);
    $('body').append(alert);
  }
});

