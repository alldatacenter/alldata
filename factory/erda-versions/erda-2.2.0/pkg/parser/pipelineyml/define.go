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

package pipelineyml

import (
	"fmt"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	Version1dot1 = "1.1"
	Version1dot0 = "1.0"
	Version1     = "1"
)

// Spec defines pipeline.yml.
type Spec struct {
	Version string `yaml:"version"`

	Name string `yaml:"name"`

	On       *TriggerConfig                `yaml:"on,omitempty"`
	Triggers []*apistructs.PipelineTrigger `yaml:"triggers,omitempty"` // todo Solve the problem that quotation marks will be automatically added after yaml is saved
	Storage  *StorageConfig                `yaml:"storage,omitempty"`

	Envs map[string]string `yaml:"envs,omitempty"`

	Cron            string           `yaml:"cron,omitempty"`
	CronCompensator *CronCompensator `yaml:"cron_compensator,omitempty"`

	Stages []*Stage `yaml:"stages"`

	Params []*PipelineParam `yaml:"params,omitempty"` // 流水线输入

	Outputs []*PipelineOutput `yaml:"outputs,omitempty"` // 流水线输出

	// describe the use of network hooks in the pipeline
	Lifecycle []*NetworkHookInfo `yaml:"lifecycle,omitempty"`

	// errs collect occurred errors when parse
	errs []error
	// warns collect occurred warns when parse
	warns []string

	// allActions represents all actions from all stages
	allActions map[ActionAlias]*indexedAction
}

// describe the use of network hook in the pipeline
type NetworkHookInfo struct {
	Hook   string                 `json:"hook"`   // hook type
	Client string                 `json:"client"` // use network client
	Labels map[string]interface{} `json:"labels"` // additional information
}

type StorageConfig struct {
	Context string `json:"context"`
}

type TriggerConfig struct {
	Push  *PushTrigger  `yaml:"push,omitempty"`
	Merge *MergeTrigger `yaml:"merge,omitempty"`
}

type PushTrigger struct {
	Branches []string `yaml:"branches,omitempty"`
	Tags     []string `yaml:"tags,omitempty"`
}

type MergeTrigger struct {
	Branches []string `yaml:"branches,omitempty"`
}

type indexedAction struct {
	*Action
	stageIndex int
}

// Stage represents a stage.
// Stages executes in series;
// Actions under a same stage executes in parallel.
type Stage struct {
	Actions []typedActionMap `yaml:"stage"`
}

type PipelineParam struct {
	Name     string      `json:"name,omitempty" yaml:"name,omitempty"`         // 名称
	Required bool        `json:"required,omitempty" yaml:"required,omitempty"` // 是否必须
	Default  interface{} `json:"default,omitempty" yaml:"default,omitempty"`   // 默认值
	Desc     string      `json:"desc,omitempty" yaml:"desc,omitempty"`         // 描述
	Type     string      `json:"type,omitempty" yaml:"type,omitempty"`         // 类型
}

type PipelineOutput struct {
	Name string `json:"name,omitempty" yaml:"name,omitempty"` // 名称
	Desc string `json:"desc,omitempty" yaml:"desc,omitempty"` // 描述
	Ref  string `json:"ref,omitempty" yaml:"ref,omitempty"`   // 引用那个 action 的值
}

// typedActionMap length must be 1.
type typedActionMap map[ActionType]*Action

