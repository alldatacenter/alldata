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

package list

import (
	"context"
	"fmt"
	"os"
	"strconv"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/rancher/wrangler/pkg/data"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"
	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-cluster-list/common"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/cmp/metrics"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/k8sclient"
)

var (
	steveServer   cmp.SteveServer
	metricsServer metrics.Interface
)

func (l *List) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	mserver, ok := ctx.Service("cmp").(metrics.Interface)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a metrics server")
	}
	steveServer = server
	metricsServer = mserver

	return nil
}

func (l *List) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	var (
		err error
		d   map[string][]DataItem
	)

	l.SDK = cputil.SDK(ctx)
	bdl := ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	l.Bdl = bdl
	clusterSvc := ctx.Value(types.ClusterSvc).(clusterpb.ClusterServiceServer)
	l.ClusterSvc = clusterSvc
	err = l.GetComponentValue()
	if err != nil {
		return err
	}
	l.Ctx = ctx
	switch event.Operation {
	case cptype.DefaultRenderingKey, common.CMPClusterList, cptype.InitializeOperation:

	default:
		logrus.Warnf("operation [%s] not support, scenario:%v, event:%v", event.Operation, l, event)
		return nil
	}

	d, err = l.GetData(ctx)
	if err != nil {
		return err
	}
	l.Data = d
	err = l.SetComponentValue(c)
	if err != nil {
		return err
	}
	return nil
}

//func (l *List) GetMetrics(ctx context.Context, clusterName, orgName string) map[string]*metrics.MetricsData {
//	// Get all nodes by cluster name
//	req := &metrics.MetricsRequest{
//		UserId:           l.SDK.Identity.UserID,
//		OrgId:            l.SDK.Identity.OrgID,
//		Cluster:          clusterName,
//		OrganizationName: orgName,
//		Kind:             metrics.Node,
//	}
//	metricsData, err := metricsServer.NodeAllMetrics(ctx, req)
//	if err != nil {
//		logrus.Error(err)
//		return nil
//	}
//	return metricsData
//}

func (l *List) GetNodes(clusterName string) ([]data.Object, error) {
	var nodes []data.Object
	// Get all nodes by cluster name
	nodeReq := &apistructs.SteveRequest{}
	nodeReq.OrgID = l.SDK.Identity.OrgID
	nodeReq.UserID = l.SDK.Identity.UserID
	nodeReq.Type = apistructs.K8SNode
	nodeReq.ClusterName = clusterName
	resp, err := steveServer.ListSteveResource(l.Ctx, nodeReq)
	if err != nil {
		return nil, err
	}
	for _, item := range resp {
		nodes = append(nodes, item.Data())
	}
	return nodes, nil
}

func (l *List) GetState() State {
	return State{PageNo: false}
}

func (l *List) GetOperations(clusterInfo *clusterpb.ClusterInfo, status string) map[string]Operation {
	mapp := make(map[string]interface{})
	err := common.Transfer(clusterInfo, &mapp)

	nameMap := make(map[string]interface{})
	nameMap["name"] = clusterInfo.Name

	addCloudMachinesMap := make(map[string]interface{})
	addCloudMachinesMap["name"] = clusterInfo.Name
	addCloudMachinesMap["cloudVendor"] = clusterInfo.CloudVendor

	showRegisterCommandMap := make(map[string]interface{})
	showRegisterCommandMap["name"] = clusterInfo.Name
	showRegisterCommandMap["clusterStatus"] = status
	showRegisterCommandMap["initJobClusterName"] = os.Getenv("DICE_CLUSTER_NAME")
	showRegisterCommandMap["clusterInitContainerID"] = os.Getenv("DICE_CLUSTER_NAME")

	if err != nil {
		return nil
	}
	ops := map[string]Operation{
		"click": {
			Key:    "click",
			Reload: false,
			Show:   false,
			Meta:   nameMap,
		},
		"edit": {
			Key:    "edit",
			Reload: false,
			Text:   l.SDK.I18n("Edit Configuration"),
			Show:   true,
			Meta:   mapp,
		},
		"addMachine": {
			Key:    "addMachine",
			Reload: false,
			Text:   l.SDK.I18n("Add Machine"),
			Show:   true,
			Meta:   nameMap,
		},
		"addCloudMachines": {
			Key:    "addCloudMachines",
			Reload: false,
			Text:   l.SDK.I18n("Add Ali Cloud Machine"),
			Show:   true,
			Meta:   addCloudMachinesMap,
		},
		"upgrade": {
			Key:    "upgrade",
			Reload: false,
			Text:   l.SDK.I18n("Cluster Upgrade"),
			Show:   true,
			Meta:   nameMap,
		},
		"deleteCluster": {
			Key:    "deleteCluster",
			Reload: false,
			Text:   l.SDK.I18n("Cluster Offline"),
			Meta:   nameMap,
			Show:   true,
		},
		"tokenManagement": {
			Key:    "tokenManagement",
			Reload: false,
			Text:   l.SDK.I18n("Token Management"),
			Meta:   nameMap,
			Show:   true,
		},
	}
	manageType := common.ParseManageType(clusterInfo.ManageConfig)
	if clusterInfo.Type == "edas" && manageType == "agent" && !(clusterInfo.ManageConfig != nil && (clusterInfo.ManageConfig.Type == apistructs.ManageProxy &&
		clusterInfo.ManageConfig.AccessKey == "")) || clusterInfo.Type == "k8s" && manageType == "agent" {
		ops["showRegisterCommand"] = Operation{
			Key:    "showRegisterCommand",
			Reload: false,
			Show:   true,
			Text:   l.SDK.I18n("Registry Command"),
			//
			Meta: nameMap,
		}
	}
	if clusterInfo.Type == "k8s" || clusterInfo.Type == "edas" {
		if status == common.StatusUnknown || status == common.StatusInitializeError {
			ops["retryInit"] = Operation{
				Key:    "retryInit",
				Reload: false,
				Show:   true,
				Text:   l.SDK.I18n("Init"),
				Meta:   nameMap,
			}
		}
	}
	return ops
}
func (l *List) GetComponentValue() error {
	l.GetState()
	return nil
}

