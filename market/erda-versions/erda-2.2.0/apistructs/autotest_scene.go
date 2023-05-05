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

package apistructs

import (
	"encoding/json"
	"strconv"
	"time"
)

type AutoTestRunCustom struct {
	LanguageType string   `json:"languageType"`
	Commands     []string `json:"commands"`
	Image        string   `json:"image"`
}

type AutoTestRunScene struct {
	RunParams map[string]interface{} `json:"runParams,omitempty"`
	SceneID   uint64                 `json:"sceneID"`
}

type AutoTestRunStep struct {
	ApiSpec map[string]interface{} `json:"apiSpec"`
	Loop    *PipelineTaskLoop      `json:"loop"`
}

type AutoTestRunWait struct {
	WaitTime    int `json:"waitTime" env:"ACTION_WAIT_TIME"` // TODO delete this field after 1.3
	WaitTimeSec int `json:"waitTimeSec" env:"ACTION_WAIT_TIME_SEC"`
}

type AutoTestRunConfigSheet struct {
	RunParams       map[string]interface{} `json:"runParams,omitempty"`
	ConfigSheetID   string                 `json:"configSheetID"`
	ConfigSheetName string                 `json:"configSheetName"`
}

type AutoTestSceneParams struct {
	ID        uint64 `json:"id,omitempty"`
	SpaceID   uint64 `json:"spaceID,omitempty"`   // 场景所属测试空间ID
	CreatorID string `json:"creatorID,omitempty"` // 创建者
	UpdaterID string `json:"updaterID,omitempty"` // 更新者
}

type AutoTestScene struct {
	AutoTestSceneParams `mapstructure:",squash"`
	Name                string                `json:"name"`
	Description         string                `json:"description"` // 描述
	PreID               uint64                `json:"preID"`       // 排序的前驱ID
	SetID               uint64                `json:"setID"`       // 场景集ID
	CreateAt            *time.Time            `json:"createAt"`
	UpdateAt            *time.Time            `json:"updateAt"`
	Status              SceneStatus           `json:"status"`    // 最新运行状态
	StepCount           uint64                `json:"stepCount"` // 步骤数量
	Inputs              []AutoTestSceneInput  `json:"inputs"`    // 输入参数
	Output              []AutoTestSceneOutput `json:"output"`    // 输出参数
	Steps               []AutoTestSceneStep   `json:"steps"`     // 步骤
	RefSetID            uint64                `json:"refSetID"`  // 引用场景集ID
	GroupID             uint64                `json:"groupID"`   // scene group
	Policy              PolicyType            `json:"policy"`
}

type AutoTestSceneInput struct {
	AutoTestSceneParams
	Name        string `json:"name"`
	Description string `json:"description"` // 描述
	Value       string `json:"value"`       // 默认值
	Temp        string `json:"temp"`        // 当前值
	SceneID     uint64 `json:"sceneID"`     // 场景id
}

type AutoTestSceneOutput struct {
	AutoTestSceneParams
	Name        string `json:"name"`
	Description string `json:"description"` // 描述
	Value       string `json:"value"`
	SceneID     uint64 `json:"sceneID"`
}

type AutoTestSceneStep struct {
	AutoTestSceneParams `mapstructure:",squash"`
	Type                StepAPIType         `json:"type"`       // 类型
	Method              StepAPIMethod       `json:"method"`     // method
	Value               string              `json:"value"`      // 值
	Name                string              `json:"name"`       // 名称
	PreID               uint64              `json:"preID"`      // 排序id
	PreType             PreType             `json:"preType"`    // 串行/并行类型
	SceneID             uint64              `json:"sceneID"`    // 场景ID
	SpaceID             uint64              `json:"spaceID"`    // 所属测试空间ID
	IsDisabled          bool                `json:"isDisabled"` // disable or enable step execute
	CreatorID           string              `json:"creatorID"`
	UpdaterID           string              `json:"updaterID"`
	Children            []AutoTestSceneStep // 并行子节点
	APISpecID           uint64              `json:"apiSpecID"` // api集市id
}

func (a *AutoTestSceneStep) ToJsonCopyText() string {
	dat := map[string]interface{}{
		"name":   a.Name,
		"type":   a.Type,
		"method": a.Method,
		"value":  a.Value,
	}
	b, _ := json.MarshalIndent(dat, "", "\t")
	return string(b)
}

