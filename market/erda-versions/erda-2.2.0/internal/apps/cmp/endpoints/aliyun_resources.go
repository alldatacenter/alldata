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

package endpoints

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	sdkecs "github.com/aliyun/alibaba-cloud-sdk-go/services/ecs"
	sdkslb "github.com/aliyun/alibaba-cloud-sdk-go/services/slb"
	sdkvpc "github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/ack"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/ecs"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/eip"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/es"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/nas"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/rds"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/slb"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/crypto/encrypt"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) ListAliyunResources(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	return mkResponse(apistructs.ListCloudResourcesResponse{
		Header: apistructs.Header{Success: true},
		Data: []apistructs.ListCloudResourceTypeData{
			{
				Name:        aliyun_resources.CloudResourceTypeCompute.String(),
				DisplayName: i18n.Sprintf(aliyun_resources.CloudResourceTypeCompute.String()),
			},
			{
				Name:        aliyun_resources.CloudResourceTypeNetwork.String(),
				DisplayName: i18n.Sprintf(aliyun_resources.CloudResourceTypeNetwork.String()),
			},
			{
				Name:        aliyun_resources.CloudResourceTypeStorage.String(),
				DisplayName: i18n.Sprintf(aliyun_resources.CloudResourceTypeStorage.String()),
			},
			{
				Name:        aliyun_resources.CloudResourceTypeAddon.String(),
				DisplayName: i18n.Sprintf(aliyun_resources.CloudResourceTypeAddon.String()),
			},
		},
	})
}

func (e *Endpoints) TagResources(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.TagResourceRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		err := fmt.Errorf("unmarshal to TagResourceRequest failed, error: %+v", err)
		logrus.Errorf(err.Error())
		return mkResponse(apistructs.CloudClusterResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}
	logrus.Debugf("cloud-cluster request: %+v", req)
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		_, err := user.GetUserID(r)
		if err != nil {
			errStr := fmt.Sprintf("failed to get user-id in http header")
			return mkResponse(apistructs.CloudClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errStr},
				},
			})
		}
	}

	// for old existed cluster, if cloud related config is empty, try to get it form db
	if !req.IsNewCluster && (req.VpcID == "" || req.Region == "" ||
		req.AccessKey == "" || req.SecretKey == "" ||
		req.ClusterName == "") {

		logrus.Infof("get cmp config from db, cluster name: %v", req.ClusterName)

		ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
		resp, err := e.ClusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: req.ClusterName})
		if err != nil {
			logrus.Errorf("failed to get cluster info, cluster name: %v, error: %v", req.ClusterName, err)
			return mkResponse(apistructs.CloudResourcesDetailResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}

		ci := resp.Data
		if ci.OpsConfig == nil ||
			ci.OpsConfig.Region == "" ||
			ci.OpsConfig.AccessKey == "" ||
			ci.OpsConfig.SecretKey == "" ||
			ci.OpsConfig.VpcID == "" {
			logrus.Errorf("invalid opsconfig for cluster: %s, %v", req.ClusterName, ci.OpsConfig)
			return mkResponse(apistructs.CloudResourcesDetailResponse{
				Header: apistructs.Header{Success: true},
				Data:   nil,
			})
		}

		req.VpcID = ci.OpsConfig.VpcID
		req.Region = ci.OpsConfig.Region
		req.AccessKey = encrypt.AesDecrypt(ci.OpsConfig.AccessKey, apistructs.TerraformEcyKey)
		req.SecretKey = encrypt.AesDecrypt(ci.OpsConfig.SecretKey, apistructs.TerraformEcyKey)
	}

	ak_ctx := aliyun_resources.Context{
		AccessKeySecret: aliyun_resources.AccessKeySecret{
			AccessKeyID:  req.AccessKey,
			AccessSecret: req.SecretKey,
			Region:       req.Region,
		},
		VpcID: req.VpcID,
	}
	pagenum_ := 1
	pagesize_ := 20

	page := aliyun_resources.PageOption{
		PageNumber: &pagenum_,
		PageSize:   &pagesize_,
	}

	// vpc
	if req.VpcID != "" {
		if err := vpc.TagResource(ak_ctx, []string{req.ClusterName}, []string{req.VpcID}, aliyun_resources.TagResourceTypeVpc); err != nil {
			logrus.Errorf("tag vpc failed, cluster:%s error:%+v", req.ClusterName, err)
		}
	}

	// ack
	if err := ack.TagResource(ak_ctx, req.ClusterName, req.AckIDs); err != nil {
		logrus.Errorf("tag ack failed, cluster:%s error:%+v", req.ClusterName, err)
	}

	// ecs
	// for old existed cluster, if not ecs ids, get it by vpc
	if len(req.EcsIDs) == 0 && !req.IsNewCluster {
		ecsIDs, err := ecs.GetAllResourceIDs(ak_ctx)
		if err != nil {
			logrus.Errorf("get ecs ids failed, cluster:%s, error:%+v", req.ClusterName, err)
		} else {
			req.EcsIDs = ecsIDs
		}
		logrus.Debugf("try to get ecs ids by vpc id, ecs ids: %+v", ecsIDs)
	}
	// logrus.Debugf("tag resource request: %+v", req)

	if err := ecs.TagResource(ak_ctx, req.EcsIDs, []string{req.ClusterName}); err != nil {
		logrus.Errorf("tag ecs failed, error:%+v", err)
	}

	// eip
	natEipIDs, err := eip.GetEipIDByNat(ak_ctx, page, req.NatIDs)
	if err != nil {
		logrus.Errorf("get eip by nat gateway failed, cluster: %s, error: %+v", req.ClusterName, err)
	} else {
		req.EipIDs = append(req.EcsIDs, natEipIDs...)
	}
	slbEipIDs, err := eip.GetEipIDBySlb(ak_ctx, page, req.SlbIDs)
	if err != nil {
		logrus.Errorf("get eip by slb failed, cluster: %s, error: %+v", req.ClusterName, err)
	} else {
		req.EipIDs = append(req.EcsIDs, slbEipIDs...)
	}

	if err := eip.TagResource(ak_ctx, req.ClusterName, req.EipIDs); err != nil {
		logrus.Errorf("tag eip failed, error:%+v", err)
	}

	// es
	if err := es.TagResource(ak_ctx, req.ClusterName, req.EsIDs); err != nil {
		logrus.Errorf("tag es failed, error:%+v", err)
	}

	// nas
	if err := nas.TagResource(ak_ctx, req.ClusterName, req.NasIDs); err != nil {
		logrus.Errorf("tag nas failed, error:%+v", err)
	}

	// rds
	if err := rds.TagResource(ak_ctx, req.RdsIDs, []string{req.ClusterName}); err != nil {
		logrus.Errorf("tag rds failed, error:%+v", err)
	}

	// slb
	if err := slb.TagResource(ak_ctx, req.ClusterName, req.SlbIDs); err != nil {
		logrus.Errorf("tag slb failed, error:%+v", err)
	}

	return mkResponse(apistructs.CloudResourcesDetailResponse{
		Header: apistructs.Header{Success: true},
	})
}

