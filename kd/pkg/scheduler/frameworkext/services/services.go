/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package services

import (
	"net/http"
	"sort"
	"sync"
	"sync/atomic"

	"github.com/gin-gonic/gin"
	"k8s.io/kubernetes/pkg/scheduler"

	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
)

const (
	servicesBaseRelativePath       = "/apis/v1/"
	pluginServicesBaseRelativePath = servicesBaseRelativePath + "plugins"
)

var once sync.Once
var engine atomic.Value

type mux interface {
	Handle(string, http.Handler)
	HandlePrefix(string, http.Handler)
}

func InstallAPIHandler(mux mux, e *Engine, sched *scheduler.Scheduler, isLeader func() bool) {
	mux.HandlePrefix(servicesBaseRelativePath, handle(isLeader))

	once.Do(func() {
		engine.Store(e)
		baseGroup := e.Group(servicesBaseRelativePath)
		baseGroup.GET("/nodes/:nodeName", queryNodeInfo(sched))
		baseGroup.GET("/__services__", listRegisteredServices(e.Engine))
	})
}

func handle(isLeader func() bool) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if !isLeader() {
			w.WriteHeader(http.StatusServiceUnavailable)
			w.Write([]byte(`{"message": "the instance is not leader"}`))
			return
		}

		e, ok := engine.Load().(*Engine)
		if !ok {
			http.Error(w, "The component is initializing and temporarily unavailable", http.StatusServiceUnavailable)
			return
		}
		e.ServeHTTP(w, r)
	}
}

type Engine struct {
	*gin.Engine
}

func NewEngine(e *gin.Engine) *Engine {
	return &Engine{
		Engine: e,
	}
}

func (e *Engine) RegisterPluginService(plugin framework.Plugin) {
	if serviceProvider, ok := plugin.(APIServiceProvider); ok {
		baseGroup := e.Engine.Group(pluginServicesBaseRelativePath)
		pluginServiceGroup := baseGroup.Group(plugin.Name())
		serviceProvider.RegisterEndpoints(pluginServiceGroup)
	}
}

func listRegisteredServices(e *gin.Engine) gin.HandlerFunc {
	return func(context *gin.Context) {
		routes := e.Routes()
		services := map[string][]string{}
		for _, v := range routes {
			services[v.Method] = append(services[v.Method], v.Path)
		}
		for _, paths := range services {
			sort.Strings(paths)
		}
		context.JSON(http.StatusOK, services)
	}
}

func queryNodeInfo(sched *scheduler.Scheduler) gin.HandlerFunc {
	return func(context *gin.Context) {
		nodeName := context.Param("nodeName")
		dump := sched.SchedulerCache.Dump()
		nodeInfo := dump.Nodes[nodeName]
		if nodeInfo == nil || nodeInfo.Node() == nil {
			ResponseErrorMessage(context, http.StatusNotFound, "cannot find node %s", nodeName)
			return
		}
		n := convertNodeInfo(nodeInfo)
		context.JSON(http.StatusOK, n)
	}
}
