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

App.CodeMirrorComponent = Ember.Component.extend({
  tagName: "textarea",
  readOnly:false,
  codeMirror:null,
  fullscreen:false,
  updateCM:function () {
    var cm = this.get('codeMirror');
    if (this.get('readOnly')) {
      cm.setValue((this.get('content.fileContent')||''));
      return cm.setOption('readOnly',true);
    }
    var cmElement = $(cm.display.wrapper);
    if (this.get('content.isFulfilled')) {
      cm.setOption('readOnly',false);
      cmElement.removeClass('inactive');
      cm.setValue((this.get('content.fileContent')||''));
      cm.markClean();
    } else {
      cm.setOption('readOnly',true);
      cmElement.addClass('inactive');
    }
  }.observes('codeMirror', 'content.isFulfilled'),
  toggleFullScreen:function () {
    Em.run.next(this,function () {
      this.get('codeMirror').setOption("fullScreen", this.get('fullscreen'));
    });
  }.observes('fullscreen'),
  didInsertElement: function() {
    var cm = CodeMirror.fromTextArea(this.get('element'),{
      lineNumbers: true,
      matchBrackets: true,
      indentUnit: 4,
      keyMap: "emacs",
      mode: "text/x-pig",
      extraKeys: {
        "Ctrl-Space": function (cm) {
          if (!cm.getOption('readOnly')) {
            cm.showHint({completeSingle:false});
          }
        },
        "F11": function(cm) {
          this.set('fullscreen',!this.get("fullscreen"));
        }.bind(this),
        "Esc": function(cm) {
          if (this.get("fullscreen")) this.set("fullscreen", false);

        }.bind(this)
      }
    });

    var updateToggle = function () {
      var addMargin = $('.CodeMirror-vscrollbar').css('display') === "block";
      var margin = $('.CodeMirror-vscrollbar').width();
      $('.fullscreen-toggle').css('right',((addMargin)?3+margin:3));

      $('#scriptDetails pre.CodeMirror-line').each(function( index ) {
        $(this).css('margin-left', '30px');
      });

      $('.CodeMirror-gutters').css('width', '29px');

    };

    cm.on('viewportChange',updateToggle);

    $('.editor-container').resizable({
      stop:function () {
        cm.setSize(null, this.style.height);
        updateToggle();
      },
      resize: function() {
        this.getElementsByClassName('CodeMirror')[0].style.height = this.style.height;
        this.getElementsByClassName('CodeMirror-gutters')[0].style.height = this.style.height;
      },
      minHeight:218,
      handles: {'s': '#sgrip' }
    });

    this.set('codeMirror',cm);
    if (!this.get('readOnly')) {
      cm.on('keyup',function (cm, e) {
        var inp = String.fromCharCode(e.keyCode);
        if (e.type == "keyup" && /[a-zA-Z0-9-_ ]/.test(inp)) {
          cm.showHint({completeSingle:false});
        }
      });
      cm.focus();
      cm.on('change', Em.run.bind(this,this.editorDidChange));
    }

  },
  editorDidChange:function () {
    var pig_script = this.get('content');
    if (pig_script.get('isLoaded')){
      pig_script.set('fileContent',this.get('codeMirror').getValue());
      this.get('codeMirror').markClean();
    }
  },
  scriptDidChange:function () {
    if (this.get('codeMirror').isClean() && this.get('content.isDirty')) {
      this.get('codeMirror').setValue(this.get(('content.fileContent')||''));
    }
  }.observes('content.fileContent'),
  willClearRender:function () {
    this.set('fullscreen', false);
    this.get('codeMirror').toTextArea();
  }
});
