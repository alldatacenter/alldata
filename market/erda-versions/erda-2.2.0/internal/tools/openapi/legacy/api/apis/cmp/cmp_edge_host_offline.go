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

package cmp

import (
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

type EdgeHostOffline struct {
	SiteIP string `json:"siteIP"`
}

var CMP_EDGE_HOST_OFFLINE = apis.ApiSpec{
	Path:        "/api/edge/site/offline/<ID>",
	BackendPath: "/api/edge/site/offline/<ID>",
	Host:        "ecp.marathon.l4lb.thisdcos.directory:9029",
	Scheme:      "http",
	Method:      "DELETE",
	RequestType: EdgeHostOffline{},
	CheckLogin:  true,
	Doc:         "下线边缘计算站点机器",
}
