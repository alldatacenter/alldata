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

package action

import (
	"fmt"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

// 组件状态
type ComponentActionState struct {
	Version string `json:"version"`
}

// 将组件关心的数据类型具体化
type ComponentAction struct {
	ctxBdl   protocol.ContextBundle
	CompName string                 `json:"name"`
	Props    map[string]interface{} `json:"props"`
	State    ComponentActionState   `json:"state"`
}

type VersionOption struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}

func (a *ComponentAction) SetBundle(b protocol.ContextBundle) error {
	if b.Bdl == nil {
		err := fmt.Errorf("invalie bundle")
		return err
	}
	a.ctxBdl = b
	return nil
}

func (a *ComponentAction) SetCompName(name string) error {
	if name == "" {
		return nil
	}
	if a.CompName == "" {
		a.CompName = name
	}
	return nil
}

func (a *ComponentAction) SetActionState(s ComponentActionState) {
	a.State = s
}

func (a *ComponentAction) SetActionProps(p interface{}) {
	a.Props = map[string]interface{}{"fields": p}
}

func GetActionVersion(c *apistructs.Component) string {
	version, ok := c.State["version"]
	if !ok {
		return ""
	}
	versionValue, ok := version.(string)
	if !ok {
		return ""
	}
	return versionValue
}

func QueryExtensionVersion(bdl *bundle.Bundle, name, version string) (*apistructs.ExtensionVersion, []VersionOption, error) {
	var (
		target   *apistructs.ExtensionVersion
		versions []VersionOption
	)
	actions, err := bdl.QueryExtensionVersions(apistructs.ExtensionVersionQueryRequest{Name: name, YamlFormat: false})
	if err != nil {
		return nil, nil, err
	}
	if len(actions) == 0 {
		err := fmt.Errorf("empty public action: [%s]", name)
		return nil, nil, err
	} else {
		for i, v := range actions {
			// 所有action版本
			versions = append(versions, VersionOption{Name: v.Version, Value: v.Version})
			// 如果version为空，选择默认版本
			if version == "" && v.IsDefault {
				target = &actions[i]
			}
			// 如果version不为空，选择指定版本
			if version != "" && v.Version == version {
				target = &actions[i]
			}
		}
	}

	if version != "" && target == nil {
		err := fmt.Errorf("cannot find action [%s] with version [%s]", name, version)
		return nil, nil, err
	} else if target == nil {
		// version 为空， target 为空, 没有找到默认值
		// 如果版本只有一个，则为默认值；如果版本有多个，用一个做默认值，并告警
		target = &actions[0]
		if len(actions) > 0 {
			logrus.Warnf("action [%s] with [%d] versions but none default one, select the first one, version [%s]", name, len(actions), target.Version)
		}
	}
	return target, versions, nil
}

func GetFromProps(extVersion *apistructs.ExtensionVersion) (interface{}, error) {
	if extVersion == nil {
		err := fmt.Errorf("empty action extension")
		return nil, err
	}

	specBytes, err := yaml.Marshal(extVersion.Spec)
	if err != nil {
		err := fmt.Errorf("failed to marshal action spec, error:%v", err)
		return nil, err
	}
	spec := apistructs.ActionSpec{}
	err = yaml.Unmarshal(specBytes, &spec)
	if err != nil {
		err := fmt.Errorf("failed to unmarshal action spec, error:%v", err)
		return nil, err
	}
	if spec.FormProps == nil {
		err := fmt.Errorf("empty form props, action:%s, version:%s", extVersion.Name, extVersion.Version)
		return nil, err
	}
	return spec.FormProps, nil
}
