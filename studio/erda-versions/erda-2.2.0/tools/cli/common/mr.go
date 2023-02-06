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

package common

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/utils"
)

func GetMergeState() {

}

func CreateMr(ctx *command.Context, orgID uint64, project, application string, request *apistructs.GittarCreateMergeRequest) (*apistructs.MergeRequestInfo, error) {
	var resp apistructs.GittarCreateMergeResponse
	var b bytes.Buffer

	path := fmt.Sprintf("/api/repo/%s/%s/merge-requests", project, application)
	respponse, err := ctx.Post().Path(path).JSONBody(request).
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Do().Body(&b)
	if err != nil {
		return nil, fmt.Errorf(
			utils.FormatErrMsg("create", "failed to request ("+err.Error()+")", false))
	}

	if !respponse.IsOK() {
		return nil, fmt.Errorf(utils.FormatErrMsg("create mr",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				respponse.StatusCode(), respponse.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return nil, fmt.Errorf(utils.FormatErrMsg("create mr",
			fmt.Sprintf("failed to unmarshal create response ("+err.Error()+")"), false))
	}

	return resp.Data, nil
}
