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

package cloudapi

import (
	"strings"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	aliyun_cloudapi "github.com/aliyun/alibaba-cloud-sdk-go/services/cloudapi"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	resource_factory "github.com/erda-project/erda/internal/apps/cmp/impl/resource-factory"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

type GatewayVpcGrantFactory struct {
	*resource_factory.BaseResourceFactory
}

func vpcGrantCreator(ctx aliyun_resources.Context, m resource_factory.BaseResourceMaterial, r *dbclient.Record, d *apistructs.CreateCloudResourceRecord, v apistructs.CloudResourceVpcBaseInfo) (*apistructs.AddonConfigCallBackResponse, *dbclient.ResourceRouting, error) {
	var err error

	req, ok := m.(apistructs.ApiGatewayVpcGrantRequest)
	if !ok {
		return nil, nil, errors.Errorf("convert material failed, material: %+v", m)
	}

	logrus.Infof("start to create apigateway vpc grant, request: %+v", req)

	if req.ID == "" {
		greq := aliyun_cloudapi.CreateCreateInstanceRequest()
		greq.InstanceName = req.Name
		greq.InstanceSpec = req.Spec
		greq.HttpsPolicy = req.HttpsPolicy
		greq.Token = uuid.UUID()
		if strings.ToLower(req.ChargeType) == aliyun_resources.ChargeTypePrepaid {
			greq.ChargeType = "PrePay"
			greq.PricingCycle = "Month"
			greq.Duration = requests.Integer(req.ChargePeriod)
			greq.AutoPay = requests.NewBoolean(req.AutoRenew)
		} else if strings.ToLower(req.ChargeType) == "postpaid" {
			greq.ChargeType = "PostPay"
		}
		gid, err := CreateAPIGateway(ctx, greq)
		if err != nil {
			return nil, nil, errors.Wrap(err, "create api gateway failed")
		}
		req.ID = gid
	}

	grantName, err := CreateVpcGrant(ctx, &req)
	if err != nil {
		return nil, nil, errors.Wrap(err, "create vpc grant failed")
	}

	cbResp := &apistructs.AddonConfigCallBackResponse{
		Config: []apistructs.AddonConfigCallBackItemResponse{
			{
				Name: "ALIYUN_GATEWAY_INSTANCE_ID",
				// mysql intranet endpoint
				Value: req.ID,
			},
			{
				Name:  "ALIYUN_GATEWAY_VPC_GRANT",
				Value: grantName,
			},
		},
	}

	routing := &dbclient.ResourceRouting{
		ResourceID:   req.ID + ":" + req.Slb.ID,
		ResourceName: req.Name + ":" + req.Slb.Name,
		ResourceType: dbclient.ResourceTypeGateway,
		Vendor:       req.Vendor,
		OrgID:        req.OrgID,
		ClusterName:  req.ClusterName,
		ProjectID:    req.ProjectID,
		AddonID:      req.AddonID,
		Status:       dbclient.ResourceStatusAttached,
		RecordID:     r.ID,
	}
	return cbResp, routing, nil
}

func init() {
	vpcGrantFactory := GatewayVpcGrantFactory{BaseResourceFactory: &resource_factory.BaseResourceFactory{}}
	vpcGrantFactory.Creator = vpcGrantCreator
	vpcGrantFactory.RecordType = dbclient.RecordTypeCreateAliCloudGateway
	err := resource_factory.Register(dbclient.ResourceTypeGateway, vpcGrantFactory)
	if err != nil {
		panic(err)
	}

}
