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
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/strutil"
)

type PipelineYml struct {
	data []byte
	s    *Spec

	// options below
	envs              map[string]string // 优先级高于 pipeline.yml 中 envs 字段指定的值
	flatParams        bool              // 是否将 params 扁平化(map[string]interface{} -> map[string]string)
	actionTypeMapping map[string]string // 只在升级时生效

	// outputs
	aliasToCheckRefOp               []ActionAlias
	refs                            Refs
	outputs                         Outputs
	allowMissingCustomScriptOutputs bool

	// snippet
	globalSnippetConfigLabels map[string]string         // 当前 pipeline 的提交信息, 用作 snippet 的 local 模式查询 gitta 中的文件
	SnippetCaches             []SnippetPipelineYmlCache // snippet 缓存

	// secrets
	secrets                     map[string]string
	secretsRecursiveRenderTimes int // 递归渲染次数

	// upgrade
	needUpgrade        bool   // 是否需要升级
	upgradedYmlContent []byte // 升级后的 yml content

	// triggerLabels
	triggerLabels map[string]string

	runParams []apistructs.PipelineRunParam // 运行时的输入参数
}

func (pipelineYml PipelineYml) HasOnPushBranch(branch string) bool {
	if pipelineYml.Spec().On == nil || pipelineYml.Spec().On.Push == nil {
		return false
	}

	var isOnPushBranch bool
	for _, pushBranch := range pipelineYml.Spec().On.Push.Branches {
		if pushBranch == branch {
			isOnPushBranch = true
		}
	}
	return isOnPushBranch
}

func New(b []byte, ops ...Option) (_ *PipelineYml, err error) {
	y := PipelineYml{
		data: b,
		s:    &Spec{},

		flatParams:        false,
		actionTypeMapping: defaultActionTypeMapping,

		refs: Refs{},

		outputs:                         Outputs{},
		allowMissingCustomScriptOutputs: false,
	}
	for _, op := range ops {
		op(&y)
	}

	defer func() {
		if r := recover(); r != nil {
			y.s.appendError(errors.Errorf("recover from parser, err: %v", r))
			err = y.s.mergeErrors()
		}
	}()

	// parse pipeline yml
	if err := y.parse(strutil.NormalizeNewlines(b)); err != nil {
		return nil, err
	}

	y.s.Accept(NewVersionVisitor())
	y.s.Accept(NewEnvVisitor(y.envs))
	// secretVisitor 需要在 stageVisitor 之前执行，先执行文本替换，再按需 JSON(params)
	// 否则，若先执行 stageVisitor 并 JSON(params)，然后再文本替换，替换后的 json 可能是无效的
	// example: flatParam=true，url=\
	//   ...
	//   params:
	//     bp_args:
	//       URL: ((url))
	// 先 stageVisitor:
	//   bp_args: '{"URL":"((url))"}' -> bp_args: '{"URL":"\"}' -> invalid json
	//
	// 先 secretVisitor:
	//   bp_args:     -> bp_args: '{"URL":"\\"}' -> valid json
	//     URL: \
	//
	if y.triggerLabels != nil {
		y.s.Accept(NewLabelsVisitor(y.data, y.triggerLabels))
		y.data, err = GenerateYml(y.s)
		if err != nil {
			panic(err)
		}
	}
	if y.secrets != nil {
		// 占位符文本渲染，yaml hint 不丢失，更新结构体。示例：!!str ((secret_a)) -> !!str 12345 -> "12345" (string in struct)
		y.s.Accept(NewSecretVisitor(y.data, y.secrets, y.secretsRecursiveRenderTimes))
		// 处理结构体，插入 envs 等，等待渲染
		y.s.Accept(NewEnvInsertVisitor(y.envs))
		// 重新序列化为 yaml
		y.data, err = GenerateYml(y.s)
		if err != nil {
			panic(err)
		}
		// 统一使用文本渲染方式进行占位符渲染
		y.s.Accept(NewSecretVisitor(y.data, y.secrets, y.secretsRecursiveRenderTimes))
		// 校验不存在的占位符
		y.s.Accept(NewSecretNotFoundSecret(y.data, y.secrets))
	}

	if y.runParams != nil {
		y.s.Accept(NewParamsVisitor(y.data, y.runParams))
		if len(y.s.errs) > 0 {
			return nil, y.s.mergeErrors()
		}
		y.data, err = GenerateYml(y.s)
		if err != nil {
			panic(err)
		}
		if y.secrets != nil {
			y.s.Accept(NewSecretVisitor(y.data, y.secrets, y.secretsRecursiveRenderTimes))
		}
	}

	// 遍历 action，为 render ref,output 做准备
	// 不做 flatParams，JSON 序列化在最后进行，防止简单 render 后 JSON 无效
	y.s.Accept(NewStageVisitor(false))

	y.s.Accept(NewCronVisitor())
	y.s.Accept(NewTimeoutVisitor())

	if len(y.aliasToCheckRefOp) > 0 {
		y.s.Accept(NewRefOpVisitor(y.aliasToCheckRefOp, y.refs, y.outputs, y.allowMissingCustomScriptOutputs, y.globalSnippetConfigLabels))
	}

	// 设置flatParams, 假如是render就放入loadPipelineTemplateToAction方法中了
	// 最后在 yaml 基础上 flatParams，保证 JSON 有效
	if y.flatParams {
		y.s.Accept(NewStageVisitor(y.flatParams)) // 重新渲染参数
	}

	return &y, y.s.mergeErrors()
}

