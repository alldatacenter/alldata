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
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateTestCase 创建测试用例
func (e *Endpoints) CreateTestCase(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateTestCase.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrCreateTestCase.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.TestCaseCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateTestCase.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// TODO:鉴权

	tcID, err := e.testcase.CreateTestCase(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(tcID)
}

func (e *Endpoints) BatchCreateTestCases(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrBatchCreateTestCases.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrBatchCreateTestCases.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.TestCaseBatchCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrBatchCreateTestCases.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// TODO:鉴权

	testCaseIDs, err := e.testcase.BatchCreateTestCases(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(testCaseIDs)
}

// UpdateTestCase 更新测试用例
func (e *Endpoints) UpdateTestCase(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateTestCase.NotLogin().ToResp(), nil
	}

	testCaseID, err := strconv.ParseUint(vars["testCaseID"], 10, 64)
	if err != nil {
		return apierrors.ErrUpdateTestCase.InvalidParameter("testCaseID").ToResp(), nil
	}

	// TODO:鉴权

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrUpdateTestCase.MissingParameter("request body").ToResp(), nil
	}
	var req apistructs.TestCaseUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateTestCase.InvalidParameter(err).ToResp(), nil
	}

	req.ID = testCaseID
	req.IdentityInfo = identityInfo
	if err := e.testcase.UpdateTestCase(req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

// GetTestCase 获取测试用例详情
func (e *Endpoints) GetTestCase(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	tcID, err := strconv.ParseUint(vars["testCaseID"], 10, 64)
	if err != nil {
		return apierrors.ErrGetTestCase.InvalidParameter("testCaseID").ToResp(), nil
	}

	// TODO: 操作鉴权

	tc, err := e.testcase.GetTestCase(tcID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(*tc, strutil.DedupSlice([]string{tc.CreatorID, tc.UpdaterID}, true))
}

// BatchUpdateTestCases 批量更新测试用例
func (e *Endpoints) BatchUpdateTestCases(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrBatchUpdateTestCases.NotLogin().ToResp(), nil
	}

	// TODO:鉴权

	// 校验 body 合法性
	var req apistructs.TestCaseBatchUpdateRequest
	if r.ContentLength == 0 {
		return apierrors.ErrBatchUpdateTestCases.MissingParameter("request body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrBatchUpdateTestCases.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	if err := e.testcase.BatchUpdateTestCases(req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

// BatchUpdateTestCases 批量复制测试用例
func (e *Endpoints) BatchCopyTestCases(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrBatchCopyTestCases.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrBatchCopyTestCases.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.TestCaseBatchCopyRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrBatchCreateTestCases.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// TODO:鉴权

	copiedTestCaseIDs, err := e.testcase.BatchCopyTestCases(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(copiedTestCaseIDs)
}

func (e *Endpoints) BatchCleanTestCasesFromRecycleBin(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrBatchCleanTestCasesFromRecycleBin.NotLogin().ToResp(), nil
	}

	if r.ContentLength == 0 {
		return apierrors.ErrBatchCleanTestCasesFromRecycleBin.MissingParameter("request body").ToResp(), nil
	}
	var req apistructs.TestCaseBatchCleanFromRecycleBinRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrBatchCleanTestCasesFromRecycleBin.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	if err := e.testcase.BatchCleanFromRecycleBin(req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

// PagingTestCases 获取测试用例列表
func (e *Endpoints) PagingTestCases(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.TestCasePagingRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrPagingTestCases.InvalidParameter(err).ToResp(), nil
	}

	pagingResult, err := e.testcase.PagingTestCases(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(pagingResult, pagingResult.UserIDs)
}

// ExportTestCases 导出测试用例
func (e *Endpoints) ExportTestCases(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrExportTestCases.NotLogin().ToResp(), nil
	}

	var req apistructs.TestCaseExportRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrExportTestCases.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	l := e.bdl.GetLocaleByRequest(r)
	req.Locale = l.Name()

	// TODO:鉴权

	fileID, err := e.testcase.Export(req)
	if err != nil {
		return apierrors.ErrExportTestCases.InternalError(err).ToResp(), nil
	}

	ok, _, err := e.testcase.GetFirstFileReady(apistructs.FileActionTypeExport)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if ok {
		e.ExportChannel <- fileID
	}

	return httpserver.HTTPResponse{
		Status:  http.StatusAccepted,
		Content: fileID,
	}, nil
}

// ImportTestCases 导入测试用例
func (e *Endpoints) ImportTestCases(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrImportTestCases.NotLogin().ToResp(), nil
	}

	var req apistructs.TestCaseImportRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrImportTestCases.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// TODO:鉴权

	importResult, err := e.testcase.Import(req, r)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	ok, _, err := e.testcase.GetFirstFileReady(apistructs.FileActionTypeImport)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if ok {
		e.ImportChannel <- importResult.Id
	}

	return httpserver.HTTPResponse{
		Status:  http.StatusAccepted,
		Content: importResult,
	}, nil
}
