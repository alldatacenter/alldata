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

package table

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"reflect"
	"sort"
	"strconv"
	"strings"
	"sync"

	"github.com/rancher/wrangler/pkg/data"
	"github.com/sirupsen/logrus"
	"github.com/spf13/cast"
	v1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common/filter"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common/label"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/nodeFilter"
	"github.com/erda-project/erda/internal/apps/cmp/metrics"
	"github.com/erda-project/erda/internal/apps/cmp/steve"
)

type Table struct {
	CpuTable GetTable
	MemTable GetTable
	PodTable GetTable

	SDK        *cptype.SDK
	Ctx        context.Context
	Metrics    metrics.Interface
	Server     cmp.SteveServer
	Type       string                 `json:"type"`
	Props      map[string]interface{} `json:"props"`
	Operations map[string]interface{} `json:"operations"`
	State      State                  `json:"state"`
}

type TableType string

const (
	DefaultPageSize = 10
	DefaultPageNo   = 1

	Pod    TableType = "pod"
	Memory TableType = "memory"
	Cpu    TableType = "cpu"
)

const (
	CPU_TAB TableType = "cpu-analysis"

	MEM_TAB TableType = "mem-analysis"

	POD_TAB TableType = "pod-analysis"
)

var nodeLabelBlacklist = map[string]string{
	"dice/platform":         "true",
	"dice/lb":               "true",
	"dice/cassandra":        "true",
	"dice/es":               "true",
	"dice/kafka":            "true",
	"dice/nexus":            "true",
	"dice/gittar":           "true",
	"dice/stateful-service": "true",
}

type Columns struct {
	Title     string `json:"title,omitempty"`
	DataIndex string `json:"dataIndex,omitempty"`
	Width     int    `json:"width,omitempty"`
	Sortable  bool   `json:"sorter,omitempty"`
	Fixed     string `json:"fixed,omitempty"`
	TitleTip  string `json:"titleTip"`
	Hidden    bool   `json:"hidden"`
	Align     string `json:"align"`
}

type Meta struct {
	Id       int    `json:"id,omitempty"`
	PageSize int    `json:"pageSize,omitempty"`
	PageNo   int    `json:"pageNo,omitempty"`
	Sorter   Sorter `json:"sorter,omitempty"`
}

type Sorter struct {
	Field string `json:"field,omitempty"`
	Order string `json:"order,omitempty"`
}

type Scroll struct {
	X int `json:"x,omitempty"`
	Y int `json:"y,omitempty"`
}

type RowItem struct {
	ID      string      `json:"id,omitempty"`
	IP      string      `json:"IP,omitempty"`
	Status  SteveStatus `json:"Status,omitempty"`
	Node    Node        `json:"Node,omitempty"`
	NodeID  string      `json:"nodeId,omitempty"`
	Role    Role        `json:"Role,omitempty"`
	Version string      `json:"Version,omitempty"`
	//
	Distribution     Distribution     `json:"Distribution,omitempty"`
	Usage            Distribution     `json:"Usage,omitempty"`
	DistributionRate DistributionRate `json:"DistributionRate,omitempty"`
	Operate          Operate          `json:"Operate,omitempty"`
	// batchOperations for json
	BatchOperations []string `json:"batchOperations,omitempty"`
}

type Operate struct {
	RenderType string               `json:"renderType,omitempty"`
	Operations map[string]Operation `json:"operations,omitempty"`
}

type Operation struct {
	Key           string      `json:"key,omitempty"`
	Reload        bool        `json:"reload"`
	FillMeta      string      `json:"fillMeta,omitempty"`
	Confirm       string      `json:"confirm"`
	Target        string      `json:"target,omitempty"`
	Meta          interface{} `json:"meta,omitempty"`
	ClickableKeys interface{} `json:"clickableKeys,omitempty"`
	Text          string      `json:"text,omitempty"`
	Command       *Command    `json:"command,omitempty"`
}

type Role struct {
	RenderType string    `json:"renderType"`
	Value      RoleValue `json:"value"`
	Size       string    `json:"size"`
}

