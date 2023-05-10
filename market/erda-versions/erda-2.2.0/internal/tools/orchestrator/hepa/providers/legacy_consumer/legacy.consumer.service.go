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

package legacy_consumer

import (
	"context"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/core/hepa/consumer/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/legacy_consumer"
	erdaErr "github.com/erda-project/erda/pkg/common/errors"
)

type legacyConsumerService struct {
	p *provider
}

func (s *legacyConsumerService) GetConsumer(ctx context.Context, req *pb.GetConsumerRequest) (resp *pb.GetConsumerResponse, err error) {
	service := legacy_consumer.Service.Clone(ctx)
	consumerDto, err := service.GetProjectConsumerInfo(req.OrgId, req.ProjectId, req.Env)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetConsumerResponse{
		ConsumerName: consumerDto.ClusterName,
		ConsumerId:   consumerDto.ConsumerId,
		Endpoint: &pb.Endpoint{
			OuterAddr: consumerDto.Endpoint.OuterAddr,
			InnerAddr: consumerDto.Endpoint.InnerAddr,
			InnerTips: consumerDto.Endpoint.InnerTips,
		},
		GatewayInstance: consumerDto.GatewayInstance,
		ClusterName:     consumerDto.ClusterName,
	}
	return
}
