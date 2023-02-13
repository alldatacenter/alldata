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
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"sync"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/assembler"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	kongDto "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/global"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/legacy_consumer"
)

type GatewayConsumerServiceImpl struct {
	consumerDb    db.GatewayConsumerService
	policyDb      db.GatewayPolicyService
	apiDb         db.GatewayApiService
	consumerApiDb db.GatewayConsumerApiService
	azDb          db.GatewayAzInfoService
	kongDb        db.GatewayKongInfoService
	kongAssembler assembler.GatewayKongAssembler
	dbAssembler   assembler.GatewayDbAssembler
	globalBiz     *global.GatewayGlobalService
	reqCtx        context.Context
}

var once sync.Once

func NewGatewayConsumerServiceImpl() error {
	once.Do(
		func() {
			consumerDb, _ := db.NewGatewayConsumerServiceImpl()
			policyDb, _ := db.NewGatewayPolicyServiceImpl()
			apiDb, _ := db.NewGatewayApiServiceImpl()
			azDb, _ := db.NewGatewayAzInfoServiceImpl()
			kongDb, _ := db.NewGatewayKongInfoServiceImpl()
			consumerApiDb, _ := db.NewGatewayConsumerApiServiceImpl()

			legacy_consumer.Service = &GatewayConsumerServiceImpl{
				consumerDb:    consumerDb,
				policyDb:      policyDb,
				apiDb:         apiDb,
				consumerApiDb: consumerApiDb,
				azDb:          azDb,
				kongDb:        kongDb,
				kongAssembler: assembler.GatewayKongAssemblerImpl{},
				dbAssembler:   assembler.GatewayDbAssemblerImpl{},
				globalBiz:     &global.Service,
			}
		})
	return nil
}

func (impl GatewayConsumerServiceImpl) Clone(ctx context.Context) legacy_consumer.GatewayConsumerService {
	newService := impl
	newService.reqCtx = ctx
	return &newService
}

func (impl GatewayConsumerServiceImpl) CreateDefaultConsumer(orgId, projectId, env, az string) (*orm.GatewayConsumer, *orm.ConsumerAuthConfig, StandardErrorCode, error) {
	return impl.createConsumer(orgId, projectId, env, az, impl.consumerDb.GetDefaultConsumerName(&orm.GatewayConsumer{
		OrgId:     orgId,
		ProjectId: projectId,
		Env:       env,
		Az:        az,
	}), false)
}

func (impl GatewayConsumerServiceImpl) getCredentialList(kongAdapter kong.KongAdapter, consumerId string) (map[string]kongDto.KongCredentialListDto, error) {
	kCredentials, err := kongAdapter.GetCredentialList(consumerId, orm.KEYAUTH)
	if err != nil {
		kCredentials = &kongDto.KongCredentialListDto{}
	}
	oCredentials, err := kongAdapter.GetCredentialList(consumerId, orm.OAUTH2)
	if err != nil {
		oCredentials = &kongDto.KongCredentialListDto{}
	}
	return map[string]kongDto.KongCredentialListDto{
		orm.KEYAUTH: *kCredentials,
		orm.OAUTH2:  *oCredentials,
	}, nil
}

