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

define(["require"], function(require) {
    "use strict";

    var Globals = {};
    Globals.settings = {};
    Globals.settings.PAGE_SIZE = 25;
    Globals.saveApplicationState = {
        mainPageState: {},
        tabState: {
            stateChanged: false,
            tagUrl: "#!/tag",
            searchUrl: "#!/search",
            glossaryUrl: "#!/glossary",
            administratorUrl: "#!/administrator",
            debugMetricsUrl: "#!/debugMetrics"
        },
        detailPageState: {}
    };
    Globals.userLogedIn = {
        status: false,
        response: {}
    };
    Globals.serviceTypeMap = {};
    Globals.entityImgPath = "/img/entity-icon/";
    Globals.DEFAULT_UI = "v2";

    // Date Format
    Globals.dateTimeFormat = "MM/DD/YYYY hh:mm:ss A";
    Globals.dateFormat = "MM/DD/YYYY";
    Globals.isTimezoneFormatEnabled = true;

    Globals.isDebugMetricsEnabled = false;
    Globals.isTasksEnabled = false;
    Globals.advanceSearchData = {};
    Globals.idealTimeoutSeconds = 900;

    return Globals;
});