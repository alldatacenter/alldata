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
	"encoding/base64"
	"encoding/json"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/dop/projectpipeline/pb"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	DefaultPipelineYmlName        = "pipeline.yml"
	DefaultPipelinePath    string = ""
	DicePipelinePath       string = ".dice/pipelines"
	ErdaPipelinePath       string = ".erda/pipelines"
	YmlSuffix              string = ".yml"
	YamlSuffix             string = ".yaml"

	//用作PipelinePageListRequest order by 的表字段名称
	PipelinePageListRequestIdColumn = "id"
)

const (
	BuildkitSecretMountName = "cert"
	BuildkitSecretMountPath = "/.buildkit"
	BuildkitClientSecret    = "buildkit-client-certs"

	BuildkitEnable  = "BUILDKIT_ENABLE" // will force true when ECIEnable is true
	BuildkitHitRate = "BUILDKIT_HIT_RATE"
)

const (
	EnvDiceOrgName    = "DICE_ORG_NAME"
	EnvDiceOrgID      = "DICE_ORG_ID"
	EnvDiceWorkspace  = "DICE_WORKSPACE"
	EnvIsEdgePipeline = "IS_EDGE_PIPELINE"
	EnvPipelineAddr   = "PIPELINE_ADDR"
)

// pipeline reconcileTask context key
const ClusterNameContextKey = "cluster_name"

// pipeline create

type PipelineCreateRequest struct {
	AppID              uint64            `json:"appID"`
	Branch             string            `json:"branch"`
	Source             PipelineSource    `json:"source"`
	PipelineYmlSource  PipelineYmlSource `json:"pipelineYmlSource"`
	PipelineYmlName    string            `json:"pipelineYmlName"` // 与 pipelineYmlContent 匹配，如果为空，则为 pipeline.yml
	PipelineYmlContent string            `json:"pipelineYmlContent"`
	AutoRun            bool              `json:"autoRun"`
	CallbackURLs       []string          `json:"callbackURLs"`

	UserID          string `json:"userID"`
	IsCronTriggered bool   `json:"isCronTriggered"`
}

type PipelineRunParam struct {
	Name  string      `json:"name"`
	Value interface{} `json:"value"`
}

type PipelineRunParamWithValue struct {
	PipelineRunParam             // 从 pipeline.yml 中解析出来的值
	TrueValue        interface{} `json:"trueValue,omitempty"` // 真正的值，如果是占位符则会被替换，否则为原值
}
type PipelineRunParamsWithValue []PipelineRunParamWithValue

func (rps PipelineRunParamsWithValue) ToPipelineRunParams() PipelineRunParams {
	var result PipelineRunParams
	for _, rp := range rps {
		result = append(result, PipelineRunParam{Name: rp.Name, Value: rp.Value})
	}
	return result
}

