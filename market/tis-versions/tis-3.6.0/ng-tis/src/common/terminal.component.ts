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

import {AfterContentInit, Component, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {Subject, Unsubscribable} from "rxjs";
import {NgTerminal} from "ng-terminal";
import {WSMessage} from "./basic.form.component";

@Component({
  template: `
      <ng-terminal #term></ng-terminal>
  `,
  styles: [
      `
    `
  ]
})
export class TerminalComponent implements AfterContentInit, OnInit, OnDestroy {

  logSubject: Subject<WSMessage>;
  @ViewChild('term', {static: true}) terminal: NgTerminal;

  // subscription: Unsubscribable;

  ngAfterContentInit(): void {
  }

  ngOnDestroy(): void {
    // if (this.subscription) {
    //   this.subscription.unsubscribe();
    // }
   // this.logSubject.next(new WSMessage("full", "unsubscribe"));
  }

  ngOnInit(): void {
    if (!this.logSubject) {
      throw new Error("logSubject can not be null");
    }
    if (!this.terminal) {
      throw new Error("terminal can not be null");
    }
    this.logSubject.subscribe((msg) => {
      if (msg && msg.logtype === 'full') {
        this.terminal.write(msg.data.msg + "\r\n");
      }
    });
  }

}
