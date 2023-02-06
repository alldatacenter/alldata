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
	"regexp"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1"
	"github.com/erda-project/erda/pkg/strutil"
)

// UpgradeYmlFromV1 根据传入的 v1 yaml content 给出 v1.1 yaml content
func UpgradeYmlFromV1(v1 []byte) ([]byte, error) {
	newPipelineYml := PipelineYml{data: v1, s: &Spec{}, flatParams: false} // upgrade 时，不做 params 的 flat 转化，保持原有结构
	if err := newPipelineYml.parseV1(); err != nil {
		return nil, err
	}
	if err := newPipelineYml.upgradeYmlFromV1(); err != nil {
		return nil, err
	}
	return newPipelineYml.upgradedYmlContent, nil
}

func (y *PipelineYml) upgradeYmlFromV1() error {
	b, err := GenerateYml(y.s)
	if err != nil {
		return err
	}
	y.upgradedYmlContent = b
	return nil
}

// parseV1 parse v1.0 to v1.1
func (y *PipelineYml) parseV1() error {
	pipelineYmlV1 := pipelineymlv1.New(y.data)
	if err := pipelineYmlV1.Parse(); err != nil {
		return err
	}
	o := pipelineYmlV1.Object()

	// version
	y.s.Version = Version1dot1

	// envs
	y.s.Envs = o.Envs

	// cron
	for _, trigger := range o.Triggers {
		y.s.Cron = trigger.Schedule.Cron
		if y.s.Cron != "" {
			break
		}
	}

	var errs []string

	// stages
	y.s.Stages = make([]*Stage, 0)
	allNamespaces := make(map[string]struct{})
	for _, oldStage := range o.Stages {

		actions := make([]typedActionMap, 0)

		for _, oldTask := range oldStage.Tasks {

			action := &Action{}

			// --- type ---
			var actionType ActionType
			if oldTask.IsResourceTask() {
				actionType = ActionType(oldTask.GetResourceType())
			} else {
				actionType = ActionType("custom")
			}
			action.Type = mappingActionType(y.actionTypeMapping, actionType)

			// --- alias ---
			action.Alias = ActionAlias(oldTask.Name())

			// --- params ---
			params := make(map[string]interface{})
			if len(oldTask.GetTaskParams()) > 0 {
				for k, v := range oldTask.GetTaskParams() {
					params[k] = v
				}
			}
			if oldTask.GetResource() != nil && len(oldTask.GetResource().Source) > 0 {
				for k, v := range oldTask.GetResource().Source {
					params[k] = v
				}
			}
			action.Params = params
			simplifyParams(action.Params)
			mappingParamRef(action.Params, allNamespaces)
			action.noNullParams()
			// flat
			if y.flatParams {
				if err := action.flatParams(); err != nil {
					errs = append(errs, err.Error())
				}
			}

			// --- commands ---
			if !oldTask.IsResourceTask() {
				cmd := oldTask.GetCustomTaskRunPathArgs()[0]
				if len(oldTask.GetCustomTaskRunPathArgs()) > 1 {
					args := strutil.Join(oldTask.GetCustomTaskRunPathArgs()[1:], " ", false)
					cmd = strutil.Concat(cmd, " ", args)
				}
				action.Commands = []string{"cd ..", cmd}
			}
			// TODO 对自定义任务的 commands 不做 ref 替换
			// mappingCommandsRef(action.Commands, allActionAlias)
			// TODO 魔改 cmd 改变 pwd，保证用户不用修改脚本

			// --- version ---
			// 从市场获取

			// --- image ---
			// 从市场获取

			// --- timeout ---
			// 为空

			// --- needs ---
			// 为空，会将所有前置 action 作为依赖

			// --- namespaces ---
			for _, output := range oldTask.OutputToContext() {
				action.Namespaces = append(action.Namespaces, output)
				allNamespaces[output] = struct{}{}
			}

			actions = append(actions, typedActionMap{action.Type: action})
		}

		y.s.Stages = append(y.s.Stages, &Stage{Actions: actions})
	}

	if err := y.upgradeYmlFromV1(); err != nil {
		errs = append(errs, err.Error())
	}

	if len(errs) > 0 {
		return errors.New(strutil.Join(errs, "\n", true))
	}

	return nil
}

// simplifyParams 平台注入的占位符不需要用户填写
func simplifyParams(params map[string]interface{}) {
	m := map[string]struct{}{
		"((gittar.repo))":      {},
		"((gittar.branch))":    {},
		"((gittar.username))":  {},
		"((gittar.password))":  {},
		"((dice.url))":         {}, // ui public url
		"((dice.id))":          {}, // app id
		"((dice.operator.id))": {},
		"((dice.env))":         {}, // workspace
	}
	for k, v := range params {
		if vv, ok := v.(string); ok {
			if _, ok := m[vv]; ok {
				delete(params, k)
			}
		}
	}
}

func mappingParamRef(params map[string]interface{}, allNamespaces map[string]struct{}) {
	for k, v := range params {
		params[k] = replace(v, allNamespaces)
	}
}

func mappingCommandsRef(commands []string, allNamespaces map[string]struct{}) {
	for i, v := range commands {
		s, ok := replace(v, allNamespaces).(string)
		if ok {
			commands[i] = s
		}
	}
}

func replace(v interface{}, allNamespaces map[string]struct{}) interface{} {
	v_, err := yaml.Marshal(&v)
	if len(v_) > 0 {
		v_ = v_[:len(v_)-1]
	}
	s := string(v_)
	if err != nil {
		logrus.Warn(err)
		return v
	}

	re := regexp.MustCompile(`[^${\s-/\\]+[\w-.]+`)

	ss := strings.Split(s, "\n")
	for i := range ss {
		vv := strings.Split(ss[i], ",")
		for i := range vv {
			replaceOnce := false
			replaced := false
			if strings.Contains(vv[i], "/") {
				replaceOnce = true
			}
			vv[i] = re.ReplaceAllStringFunc(vv[i], func(matched string) string {
				if replaceOnce && replaced {
					return matched
				}
				for ref := range allNamespaces {
					if matched == ref {
						replaced = true
						return fmt.Sprintf(`${%s}`, ref)
					}
				}
				return matched
			})
		}

		ss[i] = strings.Join(vv, ",")
	}

	s = strings.Join(ss, "\n")

	if err := yaml.Unmarshal([]byte(s), &v); err != nil {
		logrus.Warn(err)
		return v
	}
	return v
}

// mappingActionType 新老 action 名字映射
func mappingActionType(mapping map[string]string, old ActionType) ActionType {
	n, ok := mapping[string(old)]
	if ok {
		return ActionType(n)
	}
	return old
}

// 默认映射关系，只在升级时生效
var defaultActionTypeMapping = map[string]string{
	"git":     "git-checkout",
	"dicehub": "release",
	"sonar":   "qa",
	"ut":      "unit-test",
	"it":      "integration-test",
	"dice":    "dice",
	"custom":  "custom-script",
}
