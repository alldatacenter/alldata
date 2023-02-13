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
	"io/ioutil"
	"net/http"
	"strconv"

	"github.com/sirupsen/logrus"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cap"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/instanceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/job"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/labelmanager"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/resourceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/terminal"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/volume"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

type HTTPEndpoints struct {
	volumeImpl        volume.Volume
	ServiceGroupImpl  servicegroup.ServiceGroup
	clusterImpl       cluster.Cluster
	Job               job.Job
	labelManager      labelmanager.LabelManager
	instanceinfoImpl  instanceinfo.InstanceInfo
	ClusterinfoImpl   clusterinfo.ClusterInfo
	componentinfoImpl instanceinfo.ComponentInfo
	resourceinfoImpl  resourceinfo.ResourceInfo
	Cap               cap.Cap
	//metric            metric.Metric
	clusterSvc clusterpb.ClusterServiceServer
	// TODO: add more impl here
}

func NewHTTPEndpoints(
	volume volume.Volume,
	servicegroup servicegroup.ServiceGroup,
	cluster cluster.Cluster,
	job job.Job,
	labelManager labelmanager.LabelManager,
	instanceinfo instanceinfo.InstanceInfo,
	clusterinfo clusterinfo.ClusterInfo,
	componentinfo instanceinfo.ComponentInfo,
	resourceinfo resourceinfo.ResourceInfo,
	cap cap.Cap,
	clusterSvc clusterpb.ClusterServiceServer) *HTTPEndpoints {
	return &HTTPEndpoints{
		volume,
		servicegroup,
		cluster,
		job,
		labelManager,
		instanceinfo,
		clusterinfo,
		componentinfo,
		resourceinfo,
		cap,
		clusterSvc,
	}
}

const (
	RetainNamespace = "RETAIN_NAMESPACE"
)

