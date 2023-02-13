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
	"encoding/json"
	"fmt"
	"reflect"
	"regexp"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/strutil"
)

var (
	aliasRegex = regexp.MustCompile("^[^\\\\]*$")
)

type StageVisitor struct {
	flatParams bool
}

func NewStageVisitor(flatParams bool) *StageVisitor {
	v := StageVisitor{}
	v.flatParams = flatParams
	return &v
}

func (v *StageVisitor) Visit(s *Spec) {
	if len(s.Stages) == 0 {
		s.Stages = make([]*Stage, 0)
		return
	}
	// init or clean original
	s.allActions = make(map[ActionAlias]*indexedAction)

	// availableNamespaces 表示遍历到不同 stages 时当前所有前置 stage 的 namespace
	availableNamespaces := make(map[string]struct{})

	// availableActions 表示遍历到不同 stages 时当前所有前置 stage 的 action
	availableActions := make(map[ActionAlias]struct{})

	for stageIndex, stage := range s.Stages {
		if len(stage.Actions) == 0 {
			s.appendError(errors.New("doesn't have any actions"), stageIndex)
		}
		// stageNamespaces 表示该 stage 下所有的 namespace
		stageNamespaces := make(map[string]struct{})
		// stageActions 表示该 stage 下所有的 action
		stageActions := make(map[ActionAlias]struct{})
		for actionIndex, typedActionMap := range stage.Actions {
			if len(typedActionMap) == 0 {
				s.appendError(errors.New("empty!"), stageIndex, actionIndex)
			}
			if len(typedActionMap) > 1 {
				var actionNames []string
				for name := range typedActionMap {
					actionNames = append(actionNames, string(name))
				}
				s.appendError(errors.Errorf("indent is incorrect! nearby: %s",
					strutil.Join(actionNames, ", ", true)), stageIndex, actionIndex)
			}
			for actionType, action := range typedActionMap {
				// 兼容没有写 git: {} 的情况
				// - stage:
				//   - git:
				if action == nil {
					action = &Action{}
					typedActionMap[actionType] = action
				}

				// alias 的 默认值 为 actionType
				if action.Alias == "" {
					action.Alias = ActionAlias(actionType)
				}
				// find duplicated action name
				if _, ok := s.allActions[action.Alias]; ok {
					s.appendError(errors.Errorf("action name %q is duplicated", action.Alias), stageIndex, action.Alias)
				}
				if !aliasRegex.MatchString(string(action.Alias)) {
					s.appendError(errors.Errorf("invalid action alias name: %s, regex: %s", action.Alias, aliasRegex.String()))
				}

				action.Type = actionType

				// params
				action.noNullParams()
				action.markParamsValueType()
				if v.flatParams {
					if err := action.flatParams(); err != nil {
						s.appendError(err, stageIndex, action.Alias)
					}
				}

				// needs
				if len(action.Needs) == 0 {
					action.Needs = toList(availableActions)
				}

				// needNamespaces
				if len(action.NeedNamespaces) == 0 {
					action.NeedNamespaces = toListStr(availableNamespaces)
				}

				// namespaces
				action.Namespaces = strutil.DedupSlice(append(action.Namespaces, action.Alias.String()))
				// find duplicated namespace
				for _, ns := range action.Namespaces {
					if _, ok := availableNamespaces[ns]; ok {
						s.appendError(errors.Errorf("action namespaces %q is duplicated", ns), stageIndex, action.Alias)
					}
				}

				// update stageNamespaces
				for _, ns := range action.Namespaces {
					stageNamespaces[ns] = struct{}{}
				}

				// update stageActions
				stageActions[action.Alias] = struct{}{}

				s.allActions[action.Alias] = &indexedAction{action, stageIndex}
			}
		}
		for ns := range stageNamespaces {
			availableNamespaces[ns] = struct{}{}
		}
		for action := range stageActions {
			availableActions[action] = struct{}{}
		}
	}
}

// flatParams 将 params 的 value (包括复杂结构体) 转换为 json(string)
func (action *Action) flatParams() error {
	for k, v := range action.Params {
		typ := reflect.TypeOf(v)
		if typ == nil {
			action.Params[k] = ""
			continue
		}
		switch typ.Kind() {
		// 非普通字段转换为 json
		case reflect.Map, reflect.Slice, reflect.Struct:
			replacedJSONByte, err := json.Marshal(markInterfaceType(v))
			if err != nil {
				return err
			}
			action.Params[k] = string(replacedJSONByte)

		// 其他转换为 string
		default:
			action.Params[k] = fmt.Sprintf("%v", v)
		}
	}
	return nil
}

// noNullParams 将 params 中 value == nil 的 key 设置为 key: ""
func (action *Action) noNullParams() {
	for k, v := range action.Params {
		if v == nil {
			action.Params[k] = ""
		}
	}
}

func (action *Action) markParamsValueType() {
	for k, v := range action.Params {
		action.Params[k] = markInterfaceType(v)
	}
}

func toList(m map[ActionAlias]struct{}) []ActionAlias {
	var r []ActionAlias
	for k := range m {
		r = append(r, k)
	}
	return r
}

func toListStr(m map[string]struct{}) []string {
	var r []string
	for k := range m {
		r = append(r, k)
	}
	return r
}

func markInterfaceType(i interface{}) interface{} {
	switch x := i.(type) {
	case map[interface{}]interface{}:
		m := map[string]interface{}{}
		for k, v := range x {
			ks := fmt.Sprintf("%v", k)
			m[ks] = markInterfaceType(v)
		}
		return m
	case []interface{}:
		for i, v := range x {
			x[i] = markInterfaceType(v)
		}
	}
	return i
}

func GetAction(s *Spec, alias ActionAlias) (*Action, error) {
	action, ok := s.allActions[alias]
	if !ok {
		return nil, errors.Errorf("not found, alias: %s", alias)
	}
	return action.Action, nil
}

func ListAction(s *Spec) map[ActionAlias]*Action {
	result := make(map[ActionAlias]*Action)
	for alias, indexAction := range s.allActions {
		result[alias] = indexAction.Action
	}
	return result
}
