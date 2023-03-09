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
import {async, ComponentFixture, TestBed, inject} from '@angular/core/testing';

import { AppModule } from '../../app.module';
import {CreateMeasureComponent} from './create-measure.component';
import { LoaderService } from '../../loader/loader.service';

describe('CreateMeasureComponent', () => {
  let component: CreateMeasureComponent;
  let fixture: ComponentFixture<CreateMeasureComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ AppModule ],
      declarations: [],
      providers: [ ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CreateMeasureComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', inject([LoaderService], () => {
    expect(component).toBeTruthy();
  }));
});