type Option func(*PipelineYml)

func WithSecrets(secrets map[string]string) Option {
	return func(y *PipelineYml) { y.secrets = secrets }
}

func WithSecretsRecursiveRenderTimes(times int) Option {
	return func(y *PipelineYml) { y.secretsRecursiveRenderTimes = times }
}

// WithAliasesToCheckRefOp 设置哪些 action alias 需要检查 ref op
func WithAliasesToCheckRefOp(globalSnippetConfigLabels map[string]string, aliases ...ActionAlias) Option {
	return func(y *PipelineYml) {
		for _, a := range aliases {
			y.aliasToCheckRefOp = append(y.aliasToCheckRefOp, a)
		}
		y.globalSnippetConfigLabels = globalSnippetConfigLabels
	}
}

// WithRefs 设置可用的 refs
func WithRefs(refs Refs) Option {
	return func(y *PipelineYml) {
		y.refs = refs
	}
}

// WithRefOpOutputs 设置可用的 ref op outputs
func WithRefOpOutputs(outputs Outputs) Option {
	return func(y *PipelineYml) {
		y.outputs = outputs
	}
}

func WithRunParams(runParams []apistructs.PipelineRunParamWithValue) Option {
	return func(y *PipelineYml) {
		var polished []apistructs.PipelineRunParam
		for _, rp := range runParams {
			value := rp.Value
			if rp.TrueValue != nil {
				value = rp.TrueValue
			}
			polished = append(polished, apistructs.PipelineRunParam{Name: rp.Name, Value: value})
		}
		y.runParams = polished
	}
}

// WithAllowMissingCustomScriptOutputs 设置是否允许 custom-script 的 outputs 不存在，即忽略错误。
// Default: false，默认不忽略。
// custom-script 的 outputs 在运行时才能确定，因此在 precheck 时该参数应该设置为 true。
func WithAllowMissingCustomScriptOutputs(allow bool) Option {
	return func(y *PipelineYml) {
		y.allowMissingCustomScriptOutputs = allow
	}
}

func WithActionTypeMapping(mapping map[string]string) Option {
	return func(y *PipelineYml) {
		for k, v := range mapping {
			y.actionTypeMapping[k] = v
		}
	}
}

func WithFlatParams(flatParams bool) Option {
	return func(y *PipelineYml) {
		y.flatParams = flatParams
	}
}

func WithEnvs(envs map[string]string) Option {
	return func(y *PipelineYml) {
		y.envs = envs
	}
}

func WithTriggerLabels(triggerLabels map[string]string) Option {
	return func(y *PipelineYml) {
		y.triggerLabels = triggerLabels
	}
}

func (y *PipelineYml) Spec() *Spec {
	return y.s
}

func (y *PipelineYml) Errors() []error {
	return y.s.errs
}

func (y *PipelineYml) Warns() []string {
	return y.s.warns
}

func (y *PipelineYml) NeedUpgrade() bool {
	return y.needUpgrade
}

func (y *PipelineYml) UpgradedYmlContent() []byte {
	return y.upgradedYmlContent
}
