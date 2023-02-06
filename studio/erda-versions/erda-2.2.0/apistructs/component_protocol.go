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

// 组件化协议定义
type ComponentProtocol struct {
	Version     string                   `json:"version" yaml:"version"`
	Scenario    string                   `json:"scenario" yaml:"scenario"`
	GlobalState *GlobalStateData         `json:"state" yaml:"state"`
	Hierarchy   Hierarchy                `json:"hierarchy" yaml:"hierarchy"`
	Components  map[string]*Component    `json:"components" yaml:"components"`
	Rendering   map[string][]RendingItem `json:"rendering" yaml:"rendering"`
}

type GlobalStateData map[string]interface{}

// Hierarchy只是前端关心，只读，且有些字结构是字典有些是列表，后端不需要关心这部分
type Hierarchy struct {
	Version string `json:"version" yaml:"version"`
	Root    string `json:"root" yaml:"root"`
	// structure的结构可能是list、map
	Structure map[string]interface{} `json:"structure" yaml:"structure"`
}

type Component struct {
	Version string `json:"version,omitempty" yaml:"version,omitempty"`
	// 组件类型
	Type string `json:"type,omitempty" yaml:"type,omitempty"`
	// 组件名字
	Name string `json:"name,omitempty" yaml:"name,omitempty"`
	// table 动态字段
	Props interface{} `json:"props,omitempty" yaml:"props,omitempty"`
	// 组件业务数据
	Data ComponentData `json:"data,omitempty" yaml:"data,omitempty"`
	// 前端组件状态
	State map[string]interface{} `json:"state,omitempty" yaml:"state,omitempty"`
	// 组件相关操作（前端定义）
	Operations ComponentOps `json:"operations,omitempty" yaml:"operations,omitempty"`
}

type ComponentData map[string]interface{}

type ComponentOps map[string]interface{}

type Operation struct {
	Key      string      `json:"key"`
	Value    string      `json:"value"`
	Reload   bool        `json:"reload"`
	FillMeta string      `json:"fillMeta"`
	Command  interface{} `json:"command"`
}

type RendingItem struct {
	Name  string         `json:"name" yaml:"name"`
	State []RendingState `json:"state" yaml:"state"`
}

type RendingState struct {
	Name  string `json:"name" yaml:"name"`
	Value string `json:"value" yaml:"value"`
}

type ComponentRenderCtx ComponentProtocolRequest

// request
type ComponentProtocolRequest struct {
	Scenario ComponentProtocolScenario `json:"scenario"`
	Event    ComponentEvent            `json:"event"`
	InParams map[string]interface{}    `json:"inParams"`
	// 初次请求为空，事件出发后，把包含状态的protocol传到后端
	Protocol *ComponentProtocol `json:"protocol"`

	// DebugOptions debug 选项
	DebugOptions *ComponentProtocolDebugOptions `json:"debugOptions,omitempty"`
}

type ComponentProtocolScenario struct {
	// 场景类型, 没有则为空
	ScenarioType string `json:"scenarioType" query:"scenarioType"`
	// 场景Key
	ScenarioKey string `json:"scenarioKey" query:"scenarioKey"`
}

type ComponentEvent struct {
	Component     string                 `json:"component"`
	Operation     OperationKey           `json:"operation"`
	OperationData map[string]interface{} `json:"operationData"`
}

type OperationKey string

func (o OperationKey) String() string {
	return string(o)
}

