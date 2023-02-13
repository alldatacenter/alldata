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

package org

import (
	"context"
	"testing"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	calcu "github.com/erda-project/erda/pkg/resourcecalculator"
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

func Test_extractWorkspacesFromLabels(t *testing.T) {
	var labels = []string{
		"dice/workspace-prod=true",
		"dice/workspace-staging=true",
		"dice/workspace-test=true",
		"dice/workspace-dev=true",
		"other/some-label",
	}
	workspaces := extractWorkspacesFromLabels(labels)
	if !hasWorkspace(workspaces, calcu.Prod) {
		t.Fatal("error for prod")
	}
	if !hasWorkspace(workspaces, calcu.Staging) {
		t.Fatal("error for staging")
	}
	if !hasWorkspace(workspaces, calcu.Test) {
		t.Fatal("error for test")
	}
	if !hasWorkspace(workspaces, calcu.Dev) {
		t.Fatal("error for dev")
	}
}

func TestOrg_makeTips(t *testing.T) {
	var (
		o          = New(WithTrans(fakeTrans{}))
		resource   = new(apistructs.ClusterResources)
		calcalator = calcu.New("erda-hongkong")
	)
	o.makeTips(context.Background(), resource, calcalator, calcu.Prod)
	resource.CPUAllocatable = 1
	resource.MemAllocatable = 1
	o.makeTips(context.Background(), resource, calcalator, calcu.Prod)
}

func hasWorkspace(workspaces []calcu.Workspace, workspace calcu.Workspace) bool {
	for _, w := range workspaces {
		if w == workspace {
			return true
		}
	}
	return false
}