// PipelineCreateRequestV2 used to create pipeline via pipeline V2 API.
type PipelineCreateRequestV2 struct {
	// PipelineYml is pipeline yaml content.
	// +required
	PipelineYml string `json:"pipelineYml"`

	// ClusterName represents the cluster the pipeline will be executed.
	// +required
	ClusterName string `json:"clusterName"`

	// Namespace upper layer could customize pipeline running in which namespace
	// +optional
	Namespace string `json:"namespace"`

	// PipelineYmlName
	// Equal to `Name`.
	// Default is `pipeline.yml`.
	// +optional
	PipelineYmlName string `json:"pipelineYmlName"`

	// PipelineSource represents the source where pipeline created from.
	// Equal to `Namespace`.
	// +required
	PipelineSource PipelineSource `json:"pipelineSource"`

	// Labels is Map of string keys and values, can be used to filter pipeline.
	// If label key or value is too long, it will be moved to NormalLabels automatically and overwrite value if key already exists in NormalLabels.
	// +optional
	Labels map[string]string `json:"labels"`

	// NormalLabels is Map of string keys and values, cannot be used to filter pipeline.
	// +optional
	NormalLabels map[string]string `json:"normalLabels"`

	// Envs is Map of string keys and values.
	// +optional
	Envs map[string]string `json:"envs"`

	// ConfigManageNamespaces pipeline fetch configs from cms by namespaces in order.
	// Pipeline won't generate default ns.
	// +optional
	ConfigManageNamespaces []string `json:"configManageNamespaces"`

	// AutoRun represents whether auto run the created pipeline.
	// Default is false.
	// +optional
	// Deprecated, please use `AutoRunAtOnce` or `AutoStartCron`.
	// Alias for AutoRunAtOnce.
	AutoRun bool `json:"autoRun"`

	// ForceRun represents stop other running pipelines to run.
	// Default is false.
	// +optional
	ForceRun bool `json:"forceRun"`

	// AutoRunAtOnce alias for `AutoRun`.
	// AutoRunAtOnce represents whether auto run the created pipeline.
	// Default is false.
	// +optional
	AutoRunAtOnce bool `json:"autoRunAtOnce"`

	// AutoStartCron represents whether auto start cron.
	// If a pipeline doesn't have `cron` field, ignore.
	// Default is false.
	// +optional
	AutoStartCron bool `json:"autoStartCron"`

	// CronStartFrom specify time when to start
	// +optional
	CronStartFrom *time.Time `json:"cronStartFrom"`

	// GC represents pipeline gc configs.
	// If config is empty, will use default config.
	// +optional
	GC PipelineGC `json:"gc,omitempty"`

	// RunPipelineParams represents pipeline params runtime input
	// if pipeline have params runPipelineParams can not be empty
	// +optional
	RunParams PipelineRunParams `json:"runParams"`

	// Internal-Use below

	// BindQueue represents the queue pipeline binds, internal use only, parsed from Labels: LabelBindPipelineQueueID
	BindQueue *PipelineQueue `json:"-"`

	// DefinitionID pipeline definition id
	// +optional
	DefinitionID string `json:"definitionID"`

	// passed from the invoker, different from config cms
	// eg: gittar.repo
	// +optional
	Secrets map[string]string `json:"secrets"`

	IdentityInfo
}

type PipelineRunParams []PipelineRunParam

func (rps PipelineRunParams) ToPipelineRunParamsWithValue() []PipelineRunParamWithValue {
	var result []PipelineRunParamWithValue
	for _, rp := range rps {
		result = append(result, PipelineRunParamWithValue{PipelineRunParam: PipelineRunParam{Name: rp.Name, Value: rp.Value}})
	}
	return result
}

// PipelineGC
type PipelineGC struct {
	ResourceGC PipelineResourceGC `json:"resourceGC,omitempty"`
	DatabaseGC PipelineDatabaseGC `json:"databaseGC,omitempty"`
}

// PipelineResourceGC releases occupied resource by pipeline, such as:
// - k8s pv (netdata volume)
// - k8s pod
// - k8s namespace
type PipelineResourceGC struct {
	// SuccessTTLSecond means when to release resource if pipeline status is Success.
	// Normally success ttl should be small even to zero, because everything is ok and don't need to rerun.
	// Default is 1800s(30min)
	SuccessTTLSecond *uint64 `json:"successTTLSecond,omitempty"`
	// FailedTTLSecond means when to release resource if pipeline status is Failed.
	// Normally failed ttl should larger than SuccessTTLSecond, because you may want to rerun this failed pipeline,
	// which need these resource.
	// Default is 1800s.
	FailedTTLSecond *uint64 `json:"failedTTLSecond,omitempty"`
}

// PipelineDatabaseGC represents database record gc strategy.
type PipelineDatabaseGC struct {
	// Analyzed contains gc strategy to analyzed pipeline.
	Analyzed PipelineDBGCItem `json:"analyzed,omitempty"`
	// Finished contains gc strategy to finished(success/failed) pipeline.
	Finished PipelineDBGCItem `json:"finished,omitempty"`
}

// PipelineDBGCItem archives or deletes database record to ease db pressure.
type PipelineDBGCItem struct {
	// NeedArchive means whether this record need be archived:
	// If true, archive record to specific archive table;
	// If false, delete record and cannot be found anymore.
	NeedArchive *bool `json:"needArchive,omitempty"`
	// TTLSecond means when to do archive or delete operation.
	TTLSecond *uint64 `json:"ttlSecond,omitempty"`
}