const (
	// 协议定义的操作
	// 用户通过URL初次访问
	InitializeOperation OperationKey = "__Initialize__"
	// 用于替换掉DefaultOperation，前端触发某组件，在协议Rending中定义了关联渲染组件，传递的事件是RendingOperation
	RenderingOperation OperationKey = "__Rendering__"
	// Action
	DefaultOperation          OperationKey = "default"
	ChangeOperation           OperationKey = "change"
	ClickOperation            OperationKey = "click"
	OnSearchOperation         OperationKey = "onSearch"
	OnChangeOperation         OperationKey = "onChange"
	OnLoadDataOperation       OperationKey = "onLoadData"
	OnSubmit                  OperationKey = "submit"
	OnCancel                  OperationKey = "cancel"
	OnChangePageNoOperation   OperationKey = "changePageNo"
	OnChangePageSizeOperation OperationKey = "changePageSize"
	OnChangeSortOperation     OperationKey = "changeSort"
	// Issue
	MoveOutOperation        OperationKey = "MoveOut"
	DragOperation           OperationKey = "drag"
	MoveToOperation         OperationKey = "MoveTo"
	FilterOperation         OperationKey = "changeViewType"
	MoveToCustomOperation   OperationKey = "MoveToCustom"
	DragToCustomOperation   OperationKey = "DragToCustom"
	CreateCustomOperation   OperationKey = "CreateCustom"
	DeleteCustomOperation   OperationKey = "DeleteCustom"
	UpdateCustomOperation   OperationKey = "UpdateCustom"
	MoveToAssigneeOperation OperationKey = "MoveToAssignee"
	DragToAssigneeOperation OperationKey = "DragToAssignee"
	MoveToPriorityOperation OperationKey = "MoveToPriority"
	DragToPriorityOperation OperationKey = "DragToPriority"
	ChangePageNoOperation   OperationKey = "changePageNo"
	// filetree
	FileTreeSubmitOperationKey      OperationKey = "submit"
	FileTreeDeleteOperationKey      OperationKey = "delete"
	FileTreeAddDefaultOperationsKey OperationKey = "addDefault"
	// autotest space
	AutoTestSpaceCreateOperationKey         OperationKey = "addSpace"
	AutoTestSpaceUpdateOperationKey         OperationKey = "updateSpace"
	AutoTestSpaceDeleteOperationKey         OperationKey = "delete"
	AutoTestSpaceCopyOperationKey           OperationKey = "copy"
	AutoTestSpaceRetryOperationKey          OperationKey = "retry"
	AutoTestSpaceExportOperationKey         OperationKey = "export"
	AutoTestSpaceChangePageNoOperationKey   OperationKey = "changePageNo"
	AutoTestSpaceChangePageSizeOperationKey OperationKey = "changePageSize"
	AutoTestSpaceSubmitOperationKey         OperationKey = "submit"
	AutoTestSpaceClickRowOperationKey       OperationKey = "clickRow"
	// autotest scene
	AutoTestSceneListOperationKey OperationKey = "ListScene"
	// autotest scene input
	AutoTestSceneInputUpdateOperationKey   OperationKey = "save"
	AutoTestSceneInputOnSelectOperationKey OperationKey = "onSelectOption"
	// autotest scene output
	AutoTestSceneOutputUpdateOperationKey OperationKey = "save"
	// autotest scene step
	AutoTestSceneStepCreateOperationKey     OperationKey = "addParallelAPI"
	AutoTestSceneStepCopyOperationKey       OperationKey = "copyParallelAPI"
	AutoTestSceneStepMoveItemOperationKey   OperationKey = "moveItem"
	AutoTestSceneStepMoveGroupOperationKey  OperationKey = "moveGroup"
	AutoTestSceneStepDeleteOperationKey     OperationKey = "deleteAPI"
	AutoTestSceneStepSplitOperationKey      OperationKey = "standalone"
	AutoTestSceneStepSwitchOperationKey     OperationKey = "switch"
	AutoTestSceneStepCopyAsJsonOperationKey OperationKey = "copyAsJson"

	//auto-test scene set
	ListSceneSetOperationKey          OperationKey = "ListSceneSet"
	UpdateSceneSetOperationKey        OperationKey = "UpdateSceneSet"
	ClickSceneSetOperationKey         OperationKey = "ClickSceneSet"
	ClickSceneOperationKey            OperationKey = "ClickScene"
	ExpandSceneSetOperationKey        OperationKey = "ExpandSceneSet"
	AddSceneOperationKey              OperationKey = "AddScene"
	RefSceneSetOperationKey           OperationKey = "RefSceneSet"
	SubmitSceneOperationKey           OperationKey = "SubmitScene"
	UpdateSceneOperationKey           OperationKey = "UpdateScene"
	DeleteSceneOperationKey           OperationKey = "DeleteScene"
	DeleteSceneSetOperationKey        OperationKey = "DeleteSceneSet"
	ExportSceneSetOperationKey        OperationKey = "exportSceneSet"
	ClickAddSceneSeButtonOperationKey OperationKey = "ClickAddSceneSet"
	DragSceneSetOperationKey          OperationKey = "DragSceneSet"
	CopySceneOperationKey             OperationKey = "CopyScene"

	// auto-test-plan-stage
	AutoTestPlanStageDeleteOperationKey OperationKey = "delete"

	// autotest folderDetail
	AutoTestFolderDetailDeleteOperationKey OperationKey = "delete"
	AutoTestFolderDetailCopyOperationKey   OperationKey = "copy"
	AutoTestFolderDetailEditOperationKey   OperationKey = "edit"
	AutoTestFolderDetailPageOperationKey   OperationKey = "changePageNo"
	AutoTestFolderDetailClickOperationKey  OperationKey = "clickRow"

	// auto-test scene execute
	ExecuteChangePageNoOperationKey OperationKey = "changePageNo"
	ExecuteClickRowNoOperationKey   OperationKey = "clickRow"
	ExecuteAddApiOperationKey       OperationKey = "addApi"
	ExecuteTaskBreadcrumbSelectItem OperationKey = "selectItem"
	ExecuteSubmitCopyOperationKey   OperationKey = "submitCopy"

	//org-list
	FilterOrgsOperationKey         OperationKey = "filter"
	ChangeOrgsPageNoOperationKey   OperationKey = "changePageNo"
	ChangeOrgsPageSizeOperationKey OperationKey = "changePageSize"
	ExitOrgOperationKey            OperationKey = "exit"
	RedirectPublicOperationKey     OperationKey = "toPublicOrg"

	// list-project
	ListProjectToManageOperationKey   OperationKey = "toManage"
	ListProjectExistOperationKey      OperationKey = "exist"
	ListProjectFilterOperation        OperationKey = "filter"
	ApplyDeployProjectFilterOperation OperationKey = "applyDeploy"

	//notify_list
	NotifySubmit OperationKey = "submit"
	NotifyDelete OperationKey = "delete"
	NotifySwitch OperationKey = "switch"
	NotifyEdit   OperationKey = "edit"

	// workbench
	SubmitOrgOperationKey OperationKey = "submitOrg"

	// issue gantt
	Update     OperationKey = "update"
	ExpandNode OperationKey = "expandNode"
)

type ComponentProtocolParams interface{}

// response
type ComponentProtocolResponse struct {
	Header
	Data     ComponentProtocolResponseData `json:"data"`
	UserIDs  []string                      `json:"userIDs"`
	UserInfo map[string]UserInfo           `json:"userInfo"`
}

type ComponentProtocolResponseData struct {
	Scenario ComponentProtocolScenario `json:"scenario"`
	// 后端渲染后的protocol返回前端
	Protocol ComponentProtocol `json:"protocol"`
}

type ComponentProtocolDebugOptions struct {
	ComponentKey string `json:"componentKey"`
}