func (impl GatewayConsumerServiceImpl) UpdateConsumerInfo(consumerId string, consumerInfo *gw.ConsumerDto) *common.StandardResult {
	res := &common.StandardResult{Success: false}
	consumer, err := impl.consumerDb.GetById(consumerId)
	if err != nil || consumer == nil {
		log.Errorf("get consumer failed, err:%+v", err)
		return res.SetReturnCode(CONSUMER_NOT_EXIST)
	}
	kongAdapter := kong.NewKongAdapterForConsumer(consumer)
	credentialListMap, err := impl.getCredentialList(kongAdapter, consumer.ConsumerId)
	if err != nil {
		log.Errorf("get credential list failed, err:%+v", err)
		return res
	}
	newAuth := consumerInfo.AuthConfig
	adds := map[string][]kongDto.KongCredentialDto{}
	dels := map[string][]kongDto.KongCredentialDto{}
	for _, item := range newAuth.Auths {
		var oldCredentials []kongDto.KongCredentialDto
		credentialList := item.AuthData
		oldCredentialList, ok := credentialListMap[item.AuthType]
		if !ok {
			log.Warnf("can't find auth type[%s], need adds", item.AuthType)
			adds[item.AuthType] = credentialList.Data
			continue
		}
		oldCredentials = oldCredentialList.Data

		for _, credential := range credentialList.Data {
			needAdd := true
			for i := len(oldCredentials) - 1; i >= 0; i-- {
				if oldCredentials[i].Id == credential.Id {
					needAdd = false
					oldCredentials = append(oldCredentials[:i], oldCredentials[i+1:]...)
					break
				}
			}
			if needAdd {
				adds[item.AuthType] = append(adds[item.AuthType], credential)
			}
		}
		dels[item.AuthType] = oldCredentials
	}
	for authType, credentials := range adds {
		for _, credential := range credentials {
			if authType == orm.OAUTH2 {
				if credential.RedirectUrl == nil {
					credential.RedirectUrl = []string{"http://none"}
				} else {
					if url, ok := credential.RedirectUrl.(string); ok {
						if !strings.HasPrefix(url, "http") {
							url = "http://" + url
						}
						credential.RedirectUrl = []string{url}
					} else if urls, ok := credential.RedirectUrl.([]string); ok {
						for i := 0; i < len(urls); i++ {
							if urls[i] == "" {
								urls[i] = "http://none"
							} else if urls[i] == "http://" || urls[i] == "https://" {
								urls[i] += "none"
							}
						}
					}
				}
			}
			_, err = impl.createCredential(kongAdapter, authType, consumer.ConsumerId, &credential)
			if err != nil {
				log.Errorf("create credential failed, err:%+v", err)
				cstr, _ := json.Marshal(credential)
				return res.SetErrorInfo(&common.ErrInfo{
					Code: "创建失败",
					Msg: fmt.Sprintf("创建凭证失败,请检查是否已存在,凭证类型:%s, 凭证内容:%s",
						authType, cstr),
				})
			}
		}
	}
	for authType, credentials := range dels {
		for _, credential := range credentials {
			err = kongAdapter.DeleteCredential(consumer.ConsumerId, authType, credential.Id)
			if err != nil {
				log.Errorf("delete credential failed, err:%+v", err)
				return res
			}
		}
	}
	credentialListMap, err = impl.getCredentialList(kongAdapter, consumer.ConsumerId)
	if err != nil {
		log.Errorf("get credential list failed, err:%+v", err)
		return res
	}
	dto := &gw.ConsumerDto{
		ConsumerCredentialsDto: gw.ConsumerCredentialsDto{
			ConsumerId:   consumerId,
			ConsumerName: consumer.ConsumerName,
			AuthConfig: &orm.ConsumerAuthConfig{
				Auths: []orm.AuthItem{
					{
						AuthTips: orm.KeyAuthTips,
						AuthType: orm.KEYAUTH,
						AuthData: credentialListMap[orm.KEYAUTH],
					},
					{
						AuthType: orm.OAUTH2,
						AuthData: credentialListMap[orm.OAUTH2],
					},
				},
			},
		},
	}
	return res.SetSuccessAndData(dto)
}