type PipelineCreateResponse struct {
	Header
	Data *PipelineDTO `json:"data"`
}

// pipeline batch create

type PipelineBatchCreateRequest struct {
	AppID                 uint64         `json:"appID"`
	Branch                string         `json:"branch"`
	Source                PipelineSource `json:"source"`
	BatchPipelineYmlPaths []string       `json:"batchPipelineYmlPaths"`
	AutoRun               bool           `json:"autoRun"`
	CallbackURLs          []string       `json:"callbackURLs"`

	UserID string `json:"userID"`
}

type PipelineBatchCreateResponse struct {
	Header
	Data map[string]PipelineDTO `json:"data"`
}

// pipeline detail
type PipelineDetailRequest struct {
	SimplePipelineBaseResult bool   `json:"simplePipelineBaseResult"`
	PipelineID               uint64 `json:"pipelineID"`
}

// pipeline detail
type PipelineDetailResponse struct {
	Header
	Data *PipelineDetailDTO `json:"data"`
}

// pipeline page list
type PipelinePageListRequest struct {
	// Deprecated, use schema `branch`
	CommaBranches string `schema:"branches"`
	// Deprecated, use schema `source`
	CommaSources string `schema:"sources"`
	// Deprecated, use schema `ymlName`
	CommaYmlNames string `schema:"ymlNames"`
	// Deprecated, use schema `status`
	CommaStatuses string `schema:"statuses"`

	// Deprecated, use mustMatchLabels, key=appID
	AppID uint64 `schema:"appID"`
	// Deprecated, use mustMatchLabels, key=branch
	Branches []string `schema:"branch"`

	Sources      []PipelineSource      `schema:"source"`
	AllSources   bool                  `schema:"allSources"`
	YmlNames     []string              `schema:"ymlName"`
	Statuses     []string              `schema:"status"`
	NotStatuses  []string              `schema:"notStatus"`
	TriggerModes []PipelineTriggerMode `schema:"triggerMode"`
	ClusterNames []string              `schema:"clusterName"`

	// IncludeSnippet 是否展示嵌套流水线，默认不展示。
	// 嵌套流水线一般来说只需要在详情中展示即可。
	IncludeSnippet bool `schema:"includeSnippet"`

	// time

	// 开始执行时间 左闭区间
	StartTimeBegin time.Time `schema:"-"`
	// http GET query param 请赋值该字段
	StartTimeBeginTimestamp int64 `schema:"startTimeBeginTimestamp"`
	// Deprecated, use `StartedAtTimestamp`.
	// format: 2006-01-02T15:04:05, TZ: CST
	StartTimeBeginCST string `schema:"startedAt"`

	// 开始执行时间 右闭区间
	EndTimeBegin time.Time `schema:"-"`
	// http GET query param 请赋值该字段
	EndTimeBeginTimestamp int64 `schema:"endTimeBeginTimestamp"`
	// Deprecated, use `StartedAtTimestamp`.
	// format: 2006-01-02T15:04:05, TZ: CST
	EndTimeBeginCST string `schema:"endedAt"`

	// 创建时间 左闭区间
	StartTimeCreated time.Time `schema:"-"`
	// http GET query param 请赋值该字段
	StartTimeCreatedTimestamp int64 `schema:"startTimeCreatedTimestamp"`

	// 创建时间 右闭区间
	EndTimeCreated time.Time `schema:"-"`
	// http GET query param 请赋值该字段
	EndTimeCreatedTimestamp int64 `schema:"endTimeCreatedTimestamp"`

	// labels

	// Deprecated
	// 供 CDP 工作流明细查询使用，JSON(map[string]string)
	MustMatchLabelsJSON string `schema:"mustMatchLabels"`
	// ?mustMatchLabel=key1=value1
	// &mustMatchLabel=key1=value2
	// &mustMatchLabel=key2=value3
	MustMatchLabelsQueryParams []string `schema:"mustMatchLabel"`
	// 直接构造对象 请赋值该字段
	MustMatchLabels map[string][]string `schema:"-"`

	// Deprecated
	// 供 CDP 工作流明细查询使用，JSON(map[string]string)
	AnyMatchLabelsJSON string `schema:"anyMatchLabels"`
	// ?anyMatchLabel=key1=value1
	// &anyMatchLabel=key1=value2
	// &anyMatchLabel=key2=value3
	AnyMatchLabelsQueryParams []string `schema:"anyMatchLabel"`
	// 直接构造对象 请赋值该字段
	AnyMatchLabels map[string][]string `schema:"-"`

	PageNum       int  `schema:"pageNum"` // still use this field to handle page number
	PageNo        int  `schema:"pageNo"`  // UI standard compatible, same with PageNum (transfer to pageNum)
	PageSize      int  `schema:"pageSize"`
	LargePageSize bool `schema:"largePageSize"` // 允许 pageSize 超过默认值(100)，由内部调用方保证数据量大小

	CountOnly bool `schema:"countOnly"` // 是否只获取 total

	// internal use
	SelectCols []string `schema:"-" ` // 需要赋值的字段列表，若不声明，则全赋值
	AscCols    []string `schema:"ascCol"`
	DescCols   []string `schema:"descCol"`

	// pipeline definition search
	PipelineDefinitionRequest           *PipelineDefinitionRequest
	PipelineDefinitionRequestJSONBase64 string `schema:"pipelineDefinition"`
}

