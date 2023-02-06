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

package impl

import (
	"context"
	"sync"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/exdto"
	kongDto "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_consumer"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_rule"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/org_client"
)

type GatewayOrgClientServiceImpl struct {
	clientDb    db.GatewayOrgClientService
	packageDb   db.GatewayPackageService
	consumerDb  db.GatewayConsumerService
	consumerBiz *openapi_consumer.GatewayOpenapiConsumerService
	ruleBiz     *openapi_rule.GatewayOpenapiRuleService
	reqCtx      context.Context
}

var once sync.Once

func NewGatewayOrgClientServiceImpl() (e error) {
	once.Do(
		func() {
			clientDb, err := db.NewGatewayOrgClientServiceImpl()
			if err != nil {
				e = err
				return
			}
			packageDb, err := db.NewGatewayPackageServiceImpl()
			if err != nil {
				e = err
				return
			}
			consumerDb, err := db.NewGatewayConsumerServiceImpl()
			if err != nil {
				e = err
				return
			}
			org_client.Service = &GatewayOrgClientServiceImpl{
				clientDb:    clientDb,
				packageDb:   packageDb,
				consumerDb:  consumerDb,
				consumerBiz: &openapi_consumer.Service,
				ruleBiz:     &openapi_rule.Service,
			}
		})
	return
}

func (impl GatewayOrgClientServiceImpl) Clone(ctx context.Context) org_client.GatewayOrgClientService {
	newService := impl
	newService.reqCtx = ctx
	return newService
}

func (impl GatewayOrgClientServiceImpl) Create(orgId, name string) (res dto.ClientInfoDto, exists bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	if orgId == "" || name == "" {
		err = errors.New("empty arguments")
		return
	}
	uniq, err := impl.clientDb.CheckUnique(&orm.GatewayOrgClient{
		OrgId: orgId,
		Name:  name,
	})
	if err != nil {
		return
	}
	if !uniq {
		exists = true
		err = errors.New("client already existed")
		return
	}
	secret, err := util.GenUniqueId()
	if err != nil {
		return
	}
	dao := &orm.GatewayOrgClient{
		OrgId:        orgId,
		Name:         name,
		ClientSecret: secret,
	}
	err = impl.clientDb.Insert(dao)
	if err != nil {
		return
	}
	res = dto.ClientInfoDto{
		ClientId:     dao.Id,
		ClientSecret: secret,
	}
	return
}

func (impl GatewayOrgClientServiceImpl) Delete(id string) (res bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	if id == "" {
		err = errors.New("empty arguments")
		return
	}
	consumers, err := impl.consumerDb.SelectByAny(&orm.GatewayConsumer{
		ClientId: id,
	})
	if err != nil {
		return
	}
	for _, consumer := range consumers {
		_, err = (*impl.consumerBiz).DeleteConsumer(consumer.Id)
		if err != nil {
			return
		}
	}
	err = impl.clientDb.DeleteById(id)
	if err != nil {
		return
	}
	res = true
	return
}

func (impl GatewayOrgClientServiceImpl) GetCredentials(id string) (res dto.ClientInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	if id == "" {
		err = errors.New("empty arguments")
		return
	}
	dao, err := impl.clientDb.GetById(id)
	if err != nil {
		return
	}
	res = dto.ClientInfoDto{
		ClientId:     dao.Id,
		ClientSecret: dao.ClientSecret,
	}
	return
}