func (e *Endpoints) QueryCloudResourceDetail(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	resource := strutil.ToUpper(r.URL.Query().Get("resource"))
	cluster := r.URL.Query().Get("cluster")

	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	resp, err := e.ClusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: cluster})
	if err != nil {
		return mkResponse(apistructs.CloudResourcesDetailResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}

	clusterinfo := resp.Data
	if clusterinfo.OpsConfig == nil ||
		clusterinfo.OpsConfig.Region == "" ||
		clusterinfo.OpsConfig.AccessKey == "" ||
		clusterinfo.OpsConfig.SecretKey == "" {
		logrus.Errorf("empty opsconfig for cluster: %s, %v", cluster, clusterinfo.OpsConfig)
		return mkResponse(apistructs.CloudResourcesDetailResponse{
			Header: apistructs.Header{Success: true},
			Data:   nil,
		})
	}

	ak_ctx := aliyun_resources.Context{
		AccessKeySecret: aliyun_resources.AccessKeySecret{
			AccessKeyID:  encrypt.AesDecrypt(clusterinfo.OpsConfig.AccessKey, apistructs.TerraformEcyKey),
			AccessSecret: encrypt.AesDecrypt(clusterinfo.OpsConfig.SecretKey, apistructs.TerraformEcyKey),
			Region:       clusterinfo.OpsConfig.Region,
		},
	}
	pagenum_ := 1
	pagesize_ := 20

	page := aliyun_resources.PageOption{
		PageNumber: &pagenum_,
		PageSize:   &pagesize_,
	}

	switch resource {
	case "COMPUTE":
		ecsResult, err := ecs.ListByCluster(ak_ctx, page, cluster)
		if err != nil {
			logrus.Errorf("ctx: %v, err: %v", ak_ctx, err)
			return mkResponseErr("", fmt.Sprintf("failed to list ecs resource: %v", err))
		}
		ackResult, err := ack.ListByCluster(ak_ctx, page, cluster)
		if err != nil {
			logrus.Errorf("cluster: %v, err: %v", cluster, err)
			return mkResponseErr("", fmt.Sprintf("failed to list ack resource: %v", err))
		}
		return mkResponse(apistructs.CloudResourcesDetailResponse{
			Header: apistructs.Header{Success: true},
			Data:   fmtComputeResourceTable(ctx, ecsResult, ackResult),
		})
	case "NETWORK":
		vpcResult, err := vpc.ListByCluster(ak_ctx, page, cluster)
		if err != nil {
			logrus.Errorf("ctx: %v, err: %v", ak_ctx, err)
			return mkResponseErr("", fmt.Sprintf("failed to list vpc resource: %v", err))
		}
		eipResult, err := eip.ListByCluster(ak_ctx, page, cluster)
		if err != nil {
			logrus.Errorf("ctx: %v, err: %v", ak_ctx, err)
			return mkResponseErr("", fmt.Sprintf("failed to list eip resource: %v", err))
		}
		slbResult, err := slb.ListByCluster(ak_ctx, page, cluster)
		if err != nil {
			logrus.Errorf("ctx: %v, err: %v", ak_ctx, err)
			return mkResponseErr("", fmt.Sprintf("failed to list slb resource: %v", err))
		}
		return mkResponse(apistructs.CloudResourcesDetailResponse{
			Header: apistructs.Header{Success: true},
			Data:   fmtNetworkResourceTable(ctx, vpcResult, eipResult, slbResult),
		})
	case "STORAGE":
		nasResult, err := nas.ListByCluster(ak_ctx, page, cluster)
		if err != nil {
			logrus.Errorf("ctx: %v, err: %v", ak_ctx, err)
			return mkResponseErr("", fmt.Sprintf("failed to list nas resource: %v", err))
		}
		return mkResponse(apistructs.CloudResourcesDetailResponse{
			Header: apistructs.Header{Success: true},
			Data:   fmtStorageResourceTable(ctx, &nasResult),
		})
	case "ADDON":
		rdsResult, err := rds.ListByCluster(ak_ctx, page, cluster)
		if err != nil {
			logrus.Errorf("ctx: %v, err: %v", ak_ctx, err)
			return mkResponseErr("", fmt.Sprintf("failed to list rds resource: %v", err))
		}
		esResult, err := es.ListByCluster(ak_ctx, page, cluster)
		if err != nil {
			logrus.Errorf("ctx: %v, err: %v", ak_ctx, err)
			return mkResponseErr("", fmt.Sprintf("failed to list es resource: %v", err))
		}
		return mkResponse(apistructs.CloudResourcesDetailResponse{
			Header: apistructs.Header{Success: true},
			Data:   fmtAddonsResourceTable(ctx, &rdsResult, &esResult),
		})
	default:
		return mkResponseErr("", fmt.Sprintf("unknown resource: %s", resource))
	}
}

