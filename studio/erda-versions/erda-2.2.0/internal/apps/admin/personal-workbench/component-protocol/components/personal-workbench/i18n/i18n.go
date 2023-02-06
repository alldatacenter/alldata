// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package i18n

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
)

const (
	I18nKeyIssueExpired         = "IssueExpired"
	I18nKeyIssueExpireToday     = "IssueExpireToday"
	I18nKeyIssueUndo            = "IssueUndo"
	I18nKeyMspServiceCount      = "ServiceCount"
	I18nKeyMspLast24HAlertCount = "Last24HAlertCount"
	I18nKeyMR                   = "MR"
	I18nKeyRuntime              = "Runtime"
	I18nKeyProject              = "Project"
	I18nKeySearchByProjectName  = "SearchByProjectName"
	I18nKeySearchByAppName      = "SearchByAppName"
	I18nKeyMyProject            = "MyProject"
	I18nKeyMyApp                = "MyApplication"
	I18nKeyDevOpsProject        = "DevOpsProject"
	I18nKeyMspProject           = "MspProject"
	I18nKeyProjManagement       = "ProjManagement"
	I18nKeyAppDevelop           = "AppDevelop"
	I18nKeyTestManagement       = "TestManagement"
	I18nKeyServiceMonitor       = "ServiceMonitor"
	I18nKeyProjSetting          = "ProjSetting"
	I18nKeyServiceList          = "ServiceList"
	I18nKeyServiceObservation   = "ServiceObservation"
	I18nKeyServiceTracing       = "ServiceTracing"
	I18nKeyLogAnalysis          = "LogAnalysis"
	I18nKeySubscribeThisProj    = "SubscribeThisProj"
	I18nKeySubscribeThisApp     = "SubscribeThisApp"
	I18nKeyUnSubscribe          = "UnSubscribe"
	I18nKeyGitRepo              = "GitRepo"
	I18nKeyPipeline             = "Pipeline"
	I18nKeyAppApiDesign         = "AppApiDesign"
	I18nKeyAppDeployCenter      = "AppDeployCenter"
	I18nKeyAppModeLIBRARY       = "AppModeLIBRARY"
	I18nKeyAppModeBIGDATA       = "AppModeBIGDATA"
	I18nKeyAppModeSERVICE       = "AppModeSERVICE"
	I18nAppModePROJECTSERVICE   = "AppModePROJECTSERVICE"
	I18nKeyAppModeMOBILE        = "AppModeMOBILE"
	I18nKeyUnreadMes            = "UnreadMessages"
	I18nKeyTicket               = "Tickets"
	I18nKeyApproveRequest       = "ApproveRequest"
	I18nKeyActivities           = "Activities"
	I18nStarAPP                 = "star application"
	I18nStarProject             = "star project"
	I18nReachLimit              = "ReachLimit"
)

func GenStarTip(itemType apistructs.WorkbenchItemType, star bool) string {
	if star {
		return I18nKeyUnSubscribe
	}
	support := []apistructs.WorkbenchItemType{apistructs.WorkbenchItemProj, apistructs.WorkbenchItemApp}
	switch itemType {
	case apistructs.WorkbenchItemProj:
		return I18nKeySubscribeThisProj
	case apistructs.WorkbenchItemApp:
		return I18nKeySubscribeThisApp
	default:
		logrus.Warnf("unknown workbench item type, not in %v, return empty", support)
		return ""
	}
}
