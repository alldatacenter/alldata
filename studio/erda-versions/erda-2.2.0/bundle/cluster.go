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
	"net/http"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// GetCluster 查询集群
func (b *Bundle) GetCluster(idOrName string) (*apistructs.ClusterInfo, error) {
	host, err := b.urls.ClusterManager()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var getResp apistructs.GetClusterResponse
	resp, err := hc.Get(host).Path(strutil.Concat("/api/clusters/", idOrName)).
		Header("Internal-Client", "bundle").
		Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return &getResp.Data, nil
}

// ListClusters 返回 org 下所有集群; 当 orgID == "" 时，返回所有集群.
func (b *Bundle) ListClusters(clusterType string, orgID ...uint64) ([]apistructs.ClusterInfo, error) {
	clusters, err := b.ListClustersWithType(clusterType)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if len(orgID) == 0 || (len(orgID) > 0 && orgID[0] == 0) {
		return clusters, nil
	}

	clusterRelation, err := b.GetOrgClusterRelationsByOrg(orgID[0])
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	clusterRes := make([]apistructs.ClusterInfo, 0)

	for _, dto := range clusterRelation {
		for _, cluster := range clusters {
			if uint64(cluster.ID) == dto.ClusterID {
				clusterRes = append(clusterRes, cluster)
				break
			}
		}
	}

	return clusterRes, nil
}

// ListClustersWithType List clusters with cluster type
func (b *Bundle) ListClustersWithType(clusterType string) ([]apistructs.ClusterInfo, error) {
	host, err := b.urls.ClusterManager()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	req := hc.Get(host).Path("/api/clusters").Header("Internal-Client", "bundle")

	if clusterType != "" {
		req.Param("clusterType", fmt.Sprintf("%s", clusterType))
	}

	var getResp apistructs.ClusterListResponse
	resp, err := req.Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}

	return getResp.Data, nil
}

// DeleteImageManifests 调用 officer 删除 registry image manifests, 真实镜像 blob 由 soldier 删除.
func (b *Bundle) DeleteImageManifests(clusterIDOrName string, images []string) (
	*apistructs.RegistryManifestsRemoveResponseData, error) {

	host, err := b.urls.CMP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var deleteResp apistructs.RegistryManifestsRemoveResponse
	resp, err := hc.Post(host).
		Path(strutil.Concat("/api/clusters/", clusterIDOrName, "/registry/manifests/actions/remove")).
		JSONBody(&apistructs.RegistryManifestsRemoveRequest{Images: images}).
		Do().JSON(&deleteResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !deleteResp.Success {
		return nil, toAPIError(resp.StatusCode(), deleteResp.Error)
	}

	return &deleteResp.Data, nil
}

// UpdateCluster update cluster
func (b *Bundle) UpdateCluster(req apistructs.ClusterUpdateRequest, header ...http.Header) error {
	host, err := b.urls.ClusterManager()
	if err != nil {
		return err
	}
	hc := b.hc

	var updateResp apistructs.ClusterUpdateResponse

	q := hc.Put(host).Path("/api/clusters").Header(httputil.InternalHeader, "bundle")
	if len(header) > 0 {
		q.Headers(header[0])
	}
	resp, err := q.JSONBody(req).
		Do().
		JSON(&updateResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !updateResp.Success {
		return toAPIError(resp.StatusCode(), updateResp.Error)
	}
	return nil
}

// CreateCluster Create cluster with event
func (b *Bundle) CreateCluster(req *apistructs.ClusterCreateRequest, header ...http.Header) error {
	host, err := b.urls.ClusterManager()
	if err != nil {
		return err
	}
	hc := b.hc

	var createResp apistructs.ClusterCreateResponse

	q := hc.Post(host).Path("/api/clusters").Header(httputil.InternalHeader, "bundle")
	if len(header) > 0 {
		q.Headers(header[0])
	}
	resp, err := q.JSONBody(req).
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

func (b *Bundle) CreateClusterWithOrg(userID string, orgID uint64, req *apistructs.ClusterCreateRequest, header ...http.Header) error {
	if err := b.CreateCluster(req, header...); err != nil {
		return err
	}

	if err := b.CreateOrgClusterRelationsByOrg(req.Name, userID, orgID); err != nil {
		return err
	}

	return nil
}

// PatchCluster patch cluster with event
func (b *Bundle) PatchCluster(req *apistructs.ClusterPatchRequest, header ...http.Header) error {
	host, err := b.urls.ClusterManager()
	if err != nil {
		return err
	}
	hc := b.hc

	q := hc.Patch(host).Path("/api/clusters")
	if len(header) > 0 {
		q.Headers(header[0])
	}

	var httpResp httpserver.Resp

	resp, err := q.JSONBody(req).Do().JSON(&httpResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	if !resp.IsOK() || !httpResp.Success {
		return toAPIError(resp.StatusCode(), httpResp.Err)
	}

	return nil
}

// DeleteCluster Delete cluster with event
func (b *Bundle) DeleteCluster(clusterName string, header ...http.Header) error {
	host, err := b.urls.ClusterManager()
	if err != nil {
		return err
	}
	hc := b.hc

	q := hc.Delete(host).Path("/api/clusters/" + clusterName)
	if len(header) > 0 {
		q.Headers(header[0])
	}

	var httpResp httpserver.Resp

	resp, err := q.Do().JSON(&httpResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	if !resp.IsOK() || !httpResp.Success {
		return toAPIError(resp.StatusCode(), httpResp.Err)
	}

	return nil
}

// GetRegistryAddress gets the address of the cluster inner image registry from the service cluster-manager
func (b *Bundle) GetRegistryAddress(name string) (string, error) {
	cluster, err := b.GetCluster(name)
	if err != nil {
		return "", err
	}
	if cluster == nil {
		return "", errors.New("failed to GetCluster: response data is nil")
	}
	if cluster.URLs == nil {
		return "", errors.New("failed to GetCluster: response data.URLs is nil")
	}
	address, ok := cluster.URLs["registry"]
	if !ok {
		return "", errors.New("failed to get registry address from cluster info: registry is not in data.URLs")
	}
	return address, nil
}