type Action struct {
	Alias       ActionAlias            `yaml:"alias,omitempty"`
	Description string                 `yaml:"description,omitempty"`
	Version     string                 `yaml:"version,omitempty"`
	Params      map[string]interface{} `yaml:"params,omitempty"`
	Labels      map[string]string      `yaml:"labels,omitempty"`

	Workspace string                       `yaml:"workspace,omitempty"`
	Image     string                       `yaml:"image,omitempty"`
	Commands  []string                     `yaml:"commands,omitempty"`
	Loop      *apistructs.PipelineTaskLoop `yaml:"loop,omitempty"`

	Timeout int64 `yaml:"timeout,omitempty"` // unit: second

	Resources Resources `yaml:"resources,omitempty"`

	Type ActionType `yaml:"-"`

	Caches []ActionCache `yaml:"caches,omitempty"` // action 构建缓存

	Policy *Policy `yaml:"policy,omitempty"` // action execution strategy

	SnippetConfig *SnippetConfig `yaml:"snippet_config,omitempty"` // snippet 类型的 action 的配置

	If string `yaml:"if,omitempty"` // 条件执行

	Disable bool `yaml:"disable,omitempty"` // make task disable or enable

	// TODO 在未来版本中，可能去除 stage，依赖关系则必须通过 Needs 来声明。
	// 目前不开放给用户使用。由 parser 自动赋值。
	// Needs 显式声明依赖的 actions。隐式依赖关系是下一个 stage 依赖之前所有 stage 里的 action。
	// Needs 可以绕开 stage 限制，以 DAG 方式声明依赖关系。
	// Needs 一旦声明，只包含声明的值，不会注入其他依赖。
	Needs []ActionAlias `yaml:"-"`

	// TODO 该字段目前是兼容字段。
	// 在 1.1 版本中，Needs = NeedNamespaces
	// 在 1.0 版本中，Needs <= NeedNamespaces
	// 目前不开放给用户使用。由 parser 自动赋值。
	// NeedNamespaces 显式声明依赖的 namespaces。隐式依赖关系是下一个 stage 依赖之前所有 stage 的 namespaces。
	// NeedNamespaces 一旦声明，只包含声明的值，不会注入其他依赖。
	NeedNamespaces []string `yaml:"-"`

	// TODO 该字段目前是兼容字段，在未来版本中可以通过该字段扩展上下文。
	// 目前不开放给用户使用。由 parser 自动赋值。
	// Namespaces 显式声明 action 的命名空间，每个命名空间在流水线上下文目录下是唯一的，可以是目录或者文件。
	// 隐式命名空间为一个 alias，对应流水线上下文目录下的一个目录。
	// Namespaces 即使声明，同时会注入默认值 alias，也就是说每个 action 至少会有一个 namespace。
	Namespaces []string `yaml:"namespaces,omitempty"`
}

type Policy struct {
	Type apistructs.PolicyType `yaml:"type,omitempty"`
}

type SnippetConfig struct {
	Source string            `yaml:"source,omitempty"` // 来源 gittar dice test
	Name   string            `yaml:"name,omitempty"`   // 名称
	Labels map[string]string `yaml:"labels,omitempty"` // 额外标签
}

type SnippetPipelineYmlCache struct {
	SnippetConfig SnippetConfig
	PipelineYaml  *apistructs.PipelineYml
}

func (v *SnippetConfig) toApiSnippetConfig() (config *apistructs.SnippetConfig) {
	if v != nil {
		config = &apistructs.SnippetConfig{
			Name:   v.Name,
			Source: v.Source,
			Labels: v.Labels,
		}
		return config
	}
	return nil
}

type ActionCache struct {
	// 缓存生成的 key 或者是用户指定的 key
	// 用户指定的话 需要 {{basePath}}/路径/{{endPath}} 来自定义 key
	// 用户没有指定 key 有一定的生成规则, 具体生成规则看 prepare.go 的 setActionCacheStorageAndBinds 方法
	Key  string `yaml:"key,omitempty"`
	Path string `yaml:"path,omitempty"` // 指定那个目录被缓存, 只能是由 / 开始的绝对路径
}

type ActionType string
type ActionAlias string

func (t ActionType) String() string {
	return string(t)
}

func (t ActionType) IsCustom() bool {
	return string(t) == apistructs.ActionTypeCustomScript
}

func (t ActionType) IsSnippet() bool {
	return string(t) == apistructs.ActionTypeSnippet
}

func (a ActionAlias) String() string {
	return string(a)
}

// example: git, git@1.0, git@1.1
func (action *Action) GetActionTypeVersion() string {
	r := action.Type.String()
	if action.Version != "" {
		r = r + "@" + action.Version
	}
	return r
}

type Resources struct {
	CPU     float64           `yaml:"cpu,omitempty"`
	MaxCPU  float64           `yaml:"max_cpu,omitempty"`
	Mem     int               `yaml:"mem,omitempty"`
	Disk    int               `yaml:"disk,omitempty"`
	Network map[string]string `yaml:"network,omitempty"`
}

type CronCompensator struct {
	Enable               bool `yaml:"enable"`
	LatestFirst          bool `yaml:"latest_first"`
	StopIfLatterExecuted bool `yaml:"stop_if_latter_executed"`
}