type PipelineDefinitionRequest struct {
	Name          string   `json:"name"`
	Creators      []string `json:"creators"`
	SourceRemotes []string `json:"sourceRemotes"`
	Location      string   `json:"location"`
}

func (definition *PipelineDefinitionRequest) IsEmptyValue() bool {
	if definition == nil {
		return true
	}
	if len(definition.Name) > 0 {
		return false
	}
	if len(definition.Creators) > 0 {
		return false
	}
	if len(definition.SourceRemotes) > 0 {
		return false
	}
	if definition.Location != "" {
		return false
	}
	return true
}

type PipelineSourceRequest struct {
	Remote     string `json:"remote"`
	Ref        string `json:"ref"`
	Path       string `json:"path"`
	Name       string `json:"name"`
	SourceType string `json:"sourceType"`
}

func (req *PipelinePageListRequest) PostHandleQueryString() error {
	// comma
	const comma = ","
	req.Branches = append(req.Branches, strutil.Split(req.CommaBranches, comma, true)...)
	for _, source := range strutil.Split(req.CommaSources, comma, true) {
		req.Sources = append(req.Sources, PipelineSource(source))
	}
	req.Statuses = append(req.Statuses, strutil.Split(req.CommaStatuses, comma, true)...)
	req.YmlNames = append(req.YmlNames, strutil.Split(req.CommaYmlNames, comma, true)...)

	// labels
	if req.MustMatchLabels == nil {
		req.MustMatchLabels = make(map[string][]string)
	}
	if req.MustMatchLabelsJSON != "" {
		mustMatchLabels := make(map[string]string)
		if err := json.Unmarshal([]byte(req.MustMatchLabelsJSON), &mustMatchLabels); err != nil {
			return err
		}
		for k, v := range mustMatchLabels {
			req.MustMatchLabels[k] = strutil.DedupSlice(append(req.MustMatchLabels[k], v))
		}
	}
	for _, param := range req.MustMatchLabelsQueryParams {
		kv := strings.SplitN(param, "=", 2)
		if len(kv) != 2 {
			return errors.Errorf("invalid mustMatchLabel: %s", param)
		}
		req.MustMatchLabels[kv[0]] = strutil.DedupSlice(append(req.MustMatchLabels[kv[0]], kv[1]))
	}

	if req.AnyMatchLabels == nil {
		req.AnyMatchLabels = make(map[string][]string)
	}
	if req.AnyMatchLabelsJSON != "" {
		anyMatchLabels := make(map[string]string)
		if err := json.Unmarshal([]byte(req.AnyMatchLabelsJSON), &anyMatchLabels); err != nil {
			return err
		}
		for k, v := range anyMatchLabels {
			req.AnyMatchLabels[k] = strutil.DedupSlice(append(req.AnyMatchLabels[k], v))
		}
	}
	for _, param := range req.AnyMatchLabelsQueryParams {
		kv := strings.SplitN(param, "=", 2)
		if len(kv) != 2 {
			return errors.Errorf("invalid anyMatchLabel: %s", param)
		}
		req.AnyMatchLabels[kv[0]] = strutil.DedupSlice(append(req.AnyMatchLabels[kv[0]], kv[1]))
	}

	// time
	// 历史遗留问题，cdp 对接时时区使用了 CST
	l, _ := time.LoadLocation("Asia/Shanghai")

	if req.StartTimeBeginTimestamp > 0 {
		req.StartTimeBegin = time.Unix(req.StartTimeBeginTimestamp, 0)
	} else if req.StartTimeBeginCST != "" {
		startedAtCST, _ := time.ParseInLocation("2006-01-02T15:04:05", req.StartTimeBeginCST, l)
		if !startedAtCST.IsZero() {
			req.StartTimeBegin = time.Unix(startedAtCST.Unix(), 0)
		}
	}
	if req.EndTimeBeginTimestamp > 0 {
		req.EndTimeBegin = time.Unix(req.EndTimeBeginTimestamp, 0)
	} else if req.EndTimeBeginCST != "" {
		endedAtCST, _ := time.ParseInLocation("2006-01-02T15:04:05", req.EndTimeBeginCST, l)
		if !endedAtCST.IsZero() {
			req.EndTimeBegin = time.Unix(endedAtCST.Unix(), 0)
		}
	}

	if req.StartTimeCreatedTimestamp > 0 {
		req.StartTimeCreated = time.Unix(req.StartTimeCreatedTimestamp, 0)
	}
	if req.EndTimeCreatedTimestamp > 0 {
		req.EndTimeCreated = time.Unix(req.EndTimeCreatedTimestamp, 0)
	}
	if req.PipelineDefinitionRequestJSONBase64 != "" {
		var pipelineDefinitionRequest = PipelineDefinitionRequest{}
		value, err := base64.StdEncoding.DecodeString(req.PipelineDefinitionRequestJSONBase64)
		if err != nil {
			return errors.Errorf("invalid PipelineDefinitionRequestJSONBase64: %s", req.PipelineDefinitionRequestJSONBase64)
		}
		err = json.Unmarshal(value, &pipelineDefinitionRequest)
		if err != nil {
			return errors.Errorf("invalid PipelineDefinitionRequestJSONBase64: %s", req.PipelineDefinitionRequestJSONBase64)
		}
		req.PipelineDefinitionRequest = &pipelineDefinitionRequest
	}

	return nil
}

