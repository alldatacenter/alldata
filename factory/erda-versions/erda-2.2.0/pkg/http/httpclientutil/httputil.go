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

package httpclientutil

import (
	"bytes"
	"encoding/json"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

type RespForRead struct {
	Success bool                      `json:"success"`
	Data    json.RawMessage           `json:"data,omitempty"`
	Err     *apistructs.ErrorResponse `json:"err,omitempty"`
}

func DoJson(r *httpclient.Request, o interface{}) error {
	var b bytes.Buffer
	hr, err := r.Header("Content-Type", "application/json").Do().Body(&b)
	if err != nil {
		return errors.Wrap(err, "failed to request http")
	}
	if !hr.IsOK() {
		return errors.Errorf("failed to request http, status-code %d, content-type %s, raw body %s",
			hr.StatusCode(), hr.ResponseHeader("Content-Type"), b.String())
	}
	var resp RespForRead
	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return errors.Wrapf(err, "response not json, raw body %s", b.String())
	}
	if !resp.Success {
		return errors.Errorf("rest api not success, raw body %s", b.String())
	}
	if o == nil {
		return nil
	}
	if err := json.Unmarshal([]byte(resp.Data), o); err != nil {
		return errors.Wrapf(err, "resp.Data not json, raw string body %s", string(resp.Data))
	}
	return nil
}