// SetComponentValue mapping properties to Component
func (l *List) SetComponentValue(c *cptype.Component) error {
	var err error
	if err = common.Transfer(l.State, &c.State); err != nil {
		return err
	}
	if err = common.Transfer(l.Data, &c.Data); err != nil {
		return err
	}
	//if err = common.Transfer(l.Props, &c.Props); err != nil {
	//	return err
	//}
	//if err = common.Transfer(l.Operations, &c.Operations); err != nil {
	//	return err
	//}
	return nil
}

func (l *List) GetData(ctx context.Context) (map[string][]DataItem, error) {
	var (
		err      error
		clusters []*clusterpb.ClusterInfo
		nodes    []data.Object
	)
	orgId, err := strconv.ParseUint(l.SDK.Identity.OrgID, 10, 64)
	if err != nil {
		logrus.Errorf("org id parse err :%v", err)
	}
	logrus.Infof("cluster start get data")
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	resp, err := l.ClusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{
		OrgID: uint32(orgId),
	})
	if err != nil {
		return nil, err
	}
	clusters = resp.Data

	wg := sync.WaitGroup{}
	wg.Add(2)
	res := make(map[string]*ResData)
	clusterNames := make([]string, 0)
	// cluster -> key -> value
	clusterInfos := make(map[string]*ClusterInfoDetail)
	for i := 0; i < len(clusters); i++ {
		res[clusters[i].Name] = &ResData{}
		clusterNames = append(clusterNames, clusters[i].Name)
		ci := &ClusterInfoDetail{}
		ci.Name = clusters[i].Name
		clusterInfos[clusters[i].Name] = ci
	}
	go func() {
		logrus.Infof("get nodes start")
		for i := 0; i < len(clusters); i++ {
			nodes, err = l.GetNodes(clusters[i].Name)
			if err != nil {
				logrus.Error(err)
			}
			allocatedRes, err := cmp.GetNodesAllocatedRes(ctx, steveServer, false, clusters[i].Name, l.SDK.Identity.UserID, l.SDK.Identity.OrgID, nodes)
			if err != nil {
				return
			}
			for _, m := range nodes {
				if cmp.IsVirtualNode(m) {
					continue
				}
				cpuCapacity, _ := resource.ParseQuantity(m.String("status", "capacity", "cpu"))
				memoryCapacity, _ := resource.ParseQuantity(m.String("status", "capacity", "memory"))
				podCapacity, _ := resource.ParseQuantity(m.String("status", "capacity", "pods"))
				res[clusters[i].Name].CpuTotal += float64(cpuCapacity.Value())
				res[clusters[i].Name].MemoryTotal += float64(memoryCapacity.Value())
				res[clusters[i].Name].PodTotal += float64(podCapacity.Value())
				id := m.String("metadata", "name")
				res[clusters[i].Name].CpuUsed += float64(allocatedRes[id].CPU) / 1000
				res[clusters[i].Name].MemoryUsed += float64(allocatedRes[id].Mem)
				res[clusters[i].Name].PodUsed += float64(allocatedRes[id].PodNum)
			}
			clusterInfos[clusters[i].Name].NodeCnt = len(nodes)
		}
		logrus.Infof("get nodes from steve finished")
		wg.Done()
	}()
	go func() {
		logrus.Infof("start query cluster info")
		for _, c := range clusters {
			if ci, err := l.Bdl.QueryClusterInfo(c.Name); err != nil {
				errStr := fmt.Sprintf("failed to queryclusterinfo: %v, cluster: %v", err, c.Name)
				logrus.Errorf(errStr)
			} else {
				clusterInfos[c.Name].Version = ci.Get(apistructs.DICE_VERSION)
				clusterInfos[c.Name].ClusterType = ci.Get(apistructs.DICE_CLUSTER_TYPE)
				clusterInfos[c.Name].Management = common.ParseManageType(c.ManageConfig)
				clusterInfos[c.Name].CreateTime = c.CreatedAt.AsTime().Local().Format("2006-01-02")
				clusterInfos[c.Name].UpdateTime = c.UpdatedAt.AsTime().Local().Format("2006-01-02")
				kc, err := k8sclient.NewWithTimeOut(c.Name, 2*time.Second)
				if err != nil {
					logrus.Error(err)
					continue
				}
				statusStr, err := common.GetClusterStatus(kc, c)
				if err != nil {
					logrus.Error(err)
				}
				status := ""
				//"pending","online","offline" ,"initializing","initialize error","unknown"

				//"success","error","default" ,"processing","warning"
				switch statusStr {
				case common.StatusInitializing:
					status = common.Processing
				case common.StatusOnline:
					status = common.Success
				case common.StatusUnknown:
					status = common.Default
				case common.StatusOffline, common.StatusPending:
					status = common.Warning
				case common.StatusInitializeError:
					status = common.Error
				}
				clusterInfos[c.Name].Status = status
				clusterInfos[c.Name].RawStatus = statusStr
			}
		}
		logrus.Infof("query cluster info finished")
		wg.Done()
	}()
	wg.Wait()
	di := make([]DataItem, 0)
	logrus.Infof("start set data")
	for _, c := range clusters {
		status := ItemStatus{Text: l.SDK.I18n(clusterInfos[c.Name].RawStatus), Status: clusterInfos[c.Name].Status}
		description := "-"
		if c.Description != "" {
			description = c.Description
		}
		displayName := c.DisplayName
		if displayName == "" {
			displayName = c.Name
		}
		i := DataItem{
			ID:            int(c.Id),
			Title:         displayName,
			Description:   description,
			PrefixImg:     "cluster",
			BackgroundImg: l.GetBgImage(c),
			ExtraInfos:    l.GetExtraInfos(clusterInfos[c.Name]),
			Status:        status,
			ExtraContent:  l.GetExtraContent(res[c.Name]),
			Operations:    l.GetOperations(c, clusterInfos[c.Name].RawStatus),
		}
		di = append(di, i)
	}

	d := make(map[string][]DataItem)
	d["list"] = di
	logrus.Infof("cluster get data finished")
	return d, nil
}
func (l *List) GetVersion(clusterName string) (string, error) {
	client, err := k8sclient.New(clusterName)
	if err != nil {
		return "", err
	}
	info, err := client.ClientSet.Discovery().ServerVersion()
	if err != nil {
		return "", err
	}
	return info.GitVersion, nil
}

