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
import {Injectable} from "@angular/core";
import {environment} from "../../environments/environment";

@Injectable()
export class ServiceService {
  constructor() {
  }

  public BACKEND_SERVER = environment.BACKEND_SERVER;
  public API_ROOT_PATH = "/api/v1";

  public config = {
    // URI paths, always have a trailing /
    uri: {
      base: this.BACKEND_SERVER + this.API_ROOT_PATH,

      login: this.BACKEND_SERVER + this.API_ROOT_PATH + "/login/authenticate",
      
      dbtree: this.BACKEND_SERVER + this.API_ROOT_PATH + "/metadata/hive/dbs/tables",
      dataassetlist: this.BACKEND_SERVER + this.API_ROOT_PATH + "/metadata/hive/dbs/tables",
      dblist: this.BACKEND_SERVER + this.API_ROOT_PATH + "/metadata/hive/dbs",
      tablenames: this.BACKEND_SERVER + this.API_ROOT_PATH + "/metadata/hive/tables/names", // ?db=...
      dbtablenames: this.BACKEND_SERVER + this.API_ROOT_PATH + "/metadata/hive/dbs/tables/names",
      dbtable: this.BACKEND_SERVER + this.API_ROOT_PATH + "/metadata/hive/table", // ?db=...&table=...

      getdataasset: this.BACKEND_SERVER + this.API_ROOT_PATH + "/dataassets",

      //mydashboard
      getmydashboard:
      this.BACKEND_SERVER + this.API_ROOT_PATH + "/metrics/mydashboard/",
      // getsubscribe: this.BACKEND_SERVER + this.API_ROOT_PATH + '/subscribe/',
      // newsubscribe: this.BACKEND_SERVER + this.API_ROOT_PATH + '/subscribe',

      //metrics

      statistics: this.BACKEND_SERVER + this.API_ROOT_PATH + "/jobs/health",

      // briefmetrics: this.BACKEND_SERVER + this.API_ROOT_PATH + '/metrics/briefmetrics',
      heatmap: this.BACKEND_SERVER + this.API_ROOT_PATH + "/metrics/heatmap",
      // metricdetail: this.BACKEND_SERVER + this.API_ROOT_PATH + '/metrics/complete',
      // rulemetric: this.BACKEND_SERVER + this.API_ROOT_PATH + '/metrics/brief',
      //              organization:this.BACKEND_SERVER+'/org',

      orgmap: this.BACKEND_SERVER + this.API_ROOT_PATH + "/metrics/org",

      metricsByOrg: "",
      //              organization:'/org.json',
      //              dashboard:'/dashboard.json',

      // organization:this.BACKEND_SERVER + this.API_ROOT_PATH + '/org/measure/jobs',
      dashboard: this.BACKEND_SERVER + this.API_ROOT_PATH + "/metrics",
      metricdetail:
      this.BACKEND_SERVER + this.API_ROOT_PATH + "/metrics/values",

      // dashboard:this.ES_SERVER+'/griffin/accuracy/_search?pretty&filter_path=hits.hits._source',
      metricsample:
      this.BACKEND_SERVER + this.API_ROOT_PATH + "/metrics/sample",
      metricdownload:
      this.BACKEND_SERVER + this.API_ROOT_PATH + "/metrics/download",
      missRecordDownload:
      this.BACKEND_SERVER + this.API_ROOT_PATH + "/jobs/download",


      //Models
      allModels: this.BACKEND_SERVER + this.API_ROOT_PATH + "/measures",
      addModels: this.BACKEND_SERVER + this.API_ROOT_PATH + "/measures",
      deleteModel: this.BACKEND_SERVER + this.API_ROOT_PATH + "/measures",
      getModel: this.BACKEND_SERVER + this.API_ROOT_PATH + "/measures",
      enableModel:
      this.BACKEND_SERVER + this.API_ROOT_PATH + "/models/enableModel",

      //Jobs
      allJobs: this.BACKEND_SERVER + this.API_ROOT_PATH + "/jobs",
      addJobs: this.BACKEND_SERVER + this.API_ROOT_PATH + "/jobs",
      modifyJobs: this.BACKEND_SERVER + this.API_ROOT_PATH + "/jobs",
      getJobById: this.BACKEND_SERVER + this.API_ROOT_PATH + "/jobs/config",
      triggerJobById: this.BACKEND_SERVER + this.API_ROOT_PATH + "/jobs/trigger",
      getMeasuresByOwner:
      this.BACKEND_SERVER + this.API_ROOT_PATH + "/measures/owner/",
      deleteJob: this.BACKEND_SERVER + this.API_ROOT_PATH + "/jobs",
      getInstances:
      this.BACKEND_SERVER + this.API_ROOT_PATH + "/jobs/instances",

      //              allJobs:'/jobs.json',
      newAccuracyModel: this.BACKEND_SERVER + this.API_ROOT_PATH + "/models",
      newValidityModel: this.BACKEND_SERVER + this.API_ROOT_PATH + "/models",
      newAnomalyModel: this.BACKEND_SERVER + this.API_ROOT_PATH + "/models",
      newPublishModel: this.BACKEND_SERVER + this.API_ROOT_PATH + "/models",
      // newAccuracyModel: this.BACKEND_SERVER + this.API_ROOT_PATH + '/models/newAccuracyModel' ,
      // newValidityModel: this.BACKEND_SERVER + this.API_ROOT_PATH + '/model/newValidityModel' ,
      // newAnomalyModel: this.BACKEND_SERVER + this.API_ROOT_PATH + '/model/newAnomalyModel' ,
      // newPublishModel: this.BACKEND_SERVER + this.API_ROOT_PATH + '/model/newPublishModel' ,
      // getAccuracyModel: this.BACKEND_SERVER + this.API_ROOT_PATH + '/model/getAccuracyModel',
      // getValidityModel: this.BACKEND_SERVER + this.API_ROOT_PATH + '/model/getValidityModel',
      // getPublishModel: this.BACKEND_SERVER + this.API_ROOT_PATH + '/model/getPublishModel',
      // getAnomalyModel: this.BACKEND_SERVER + this.API_ROOT_PATH + '/model/getAnomalyModel',

      //Notification
      getnotifications:
      this.BACKEND_SERVER + this.API_ROOT_PATH + "/notifications"
    }
  };
}
