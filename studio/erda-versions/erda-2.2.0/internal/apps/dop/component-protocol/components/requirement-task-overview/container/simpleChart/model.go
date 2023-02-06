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

package simpleChart

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

type SimpleChart struct {
	Issues []dao.IssueItem `json:"-"`
	Type   string          `json:"type"`
	Data   Data            `json:"data"`
}

type Data struct {
	Main             string `json:"main"`
	Sub              string `json:"sub"`
	CompareText      string `json:"compareText"`
	CompareValue     string `json:"compareValue"`
	Chart            Chart  `json:"chart"`
	CompareValueLink Link   `json:"compareValueLink,omitempty"`
	MainLink         Link   `json:"mainLink,omitempty"`
}

type Chart struct {
	XAxis  []string     `json:"xAxis"`
	Series []SeriesData `json:"series"`
}

type SeriesData struct {
	Name string `json:"name"`
	Data []int  `json:"data"`
}

type Link struct {
	Target string                 `json:"target,omitempty"`
	Params map[string]interface{} `json:"params,omitempty"`
}

func (s *SimpleChart) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(s)
	if err != nil {
		return err
	}
	return json.Unmarshal(b, &c)
}

func (s *SimpleChart) InitFromProtocol(ctx context.Context, c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	return json.Unmarshal(b, s)
}
