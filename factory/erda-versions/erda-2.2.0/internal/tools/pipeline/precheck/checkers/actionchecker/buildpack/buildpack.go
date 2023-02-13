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

package buildpack

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/internal/tools/pipeline/precheck/prechecktype"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

type bp struct{}

func New() *bp {
	return &bp{}
}

func (b *bp) ActionType() pipelineyml.ActionType {
	return "buildpack"
}

func (b *bp) Check(ctx context.Context, data interface{}, itemsForCheck prechecktype.ItemsForCheck) (abort bool, message []string) {
	// data type: pipelineyml.Action
	actualAction, ok := data.(pipelineyml.Action)
	if !ok {
		abort = false
		return
	}

	// 校验基于以下逻辑：
	//   buildpack action 的 module.image 都被会 release action 根据 name 插入 service.image
	//   若 modules 中存在 dice.yml services 里不存在的 name，release action insert 时会报错

	modulesStr, ok := actualAction.Params["modules"]
	if !ok {
		message = append(message, "no modules")
		return
	}

	var modules []module
	if err := json.Unmarshal([]byte(modulesStr.(string)), &modules); err != nil {
		abort = true
		message = append(message, fmt.Sprintf("failed to parse modules, err: %v", err))
		return
	}
	moduleImageMap := make(map[string]string, len(modules))
	for _, m := range modules {
		moduleImageMap[m.Name] = "${imageName}"
	}

	// try insert into dice.yml
	diceymlContent, ok := itemsForCheck.Files["dice.yml"]
	if !ok {
		abort = false
		return
	}
	d, err := diceyml.New([]byte(diceymlContent), false)
	if err != nil {
		abort = true
		message = append(message, fmt.Sprintf("failed to parse dice.yml without validate, err: %v", err))
		return
	}
	if err := diceyml.InsertImage(d.Obj(), moduleImageMap, nil); err != nil {
		abort = true
		message = append(message, fmt.Sprintf("module names not match services in dice.yml, err: %v", err))
		return
	}

	return
}

type module struct {
	Name string `json:"name"`
}
