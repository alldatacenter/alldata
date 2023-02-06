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
	"sort"

	"github.com/erda-project/erda/pkg/encoding/jsonparse"
)

const (
	ActionSourceType = "action"

	ActionTypeAPITest      = "api-test"
	ActionTypeSnippet      = "snippet"
	ActionTypeCustomScript = "custom-script"
	ActionTypeWait         = "wait"

	SnippetSourceLocal = "local"
)

const (
	PipelineParamStringType = "string"
	PipelineParamIntType    = "int"
	PipelineParamBoolType   = "boolean"
)

type PipelineYml struct {
	// 用于构造 pipeline yml
	Version         string                 `json:"version"` // 版本
	Name            string                 `json:"name"`
	Envs            map[string]string      `json:"envs,omitempty"`                                             // 环境变量
	Cron            string                 `json:"cron,omitempty"`                                             // 定时配置
	CronCompensator *CronCompensator       `json:"cronCompensator,omitempty" yaml:"cronCompensator,omitempty"` // 定时补偿配置
	Stages          [][]*PipelineYmlAction `json:"stages"`                                                     // 流水线
	FlatActions     []*PipelineYmlAction   `json:"flatActions"`                                                // 展平了的流水线

	Params []*PipelineParam `json:"params,omitempty"` // 流水线输入

	Outputs []*PipelineOutput `json:"outputs,omitempty"` // 流水线输出

	// --- 以下字段与构造 pipeline yml 无关 ---

	// 1.0 升级相关
	NeedUpgrade bool `json:"needUpgrade"` // pipeline yml 是否需要升级

	// YmlContent:
	// 1) 当 needUpgrade 为 true  时，ymlContent 返回升级后的 yml
	// 2) 当 needUpgrade 为 false 时：
	//    1) 用户传入的为 YAML(apistructs.PipelineYml) 时，ymlContent 返回 YAML(spec.PipelineYml)
	//    2) 用户传入的为 YAML(spec.PipelineYml) 时，返回优化后的 YAML(spec.PipelineYml)
	YmlContent string             `json:"ymlContent,omitempty"`
	On         *TriggerConfig     `json:"on,omitempty"`
	Triggers   []*PipelineTrigger `json:"triggers,omitempty"`
	// describe the use of network hooks in the pipeline
	Lifecycle []*NetworkHookInfo `json:"lifecycle"`
}

type NetworkHookInfo struct {
	Hook   string                 `json:"hook"`   // hook type
	Client string                 `json:"client"` // use network client
	Labels map[string]interface{} `json:"labels"` // additional information
}

type TriggerConfig struct {
	Push  *PushTrigger  `yaml:"push,omitempty" json:"push,omitempty"`
	Merge *MergeTrigger `yaml:"merge,omitempty" json:"merge,omitempty"`
}

type PushTrigger struct {
	Branches []string `yaml:"branches,omitempty" json:"branches,omitempty"`
	Tags     []string `yaml:"tags,omitempty" json:"tags,omitempty"`
}

type MergeTrigger struct {
	Branches []string `yaml:"branches,omitempty" json:"branches,omitempty"`
}

type PipelineYmlAction struct {
	Alias         string                 `json:"alias,omitempty"`                                          // action 实例名
	Type          string                 `json:"type"`                                                     // action 类型，比如：git-checkout, release
	Description   string                 `json:"description,omitempty"`                                    // 描述
	Version       string                 `json:"version,omitempty"`                                        // action 版本
	Params        map[string]interface{} `json:"params,omitempty"`                                         // 参数
	Image         string                 `json:"image,omitempty"`                                          // 镜像
	Commands      []string               `json:"commands,omitempty"`                                       // 命令行
	Timeout       int64                  `json:"timeout,omitempty"`                                        // 超时设置，单位：秒
	Namespaces    []string               `json:"namespaces,omitempty"`                                     // Action 输出的命名空间
	Resources     Resources              `json:"resources,omitempty"`                                      // 资源
	DisplayName   string                 `json:"displayName,omitempty"`                                    // 中文名称
	LogoUrl       string                 `json:"logoUrl,omitempty"`                                        // logo
	Caches        []ActionCache          `json:"caches,omitempty"`                                         // 缓存
	SnippetConfig *SnippetConfig         `json:"snippet_config,omitempty" yaml:"snippet_config,omitempty"` // snippet 的配置
	If            string                 `json:"if,omitempty"`                                             // 条件执行
	Disable       bool                   `json:"disable,omitempty"`                                        // task is disable or enable
	Loop          *PipelineTaskLoop      `json:"loop,omitempty"`                                           // 循环执行
	SnippetStages *SnippetStages         `json:"snippetStages,omitempty"`                                  // snippetStages snippet 展开
	Policy        *Policy                `json:"policy,omitempty"`                                         // action execution strategy
}

type PolicyType string

// todo add other types of implementation
// try-latest-result (if not exist -> new-run)
// force-latest-result (throw error or wait?)
//
// try-latest-success-result
// force-latest-success-result
//
// new-run (default, can omit)
//
// run-once-from-root-pipeline
const (
	NewRunPolicyType                 PolicyType = "new-run"
	TryLatestSuccessResultPolicyType PolicyType = "try-latest-success-result"
	TryLatestResultPolicyType        PolicyType = "try-latest-result"
)