type AutotestSceneRequest struct {
	AutoTestSceneParams
	Name        string `json:"name,omitempty"`
	Description string `json:"description,omitempty"` // 描述
	Value       string `json:"value,omitempty"`       // 默认值
	Temp        string `json:"temp,omitempty"`        // 当前值
	SceneID     uint64 `json:"sceneID,omitempty"`     // 场景ID
	SetID       uint64 `json:"setID,omitempty"`       // 场景集ID
	APISpecID   uint64 `json:"apiSpecID,omitempty"`   // api集市id
	RefSetID    uint64 `json:"refSetID,omitempty"`    // 引用场景集的ID

	Type       StepAPIType `json:"type,omitempty"`
	Target     int64       `json:"target,omitempty"`     // 目标位置
	GroupID    int64       `json:"groupID,omitempty"`    // 串行ID
	PreType    PreType     `json:"preType,omitempty"`    // 并行/并行
	Position   int64       `json:"position,omitempty"`   // 插入位置 (-1为前/1为后)
	IsGroup    bool        `json:"isGroup,omitempty"`    // 是否整组移动
	IsDisabled *bool       `json:"isDisabled,omitempty"` // disable or enable step execute

	PageNo   uint64 `json:"pageNo"`
	PageSize uint64 `json:"pageSize"`

	SceneGroupID uint64 `json:"sceneGroupID,omitempty"` // scene group, set the min sceneID as groupID
	PreID        uint64 `json:"preID,omitempty"`

	Policy PolicyType `json:"policy,omitempty"`
	IdentityInfo
}

type AutotestSceneMoveRequest struct {
	IdentityInfo
	FirstID  uint64 `json:"firstID"`  // first sceneID in the group
	LastID   uint64 `json:"lastID"`   // last sceneID in the group
	PreID    uint64 `json:"preID"`    // preID
	TargetID uint64 `json:"targetID"` // target sceneID
	IsGroup  bool   `json:"isGroup"`  // true: means move with group
	SetID    uint64 `json:"setID"`
}

type AutotestScenesRequest struct {
	AutoTestSceneParams
	SetIDs []uint64 `json:"setIds"`

	IdentityInfo
}

type AutotestSceneSceneUpdateRequest struct {
	SceneID     uint64      `json:"sceneID"`
	Name        string      `json:"name"`
	Description string      `json:"description"`
	Status      SceneStatus `json:"status"`
	SetID       uint64      `json:"setID"`
	IsStatus    bool        `json:"isStatus"` // 为true的情况下不会改变更新人
	Policy      PolicyType  `json:"policy"`
	IdentityInfo
}

type AutotestSceneInputUpdateRequest struct {
	AutotestSceneRequest
	List []AutoTestSceneInput `json:"list"`
	IdentityInfo
}
type AutotestSceneOutputUpdateRequest struct {
	AutotestSceneRequest
	List []AutoTestSceneOutput `json:"list"`
	IdentityInfo
}

type AutotestSceneCopyRequest struct {
	PreID   uint64 `json:"preID"`   // 目标前节点
	SceneID uint64 `json:"sceneID"` // 被复制场景ID
	SetID   uint64 `json:"setID"`   // 目标场景集
	SpaceID uint64 `json:"spaceID"` // 目标测试空间
	IdentityInfo
}

func (ats *AutotestSceneRequest) URLQueryString() map[string][]string {
	query := make(map[string][]string)
	if ats.ID != 0 {
		query["id"] = []string{strconv.FormatInt(int64(ats.ID), 10)}
	}
	if ats.SpaceID != 0 {
		query["spaceID"] = []string{strconv.FormatInt(int64(ats.SpaceID), 10)}
	}
	if ats.CreatorID != "" {
		query["creatorID"] = append(query["creatorID"], ats.CreatorID)
	}
	if ats.UpdaterID != "" {
		query["updaterID"] = append(query["updaterID"], ats.UpdaterID)
	}
	if ats.Name != "" {
		query["name"] = append(query["name"], ats.Name)
	}
	if ats.Description != "" {
		query["description"] = append(query["description"], ats.Description)
	}
	if ats.Value != "" {
		query["value"] = append(query["value"], ats.Value)
	}
	if ats.Temp != "" {
		query["temp"] = append(query["temp"], ats.Temp)
	}
	if ats.SceneID != 0 {
		query["sceneID"] = []string{strconv.FormatInt(int64(ats.SceneID), 10)}
	}
	if ats.SetID != 0 {
		query["setID"] = []string{strconv.FormatInt(int64(ats.SetID), 10)}
	}
	if ats.Type != "" {
		query["type"] = append(query["type"], ats.Type.String())
	}
	if ats.Target != 0 {
		query["target"] = []string{strconv.FormatInt(ats.Target, 10)}
	}
	if ats.GroupID != 0 {
		query["groupID"] = []string{strconv.FormatInt(ats.GroupID, 10)}
	}
	if ats.PreType != "" {
		query["preType"] = append(query["preType"], string(ats.PreType))
	}
	if ats.Position != 0 {
		query["position"] = []string{strconv.FormatInt(ats.Position, 10)}
	}
	if ats.IsGroup == true {
		query["isGroup"] = []string{"true"}
	}
	if ats.PageSize != 0 {
		query["pageSize"] = []string{strconv.FormatInt(int64(ats.PageSize), 10)}
	}
	if ats.PageNo != 0 {
		query["pageNo"] = []string{strconv.FormatInt(int64(ats.PageNo), 10)}
	}
	return query
}

type StepAPIType string

const (
	StepTypeWait         StepAPIType = "WAIT"
	StepTypeAPI          StepAPIType = "API"
	StepTypeScene        StepAPIType = "SCENE"
	StepTypeCustomScript StepAPIType = "CUSTOM"
	StepTypeConfigSheet  StepAPIType = "CONFIGSHEET"
	AutotestType                     = "AUTOTESTTYPE"
	AutotestSceneStep                = "STEP"
	AutotestSceneSet                 = "SCENESET"
	AutotestScene                    = "SCENE"
	AutoTestPlan                     = "TESTPLAN"
)