func fmtComputeResourceTable(ctx context.Context, ecsResult *sdkecs.DescribeInstancesResponse, ackResult *ack.DescribeACKInstancesResponse) map[string]apistructs.CloudResourcesDetailData {
	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	ecs := apistructs.CloudResourcesDetailData{
		DisplayName: i18n.Sprintf("ECS"),
		LabelOrder:  []string{"id", "count"},
		Labels:      map[string]string{"id": "ID", "count": "count"},
		Data:        []map[string]string{{"id": "ECS", "count": strconv.Itoa(ecsResult.TotalCount)}},
	}
	ackData := []map[string]string{}
	for _, ack := range ackResult.Instances {
		ackData = append(ackData, map[string]string{
			"name":           ack.Name,
			"size":           strconv.Itoa(ack.Size),
			"clustertype":    ack.ClusterType,
			"currentversion": ack.CurrentVersion,
			"chargetype":     i18n.Sprintf(ack.Parameters.MasterInstanceChargeType),
			"state":          i18n.Sprintf(ack.State),
		})
	}
	ack := apistructs.CloudResourcesDetailData{
		DisplayName: i18n.Sprintf("Container Service for Kubernetes"),
		LabelOrder:  []string{"name", "size", "clustertype", "currentversion", "state", "chargetype"},
		Labels: map[string]string{
			"name":           i18n.Sprintf("Name"),
			"size":           i18n.Sprintf("NodeCount"),
			"clustertype":    i18n.Sprintf("ClusterType"),
			"currentversion": i18n.Sprintf("CurrentVersion"),
			"chargetype":     i18n.Sprintf("ChargeType"),
			"state":          i18n.Sprintf("State"),
		},
		Data: ackData,
	}
	response := map[string]apistructs.CloudResourcesDetailData{}
	if ecsResult.TotalCount > 0 {
		response["ECS"] = ecs
	}
	if len(ackData) > 0 {
		response["ACK"] = ack
	}
	return response
}

