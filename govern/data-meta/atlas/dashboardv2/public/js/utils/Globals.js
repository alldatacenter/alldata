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

define(["require", "underscore"], function(require, _) {
    "use strict";

    var Globals = {};
    //We have assigned underscore object to the window object, So that we don't have to import,
    // it in every file where underscore functions are used because of the version update.
    window._ = _;
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
            debugMetricsUrl: "#!/debugMetrics",
            relationUrl: '#!/relationship'
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
    Globals.idealTimeoutSeconds = 900;
    Globals.isFullScreenView = false;
    Globals.isLineageOnDemandEnabled = false;
    Globals.lineageNodeCount = 3;
    Globals.lineageDepth = 3;
    Globals.fromRelationshipSearch = false;

    return Globals;
});