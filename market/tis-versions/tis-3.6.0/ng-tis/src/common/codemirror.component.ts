/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

/**
 * Created by baisui on 2017/3/29 0029.
 */
import {AfterContentInit, AfterViewInit, Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild} from '@angular/core';

import {Editor, EditorConfiguration, EditorFromTextArea, fromTextArea} from 'codemirror';
// @ts-ignore

import "node_modules/codemirror/lib/codemirror.js";
import "node_modules/codemirror/mode/sql/sql.js";
import "node_modules/codemirror/mode/javascript/javascript.js";
import "node_modules/codemirror/mode/clike/clike.js";
import "node_modules/codemirror/mode/xml/xml.js";
import "node_modules/codemirror/mode/solr/solr.js";
import "node_modules/codemirror/mode/yaml/yaml.js";
import "node_modules/codemirror/mode/velocity/velocity.js";
// ControlValueAccessor
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from "@angular/forms";

@Component({
  selector: 'tis-codemirror',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CodemirrorComponent),
      multi: true
    }
  ],
  template: `
      <textarea #host></textarea>
  `,
  styles: [
      `
          .CodeMirror {
              height: auto;
          }

          .CodeMirror-scroll {
              height: auto;
              overflow-y: auto;
              overflow-x: auto;
          }
    `
  ]
})

export class CodemirrorComponent implements AfterViewInit, AfterContentInit, ControlValueAccessor, OnInit {
  @ViewChild('host', {static: false}) scriptEditor: ElementRef;
  private mirror: EditorFromTextArea;

  @Input() config: any;
  @Input() size: { width: number, height: number };
  @Output() change = new EventEmitter();
  @Output() focus = new EventEmitter();
  @Output() blur = new EventEmitter();
  @Output() cursorActivity = new EventEmitter();


  _value = '';

  constructor() {

  }

  public save(): void {
    // console.log("save");
    this.mirror.focus();
  }

  ngOnInit(): void {

  }

  ngAfterContentInit(): void {
  }

  ngAfterViewInit(): void {
    this.config = Object.assign(this.editorOption, this.config || {});
    // console.log(this.config);
    this.codemirrorInit(this.config);
  }

  codemirrorInit(config: any) {
    this.mirror = fromTextArea(this.scriptEditor.nativeElement, config);

    if (this.size) {
      this.mirror.setSize(this.size.width, this.size.height);
    } else {
      this.mirror.setSize("100%", "100%");
    }

    this.mirror.on('change', () => {
      this.updateValue(this.mirror.getValue());
    });

    this.mirror.on('focus', (instance: Editor) => {
      // console.log("fire focus");
      this.focus.emit({instance});
    });

    this.mirror.on('cursorActivity', (instance) => {
      this.cursorActivity.emit({instance});
    });

    this.mirror.on('blur', (instance: Editor) => {
      this.blur.emit({instance});
    });
  }

  /**
   * Implements ControlValueAccessor
   */
  writeValue(value: string) {
    this._value = value || '';
    if (this.mirror) {
      // console.log(this._value);
      this.mirror.setValue(this._value);
    }
  }

  registerOnChange(fn: any) {
    this.onChange = fn;
  }

  registerOnTouched(fn: any) {
    this.onTouched = fn;
  }

  /**
   * Value update process
   */
  updateValue(value: string) {
    this.value = value;
    this.onTouched();
    this.change.emit(value);
  }

  get value() {
    return this._value;
  }

  @Input() set value(v) {
    if (v !== this._value) {
      this._value = v;
      this.onChange(v);
    }
  }

  onChange(_: any) {
  }

  onTouched() {
  }


  private get editorOption(): EditorConfiguration {
    return {
      mode: "text/x-hive",
      indentWithTabs: true,
      // theme: "eclipse",
      smartIndent: true,
      lineNumbers: true,
      lineWrapping: true,
      // matchBrackets : true,
      // autofocus: true,
      // extraKeys: {"Ctrl-Space": "autocomplete"},
      // hintOptions: {tables: {
      //   users: ["name", "score", "birthDate"],
      //     countries: ["name", "population", "size"]
      // }}
    };

  }

}

