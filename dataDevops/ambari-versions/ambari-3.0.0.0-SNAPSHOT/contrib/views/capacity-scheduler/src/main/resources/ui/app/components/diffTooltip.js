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

App.DiffTooltipComponent = Em.Component.extend({
  classNames:'fa fa-fw fa-lg'.w(),
  tagName:'i',
  queue:null,
  isActive:true,
  toggleTooltip: function () {
    if (this.get('isActive')) {
      this.$().tooltip({
        title:this.buildDiff.bind(this),
        html:true,
        placement:'bottom'
      });
    } else {
      this.$().tooltip('destroy');
    }
  }.observes('isActive').on('didInsertElement'),
  buildDiff: function () {
    var queue = this.get('queue'),
        caption = '',
        fmtString = '<span>%@: %@ -> %@</span>\n',
        emptyValue = '<small><em>not set</em></small>',
        changes = queue.changedAttributes(),
        idsToNames = function (l) {
          return l.split('.').get('lastObject');
        },
        formatChangedAttributes = function (prefix,item) {
          // don't show this to user.
          if (item == '_accessAllLabels') return;

          var oldV = this[item].objectAt(0),
              newV = this[item].objectAt(1);

          caption += fmtString.fmt(
              [prefix,item].compact().join('.'),
              (oldV != null && '\'%@\''.fmt(oldV)) || emptyValue,
              (newV != null && '\'%@\''.fmt(newV)) || emptyValue
            );
        },
        initialLabels,
        currentLabels,
        isAllChanged,
        oldV,
        newV;

    if (queue.get('isError')) {
      return 'Data was not saved';
    }

    Em.keys(changes).forEach(Em.run.bind(changes,formatChangedAttributes,null));

    if (queue.constructor.typeKey === 'queue') {
      //cpmpare labels
      isAllChanged = changes.hasOwnProperty('_accessAllLabels');
      initialLabels = queue.get('initialLabels').sort();
      currentLabels = queue.get('labels').mapBy('id').sort();

      if (queue.get('isLabelsDirty') || isAllChanged) {

        oldV = ((isAllChanged && changes._accessAllLabels.objectAt(0)) || (queue.get('accessAllLabels') && !isAllChanged))?'*':initialLabels.map(idsToNames).join(',') || emptyValue;
        newV = ((isAllChanged && changes._accessAllLabels.objectAt(1)) || (queue.get('accessAllLabels') && !isAllChanged))?'*':currentLabels.map(idsToNames).join(',') || emptyValue;

        caption += fmtString.fmt('accessible-node-labels', oldV, newV);
      }

      queue.get('labels').forEach(function (label) {
        var labelsChanges = label.changedAttributes(),
            prefix = ['accessible-node-labels',label.get('name')].join('.');
        Em.keys(labelsChanges).forEach(Em.run.bind(labelsChanges,formatChangedAttributes,prefix));
      });
    }

    return caption;
  }
});
