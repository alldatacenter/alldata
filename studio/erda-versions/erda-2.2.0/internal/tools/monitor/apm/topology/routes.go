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

package topology

import (
	"net/http"
	"time"

	"github.com/erda-project/erda-infra/providers/httpserver"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (topology *provider) initRoutes(routes httpserver.Router) error {

	routes.GET("/api/apm/topology/search/tags", topology.searchTags)
	routes.GET("/api/apm/topology/search/tagv", topology.searchTagv)
	routes.GET("/api/apm/topology", topology.topology)
	routes.GET("/api/apm/topology/overview", topology.overview)
	routes.GET("/api/apm/topology/services", topology.services)
	routes.GET("/api/apm/topology/service/overview", topology.serviceOverview)
	routes.GET("/api/apm/topology/service/requests", topology.serviceRequest)
	routes.GET("/api/apm/topology/service/instances", topology.serviceInstances)
	routes.GET("/api/apm/topology/service/instance/ids", topology.serviceInstanceIds)
	routes.GET("/api/apm/topology/process/diskio", topology.processDiskIo)
	routes.GET("/api/apm/topology/process/netio", topology.processNetIo)
	routes.GET("/api/apm/topology/exception/message", topology.exceptionMessage)
	routes.GET("/api/apm/topology/exception/types", topology.exceptionTypes)
	routes.GET("/api/apm/topology/translation", topology.translation)
	routes.GET("/api/apm/topology/translation/db", topology.middlewareTransaction)
	routes.GET("/api/apm/topology/translation/slow", topology.slowTranslationTrace)
	return nil
}

type ServicesVo struct {
	StartTime   int64               `query:"start" validate:"required"`
	EndTime     int64               `query:"end" validate:"required"`
	TerminusKey string              `query:"terminusKey" validate:"required"`
	Tags        map[string][]string `query:"tags"`
}

type GlobalParams struct {
	Scope     string `query:"scope" default:"micro_service"`
	ScopeId   string `query:"terminusKey" validate:"required"`
	StartTime int64  `query:"start" validate:"required"`
	EndTime   int64  `query:"end" validate:"required"`
}

type ProcessParams struct {
	TerminusKey string `query:"terminusKey" validate:"required"`
	ServiceName string `query:"serviceName" validate:"required"`
}

type ServiceParams struct {
	Scope       string `query:"scope" default:"micro_service"`
	ScopeId     string `query:"terminusKey" validate:"required"`
	StartTime   int64  `query:"start" validate:"required"`
	EndTime     int64  `query:"end" validate:"required"`
	ServiceName string `query:"serviceName" validate:"required"`
	ServiceId   string `query:"serviceId" validate:"required"`
	InstanceId  string `query:"instanceId"`
}

type translation struct {
	Start             int64  `query:"start" validate:"required"`
	End               int64  `query:"end" validate:"required"`
	Limit             int64  `query:"limit" default:"20"`
	Search            string `query:"search"`
	Layer             string `query:"layer" validate:"required"`
	FilterServiceName string `query:"filterServiceName" validate:"required"`
	TerminusKey       string `query:"terminusKey" validate:"required"`
	Sort              int64  `query:"sort"`
	ServiceId         string `query:"serviceId" validate:"required"`
	Type              string `query:"type"`
}

func (topology *provider) exceptionTypes(r *http.Request, params ServiceParams) interface{} {
	types, err := topology.GetExceptionTypes(api.Language(r), params)
	if err != nil {
		return api.Errors.Internal(err)
	}

	return api.Success(map[string]interface{}{
		"data": types,
	})
}

func (topology *provider) processDiskIo(r *http.Request, params ServiceParams) interface{} {
	processDiskIo, err := topology.GetProcessDiskIo(api.Language(r), params)
	if err != nil {
		return api.Errors.Internal(err)
	}

	return api.Success(map[string]interface{}{
		"data": processDiskIo,
	})
}

func (topology *provider) processNetIo(r *http.Request, params ServiceParams) interface{} {
	processNetIo, err := topology.GetProcessDiskIo(api.Language(r), params)
	if err != nil {
		return api.Errors.Internal(err)
	}

	return api.Success(map[string]interface{}{
		"data": processNetIo,
	})
}

func (topology *provider) exceptionMessage(r *http.Request, params ServiceParams, exceptionParams struct {
	Limit         int64  `query:"limit"`
	Sort          string `query:"sort"`
	ExceptionType string `query:"exceptionType"`
}) interface{} {
	exceptionMessages, err := topology.GetExceptionMessage(api.Language(r), params, exceptionParams.Limit, exceptionParams.Sort, exceptionParams.ExceptionType)
	if err != nil {
		return api.Errors.Internal(err)
	}

	return api.Success(map[string]interface{}{
		"data": exceptionMessages,
	})
}

func (topology *provider) serviceInstanceIds(r *http.Request, params ServiceParams) interface{} {
	serviceInstanceIds, err := topology.GetServiceInstanceIds(api.Language(r), params)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(map[string]interface{}{
		"data": serviceInstanceIds,
	})
}

func (topology *provider) serviceInstances(r *http.Request, params ServiceParams) interface{} {
	reqTranslations, err := topology.GetServiceInstances(api.Language(r), params)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(map[string]interface{}{
		"data": reqTranslations,
	})
}

func (topology *provider) serviceRequest(r *http.Request, params ServiceParams) interface{} {
	reqTranslations, err := topology.GetServiceRequest(api.Language(r), params)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(map[string]interface{}{
		"data": reqTranslations,
	})
}

func (topology *provider) serviceOverview(r *http.Request, params ServiceParams) interface{} {
	overview, err := topology.GetServiceOverview(api.Language(r), params)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(map[string]interface{}{
		"data": overview,
	})
}

func (topology *provider) overview(r *http.Request, params GlobalParams) interface{} {
	overview, err := topology.GetOverview(api.Language(r), params)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(overview)
}

func (topology *provider) searchTags(r *http.Request, params struct {
	TerminusKey string `query:"terminusKey" validate:"required"`
}) interface{} {
	return api.Success(topology.GetSearchTags(r))
}

func (topology *provider) searchTagv(r *http.Request, params struct {
	TerminusKey string `query:"terminusKey" validate:"required"`
	StartTime   int64  `query:"startTime" validate:"required"`
	EndTime     int64  `query:"endTime" validate:"required"`
	Tag         string `query:"tag" validate:"required"`
}) interface{} {
	// default get time: 1 hour.
	if params.EndTime == 0 {
		params.EndTime = time.Now().UnixNano() / 1e6
		h, _ := time.ParseDuration("-1h")
		params.StartTime = time.Now().Add(h).UnixNano() / 1e6
	}

	tagv, err := topology.GetSearchTagv(r, params.Tag, params.TerminusKey, params.StartTime, params.EndTime)
	if err != nil {
		api.Errors.Internal(err)
	}
	return api.Success(tagv)
}

func (topology *provider) topology(r *http.Request, params Vo) interface{} {
	// default get time: 1 hour.
	if params.EndTime == 0 {
		params.EndTime = time.Now().UnixNano() / 1e6
		h, _ := time.ParseDuration("-1h")
		params.StartTime = time.Now().Add(h).UnixNano() / 1e6
	}

	now := time.Now().UnixNano() / 1e6
	before7dayMs := time.Now().AddDate(0, 0, -7).UnixNano() / 1e6 // ms

	if params.StartTime < before7dayMs {
		params.StartTime = before7dayMs
	}
	if params.EndTime > now {
		params.EndTime = now
	}

	if params.StartTime >= params.EndTime {
		return api.Success(nil)
	}

	nodes, err := topology.ComposeTopologyNode(r, params)
	if err != nil {
		return api.Errors.Internal(err)
	}

	tr := Response{
		Nodes: nodes,
	}

	return api.Success(tr)
}

func (topology *provider) services(r *http.Request, params struct {
	ScopeId     string `query:"terminusKey" validate:"required"`
	StartTime   int64  `query:"start" validate:"required"`
	EndTime     int64  `query:"end" validate:"required"`
	ServiceName string `query:"serviceName"`
}) interface{} {
	// default get time: 1 hour.
	if params.EndTime == 0 {
		params.EndTime = time.Now().UnixNano() / 1e6
		h, _ := time.ParseDuration("-1h")
		params.StartTime = time.Now().Add(h).UnixNano() / 1e6
	}

	now := time.Now().UnixNano() / 1e6
	before7dayMs := time.Now().AddDate(0, 0, -7).UnixNano() / 1e6 // ms

	if params.StartTime < before7dayMs {
		params.StartTime = before7dayMs
	}
	if params.EndTime > now {
		params.EndTime = now
	}

	if params.StartTime >= params.EndTime {
		return api.Success(nil)
	}

	var topologyParams Vo
	topologyParams.StartTime = params.StartTime
	topologyParams.EndTime = params.EndTime
	topologyParams.TerminusKey = params.ScopeId
	nodes, err := topology.ComposeTopologyNode(r, topologyParams)
	if err != nil {
		return api.Errors.Internal(err)
	}

	services := topology.Services(params.ServiceName, nodes)
	return api.Success(map[string]interface{}{
		"data": services,
	})
}
