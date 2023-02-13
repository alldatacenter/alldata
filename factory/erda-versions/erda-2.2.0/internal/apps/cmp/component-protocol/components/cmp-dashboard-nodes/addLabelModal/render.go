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

package AddLabelModal

import (
	"context"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common"
)

var steveServer cmp.SteveServer

func (alm *AddLabelModal) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (alm *AddLabelModal) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	alm.Ctx = ctx
	alm.SDK = cputil.SDK(ctx)
	alm.GetState()
	err := common.Transfer(c.State, &alm.State)
	if err != nil {
		return err
	}
	alm.getProps()
	alm.getOperations()
	clusterNameIter := alm.SDK.InParams["clusterName"]
	if clusterNameIter == nil {
		return errors.New("clusterName not found")
	}
	switch event.Operation {
	case common.CMPDashboardSubmit:
		c.Props = cputil.MustConvertProps(alm.Props)
		req := &apistructs.SteveRequest{
			UserID:      alm.SDK.Identity.UserID,
			OrgID:       alm.SDK.Identity.OrgID,
			Type:        apistructs.K8SNode,
			Name:        alm.State.FormData["recordId"],
			ClusterName: clusterNameIter.(string),
		}
		labelValue := ""
		labelKey := alm.State.FormData["labelGroup"]
		if labelKey == "custom" {
			labelKey = alm.State.FormData["label_custom_key"]
			labelValue = alm.State.FormData["label_custom_value"]
		} else {
			labelValue = alm.State.FormData[labelKey]
			splits := strings.Split(labelValue, "=")
			labelKey = splits[0]
			if len(splits) == 1 {
				labelValue = ""
			} else {
				labelValue = splits[1]
			}
		}
		err := steveServer.LabelNode(alm.Ctx, req, map[string]string{labelKey: labelValue})
		if err != nil {
			return err
		}
		alm.State.Visible = false
	}
	return alm.SetComponentValue(c)
}

// SetComponentValue mapping properties to Component
func (alm *AddLabelModal) SetComponentValue(c *cptype.Component) error {
	var err error
	if err = common.Transfer(alm.State, &c.State); err != nil {
		return err
	}
	if err = common.Transfer(alm.Props, &c.Props); err != nil {
		return err
	}
	if err = common.Transfer(alm.Operations, &c.Operations); err != nil {
		return err
	}
	return nil
}

func (alm *AddLabelModal) getProps() {
	fields := []Fields{
		{
			Label:     alm.SDK.I18n("category"),
			Component: "select",
			Required:  true,
			Key:       "labelGroup",
			ComponentProps: ComponentProps{
				Options: []Option{
					{
						Name:  alm.SDK.I18n("env-label"),
						Value: "environment",
					},
					{
						Name:  alm.SDK.I18n("service-label"),
						Value: "service",
					},
					{
						Name:  alm.SDK.I18n("job-label"),
						Value: "job",
					},
					{
						Name:  alm.SDK.I18n("other-label"),
						Value: "other",
					},
					{
						Name:  alm.SDK.I18n("custom-label"),
						Value: "custom",
					},
				},
			},
		},
		{
			Key:            "environment",
			ComponentProps: ComponentProps{Options: []Option{{Name: alm.SDK.I18n("workspace-dev"), Value: "dice/workspace-dev=true"}, {Name: alm.SDK.I18n("workspace-test"), Value: "dice/workspace-test=true"}, {Name: alm.SDK.I18n("workspace-staging"), Value: "dice/workspace-staging=true"}, {Name: alm.SDK.I18n("workspace-prod"), Value: "dice/workspace-prod=true"}}},
			Label:          alm.SDK.I18n("label"),
			Component:      "select",
			Required:       true,
			RemoveWhen: [][]RemoveWhen{
				{
					{
						Field: "labelGroup", Operator: "!=", Value: "environment",
					},
				},
			},
		},
		{
			Key:            "service",
			ComponentProps: ComponentProps{Options: []Option{{Name: alm.SDK.I18n("stateful-service"), Value: "dice/stateful-service=true"}, {Name: alm.SDK.I18n("stateless-service"), Value: "dice/stateless-service=true"}, {Name: alm.SDK.I18n("location-cluster-service"), Value: "dice/location-cluster-service=true"}}},
			Label:          alm.SDK.I18n("label"),
			Component:      "select",
			Required:       true,
			RemoveWhen: [][]RemoveWhen{
				{
					{
						Field: "labelGroup", Operator: "!=", Value: "service",
					},
				},
			},
		},
		{
			Key:            "job",
			ComponentProps: ComponentProps{Options: []Option{{Name: alm.SDK.I18n("cicd-job"), Value: "dice/job=true"}, {Name: alm.SDK.I18n("bigdata-job"), Value: "dice/bigdata-job=true"}}},
			Label:          alm.SDK.I18n("label"),
			Component:      "select",
			Required:       true,
			RemoveWhen: [][]RemoveWhen{
				{
					{
						Field: "labelGroup", Operator: "!=", Value: "job",
					},
				},
			},
		},
		{
			Key:            "other",
			ComponentProps: ComponentProps{Options: []Option{{Name: alm.SDK.I18n("lb"), Value: "dice/lb=true"}, {Name: alm.SDK.I18n("platform"), Value: "dice/platform=true"}}},
			Label:          alm.SDK.I18n("label"),
			Component:      "select",
			Required:       true,
			RemoveWhen: [][]RemoveWhen{
				{
					{
						Field: "labelGroup", Operator: "!=", Value: "other",
					},
				},
			},
		},
		{
			Key:       "label_custom_key",
			Label:     alm.SDK.I18n("label"),
			Component: "input",
			Required:  true,
			Rules: Rules{
				Msg:     "",
				Pattern: "pattern: '/^[.a-z\\u4e00-\\u9fa5A-Z0-9_-\\s]*$/'",
			},
			RemoveWhen: [][]RemoveWhen{
				{
					{
						Field: "labelGroup", Operator: "!=", Value: "custom",
					},
				},
			},
		},
		{
			Component: "input",
			Key:       "label_custom_value",
			Label:     alm.SDK.I18n("label-value"),
			Required:  true,
			Rules: Rules{
				Msg:     "",
				Pattern: "/^[.a-z\\u4e00-\\u9fa5A-Z0-9_-\\s]*$/",
			},
			RemoveWhen: [][]RemoveWhen{
				{
					{
						Field: "labelGroup", Operator: "!=", Value: "custom",
					},
				},
			},
		},
	}
	alm.Props = Props{
		Fields: fields,
		Title:  alm.SDK.I18n("addLabel"),
	}
}

func (alm *AddLabelModal) getOperations() {
	alm.Operations = map[string]Operations{
		"submit": {
			Key:    "submit",
			Reload: true,
		},
	}
}

func (alm *AddLabelModal) GetState() {
	alm.State = State{
		Visible:  false,
		FormData: map[string]string{},
	}
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-nodes", "addLabelModal", func() servicehub.Provider {
		return &AddLabelModal{}
	})
}
