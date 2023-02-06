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

package loader

import (
	"net/http"

	"github.com/dustin/go-humanize"

	"github.com/erda-project/erda-infra/providers/httpserver"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) intRoutes(routes httpserver.Router, prefix string) error {
	routes.GET(prefix+"/loader/indices", p.getIndicesCache)
	routes.GET(prefix+"/loader/indices/tenant-size", p.tenantSizeBytes)
	return nil
}

func (p *provider) tenantSizeBytes() interface{} {
	data := p.AllIndices()
	if data == nil {
		return api.Success(nil)
	}
	res := make(map[string]interface{})
	for tenant, ig := range data.Groups {
		res[tenant] = humanize.Bytes(uint64(getSizeOfTenant(ig, 0)))
	}
	return api.Success(res)
}

func getSizeOfTenant(ig *IndexGroup, size int64) int64 {
	for _, nig := range ig.Groups {
		size += getSizeOfTenant(nig, size)
	}
	for _, item := range ig.List {
		size += item.StoreBytes
	}
	for _, item := range ig.Fixed {
		size += item.StoreBytes
	}
	return size
}

func (p *provider) getIndicesCache(r *http.Request) interface{} {
	v := p.indices.Load()
	return api.Success(v)
}
