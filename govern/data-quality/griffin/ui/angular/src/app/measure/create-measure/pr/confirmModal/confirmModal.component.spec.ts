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
import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {PrConfirmModal} from './confirmModal.component';
import { ProfilingStep1, ProfilingStep2, ProfilingStep3, ProfilingStep4 } from '../pr.component';

describe('PrConfirmModalComponent', () => {
  let component: PrConfirmModal;
  let fixture: ComponentFixture<PrConfirmModal>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [PrConfirmModal]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PrConfirmModal);
    component = fixture.componentInstance;
    component.step1 = new ProfilingStep1();
    component.step2 = new ProfilingStep2();
    component.step3 = new ProfilingStep3();
    component.step4 = new ProfilingStep4();
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
