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

package utils

import (
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

type Resp struct {
	Success bool
	Data    json.RawMessage
	Err     *Err
}

type Page struct {
	Total int
	List  json.RawMessage
}

type Err struct {
	Code string
	Msg  string
	Ctx  interface{}
}

func (resp *Resp) ParseData(o interface{}) error {
	if !resp.Success {
		return errors.Errorf("response failed, code=%v,msg=%v,ctx=%v", resp.Err.Code, resp.Err.Msg, resp.Err.Ctx)
	}
	if o == nil {
		return nil
	}
	if err := json.Unmarshal([]byte(resp.Data), o); err != nil {
		return errors.Wrapf(err, "parse data, resp data not json, data=%v", string(resp.Data))
	}
	return nil
}

func (resp *Resp) ParsePagingListData(o interface{}) error {
	if !resp.Success {
		return errors.Errorf(" get paging resp , code=%v,msg=%v,ctx=%v", resp.Err.Code, resp.Err.Msg, resp.Err.Ctx)
	}
	var page Page
	if o == nil {
		return nil
	}
	if err := json.Unmarshal([]byte(resp.Data), &page); err != nil {
		return errors.Wrapf(err, "parse paging resp paging data not json, data=%v", string(resp.Data))
	}
	if err := json.Unmarshal([]byte(page.List), &o); err != nil {
		return errors.Wrapf(err, "parse paging list data not json, data=%v", string(page.List))
	}
	return nil
}

func DoResp(r *httpclient.Request) (Resp, error) {
	var b bytes.Buffer
	response, err := r.Do().Body(&b)
	if err != nil {
		return Resp{}, err
	}
	if !response.IsOK() {
		return Resp{}, fmt.Errorf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
			response.StatusCode(), response.ResponseHeader("Content-Type"), b.String())
	}
	var resp Resp

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return Resp{}, fmt.Errorf("response not json, raw body %s", b.String())
	}
	return resp, nil
}