// UrlQueryString 不兼容 deprecated 字段
func (req *PipelinePageListRequest) UrlQueryString() map[string][]string {

	query := make(map[string][]string)

	for _, source := range req.Sources {
		query["source"] = append(query["source"], source.String())
	}
	query["allSources"] = []string{strconv.FormatBool(req.AllSources)}
	query["ymlName"] = append(query["ymlName"], req.YmlNames...)
	query["status"] = append(query["status"], req.Statuses...)
	query["notStatus"] = append(query["notStatus"], req.NotStatuses...)
	for _, mode := range req.TriggerModes {
		query["triggerMode"] = append(query["triggerMode"], mode.String())
	}
	query["clusterName"] = append(query["clusterName"], req.ClusterNames...)
	if req.StartTimeBeginTimestamp > 0 {
		query["startTimeBeginTimestamp"] = []string{strconv.FormatInt(req.StartTimeBeginTimestamp, 10)}
	}
	if req.EndTimeBeginTimestamp > 0 {
		query["endTimeBeginTimestamp"] = []string{strconv.FormatInt(req.EndTimeBeginTimestamp, 10)}
	}
	if req.StartTimeCreatedTimestamp > 0 {
		query["startTimeCreatedTimestamp"] = []string{strconv.FormatInt(req.StartTimeCreatedTimestamp, 10)}
	}
	if req.EndTimeCreatedTimestamp > 0 {
		query["endTimeCreatedTimestamp"] = []string{strconv.FormatInt(req.EndTimeCreatedTimestamp, 10)}
	}
	query["selectCol"] = append(query["selectCol"], req.SelectCols...)
	query["ascCol"] = append(query["ascCol"], req.AscCols...)
	query["descCol"] = append(query["descCol"], req.DescCols...)
	query["countOnly"] = []string{strconv.FormatBool(req.CountOnly)}
	query["mustMatchLabels"] = []string{req.MustMatchLabelsJSON}
	query["mustMatchLabel"] = req.MustMatchLabelsQueryParams
	query["anyMatchLabels"] = []string{req.AnyMatchLabelsJSON}
	query["anyMatchLabel"] = req.AnyMatchLabelsQueryParams
	query["pageNum"] = []string{strconv.FormatInt(int64(req.PageNum), 10)}
	query["pageSize"] = []string{strconv.FormatInt(int64(req.PageSize), 10)}
	query["largePageSize"] = []string{strconv.FormatBool(req.LargePageSize)}
	query["countOnly"] = []string{strconv.FormatBool(req.CountOnly)}
	if req.PipelineDefinitionRequestJSONBase64 != "" {
		query["pipelineDefinition"] = []string{req.PipelineDefinitionRequestJSONBase64}
	}
	return query
}

