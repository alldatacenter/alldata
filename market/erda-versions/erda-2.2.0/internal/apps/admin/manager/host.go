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

package manager

import (
	"context"
	"net/http"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/admin/apierrors"
	"github.com/erda-project/erda/internal/apps/admin/model"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (am *AdminManager) AppendHostEndpoint() {
	am.endpoints = append(am.endpoints, []httpserver.Endpoint{
		{Path: "/api/hosts/{host}", Method: http.MethodGet, Handler: am.GetHost},
	}...)
}

func (am *AdminManager) GetHost(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrGetHost.NotLogin().ToResp(), nil
	}
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetHost.InvalidParameter(err).ToResp(), nil
	}

	clusterName := r.URL.Query().Get("clusterName")
	if clusterName == "" {
		return apierrors.ErrGetHost.MissingParameter("clusterName").ToResp(), nil
	}

	orgObj, err := am.bundle.GetOrg(orgID)
	if err != nil {
		return apierrors.ErrGetOrg.InternalError(err).ToResp(), nil
	}

	addr := vars["host"]
	if addr == "" {
		return apierrors.ErrGetHost.MissingParameter("host").ToResp(), nil
	}

	host, err := am.getByClusterAndIP(clusterName, addr)
	if err != nil {
		return apierrors.ErrGetHost.InternalError(err).ToResp(), nil
	}
	if host == nil {
		return apierrors.ErrGetHost.NotFound().ToResp(), nil
	}

	// validate host org name and org name is equal
	if !strings.Contains(host.OrgName, orgObj.Name) {
		return apierrors.ErrGetHost.NotFound().ToResp(), nil
	}

	return httpserver.OkResp(host)
}

func (am *AdminManager) getByClusterAndIP(clusterName, privateAddr string) (*apistructs.Host, error) {
	host, err := am.db.GetHostByClusterAndIP(clusterName, privateAddr)
	if err != nil {
		return nil, err
	}
	return composeHostFromModel(host), nil
}

func composeHostFromModel(host *model.Host) *apistructs.Host {
	if host == nil {
		return nil
	}
	return &apistructs.Host{
		Name:          host.Name,
		OrgName:       host.OrgName,
		PrivateAddr:   host.PrivateAddr,
		Cpus:          host.Cpus,
		CpuUsage:      host.CpuUsage,
		Memory:        host.Memory,
		MemoryUsage:   host.MemoryUsage,
		Disk:          host.Disk,
		DiskUsage:     host.DiskUsage,
		Load5:         host.Load5,
		Cluster:       host.Cluster,
		Labels:        convertLegacyLabel(host.Labels),
		OS:            host.OS,
		KernelVersion: host.KernelVersion,
		SystemTime:    host.SystemTime,
		Birthday:      host.Birthday,
		TimeStamp:     host.TimeStamp,
		Deleted:       host.Deleted,
	}
}

// convertLegacyLabel compatible the data of marathon and the old labels will same as new
func convertLegacyLabel(labels string) string {
	labelSlice := strings.Split(labels, ",")
	newLabels := make([]string, 0, len(labelSlice))
	for _, v := range labelSlice {
		switch v {
		case "pack":
			newLabels = append(newLabels, "pack-job")
		case "bigdata":
			newLabels = append(newLabels, "bigdata-job")
		case "stateful", "service-stateful":
			newLabels = append(newLabels, "stateful-service")
		case "stateless", "service-stateless":
			newLabels = append(newLabels, "stateless-service")
		default:
			newLabels = append(newLabels, v)
		}
	}

	return strings.Join(newLabels, ",")
}
