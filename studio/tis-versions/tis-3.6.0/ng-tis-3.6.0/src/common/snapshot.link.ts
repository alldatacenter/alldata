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
 * Created by baisui on 2017/4/18 0018.
 */
import {Component, Input} from '@angular/core';
import {BasicFormComponent} from "./basic.form.component";
import {TISService} from "./tis.service";
import {ActivatedRoute, Router} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";


@Component({
  selector: "snapshot-linker",
  template: `
      <a nz-dropdown [nzClickHide]="false" [nzDropdownMenu]="schemaEditDropdown">[schema.xml]<i nz-icon nzType="down"></i></a>
      <nz-dropdown-menu #schemaEditDropdown>
          <ul nz-menu>
              <li nz-menu-item (click)="openSchemaDialog(true)"><i nz-icon nzType="file-excel" nzTheme="outline"></i>xml</li>
              <li nz-menu-item (click)="openSchemaVisualDialog()"><i nz-icon nzType="eye" nzTheme="outline"></i>高级</li>
          </ul>
      </nz-dropdown-menu>
      <button nz-button nzType="link" (click)="openSolrConfigDialog()">[solr.xml]</button>
  `,
})
export class SnapshotLinkComponent extends BasicFormComponent {
  @Input() snapshot: SnapshotLink;

  // constructor(private modalService: NgbModal) {
  //
  // }

  constructor(tisService: TISService, modalService: NzModalService, private router: Router, private route: ActivatedRoute) {
    super(tisService, modalService);
  }

  openSchemaDialog(editable: boolean): boolean {
    this.router.navigate(['../xml_conf/', 'schema', this.snapshot.snId], {relativeTo: this.route});
    return false;
  }

  openSchemaVisualDialog(): void {
    // let modalRef: NgbModalRef = this.openNormalDialog(SchemaEditVisualizingModelComponent);
    // modalRef.componentInstance.snapshotid = this.snapshot.snId;

    this.router.navigate(['../schema_visual/', this.snapshot.snId], {relativeTo: this.route});
  }


  openSolrConfigDialog(): void {
    this.router.navigate(['../xml_conf/', 'config', this.snapshot.snId], {relativeTo: this.route});
  }

}

// {
//   "appId":111498,
//   "createTime":1557187739000,
//   "createUserId":999,
//   "createUserName":"baisui",
//   "preSnId":20664,
//   "resApplicationId":13380,
//   "resCorePropId":13307,
//   "resJarId":13226,
//   "resSchemaId":15002,
//   "resSolrId":14478,
//   "snId":20688,
//   "updateTime":1557187739000
// },
interface SnapshotLink {
  snId: number;
  resSolrId: number;
  resSchemaId: number;
  appId: number;
  createTime: number;
}