type PipelinePageListResponse struct {
	Header
	Data *PipelinePageListData `json:"data"`
}

type PipelinePageListData struct {
	Pipelines       []PagePipeline `json:"pipelines,omitempty"`
	Total           int64          `json:"total"`
	CurrentPageSize int64          `json:"currentPageSize"`
}

// pipeline run

type PipelineRunRequest struct {
	PipelineID             uint64            `json:"pipelineID"`
	ForceRun               bool              `json:"forceRun"`
	PipelineRunParams      PipelineRunParams `json:"runParams"`
	ConfigManageNamespaces []string          `json:"configManageNamespaces"`
	Secrets                map[string]string `json:"secrets"`
	IdentityInfo
}

type PipelineRunResponse struct {
	Header
}

// pipeline cancel
type PipelineCancelRequest struct {
	PipelineID uint64 `json:"pipelineID"`
	IdentityInfo
}

type PipelineCancelResponse struct {
	Header
}

// pipeline rerun
type PipelineRerunRequest struct {
	PipelineID    uint64            `json:"pipelineID"`
	AutoRunAtOnce bool              `json:"autoRunAtOnce"`
	Secrets       map[string]string `json:"secrets"`
	IdentityInfo
}

type PipelineRerunResponse struct {
	Header
	Data *PipelineDTO `json:"data"`
}

// pipeline rerun failed

type PipelineRerunFailedRequest struct {
	PipelineID    uint64            `json:"pipelineID"`
	AutoRunAtOnce bool              `json:"autoRunAtOnce"`
	Secrets       map[string]string `json:"secrets"`
	IdentityInfo
}

type PipelineRerunFailedResponse struct {
	Header
	Data *PipelineDTO `json:"data"`
}

// cron

type PipelineCronListResponse struct {
	Header
	Data []PipelineCronDTO `json:"data"`
}

// pipeline operate

type PipelineGetBranchRuleResponse struct {
	Header
	Data *ValidBranch `json:"data"`
}

type PipelineOperateRequest struct {
	TaskOperates []PipelineTaskOperateRequest `json:"taskOperates,omitempty"`
}

type PipelineTaskOperateRequest struct {
	TaskID    uint64 `json:"taskID"`
	TaskAlias string `json:"taskAlias"` // the task will not be created until the pipeline is executed, so you must use alias to disable the task later
	Disable   *bool  `json:"disable,omitempty"`
	Pause     *bool  `json:"pause,omitempty"`
}

type PipelineOperateResponse struct {
	Header
}

type PipelineConfigNamespacesFetchResponse struct {
	Header
	Data *PipelineConfigNamespaceResponseData `json:"data"`
}

type PipelineConfigNamespaceResponseData struct {
	Namespaces []PipelineConfigNamespaceItem `json:"namespaces"`
}

type PipelineConfigNamespaceItem struct {
	ID          string `json:"id"` // default | branchPrefix
	Namespace   string `json:"namespace"`
	Workspace   string `json:"workspace"`
	Branch      string `json:"branch"`
	IsOldConfig bool   `json:"isOldConfig"`
}

type PipelineAppInvokedBranchesResponse struct {
	Header
	Data []string `json:"data"`
}

