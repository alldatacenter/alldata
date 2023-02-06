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

package edgepipeline_register

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"
	"time"

	"github.com/mohae/deepcopy"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

var (
	// center pipeline could receive edge event after edge side pipeline registered
	EdgeHookApiPath = "/api/pipeline-edge/actions/hook"
)

type EventHandler func(ctx context.Context, eventDetail apistructs.ClusterManagerClientDetail)

func (p *provider) registerClientHook(ctx context.Context) error {
	ev := apistructs.CreateHookRequest{
		Name:   "pipeline_watch_edge_client_changed",
		Events: []string{apistructs.ClusterManagerClientTypePipeline.GenEventName(apistructs.ClusterManagerClientEventRegister)},
		URL:    strutil.Concat("http://", discover.Pipeline(), EdgeHookApiPath),
		Active: true,
		HookLocation: apistructs.HookLocation{
			Org:         "-1",
			Project:     "-1",
			Application: "-1",
		},
	}
	if err := p.bdl.CreateWebhook(ev); err != nil {
		return err
	}
	return nil
}

func (p *provider) registerClientHookUntilSuccess(ctx context.Context) {
	p.initClientHook(ctx)
	for {
		select {
		case <-ctx.Done():
			return
		default:
			err := p.registerClientHook(ctx)
			if err == nil {
				return
			}
			p.Log.Errorf("failed to register edge clients hook(auto retry), err: %v", err)
			time.Sleep(3 * time.Second)
		}
	}
}

func (p *provider) initClientHook(ctx context.Context) {
	p.Register.Add(http.MethodPost, EdgeHookApiPath, func(rw http.ResponseWriter, r *http.Request) {
		req := apistructs.ClusterManagerClientEvent{}
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			p.Log.Errorf("failed to decode request body, err: %v", err)
			httpserver.WriteErr(rw, strconv.FormatInt(int64(http.StatusBadRequest), 10), err.Error())
			return
		}
		p.Log.Infof("received edge client event: %v", req)
		p.updateClientByEvent(&req)
		p.emitClientEvent(ctx, req.Content)
	})
}

func (p *provider) RegisterEventHandler(handler EventHandler) {
	p.Lock()
	defer p.Unlock()
	p.eventHandlers = append(p.eventHandlers, handler)
}

func (p *provider) updateClientByEvent(ev *apistructs.ClusterManagerClientEvent) {
	detail := ev.Content
	if detail == nil {
		return
	}
	clusterKey := detail.Get(apistructs.ClusterManagerDataKeyClusterKey)
	if clusterKey == "" {
		return
	}
	p.Lock()
	p.edgeClients[clusterKey] = detail
	p.Unlock()
}

func (p *provider) continuousUpdateClient(ctx context.Context) {
	ticker := time.NewTicker(1 * time.Millisecond)
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			ticker.Reset(p.Cfg.UpdateClientInterval)
			edgeClients, err := p.bdl.ListClusterManagerClientByType(apistructs.ClusterManagerClientTypePipeline)
			if err != nil {
				p.Log.Errorf("failed to list edge clients, err: %v", err)
				continue
			}
			for _, edgeClient := range edgeClients {
				p.updateClientByEvent(&apistructs.ClusterManagerClientEvent{Content: edgeClient})
			}
		}
	}
}

func (p *provider) emitClientEvent(ctx context.Context, eventDetail apistructs.ClusterManagerClientDetail) {
	p.Lock()
	for _, handler := range p.eventHandlers {
		go handler(ctx, eventDetail)
	}
	p.Unlock()
}

func (p *provider) ListAllClients() []apistructs.ClusterManagerClientDetail {
	p.Lock()
	defer p.Unlock()
	var result []apistructs.ClusterManagerClientDetail
	for _, detail := range p.edgeClients {
		clientDetailDup, ok := deepcopy.Copy(detail).(apistructs.ClusterManagerClientDetail)
		if !ok {
			continue
		}
		result = append(result, clientDetailDup)
	}
	return result
}
