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
	"net/url"
	"sort"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp"
	cputil2 "github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
	"github.com/erda-project/erda/pkg/http/httpclient"

	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-events-list", "filter", func() servicehub.Provider {
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
		if err := f.DecodeURLQuery(); err != nil {
			return fmt.Errorf("failed to decode url query for filter component, %v", err)
		}
	}
	if err := f.SetComponentValue(ctx); err != nil {
		return fmt.Errorf("failed to set filter component value, %v", err)
	}
	if err := f.EncodeURLQuery(); err != nil {
		return fmt.Errorf("failed to encode url query for filter component, %v", err)
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
	urlQuery, ok := f.sdk.InParams["filter__urlQuery"].(string)
	if !ok {
		return nil
	}
	decode, err := base64.StdEncoding.DecodeString(urlQuery)
	if err != nil {
		return err
	}
	var v Values
	if err := json.Unmarshal(decode, &v); err != nil {
		return err
	}
	f.State.Values = v
	return nil
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

func (f *ComponentFilter) GenComponentState(c *cptype.Component) error {
	if c == nil || c.State == nil {
		return nil
	}
	var s State
	cont, err := json.Marshal(c.State)
	if err != nil {
		logrus.Errorf("marshal component state failed, content:%v, err:%v", c.State, err)
		return err
	}
	err = json.Unmarshal(cont, &s)
	if err != nil {
		logrus.Errorf("unmarshal component state failed, content:%v, err:%v", cont, err)
		return err
	}
	f.State = s
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
		Value: "dev",
	}
	testNs := Option{
		Label: cputil.I18n(ctx, "workspace-test"),
		Value: "test",
	}
	stagingNs := Option{
		Label: cputil.I18n(ctx, "workspace-staging"),
		Value: "staging",
	}
	productionNs := Option{
		Label: cputil.I18n(ctx, "workspace-prod"),
		Value: "production",
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
				continue
			}
			id := splits[1]
			num, err := strconv.ParseInt(id, 10, 64)
			if err != nil {
				logrus.Errorf("failed to parse project id %s from namespace %s, %v", id, name, err)
				continue
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
		Key:   "type",
		Label: cputil.I18n(ctx, "eventType"),
		Type:  "select",
		Fixed: true,
		Options: []Option{
			{
				Label: cputil.I18n(ctx, "Normal"),
				Value: "Normal",
			},
			{
				Label: cputil.I18n(ctx, "Warning"),
				Value: "Warning",
			},
		},
	})

	f.State.Conditions = append(f.State.Conditions, Condition{
		Key:         "search",
		Label:       cputil.I18n(ctx, "search"),
		Placeholder: cputil.I18n(ctx, "eventSearchPlaceHolder"),
		Type:        "input",
		Fixed:       true,
	})

	f.Operations = make(map[string]interface{})
	f.Operations["filter"] = Operation{
		Key:    "filter",
		Reload: true,
	}
	return nil
}

func (f *ComponentFilter) Transfer(component *cptype.Component) {
	component.State = map[string]interface{}{
		"clusterName":      f.State.ClusterName,
		"conditions":       f.State.Conditions,
		"values":           f.State.Values,
		"filter__urlQuery": f.State.FilterURLQuery,
	}
	component.Operations = f.Operations
}

func (f *ComponentFilter) getDisplayName(name string) (string, error) {
	splits := strings.Split(name, "-")
	if len(splits) != 3 {
		return "", errors.New("invalid name")
	}
	id := splits[1]
	num, err := strconv.ParseInt(id, 10, 64)
	if err != nil {
		return "", err
	}
	project, err := f.bdl.GetProjectWithSetter(uint64(num), httpclient.SetParams(url.Values{"withQuota": {"true"}}))
	if err != nil {
		return "", err
	}
	return project.DisplayName, nil
}

func hasSuffix(name string) (string, bool) {
	suffixes := []string{"-dev", "-staging", "-test", "-prod"}
	for _, suffix := range suffixes {
		if strings.HasSuffix(name, suffix) {
			return suffix, true
		}
	}
	return "", false
}