type RoleValue struct {
	Label string `json:"label"`
}

type BatchOperation struct {
	Key       string   `json:"key,omitempty"`
	Text      string   `json:"text,omitempty"`
	Reload    bool     `json:"reload,omitempty"`
	ShowIndex []string `json:"showIndex,omitempty"`
}
type Command struct {
	Key     string       `json:"key,omitempty"`
	Command CommandState `json:"state,omitempty"`
	Target  string       `json:"target,omitempty"`
	JumpOut bool         `json:"jumpOut,omitempty"`
}

type CommandState struct {
	Params   Params                 `json:"params,omitempty"`
	Visible  bool                   `json:"visible,omitempty"`
	FormData FormData               `json:"formData,omitempty"`
	Query    map[string]interface{} `json:"query,omitempty"`
}
type Params struct {
	NodeId string `json:"nodeId,omitempty"`
	NodeIP string `json:"nodeIP,omitempty"`
}

type FormData struct {
	RecordId string `json:"recordId,omitempty"`
}

type Node struct {
	RenderType string        `json:"renderType,omitempty"`
	Direction  string        `json:"direction"`
	Renders    []interface{} `json:"renders,omitempty"`
}

type NodeLink struct {
	RenderType string               `json:"renderType,omitempty"`
	Value      string               `json:"value,omitempty"`
	Operations map[string]Operation `json:"operations,omitempty"`
	Reload     bool                 `json:"reload"`
}

type NodeIcon struct {
	RenderType string `json:"renderType,omitempty"`
	Icon       string `json:"icon,omitempty"`
}

type NodeTags struct {
	RenderType string               `json:"renderType,omitempty"`
	Value      []label.Label        `json:"value,omitempty"`
	Operation  map[string]Operation `json:"operation,omitempty"`
}

type Labels struct {
	RenderType string               `json:"renderType,omitempty"`
	Value      []label.Label        `json:"value,omitempty"`
	Operations map[string]Operation `json:"operations,omitempty"`
}

type Distribution struct {
	RenderType string `json:"renderType,omitempty"`
	//Value      DistributionValue `json:"value,omitempty"`
	Value  string `json:"value"`
	Status string `json:"status,omitempty"`
	Tip    string `json:"tip,omitempty"`
}

type DistributionRate struct {
	RenderType        string  `json:"renderType,omitempty"`
	Value             string  `json:"value"`
	DistributionValue float64 `json:"distributionValue"`
}

type DistributionValue struct {
	Text    string `json:"text"`
	Percent string `json:"percent"`
}

type LabelsValue struct {
	Label string `json:"label"`
	Group string `json:"group"`
}

type GetRowItem interface {
	GetRowItems(c []data.Object, requests map[string]cmp.AllocatedRes) ([]RowItem, error)
}

type GetTableProps interface {
	GetProps() map[string]interface{}
}

type GetTable interface {
	GetRowItem
	GetTableProps
}

func (t *Table) GetUsageValue(used, total float64, resourceType TableType) DistributionValue {
	return DistributionValue{
		Text:    t.GetScaleValue(used, total, resourceType),
		Percent: common.GetPercent(used, total),
	}
}

func GetDistributionStatus(str string) string {
	d := cast.ToFloat64(str)
	if d >= 100 {
		return "error"
	} else if d >= 80 {
		return "warning"
	} else {
		return "success"
	}
}

func IsNodeOffline(node data.Object) bool {
	labels := node.Map("metadata", "labels")
	offlineLabel := labels.String(steve.OfflineLabel)
	return offlineLabel == "true"
}

