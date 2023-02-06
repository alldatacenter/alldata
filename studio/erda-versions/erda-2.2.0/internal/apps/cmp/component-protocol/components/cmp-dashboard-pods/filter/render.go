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

package filter

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"sort"
	"strconv"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp"
	cputil2 "github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-pods", "filter", func() servicehub.Provider {
		return &ComponentFilter{}
	})
}

var steveServer cmp.SteveServer

func (f *ComponentFilter) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (f *ComponentFilter) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, _ *cptype.GlobalStateData) error {
	f.InitComponent(ctx)
	if err := f.GenComponentState(component); err != nil {
		return fmt.Errorf("failed to gen filter component state, %v", err)
	}

	if event.Operation == cptype.InitializeOperation {
		if _, ok := f.sdk.InParams["filter__urlQuery"]; !ok {
			f.State.Values.Namespace = "default"
		} else if err := f.DecodeURLQuery(); err != nil {
			return fmt.Errorf("failed to decode url query for filter component, %v", err)
		}
	}

	if err := f.SetComponentValue(ctx); err != nil {
		return fmt.Errorf("failed to set filter component value, %v", err)
	}
	if err := f.EncodeURLQuery(); err != nil {
		return fmt.Errorf("failed to gen filter component url query, %v", err)
	}
	f.Transfer(component)
	return nil
}

func (f *ComponentFilter) InitComponent(ctx context.Context) {
	bdl := ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	f.bdl = bdl
	sdk := cputil.SDK(ctx)
	f.sdk = sdk
	f.ctx = ctx
	f.server = steveServer
}

func (f *ComponentFilter) DecodeURLQuery() error {
	query, ok := f.sdk.InParams["filter__urlQuery"].(string)
	if !ok {
		return nil
	}
	decoded, err := base64.StdEncoding.DecodeString(query)
	if err != nil {
		return err
	}

	var values Values
	if err := json.Unmarshal(decoded, &values); err != nil {
		return err
	}
	f.State.Values = values
	return nil
}

func (f *ComponentFilter) GenComponentState(component *cptype.Component) error {
	if component == nil || component.State == nil {
		return nil
	}
	var filterState State
	jsonData, err := json.Marshal(component.State)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(jsonData, &filterState); err != nil {
		return err
	}
	f.State = filterState
	return nil
}

