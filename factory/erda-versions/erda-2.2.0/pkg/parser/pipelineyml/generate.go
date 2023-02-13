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
	"bytes"

	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/strutil"
)

// GenerateYml 根据 spec 重新生成 yaml 文本，一般用于对 spec 进行调整后重新生成 yaml 文本
func GenerateYml(s *Spec) ([]byte, error) {
	polishNamespaces(s)
	var newYmlBuf bytes.Buffer
	encoder := yaml.NewEncoder(&newYmlBuf)
	encoder.SetIndent(1)
	if err := encoder.Encode(s); err != nil {
		return nil, err
	}
	return newYmlBuf.Bytes(), nil
}

// polishNamespaces 遍历 actions，判断 namespaces 长度：
// 因为目前 namespaces 的策略是 alias 一定会注入，所以可以删除 alias，优化最终生成的 yaml。
func polishNamespaces(s *Spec) {
	for _, stage := range s.Stages {
		for _, typedAction := range stage.Actions {
			for _, action := range typedAction {
				if action == nil {
					action = &Action{}
				}
				action.Namespaces = strutil.RemoveSlice(action.Namespaces, action.Alias.String())
			}
		}
	}
}