func (t *Table) GetItemStatus(node data.Object) (*SteveStatus, error) {
	if node == nil {
		return nil, common.NodeNotFoundErr
	}
	if IsNodeOffline(node) {
		return &SteveStatus{
			Value:      t.SDK.I18n("isOffline"),
			RenderType: "textWithBadge",
			Status:     "default",
		}, nil
	}
	ss := &SteveStatus{
		RenderType: "textWithBadge",
	}
	strs := make([]string, 0)
	fields := node.StringSlice("metadata", "fields")
	for _, s := range strings.Split(fields[1], ",") {
		strs = append(strs, t.SDK.I18n(s))
	}
	for _, conf := range node.Slice("status", "conditions") {
		if conf.String("type") != "Ready" && conf.String("status") != "False" {
			strs = append(strs, t.SDK.I18n(conf.String("type")))
		}
	}
	ss.Value = strings.Join(strs, ",")
	if len(strs) == 1 && strs[0] == t.SDK.I18n("Ready") {
		ss.Status = "success"
	} else {
		ss.Status = "error"
	}
	return ss, nil
}

func (t *Table) GetDistributionValue(req, total float64, resourceType TableType) DistributionValue {
	return DistributionValue{
		Text:    t.GetScaleValue(req, total, resourceType),
		Percent: common.GetPercent(req, total),
	}
}

func (t *Table) GetDistributionRate(allocate, request float64, resourceType TableType) DistributionRate {
	if request == 0 {
		return DistributionRate{RenderType: "text", Value: t.SDK.I18n("None")}
	}
	rate := allocate / request
	rate, err := strconv.ParseFloat(fmt.Sprintf("%.3f", rate), 64)
	if err != nil {
		logrus.Error(err)
		return DistributionRate{RenderType: "text", Value: t.SDK.I18n("None")}
	}
	if rate <= 0.4 {
		return DistributionRate{RenderType: "text", Value: t.SDK.I18n("Low"), DistributionValue: rate}
	} else if rate <= 0.8 {
		return DistributionRate{RenderType: "text", Value: t.SDK.I18n("Middle"), DistributionValue: rate}
	} else {
		return DistributionRate{RenderType: "text", Value: t.SDK.I18n("High"), DistributionValue: rate}
	}
}

func (t *Table) GetScaleValue(a, b float64, Type TableType) string {
	level := []string{" B", " KiB", " MiB", " GiB", " TiB"}
	level2 := []string{"", " K", " M", " G", " T"}
	i := 0
	switch Type {
	case Memory:
		for ; a >= 1024 && b >= 1024 && i < 4; i++ {
			a /= 1024
			b /= 1024
		}
		return fmt.Sprintf("%.1f%s/%.1f%s", a, level[i], b, level[i])
	case Cpu:
		a /= 1000
		b /= 1000
		return fmt.Sprintf("%.3f/%.3f", a, b)
	case Pod:
		for ; a >= 1000 && b >= 1000 && i < 4; i++ {
			a /= 1000
			b /= 1000
		}
		return fmt.Sprintf("%d%s/%d%s", int64(a), level2[i], int64(b), level2[i])
	}
	return ""
}

// SetComponentValue mapping properties to Component
func (t *Table) SetComponentValue(c *cptype.Component) error {
	var err error
	if err = common.Transfer(t.State, &c.State); err != nil {
		return err
	}
	if err = common.Transfer(t.Props, &c.Props); err != nil {
		return err
	}
	if err = common.Transfer(t.Operations, &c.Operations); err != nil {
		return err
	}
	return nil
}