func (f *ComponentFilter) SetComponentValue(ctx context.Context) error {
	userID := f.sdk.Identity.UserID
	orgID := f.sdk.Identity.OrgID

	namespaces, err := cputil2.GetAllNamespacesFromCache(ctx, f.server, userID, orgID, f.State.ClusterName)
	if err != nil {
		return err
	}
	projectID2displayName, err := cputil2.GetAllProjectsDisplayNameFromCache(f.bdl, orgID)
	if err != nil {
		return err
	}

	devNs := Option{
		Label: cputil.I18n(ctx, "workspace-dev"),
		Value: "workspace-dev",
	}
	testNs := Option{
		Label: cputil.I18n(ctx, "workspace-test"),
		Value: "workspace-test",
	}
	stagingNs := Option{
		Label: cputil.I18n(ctx, "workspace-staging"),
		Value: "workspace-staging",
	}
	productionNs := Option{
		Label: cputil.I18n(ctx, "workspace-prod"),
		Value: "workspace-prod",
	}
	addonNs := Option{
		Label: cputil.I18n(ctx, "addons"),
		Value: "addons",
	}
	pipelineNs := Option{
		Label: cputil.I18n(ctx, "pipelines"),
		Value: "pipelines",
	}
	defaultNs := Option{
		Label: cputil.I18n(ctx, "default"),
		Value: "default",
	}
	systemNs := Option{
		Label: cputil.I18n(ctx, "system"),
		Value: "system",
	}
	otherNs := Option{
		Label: cputil.I18n(ctx, "others"),
		Value: "others",
	}

	for _, name := range namespaces {
		option := Option{
			Label: name,
			Value: name,
		}
		if suf, ok := hasSuffix(name); ok && strings.HasPrefix(name, "project-") {
			splits := strings.Split(name, "-")
			if len(splits) != 3 {
				return errors.New("invalid name")
			}
			id := splits[1]
			num, err := strconv.ParseInt(id, 10, 64)
			if err != nil {
				return errors.Errorf("failed to parse project id %s, %v", id, err)
			}

			displayName, ok := projectID2displayName[uint64(num)]
			if ok {
				option.Label = fmt.Sprintf("%s (%s: %s)", name, cputil.I18n(ctx, "project"), displayName)
				switch suf {
				case "-dev":
					devNs.Children = append(devNs.Children, option)
				case "-test":
					testNs.Children = append(testNs.Children, option)
				case "-prod":
					productionNs.Children = append(productionNs.Children, option)
				case "-staging":
					stagingNs.Children = append(stagingNs.Children, option)
				}
				continue
			}
		}
		if strings.HasPrefix(name, "addon-") || strings.HasPrefix(name, "group-addon-") {
			addonNs.Children = append(addonNs.Children, option)
			continue
		}
		if strings.HasPrefix(name, "pipeline-") {
			pipelineNs.Children = append(pipelineNs.Children, option)
			continue
		}
		if name == "default" {
			defaultNs.Children = append(defaultNs.Children, option)
			continue
		}
		if name == "kube-system" || name == "erda-system" {
			systemNs.Children = append(systemNs.Children, option)
			continue
		}
		otherNs.Children = append(otherNs.Children, option)
	}

	f.State.Conditions = nil
	namespaceCond := Condition{
		HaveFilter: true,
		Key:        "namespace",
		Label:      cputil.I18n(ctx, "namespace"),
		Type:       "select",
		Fixed:      true,
		CustomProps: CustomProps{
			Mode: "single",
		},
	}
	for _, option := range []Option{defaultNs, systemNs, devNs, testNs, productionNs, stagingNs, addonNs, pipelineNs, otherNs} {
		if option.Children != nil {
			sort.Slice(option.Children, func(i, j int) bool {
				return option.Children[i].Label < option.Children[j].Label
			})
			namespaceCond.Options = append(namespaceCond.Options, option)
		}
	}
	f.State.Conditions = append(f.State.Conditions, namespaceCond)

	f.State.Conditions = append(f.State.Conditions, Condition{
		Key:   "status",
		Label: cputil.I18n(ctx, "status"),
		Type:  "select",
		Fixed: true,
		Options: []Option{
			{
				Label: cputil.I18n(ctx, "Completed"),
				Value: "Completed",
			},
			{
				Label: cputil.I18n(ctx, "ContainerCreating"),
				Value: "ContainerCreating",
			},
			{
				Label: cputil.I18n(ctx, "CrashLoopBackOff"),
				Value: "CrashLoopBackOff",
			},
			{
				Label: cputil.I18n(ctx, "Error"),
				Value: "Error",
			},
			{
				Label: cputil.I18n(ctx, "Evicted"),
				Value: "Evicted",
			},
			{
				Label: cputil.I18n(ctx, "ImagePullBackOff"),
				Value: "ImagePullBackOff",
			},
			{
				Label: cputil.I18n(ctx, "ErrImagePull"),
				Value: "ErrImagePull",
			},
			{
				Label: cputil.I18n(ctx, "Pending"),
				Value: "Pending",
			},
			{
				Label: cputil.I18n(ctx, "Running"),
				Value: "Running",
			},
			{
				Label: cputil.I18n(ctx, "Terminating"),
				Value: "Terminating",
			},
			{
				Label: cputil.I18n(ctx, "OOMKilled"),
				Value: "OOMKilled",
			},
			{
				Label: cputil.I18n(ctx, "others"),
				Value: "others",
			},
		},
	})

	var nodeOptions []Option
	nodes, err := f.getNodes()
	if err != nil {
		return err
	}
	for _, node := range nodes {
		nodeOptions = append(nodeOptions, Option{
			Label: node,
			Value: node,
		})
	}
	f.State.Conditions = append(f.State.Conditions, Condition{
		Key:     "node",
		Label:   cputil.I18n(ctx, "node"),
		Type:    "select",
		Fixed:   true,
		Options: nodeOptions,
	})

	f.State.Conditions = append(f.State.Conditions, Condition{
		Key:         "search",
		Label:       cputil.I18n(ctx, "search"),
		Placeholder: cputil.I18n(ctx, "podSearchPlaceHolder"),
		Type:        "input",
		Fixed:       true,
	})

	f.Operations = map[string]interface{}{
		"filter": Operation{
			Key:    "filter",
			Reload: true,
		},
	}
	return nil
}

func (f *ComponentFilter) getNodes() ([]string, error) {
	userID := f.sdk.Identity.UserID
	orgID := f.sdk.Identity.OrgID
	req := apistructs.SteveRequest{
		UserID:      userID,
		OrgID:       orgID,
		Type:        apistructs.K8SNode,
		ClusterName: f.sdk.InParams["clusterName"].(string),
	}
	list, err := f.server.ListSteveResource(f.ctx, &req)
	if err != nil {
		return nil, err
	}
	var res []string
	for _, item := range list {
		obj := item.Data()
		res = append(res, obj.String("metadata", "name"))
	}
	return res, nil
}

func (f *ComponentFilter) EncodeURLQuery() error {
	jsonData, err := json.Marshal(f.State.Values)
	if err != nil {
		return err
	}

	encoded := base64.StdEncoding.EncodeToString(jsonData)
	f.State.FilterURLQuery = encoded
	return nil
}

func (f *ComponentFilter) Transfer(c *cptype.Component) {
	c.State = map[string]interface{}{
		"clusterName":      f.State.ClusterName,
		"conditions":       f.State.Conditions,
		"values":           f.State.Values,
		"filter__urlQuery": f.State.FilterURLQuery,
	}
	c.Operations = f.Operations
}

func hasSuffix(name string) (string, bool) {
	targetSuffixes := []string{"-dev", "-staging", "-test", "-prod"}
	for _, s := range targetSuffixes {
		if strings.HasSuffix(name, s) {
			return s, true
		}
	}
	return "", false
}
