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

import {
  AfterContentInit,
  Component,
  ContentChildren,
  Directive,
  EventEmitter,
  Input,
  Output,
  QueryList,
  ViewContainerRef
} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";


@Directive({selector: '[tis-header-tool-content]'})
export class TisHeaderToolContent {
  @Input('content') content: TisHeaderTool;

  constructor(private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {

    if (this.content.nativeElement) {
      // console.info(this.content.nativeElement.innerHTML);
      this.viewContainerRef.element.nativeElement.innerHTML = this.content.nativeElement.innerHTML;
    }
  }
}

@Directive({selector: 'tis-header-tool'})
export class TisHeaderTool {
  // @ContentChild(TemplateRef) contentTempate: TemplateRef<any>;
  nativeElement: any;

  constructor(viewContainerRef: ViewContainerRef) {
    this.nativeElement = viewContainerRef.element.nativeElement;
  }
}


@Component({
  selector: 'tis-page-header-left',
  template: `<ng-content></ng-content>`
})
export class PageHeaderLeftComponent {
}

@Component({
  selector: 'final-exec-controller',
  template: `<ng-content></ng-content>`
})
export class FinalExecControllerComponent {
}




// implements OnInit, AfterContentInit
@Component({
  selector: 'tis-page-header',
  template: `
      <nz-breadcrumb *ngIf="showBreadcrumb">
          <nz-breadcrumb-item *ngIf="showBreadcrumbRoot">
              <a routerLink="/"> <i nz-icon nzType="home" nzTheme="outline"></i></a>
          </nz-breadcrumb-item>
          <nz-breadcrumb-item *ngFor="let b of _breadcrumb">
              <a [routerLink]="b.path">{{b.name}}</a>
          </nz-breadcrumb-item>
          <nz-breadcrumb-item>
              {{this.title}}
          </nz-breadcrumb-item>
      </nz-breadcrumb>
      <nz-affix [nzOffsetTop]="10">
          <div class="tis-head">
              <tis-msg [result]="result"></tis-msg>
              <div [ngClass]="headTitleClass">
                  <div class="head-tool-block">
                      <button *ngIf="refesh.observers.length>0" nz-button (click)="blockRefesh()"><i nz-icon nzType="redo" nzTheme="outline"></i></button>
                      <ng-content></ng-content>
                  </div>
                  <a *ngIf="back" href="javascript:void(0)" (click)="comeback()">
                      <i class="fa fa-reply" aria-hidden="true"></i></a>
                  <ng-content select="tis-page-header-left"></ng-content>
              </div>
              <div style="clear:both;"></div>
          </div>
      </nz-affix>
  `,

  styles: [`

      nz-breadcrumb {
          margin-left: 10px;
          margin-bottom: 15px;
          margin-top: 10px;
      }

      .tis-head {
          margin-bottom: 8px;
      }

      .head-title-lg {
          font-size: 28px;
      }

      .head-title-sm {
          font-size: 20px;
      }

      .head-tool-block {
          float: right;
      }

      .head-tool-block button {
          margin-right: 10px;
      }

      a:link {
          color: black;
      }

      a:hover {
          color: #0275d8;
      }
  `]
})
export class PageHeaderComponent implements AfterContentInit {

  @ContentChildren(TisHeaderTool) tools: QueryList<TisHeaderTool>;
  @Output() refesh = new EventEmitter<any>();

 // @Input() needRefesh = false;
  @Input() back: string;
  @Input()
  result: { success: boolean, msg: any[], errormsg: any[] };
  @Input() title: string;
  @Input() size: 'sm' | 'lg' = 'lg';
  @Input() showBreadcrumb = true;
  @Input() showBreadcrumbRoot = true;
  _breadcrumb: Array<{ name: string, path: string }> = []

  @Input()
  set breadcrumb(vals: Array<string>) {

    if (!vals || (vals.length % 2) !== 0) {
      throw new Error(`length of param vals shall be even,length:${vals.length}`);
    }

    for (let i = 0; i < vals.length; i += 2) {
      this._breadcrumb.push({name: vals[i], path: vals[i + 1]})
    }
  }


  constructor(private router: Router, private route: ActivatedRoute) {
  }

  get headTitleClass(): string {
    return this.size === 'sm' ? 'head-title-sm' : 'head-title-lg';
  }

  comeback(): void {
    this.router.navigate([this.back], {relativeTo: this.route});
  }

  ngAfterContentInit() {
  }

  public blockRefesh(): void {
    this.refesh.emit();
  }

}



