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

/**
 * This file contains patched methods for twitter bootstrap.js
 */

/**
 * Fixes error when <code>destroy</code> method called more than one time.
 * For more info check https://github.com/twbs/bootstrap/issues/20511
 */
$.fn.tooltip.Constructor.prototype.destroy = function() {
  var that = this
  clearTimeout(this.timeout)
  this.hide(function () {
    if (that.$element !== null) {
      that.$element.off('.' + that.type).removeData('bs.' + that.type)
    }
    if (that.$tip) {
      that.$tip.detach()
    }
    that.$tip = null
    that.$arrow = null
    that.$viewport = null
    that.$element = null
  })
};

// function required for clearMenus
var getParent = function($this) {
  var selector = $this.attr('data-target');

  if (!selector) {
    selector = $this.attr('href');
    selector = selector && /#[A-Za-z]/.test(selector) && selector.replace(/.*(?=#[^\s]*$)/, '') // strip for ie7;
  }

  var $parent = selector && $(selector);

  return $parent && $parent.length ? $parent : $this.parent();
};

// new exclusion added, clicking on elements with class checkbox-label should be ignored to close dropdown
var clearMenus = function (e) {
  if (e && e.which === 3) return;
  $('.dropdown-backdrop').remove();
  $('[data-toggle="dropdown"]').each(function () {
    var $this = $(this);
    var $parent = getParent($this);
    var relatedTarget = {relatedTarget: this};

    if (!$parent.hasClass('open')) return;

    if (e && e.type == 'click' && (/input|textarea/i.test(e.target.tagName) || e.target.className.contains('checkbox-label')) && $.contains($parent[0], e.target)) return;

    $parent.trigger(e = $.Event('hide.bs.dropdown', relatedTarget));

    if (e.isDefaultPrevented()) return;

    $this.attr('aria-expanded', 'false');
    $parent.removeClass('open').trigger($.Event('hidden.bs.dropdown', relatedTarget));
  });
};

$(document).off('click.bs.dropdown.data-api');
$(document).on('click.bs.dropdown.data-api', clearMenus);
$(document).on('click.bs.dropdown.data-api', '.dropdown form', function (e) {
  e.stopPropagation()
});
$(document).on('click.bs.dropdown.data-api', '[data-toggle="dropdown"]', $.fn.dropdown.Constructor.prototype.toggle);

