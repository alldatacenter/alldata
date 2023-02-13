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

package release

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/erda-project/erda/internal/tools/pipeline/precheck/prechecktype"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

const (
	paramCrossCluster = "cross_cluster"
	ActionType        = "release"
)

type release struct{}

func New() *release {
	return &release{}
}

func (r *release) ActionType() pipelineyml.ActionType {
	return ActionType
}

func (r *release) Check(ctx context.Context, data interface{}, itemsForCheck prechecktype.ItemsForCheck) (abort bool, messages []string) {
	// data type: pipelineyml.Action
	actualAction, ok := data.(pipelineyml.Action)
	if !ok {
		abort = false
		return
	}

	// check dice.yml
	diceymlContent, _ := itemsForCheck.Files["dice.yml"]
	if diceymlContent == "" {
		abort = false
		return
	}

	checkDiceYml := true
	if actualAction.Params != nil {
		checkDiceYmlStr, ok := actualAction.Params["check_diceyml"]
		if ok {
			b, err := strconv.ParseBool(checkDiceYmlStr.(string))
			if err != nil {
				abort = true
				messages = append(messages, fmt.Sprintf("invalid param 'check_diceyml', value: %s", checkDiceYmlStr))
			}
			checkDiceYml = b
		}
	}

	// parse dice.yml
	d, err := diceyml.New([]byte(diceymlContent), checkDiceYml)
	if err != nil {
		abort = true
		messages = append(messages, fmt.Sprintf("failed to parse dice.yml, err: %v", err))
		return
	}
	// check images
	if actualAction.Params != nil {
		// param: image
		imageJson, ok := actualAction.Params["image"]
		if ok {
			images := make(map[string]string)
			err := json.Unmarshal([]byte(imageJson.(string)), &images)
			if err != nil {
				abort = true
				messages = append(messages, fmt.Sprintf("invalid param image, err: %v", err))
			}
			if err := d.InsertImage(images, nil); err != nil {
				abort = true
				messages = append(messages, fmt.Sprintf("failed to insert image into dice.yml, err: %v", err))
				return
			}
		}
	}

	// check cross_cluster 参数
	spec := itemsForCheck.ActionSpecs[actualAction.GetActionTypeVersion()]
	crossCluster := false
	for _, param := range spec.Params {
		if param.Name == paramCrossCluster {
			v, ok := actualAction.Params[paramCrossCluster]
			if !ok {
				// use default value
				v = param.Default
			}
			switch v.(type) {
			case bool:
				crossCluster = v.(bool)
			case string:
				crossCluster, _ = strconv.ParseBool(v.(string))
			}
		}
	}
	prechecktype.PutContextResult(ctx, prechecktype.CtxResultKeyCrossCluster, crossCluster)

	return
}
