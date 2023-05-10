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

package query

import (
	"fmt"

	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
)

func (p *provider) getScope(ctx httpserver.Context) (string, error) {
	req := ctx.Request()
	scope := req.URL.Query().Get("scope")
	if scope != "" {
		if scope == permission.ScopeMicroService {
			return permission.ScopeProject, nil
		}
		return scope, nil
	}
	err := fmt.Errorf("the scope must not empty")
	return "", err
}

func (p *provider) getScopeID(ctx httpserver.Context) (string, error) {
	var id string
	req := ctx.Request()
	scopeId := req.URL.Query().Get("scopeId")
	if scopeId != "" {
		id = scopeId
	} else {
		err := fmt.Errorf("the scopeID must not empty")
		return "", err
	}
	scope, err := p.getScope(ctx)
	if err != nil {
		return "", err
	}
	if scope != permission.ScopeProject {
		return id, nil
	} else {
		scopeID, err := p.N.GetProjectByScopeID(id)
		if err != nil {
			return "", err
		}
		return scopeID, nil
	}
}
