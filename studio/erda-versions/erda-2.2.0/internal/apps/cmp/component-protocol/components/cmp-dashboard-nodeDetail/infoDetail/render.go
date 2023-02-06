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

package infoDetail

import (
	"context"
	"fmt"
	"sort"
	"strings"

	"github.com/pkg/errors"
	"github.com/rancher/wrangler/pkg/data"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodeDetail/common"
)

var steveServer cmp.SteveServer

func (infoDetail *InfoDetail) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (infoDetail *InfoDetail) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	infoDetail.Ctx = ctx
	infoDetail.SDK = cputil.SDK(ctx)
	if event.Operation == common.CMPDashboardRemoveLabel {
		metaName := event.OperationData["fillMeta"].(string)
		label := event.OperationData["meta"].(map[string]interface{})[metaName].(map[string]interface{})["label"].(string)
		recordId := event.OperationData["meta"].(map[string]interface{})["recordId"].(string)
		req := apistructs.SteveRequest{}
		req.ClusterName = infoDetail.SDK.InParams["clusterName"].(string)
		req.OrgID = infoDetail.SDK.Identity.OrgID
		req.UserID = infoDetail.SDK.Identity.UserID
		req.Type = apistructs.K8SNode
		req.Name = recordId
		labelKey := strings.Split(label, "=")[0]
		err := steveServer.UnlabelNode(ctx, &req, []string{labelKey})
		if err != nil {
			return err
		}
		resp, err := steveServer.GetSteveResource(ctx, &req)
		if err != nil {
			return err
		}
		(*gs)["node"] = resp.Data()
	}
	var node data.Object
	node = (*gs)["node"].(data.Object)
	infoDetail.Props = infoDetail.getProps(node)
	//infoDetail.setOperation(node.String("id"))
	d := Data{}
	d.Os = node.String("status", "nodeInfo", "osImage")
	d.Version = node.String("status", "nodeInfo", "kubeletVersion")
	d.ContainerRuntimeVersion = node.StringSlice("metadata", "fields")[9]
	d.NodeIP = infoDetail.getIp(node)
	d.PodNum = node.String("status", "capacity", "pods")
	d.Tags = infoDetail.getTags(node)
	d.Annotation = infoDetail.getAnnotations(node)
	d.Taints = infoDetail.getTaints(node)
	d.Survive = node.StringSlice("metadata", "fields")[3]
	c.Props = cputil.MustConvertProps(infoDetail.Props)
	c.Data = map[string]interface{}{"data": d}
	return nil
}

func (infoDetail *InfoDetail) getIp(node data.Object) string {
	for _, addr := range node.Slice("status", "addresses") {
		if addr.String("type") == "InternalIP" {
			return addr.String("address")
		}
	}
	return ""
}
func (infoDetail *InfoDetail) GetLabelGroupAndDisplayName(label string) (string, string) {
	//ls := []string{
	//	"dice/workspace-dev", "dice/workspace-test", "staging", "prod", "stateful", "stateless", "packJob", "cluster-service", "mono", "cordon", "drain", "platform",
	//}
	groups := make(map[string]string)
	groups["dice/workspace-dev=true"] = infoDetail.SDK.I18n("env-label")
	groups["dice/workspace-test=true"] = infoDetail.SDK.I18n("env-label")
	groups["dice/workspace-staging=true"] = infoDetail.SDK.I18n("env-label")
	groups["dice/workspace-prod=true"] = infoDetail.SDK.I18n("env-label")

	groups["dice/stateful-service=true"] = infoDetail.SDK.I18n("service-label")
	groups["dice/stateless-service=true"] = infoDetail.SDK.I18n("service-label")
	groups["dice/location-cluster-service=true"] = infoDetail.SDK.I18n("service-label")

	groups["dice/job=true"] = infoDetail.SDK.I18n("job-label")
	groups["dice/bigdata-job=true"] = infoDetail.SDK.I18n("job-label")

	groups["dice/lb=true"] = infoDetail.SDK.I18n("other-label")
	groups["dice/platform=true"] = infoDetail.SDK.I18n("other-label")

	if group, ok := groups[label]; ok {
		return group, label
	}
	return infoDetail.SDK.I18n("other-label"), label
}

