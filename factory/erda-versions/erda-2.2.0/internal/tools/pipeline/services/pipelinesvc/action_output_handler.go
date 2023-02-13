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

package pipelinesvc

import (
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/encoding/jsonpath"
)

func handlerActionOutputsWithJq(action *apistructs.PipelineYmlAction, jq string) ([]string, error) {
	if action.Params == nil || len(jq) <= 0 {
		return nil, nil
	}

	paramsJson, err := json.Marshal(action.Params)
	if err != nil {
		return nil, err
	}

	filterValue, err := jsonpath.JQ(string(paramsJson), jq)
	if err != nil {
		return nil, err
	}

	filterJson, err := json.Marshal(filterValue)
	if err != nil {
		return nil, err
	}

	var outputs []string
	err = json.Unmarshal(filterJson, &outputs)
	if err != nil {
		logrus.Warnf("action jq filter outputs error: %v not []string", err)
		return nil, nil
	}

	return outputs, nil
}
