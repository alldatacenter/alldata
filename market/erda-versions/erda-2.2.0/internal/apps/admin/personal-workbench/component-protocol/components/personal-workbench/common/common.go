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

package common

import (
	"reflect"

	"github.com/pkg/errors"
	"gopkg.in/square/go-jose.v2/json"
)

const (
	ScenarioKey = "personal-workbench"

	// FilterNameKey list query key
	FilterNameKey = "FilterName"
	// WorkTabKey work tab switch, e.g: project, app
	WorkTabKey = "workTabKey"
	MsgTabKey  = "messageTabKey"

	// TabData load data from tabs transfer with global state.
	TabData = "tabData"

	AppService     = "appService"
	ProjectService = "projectService"

	EventChangeEventTab = "onChange"

	// ProjDevOpsStatus titleState status; error(red), success(green), processing(blue), warning(yellow), default(gray)
	ProjDevOpsStatus        = "processing"
	ProjMspStatus           = "warning"
	AppLibraryStatus        = "success"
	AppBigdataStatus        = "processing"
	AppServiceStatus        = "warning"
	AppMobileStatus         = "processing"
	AppProjectServiceStatus = "error"
	UnreadMsgStatus         = "error"

	DefaultProject = ""
	MspProject     = "MSP"
	DevOpsProject  = "DevOps"

	// OpKeyProjectID  operation related keys
	OpKeyProjectID           = "projectId"
	OpKeyAppID               = "appId"
	OpKeyWorkSpace           = "workspace"
	OpKeyIssueFilterUrlQuery = "issueFilter__urlQuery"

	// OpValTargetProjAllIssue target related keys
	OpValTargetProjAllIssue             = "projectAllIssue"
	OpValTargetProjApps                 = "projectApps"
	OpValTargetMspServiceList           = "mspServiceList"
	OpValTargetMspOverview              = "mspOverview"
	OpValTargetMspMonitorServiceAnalyze = "mspMonitorServiceAnalyze"
	OpValTargetMicroTrace               = "microTrace"
	OpValTargetMspLogAnalyze            = "mspLogAnalyze"
	OpValTargetMicroServiceAlarmRecord  = "microServiceAlarmRecord"
	OpValTargetProjectTestDashboard     = "projectTestDashboard"
	OpValTargetProjectSetting           = "projectSetting"
	OpValTargetProject                  = "project"
	OpValTargetAppOpenMr                = "appOpenMr"
	OpValTargetAppDeploy                = "projectDeployEnv"
	OpValTargetRepo                     = "repo"
	OpValTargetPipelineRoot             = "pipelineRoot"
	OpValTargetAppApiDesign             = "appApiDesign"

	// IconProjManagement icon value
	IconProjManagement     = "xiangmuguanli"
	IconAppDevelop         = "yingyongkaifa"
	IconTestManagement     = "ceshiguanli"
	IconServiceMonitor     = "fuwujiankong"
	IconProjSetting        = "xiangmushezhi"
	IconServiceList        = "fuwuliebiao"
	IconServiceObservation = "fuwuguance"
	IconServiceTracing     = "lianluzhuizong"
	IconLogAnalysis        = "rizhifenxi"
	IconRepo               = "daimacangku"
	IconPipeline           = "liushuixian"
	IconAppApiDesign       = "apisheji"
	IconAppDeployCenter    = "bushuzhongxin"
)

var (
	PtrRequiredErr     = errors.New("b must be a pointer")
	NothingToBeDoneErr = errors.New("nothing to be done")
)

type Operation struct {
	JumpOut bool                   `json:"jumpOut"`
	Target  string                 `json:"target"`
	Query   map[string]interface{} `json:"query"`
	Params  map[string]interface{} `json:"params"`
}

// Transfer transfer a to b with json, kind of b must be pointer
func Transfer(a, b interface{}) error {
	if reflect.ValueOf(b).Kind() != reflect.Ptr {
		return PtrRequiredErr
	}
	if a == nil {
		return NothingToBeDoneErr
	}
	aBytes, err := json.Marshal(a)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(aBytes, b); err != nil {
		return err
	}
	return nil
}