func (t *Table) RenderList(component *cptype.Component, tableType TableType, gs *cptype.GlobalStateData) error {
	var (
		err        error
		sortColumn string
		asc        bool
		items      []RowItem
		nodes      []data.Object
		request    map[string]cmp.AllocatedRes
	)
	clusterName := ""
	if t.SDK.InParams["clusterName"] != nil {
		clusterName = t.SDK.InParams["clusterName"].(string)
	} else {
		return common.ClusterNotFoundErr
	}
	wg := sync.WaitGroup{}
	wg.Add(2)
	nodes, err = t.GetNodes(t.Ctx, gs)
	if err != nil {
		logrus.Error(err)
	}
	go func() {
		request, err = cmp.GetNodesAllocatedRes(t.Ctx, t.Server, false, clusterName, t.SDK.Identity.UserID, t.SDK.Identity.OrgID, nodes)
		if err != nil {
			logrus.Error(err)
		}
		wg.Done()
	}()
	if tableType != Pod {
		go func() {
			t.GetMetrics(t.Ctx)
			wg.Done()
		}()
	} else {
		wg.Done()
	}
	wg.Wait()
	//if t.State.PageNo == 0 {
	//	t.State.PageNo = DefaultPageNo
	//}
	//if t.State.PageSize == 0 {
	//	t.State.PageSize = DefaultPageSize
	//}
	if t.State.Sorter.Field != "" {
		sortColumn = t.State.Sorter.Field
		asc = strings.ToLower(t.State.Sorter.Order) == "ascend"
	}

	if items, err = t.SetData(nodes, tableType, request); err != nil {
		return err
	}
	if sortColumn != "" {
		refCol := reflect.ValueOf(RowItem{}).FieldByName(sortColumn)
		switch refCol.Type() {
		case reflect.TypeOf(""):
			SortByString(items, sortColumn, asc)
		case reflect.TypeOf(Node{}):
			SortByNode(items, sortColumn, asc)
		case reflect.TypeOf(Role{}):
			SortByRole(items, sortColumn, asc)
		case reflect.TypeOf(Distribution{}):
			SortByDistribution(items, sortColumn, asc)
		case reflect.TypeOf(DistributionRate{}):
			SortByDistributionRate(items, sortColumn, asc)
		case reflect.TypeOf(SteveStatus{}):
			SortByStatus(items, sortColumn, asc)
		default:
			logrus.Errorf("sort by column %s not found", sortColumn)
			return common.TypeNotAvailableErr
		}
	}

	//t.State.Left = len(nodes)
	//start := (t.State.PageNo - 1) * t.State.PageSize
	//end := mathutil.Min(t.State.PageNo*t.State.PageSize, t.State.Left)

	//component.Data = map[string]interface{}{"list": items[start:end]}
	component.Data = map[string]interface{}{"list": items}
	return nil
}

// SetData assemble rowItem of table
func (t *Table) SetData(nodes []data.Object, tableType TableType, requests map[string]cmp.AllocatedRes) ([]RowItem, error) {
	switch tableType {
	case CPU_TAB:
		t.Props = t.CpuTable.GetProps()
		return t.CpuTable.GetRowItems(nodes, requests)
	case MEM_TAB:
		t.Props = t.MemTable.GetProps()
		return t.MemTable.GetRowItems(nodes, requests)
	case POD_TAB:
		t.Props = t.PodTable.GetProps()
		return t.PodTable.GetRowItems(nodes, requests)
	}
	return nil, nil
}

func (t *Table) GetNodes(ctx context.Context, gs *cptype.GlobalStateData) ([]data.Object, error) {
	var nodes []data.Object
	if (*gs)["nodes"] == nil {
		// Get all nodes by cluster name
		nodeReq := &apistructs.SteveRequest{}
		nodeReq.OrgID = t.SDK.Identity.OrgID
		nodeReq.UserID = t.SDK.Identity.UserID
		nodeReq.Type = apistructs.K8SNode
		if t.SDK.InParams["clusterName"] != nil {
			nodeReq.ClusterName = t.SDK.InParams["clusterName"].(string)
		} else {
			return nil, common.ClusterNotFoundErr
		}
		resp, err := t.Server.ListSteveResource(t.Ctx, nodeReq)
		if err != nil {
			return nil, err
		}
		for _, item := range resp {
			nodes = append(nodes, item.Data())
		}
		nodes = nodeFilter.DoFilter(nodes, t.State.Values)
	} else {
		nodes = (*gs)["nodes"].([]data.Object)
	}
	return nodes, nil
}

func (t *Table) GetMetrics(ctx context.Context) {
	// Get all nodes by cluster name
	req := &metrics.MetricsRequest{
		Cluster: t.SDK.InParams["clusterName"].(string),
		Type:    metrics.Cpu,
		Kind:    metrics.Node,
	}
	_, err := t.Metrics.NodeMetrics(ctx, req)
	if err != nil {
		logrus.Error(err)
	}
}