func fmtNetworkResourceTable(ctx context.Context,
	vpcResult *sdkvpc.DescribeVpcsResponse,
	eipResult *sdkvpc.DescribeEipAddressesResponse,
	slbResult *sdkslb.DescribeLoadBalancersResponse) map[string]apistructs.CloudResourcesDetailData {
	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	vpcData := []map[string]string{}
	for _, vpc := range vpcResult.Vpcs.Vpc {
		vpcData = append(vpcData, map[string]string{
			"id":        vpc.VpcId,
			"name":      vpc.VpcName,
			"status":    i18n.Sprintf(vpc.Status),
			"cidrblock": vpc.CidrBlock,
			"region":    vpc.RegionId,
			"natcount":  strconv.Itoa(len(vpc.NatGatewayIds.NatGatewayIds)),
		})
	}
	vpc := apistructs.CloudResourcesDetailData{
		DisplayName: i18n.Sprintf("VPC"),
		LabelOrder:  []string{"id", "name", "cidrblock", "region", "natcount", "status"},
		Labels: map[string]string{
			"id":        "ID",
			"name":      i18n.Sprintf("Name"),
			"status":    i18n.Sprintf("Status"),
			"cidrblock": i18n.Sprintf("CidrBlock"),
			"region":    i18n.Sprintf("Region"),
			"natcount":  i18n.Sprintf("NatGatewayCount"),
		},
		Data: vpcData,
	}

	eipData := []map[string]string{}
	for _, eip := range eipResult.EipAddresses.EipAddress {
		eipData = append(eipData, map[string]string{
			"name":       eip.Name,
			"ipaddress":  eip.IpAddress,
			"bandwidth":  eip.Bandwidth + "M",
			"chargetype": i18n.Sprintf(eip.ChargeType),
			"status":     i18n.Sprintf(eip.Status),
		})
	}
	eip := apistructs.CloudResourcesDetailData{
		DisplayName: i18n.Sprintf("EIP"),
		LabelOrder:  []string{"name", "ipaddress", "bandwidth", "status", "chargetype"},
		Labels: map[string]string{
			"name":       i18n.Sprintf("Name"),
			"ipaddress":  i18n.Sprintf("IpAddress"),
			"bandwidth":  i18n.Sprintf("Bandwidth"),
			"chargetype": i18n.Sprintf("ChargeType"),
			"status":     i18n.Sprintf("Status"),
		},
		Data: eipData,
	}
	slbData := []map[string]string{}
	for _, slb := range slbResult.LoadBalancers.LoadBalancer {

		if strings.Contains(strings.ToLower(slb.PayType), "pre") {
			slb.PayType = apistructs.PrePaidChargeType
		} else {
			slb.PayType = apistructs.PostPaidChargeType
		}

		slbData = append(slbData, map[string]string{
			"name":       slb.LoadBalancerName,
			"address":    slb.Address,
			"chargetype": i18n.Sprintf(slb.PayType),
			"status":     i18n.Sprintf(slb.LoadBalancerStatus),
		})
	}
	slb := apistructs.CloudResourcesDetailData{
		DisplayName: i18n.Sprintf("SLB"),
		LabelOrder:  []string{"name", "address", "status", "chargetype"},
		Labels: map[string]string{
			"name":       i18n.Sprintf("Name"),
			"address":    i18n.Sprintf("IpAddress"),
			"chargetype": i18n.Sprintf("ChargeType"),
			"status":     i18n.Sprintf("Status"),
		},
		Data: slbData,
	}
	response := map[string]apistructs.CloudResourcesDetailData{}
	if len(vpcData) > 0 {
		response["VPC"] = vpc
	}
	if len(eipData) > 0 {
		response["EIP"] = eip
	}
	if len(slbData) > 0 {
		response["SLB"] = slb
	}
	return response
}

