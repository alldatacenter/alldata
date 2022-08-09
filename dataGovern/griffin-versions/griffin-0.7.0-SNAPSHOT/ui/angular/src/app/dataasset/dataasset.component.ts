/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
import {Component, OnInit} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import * as $ from 'jquery';
import {ServiceService} from '../service/service.service';

@Component({
  selector: 'app-dataasset',
  templateUrl: './dataasset.component.html',
  providers: [ServiceService],
  styleUrls: ['./dataasset.component.css']
})
export class DataassetComponent implements OnInit {
  public results = [];
  public visible = false;
  public visibleAnimate = false;
  sourceTable: string;
  targetTable: string;
  data: object;

  public hide(): void {
    this.visibleAnimate = false;
    setTimeout(() => this.visible = false, 300);
  }

  public onContainerClicked(event: MouseEvent): void {
    if ((<HTMLElement>event.target).classList.contains('modal')) {
      this.hide();
    }
  }

  constructor(private http: HttpClient, public serviceService: ServiceService) {
  }

  ngOnInit() {
    var allDataassets = this.serviceService.config.uri.dataassetlist;
    this.http.get(allDataassets).subscribe(data => {
      for (let db in data) {
        for (let table of data[db]) {
          table.location = table.sd.location;
          this.results.push(table);
        }
      }
      $('.icon').hide();
    }, err => {

    });
  };
}
