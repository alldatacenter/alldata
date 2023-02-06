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

package retention

import (
	"net/http"

	"github.com/erda-project/erda-infra/providers/httpserver"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) intRoutes(routes httpserver.Router) error {
	routes.GET("/api/monitor/"+p.typ+"/storage/retention-strategy/ttl", p.getTTLConfigCache)
	routes.GET("/api/monitor/"+p.typ+"/storage/retention-strategy/ttl-keys", p.getTTLKeysCache)
	return nil
}

func (p *provider) getTTLConfigCache(r *http.Request) interface{} {
	rc, _ := p.value.Load().(*retentionConfig)
	if rc != nil {
		return rc.matcher.SprintTree(true)
	}
	return ""
}

func (p *provider) getTTLKeysCache(r *http.Request) interface{} {
	mc, _ := p.value.Load().(*retentionConfig)
	if mc != nil {
		return api.Success(mc.keysTTL)
	}
	return api.Success(nil)
}
