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
import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {HttpClientModule, HTTP_INTERCEPTORS} from '@angular/common/http';
import {DataTableModule} from "angular2-datatable";
import {TreeModule} from 'angular-tree-component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {NgxEchartsModule} from 'ngx-echarts';
import {MatDatepickerModule, MatNativeDateModule} from '@angular/material';
import {Location, LocationStrategy, HashLocationStrategy} from '@angular/common';
import {ToasterModule, ToasterService} from 'angular2-toaster';
import {FormsModule} from '@angular/forms';
import {AppComponent} from './app.component';
import {MeasureComponent} from './measure/measure.component';
import {JobComponent} from './job/job.component';
import {SidebarComponent} from './sidebar/sidebar.component';
import {HealthComponent} from './health/health.component';
import {MydashboardComponent} from './mydashboard/mydashboard.component';
import {CreateMeasureComponent} from './measure/create-measure/create-measure.component';
import {MeasureDetailComponent} from './measure/measure-detail/measure-detail.component';
import {MetricComponent} from './metric/metric.component';
import {DetailMetricComponent} from './metric/detail-metric/detail-metric.component';
import {DataassetComponent} from './dataasset/dataasset.component';
import {BatchComponent} from './job/create-job/batch/batch.component';
import {AcComponent} from './measure/create-measure/ac/ac.component';
import {PrComponent} from './measure/create-measure/pr/pr.component';
import {PrStep1Component} from './measure/create-measure/pr/step1/step1.component';
import {PrStep2Component} from './measure/create-measure/pr/step2/step2.component';
import {PrStep3Component} from './measure/create-measure/pr/step3/step3.component';
import {PrStep4Component} from './measure/create-measure/pr/step4/step4.component';
import {PrConfirmModal} from './measure/create-measure/pr/confirmModal/confirmModal.component';
import {PubComponent} from './measure/create-measure/pub/pub.component';
import {LoginComponent} from './login/login.component';
import {AngularMultiSelectModule} from 'angular2-multiselect-dropdown/angular2-multiselect-dropdown';
import {RuleComponent} from './measure/create-measure/pr/rule/rule.component';
import {TruncatePipe} from './sidebar/truncate.pipe';
import {ConfigurationComponent} from './measure/create-measure/configuration/configuration.component';
import {NouisliderModule} from 'ng2-nouislider';
import {HttpService} from './service/http.service';
import {LoaderService} from './loader/loader.service';
import {LoaderComponent} from './loader/loader.component';
import {JobDetailComponent} from './job/job-detail/job-detail.component';
import {StreamingComponent} from './job/create-job/streaming/streaming.component';
import {RawComponent} from "./measure/create-measure/raw/raw.component";


const appRoutes: Routes = [
  {
    path: 'health',
    component: HealthComponent
  },
  {
    path: 'measures',
    component: MeasureComponent
  },
  {
    path: 'measure/:id',
    component: MeasureDetailComponent
  },
  {
    path: 'mydashboard',
    component: MetricComponent
  },
  {
    path: 'jobs',
    component: JobComponent
  },
  {
    path: 'job/:id',
    component: JobDetailComponent
  },
  {
    path: 'createjob/batch',
    component: BatchComponent

  },
  {
    path: 'createjob/streaming',
    component: StreamingComponent

  },
  {
    path: 'createmeasure',
    component: CreateMeasureComponent
  },
  {
    path: 'createmeasureac',
    component: AcComponent
  },
  {
    path: 'createmeasurepr',
    component: PrComponent
  },
  {
    path: 'createmeasurepub',
    component: PubComponent
  },
  {
    path: 'createmeasureraw',
    component: RawComponent
  },
  {
    path: 'detailed/:name',
    component: DetailMetricComponent
  },
  {
    path: 'dataassets',
    component: DataassetComponent
  },
  {
    path: 'metrics',
    component: MetricComponent
  },
  {
    path: '',
    redirectTo: 'health',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent
  },
  // {
  //    path: '**',
  //    component: AppComponent
  // }

];

@NgModule({
  declarations: [
    AppComponent,
    MeasureComponent,
    JobComponent,
    SidebarComponent,
    HealthComponent,
    MydashboardComponent,
    CreateMeasureComponent,
    MeasureDetailComponent,
    MetricComponent,
    DetailMetricComponent,
    DataassetComponent,
    BatchComponent,
    AcComponent,
    PrComponent,
    PrStep1Component,
    PrStep2Component,
    PrStep3Component,
    PrStep4Component,
    PrConfirmModal,
    PubComponent,
    LoginComponent,
    RuleComponent,
    TruncatePipe,
    ConfigurationComponent,
    LoaderComponent,
    JobDetailComponent,
    StreamingComponent,
    RawComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    TreeModule,
    BrowserAnimationsModule,
    ToasterModule,
    FormsModule,
    NouisliderModule,
    NgxEchartsModule,
    DataTableModule,
    AngularMultiSelectModule,
    RouterModule.forRoot(
      appRoutes,
      {useHash: true},
    ),
    MatNativeDateModule,
    MatDatepickerModule
  ],
  exports: [
    LoaderComponent
  ],
  providers: [LoaderService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: HttpService,
      multi: true,
    }],
  bootstrap: [AppComponent]
})
export class AppModule {
}
