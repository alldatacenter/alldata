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

package slb

import (
	"encoding/json"
	"errors"
	"fmt"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/slb"
	"github.com/appscode/go/strings"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/ecs"
	"github.com/erda-project/erda/pkg/strutil"
)

func ListByCluster(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string) (*slb.DescribeLoadBalancersResponse, error) {
	if strings.IsEmpty(&cluster) {
		err := fmt.Errorf("empty cluster name")
		logrus.Errorf(err.Error())
		return nil, err
	}

	response, err := DescribeResource(ctx, page, cluster)
	if err != nil {
		logrus.Errorf("describe slb failed")
	}

	return response, nil
}

func DescribeResource(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string) (*slb.DescribeLoadBalancersResponse, error) {
	// create client
	client, err := slb.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create slb client error: %+v", err)
		return nil, err
	}

	// create request
	request := slb.CreateDescribeLoadBalancersRequest()
	request.Scheme = "https"

	request.RegionId = ctx.Region

	if page.PageNumber == nil || page.PageSize == nil || *page.PageSize <= 0 || *page.PageNumber <= 0 {
		err := fmt.Errorf("invalid page parameters: %+v", page)
		logrus.Errorf(err.Error())
		return nil, err
	}
	if page.PageSize != nil {
		request.PageSize = requests.NewInteger(*page.PageSize)
	}
	if page.PageNumber != nil {
		request.PageNumber = requests.NewInteger(*page.PageNumber)
	}

	if !strings.IsEmpty(&ctx.VpcID) {
		request.VpcId = ctx.VpcID
	}

	if !strings.IsEmpty(&cluster) {
		tagKey, tagValue := aliyun_resources.GenClusterTag(cluster)
		request.Tag = &[]slb.DescribeLoadBalancersTag{{Key: tagKey, Value: tagValue}}
	}

	// describe resource
	// status:
	//	inactive
	//  active
	//  locked
	response, err := client.DescribeLoadBalancers(request)
	if err != nil {
		logrus.Errorf("describe slb error: %+v", err)
		return nil, err
	}

	return response, nil
}

func GetAllResourceIDs(ctx aliyun_resources.Context) ([]string, error) {
	var (
		page        aliyun_resources.PageOption
		instanceIDs []string
	)
	if strings.IsEmpty(&ctx.VpcID) {
		err := fmt.Errorf("get slb resource id failed, empty vpc id")
		logrus.Errorf(err.Error())
		return nil, err
	}

	pageNumber := 1
	pageSize := 15
	page.PageNumber = &pageNumber
	page.PageSize = &pageSize

	for {
		response, err := DescribeResource(ctx, page, "")
		if err != nil {
			return nil, err
		}
		for _, i := range response.LoadBalancers.LoadBalancer {
			instanceIDs = append(instanceIDs, i.LoadBalancerId)
		}
		if response.LoadBalancers.LoadBalancer == nil || pageNumber*pageSize >= response.TotalCount {
			break
		}
		pageNumber += 1
	}
	return instanceIDs, nil
}

func TagResource(ctx aliyun_resources.Context, cluster string, resourceIDs []string) error {
	if len(resourceIDs) == 0 {
		return nil
	}

	// create client
	client, err := slb.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create slb client error: %+v", err)
		return err
	}

	request := slb.CreateTagResourcesRequest()
	request.Scheme = "https"
	request.RegionId = ctx.Region
	request.ResourceType = "instance"
	request.ResourceId = &resourceIDs
	tagKey, tagValue := aliyun_resources.GenClusterTag(cluster)
	request.Tag = &[]slb.TagResourcesTag{{Key: tagKey, Value: tagValue}}

	_, err = client.TagResources(request)
	if err != nil {
		logrus.Errorf("tag slb resource failed, cluster: %s, resource ids: %+v, error: %+v", cluster, resourceIDs, err)
		return err
	}
	return nil
}