func (impl GatewayConsumerServiceImpl) createConsumer(orgId, projectId, env, az, consumerName string, withCredential bool) (*orm.GatewayConsumer, *orm.ConsumerAuthConfig, StandardErrorCode, error) {
	ret := UNKNOW_ERROR
	consumer, err := impl.consumerDb.GetByName(consumerName)
	var respDto *kongDto.KongConsumerRespDto
	var kongAdapter kong.KongAdapter
	if err != nil {
		err = errors.WithStack(err)
		goto errorHappened
	}
	if consumer != nil {
		ret = CONSUMER_EXIST
		err = errors.Errorf("consumer[%s] alreay exist", consumerName)
		goto errorHappened
	}
	{
		var customId string
		customId, _ = util.GenUniqueId()
		var gatewayConsumer *orm.GatewayConsumer
		var keyAuth, oauth2 *kongDto.KongCredentialDto
		var consumerAuthConfig *orm.ConsumerAuthConfig
		reqDto := &kongDto.KongConsumerReqDto{
			Username: consumerName,
			CustomId: customId,
		}
		kongAdapter = kong.NewKongAdapterForProject(az, env, projectId)
		if kongAdapter == nil || !kongAdapter.KongExist() {
			err = errors.Errorf("no kong in az[%s]", az)
			ret = KONG_NOT_EXIST
			goto errorHappened
		}
		respDto, err = kongAdapter.CreateConsumer(reqDto)
		if err != nil {
			ret = CONSUMER_EXIST
			goto errorHappened
		}
		gatewayConsumer = &orm.GatewayConsumer{
			BaseRow:      orm.BaseRow{Id: customId},
			OrgId:        orgId,
			ProjectId:    projectId,
			Env:          env,
			Az:           az,
			ConsumerId:   respDto.Id,
			ConsumerName: consumerName,
		}
		if withCredential {
			keyAuth, err = impl.createCredential(kongAdapter, orm.KEYAUTH, respDto.Id, nil)
			if err != nil {
				goto errorHappened
			}
			oauth2, err = impl.createCredential(kongAdapter, orm.OAUTH2, respDto.Id,
				&kongDto.KongCredentialDto{
					Name:        "App",
					RedirectUrl: []string{"http://none"},
				})
			if err != nil {
				goto errorHappened
			}
			consumerAuthConfig = &orm.ConsumerAuthConfig{
				Auths: []orm.AuthItem{
					{
						AuthTips: orm.KeyAuthTips,
						AuthType: orm.KEYAUTH,
						AuthData: kongDto.KongCredentialListDto{
							Total: 1,
							Data:  []kongDto.KongCredentialDto{*keyAuth},
						},
					},
					{
						AuthType: orm.OAUTH2,
						AuthData: kongDto.KongCredentialListDto{
							Total: 1,
							Data:  []kongDto.KongCredentialDto{*oauth2},
						},
					},
				},
			}
		}
		err = impl.consumerDb.Insert(gatewayConsumer)
		if err != nil {
			goto errorHappened
		}
		err = kongAdapter.CreateAclGroup(respDto.Id, customId)
		if err != nil {
			goto errorHappened
		}
		return gatewayConsumer, consumerAuthConfig, ret, nil
	}
errorHappened:
	if respDto != nil {
		_ = kongAdapter.DeleteConsumer(respDto.Id)
	}
	return nil, nil, ret, err
}

func (impl GatewayConsumerServiceImpl) CreateConsumer(createDto *gw.ConsumerCreateDto) *common.StandardResult {
	res := &common.StandardResult{Success: false}
	var ret StandardErrorCode
	var err error = nil
	var orgId, az, consumerName string
	var gatewayConsumer *orm.GatewayConsumer
	var consumerAuthConfig *orm.ConsumerAuthConfig
	if createDto == nil || createDto.IsEmpty() {
		ret = PARAMS_IS_NULL
		err = errors.Errorf("invalid createDto[%+v]", createDto)
		goto errorHappened
	}
	if createDto.ConsumerName == "default" {
		ret = CONSUMER_EXIST
		goto errorHappened
	}
	orgId = strconv.Itoa(createDto.OrgId)
	az, err = impl.azDb.GetAz(&orm.GatewayAzInfo{
		OrgId:     orgId,
		ProjectId: createDto.ProjectId,
		Env:       createDto.Env,
	})
	if err != nil {
		ret = CLUSTER_NOT_EXIST
		goto errorHappened
	}
	consumerName = orgId + "_" + createDto.ProjectId + "_" + createDto.Env + "_" + az + "_" + createDto.ConsumerName
	gatewayConsumer, consumerAuthConfig, ret, err = impl.createConsumer(orgId, createDto.ProjectId, createDto.Env, az, consumerName, true)
	if err != nil {
		goto errorHappened
	}
	return res.SetSuccessAndData(&gw.CreateConsumerResp{
		ConsumerId:   gatewayConsumer.Id,
		ConsumerName: gatewayConsumer.ConsumerName,
		AuthConfig:   consumerAuthConfig,
	})
errorHappened:
	log.Errorf("error happened:%+v", err)
	return res.SetReturnCode(ret)
}

func (impl GatewayConsumerServiceImpl) createCredential(kongAdapter kong.KongAdapter, pluginName string, consumerId string, config *kongDto.KongCredentialDto) (*kongDto.KongCredentialDto, error) {
	req := &kongDto.KongCredentialReqDto{}
	req.ConsumerId = consumerId
	req.PluginName = pluginName
	req.Config = config
	return kongAdapter.CreateCredential(req)
}

