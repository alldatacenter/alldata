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
	"fmt"
	"net/http"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (e *Endpoints) PagingSonarMetricRules(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var request apistructs.SonarMetricRulesPagingRequest

	if err := e.queryStringDecoder.Decode(&request, r.URL.Query()); err != nil {
		return apierrors.ErrPagingSonarMetricRules.InvalidParameter(err).ToResp(), nil
	}

	if err := checkScopeTypeAndID(request.ScopeType, request.ScopeID); err != nil {
		return nil, err
	}

	locale := e.bdl.GetLocaleByRequest(r)
	resp, err := e.sonarMetricRule.Paging(request, locale)
	if err != nil {
		return apierrors.ErrPagingSonarMetricRules.InternalError(err).ToResp(), nil
	}

	return resp, nil
}

func (e *Endpoints) GetSonarMetricRules(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	ruleID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrQuerySonarMetricRules.InvalidParameter("ID").ToResp(), nil
	}
	if ruleID <= 0 {
		return apierrors.ErrQuerySonarMetricRules.InvalidParameter("ID").ToResp(), nil
	}

	resp, err := e.sonarMetricRule.Get(ruleID)
	if err != nil {
		return apierrors.ErrQuerySonarMetricRules.InternalError(err).ToResp(), nil
	}

	return resp, nil
}

func (e *Endpoints) UpdateSonarMetricRules(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	request := apistructs.SonarMetricRulesUpdateRequest{}
	if r.ContentLength == 0 {
		return apierrors.ErrUpdateSonarMetricRules.InvalidParameter("missing request body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrUpdateSonarMetricRules.InvalidParameter(err).ToResp(), nil
	}

	if err := checkScopeTypeAndID(request.ScopeType, request.ScopeID); err != nil {
		return nil, err
	}

	ruleID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil || ruleID <= 0 {
		return apierrors.ErrUpdateSonarMetricRules.InvalidParameter("ID").ToResp(), nil
	}
	if ruleID <= 0 {
		return apierrors.ErrUpdateSonarMetricRules.InvalidParameter("ID").ToResp(), nil
	}

	request.ID = ruleID

	resp, err := e.sonarMetricRule.Update(request)
	if err != nil {
		return apierrors.ErrUpdateSonarMetricRules.InternalError(err).ToResp(), nil
	}

	return resp, nil
}

func (e *Endpoints) BatchInsertSonarMetricRules(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	request := apistructs.SonarMetricRulesBatchInsertRequest{}
	if r.ContentLength == 0 {
		return apierrors.ErrBatchCreateSonarMetricRules.InvalidParameter("missing request body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrBatchCreateSonarMetricRules.InvalidParameter(err).ToResp(), nil
	}

	if err := checkScopeTypeAndID(request.ScopeType, request.ScopeID); err != nil {
		return nil, err
	}

	resp, err := e.sonarMetricRule.BatchInsert(&request)
	if err != nil {
		return apierrors.ErrBatchCreateSonarMetricRules.InternalError(err).ToResp(), nil
	}

	return resp, nil
}

func (e *Endpoints) BatchDeleteSonarMetricRules(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	request := apistructs.SonarMetricRulesBatchDeleteRequest{}
	if r.ContentLength == 0 {
		return apierrors.ErrDeleteSonarMetricRules.InvalidParameter("missing request body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrDeleteSonarMetricRules.InvalidParameter(err).ToResp(), nil
	}

	if err := checkScopeTypeAndID(request.ScopeType, request.ScopeID); err != nil {
		return nil, err
	}

	resp, err := e.sonarMetricRule.BatchDelete(&request)
	if err != nil {
		return apierrors.ErrDeleteSonarMetricRules.InternalError(err).ToResp(), nil
	}

	return resp, nil
}

func (e *Endpoints) DeleteSonarMetricRules(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	request := apistructs.SonarMetricRulesDeleteRequest{}

	if r.ContentLength == 0 {
		return apierrors.ErrDeleteSonarMetricRules.InvalidParameter("missing request body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrDeleteSonarMetricRules.InvalidParameter(err).ToResp(), nil
	}

	ruleID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil || ruleID <= 0 {
		return apierrors.ErrDeleteSonarMetricRules.InvalidParameter("ID").ToResp(), nil
	}
	if ruleID <= 0 {
		return apierrors.ErrDeleteSonarMetricRules.InvalidParameter("ID").ToResp(), nil
	}
	request.ID = ruleID

	if err := checkScopeTypeAndID(request.ScopeType, request.ScopeID); err != nil {
		return nil, err
	}

	resp, err := e.sonarMetricRule.Delete(&request)
	if err != nil {
		return apierrors.ErrDeleteSonarMetricRules.InternalError(err).ToResp(), nil
	}

	return resp, nil
}

func (e *Endpoints) QuerySonarMetricRules(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	request := apistructs.SonarMetricRulesListRequest{}

	if err := e.queryStringDecoder.Decode(&request, r.URL.Query()); err != nil {
		return apierrors.ErrQuerySonarMetricRules.InvalidParameter(err).ToResp(), nil
	}

	if err := checkScopeTypeAndID(request.ScopeType, request.ScopeID); err != nil {
		return nil, err
	}

	resp, err := e.sonarMetricRule.QueryMetricKeys(&request)
	if err != nil {
		return apierrors.ErrQuerySonarMetricRules.InternalError(err).ToResp(), nil
	}

	return resp, nil
}

func (e *Endpoints) QuerySonarMetricRulesDefinition(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	request := apistructs.SonarMetricRulesDefinitionListRequest{}

	if err := e.queryStringDecoder.Decode(&request, r.URL.Query()); err != nil {
		return apierrors.ErrQuerySonarMetricRuleDefinitions.InvalidParameter(err).ToResp(), nil
	}

	if err := checkScopeTypeAndID(request.ScopeType, request.ScopeID); err != nil {
		return nil, err
	}

	locale := e.bdl.GetLocaleByRequest(r)
	resp, err := e.sonarMetricRule.QueryMetricDefinition(&request, locale)
	if err != nil {
		return apierrors.ErrQuerySonarMetricRuleDefinitions.InternalError(err).ToResp(), nil
	}

	return resp, nil
}

func checkScopeTypeAndID(scopeType, scopeID string) error {
	if scopeType != apistructs.ProjectScopeType {
		return fmt.Errorf("missing params scopeType")
	}

	if len(scopeID) <= 0 {
		return fmt.Errorf("missing params scopeId")
	}

	return nil
}