func fmtStorageResourceTable(ctx context.Context,
	nasResult *nas.DescribeFileSystemResponse) map[string]apistructs.CloudResourcesDetailData {
	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	nasData := []map[string]string{}
	for _, nas := range nasResult.FileSystems {
		usedsize := strconv.FormatInt(nas.MeteredSize/1024/1024/1024, 10) // byte -> G
		nasData = append(nasData, map[string]string{
			"id":       nas.FileSystemId,
			"name":     nas.Description,
			"region":   nas.RegionId,
			"usedsize": usedsize + "G",
		})
	}
	nas := apistructs.CloudResourcesDetailData{
		DisplayName: i18n.Sprintf("NAS"),
		LabelOrder:  []string{"id", "name", "region", "usedsize"},
		Labels: map[string]string{
			"id":       "ID",
			"name":     i18n.Sprintf("Name"),
			"region":   i18n.Sprintf("Region"),
			"usedsize": i18n.Sprintf("UsedSize"),
		},
		Data: nasData,
	}

	response := map[string]apistructs.CloudResourcesDetailData{}
	if len(nasData) > 0 {
		response["NAS"] = nas
	}

	return response
}

func fmtAddonsResourceTable(ctx context.Context,
	rdsResult *rds.DescribeDBInstancesResponse,
	esResult *es.DescribeESInstancesResponse) map[string]apistructs.CloudResourcesDetailData {
	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	rdsData := []map[string]string{}
	for _, rds := range rdsResult.DBInstances {
		if strings.Contains(strings.ToLower(rds.PayType), "pre") {
			rds.PayType = apistructs.PrePaidChargeType
		} else {
			rds.PayType = apistructs.PostPaidChargeType
		}
		rdsData = append(rdsData, map[string]string{
			"id":         rds.VpcCloudInstanceId,
			"name":       rds.DBInstanceDescription,
			"spec":       rds.DBInstanceClass,
			"status":     i18n.Sprintf(rds.DBInstanceStatus),
			"chargetype": i18n.Sprintf(rds.PayType),
		})
	}

	rds := apistructs.CloudResourcesDetailData{
		DisplayName: i18n.Sprintf("RDS"),
		LabelOrder:  []string{"id", "name", "spec", "status", "chargetype"},
		Labels: map[string]string{
			"id":         "ID",
			"name":       i18n.Sprintf("Name"),
			"spec":       i18n.Sprintf("Spec"),
			"status":     i18n.Sprintf("Status"),
			"chargetype": i18n.Sprintf("ChargeType"),
		},
		Data: rdsData,
	}

	esData := []map[string]string{}
	for _, es := range esResult.Instances {
		if strings.Contains(strings.ToLower(es.PaymentType), "pre") {
			es.PaymentType = apistructs.PrePaidChargeType
		} else {
			es.PaymentType = apistructs.PostPaidChargeType
		}
		esData = append(esData, map[string]string{
			"id":         es.InstanceId,
			"name":       es.Description,
			"version":    es.Version,
			"chargetype": i18n.Sprintf(es.PaymentType),
			"nodespec":   es.NodeSpec.Spec,
			"nodecount":  strconv.Itoa(es.NodeAmount),
		})
	}
	es := apistructs.CloudResourcesDetailData{
		DisplayName: i18n.Sprintf("ElasticSearch"),
		LabelOrder:  []string{"id", "name", "version", "nodespec", "nodecount", "chargetype"},
		Labels: map[string]string{
			"id":         "ID",
			"name":       i18n.Sprintf("Name"),
			"version":    i18n.Sprintf("Version"),
			"chargetype": i18n.Sprintf("ChargeType"),
			"nodespec":   i18n.Sprintf("NodeSpec"),
			"nodecount":  i18n.Sprintf("NodeCount"),
		},
		Data: esData,
	}

	response := map[string]apistructs.CloudResourcesDetailData{}
	if len(rdsData) > 0 {
		response["RDS"] = rds
	}
	if len(esData) > 0 {
		response["ES"] = es
	}
	return response
}
