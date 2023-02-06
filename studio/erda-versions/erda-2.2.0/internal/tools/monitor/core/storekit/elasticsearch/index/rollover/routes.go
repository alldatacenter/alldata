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

package rollover

import (
	"encoding/json"

	"github.com/erda-project/erda-infra/providers/httpserver"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) intRoutes(routes httpserver.Router, prefix string) error {
	routes.POST(prefix+"/rollover", p.rolloverIndicesByRequest)
	return nil
}

func (p *provider) rolloverIndicesByRequest(params struct {
	Alias string `query:"alias" validate:"required"`
	Size  string `query:"size" validate:"required"`
}) interface{} {
	body, _ := json.Marshal(map[string]interface{}{
		"conditions": map[string]interface{}{
			"max_size": params.Size,
		},
	})
	ok, err := p.rolloverAlias(params.Alias, string(body))
	if err != nil {
		return api.Errors.Internal(err)
	}
	if ok {
		p.loader.ReloadIndices()
	}
	return api.Success(map[string]interface{}{
		"ok": ok,
	})
}