func (l *List) GetBgImage(c *clusterpb.ClusterInfo) string {
	switch c.Type {
	case "k8s":
		return "k8s_cluster_bg"
	case "dcos":
		return "dcos_cluster_bg"
	case "edas":
		return "edas_cluster_bg"
	case "ack":
		return "ali_cloud_cluster_bg"
	default:
		return ""
	}
}

func (l *List) GetExtraInfos(clusterInfo *ClusterInfoDetail) []ExtraInfos {

	ei := make([]ExtraInfos, 0)
	ei = append(ei,
		ExtraInfos{
			Icon:    "management",
			Text:    l.WithManage(clusterInfo),
			Tooltip: l.SDK.I18n("manage type"),
		},

		ExtraInfos{
			Icon:    "machine",
			Text:    l.WithMachine(clusterInfo),
			Tooltip: l.SDK.I18n("machine count"),
		},
		ExtraInfos{
			Icon:    "version",
			Text:    l.WithVersion(clusterInfo),
			Tooltip: l.SDK.I18n("cluster version"),
		},
		ExtraInfos{
			Icon:    "time",
			Text:    l.WithUpdateTime(clusterInfo),
			Tooltip: l.SDK.I18n("update time"),
		},
		ExtraInfos{
			Icon:    "type",
			Text:    l.WithType(clusterInfo),
			Tooltip: l.SDK.I18n("cluster type"),
		},
	)
	return ei
}

