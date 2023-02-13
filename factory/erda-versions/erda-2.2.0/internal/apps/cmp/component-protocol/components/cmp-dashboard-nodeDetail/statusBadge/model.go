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

package statusBadge

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/bundle"
)

type StatusBadge struct {
	CtxBdl *bundle.Bundle

	SDK  *cptype.SDK
	Ctx  context.Context
	Type string           `json:"type"`
	Data map[string][]Bar `json:"data"`
}

type Bar struct {
	Text    string `json:"text"`
	Status  string `json:"status"`
	WhiteBg bool   `json:"whiteBg"`
	Tip     string `json:"tip,omitempty"`
}
