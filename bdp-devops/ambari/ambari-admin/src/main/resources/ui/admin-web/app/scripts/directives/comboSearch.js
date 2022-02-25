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


/**
 *  Example:
 *  <combo-search suggestions="filters"
 *                filter-change="filterItems"
 *                placeholder="Search"
 *                supportCategories="true">
 *  </combo-search>
 *
 *  filters = [
 *    {
 *      key: 'property1',
 *      label: $t('propertyLabel'),
 *      category: 'category1'
 *      options: []
 *    }
 *  ]
 *  Note: "category" field is optional, should be used only when supportCategories="true"
 *
 */

angular.module('ambariAdminConsole')
.directive('comboSearch', function() {
  return {
    restrict: 'E',
    templateUrl: 'views/directives/comboSearch.html',
    scope: {
      suggestions: '=',
      filterChange: '=',
      placeholder: '@',
      supportCategories: '@'
    },
    controller: ['$scope', function($scope) {
      return {
        suggestions: $scope.suggestions,
        placeholder: $scope.placeholder,
        filterChange: $scope.filterChange,
        supportCategories: $scope.supportCategories === "true"
      }
    }],
    link: function($scope, $elem, $attr, $ctrl) {
      var idCounter = 1;
      var suggestions = $ctrl.suggestions;
      var supportCategories = $ctrl.supportCategories;
      var mainInputElement = $elem.find('.main-input.combo-search-input');
      $scope.placeholder = $ctrl.placeholder;
      $scope.searchFilterInput = '';
      $scope.filterSuggestions = [];
      $scope.showAutoComplete = false;
      $scope.appliedFilters = [];

      attachInputWidthSetter(mainInputElement);
      initKeyHandlers();
      initBlurHandler();

      $scope.$watch(function () {
        return $scope.appliedFilters.length;
      }, function () {
        attachInputWidthSetter($elem.find('.combo-search-input'));
      });

      $scope.removeFilter = function(filter) {
        $scope.appliedFilters = $scope.appliedFilters.filter(function(item) {
          return filter.id !== item.id;
        });
        $scope.observeSearchFilterInput();
        mainInputElement.focus();
        $scope.updateFilters($scope.appliedFilters);
      };

      $scope.clearFilters = function() {
        $scope.appliedFilters = [];
        $scope.updateFilters($scope.appliedFilters);
      };

      $scope.selectFilter = function(filter, event) {
        var newAppliedFilter = {
          id: 'filter_' + idCounter++,
          currentOption: null,
          filteredOptions: [],
          searchOptionInput: '',
          key: filter.key,
          label: filter.label,
          options: filter.options || [],
          showAutoComplete: false
        };
        $scope.appliedFilters.push(newAppliedFilter);
        if (event) {
          event.stopPropagation();
          event.preventDefault();
        }
        $scope.isEditing = false;
        $scope.showAutoComplete = false;
        $scope.searchFilterInput = '';
        _.debounce(function() {
          $('input[name=' + newAppliedFilter.id + ']').focus().width(4);
        }, 100)();
      };

      $scope.selectOption = function(event, option, filter) {
        $('input[name=' + filter.id + ']').val(option.label).trigger('input');
        filter.showAutoComplete = false;
        mainInputElement.focus();
        $scope.observeSearchFilterInput(event);
        filter.currentOption = option;
        $scope.updateFilters($scope.appliedFilters);
      };

      $scope.hideAutocomplete = function(filter) {
        _.debounce(function() {
          if (filter) {
            filter.showAutoComplete = false;
          } else {
            if (!$scope.isEditing) {
              $scope.showAutoComplete = false;
            }
          }
          $scope.$apply();
        }, 100)();
      };

      $scope.forceFocus = function(event, filter) {
        $(event.currentTarget).find('.combo-search-input').focus();
        $scope.showAutoComplete = false;
        $scope.observeSearchOptionInput(filter);
        event.stopPropagation();
        event.preventDefault();
      };

      $scope.makeActive = function(active, all) {
        if (active.isCategory) {
          return false;
        }
        all.forEach(function(item) {
          item.active = active.key === item.key;
        });
      };

      $scope.observeSearchFilterInput = function(event) {
        if (event) {
          mainInputElement.focus();
          $scope.isEditing = true;
          event.stopPropagation();
          event.preventDefault();
        }

        var filteredSuggestions = suggestions.filter(function(item) {
          return (!$scope.searchFilterInput || item.label.toLowerCase().indexOf($scope.searchFilterInput.toLowerCase()) !== -1);
        });
        if (filteredSuggestions.length > 0) {
          $scope.makeActive(filteredSuggestions[0], filteredSuggestions);
          $scope.showAutoComplete = true;
        } else {
          $scope.showAutoComplete = false;
        }
        $scope.filterSuggestions = supportCategories ? formatCategorySuggestions(filteredSuggestions) : filteredSuggestions;
      };

      $scope.observeSearchOptionInput = function(filter) {
        var appliedOptions = {};
        $scope.appliedFilters.forEach(function(item) {
          if (item.key === filter.key && item.currentOption) {
            appliedOptions[item.currentOption.key] = true;
          }
        });

        if (filter.currentOption && filter.currentOption.key !== filter.searchOptionInput) {
          filter.currentOption = null;
        }
        filter.filteredOptions = filter.options.filter(function(option) {
          return !(option.key === '' || option.key === undefined || appliedOptions[option.key])
            && (!filter.searchOptionInput || option.label.toLowerCase().indexOf(filter.searchOptionInput.toLowerCase()) !== -1);
        });
        resetActive(filter.filteredOptions);
        filter.showAutoComplete = filter.filteredOptions.length > 0;
      };

      $scope.extractFilters = function(filters) {
        var map = {};
        var result = [];

        filters.forEach(function(filter) {
          if (filter.currentOption) {
            if (!map[filter.key]) {
              map[filter.key] = [];
            }
            map[filter.key].push(filter.currentOption.key);
          }
        });
        for(var key in map) {
          result.push({
            key: key,
            values: map[key]
          });
        }
        return result;
      };

      $scope.updateFilters = function(appliedFilters) {
        $ctrl.filterChange($scope.extractFilters(appliedFilters));
      };

      function formatCategorySuggestions(suggestions) {
        var categories = {};
        var result = [];
        suggestions.forEach(function(item) {
          if (!item.category) {
            item.category = 'default';
          }
          if (!categories[item.category]) {
            categories[item.category] = [];
          }
          categories[item.category].push(item);
        });

        for(var cat in categories) {
          result.push({
            key: cat,
            label: cat,
            isCategory: true,
            isDefault: cat === 'default'
          });
          result = result.concat(categories[cat]);
        }
        return result;
      }

      function initBlurHandler() {
        $(document).click(function() {
          $scope.isEditing = false;
          $scope.hideAutocomplete();
        });
      }

      function findActiveByName(array, name) {
        for (var i = 0; i < array.length; i++) {
          if (array[i].id === name) {
            return i;
          }
        }
        return null;
      }

      function findActiveByProperty(array) {
        for (var i = 0; i < array.length; i++) {
          if (array[i].active) {
            return i;
          }
        }
        return -1;
      }

      function resetActive(array) {
        array.forEach(function(item) {
          item.active = false;
        });
      }

      function focusInput(filter) {
        $('input[name=' + filter.id + ']').focus();
        $scope.showAutoComplete = false;
        $scope.observeSearchOptionInput(filter);
      }

      function initKeyHandlers() {
        $($elem).keydown(function(event) {
          if (event.which === 13) { // "Enter" key
            enterKeyHandler();
            $scope.$apply();
          }
          if (event.which === 8) { // "Backspace" key
            backspaceKeyHandler(event);
            $scope.$apply();
          }
          if (event.which === 38) { // "Up" key
            upKeyHandler();
            $scope.$apply();
          }
          if (event.which === 40) { // "Down" key
            downKeyHandler();
            $scope.$apply();
          }
          if (event.which === 39) { // "Right Arrow" key
            rightArrowKeyHandler();
            $scope.$apply();
          }
          if (event.which === 37) { // "Left Arrow" key
            leftArrowKeyHandler();
            $scope.$apply();
          }
          if (event.which === 27) { // "Escape" key
            $scope.showAutoComplete = false;
            $scope.$apply();
          }
        });
      }

      function leftArrowKeyHandler() {
        var activeElement = $(document.activeElement);
        if (activeElement.is('input') && activeElement[0].selectionStart === 0 && $scope.appliedFilters.length > 0) {
          if (activeElement.hasClass('main-input')) {
            focusInput($scope.appliedFilters[$scope.appliedFilters.length - 1]);
          } else {
            var activeIndex = findActiveByName($scope.appliedFilters, activeElement.attr('name'));
            if (activeIndex !== null && activeIndex > 0) {
              focusInput($scope.appliedFilters[activeIndex - 1]);
            }
          }
        }
      }

      function rightArrowKeyHandler() {
        var activeElement = $(document.activeElement);
        if (activeElement.is('input') && activeElement[0].selectionStart === activeElement.val().length) {
          if (!activeElement.hasClass('main-input')) {
            var activeIndex = findActiveByName($scope.appliedFilters, activeElement.attr('name'));
            if (activeIndex !== null) {
              if (activeIndex === $scope.appliedFilters.length - 1) {
                mainInputElement.focus();
                $scope.observeSearchFilterInput();
              } else {
                focusInput($scope.appliedFilters[activeIndex + 1]);
              }
            }
          }
        }
      }

      function downKeyHandler() {
        var activeIndex = 0;
        var nextIndex = null;

        if ($scope.showAutoComplete) {
          activeIndex = findActiveByProperty($scope.filterSuggestions);
          if (activeIndex < $scope.filterSuggestions.length - 1) {
            if ($scope.filterSuggestions[activeIndex + 1].isCategory && activeIndex + 2 < $scope.filterSuggestions.length) {
              nextIndex = activeIndex + 2;
            } else {
              nextIndex = activeIndex + 1;
            }
          } else {
            nextIndex = ($scope.filterSuggestions[0].isCategory) ? 1 : 0;
          }
          if (nextIndex !== null) {
            $scope.makeActive($scope.filterSuggestions[nextIndex], $scope.filterSuggestions);
          }
        } else {
          var activeAppliedFilters = $scope.appliedFilters.filter(function(item) {
            return item.showAutoComplete;
          });
          if (activeAppliedFilters.length > 0) {
            var filteredOptions = activeAppliedFilters[0].filteredOptions;
            activeIndex = findActiveByProperty(filteredOptions);
            if (activeIndex < filteredOptions.length - 1) {
              nextIndex = activeIndex + 1;
            } else {
              //switch to input of option
              nextIndex = null;
              resetActive(filteredOptions);
              focusInput(activeAppliedFilters[0]);
            }
          }
          if (nextIndex !== null) {
            $scope.makeActive(filteredOptions[nextIndex], filteredOptions);
          }
        }
      }

      function upKeyHandler() {
        var activeIndex = 0;
        var nextIndex = null;

        if ($scope.showAutoComplete) {
          activeIndex = findActiveByProperty($scope.filterSuggestions);
          if (activeIndex > 0) {
            if ($scope.filterSuggestions[activeIndex - 1].isCategory) {
              nextIndex = (activeIndex - 2 > 0) ? activeIndex - 2 : $scope.filterSuggestions.length - 1;
            } else {
              nextIndex = activeIndex - 1;
            }
          } else {
            nextIndex = $scope.filterSuggestions.length - 1;
          }
          if (nextIndex !== null) {
            $scope.makeActive($scope.filterSuggestions[nextIndex], $scope.filterSuggestions);
          }
        } else {
          var activeAppliedFilters = $scope.appliedFilters.filter(function(item) {
            return item.showAutoComplete;
          });
          if (activeAppliedFilters.length > 0) {
            var filteredOptions = activeAppliedFilters[0].filteredOptions;
            activeIndex = findActiveByProperty(filteredOptions);
            if (activeIndex > 0) {
              nextIndex = activeIndex - 1;
            } else if (activeIndex === 0) {
              //switch to input of option
              nextIndex = null;
              resetActive(filteredOptions);
              focusInput(activeAppliedFilters[0]);
            } else {
              nextIndex = filteredOptions.length - 1;
            }
          }
          if (nextIndex !== null) {
            $scope.makeActive(filteredOptions[nextIndex], filteredOptions);
          }
        }
      }

      function enterKeyHandler() {
        if ($scope.showAutoComplete) {
          var activeFilters = $scope.filterSuggestions.filter(function(item) {
            return item.active;
          });
          if (activeFilters.length > 0) {
            $scope.selectFilter(activeFilters[0]);
          }
        } else {
          var activeAppliedFilters = $scope.appliedFilters.filter(function(item) {
            return item.showAutoComplete;
          });
          if (activeAppliedFilters.length > 0) {
            var activeOptions = activeAppliedFilters[0].filteredOptions.filter(function(item) {
              return item.active;
            });
            if (activeOptions.length > 0) {
              $scope.selectOption(null, activeOptions[0], activeAppliedFilters[0]);
            }
          }
          if (activeAppliedFilters.length === 0 || activeOptions.length === 0) {
            $scope.appliedFilters.filter(function(item) {
              return !item.currentOption;
            }).forEach(function(item) {
              if (item.searchOptionInput !== '') {
                $scope.selectOption(null, {
                  key: item.searchOptionInput,
                  label: item.searchOptionInput
                }, item);
              }
            });
          }
        }
      }

      function backspaceKeyHandler (event) {
        if ($(document.activeElement).is('input') && $(document.activeElement)[0].selectionStart === 0) {
          if ($(document.activeElement).hasClass('main-input') && $scope.appliedFilters.length > 0) {
            var lastFilter = $scope.appliedFilters[$scope.appliedFilters.length - 1];
            focusInput(lastFilter);
            event.stopPropagation();
            event.preventDefault();
          } else {
            var name = $(document.activeElement).attr('name');
            var activeFilter = $scope.appliedFilters.filter(function(item) {
              return name === item.id;
            })[0];
            if (activeFilter) {
              $scope.removeFilter(activeFilter);
            }
          }
        }
      }

      function attachInputWidthSetter(element) {
        var textPadding = 4;
        element.on('input', function() {
          var inputWidth = $(this).textWidth();
          $(this).css({
            width: inputWidth + textPadding
          })
        }).trigger('input');
      }
    }
  };
});

$.fn.textWidth = function(text, font) {
  if (!$.fn.textWidth.fakeEl) $.fn.textWidth.fakeEl = $('<span>').hide().appendTo(document.body);
  $.fn.textWidth.fakeEl.text(text || this.val() || this.text() || this.attr('placeholder')).css('font', font || this.css('font'));
  return $.fn.textWidth.fakeEl.width();
};
