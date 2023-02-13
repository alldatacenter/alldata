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

package endpoints

import (
	"context"
	"net/http"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

func (e *Endpoints) GetFileRecord(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetAutoTestSceneSet.NotLogin().ToResp(), nil
	}

	id, err := strconv.ParseUint(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrGetFileRecord.InvalidParameter("id").ToResp(), nil
	}

	locale := e.bdl.GetLocaleByRequest(r).Name()
	record, err := e.testcase.GetFileRecord(id, locale)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(record, []string{record.OperatorID})
}

// Get File Records
func (e *Endpoints) GetFileRecords(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListFileRecord.NotLogin().ToResp(), nil
	}

	var req apistructs.ListTestFileRecordsRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListFileRecord.InvalidParameter(err).ToResp(), nil
	}

	req.Locale = e.bdl.GetLocaleByRequest(r).Name()
	req.IdentityInfo = identityInfo
	list, operators, count, total, err := e.testcase.ListFileRecords(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(&apistructs.ListTestFileRecordsResponseData{
		List:    list,
		Counter: count,
		Total:   total,
	}, operators)
}
