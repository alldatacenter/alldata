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

package cmp

import (
	"context"

	"github.com/rancher/apiserver/pkg/types"
	apiuser "k8s.io/apiserver/pkg/authentication/user"

	"github.com/erda-project/erda/apistructs"
)

type SteveServer interface {
	GetSteveResource(context.Context, *apistructs.SteveRequest) (types.APIObject, error)
	ListSteveResource(context.Context, *apistructs.SteveRequest) ([]types.APIObject, error)
	UpdateSteveResource(context.Context, *apistructs.SteveRequest) (types.APIObject, error)
	CreateSteveResource(context.Context, *apistructs.SteveRequest) (types.APIObject, error)
	DeleteSteveResource(context.Context, *apistructs.SteveRequest) error
	PatchNode(context.Context, *apistructs.SteveRequest) error
	LabelNode(context.Context, *apistructs.SteveRequest, map[string]string) error
	UnlabelNode(context.Context, *apistructs.SteveRequest, []string) error
	CordonNode(context.Context, *apistructs.SteveRequest) error
	UnCordonNode(context.Context, *apistructs.SteveRequest) error
	DrainNode(context.Context, *apistructs.SteveRequest) error
	OfflineNode(context.Context, string, string, string, []string) error
	OnlineNode(context.Context, *apistructs.SteveRequest) error
	Auth(userID, orgID, clusterName string) (apiuser.Info, error)
}

func (p *provider) GetSteveResource(ctx context.Context, req *apistructs.SteveRequest) (types.APIObject, error) {
	return p.SteveAggregator.GetSteveResource(ctx, req)
}

func (p *provider) ListSteveResource(ctx context.Context, req *apistructs.SteveRequest) ([]types.APIObject, error) {
	return p.SteveAggregator.ListSteveResource(ctx, req)
}

func (p *provider) UpdateSteveResource(ctx context.Context, req *apistructs.SteveRequest) (types.APIObject, error) {
	return p.SteveAggregator.UpdateSteveResource(ctx, req)
}

func (p *provider) CreateSteveResource(ctx context.Context, req *apistructs.SteveRequest) (types.APIObject, error) {
	return p.SteveAggregator.CreateSteveResource(ctx, req)
}

func (p *provider) DeleteSteveResource(ctx context.Context, req *apistructs.SteveRequest) error {
	return p.SteveAggregator.DeleteSteveResource(ctx, req)
}

func (p *provider) PatchNode(ctx context.Context, req *apistructs.SteveRequest) error {
	return p.SteveAggregator.PatchNode(ctx, req)
}

func (p *provider) GetAllClusters() []string {
	return p.SteveAggregator.GetAllClusters()
}

func (p *provider) LabelNode(ctx context.Context, req *apistructs.SteveRequest, labels map[string]string) error {
	return p.SteveAggregator.LabelNode(ctx, req, labels)
}

func (p *provider) UnlabelNode(ctx context.Context, req *apistructs.SteveRequest, labels []string) error {
	return p.SteveAggregator.UnlabelNode(ctx, req, labels)
}

func (p *provider) CordonNode(ctx context.Context, req *apistructs.SteveRequest) error {
	return p.SteveAggregator.CordonNode(ctx, req)
}

func (p *provider) UnCordonNode(ctx context.Context, req *apistructs.SteveRequest) error {
	return p.SteveAggregator.UnCordonNode(ctx, req)
}

func (p *provider) DrainNode(ctx context.Context, req *apistructs.SteveRequest) error {
	return p.SteveAggregator.DrainNode(ctx, req)
}

func (p *provider) OfflineNode(ctx context.Context, userID, orgID, clusterName string, nodeIDs []string) error {
	return p.SteveAggregator.OfflineNode(ctx, userID, orgID, clusterName, nodeIDs)
}

func (p *provider) OnlineNode(ctx context.Context, req *apistructs.SteveRequest) error {
	return p.SteveAggregator.OnlineNode(ctx, req)
}

func (p *provider) Auth(userID, orgID, clusterName string) (apiuser.Info, error) {
	return p.SteveAggregator.Auth(userID, orgID, clusterName)
}
