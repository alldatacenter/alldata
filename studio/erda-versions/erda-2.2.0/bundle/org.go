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
	"fmt"
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// GetOrg get org by id from core-service.
func (b *Bundle) GetOrg(idOrName interface{}) (*apistructs.OrgDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.OrgFetchResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/orgs/%v", idOrName)).Header(httputil.InternalHeader, "bundle").Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return &fetchResp.Data, nil
}

// GetDopOrg get org by id from dop
func (b *Bundle) GetDopOrg(idOrName interface{}) (*apistructs.OrgDTO, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.OrgFetchResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/orgs/%v", idOrName)).Header(httputil.InternalHeader, "bundle").Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return &fetchResp.Data, nil
}

// ListOrgs 分页查询企业
func (b *Bundle) ListOrgs(req *apistructs.OrgSearchRequest, orgID string) (*apistructs.PagingOrgDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgSearchResponse
	r, err := hc.Get(host).Path("/api/orgs").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, req.UserID).
		Header(httputil.OrgHeader, orgID).
		Param("q", req.Q).
		Param("org", req.Org).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}

// ListDopOrgs 分页查询企业
func (b *Bundle) ListDopOrgs(req *apistructs.OrgSearchRequest) (*apistructs.PagingOrgDTO, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgSearchResponse
	r, err := hc.Get(host).Path("/api/orgs").
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", req.UserID).
		Param("q", req.Q).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}

// ListPublicOrgs get list of public orgs
func (b *Bundle) ListPublicOrgs(req *apistructs.OrgSearchRequest) (*apistructs.PagingOrgDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgSearchResponse
	r, err := hc.Get(host).Path("/api/orgs/actions/list-public").
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", req.UserID).
		Param("q", req.Q).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}

// ListDopPublicOrgs get list of public orgs form dop
func (b *Bundle) ListDopPublicOrgs(req *apistructs.OrgSearchRequest) (*apistructs.PagingOrgDTO, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgSearchResponse
	r, err := hc.Get(host).Path("/api/orgs/actions/list-public").
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", req.UserID).
		Param("q", req.Q).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}

// GetOrgByDomain 通过域名获取企业
func (b *Bundle) GetOrgByDomain(domain, orgName, userID string) (*apistructs.OrgDTO, error) {
	// TODO: userID should be deprecated
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgGetByDomainResponse
	r, err := hc.Get(host).Path("/api/orgs/actions/get-by-domain").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID). // TODO: for compatibility
		Param("domain", domain).
		Param("orgName", orgName).
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return resp.Data, nil
}

// GetDopOrgByDomain 通过域名获取企业
func (b *Bundle) GetDopOrgByDomain(domain string, userID string) (*apistructs.OrgDTO, error) {
	// TODO: userID should be deprecated
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgGetByDomainResponse
	r, err := hc.Get(host).Path("/api/orgs/actions/get-by-domain").
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", userID). // TODO: for compatibility
		Param("domain", domain).
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return resp.Data, nil
}

// CreateOrg create org
func (b *Bundle) CreateOrg(userID string, req *apistructs.OrgCreateRequest) (*apistructs.OrgDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgCreateResponse
	r, err := hc.Post(host).Path("/api/orgs").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		JSONBody(req).Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}

// CreateDopOrg create org
func (b *Bundle) CreateDopOrg(userID string, req *apistructs.OrgCreateRequest) (*apistructs.OrgDTO, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgCreateResponse
	r, err := hc.Post(host).Path("/api/orgs").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		JSONBody(req).Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}

// UpdateOrg update org
func (b *Bundle) UpdateOrg(userID string, orgID int64, req *apistructs.OrgUpdateRequestBody) (*apistructs.OrgDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgUpdateResponse
	r, err := hc.Put(host).Path(fmt.Sprintf("/api/orgs/%d", orgID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		JSONBody(req).Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}

// DeleteOrg delete org by id from core-service.
func (b *Bundle) DeleteOrg(idOrName interface{}) (*apistructs.OrgDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgDeleteResponse
	r, err := hc.Delete(host).Path(fmt.Sprintf("/api/orgs/%v", idOrName)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}

	return &resp.Data, nil
}

// GetOrgClusterRelationsByOrg get orgClusters relation by orgID
func (b *Bundle) GetOrgClusterRelationsByOrg(orgID uint64) ([]apistructs.OrgClusterRelationDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgClusterRelationDTOResponse
	r, err := hc.Get(host).Path(fmt.Sprintf("/api/orgs/clusters/relations/%d", orgID)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}

	return resp.Data, nil
}

// ListOrgClusterRelation list all relations if cluster is empty, else list all relation related to this cluster
func (b *Bundle) ListOrgClusterRelation(userID, clusterName string) ([]apistructs.OrgClusterRelationDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.OrgClusterRelationDTOResponse
	r, err := hc.Get(host).Path("/api/orgs/clusters/relations").
		Header(httputil.UserHeader, userID).
		Param("cluster", clusterName).
		Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}

	return resp.Data, nil
}

// DereferenceCluster delete the relation of org and cluster
func (b *Bundle) DereferenceCluster(orgID uint64, clusterName, userID string, force bool) (string, error) {
	referenceResp, err := b.FindClusterResource(clusterName, strconv.FormatUint(orgID, 10))
	if err != nil {
		return "", err
	}
	if (referenceResp.AddonReference > 0 || referenceResp.ServiceReference > 0) && !force {
		return "", errors.Errorf("集群中存在未清理的Addon或Service，请清理后再执行.")
	}

	host, err := b.urls.CoreServices()
	if err != nil {
		return "", err
	}
	hc := b.hc

	var resp apistructs.DeleteOrgClusterRelationResponse
	r, err := hc.Put(host).Path("/api/clusters/actions/dereference").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Param("orgID", strconv.FormatUint(orgID, 10)).
		Param("clusterName", clusterName).
		Do().
		JSON(&resp)
	if err != nil {
		return "", apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return "", toAPIError(r.StatusCode(), resp.Error)
	}
	return resp.Data, nil
}

// CreateOrgClusterRelationsByOrg create orgClusters relation by orgID
func (b *Bundle) CreateOrgClusterRelationsByOrg(clusterName string, userID string, orgID uint64) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	org, err := b.GetOrg(orgID)
	if err != nil {
		return err
	}

	var createResp apistructs.OrgClusterRelationDTOCreateResponse

	req := &apistructs.OrgClusterRelationCreateRequest{
		OrgID:       orgID,
		OrgName:     org.Name,
		ClusterName: clusterName,
	}

	resp, err := hc.Post(host).Path("/api/orgs/actions/relate-cluster").
		Header(httputil.UserHeader, userID).
		JSONBody(req).
		Do().
		JSON(&createResp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createResp.Success {
		return toAPIError(resp.StatusCode(), createResp.Error)
	}

	return nil
}
