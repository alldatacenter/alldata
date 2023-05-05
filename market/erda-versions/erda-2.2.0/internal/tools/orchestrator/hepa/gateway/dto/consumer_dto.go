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

package dto

import (
	"github.com/erda-project/erda-proto-go/core/hepa/openapi_consumer/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type EndPoint struct {
	OuterAddr string `json:"outerAddr"`
	InnerAddr string `json:"innerAddr"`
	InnerTips string `json:"innerTips"`
}

type ConsumerDto struct {
	ConsumerCredentialsDto
	Endpoint        EndPoint `json:"endpoint"`
	GatewayInstance string   `json:"gatewayInstance"`
	ClusterName     string   `json:"clusterName"`
}

type ConsumerCredentialsDto struct {
	ConsumerName string                  `json:"consumerName"`
	ConsumerId   string                  `json:"consumerId"`
	AuthConfig   *orm.ConsumerAuthConfig `json:"authConfig"`
}

func (dto ConsumerCredentialsDto) ToCredentials() *pb.ConsumerCredentials {
	res := &pb.ConsumerCredentials{
		ConsumerName: dto.ConsumerName,
		ConsumerId:   dto.ConsumerId,
	}
	if dto.AuthConfig != nil {
		authDtos := dto.AuthConfig.Auths
		auths := []*pb.ConsumerAuthItem{}
		for _, dto := range authDtos {
			auths = append(auths, dto.ToAuth())
		}
		res.AuthConfig = &pb.ConsumerAuthConfig{
			Auths: auths,
		}
	}
	return res
}

func FromCredentials(creds *pb.ConsumerCredentials) *ConsumerCredentialsDto {
	res := &ConsumerCredentialsDto{
		ConsumerName: creds.ConsumerName,
		ConsumerId:   creds.ConsumerId,
	}
	if creds.AuthConfig != nil {
		auths := creds.AuthConfig.Auths
		authDtos := []orm.AuthItem{}
		for _, auth := range auths {
			authDtos = append(authDtos, orm.FromAuth(auth))
		}
		res.AuthConfig = &orm.ConsumerAuthConfig{
			Auths: authDtos,
		}
	}
	return res
}