func (t *Table) GetPods(ctx context.Context) (map[string][]data.Object, error) {
	var podsMap = make(map[string][]data.Object)
	// Get all nodes by cluster name

	podReq := &apistructs.SteveRequest{}
	podReq.OrgID = t.SDK.Identity.OrgID
	podReq.UserID = t.SDK.Identity.UserID
	podReq.Type = apistructs.K8SPod
	if t.SDK.InParams["clusterName"] != nil {
		podReq.ClusterName = t.SDK.InParams["clusterName"].(string)
	} else {
		return nil, common.ClusterNotFoundErr
	}
	resp, err := t.Server.ListSteveResource(ctx, podReq)
	if err != nil {
		return nil, err
	}
	for _, pod := range resp {
		nodeName := pod.Data().StringSlice("metadata", "fields")[6]
		podsMap[nodeName] = append(podsMap[nodeName], pod.Data())
	}
	return podsMap, nil
}

func (t *Table) CordonNode(ctx context.Context, nodeNames []string) error {
	for _, name := range nodeNames {
		req := &apistructs.SteveRequest{
			UserID:      t.SDK.Identity.UserID,
			OrgID:       t.SDK.Identity.OrgID,
			Type:        apistructs.K8SNode,
			ClusterName: t.SDK.InParams["clusterName"].(string),
			Name:        name,
		}
		err := t.Server.CordonNode(t.Ctx, req)
		if err != nil {
			return err
		}
	}
	return nil
}

func (t *Table) UncordonNode(ctx context.Context, nodeNames []string) error {
	for _, name := range nodeNames {
		req := &apistructs.SteveRequest{
			UserID:      t.SDK.Identity.UserID,
			OrgID:       t.SDK.Identity.OrgID,
			Type:        apistructs.K8SNode,
			ClusterName: t.SDK.InParams["clusterName"].(string),
			Name:        name,
		}
		err := t.Server.UnCordonNode(t.Ctx, req)
		if err != nil {
			return err
		}
	}
	return nil
}

func (t *Table) GetNodeAddress(addrs []v1.NodeAddress) string {
	for _, addr := range addrs {
		if addr.Type == v1.NodeInternalIP {
			return addr.Address
		}
	}
	return ""
}

func (t *Table) GetTableOperation() map[string]interface{} {
	ops := map[string]Operation{
		//"changePageNo": {
		//	Key:    "changePageNo",
		//	Reload: true,
		//},
		//"changePageSize": {
		//	Key:    "changePageSize",
		//	Reload: true,
		//},
		"changeSort": {
			Key:    "changeSort",
			Reload: true,
		},
		"cordon": {
			Key:    "cordon",
			Reload: true,
			Text:   t.SDK.I18n("cordon"),
		},
		"uncordon": {
			Key:    "uncordon",
			Text:   t.SDK.I18n("uncordon"),
			Reload: true,
		},
		"drain": {
			Key:    "drain",
			Text:   t.SDK.I18n("drain"),
			Reload: true,
		},
		//"offline": {
		//	Key:    "offline",
		//	Text:   t.SDK.I18n("offline"),
		//	Reload: true,
		//},
		//"online": {
		//	Key:    "online",
		//	Text:   t.SDK.I18n("online"),
		//	Reload: true,
		//},
	}
	res := map[string]interface{}{}
	for key, op := range ops {
		res[key] = interface{}(op)
	}
	return res
}

func (t *Table) GetNodeLabels(labels data.Object) []label.Label {
	labelValues := make([]label.Label, 0)
	for key, value := range labels {
		l := fmt.Sprintf("%s=%s", key, value)
		group, displayName := t.GetLabelGroupAndDisplayName(l)
		lv := label.Label{
			Value: l,
			Name:  displayName,
			Group: group,
		}
		labelValues = append(labelValues, lv)
	}
	sort.Slice(labelValues, func(i, j int) bool {
		if labelValues[i].Group == labelValues[j].Group {
			return labelValues[i].Value < labelValues[j].Value
		}
		return labelValues[i].Group < labelValues[j].Group
	})
	return labelValues
}