func (p PolicyType) GetZhName() string {
	switch p {
	case NewRunPolicyType:
		return "重新执行并引用执行结果"
	case TryLatestSuccessResultPolicyType:
		return "最近一次执行成功的结果"
	case TryLatestResultPolicyType:
		return "最近一次执行的结果"
	default:
		return ""
	}
}

func (p PolicyType) ToString() string {
	return string(p)
}

func (p PolicyType) IsValid() bool {
	return p == "" || p == NewRunPolicyType || p == TryLatestSuccessResultPolicyType || p == TryLatestResultPolicyType
}

type Policy struct {
	Type PolicyType `json:"type,omitempty"`
}

type SnippetStages struct {
	Params  []*PipelineParam       `json:"params,omitempty"`  // 流水线输入
	Outputs []*PipelineOutput      `json:"outputs,omitempty"` // 流水线输出
	Stages  [][]*PipelineYmlAction `json:"stages,omitempty"`  // snippetStages snippet 展开
}

type SnippetConfig struct {
	Source string            `json:"source,omitempty" yaml:"source,omitempty"` // 来源 gittar dice test
	Name   string            `json:"name,omitempty" yaml:"name,omitempty"`     // 名称
	Labels map[string]string `json:"labels,omitempty" yaml:"labels,omitempty"` // 额外标签
}

type SnippetLabel struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

type SnippetConfigOrder struct {
	Source        string        `json:"source,omitempty"`
	Name          string        `json:"name,omitempty"`
	SnippetLabels SnippetLabels `json:"labels,omitempty"`
}

type SnippetLabels []SnippetLabel

func (p SnippetLabels) Len() int           { return len(p) }
func (p SnippetLabels) Less(i, j int) bool { return p[i].Key > p[j].Key }
func (p SnippetLabels) Swap(i, j int)      { p[i], p[j] = p[j], p[i] }

func (snippetConfig *SnippetConfig) Order() *SnippetConfigOrder {
	if snippetConfig == nil {
		return nil
	}

	var snippetLabels SnippetLabels
	if len(snippetConfig.Labels) > 0 {
		for k, v := range snippetConfig.Labels {
			snippetLabels = append(snippetLabels, SnippetLabel{
				Key:   k,
				Value: v,
			})
		}
		sort.Sort(snippetLabels)
	}

	var order = SnippetConfigOrder{
		Source:        snippetConfig.Source,
		Name:          snippetConfig.Name,
		SnippetLabels: snippetLabels,
	}
	return &order
}

func (snippetConfig *SnippetConfig) ToString() string {
	if snippetConfig == nil {
		return ""
	}

	return jsonparse.JsonOneLine(snippetConfig.Order())
}

type BatchSnippetConfigYml struct {
	Config SnippetConfig `json:"config"`
	Yml    string        `json:"yml"`
}

type PipelineParam struct {
	Name     string      `json:"name" yaml:"name,omitempty"`         // 名称
	Required bool        `json:"required" yaml:"required,omitempty"` // 是否必须
	Default  interface{} `json:"default" yaml:"default,omitempty"`   // 默认值
	Desc     string      `json:"desc" yaml:"desc,omitempty"`         // 描述
	Type     string      `json:"type" yaml:"type,omitempty"`         // 类型
}

type PipelineOutput struct {
	Name string `json:"name" yaml:"name,omitempty"` // 名称
	Desc string `json:"desc" yaml:"desc,omitempty"` // 描述
	Ref  string `json:"ref" yaml:"ref,omitempty"`   // 引用那个 action 的值
}

type PipelineOutputWithValue struct {
	PipelineOutput
	Value interface{} `json:"value,omitempty"` // 具体的值
}

type ActionCache struct {
	// 缓存生成的 key 或者是用户指定的 key
	// 用户指定的话 需要 {{basePath}}/路径/{{endPath}} 来自定义 key
	// 用户没有指定 key 有一定的生成规则, 具体生成规则看 prepare.go 的 setActionCacheStorageAndBinds 方法
	Key  string `json:"key,omitempty"`
	Path string `json:"path,omitempty"` // 指定那个目录被缓存, 只能是由 / 开始的绝对路径
}

type CronCompensator struct {
	Enable               bool `json:"enable"`
	LatestFirst          bool `json:"latestFirst"`
	StopIfLatterExecuted bool `json:"stopIfLatterExecuted"`
}

type PipelineYmlParseGraphRequest struct {
	PipelineYmlContent        string            `json:"pipelineYmlContent"`
	GlobalSnippetConfigLabels map[string]string `json:"globalSnippetConfigLabels"`
	SnippetConfig             *SnippetConfig    `json:"snippetConfig"`
}

type PipelineYmlParseGraphResponse struct {
	Header
	Data *PipelineYml `json:"data"`
}

type PipelineTrigger struct {
	On     string            `yaml:"on,omitempty" json:"on,omitempty"`
	Filter map[string]string `yaml:"filter,omitempty" json:"filter,omitempty"`
}
