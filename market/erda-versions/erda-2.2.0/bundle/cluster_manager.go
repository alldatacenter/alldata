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
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
)

func (b *Bundle) IsClusterManagerClientRegistered(clientType apistructs.ClusterManagerClientType, clusterKey string) (bool, error) {
	host, err := b.urls.ClusterDialer()
	if err != nil {
		return false, err
	}
	hc := b.hc

	var getResp bool
	resp, err := hc.Get(host).
		Path("/clusteragent/check").
		Param("clientType", clientType.String()).
		Param("clusterKey", clusterKey).
		Do().
		JSON(&getResp)
	if err != nil {
		return false, apierrors.ErrInvoke.InternalError(err)
	}
	if err := json.Unmarshal(resp.Body(), &getResp); err != nil {
		return false, err
	}
	return getResp, nil
}

func (b *Bundle) GetClusterManagerClientData(clientType apistructs.ClusterManagerClientType, clusterKey string) (apistructs.ClusterManagerClientDetail, error) {
	host, err := b.urls.ClusterDialer()
	if err != nil {
		return apistructs.ClusterManagerClientDetail{}, err
	}
	hc := b.hc

	var getResp apistructs.ClusterManagerClientDetail
	resp, err := hc.Get(host).
		Path(fmt.Sprintf("/clusteragent/client-detail/%s/%s", clientType, clusterKey)).
		Do().
		JSON(&getResp)
	if err != nil {
		return apistructs.ClusterManagerClientDetail{}, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apistructs.ClusterManagerClientDetail{}, apierrors.ErrInvoke.InternalError(fmt.Errorf("%s", resp.Body()))
	}
	return getResp, nil
}

func (b *Bundle) ListClusterManagerClientByType(clientType apistructs.ClusterManagerClientType) ([]apistructs.ClusterManagerClientDetail, error) {
	host, err := b.urls.ClusterManager()
	if err != nil {
		return []apistructs.ClusterManagerClientDetail{}, err
	}
	hc := b.hc

	var getResp []apistructs.ClusterManagerClientDetail
	resp, err := hc.Get(host).
		Path(fmt.Sprintf("/clusteragent/client-detail/%s", clientType)).
		Do().
		JSON(&getResp)
	if err != nil {
		return []apistructs.ClusterManagerClientDetail{}, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return []apistructs.ClusterManagerClientDetail{}, apierrors.ErrInvoke.InternalError(fmt.Errorf("%s", resp.Body()))
	}
	return getResp, nil
}