// indices:
// 0: stage index
// 1: action name or index inside a stage
func (s *Spec) appendError(err error, indices ...interface{}) {
	var prefix string
	defer func() {
		if r := recover(); r != nil {
			// ignore
		}
		if err == nil {
			return
		}
		if prefix != "" {
			err = errors.Errorf("%s: %v", prefix, err)
		}
		s.errs = append(s.errs, err)
	}()

	prefix += fmt.Sprintf("stageNum: %v, ", indices[0].(int)+1)
	switch indices[1].(type) {
	case ActionAlias:
		prefix += fmt.Sprintf("action %q", indices[1].(ActionAlias))
	case string:
		prefix += fmt.Sprintf("action %q", indices[1].(string))
	case int:
		prefix += fmt.Sprintf("actionNum %d", indices[1].(int)+1)
	default:
		prefix += fmt.Sprintf("action %v", indices[1])
	}
}

func (s *Spec) mergeErrors() error {
	if len(s.errs) == 0 {
		return nil
	}
	var errsStr []string
	for _, err := range s.errs {
		errsStr = append(errsStr, err.Error())
	}
	return errors.New(strutil.Join(errsStr, "\n", true))
}

func (s *Spec) appendWarn(warn string, indices ...interface{}) {
	var prefix string
	defer func() {
		if r := recover(); r != nil {
			// ignore
		}
		if warn == "" {
			return
		}
		if prefix != "" {
			warn = fmt.Sprintf("%s: %v", prefix, warn)
		}
		s.warns = append(s.warns, warn)
	}()

	prefix += fmt.Sprintf("stageNum: %v, ", indices[0].(int)+1)
	switch indices[1].(type) {
	case ActionAlias:
		prefix += fmt.Sprintf("action %q", indices[1].(ActionAlias))
	case string:
		prefix += fmt.Sprintf("action %q", indices[1].(string))
	default:
		prefix += fmt.Sprintf("action %v", indices[1])
	}
}

func (s *Spec) ToSimplePipelineYmlActionSlice() [][]*apistructs.PipelineYmlAction {

	if s == nil {
		return nil
	}

	if s.Stages == nil {
		return nil
	}

	var stages = make([][]*apistructs.PipelineYmlAction, len(s.Stages))
	s.LoopStagesActions(func(stage int, action *Action) {
		newAction := &apistructs.PipelineYmlAction{
			Type:  action.Type.String(),
			Alias: action.Alias.String(),
		}

		if action.SnippetConfig != nil {
			newAction.SnippetConfig = &apistructs.SnippetConfig{
				Name:   action.SnippetConfig.Name,
				Source: action.SnippetConfig.Source,
				Labels: action.SnippetConfig.Labels,
			}
		}
		stages[stage] = append(stages[stage], newAction)
	})

	return stages
}

// 遍历 spec 中的 stages 的 actions
func (s *Spec) LoopStagesActions(loopDoing func(stage int, action *Action)) {

	if s.Stages == nil {
		return
	}

	for stageIndex, stage := range s.Stages {

		if stage.Actions == nil {
			continue
		}

		for _, typedActionMap := range stage.Actions {

			if typedActionMap == nil {
				continue
			}

			for _, action := range typedActionMap {
				loopDoing(stageIndex, action)
			}
		}
	}
}

// CountActionNumByPipelineYml count action total num by pipelineYml
func CountActionNumByPipelineYml(pipelineYmlStr string) (int64, error) {
	if pipelineYmlStr == "" {
		return 0, nil
	}
	pipelineYml, err := New([]byte(pipelineYmlStr))
	if err != nil {
		return 0, err
	}

	var totalActionNum int64
	pipelineYml.Spec().LoopStagesActions(func(stage int, action *Action) {
		totalActionNum++
	})
	return totalActionNum, nil
}

func GetNameByPipelineYml(pipelineYmlStr string) (string, error) {
	if pipelineYmlStr == "" {
		return "", nil
	}
	pipelineYml, err := New([]byte(pipelineYmlStr))
	if err != nil {
		return "", err
	}
	return pipelineYml.Spec().Name, nil
}

// CountEnabledActionNumByPipelineYml count enabled action by pipelineYml
func CountEnabledActionNumByPipelineYml(pipelineYmlStr string) (int64, error) {
	if pipelineYmlStr == "" {
		return 0, nil
	}
	pipelineYml, err := New([]byte(pipelineYmlStr))
	if err != nil {
		return 0, err
	}

	var totalActionNum int64
	pipelineYml.Spec().LoopStagesActions(func(stage int, action *Action) {
		if !action.Disable {
			totalActionNum++
		}
	})
	return totalActionNum, nil
}