type ValidBranch struct {
	Name              string `json:"name"`
	IsProtect         bool   `json:"isProtect"`
	NeedApproval      bool   `json:"needApproval"`
	IsTriggerPipeline bool   `json:"isTriggerPipeline"`
	// 通过分支创建的流水线环境
	Workspace string `json:"workspace"`
	// 制品可部署的环境
	ArtifactWorkspace string `json:"artifactWorkspace"`
}

func (branch *ValidBranch) GetPermissionResource() string {
	resource := "normalBranch"
	if branch.IsProtect {
		resource = "protectedBranch"
	}
	return resource
}

type PipelineAppAllValidBranchWorkspaceResponse struct {
	Header
	Data []ValidBranch `json:"data"`
}

type PipelineCallbackRequest struct {
	Type string `json:"type"`
	Data []byte `json:"data"`
}

type PipelineCallbackResponse struct {
	Header
}

type PipelineCallbackType string

var (
	PipelineCallbackTypeOfAction             PipelineCallbackType = "ACTION"
	PipelineCallbackTypeOfEdgeTaskReport     PipelineCallbackType = "EDGE_TASK_REPORT"
	PipelineCallbackTypeOfEdgePipelineReport PipelineCallbackType = "EDGE_PIPELINE_REPORT"
	PipelineCallbackTypeOfEdgeCronReport     PipelineCallbackType = "EDGE_CRON_REPORT"
)

func (p PipelineCallbackType) String() string {
	return string(p)
}

// pipeline invoked combo 用于流水线侧边栏聚合，每个 combo 是侧边栏一条记录
// 大数据：combo(branch: master + source: bigdata  + 文件名不限)
// 流水线：combo(branch: 不限   + source: 不限     + 文件名不限)

type PipelineInvokedComboRequest struct {
	// app id
	AppID uint64 `query:"appID"`

	// comma-separated value, such as: develop,master
	Branches string `query:"branches"`

	// comma-separated value, such as: dice,bigdata
	Sources string `query:"sources"`

	// comma-separated value, such as: pipeline.yml,path1/path2/demo.workflow
	YmlNames string `query:"ymlNames"`
}

type PipelineInvokedComboResponse struct {
	Header
	Data []PipelineInvokedCombo `json:"data"`
}

type PipelineInvokedCombo struct {
	Branch         string   `json:"branch"`
	Source         string   `json:"source"`
	YmlName        string   `json:"ymlName"`
	PagingYmlNames []string `json:"pagingYmlNames"` // 拿到 combo 后，调用分页接口时，ymlNames 可指定多个

	// 其他前端展示需要的字段
	PipelineID  uint64        `json:"pipelineID"`
	Commit      string        `json:"commit"`
	Status      string        `json:"status"`
	TimeCreated *time.Time    `json:"timeCreated"`
	CancelUser  *PipelineUser `json:"cancelUser,omitempty"` // TODO 需要前端重构后，再只返回 UserID
	TriggerMode string        `json:"triggerMode"`
	Workspace   string        `json:"workspace"`
}

// PipelineStatisticRequest pipeline 执行统计请求
type PipelineStatisticRequest struct {
	Source PipelineSource `query:"source"`
}

// PipelineStatisticResponse pipeline 执行统计响应
type PipelineStatisticResponse struct {
	Header
	Data PipelineStatisticResponseData `json:"data"`
}

// PipelineStatisticResponseData pipeline 执行统计
type PipelineStatisticResponseData struct {
	Success    uint64 `json:"success"`
	Processing uint64 `json:"processing"`
	Failed     uint64 `json:"failed"`
	Completed  uint64 `json:"completed"` // success + failed
}

type PipelineDeleteResponse struct {
	Header
}

type PipelineDefinitionExtraValue struct {
	CreateRequest *PipelineCreateRequestV2 `json:"createRequest"`
	RunParams     []*pb.PipelineRunParam   `json:"runParams"`
}

type EdgeReportStatus string

const (
	InitEdgeReportStatus       EdgeReportStatus = "init"
	ProcessingEdgeReportStatus EdgeReportStatus = "processing"
	DoneEdgeReportStatus       EdgeReportStatus = "done"
)

func (e EdgeReportStatus) String() string {
	return string(e)
}
