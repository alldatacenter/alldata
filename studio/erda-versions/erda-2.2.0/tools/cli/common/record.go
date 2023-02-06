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

func GetRecord(ctx *command.Context, orgID, id uint64) (apistructs.TestFileRecord, error) {
	var resp apistructs.GetTestFileRecordResponse
	var b bytes.Buffer

	response, err := ctx.Get().
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Path(fmt.Sprintf("/api/test-file-records/%d", id)).
		Do().Body(&b)
	if err != nil {
		return apistructs.TestFileRecord{}, fmt.Errorf(utils.FormatErrMsg(
			"get project detail", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return apistructs.TestFileRecord{}, fmt.Errorf(utils.FormatErrMsg("get record",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return apistructs.TestFileRecord{}, fmt.Errorf(utils.FormatErrMsg("get record",
			fmt.Sprintf("failed to unmarshal record  response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return apistructs.TestFileRecord{}, fmt.Errorf(utils.FormatErrMsg("get record",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	return resp.Data, nil
}
