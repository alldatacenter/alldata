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
	"encoding/base64"
	"fmt"
	"strings"

	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/mock"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	RefOpOutput = "OUTPUT"

	RefOpExEscape           = "escape"
	RefOpExWrapDoubleQuotes = "wrap_double_quotes"
	RefOpExWrapSingleQuotes = "wrap_single_quotes"
)

// RefOp split from ${alias:OPERATION:key}
type RefOp struct {
	Ori string // ${alias:OPERATION:key}
	Ref string // ref: alias or namespace
	Op  string // OPERATION
	Key string // key
	Ex  string // Executor

	IsAlias           bool // 是否是 alias
	IsNamespace       bool // 是否是 namespace
	RefStageIndex     int  // ref 所属 stage index
	CurrentStageIndex int  // 当前 action 的 stage index
}

type Refs map[string]string
type Outputs map[ActionAlias]map[string]string

type HandleResult struct {
	Errs  []error
	Warns []string
}

func (r *HandleResult) AppendError(err error) {
	r.Errs = append(r.Errs, err)
}
func (r *HandleResult) AppendWarn(warn string) {
	r.Warns = append(r.Warns, warn)
}

type RefOpVisitor struct {
	aliasToCheck              map[ActionAlias]struct{}
	allActions                map[ActionAlias]*indexedAction
	currentAction             *indexedAction
	globalSnippetConfigLabels map[string]string

	// refs
	availableRefs Refs

	// OUTPUT
	availableOutputs                Outputs
	allowMissingCustomScriptOutputs bool

	// result
	result HandleResult
}

// commitDetail 用作 snippet 校验 outputs
// bdl 用作 snippet 校验 outputs
func NewRefOpVisitor(aliases []ActionAlias, availableRefs Refs, availableOutputs Outputs, allowMissingCustomScriptOutputs bool, globalSnippetConfigLabels map[string]string) *RefOpVisitor {
	aliasMap := make(map[ActionAlias]struct{})
	for _, alias := range aliases {
		aliasMap[alias] = struct{}{}
	}
	return &RefOpVisitor{
		aliasToCheck: aliasMap,

		availableRefs: availableRefs,

		availableOutputs:                availableOutputs,
		allowMissingCustomScriptOutputs: allowMissingCustomScriptOutputs,
		globalSnippetConfigLabels:       globalSnippetConfigLabels,
	}
}

func (v *RefOpVisitor) Visit(s *Spec) {
	v.allActions = s.allActions
	for _, action := range s.allActions {
		if _, ok := v.aliasToCheck[action.Alias]; !ok {
			continue
		}
		// 匹配 ${xxx}
		v.handleAction(action, func(ori string) string {
			return v.handleOneParamOrCmd(ori)
		})
		// 匹配 ${{ xxx }}
		v.handleAction(action, func(ori string) string {
			return v.handleOneParamOrCmdV2(ori)
		})
		for _, err := range v.result.Errs {
			s.appendError(err, action.stageIndex, action.Alias)
		}
		for _, warn := range v.result.Warns {
			s.appendWarn(warn, action.stageIndex, action.Alias)
		}
	}
}

// handleOneParamOrCmd handle one param or cmd, return handled result and error.
// one param or cmd will have zero or multi refOp.
// TODO make ${{ (escape outputs.alias.xx) }} instead of ${{ outputs.alias.xx.escape }}
func (v *RefOpVisitor) handleOneParamOrCmdV2(ori string) string {
	replaced := strutil.ReplaceAllStringSubmatchFunc(expression.Re, ori, func(sub []string) string {
		// inner has two formats:
		// - dirs.alias
		// - outputs.alias.key
		inner := sub[1]
		// 去除两边的空格
		inner = strings.Trim(inner, " ")

		ss := strings.SplitN(inner, ".", 4)

		if len(ss) < 2 {
			return sub[0]
		}

		refStageIndex, isAlias, isNamespace := v.getStageIndex(ss[1])

		refOp := RefOp{
			Ori:               sub[0],
			Ref:               ss[1],
			Op:                "",
			Key:               "",
			IsAlias:           isAlias,
			IsNamespace:       isNamespace,
			RefStageIndex:     refStageIndex,
			CurrentStageIndex: v.currentAction.stageIndex,
		}
		switch ss[0] {
		case expression.Dirs:
			// 可能是正常 shell 语法，例如：echo ${HOME}
			// 当且仅当 ss[0] 为合法 alias 时才替换；否则认为是正常 shell 变量语法
			// - alias
			return v.handleOneRefOrShellFormat(refOp)
		case expression.Outputs:
			// - outputs.alias.key
			refOp.Op = RefOpOutput
			refOp.Key = ss[2]
			if len(ss) == 4 {
				refOp.Ex = ss[3]
			}
			return v.handleOneRefOp(refOp)
		case expression.Random:
			typeValue := ss[1]
			value := mock.MockValue(typeValue)
			if len(ss) >= 3 {
				refOp.Ex = ss[2]
			}
			return v.handleRefEx(fmt.Sprintf("%v", value), refOp)
		case expression.Base64Decode:
			// - base64-decode.xxxxx
			baseValue := ss[1]
			decodeBytes, err := base64.StdEncoding.DecodeString(baseValue)
			if err != nil {
				v.result.AppendError(fmt.Errorf("failed to base64 decode for %s, err: %v", ori, err))
				return refOp.Ori
			}
			if len(ss) >= 3 {
				refOp.Ex = ss[2]
			}
			return v.handleRefEx(string(decodeBytes), refOp)
		default: // case 3
			return refOp.Ori
		}
	})
	return replaced
}