var EffectiveStepType = []StepAPIType{StepTypeAPI, StepTypeCustomScript, StepTypeConfigSheet}

// IsEffectiveStepType Check is effective stepType or not
func (s StepAPIType) IsEffectiveStepType() bool {
	for _, v := range EffectiveStepType {
		if s == v {
			return true
		}
	}
	return false
}

func (v StepAPIType) String() string {
	return string(v)
}

type StepAPIMethod string

var StepApiMethods = []StepAPIMethod{StepAPIMethodGet, StepAPIMethodPOST, StepAPIMethodDELETE, StepAPIMethodPUT}

const (
	StepAPIMethodGet    StepAPIMethod = "GET"
	StepAPIMethodPOST   StepAPIMethod = "POST"
	StepAPIMethodDELETE StepAPIMethod = "DELETE"
	StepAPIMethodPUT    StepAPIMethod = "PUT"
)

func (a StepAPIMethod) String() string {
	return string(a)
}

type PreType string

const (
	PreTypeSerial   PreType = "Serial"   // 串行
	PreTypeParallel PreType = "Parallel" // 并行
)

type ActiveKey string

const (
	ActiveKeyfileConfig  ActiveKey = "fileConfig"
	ActiveKeyFileExecute ActiveKey = "fileExecute"
)

func (a ActiveKey) String() string {
	return string(a)
}

type SceneStatus string

const (
	DefaultSceneStatus    SceneStatus = "default"
	ProcessingSceneStatus SceneStatus = "processing"
	SuccessSceneStatus    SceneStatus = "success"
	ErrorSceneStatus      SceneStatus = "error"
)

func (s SceneStatus) String() string {
	return string(s)
}

func (s SceneStatus) Value() string {
	switch s {
	case DefaultSceneStatus:
		return "未开始"
	case ProcessingSceneStatus:
		return "进行中"
	case SuccessSceneStatus:
		return "成功"
	case ErrorSceneStatus:
		return "失败"
	default:
		return ""
	}
}

type AutoTestSceneList struct {
	List  []AutoTestScene `json:"list"`
	Total uint64          `json:"total"`
}

type AutotestListStepOutPutRequest struct {
	IdentityInfo
	List []AutoTestSceneStep `json:"list"`
}

type AutotestCreateSceneResponse struct {
	Header
	Data uint64 `json:"data"`
}

type AutotestListSceneResponse struct {
	Header
	Data AutoTestSceneList `json:"data"`
}

type AutotestScenesModalResponse struct {
	Header
	Data map[uint64]AutoTestScene `json:"data"`
}

type AutotestGetSceneResponse struct {
	Header
	Data AutoTestScene `json:"data"`
}

type AutotestGetSceneInputResponse struct {
	Header
	Data []AutoTestSceneInput `json:"data"`
}

type AutotestGetSceneOutputResponse struct {
	Header
	Data []AutoTestSceneOutput `json:"data"`
}

type AutotestGetSceneStepResponse struct {
	Header
	Data []AutoTestSceneStep `json:"data"`
}

type AutotestGetSceneStepOutPutResponse struct {
	Header
	Data map[string]string `json:"data"`
}

type AutotestGetSceneStepReq struct {
	ID     uint64 `json:"id"`
	UserID string `json:"userId"`
}

type AutotestGetSceneStepResp struct {
	Header
	Data AutoTestSceneStep `json:"data"`
}

type AutotestExecuteSceneRequest struct {
	AutoTestScene          AutoTestScene     `json:"scene"`
	ClusterName            string            `json:"clusterName"`
	Labels                 map[string]string `json:"labels"`
	UserID                 string            `json:"userId"`
	ConfigManageNamespaces string            `json:"configManageNamespaces"`
	IdentityInfo           IdentityInfo      `json:"identityInfo"`
}

type AutotestExecuteSceneStepRequest struct {
	SceneStepID            uint64       `json:"sceneStepID"`
	UserID                 string       `json:"userId"`
	ConfigManageNamespaces string       `json:"configManageNamespaces"`
	IdentityInfo           IdentityInfo `json:"identityInfo"`
}

type AutotestExecuteSceneStepResp struct {
	Header
	Data *AutotestExecuteSceneStepRespData `json:"data"`
}

type AutotestExecuteSceneStepRespData struct {
	Info    *APIRequestInfo       `json:"requestInfo"`
	Resp    *APIResp              `json:"respInfo"`
	Asserts *APITestsAssertResult `json:"asserts"`
}

type AutotestExecuteSceneResponse struct {
	Header
	Data *PipelineDTO `json:"data"`
}

type AutotestCancelSceneRequest struct {
	AutoTestScene AutoTestScene `json:"scene"`
	UserID        string        `json:"userId"`
	IdentityInfo  IdentityInfo  `json:"identityInfo"`
}

type AutotestCancelSceneResponse struct {
	Header
	Data string `json:"data"`
}