func (infoDetail *InfoDetail) getTags(node data.Object) []Field {
	//groups := filter.GetGroups()
	tag := make([]Field, 0)
	for k, v := range node.Map("metadata", "labels") {
		g, _ := infoDetail.GetLabelGroupAndDisplayName(k + "=" + v.(string))
		tag = append(tag, Field{
			Label: fmt.Sprintf("%s=%s", k, v),
			Group: g,
		})
	}
	if len(tag) == 0 {
		tag = append(tag, Field{
			Label: fmt.Sprintf(infoDetail.SDK.I18n("none")),
		})
	}
	sort.Slice(tag, func(i, j int) bool {
		if tag[i].Group == tag[j].Group {
			return tag[i].Label < tag[j].Label
		}
		return tag[i].Group < tag[j].Group
	})
	return tag
}

func (infoDetail *InfoDetail) getAnnotations(node data.Object) []Field {
	desc := make([]Field, 0)
	for k, v := range node.Map("metadata", "annotations") {
		desc = append(desc, Field{
			Label: fmt.Sprintf("%s=%s", k, v),
		})
	}
	if len(desc) == 0 {
		desc = append(desc, Field{
			Label: fmt.Sprintf(infoDetail.SDK.I18n("none")),
		})
	}
	return desc
}

func (infoDetail *InfoDetail) getTaints(node data.Object) []Field {
	desc := make([]Field, 0)
	for _, v := range node.Slice("spec", "taints") {
		var l string
		if v.String("value") == "" {
			l = fmt.Sprintf("%s:%s", v.String("key"), v.String("effect"))
		} else {
			l = fmt.Sprintf("%s=%s:%s", v.String("key"), v.String("value"), v.String("effect"))
		}
		desc = append(desc, Field{
			Label: l,
		})
	}
	if len(desc) == 0 {
		desc = append(desc, Field{
			Label: fmt.Sprintf(infoDetail.SDK.I18n("none")),
		})
	}
	return desc
}

func (infoDetail *InfoDetail) getProps(node data.Object) Props {
	return Props{
		ColumnNum: 4,
		Fields: []Field{
			{Label: infoDetail.SDK.I18n("survive"), ValueKey: "survive"},
			{Label: infoDetail.SDK.I18n("nodeIP"), ValueKey: "nodeIP"},
			{Label: infoDetail.SDK.I18n("version"), ValueKey: "version"},
			{Label: infoDetail.SDK.I18n("os"), ValueKey: "os"},
			{Label: infoDetail.SDK.I18n("containerRuntimeVersion"), ValueKey: "containerRuntimeVersion"},
			{Label: infoDetail.SDK.I18n("podNum"), ValueKey: "podNum"},
			{Label: infoDetail.SDK.I18n("tag"), ValueKey: "tag", RenderType: "tagsRow", SpaceNum: 2, Operations: map[string]Operation{
				"add": {
					Key:    "addLabel",
					Reload: false,
					Command: Command{
						Key:    "set",
						Target: "addLabelModal",
						CommandState: CommandState{
							Visible:  true,
							FormData: FormData{RecordId: node.String("metadata", "name")},
						},
					},
				},
				"delete": {
					Key:      "deleteLabel",
					Reload:   true,
					FillMeta: "dlabel",
					Meta: map[string]interface{}{
						"recordId": node.String("metadata", "name"),
						"dlabel":   Field{Label: ""},
					},
				}}},
			{Label: infoDetail.SDK.I18n("annotation"), ValueKey: "annotation", SpaceNum: 2, RenderType: "tagsRow"},
			{Label: infoDetail.SDK.I18n("taint"), ValueKey: "taint", SpaceNum: 2, RenderType: "tagsRow"},
		},
	}
}
func init() {
	base.InitProviderWithCreator("cmp-dashboard-nodeDetail", "infoDetail", func() servicehub.Provider {
		return &InfoDetail{}
	})
}
