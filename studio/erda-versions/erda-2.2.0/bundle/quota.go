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

package bundle

import (
	"context"
	"fmt"
	"net/url"
	"strconv"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// FetchQuotaOnClusters
// orgID if is 0, fetch from all organizations. // todo:
func (b *Bundle) FetchQuotaOnClusters(orgID uint64, clusterNames []string) (*apistructs.GetQuotaOnClustersResponse, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	type response struct {
		apistructs.Header
		Data *apistructs.GetQuotaOnClustersResponse
	}
	var (
		resp   response
		params = make(url.Values)
	)
	for _, clusterName := range clusterNames {
		params.Add("clusterName", clusterName)
	}
	httpResp, err := hc.Get(host).
		Path(fmt.Sprintf("/api/projects-quota")).
		Params(params).
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrListFileRecord.InternalError(err)
	}
	if !httpResp.IsOK() {
		return nil, toAPIError(httpResp.StatusCode(), resp.Error)
	}
	return resp.Data, nil
}

// FetchNamespacesBelongsTo finds the project to which a given namespaces belongs to.
// if orgID == 0, query from all scope.
// namespaces: the key is cluster name, the value is the namespaces list in the cluster.
func (b *Bundle) FetchNamespacesBelongsTo(ctx context.Context, orgID uint64, clusters []string) (*apistructs.GetProjectsNamesapcesResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	lang := "zh-CN"
	if langCodes, _ := ctx.Value("lang_codes").(i18n.LanguageCodes); len(langCodes) > 0 {
		lang = langCodes[0].String()
	}

	var params = make(url.Values)
	for _, cluster := range clusters {
		params.Add("clusterName", cluster)
	}

	type response struct {
		apistructs.Header
		Data *apistructs.GetProjectsNamesapcesResponseData
	}
	var resp response
	httpResp, err := hc.Get(host).
		Header("lang", lang).
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		Params(params).
		Path("/api/projects-namespaces").
		Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrListFileRecord.InternalError(err)
	}
	if !httpResp.IsOK() {
		return nil, toAPIError(httpResp.StatusCode(), resp.Error)
	}
	return resp.Data, nil
}

func (b *Bundle) ListQuotaRecords() ([]*apistructs.ProjectQuota, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	type response struct {
		apistructs.Header
		Data struct {
			Total uint64                     `json:"total"`
			List  []*apistructs.ProjectQuota `json:"list"`
		}
	}
	var resp response
	httpResp, err := b.hc.Get(host).Path("/api/quota-records").Do().JSON(&resp)
	if err != nil {
		return nil, err
	}
	if !httpResp.IsOK() {
		return nil, toAPIError(httpResp.StatusCode(), resp.Error)
	}
	return resp.Data.List, nil
}