func (impl GatewayOrgClientServiceImpl) UpdateCredentials(id string, secret ...string) (res dto.ClientInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	if id == "" {
		err = errors.New("empty arguments")
		return
	}
	dao, err := impl.clientDb.GetById(id)
	if err != nil {
		return
	}
	if dao == nil {
		err = errors.New("client not exist")
		return
	}
	newSecret, err := util.GenUniqueId()
	if err != nil {
		return
	}
	if len(secret) > 0 && secret[0] != "" {
		newSecret = secret[0]
	}
	consumers, err := impl.consumerDb.SelectByAny(&orm.GatewayConsumer{
		ClientId: dao.Id,
	})
	if err != nil {
		return
	}
	dao.ClientSecret = newSecret
	authConfig := &orm.ConsumerAuthConfig{
		Auths: []orm.AuthItem{
			{
				AuthType: orm.KEYAUTH,
				AuthData: kongDto.KongCredentialListDto{
					Data: []kongDto.KongCredentialDto{
						{Key: dao.Id},
					},
				},
			},
			{
				AuthType: orm.SIGNAUTH,
				AuthData: kongDto.KongCredentialListDto{
					Data: []kongDto.KongCredentialDto{
						{
							Key:    dao.Id,
							Secret: dao.ClientSecret,
						},
					},
				},
			},
			{
				AuthType: orm.OAUTH2,
				AuthData: kongDto.KongCredentialListDto{
					Data: []kongDto.KongCredentialDto{
						{
							Name:         dao.Name,
							ClientId:     dao.Id,
							ClientSecret: dao.ClientSecret,
						},
					},
				},
			},
		},
	}
	for _, consumer := range consumers {
		_, _, err = (*impl.consumerBiz).UpdateConsumerCredentials(consumer.Id, &dto.ConsumerCredentialsDto{
			AuthConfig: authConfig,
		})
		if err != nil {
			return
		}
	}
	err = impl.clientDb.Update(dao)
	if err != nil {
		return
	}
	res = dto.ClientInfoDto{
		ClientId:     dao.Id,
		ClientSecret: dao.ClientSecret,
	}
	return
}

func (impl GatewayOrgClientServiceImpl) GrantPackage(id, packageId string) (res bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	if id == "" || packageId == "" {
		err = errors.New("empty arguments")
		return
	}
	dao, err := impl.clientDb.GetById(id)
	if err != nil {
		return
	}
	if dao == nil {
		err = errors.New("client not found")
		return
	}
	pack, err := impl.packageDb.Get(packageId)
	if err != nil {
		return
	}
	if pack == nil {
		err = errors.New("endpoint not found")
		return
	}
	consumer, err := impl.getConsuemr(id, packageId)
	if err != nil {
		return
	}
	if consumer == nil {
		consumer, err = (*impl.consumerBiz).CreateClientConsumer(dao.Name, dao.Id, dao.ClientSecret, pack.DiceClusterName)
		if err != nil {
			return
		}
	}
	err = (*impl.consumerBiz).GrantPackageToConsumer(consumer.Id, packageId)
	if err != nil {
		return
	}
	res = true
	return
}

func (impl GatewayOrgClientServiceImpl) CreateOrUpdateLimit(id, packageId string, req exdto.ChangeLimitsReq) (res bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	if id == "" || packageId == "" {
		err = errors.New("empty arguments")
		return
	}
	consumer, err := impl.getConsuemr(id, packageId)
	if err != nil {
		return
	}
	if consumer == nil {
		err = errors.New("consumer not found")
		return
	}
	err = (*impl.ruleBiz).CreateOrUpdateLimitRule(consumer.Id, packageId, req.Limits)
	if err != nil {
		return
	}
	res = true
	return
}

func (impl GatewayOrgClientServiceImpl) getConsuemr(id, packageId string) (consumer *orm.GatewayConsumer, err error) {
	dao, err := impl.clientDb.GetById(id)
	if err != nil {
		return
	}
	if dao == nil {
		err = errors.New("client not found")
		return
	}
	pack, err := impl.packageDb.Get(packageId)
	if err != nil {
		return
	}
	if pack == nil {
		err = errors.New("endpoint not found")
		return
	}
	consumer, err = impl.consumerDb.GetByAny(&orm.GatewayConsumer{
		ClientId: id,
		Az:       pack.DiceClusterName,
		Type:     orm.APIM_CLIENT_CONSUMER,
	})
	if err != nil {
		return
	}
	return
}

func (impl GatewayOrgClientServiceImpl) RevokePackage(id, packageId string) (res bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	if id == "" || packageId == "" {
		err = errors.New("empty arguments")
		return
	}
	consumer, err := impl.getConsuemr(id, packageId)
	if err != nil {
		return
	}
	if consumer == nil {
		err = errors.New("consumer not found")
		return
	}
	err = (*impl.consumerBiz).RevokePackageFromConsumer(consumer.Id, packageId)
	if err != nil {
		return
	}
	res = true
	return
}
