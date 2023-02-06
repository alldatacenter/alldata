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

package apis

import (
	"net/http"

	dicestructs "github.com/erda-project/erda/apistructs"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) listReportType(r *http.Request, params struct {
	Scope string `param:"scope"`
}) interface{} {
	if params.Scope == "" {
		params.Scope = dicestructs.OrgResource
	}
	types := []reportType{
		{Name: p.t.Text(api.Language(r), "日报"), Value: daily},
		{Name: p.t.Text(api.Language(r), "周报"), Value: weekly},
	}
	resp := reportTypeResp{
		Types: types,
		Total: len(types),
	}
	return api.Success(resp)
}