// handleAction handle action's params & commands.
func (v *RefOpVisitor) handleAction(action *indexedAction, handler func(ori string) string) {
	v.currentAction = action
	defer func() {
		v.currentAction = nil
	}()

	// params
	for key, value := range action.Params {
		// 首先使用 yaml 获得 []byte
		valueYAMLByte, err := yaml.Marshal(value)
		if err != nil {
			v.result.AppendError(err)
			continue
		}
		if len(valueYAMLByte) > 0 {
			valueYAMLByte = valueYAMLByte[:len(valueYAMLByte)-1]
		}
		replaced := handler(string(valueYAMLByte))
		if err := yaml.Unmarshal([]byte(replaced), &value); err != nil {
			v.result.AppendError(err)
			continue
		}
		action.Params[key] = value
	}

	// commands
	for i := range action.Commands {
		replaced := handler(action.Commands[i])
		action.Commands[i] = replaced
	}

	// caches, 将 caches 中的 ${git-checkout} 转化为实际地址
	caches := action.Caches
	if caches != nil {
		for index := range caches {
			replaced := handler(caches[index].Path)
			caches[index].Path = replaced
		}
	}

	// if
	if action.If != "" {
		condition := expression.ReplacePlaceholder(action.If)
		replaced := handler(condition)
		action.If = expression.AppendPlaceholder(replaced)
	}
}

// handleOneParamOrCmd handle one param or cmd, return handled result and error.
// one param or cmd will have zero or multi refOp.
func (v *RefOpVisitor) handleOneParamOrCmd(ori string) string {
	re := expression.OldRe
	replaced := strutil.ReplaceAllStringSubmatchFunc(re, ori, func(sub []string) string {
		// inner has two formats:
		// - alias
		// - alias:OPERATION:key
		inner := sub[1]

		ss := strings.SplitN(inner, ":", 3)

		refStageIndex, isAlias, isNamespace := v.getStageIndex(ss[0])

		refOp := RefOp{
			Ori:               sub[0],
			Ref:               ss[0],
			Op:                "",
			Key:               "",
			IsAlias:           isAlias,
			IsNamespace:       isNamespace,
			RefStageIndex:     refStageIndex,
			CurrentStageIndex: v.currentAction.stageIndex,
		}

		switch len(ss) {
		case 1:
			// 可能是正常 shell 语法，例如：echo ${HOME}
			// 当且仅当 ss[0] 为合法 alias 时才替换；否则认为是正常 shell 变量语法
			// - alias
			return v.handleOneRefOrShellFormat(refOp)

		case 2:
			// 可能是正常 shell 语法，默认值里有 `:`，例如：echo ${HOME1-:xxx}，输出 :xxx
			// 需要校验 ss[0] 是否是 alias，若为 alias 则非法；否则认为是正常 shell 变量语法
			return v.handleOneRefShellFormat(refOp)

		default: // case 3
			// - alias:OPERATION:key
			refOp.Op = ss[1]
			refOp.Key = ss[2]

			return v.handleOneRefOp(refOp)
		}
	})
	return replaced
}

