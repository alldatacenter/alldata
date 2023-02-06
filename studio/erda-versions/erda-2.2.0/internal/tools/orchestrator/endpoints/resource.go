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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// 获取项目资源信息，包括service和addon
func (e *Endpoints) GetProjectResource(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 校验 body 合法性
	var projectIDs []uint64
	if err := json.NewDecoder(r.Body).Decode(&projectIDs); err != nil {
		return apierrors.ErrProjectResource.InvalidParameter(err).ToResp(), nil
	}
	resourceMap := map[uint64]apistructs.ProjectResourceItem{}
	if len(projectIDs) == 0 {
		return httpserver.OkResp(resourceMap)
	}
	logrus.Infof("get project resource with project ids: %+v", projectIDs)

	serviceResource, err := e.resource.GetProjectServiceResource(projectIDs)
	if err != nil {
		return apierrors.ErrProjectResource.InvalidParameter(err).ToResp(), nil
	}
	addonResource, err := e.resource.GetProjectAddonResource(projectIDs)
	if err != nil {
		return apierrors.ErrProjectResource.InvalidParameter(err).ToResp(), nil
	}
	for _, v := range projectIDs {

		addonResourceItem := (*addonResource)[v]
		serviceResourceItem := (*serviceResource)[v]
		resourceMap[v] = apistructs.ProjectResourceItem{
			MemServiceUsed: serviceResourceItem.MemServiceUsed,
			CpuServiceUsed: serviceResourceItem.CpuServiceUsed,
			MemAddonUsed:   addonResourceItem.MemAddonUsed,
			CpuAddonUsed:   addonResourceItem.CpuAddonUsed,
		}
	}
	return httpserver.OkResp(resourceMap)

}

// 获取集群中service和addon的数量
func (e *Endpoints) GetClusterResourceReference(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 校验 body 合法性
	clusterName := r.URL.Query().Get("clusterName")
	if clusterName == "" {
		return apierrors.ErrClusterResource.MissingParameter("clusterName").ToResp(), nil
	}
	orgID := r.URL.Query().Get("orgId")
	if orgID == "" {
		return apierrors.ErrClusterResource.MissingParameter("orgID").ToResp(), nil
	}
	addonRef, err := e.db.CountAddonReferenceByClusterAndOrg(clusterName, orgID)
	if err != nil {
		return apierrors.ErrClusterResource.InternalError(err).ToResp(), nil
	}
	serviceRef, err := e.db.CountServiceReferenceByClusterAndOrg(clusterName, orgID)
	if err != nil {
		return apierrors.ErrClusterResource.InternalError(err).ToResp(), nil
	}
	logrus.Infof("cluster resource ref, addon: %d, service: %d", addonRef, serviceRef)
	return httpserver.OkResp(apistructs.ResourceReferenceData{
		AddonReference:   int64(addonRef),
		ServiceReference: int64(serviceRef),
	})

}
