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

package openapi

import (
	"encoding/json"
	"net/http"

	"github.com/erda-project/erda-infra/base/version"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
)

var DICE_METADATA = apis.ApiSpec{
	Path:   "/metadata.json",
	Scheme: "http",
	Method: "GET",
	Custom: func(rw http.ResponseWriter, req *http.Request) {
		meta := make(map[string]interface{})
		meta["version"] = map[string]interface{}{
			"dice_version": version.Version,
			"git_commit":   version.CommitID,
			"go_version":   version.GoVersion,
			"built":        version.BuildTime,
		}
		meta["openapi_public_url"] = conf.SelfPublicURL()

		metaBytes, _ := json.MarshalIndent(meta, "", "  ")
		rw.Write(metaBytes)
	},
	Doc: "Dice 平台对外的元信息",
}
