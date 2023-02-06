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

package kong

import (
	"net/http"
	"strings"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/base"
	v2 "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/v2"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
)

const InnerHost = "gateway.inner"

const (
	ConsumerRoot string = "/consumers/"
	PluginRoot   string = "/plugins/"
	ServiceRoot  string = "/services/"
	RouteRoot    string = "/routes/"
	AclRoot      string = "/acls/"
	UpstreamRoot string = "/upstreams/"
	HealthPath   string = "/health/"
	TargetPath   string = "/targets/"
)

var (
	ErrInvalidReq = errors.New("kongAdapter: invalid request")
)

func newKongAdapter(kongAddr string, client *http.Client) KongAdapter {
	var empty *base.KongAdapterImpl
	base := &base.KongAdapterImpl{
		KongAddr: kongAddr,
		Client:   client,
	}
	version, err := base.GetVersion()
	if err != nil {
		log.Errorf("get kong version failed, addr:%s", kongAddr)
		return empty
	}
	if strings.HasPrefix(version, "2.") {
		return &v2.KongAdapterImpl{
			KongAdapterImpl: base,
		}
	} else if strings.HasPrefix(version, "0.") {
		return base
	}
	log.Errorf("not support version:%s", version)
	return empty
}

func NewKongAdapter(kongAddr string) KongAdapter {
	client := &http.Client{}
	if config.ServerConf.KongDebug {
		return newKongAdapter(config.ServerConf.KongDebugAddr, client)
	}
	return newKongAdapter(kongAddr, client)
}

func NewKongAdapterForConsumer(consumer *orm.GatewayConsumer) KongAdapter {
	client := &http.Client{}
	if config.ServerConf.KongDebug {
		return newKongAdapter(config.ServerConf.KongDebugAddr, client)
	}
	azInfoService, err := service.NewGatewayAzInfoServiceImpl()
	if err != nil {
		log.Error(err)
		return nil
	}
	az, err := azInfoService.GetAz(&orm.GatewayAzInfo{
		OrgId:     consumer.OrgId,
		ProjectId: consumer.ProjectId,
		Env:       consumer.Env,
	})
	if err != nil {
		log.Error(err)
		return nil
	}
	return NewKongAdapterForProject(az, consumer.Env, consumer.ProjectId)
}

func NewKongAdapterForProject(az, env, projectId string) KongAdapter {
	client := &http.Client{}
	if config.ServerConf.KongDebug {
		return newKongAdapter(config.ServerConf.KongDebugAddr, client)
	}
	kongInfoService, err := service.NewGatewayKongInfoServiceImpl()
	if err != nil {
		log.Error(err)
		return nil
	}
	kong, err := kongInfoService.GetKongInfo(&orm.GatewayKongInfo{
		Az:        az,
		ProjectId: projectId,
		Env:       env,
	})
	if err != nil {
		log.Error(err)
		return nil
	}
	if kong == nil {
		log.Error("can't find kong")
		return nil
	}
	return newKongAdapter(kong.KongAddr, client)
}

func NewKongAdapterByConsumerId(consumerDb service.GatewayConsumerService, consumerId string) KongAdapter {
	client := &http.Client{}
	if config.ServerConf.KongDebug {
		return newKongAdapter(config.ServerConf.KongDebugAddr, client)
	}
	consumer, err := consumerDb.GetById(consumerId)
	if err != nil {
		log.Errorf("consumerDb failed[%+v]", errors.WithStack(err))
		return nil
	}
	if consumer == nil {
		log.Errorf("consumer[%s] not exists", consumerId)
		return nil
	}
	return NewKongAdapterForConsumer(consumer)
}
