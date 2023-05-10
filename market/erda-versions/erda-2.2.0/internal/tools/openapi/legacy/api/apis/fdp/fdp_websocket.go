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

package fdp

import "github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"

var FDP_WEBSOCKET = apis.ApiSpec{
	Path:        "/api/fdp-websocket/<*>",
	BackendPath: "/api/fdp-websocket/ad-hoc/ws/<*>",
	Host:        "fdp-master.marathon.l4lb.thisdcos.directory:8080",
	Scheme:      "ws",
	Method:      "GET",
	CheckLogin:  true,
	Doc: `
summary: dice's websocket proxy
`,
}