func (impl GatewayConsumerServiceImpl) GetConsumerInfo(consumerId string) *common.StandardResult {
	res := &common.StandardResult{Success: false}
	consumer, err := impl.consumerDb.GetById(consumerId)
	if err != nil || consumer == nil {
		log.Errorf("get consumer failed, err:%+v", err)
		return res.SetReturnCode(CONSUMER_NOT_EXIST)
	}
	kongAdapter := kong.NewKongAdapterForConsumer(consumer)
	credentialListMap, err := impl.getCredentialList(kongAdapter, consumer.ConsumerId)
	if err != nil {
		log.Errorf("get credential list failed, err:%+v", err)
		return res
	}
	dto := &gw.ConsumerDto{
		ConsumerCredentialsDto: gw.ConsumerCredentialsDto{
			ConsumerId:   consumerId,
			ConsumerName: consumer.ConsumerName,
			AuthConfig: &orm.ConsumerAuthConfig{
				Auths: []orm.AuthItem{
					{
						AuthTips: orm.KeyAuthTips,
						AuthType: orm.KEYAUTH,
						AuthData: credentialListMap[orm.KEYAUTH],
					},
					{
						AuthType: orm.OAUTH2,
						AuthData: credentialListMap[orm.OAUTH2],
					},
				},
			},
		},
	}
	return res.SetSuccessAndData(dto)
}

func (impl GatewayConsumerServiceImpl) GetProjectConsumerInfo(orgId string, projectId string, env string) (dto *dto.ConsumerDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened:%+v", err)
		}
	}()
	if len(orgId) == 0 || len(projectId) == 0 {
		err = errors.Errorf("invalid args: orgId[%s] projectId[%s]", orgId, projectId)
		return
	}

	var az string
	var consumer = new(orm.GatewayConsumer)
	var kongInfo *orm.GatewayKongInfo
	var outerAddr string
	az, err = impl.azDb.GetAz(&orm.GatewayAzInfo{
		OrgId:     orgId,
		ProjectId: projectId,
		Env:       env,
	})
	if err != nil {
		return
	}
	if az == "" {
		err = errors.New("empty az")
		return
	}
	consumer, err = impl.consumerDb.GetDefaultConsumer(&orm.GatewayConsumer{
		OrgId:     orgId,
		ProjectId: projectId,
		Env:       env,
		Az:        az,
	})
	if err != nil {
		return
	}
	if consumer == nil {
		consumer, _, _, err = impl.CreateDefaultConsumer(orgId, projectId, env, az)
		if err != nil {
			return
		}
	}
	kongInfo, err = impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        az,
		ProjectId: projectId,
		Env:       env,
	})
	if err != nil {
		return
	}
	outerAddr = kongInfo.Endpoint
	if !strings.EqualFold(env, ENV_TYPE_PROD) {
		outerAddr = strings.ToLower(env + config.ServerConf.SubDomainSplit + outerAddr)
	}
	dto = &gw.ConsumerDto{
		ConsumerCredentialsDto: gw.ConsumerCredentialsDto{
			ConsumerId:   consumer.Id,
			ConsumerName: consumer.ConsumerName,
		},
		Endpoint: gw.EndPoint{
			OuterAddr: outerAddr,
			InnerAddr: (*impl.globalBiz).GetServiceAddr(consumer.Env),
			InnerTips: "",
		},
		GatewayInstance: kongInfo.AddonInstanceId,
		ClusterName:     az,
	}
	return
}