func (h *HTTPEndpoints) ClusterHook(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	req := apistructs.ClusterEvent{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("decode clusterhook request fail: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{Status: http.StatusBadRequest, Content: errstr}, nil
	}
	if err := h.clusterImpl.Hook(&req); err != nil {
		errstr := fmt.Sprintf("failed to handle cluster event: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{Status: http.StatusInternalServerError, Content: errstr}, nil
	}
	return httpserver.HTTPResponse{Status: http.StatusOK}, nil
}

func (h *HTTPEndpoints) LabelList(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	return mkResponse(apistructs.ScheduleLabelListResponse{
		Header: apistructs.Header{Success: true},
		Data:   apistructs.ScheduleLabelListData{Labels: h.labelManager.List()},
	})
}

// {"force":false,"tag":"any,workspace-dev,workspace-test,workspace-staging,workspace-prod,job,service-stateful,org-terminus","hosts":["10.168.0.100"]}
func (h *HTTPEndpoints) SetNodeLabels(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	//h.metric.TotalCounter.WithLabelValues(metric.LableTotal).Add(1)
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		errstr := "Invalid request body"
		logrus.Error(errstr)
		return mkResponse(apistructs.ScheduleLabelSetResponse{
			Header: apistructs.Header{
				Success: false,
				Error: apistructs.ErrorResponse{
					Msg: errstr,
				}},
		})
	}
	var reqBody apistructs.ScheduleLabelSetRequest
	if err := json.Unmarshal(body, &reqBody); err != nil {
		errstr := fmt.Sprintf("failed to unmarshal request body: %v", err)
		logrus.Error(errstr)
		return mkResponse(apistructs.ScheduleLabelSetResponse{
			Header: apistructs.Header{
				Success: false,
				Error: apistructs.ErrorResponse{
					Msg: errstr,
				},
			},
		})
	}

	if err := h.labelManager.SetNodeLabel(labelmanager.Cluster{
		ClusterName: reqBody.ClusterName,
		ClusterType: reqBody.ClusterType,
		SoldierURL:  reqBody.SoldierURL,
	}, reqBody.Hosts, reqBody.Tags); err != nil {
		//h.metric.ErrorCounter.WithLabelValues(metric.LableError).Add(1)
		errstr := fmt.Sprintf("failed to set node labels: %v", err)
		logrus.Error(errstr)
		return mkResponse(apistructs.ScheduleLabelSetResponse{
			Header: apistructs.Header{
				Success: false,
				Error: apistructs.ErrorResponse{
					Msg: errstr,
				},
			},
		})
	}
	return mkResponse(apistructs.ScheduleLabelSetResponse{
		Header: apistructs.Header{
			Success: true,
		},
	})
}

func (h *HTTPEndpoints) InstanceInfo(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	cond := instanceinfo.QueryInstanceConditions{}
	cond.Cluster = r.URL.Query().Get("cluster")
	cond.OrgName = r.URL.Query().Get("orgName")
	cond.OrgID = r.URL.Query().Get("orgID")
	cond.ProjectName = r.URL.Query().Get("projectName")
	cond.ProjectID = r.URL.Query().Get("projectID")
	cond.ApplicationName = r.URL.Query().Get("applicationName")
	cond.EdgeApplicationName = r.URL.Query().Get("edgeApplicationName")
	cond.EdgeSite = r.URL.Query().Get("edgeSite")
	cond.ApplicationID = r.URL.Query().Get("applicationID")
	cond.RuntimeName = r.URL.Query().Get("runtimeName")
	cond.RuntimeID = r.URL.Query().Get("runtimeID")
	cond.ServiceName = r.URL.Query().Get("serviceName")
	cond.Workspace = r.URL.Query().Get("workspace")
	cond.ContainerID = r.URL.Query().Get("containerID")
	cond.InstanceIP = r.URL.Query().Get("instanceIP")
	cond.HostIP = r.URL.Query().Get("hostIP")
	cond.ServiceType = r.URL.Query().Get("serviceType")
	cond.AddonID = r.URL.Query().Get("addonID")
	phases := r.URL.Query().Get("phases")
	if phases != "" {
		cond.Phases = strutil.Map(strutil.Split(phases, ",", true), strutil.ToLower, strutil.Title)
	}
	limitstr := r.URL.Query().Get("limit")
	if limitstr == "" {
		cond.Limit = 0
	} else {
		limit, err := strconv.Atoi(limitstr)
		if err != nil {
			errstr := fmt.Sprintf("failed to parse limit query condition: %v", err)
			logrus.Error(errstr)
			return mkResponse(apistructs.InstanceInfoResponse{
				Header: apistructs.Header{
					Success: false,
					Error: apistructs.ErrorResponse{
						Msg: errstr,
					},
				},
			})
		}
		cond.Limit = limit
	}
	instances, err := h.instanceinfoImpl.QueryInstance(cond)
	if err != nil {
		errstr := fmt.Sprintf("failed to query instance info: %v", err)
		logrus.Error(errstr)
		return mkResponse(apistructs.InstanceInfoResponse{
			Header: apistructs.Header{
				Success: false,
				Error: apistructs.ErrorResponse{
					Msg: errstr,
				},
			},
		})
	}

	return mkResponse(apistructs.InstanceInfoResponse{
		Header: apistructs.Header{Success: true},
		Data:   instances,
	})
}

// ClusterInfo 获取集群信息
func (h *HTTPEndpoints) ClusterInfo(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	name := vars["clusterName"]
	if name == "" {
		errstr := fmt.Sprintf("empty cluster name")
		return mkResponse(apistructs.ClusterInfoResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}

	info, err := h.ClusterinfoImpl.Info(name)
	if err != nil {
		errstr := fmt.Sprintf("failed to get cluster info, clusterName: %s, (%v)", name, err)
		return mkResponse(apistructs.ClusterInfoResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}
	return mkResponse(apistructs.ClusterInfoResponse{
		apistructs.Header{Success: true},
		info,
	})
}

func (h *HTTPEndpoints) ResourceInfo(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	name := vars["clusterName"]
	if name == "" {
		errstr := fmt.Sprintf("empty cluster name")
		return mkResponse(apistructs.ClusterResourceInfoResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}
	brief := false
	briefArg := r.URL.Query().Get("brief")
	if briefArg != "" {
		brief = true
	}
	data, err := h.resourceinfoImpl.Info(name, brief)
	if err != nil {
		errstr := fmt.Sprintf("failed to get resourceinfo: %v", err)
		return mkResponse(apistructs.ClusterResourceInfoResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}

	return mkResponse(apistructs.ClusterResourceInfoResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: data,
	})
}

func (h *HTTPEndpoints) Terminal(ctx context.Context, w http.ResponseWriter, r *http.Request, vars map[string]string) error {
	terminal.Terminal(h.clusterSvc, w, r)
	return nil
}

func (h *HTTPEndpoints) CapacityInfo(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	clustername := r.URL.Query().Get("clusterName")
	data := h.Cap.CapacityInfo(clustername)
	return mkResponse(apistructs.CapacityInfoResponse{
		Header: apistructs.Header{Success: true},
		Data:   data,
	})
}

func mkResponse(content interface{}) (httpserver.Responser, error) {
	return httpserver.HTTPResponse{
		Status:  http.StatusOK,
		Content: content,
	}, nil
}