// handleOneRefOrShellFormat handle format: ${alias}, ${namespace} or shell format
func (v *RefOpVisitor) handleOneRefOrShellFormat(refOp RefOp) (replaced string) {
	replaced = refOp.Ori

	// check alias or namespace
	if refOp.IsAlias || refOp.IsNamespace {
		// 是否可获取
		refValue, available := v.availableRefs[refOp.Ref]
		if !available {
			// parallel
			if refOp.CurrentStageIndex == refOp.RefStageIndex {
				v.result.AppendError(fmt.Errorf("invalid ref: %s, cannot reference parallel action %q", refOp.Ori, refOp.Ref))
			} else {
				v.result.AppendError(fmt.Errorf("invalid ref: %s, cannot reference not-executed action %q", refOp.Ori, refOp.Ref))
			}
			return
		}
		return refValue
	}

	// not alias, treat as shell format: ${HOME}
	return
}

// handleOneRefShellFormat handle format: ${alias:xxx}
// 可能是正常 shell 语法，默认值里有 `:`，例如：echo ${HOME1-:xxx}，输出 :xxx
// 需要校验 ss[0] 是否是 alias，若为 alias 则非法；否则认为是正常 shell 变量语法
func (v *RefOpVisitor) handleOneRefShellFormat(refOp RefOp) (replaced string) {
	replaced = refOp.Ori

	// check alias
	if refOp.IsAlias {
		v.result.AppendError(fmt.Errorf("%q, bad format, only support `${alias}` or `${alias:OPERATION:key}`", refOp.Ori))
		return
	}

	// 返回原值，运行时 shell 输出
	return
}

// handleOneRefOp handle format: ${alias:OPERATION:key}
func (v *RefOpVisitor) handleOneRefOp(refOp RefOp) (replaced string) {
	replaced = refOp.Ori

	// check alias
	if !refOp.IsAlias {
		v.result.AppendError(fmt.Errorf("%q, not found alias %q in pipeline", refOp.Ori, refOp.Ref))
		return
	}

	// handle key by op
	switch refOp.Op {
	case RefOpOutput:
		return v.handleOneRefOpOutput(refOp)
	default:
		v.result.AppendError(fmt.Errorf("%q, invalid operation [%s], only support [%s] now", refOp.Ori, refOp.Op, RefOpOutput))
		return
	}
}

// handleOneRefOpOutput handle ${alias:OUTPUT:key}
func (v *RefOpVisitor) handleOneRefOpOutput(refOp RefOp) (replaced string) {
	replaced = refOp.Ori

	// found output, return
	if v.availableOutputs[ActionAlias(refOp.Ref)] != nil {
		if output, ok := v.availableOutputs[ActionAlias(refOp.Ref)][refOp.Key]; ok {
			// If the user specifies a special escape type， use the executor escape output
			return v.handleRefEx(output, refOp)
		}
	}

	// not found
	if refOp.RefStageIndex < refOp.CurrentStageIndex {
		if v.allowMissingCustomScriptOutputs {
			v.result.AppendWarn(fmt.Sprintf("%q, action %q may not have output %q", refOp.Ori, refOp.Ref, refOp.Key))
		} else {
			v.result.AppendError(fmt.Errorf("%q, action %q doesn't have output %q", refOp.Ori, refOp.Ref, refOp.Key))
		}
	} else if refOp.RefStageIndex == refOp.CurrentStageIndex {
		v.result.AppendError(fmt.Errorf("%q, cannot reference parallel action %q", refOp.Ori, refOp.Ref))
	} else {
		v.result.AppendError(fmt.Errorf("%q, cannot reference not-executed action %q", refOp.Ori, refOp.Ref))
	}

	return
}

func (v *RefOpVisitor) getStageIndex(namespace string) (stageIndex int, isAlias bool, isNamespace bool) {
	stageIndex, isAlias, isNamespace = -1, false, false
	for _, action := range v.allActions {
		if action.Alias.String() == namespace {
			stageIndex, isAlias, isNamespace = action.stageIndex, true, true
			return
		}
		for _, one := range action.Namespaces {
			if one == namespace {
				stageIndex, isAlias, isNamespace = action.stageIndex, false, true
				return
			}
		}
	}
	return
}

func (v *RefOpVisitor) handleRefEx(output string, refOp RefOp) string {
	switch refOp.Ex {
	case RefOpExEscape:
		return expression.Quote(output)
	case RefOpExWrapDoubleQuotes:
		return fmt.Sprintf(`"%v"`, output)
	case RefOpExWrapSingleQuotes:
		return fmt.Sprintf(`'%v'`, output)
	default:
		// do nothing
		return output
	}
}