func (impl GatewayConsumerServiceImpl) GetConsumerList(orgId string, projectId string, env string) *common.StandardResult {
	res := &common.StandardResult{Success: false}
	ret := UNKNOW_ERROR
	var err error
	var az, defaultName string
	var cond *orm.GatewayConsumer
	consumerAllResp := &gw.ConsumerAllResp{}
	var consumerList []orm.GatewayConsumer
	consumerDtos := []gw.ConsumerInfoDto{}
	if len(orgId) == 0 || len(projectId) == 0 || len(env) == 0 {
		err = errors.New("invalid arg")
		ret = CONSUMER_PARAMS_MISS
		goto errorHappened
	}
	az, err = impl.azDb.GetAz(&orm.GatewayAzInfo{
		OrgId:     orgId,
		ProjectId: projectId,
		Env:       env,
	})
	if err != nil {
		ret = CLUSTER_NOT_EXIST
		goto errorHappened
	}
	cond = &orm.GatewayConsumer{
		OrgId:     orgId,
		ProjectId: projectId,
		Env:       env,
		Az:        az,
	}
	consumerList, err = impl.consumerDb.SelectByAny(cond)
	if err != nil {
		goto errorHappened
	}
	defaultName = impl.consumerDb.GetDefaultConsumerName(cond)
	for _, consumer := range consumerList {
		var consumerDto *gw.ConsumerInfoDto
		if consumer.ConsumerName == defaultName {
			continue
		}
		consumerDto, err = impl.dbAssembler.BuildConsumerInfo(&consumer)
		var consumerApis []orm.GatewayConsumerApi
		consumerApiDtos := []gw.ConsumerApiInfoDto{}
		if err != nil {
			err = errors.WithStack(err)
			goto errorHappened
		}
		consumerApis, err = impl.consumerApiDb.SelectByConsumer(consumer.Id)
		if err != nil {
			err = errors.WithStack(err)
			goto errorHappened
		}
		if len(consumerApis) == 0 {
			consumerDtos = append(consumerDtos, *consumerDto)
			continue
		}
		for _, consumerApi := range consumerApis {
			var api *orm.GatewayApi
			var consumerApiDto *gw.ConsumerApiInfoDto
			api, err = impl.apiDb.GetById(consumerApi.ApiId)
			if err != nil {
				err = errors.WithStack(err)
				goto errorHappened
			}
			if api == nil {
				log.Warnf("api of id[%s] is nil", consumerApi.ApiId)
				continue
			}
			consumerApiDto, err = impl.dbAssembler.BuildConsumerApiInfo(&consumerApi, api)
			if err != nil {
				err = errors.WithStack(err)
				goto errorHappened
			}
			if len(consumerApi.Policies) > 0 {
				policyList := []string{}
				err = json.Unmarshal([]byte(consumerApi.Policies), &policyList)
				if err != nil {
					err = errors.WithStack(err)
					goto errorHappened
				}
				if len(policyList) > 0 {
					var policiesList []orm.GatewayPolicy
					policiesList, err = impl.policyDb.SelectInIds(policyList...)
					if err != nil {
						err = errors.WithStack(err)
						goto errorHappened
					}
					for _, policy := range policiesList {
						if policy.Category == TRAFFIC_CONTROL.Name {
							var policyDto *gw.ConsumerApiPolicyInfoDto
							policyDto, err = impl.dbAssembler.BuildConsumerApiPolicyInfo(&policy)
							if err != nil {
								err = errors.WithStack(err)
								goto errorHappened
							}
							consumerApiDto.RateLimitPolicy = *policyDto

						}
					}
				}
			}
			consumerApiDtos = append(consumerApiDtos, *consumerApiDto)
		}
		consumerDto.ConsumerApiInfos = consumerApiDtos
		consumerDtos = append(consumerDtos, *consumerDto)
	}
	consumerAllResp.Consumers = consumerDtos
	return res.SetSuccessAndData(consumerAllResp)
errorHappened:
	log.Errorf("error happened:%+v", err)
	return res.SetReturnCode(ret)

}
func (impl GatewayConsumerServiceImpl) DeleteConsumer(consumerId string) *common.StandardResult {
	res := &common.StandardResult{Success: false}
	ret := UNKNOW_ERROR
	var err error
	var consumer *orm.GatewayConsumer
	if len(consumerId) == 0 {
		ret = PARAMS_IS_NULL
		err = errors.New("consumerId is empty")
		goto errorHappened
	}
	consumer, err = impl.consumerDb.GetById(consumerId)
	if err != nil {
		err = errors.WithStack(err)
		goto errorHappened
	}
	if consumer == nil {
		ret = CONSUMER_NOT_EXIST
		err = errors.New("consumer is nil")
		goto errorHappened
	}
	err = impl.consumerDb.DeleteById(consumerId)
	if err != nil {
		err = errors.WithStack(err)
		goto errorHappened
	}
	_ = kong.NewKongAdapterForConsumer(consumer).DeleteConsumer(consumer.ConsumerId)
	return res.SetSuccessAndData(true)
errorHappened:
	log.Errorf("error happened:%+v", err)
	return res.SetReturnCode(ret)
}
