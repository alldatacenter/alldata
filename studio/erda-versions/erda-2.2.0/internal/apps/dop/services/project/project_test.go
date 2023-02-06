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

package project_test

import (
	"context"
	"testing"

	"github.com/erda-project/erda-infra/providers/i18n"
	dashboardPb "github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/project"
)

type fakeTrans struct{}

func (fakeTrans) Get(i18n.LanguageCodes, string, string) string {
	return ""
}

func (fakeTrans) Text(i18n.LanguageCodes, string) string {
	return ""
}

func (fakeTrans) Sprintf(i18n.LanguageCodes, string, ...interface{}) string {
	return ""
}

type fakeCMP struct{}

func (c fakeCMP) GetPodsByLabels(ctx context.Context, request *dashboardPb.GetPodsByLabelsRequest) (*dashboardPb.GetPodsByLabelsResponse, error) {
	return nil, nil
}

func (fakeCMP) GetClustersResources(context.Context, *dashboardPb.GetClustersResourcesRequest) (*dashboardPb.GetClusterResourcesResponse, error) {
	return nil, nil
}

func (fakeCMP) GetNamespacesResources(context.Context, *dashboardPb.GetNamespacesResourcesRequest) (*dashboardPb.GetNamespacesResourcesResponse, error) {
	return nil, nil
}

func TestNew(t *testing.T) {
	var (
		bdl   = new(bundle.Bundle)
		trans fakeTrans
		cmp   fakeCMP
	)
	p := project.New(
		project.WithBundle(bdl),
		project.WithTrans(trans),
		project.WithCMP(cmp),
	)
	if p == nil {
		t.Fatal("error for New")
	}
}