func (t *Table) GetLabelGroupAndDisplayName(label string) (string, string) {

	groups := make(map[string]string)
	groups["dice/workspace-dev=true"] = t.SDK.I18n("env-label")
	groups["dice/workspace-test=true"] = t.SDK.I18n("env-label")
	groups["dice/workspace-staging=true"] = t.SDK.I18n("env-label")
	groups["dice/workspace-prod=true"] = t.SDK.I18n("env-label")

	groups["dice/stateful-service=true"] = t.SDK.I18n("service-label")
	groups["dice/stateless-service=true"] = t.SDK.I18n("service-label")
	groups["dice/location-cluster-service=true"] = t.SDK.I18n("service-label")

	groups["dice/job=true"] = t.SDK.I18n("job-label")
	groups["dice/bigdata-job=true"] = t.SDK.I18n("job-label")

	groups["dice/lb=true"] = t.SDK.I18n("other-label")
	groups["dice/platform=true"] = t.SDK.I18n("other-label")

	if group, ok := groups[label]; ok {
		return group, label
	}
	return t.SDK.I18n("other-label"), label
}

func (t *Table) GetLabelOperation(rowId string) map[string]Operation {
	return map[string]Operation{
		"add": {
			Key:    "addLabel",
			Reload: false,
			Command: &Command{
				Key: "set",
				Command: CommandState{
					Visible:  true,
					FormData: FormData{RecordId: rowId},
				},
				Target: "addLabelModal",
			},
		},
		"delete": {
			Key:      "deleteLabel",
			Reload:   true,
			FillMeta: "dlabel",
			Meta: map[string]interface{}{
				"recordId": rowId,
				"dlabel":   label.Label{Value: ""},
			},
		},
	}
}
func (t *Table) GetIp(node data.Object) string {
	for _, k := range node.Slice("status", "addresses") {
		if k.String("type") == "InternalIP" {
			return k.String("address")
		}
	}
	return ""
}

func (t *Table) GetRenders(id string, labelMap data.Object) []interface{} {
	ni := NodeIcon{
		Icon:       "default_k8s_node",
		RenderType: "icon",
	}
	nl := NodeLink{
		RenderType: "linkText",
		Value:      id,
		Operations: map[string]Operation{"click": {
			Key:    "gotoNodeDetail",
			Text:   t.SDK.I18n("nodeDetail"),
			Reload: false,
		},
		},
		Reload: false,
	}
	nt := Labels{
		RenderType: "tagsRow",
		Value:      t.GetNodeLabels(labelMap),
		Operations: t.GetLabelOperation(id),
	}
	return []interface{}{[]interface{}{ni}, []interface{}{nl, nt}}
}

func (t *Table) GetOperate(id string) Operate {
	obj := map[string]interface{}{
		"node": []string{
			id,
		},
	}
	bytes, _ := json.Marshal(obj)
	encode := base64.StdEncoding.EncodeToString(bytes)
	return Operate{
		RenderType: "tableOperation",
		Operations: map[string]Operation{
			"gotoPod": {Key: "gotoPod", Command: &Command{
				Key: "goto",
				Command: CommandState{
					Query: map[string]interface{}{
						"filter__urlQuery": encode,
					},
				},
				JumpOut: true,
				Target:  "cmpClustersPods",
			},
				Text:   t.SDK.I18n("show"),
				Reload: false,
			},
		},
	}
}

func (t *Table) DecodeURLQuery() error {
	query, ok := t.SDK.InParams["table__urlQuery"].(string)
	if !ok {
		return nil
	}
	decoded, err := base64.StdEncoding.DecodeString(query)
	if err != nil {
		return err
	}

	var values State
	if err := json.Unmarshal(decoded, &values); err != nil {
		return err
	}
	t.State.SelectedRowKeys = values.SelectedRowKeys
	t.State.Sorter = values.Sorter
	return nil
}

