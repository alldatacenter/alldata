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

App.XmldiffViewerComponent = Ember.Component.extend({
  layoutName: 'components/xmldiffViewer',
  diffConfig: null,
  viewConfig: null,

  actions: {
    closeDiffViewer: function() {
      this.set('diffConfig', null);
      this.set('viewConfig', null);
    }
  },

  diffViewObserver: function() {
    if (this.get('diffConfig')) {
      this.showXmlDiffView();
    }
  }.observes('diffConfig'),

  viewConfigObserver: function() {
    if (this.get('viewConfig')) {
      this.showCapSchedViewXml();
    }
  }.observes('viewConfig'),

  showXmlDiffView: function() {
    this.$("#xmldiffViewerDialog").modal('show');
    var config = this.get('diffConfig'),
      basetxt = difflib.stringAsLines(config.baseXML);
      newtxt = difflib.stringAsLines(config.newXML),
      sm = new difflib.SequenceMatcher(basetxt, newtxt),
      opcodes = sm.get_opcodes(),
      $diffOutput = this.$('#xmldiffOutput');

    this.showHideOutputContainers('diffView');
    this.setDialogDimentions();

    $diffOutput.append(diffview.buildView({
      baseTextLines: basetxt,
      newTextLines: newtxt,
      opcodes: opcodes,
      baseTextName: 'Base XML',
      newTextName: 'New XML',
      contextSize: 2,
      viewType: 0
    }));
  },

  showCapSchedViewXml: function() {
    this.$("#xmldiffViewerDialog").modal('show');
    var config = this.get('viewConfig'),
      $output = this.$('#capshedViewXml');

    this.showHideOutputContainers('xmlView');
    this.setDialogDimentions(true);
    $output.text(config.xmlConfig);
  },

  showHideOutputContainers: function(viewType) {
    this.$('#xmldiffOutput').empty().hide();
    this.$('#capshedViewXml').empty().hide();
    if (viewType === 'diffView') {
      this.$('#xmldiffOutput').show();
    } else {
      this.$('#capshedViewXml').show();
    }
  },

  setDialogDimentions: function(hideOverflow) {
    var dialogHt = this.$("#xmldiffViewerDialog").height(),
      contentHt = dialogHt-60,
      bodyHt = contentHt-170;

    this.$("#xmldiffViewerDialog").find('.modal-content').height(contentHt);
    this.$("#xmldiffViewerDialog").find('.modal-body').height(bodyHt);
    if (hideOverflow) {
      this.$("#xmldiffViewerDialog").find('.modal-body').css('overflow', 'hidden');
    } else {
      this.$("#xmldiffViewerDialog").find('.modal-body').css('overflow', 'auto');
    }
  }
});