func (l *List) WithVersion(clusterInfo *ClusterInfoDetail) string {
	if clusterInfo.Version == "" {
		return "-"
	} else {
		return clusterInfo.Version
	}
}
func (l *List) WithUpdateTime(clusterInfo *ClusterInfoDetail) string {
	if clusterInfo.UpdateTime == "" {
		return "-"
	} else {
		return clusterInfo.UpdateTime
	}
}
func (l *List) WithManage(clusterInfo *ClusterInfoDetail) string {
	if clusterInfo.Management == "" {
		return "-"
	} else {
		return l.SDK.I18n(clusterInfo.Management)
	}
}
func (l *List) WithType(clusterInfo *ClusterInfoDetail) string {
	if clusterInfo.ClusterType == "" {
		return "-"
	} else {
		if clusterInfo.ClusterType == "kubernetes" {
			vs, err := l.GetVersion(clusterInfo.Name)
			if err != nil {
				return clusterInfo.ClusterType
			}
			return clusterInfo.ClusterType + "(" + vs + ")"
		}
		return clusterInfo.ClusterType
	}
}
func (l *List) WithMachine(clusterInfo *ClusterInfoDetail) string {
	return fmt.Sprintf("%d", clusterInfo.NodeCnt)
}

func (l *List) GetExtraContent(res *ResData) ExtraContent {
	ec := ExtraContent{
		Type: "PieChart",
		//RowNum: 3,
	}
	cpuRate, memRate, podRate := 0.0, 0.0, 0.0
	if res.CpuTotal != 0 {
		cpuRate, _ = strconv.ParseFloat(fmt.Sprintf("%.1f", res.CpuUsed/res.CpuTotal*100), 64)
	}
	if res.MemoryTotal != 0 {
		memRate, _ = strconv.ParseFloat(fmt.Sprintf("%.1f", res.MemoryUsed/res.MemoryTotal*100), 64)
	}
	if res.PodTotal != 0 {
		podRate, _ = strconv.ParseFloat(fmt.Sprintf("%.1f", res.PodUsed/res.PodTotal*100), 64)
	}
	ec.ExtraData = []ExtraData{
		{
			Name:        l.SDK.I18n("CPU Rate"),
			Value:       cpuRate,
			Total:       100,
			Color:       "green",
			CenterLabel: "CPU",
			Info: []ExtraDataItem{
				{
					Main: fmt.Sprintf("%.1f%%", cpuRate),
					Sub:  l.SDK.I18n("Rate"),
				}, {
					Main: fmt.Sprintf("%.1f %s", res.CpuUsed, l.SDK.I18n("Core")),
					Sub:  l.SDK.I18n("Request"),
				}, {
					Main: fmt.Sprintf("%.1f %s", res.CpuTotal, l.SDK.I18n("Core")),
					Sub:  l.SDK.I18n("Limit"),
				},
			},
		},
		{
			Name:        l.SDK.I18n("Memory Rate"),
			Value:       memRate,
			Total:       100,
			Color:       "green",
			CenterLabel: l.SDK.I18n("Mem"),
			Info: []ExtraDataItem{
				{
					Main: fmt.Sprintf("%.1f%%", memRate),
					Sub:  l.SDK.I18n("Rate"),
				}, {
					Main: common.RescaleBinary(res.MemoryUsed),
					Sub:  l.SDK.I18n("Request"),
				}, {
					Main: common.RescaleBinary(res.MemoryTotal),
					Sub:  l.SDK.I18n("Limit"),
				},
			},
		},
		{
			Name:        l.SDK.I18n("Pod Rate"),
			Value:       podRate,
			Total:       100,
			Color:       "green",
			CenterLabel: l.SDK.I18n("Pod"),
			Info: []ExtraDataItem{
				{
					Main: fmt.Sprintf("%.1f%%", podRate),
					Sub:  l.SDK.I18n("Rate"),
				}, {
					Main: fmt.Sprintf("%d", int64(res.PodUsed)),
					Sub:  l.SDK.I18n("Request"),
				}, {
					Main: fmt.Sprintf("%d", int64(res.PodTotal)),
					Sub:  l.SDK.I18n("Limit"),
				},
			},
		},
	}
	return ec
}

func init() {
	base.InitProviderWithCreator("cmp-cluster-list", "list", func() servicehub.Provider {
		return &List{}
	})
}