func (t *Table) EncodeURLQuery() error {
	jsonData, err := json.Marshal(t.State)
	if err != nil {
		return err
	}
	encoded := base64.StdEncoding.EncodeToString(jsonData)
	t.State.FilterUrlQuery = encoded
	return nil
}

// SortByString sort by string value
func SortByString(data []RowItem, sortColumn string, ascend bool) {
	sort.Slice(data, func(i, j int) bool {
		a := reflect.ValueOf(data[i])
		b := reflect.ValueOf(data[j])
		if ascend {
			return a.FieldByName(sortColumn).String() < b.FieldByName(sortColumn).String()
		}
		return a.FieldByName(sortColumn).String() > b.FieldByName(sortColumn).String()
	})
}

// SortByNode sort by node struct
func SortByNode(data []RowItem, _ string, ascend bool) {
	sort.Slice(data, func(i, j int) bool {
		if ascend {
			return data[i].Node.Renders[1].([]interface{})[0].(NodeLink).Value < data[j].Node.Renders[1].([]interface{})[0].(NodeLink).Value
		}
		return data[i].Node.Renders[1].([]interface{})[0].(NodeLink).Value > data[j].Node.Renders[1].([]interface{})[0].(NodeLink).Value
	})
}

// SortByRole sort by node struct
func SortByRole(data []RowItem, _ string, ascend bool) {
	sort.Slice(data, func(i, j int) bool {
		if ascend {
			return data[i].Role.Value.Label < data[j].Role.Value.Label
		}
		return data[i].Role.Value.Label > data[j].Role.Value.Label
	})
}

// SortByDistribution sort by percent
func SortByDistribution(data []RowItem, sortColumn string, ascend bool) {
	sort.Slice(data, func(i, j int) bool {
		a := reflect.ValueOf(data[i])
		b := reflect.ValueOf(data[j])
		aValue := cast.ToFloat64(a.FieldByName(sortColumn).FieldByName("Value").String())
		bValue := cast.ToFloat64(b.FieldByName(sortColumn).FieldByName("Value").String())
		if ascend {
			return aValue < bValue
		}
		return aValue > bValue
	})
}

// SortByDistributionRate sort by percent
func SortByDistributionRate(data []RowItem, sortColumn string, ascend bool) {
	sort.Slice(data, func(i, j int) bool {
		a := reflect.ValueOf(data[i])
		b := reflect.ValueOf(data[j])
		aValue := a.FieldByName(sortColumn).FieldByName("DistributionValue").Float()
		bValue := b.FieldByName(sortColumn).FieldByName("DistributionValue").Float()
		if ascend {
			return aValue < bValue
		}
		return aValue > bValue
	})
}

// SortByStatus sort by percent
func SortByStatus(data []RowItem, _ string, asc bool) {
	sort.Slice(data, func(i, j int) bool {
		if asc {
			return data[i].Status.Value < data[j].Status.Value
		}
		return data[i].Status.Value > data[j].Status.Value
	})
}

func IsNodeLabelInBlacklist(node data.Object) bool {
	labels := node.Map("metadata", "labels")
	for k, v := range labels {
		value, ok := v.(string)
		if !ok {
			continue
		}
		if s, ok := nodeLabelBlacklist[k]; ok && s == value {
			return true
		}
	}
	return false
}

type State struct {
	//PageNo          int           `json:"pageNo,omitempty"`
	//PageSize        int           `json:"pageSize,omitempty"`
	//Left           int           `json:"total,omitempty"`
	SelectedRowKeys []string      `json:"selectedRowKeys,omitempty"`
	Sorter          Sorter        `json:"sorterData,omitempty"`
	Values          filter.Values `json:"values,omitempty"`
	FilterUrlQuery  string        `json:"table__urlQuery,omitempty"`
}

type SteveStatus struct {
	Value      string `json:"value,omitempty"`
	RenderType string `json:"renderType"`
	Status     string `json:"status"`
}