// - CreateLoadBalancer
// - CreateVServerGroup
// - CreateLoadBalancerTCPListener
func CreateIntranetSLBTCPListener(ctx aliyun_resources.Context,
	clustername string,
	slbreq *slb.CreateLoadBalancerRequest,
	slblistener *slb.CreateLoadBalancerTCPListenerRequest) (string, error) {
	if slbreq.LoadBalancerName == "" {
		return "", errors.New("LoadBalancerName is required")
	}

	bs, err := filterSLBECSNode(ctx, clustername, ctx.Region)
	if err != nil {
		return "", err
	}
	client, err := slb.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create slb client error: %+v", err)
		return "", err
	}

	var lbid string
	var vservergroupid string
	var resulterr error
	defer func() {
		if resulterr == nil {
			return
		}
		if lbid != "" {
			request := slb.CreateDeleteLoadBalancerRequest()
			request.Scheme = "https"
			request.LoadBalancerId = lbid
			if _, err := client.DeleteLoadBalancer(request); err != nil {
				logrus.Errorf("delete loadbalancer failed: %v, lbid: %s", err, lbid)
			}
		}
		if vservergroupid != "" {
			request := slb.CreateDeleteVServerGroupRequest()
			request.Scheme = "https"
			if _, err := client.DeleteVServerGroup(request); err != nil {
				logrus.Errorf("delete vservergroup failed: %v, vservergroupid: %s", err, vservergroupid)
			}
		}
	}()

	slbreq.Scheme = "https"
	slbreq.AddressType = "intranet"
	response, err := client.CreateLoadBalancer(slbreq)
	if err != nil {
		resulterr = err
		return "", err
	}
	lbid = response.LoadBalancerId

	request := slb.CreateCreateVServerGroupRequest()
	request.Scheme = "https"
	request.LoadBalancerId = lbid
	request.VServerGroupName = slbreq.LoadBalancerName + fmt.Sprintf("-%s-", lbid) + "vservergroup"
	backendservers, _ := json.Marshal(bs)
	request.BackendServers = string(backendservers)
	response1, err := client.CreateVServerGroup(request)
	if err != nil {
		resulterr = err
		return "", err
	}
	vservergroupid = response1.VServerGroupId

	slblistener.LoadBalancerId = lbid
	slblistener.VServerGroupId = vservergroupid
	if _, err := client.CreateLoadBalancerTCPListener(slblistener); err != nil {
		resulterr = err
		return "", err
	}
	startreq := slb.CreateStartLoadBalancerListenerRequest()
	startreq.Scheme = "https"
	startreq.LoadBalancerId = lbid
	startreq.ListenerPort = slblistener.ListenerPort
	if _, err := client.StartLoadBalancerListener(startreq); err != nil {
		return "", err
	}
	return lbid, nil
}

type backendserver struct {
	Description string `json:"Description"` // "test-112",
	Port        string `json:"Port"`        // "80",
	ServerIp    string `json:"ServerIp"`    // "192.168.**.**",
	Type        string `json:"Type"`        // "eni",
	Weight      string `json:"Weight"`      // "100",
	ServerId    string `json:"ServerId"`    // "eni-xxxxxxxxx"

}

func filterSLBECSNode(ctx aliyun_resources.Context, clustername string, region string) ([]backendserver, error) {
	bdl := bundle.New(bundle.WithScheduler())
	clusterinfo, err := bdl.QueryClusterInfo(clustername)
	if err != nil {
		return nil, err
	}
	LBAddr := clusterinfo.Get(apistructs.LB_ADDR)
	LBAddrs := strutil.Split(LBAddr, ",", true)
	if len(LBAddrs) == 0 {
		return nil, errors.New("failed to get LB_ADDR env")
	}

	LBIPs := []string{}
	for _, lb := range LBAddrs {
		ip := strutil.Split(lb, ":")[0]
		if ip == "" {
			return nil, errors.New("illegal LB_ADDR env")
		}
		LBIPs = append(LBIPs, ip)
	}

	ins, _, err := ecs.List(ctx, aliyun_resources.DefaultPageOption, []string{region}, "", LBIPs)
	if err != nil {
		return nil, err
	}
	if len(ins) != len(LBIPs) {
		return nil, fmt.Errorf("failed to find ecs(%v), ak: %s, region: %v", LBIPs, ctx.AccessKeyID, region)
	}
	bs := []backendserver{}
	for _, i := range ins {
		bs = append(bs, backendserver{
			Port:     "80",
			ServerIp: i.VpcAttributes.PrivateIpAddress.IpAddress[0],
			Type:     "ecs",
			Weight:   "100",
			ServerId: i.InstanceId,
		})
	}
	return bs, nil
}
