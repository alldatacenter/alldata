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

package tasks_test

import (
	"context"
	"testing"

	"github.com/rancher/apiserver/pkg/types"

	"github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/internal/apps/cmp/tasks"
)

type fakeCmp struct{}

func (c fakeCmp) ListSteveResource(ctx context.Context, req *apistructs.SteveRequest) ([]types.APIObject, error) {
	return nil, nil
}

func (c fakeCmp) GetNamespacesResources(ctx context.Context, nReq *pb.GetNamespacesResourcesRequest) (*pb.GetNamespacesResourcesResponse, error) {
	return nil, nil
}

func (c fakeCmp) GetClustersResources(ctx context.Context, cReq *pb.GetClustersResourcesRequest) (*pb.GetClusterResourcesResponse, error) {
	return nil, nil
}

func (c fakeCmp) GetAllClusters() []string {
	return nil
}

func TestNewDailyQuotaCollector(t *testing.T) {
	var (
		db  = new(dbclient.DBClient)
		bdl = new(bundle.Bundle)
		cmp = new(fakeCmp)
	)
	tasks.NewDailyQuotaCollector(
		tasks.DailyQuotaCollectorWithDBClient(db),
		tasks.DailyQuotaCollectorWithBundle(bdl),
		tasks.DailyQuotaCollectorWithCMPAPI(cmp),
	)
}
