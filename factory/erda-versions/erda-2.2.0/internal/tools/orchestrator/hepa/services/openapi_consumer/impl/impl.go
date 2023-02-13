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
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"strings"
	"sync"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	kongDto "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_consumer"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_rule"
)

type GatewayOpenapiConsumerServiceImpl struct {
	packageDb      db.GatewayPackageService
	packageApiDb   db.GatewayPackageApiService
	consumerDb     db.GatewayConsumerService
	azDb           db.GatewayAzInfoService
	kongDb         db.GatewayKongInfoService
	packageInDb    db.GatewayPackageInConsumerService
	packageApiInDb db.GatewayPackageApiInConsumerService
	ruleBiz        *openapi_rule.GatewayOpenapiRuleService
	reqCtx         context.Context
}

var once sync.Once

func NewGatewayOpenapiConsumerServiceImpl() error {
	once.Do(
		func() {
			packageDb, _ := db.NewGatewayPackageServiceImpl()
			packageApiDb, _ := db.NewGatewayPackageApiServiceImpl()
			consumerDb, _ := db.NewGatewayConsumerServiceImpl()
			azDb, _ := db.NewGatewayAzInfoServiceImpl()
			kongDb, _ := db.NewGatewayKongInfoServiceImpl()
			packageInDb, _ := db.NewGatewayPackageInConsumerServiceImpl()
			packageApiInDb, _ := db.NewGatewayPackageApiInConsumerServiceImpl()
			openapi_consumer.Service = &GatewayOpenapiConsumerServiceImpl{
				consumerDb:     consumerDb,
				azDb:           azDb,
				kongDb:         kongDb,
				packageInDb:    packageInDb,
				packageApiInDb: packageApiInDb,
				packageDb:      packageDb,
				packageApiDb:   packageApiDb,
				ruleBiz:        &openapi_rule.Service,
			}
		})
	return nil
}

func (impl GatewayOpenapiConsumerServiceImpl) Clone(ctx context.Context) openapi_consumer.GatewayOpenapiConsumerService {
	newService := impl
	newService.reqCtx = ctx
	return &newService
}

func (impl GatewayOpenapiConsumerServiceImpl) GetKongConsumerName(consumer *orm.GatewayConsumer) string {
	if consumer.ProjectId == "" {
		return consumer.ConsumerName
	}
	return fmt.Sprintf("%s.%s.%s.%s:%s", consumer.OrgId, consumer.ProjectId, consumer.Env, consumer.Az, consumer.ConsumerName)
}

func (impl GatewayOpenapiConsumerServiceImpl) getCredentialList(kongAdapter kong.KongAdapter, consumerId string) (map[string]kongDto.KongCredentialListDto, error) {
	kCredentials, err := kongAdapter.GetCredentialList(consumerId, orm.KEYAUTH)
	if err != nil {
		kCredentials = &kongDto.KongCredentialListDto{
			Data: []kongDto.KongCredentialDto{},
		}
	}
	oCredentials, err := kongAdapter.GetCredentialList(consumerId, orm.OAUTH2)
	if err != nil {
		oCredentials = &kongDto.KongCredentialListDto{
			Data: []kongDto.KongCredentialDto{},
		}
	}
	sCredentials, err := kongAdapter.GetCredentialList(consumerId, orm.SIGNAUTH)
	if err != nil {
		sCredentials = &kongDto.KongCredentialListDto{
			Data: []kongDto.KongCredentialDto{},
		}
	}
	hCredentials, err := kongAdapter.GetCredentialList(consumerId, orm.HMACAUTH)
	if err != nil {
		hCredentials = &kongDto.KongCredentialListDto{
			Data: []kongDto.KongCredentialDto{},
		}
	}
	return map[string]kongDto.KongCredentialListDto{
		orm.KEYAUTH:  *kCredentials,
		orm.OAUTH2:   *oCredentials,
		orm.SIGNAUTH: *sCredentials,
		orm.HMACAUTH: *hCredentials,
	}, nil
}

func (impl GatewayOpenapiConsumerServiceImpl) createCredential(kongAdapter kong.KongAdapter, pluginName string, consumerId string, config *kongDto.KongCredentialDto) (*kongDto.KongCredentialDto, error) {
	req := &kongDto.KongCredentialReqDto{}
	req.ConsumerId = consumerId
	req.PluginName = pluginName
	req.Config = config
	if pluginName == orm.HMACAUTH {
		enabled, err := kongAdapter.CheckPluginEnabled(pluginName)
		if err != nil {
			return nil, err
		}
		if !enabled {
			return &kongDto.KongCredentialDto{}, nil
		}
	}
	return kongAdapter.CreateCredential(req)
}

func (impl GatewayOpenapiConsumerServiceImpl) CreateClientConsumer(clientName, clientId, clientSecret, clusterName string) (consumer *orm.GatewayConsumer, err error) {
	dao, err := impl.consumerDb.GetByAny(&orm.GatewayConsumer{
		ConsumerName: clientName,
		Az:           clusterName,
		Type:         orm.APIM_CLIENT_CONSUMER,
	})
	if err != nil {
		return
	}
	if dao != nil {
		err = errors.Errorf("consumer:%s already exist in cluster:%s", clientName, clusterName)
		return
	}
	consumerId, err := util.GenUniqueId()
	if err != nil {
		return
	}
	consumer = &orm.GatewayConsumer{
		BaseRow:      orm.BaseRow{Id: consumerId},
		ClientId:     clientId,
		ConsumerName: clientName,
		Az:           clusterName,
		Type:         orm.APIM_CLIENT_CONSUMER,
	}
	kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az: clusterName,
	})
	if err != nil {
		return
	}
	kongAdapter := kong.NewKongAdapter(kongInfo.KongAddr)
	reqDto := &kongDto.KongConsumerReqDto{
		Username: clientName,
		CustomId: consumerId,
	}
	respDto, err := kongAdapter.CreateConsumer(reqDto)
	if err != nil {
		return
	}
	consumer.ConsumerId = respDto.Id
	err = impl.consumerDb.Insert(consumer)
	if err != nil {
		return
	}
	err = kongAdapter.CreateAclGroup(respDto.Id, reqDto.Username)
	if err != nil {
		return
	}
	_, err = impl.createCredential(kongAdapter, orm.KEYAUTH, respDto.Id, &kongDto.KongCredentialDto{
		Key: clientId,
	})
	if err != nil {
		return
	}
	_, err = impl.createCredential(kongAdapter, orm.OAUTH2, respDto.Id,
		&kongDto.KongCredentialDto{
			Name:         clientName,
			RedirectUrl:  []string{"http://none"},
			ClientId:     clientId,
			ClientSecret: clientSecret,
		})
	if err != nil {
		return
	}
	_, err = impl.createCredential(kongAdapter, orm.SIGNAUTH, respDto.Id, &kongDto.KongCredentialDto{
		Key:    clientId,
		Secret: clientSecret,
	})
	if err != nil {
		return
	}
	_, err = impl.createCredential(kongAdapter, orm.HMACAUTH, respDto.Id, &kongDto.KongCredentialDto{
		Key:    clientId,
		Secret: clientSecret,
	})
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) CreateConsumer(args *gw.DiceArgsDto, dto *gw.OpenConsumerDto) (id string, exists bool, err error) {
	auditCtx := map[string]interface{}{}
	auditCtx["consumer"] = dto.Name
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId: args.ProjectId,
			Workspace: args.Env,
		}, apistructs.CreateGatewayConsumerTemplate, err, auditCtx)
		if audit != nil {
			err = bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if err != nil {
				log.Errorf("create audit failed, err:%+v", err)
			}
		}
	}()
	if dto.Type == "" {
		dto.Type = gw.CT_PRO
	}
	if args.OrgId == "" || args.ProjectId == "" || args.Env == "" || dto.Name == "" {
		err = errors.New("args is empty")
		return
	}
	var kongConsumerName string
	var consumer *orm.GatewayConsumer
	var unique bool
	var kongInfo *orm.GatewayKongInfo
	var kongAdapter kong.KongAdapter
	var reqDto *kongDto.KongConsumerReqDto
	var respDto *kongDto.KongConsumerRespDto
	var customId string
	key, _ := util.GenUniqueId()
	secret, _ := util.GenUniqueId()
	az, err := impl.azDb.GetAz(&orm.GatewayAzInfo{
		Env:       args.Env,
		OrgId:     args.OrgId,
		ProjectId: args.ProjectId,
	})
	if err != nil {
		return
	}
	customId, err = util.GenUniqueId()
	if err != nil {
		return
	}
	consumer = &orm.GatewayConsumer{
		BaseRow:      orm.BaseRow{Id: customId},
		ConsumerName: dto.Name,
		OrgId:        args.OrgId,
		ProjectId:    args.ProjectId,
		Env:          args.Env,
		Az:           az,
		Description:  dto.Description,
	}
	unique, err = impl.consumerDb.CheckUnique(consumer)
	if err != nil {
		return
	}
	if !unique {
		exists = true
		err = errors.Errorf("consumer %s already exist", dto.Name)
		return
	}
	kongInfo, err = impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        az,
		ProjectId: args.ProjectId,
		Env:       args.Env,
	})
	if err != nil {
		return
	}
	kongAdapter = kong.NewKongAdapter(kongInfo.KongAddr)
	kongConsumerName = impl.GetKongConsumerName(consumer)
	reqDto = &kongDto.KongConsumerReqDto{
		Username: kongConsumerName,
		CustomId: customId,
	}
	respDto, err = kongAdapter.CreateConsumer(reqDto)
	if err != nil {
		return
	}
	consumer.ConsumerId = respDto.Id
	err = impl.consumerDb.Insert(consumer)
	if err != nil {
		return
	}
	err = kongAdapter.CreateAclGroup(respDto.Id, kongConsumerName)
	if err != nil {
		return
	}
	_, err = impl.createCredential(kongAdapter, orm.KEYAUTH, respDto.Id,
		&kongDto.KongCredentialDto{
			Key: key,
		})
	if err != nil {
		return
	}
	_, err = impl.createCredential(kongAdapter, orm.OAUTH2, respDto.Id,
		&kongDto.KongCredentialDto{
			Name:         "App",
			RedirectUrl:  []string{"http://none"},
			ClientId:     key,
			ClientSecret: secret,
		})
	if err != nil {
		return
	}
	_, err = impl.createCredential(kongAdapter, orm.SIGNAUTH, respDto.Id,
		&kongDto.KongCredentialDto{
			Key:    key,
			Secret: secret,
		})
	if err != nil {
		return
	}
	_, err = impl.createCredential(kongAdapter, orm.HMACAUTH, respDto.Id,
		&kongDto.KongCredentialDto{
			Key:    key,
			Secret: secret,
		})
	if err != nil {
		return
	}
	id = consumer.Id
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) openConsumerDto(dao *orm.GatewayConsumer) *gw.OpenConsumerInfoDto {
	return &gw.OpenConsumerInfoDto{
		Id:       dao.Id,
		CreateAt: dao.CreateTime.Format("2006-01-02T15:04:05"),
		OpenConsumerDto: gw.OpenConsumerDto{
			Name:        dao.ConsumerName,
			Description: dao.Description,
		},
	}
}

func (impl GatewayOpenapiConsumerServiceImpl) GetConsumers(args *gw.GetOpenConsumersDto) (res common.NewPageQuery, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if args.OrgId == "" || args.ProjectId == "" || args.Env == "" {
		err = errors.New("empty args")
		return
	}
	options := args.GenSelectOptions()
	pageInfo := common.NewPage2(args.PageSize, args.PageNo)
	var list []gw.OpenConsumerInfoDto
	var daos []orm.GatewayConsumer
	var ok bool
	var defaultName string
	var az string
	page, err := impl.consumerDb.GetPage(options, (*common.Page)(pageInfo))
	if err != nil {
		return
	}
	daos, ok = page.Result.([]orm.GatewayConsumer)
	if !ok {
		err = errors.New("type convert failed")
		return
	}
	az, err = impl.azDb.GetAz(&orm.GatewayAzInfo{
		Env:       args.Env,
		OrgId:     args.OrgId,
		ProjectId: args.ProjectId,
	})
	if err != nil {
		return
	}
	defaultName = impl.consumerDb.GetDefaultConsumerName(&orm.GatewayConsumer{
		OrgId:     args.OrgId,
		ProjectId: args.ProjectId,
		Env:       args.Env,
		Az:        az,
	})
	for _, dao := range daos {
		if strings.HasSuffix(strings.ToLower(dao.ConsumerName), strings.ToLower(defaultName)) {
			continue
		}
		list = append(list, *impl.openConsumerDto(&dao))
	}
	res = common.NewPages(list, pageInfo.TotalNum)
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) GetConsumersName(args *gw.GetOpenConsumersDto) (list []gw.OpenConsumerInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	list = []gw.OpenConsumerInfoDto{}
	if args.OrgId == "" || args.ProjectId == "" || args.Env == "" {
		err = errors.New("projectId or env is empty")
		return
	}
	var defaultName string
	var az string
	options := args.GenSelectOptions()
	daos, err := impl.consumerDb.SelectByOptions(options)
	if err != nil {
		return
	}
	az, err = impl.azDb.GetAz(&orm.GatewayAzInfo{
		Env:       args.Env,
		OrgId:     args.OrgId,
		ProjectId: args.ProjectId,
	})
	if err != nil {
		return
	}
	defaultName = impl.consumerDb.GetDefaultConsumerName(&orm.GatewayConsumer{
		OrgId:     args.OrgId,
		ProjectId: args.ProjectId,
		Env:       args.Env,
		Az:        az,
	})
	for _, dao := range daos {
		if dao.ConsumerName == defaultName {
			continue
		}
		list = append(list, *impl.openConsumerDto(&dao))
	}
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) UpdateConsumer(id string, dto *gw.OpenConsumerDto) (res *gw.OpenConsumerInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if id == "" {
		err = errors.New("consumerId is empty")
		return
	}
	dao, err := impl.consumerDb.GetById(id)
	if err != nil {
		return
	}
	if dao == nil {
		err = errors.New("consumer not exist")
		return
	}
	dao.Description = dto.Description
	err = impl.consumerDb.Update(dao)
	if err != nil {
		return
	}
	res = impl.openConsumerDto(dao)
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) DeleteConsumer(id string) (res bool, err error) {
	var consumer *orm.GatewayConsumer
	auditCtx := map[string]interface{}{}
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
		if consumer == nil {
			return
		}
		auditCtx["consumer"] = consumer.ConsumerName
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId: consumer.ProjectId,
			Workspace: consumer.Env,
		}, apistructs.DeleteGatewayConsumerTemplate, err, auditCtx)
		if audit != nil {
			err = bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if err != nil {
				log.Errorf("create audit failed, err:%+v", err)
			}
		}
	}()
	if id == "" {
		err = errors.New("consumer id is empty")
		return
	}
	var kongInfo *orm.GatewayKongInfo
	var kongAdapter kong.KongAdapter
	consumer, err = impl.consumerDb.GetById(id)
	if err != nil {
		return
	}
	if consumer == nil {
		res = true
		return
	}
	err = impl.packageInDb.DeleteByConsumerId(id)
	if err != nil {
		return
	}
	err = impl.packageApiInDb.DeleteByConsumerId(id)
	if err != nil {
		return
	}
	kongInfo, err = impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        consumer.Az,
		ProjectId: consumer.ProjectId,
		Env:       consumer.Env,
	})
	if err != nil {
		return
	}
	kongAdapter = kong.NewKongAdapter(kongInfo.KongAddr)
	err = kongAdapter.DeleteConsumer(consumer.ConsumerId)
	if err != nil {
		return
	}
	err = impl.consumerDb.DeleteById(id)
	if err != nil {
		return
	}
	res = true
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) GetConsumerCredentials(id string) (res gw.ConsumerCredentialsDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if id == "" {
		err = errors.New("id is empty")
		return
	}
	var kongInfo *orm.GatewayKongInfo
	var kongAdapter kong.KongAdapter
	var credentialListMap map[string]kongDto.KongCredentialListDto
	consumer, err := impl.consumerDb.GetById(id)
	if err != nil {
		return
	}
	if consumer == nil {
		err = errors.New("consumer not exist")
		return
	}
	kongInfo, err = impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        consumer.Az,
		ProjectId: consumer.ProjectId,
		Env:       consumer.Env,
	})
	if err != nil {
		return
	}
	kongAdapter = kong.NewKongAdapter(kongInfo.KongAddr)
	credentialListMap, err = impl.getCredentialList(kongAdapter, consumer.ConsumerId)
	if err != nil {
		return
	}
	res = gw.ConsumerCredentialsDto{
		ConsumerName: consumer.ConsumerName,
		ConsumerId:   id,
		AuthConfig: &orm.ConsumerAuthConfig{
			Auths: []orm.AuthItem{
				{
					AuthTips: orm.KeyAuthTips,
					AuthType: orm.KEYAUTH,
					AuthData: credentialListMap[orm.KEYAUTH],
				},
				{
					AuthTips: orm.SignAuthTips,
					AuthType: orm.SIGNAUTH,
					AuthData: credentialListMap[orm.SIGNAUTH],
				},
				{
					AuthType: orm.OAUTH2,
					AuthData: credentialListMap[orm.OAUTH2],
				},
				{
					AuthType: orm.HMACAUTH,
					AuthData: credentialListMap[orm.HMACAUTH],
				},
			},
		},
	}
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) UpdateConsumerCredentials(id string, dto *gw.ConsumerCredentialsDto) (res gw.ConsumerCredentialsDto, exists string, err error) {
	var consumer *orm.GatewayConsumer
	auditCtx := map[string]interface{}{}
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
		if consumer == nil {
			return
		}
		auditCtx["consumer"] = consumer.ConsumerName
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId: consumer.ProjectId,
			Workspace: consumer.Env,
		}, apistructs.UpdateGatewayConsumerTemplate, err, auditCtx)
		if audit != nil {
			err = bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if err != nil {
				log.Errorf("create audit failed, err:%+v", err)
			}
		}
	}()
	if id == "" {
		err = errors.New("id is empty")
		return
	}
	var kongInfo *orm.GatewayKongInfo
	var kongAdapter kong.KongAdapter
	var credentialListMap map[string]kongDto.KongCredentialListDto
	newAuth := dto.AuthConfig
	adds := map[string][]kongDto.KongCredentialDto{}
	dels := map[string][]kongDto.KongCredentialDto{}
	consumer, err = impl.consumerDb.GetById(id)
	if err != nil {
		return
	}
	if consumer == nil {
		err = errors.New("consumer not exist")
		return
	}
	kongInfo, err = impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        consumer.Az,
		ProjectId: consumer.ProjectId,
		Env:       consumer.Env,
	})
	if err != nil {
		return
	}
	kongAdapter = kong.NewKongAdapter(kongInfo.KongAddr)
	credentialListMap, err = impl.getCredentialList(kongAdapter, consumer.ConsumerId)
	if err != nil {
		return
	}
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
	for authType, credentials := range dels {
		for _, credential := range credentials {
			err = kongAdapter.DeleteCredential(consumer.ConsumerId, authType, credential.Id)
			if err != nil {
				return
			}
		}
	}
	for authType, credentials := range adds {
		var hmacEnabled bool
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
			if authType == orm.HMACAUTH {
				hmacEnabled, err = kongAdapter.CheckPluginEnabled(orm.HMACAUTH)
				if err != nil {
					return
				}
				if !hmacEnabled {
					log.Errorf("create hmac credential ignored, since not enabled")
					continue
				}
			}
			_, err = impl.createCredential(kongAdapter, authType, consumer.ConsumerId, &credential)
			if err != nil {
				log.Errorf("create credential failed, err:%+v", err)
				existsByte, _ := json.Marshal(credential)
				exists = string(existsByte)
				return
			}
		}
	}
	credentialListMap, err = impl.getCredentialList(kongAdapter, consumer.ConsumerId)
	if err != nil {
		return
	}
	res = gw.ConsumerCredentialsDto{
		ConsumerId:   id,
		ConsumerName: consumer.ConsumerName,
		AuthConfig: &orm.ConsumerAuthConfig{
			Auths: []orm.AuthItem{
				{
					AuthTips: orm.KeyAuthTips,
					AuthType: orm.KEYAUTH,
					AuthData: credentialListMap[orm.KEYAUTH],
				},
				{
					AuthTips: orm.SignAuthTips,
					AuthType: orm.SIGNAUTH,
					AuthData: credentialListMap[orm.SIGNAUTH],
				},
				{
					AuthType: orm.OAUTH2,
					AuthData: credentialListMap[orm.OAUTH2],
				},
				{
					AuthType: orm.HMACAUTH,
					AuthData: credentialListMap[orm.HMACAUTH],
				},
			},
		},
	}
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) GetPackageApiAcls(packageId string, packageApiId string) (result []gw.PackageAclInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	var packageRes []gw.PackageAclInfoDto
	var consumers []orm.GatewayConsumer
	var defaultName string
	var pack *orm.GatewayPackage
	selected := []gw.PackageAclInfoDto{}
	unselect := []gw.PackageAclInfoDto{}
	selectMap := map[string]bool{}
	var apiAclRules []gw.OpenapiRuleInfo
	if packageId == "" || packageApiId == "" {
		err = errors.New("id is empty")
		return
	}
	consumerIn, err := impl.packageApiInDb.SelectByPackageApi(packageId, packageApiId)
	if err != nil {
		return
	}
	apiAclRules, err = (*impl.ruleBiz).GetApiRules(packageApiId, gw.ACL_RULE)
	if err != nil {
		return
	}
	packageRes, err = impl.GetPackageAcls(packageId)
	if err != nil {
		return
	}
	if len(apiAclRules) == 0 {
		result = packageRes
		return
	}
	pack, err = impl.packageDb.Get(packageId)
	if err != nil {
		return
	}
	if pack == nil {
		err = errors.New("package not exist")
		return
	}
	consumers, err = impl.consumerDb.SelectByAny(&orm.GatewayConsumer{
		OrgId:     pack.DiceOrgId,
		ProjectId: pack.DiceProjectId,
		Env:       pack.DiceEnv,
		Az:        pack.DiceClusterName,
	})
	if err != nil {
		return
	}
	consumers = impl.adjustConsumers(pack, consumers)
	for _, in := range consumerIn {
		selectMap[in.ConsumerId] = true
	}
	defaultName = impl.consumerDb.GetDefaultConsumerName(&orm.GatewayConsumer{
		OrgId:     pack.DiceOrgId,
		ProjectId: pack.DiceProjectId,
		Env:       pack.DiceEnv,
		Az:        pack.DiceClusterName,
	})
	log.Debugf("default consumer name: %s", defaultName) // output for debug
	for _, consumer := range consumers {
		if strings.HasSuffix(strings.ToLower(consumer.ConsumerName), strings.ToLower(defaultName)) {
			continue
		}
		dto := gw.PackageAclInfoDto{
			OpenConsumerInfoDto: gw.OpenConsumerInfoDto{
				OpenConsumerDto: gw.OpenConsumerDto{
					Name:        consumer.ConsumerName,
					Description: consumer.Description,
				},
				Id:       consumer.Id,
				CreateAt: consumer.CreateTime.Format("2006-01-02T15:04:05"),
			},
		}
		if _, exist := selectMap[consumer.Id]; exist {
			dto.Selected = true
			selected = append(selected, dto)
		} else {
			dto.Selected = false
			unselect = append(unselect, dto)
		}
	}
	result = append(result, selected...)
	result = append(result, unselect...)
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) adjustConsumers(pack *orm.GatewayPackage, consumers []orm.GatewayConsumer) []orm.GatewayConsumer {
	if pack.AuthType != gw.AT_ALIYUN_APP {
		return consumers
	}
	for i := len(consumers) - 1; i >= 0; i-- {
		if consumers[i].CloudapiAppId == "" {
			consumers = append(consumers[:i], consumers[i+1:]...)
		}
	}
	return consumers

}

func (impl GatewayOpenapiConsumerServiceImpl) GetPackageAcls(id string) (result []gw.PackageAclInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if id == "" {
		err = errors.New("id is empty")
		return
	}
	var consumers []orm.GatewayConsumer
	var consumerIn []orm.GatewayPackageInConsumer
	var defaultName string
	selected := []gw.PackageAclInfoDto{}
	unselect := []gw.PackageAclInfoDto{}
	selectMap := map[string]bool{}
	pack, err := impl.packageDb.Get(id)
	if err != nil {
		return
	}
	if pack == nil {
		err = errors.New("package not exist")
		return
	}
	consumers, err = impl.consumerDb.SelectByAny(&orm.GatewayConsumer{
		OrgId:     pack.DiceOrgId,
		ProjectId: pack.DiceProjectId,
		Env:       pack.DiceEnv,
		Az:        pack.DiceClusterName,
	})
	if err != nil {
		return
	}
	consumers = impl.adjustConsumers(pack, consumers)
	consumerIn, err = impl.packageInDb.SelectByPackage(id)
	if err != nil {
		return
	}
	for _, in := range consumerIn {
		selectMap[in.ConsumerId] = true
	}
	defaultName = impl.consumerDb.GetDefaultConsumerName(&orm.GatewayConsumer{
		OrgId:     pack.DiceOrgId,
		ProjectId: pack.DiceProjectId,
		Env:       pack.DiceEnv,
		Az:        pack.DiceClusterName,
	})
	log.Debugf("default consumer name: %s", defaultName) // output for debug
	for _, consumer := range consumers {
		if strings.HasSuffix(strings.ToLower(consumer.ConsumerName), strings.ToLower(defaultName)) {
			continue
		}
		dto := gw.PackageAclInfoDto{
			OpenConsumerInfoDto: gw.OpenConsumerInfoDto{
				OpenConsumerDto: gw.OpenConsumerDto{
					Name:        consumer.ConsumerName,
					Description: consumer.Description,
				},
				Id:       consumer.Id,
				CreateAt: consumer.CreateTime.Format("2006-01-02T15:04:05"),
			},
		}
		if _, exist := selectMap[consumer.Id]; exist {
			dto.Selected = true
			selected = append(selected, dto)
		} else {
			dto.Selected = false
			unselect = append(unselect, dto)
		}
	}
	result = append(result, selected...)
	result = append(result, unselect...)
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) UpdatePackageApiAcls(packageId, packageApiId string, dto *gw.PackageAclsDto) (result bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if packageId == "" || packageApiId == "" {
		err = errors.New("id is empty")
		return
	}
	var consumerIn []orm.GatewayPackageApiInConsumer
	var api *orm.GatewayPackageApi
	var consumer *orm.GatewayConsumer
	selectMap := map[string]bool{}
	pack, err := impl.packageDb.Get(packageId)
	if err != nil {
		return
	}
	if pack == nil {
		err = errors.New("package not exist")
		return
	}
	api, err = impl.packageApiDb.Get(packageApiId)
	if err != nil {
		return
	}
	if api == nil {
		err = errors.New("package api not exist")
		return
	}
	consumerIn, err = impl.packageApiInDb.SelectByPackageApi(packageId, packageApiId)
	if err != nil {
		return
	}
	for _, in := range consumerIn {
		consumer, err = impl.consumerDb.GetById(in.ConsumerId)
		if err != nil {
			return
		}
		if consumer != nil && consumer.Type == orm.APIM_CLIENT_CONSUMER {
			continue
		}
		selectMap[in.ConsumerId] = false
	}
	for _, consumerId := range dto.Consumers {
		if _, selected := selectMap[consumerId]; selected {
			selectMap[consumerId] = true
			continue
		}
		err = impl.packageApiInDb.Insert(&orm.GatewayPackageApiInConsumer{
			ConsumerId:   consumerId,
			PackageId:    packageId,
			PackageApiId: packageApiId,
		})
		if err != nil {
			return
		}

	}
	for consumerId, selected := range selectMap {
		if !selected {
			err = impl.packageApiInDb.Delete(packageId, packageApiId, consumerId)
			if err != nil {
				return
			}
		}
	}
	err = impl.touchPackageApiAclRules(packageId, packageApiId)
	if err != nil {
		return
	}
	result = true
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) touchPackageApiAclRules(packageId, packageApiId string) error {
	pack, err := impl.packageDb.Get(packageId)
	if err != nil {
		return err
	}
	if pack == nil {
		return errors.New("package not found")
	}
	consumers, err := impl.GetConsumersOfPackageApi(packageId, packageApiId)
	if err != nil {
		return err
	}
	var buffer bytes.Buffer
	for _, consumer := range consumers {
		if buffer.Len() > 0 {
			buffer.WriteString(",")
		}
		buffer.WriteString(impl.GetKongConsumerName(&consumer))
	}
	wl := buffer.String()
	config := map[string]interface{}{}
	if wl == "" {
		wl = ","
	}
	config["whitelist"] = wl
	aclRules, err := (*impl.ruleBiz).GetApiRules(packageApiId, gw.ACL_RULE)
	if err != nil {
		return err
	}
	for _, rule := range aclRules {
		rule.Config = config
		_, err = (*impl.ruleBiz).UpdateRule(rule.Id, &rule.OpenapiRule)
		if err != nil {
			return err
		}
	}
	if len(aclRules) == 0 {
		newAclRule := &gw.OpenapiRule{
			PackageId:    packageId,
			PackageApiId: packageApiId,
			PluginName:   gw.ACL,
			Category:     gw.ACL_RULE,
			Config:       config,
			Enabled:      true,
			Region:       gw.API_RULE,
		}
		err = (*impl.ruleBiz).CreateRule(gw.DiceInfo{
			ProjectId: pack.DiceProjectId,
			Env:       pack.DiceEnv,
			Az:        pack.DiceClusterName,
		}, newAclRule, nil)
		if err != nil {
			return err
		}
		err = (*impl.ruleBiz).SetPackageKongPolicies(pack, nil)
		if err != nil {
			return err
		}
	}
	return nil
}

func (impl GatewayOpenapiConsumerServiceImpl) splitApis(apis []string, size int) [][]string {
	var res [][]string
	var curSlice []string
	for _, api := range apis {
		if len(curSlice) == size {
			res = append(res, curSlice)
			curSlice = []string{}
			continue
		}
		curSlice = append(curSlice, api)
	}
	return res
}

func (impl GatewayOpenapiConsumerServiceImpl) UpdatePackageAcls(id string, dto *gw.PackageAclsDto) (result bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if id == "" {
		err = errors.New("id is empty")
		return
	}
	var consumerIn []orm.GatewayPackageInConsumer
	var consumer *orm.GatewayConsumer
	selectMap := map[string]bool{}
	pack, err := impl.packageDb.Get(id)
	if err != nil {
		return
	}
	if pack == nil {
		err = errors.New("package not exist")
		return
	}
	consumerIn, err = impl.packageInDb.SelectByPackage(id)
	if err != nil {
		return
	}
	for _, in := range consumerIn {
		consumer, err = impl.consumerDb.GetById(in.ConsumerId)
		if err != nil {
			return
		}
		if consumer != nil && consumer.Type == orm.APIM_CLIENT_CONSUMER {
			continue
		}
		selectMap[in.ConsumerId] = false
	}
	for _, consumerId := range dto.Consumers {
		if _, selected := selectMap[consumerId]; selected {
			selectMap[consumerId] = true
			continue
		}
		err = impl.packageInDb.Insert(&orm.GatewayPackageInConsumer{
			ConsumerId: consumerId,
			PackageId:  id,
		})
		if err != nil {
			return
		}
	}
	for consumerId, selected := range selectMap {
		if !selected {
			err = impl.packageInDb.Delete(id, consumerId)
			if err != nil {
				return
			}
		}
	}
	err = impl.updatePackageAclRules(id)
	if err != nil {
		return
	}
	result = true
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) adjustPackages(consumer *orm.GatewayConsumer, packages []orm.GatewayPackage) []orm.GatewayPackage {
	if consumer.CloudapiAppId != "" {
		return packages
	}
	for i := len(packages) - 1; i >= 0; i-- {
		if packages[i].AuthType == gw.AT_ALIYUN_APP {
			packages = append(packages[:i], packages[i+1:]...)
		}
	}
	return packages
}

func (impl GatewayOpenapiConsumerServiceImpl) GetConsumerAcls(id string) (result []gw.ConsumerAclInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if id == "" {
		err = errors.New("id is empty")
		return
	}
	var packages []orm.GatewayPackage
	var packageIn []orm.GatewayPackageInConsumer
	result = []gw.ConsumerAclInfoDto{}
	selected := []gw.ConsumerAclInfoDto{}
	unselect := []gw.ConsumerAclInfoDto{}
	selectMap := map[string]bool{}
	consumer, err := impl.consumerDb.GetById(id)
	if err != nil {
		return
	}
	if consumer == nil {
		err = errors.New("consumer not exist")
		return
	}
	packages, err = impl.packageDb.SelectByAny(&orm.GatewayPackage{
		DiceProjectId:   consumer.ProjectId,
		DiceEnv:         consumer.Env,
		DiceClusterName: consumer.Az,
		Scene:           orm.OPENAPI_SCENE,
	})
	if err != nil {
		return
	}
	packages = impl.adjustPackages(consumer, packages)
	packageIn, err = impl.packageInDb.SelectByConsumer(id)
	if err != nil {
		return
	}
	for _, in := range packageIn {
		selectMap[in.PackageId] = true
	}
	for _, pack := range packages {
		dto := gw.ConsumerAclInfoDto{
			PackageInfoDto: gw.PackageInfoDto{
				PackageDto: gw.PackageDto{
					Name:        pack.PackageName,
					Description: pack.Description,
				},
				Id:       pack.Id,
				CreateAt: pack.CreateTime.Format("2006-01-02T15:04:05"),
			},
		}
		if _, exist := selectMap[pack.Id]; exist {
			dto.Selected = true
			selected = append(selected, dto)
		} else {
			dto.Selected = false
			unselect = append(unselect, dto)
		}
	}
	result = append(result, selected...)
	result = append(result, unselect...)
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) updatePackageAclRules(packageId string) error {
	consumers, err := impl.GetConsumersOfPackage(packageId)
	if err != nil {
		return err
	}
	var buffer bytes.Buffer
	for _, consumer := range consumers {
		if buffer.Len() > 0 {
			buffer.WriteString(",")
		}
		buffer.WriteString(impl.GetKongConsumerName(&consumer))
	}
	wl := buffer.String()
	config := map[string]interface{}{}
	if wl == "" {
		wl = ","
	}
	config["whitelist"] = wl
	aclRules, err := (*impl.ruleBiz).GetPackageRules(packageId, nil, gw.ACL_RULE)
	if err != nil {
		return err
	}
	for _, rule := range aclRules {
		rule.Config = config
		_, err = (*impl.ruleBiz).UpdateRule(rule.Id, &rule.OpenapiRule)
		if err != nil {
			return err
		}
	}
	return nil
}

func (impl GatewayOpenapiConsumerServiceImpl) GrantPackageToConsumer(consumerId, packageId string) error {
	pack, err := impl.packageDb.Get(packageId)
	if err != nil {
		return err
	}
	if pack == nil {
		return errors.New("package not found")
	}
	packageIn, err := impl.packageInDb.SelectByConsumer(consumerId)
	if err != nil {
		return err
	}
	for _, in := range packageIn {
		if in.PackageId == packageId {
			// granted
			return nil
		}
	}
	err = impl.packageInDb.Insert(&orm.GatewayPackageInConsumer{
		ConsumerId: consumerId,
		PackageId:  packageId,
	})
	if err != nil {
		return err
	}
	err = impl.updatePackageAclRules(packageId)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiConsumerServiceImpl) RevokePackageFromConsumer(consumerId, packageId string) error {
	pack, err := impl.packageDb.Get(packageId)
	if err != nil {
		return err
	}
	if pack == nil {
		return errors.New("package not found")
	}
	packageIn, err := impl.packageInDb.SelectByConsumer(consumerId)
	if err != nil {
		return err
	}
	granted := false
	for _, in := range packageIn {
		if in.PackageId == packageId {
			granted = true
			break
		}
	}
	if !granted {
		return nil
	}
	err = impl.packageInDb.Delete(packageId, consumerId)
	if err != nil {
		return err
	}
	err = impl.updatePackageAclRules(packageId)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiConsumerServiceImpl) UpdateConsumerAcls(id string, dto *gw.ConsumerAclsDto) (res bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if id == "" {
		err = errors.New("id is empty")
		return
	}
	var packageIn []orm.GatewayPackageInConsumer
	selectMap := map[string]bool{}
	consumer, err := impl.consumerDb.GetById(id)
	if err != nil {
		return
	}
	if consumer == nil {
		err = errors.New("consumer not exist")
		return
	}
	packageIn, err = impl.packageInDb.SelectByConsumer(id)
	if err != nil {
		return
	}
	for _, in := range packageIn {
		selectMap[in.PackageId] = false
	}
	for _, packageId := range dto.Packages {
		var pack *orm.GatewayPackage
		pack, err = impl.packageDb.Get(packageId)
		if err != nil {
			return
		}
		if pack == nil {
			continue
		}
		if _, selected := selectMap[packageId]; selected {
			selectMap[packageId] = true
			continue
		}
		err = impl.packageInDb.Insert(&orm.GatewayPackageInConsumer{
			ConsumerId: consumer.Id,
			PackageId:  packageId,
		})
		if err != nil {
			return
		}
		err = impl.updatePackageAclRules(packageId)
		if err != nil {
			return
		}
	}
	for packageId, selected := range selectMap {
		if !selected {
			var pack *orm.GatewayPackage
			pack, err = impl.packageDb.Get(packageId)
			if err != nil {
				return
			}
			if pack == nil {
				continue
			}
			err = impl.packageInDb.Delete(packageId, consumer.Id)
			if err != nil {
				return
			}
			err = impl.updatePackageAclRules(packageId)
			if err != nil {
				return
			}
		}
	}
	res = true
	return
}

func (impl GatewayOpenapiConsumerServiceImpl) GetConsumersOfPackage(packageId string) ([]orm.GatewayConsumer, error) {
	consumerIds, err := impl.packageInDb.SelectByPackage(packageId)
	if err != nil {
		return nil, err
	}
	var consumers []orm.GatewayConsumer
	for _, consumerId := range consumerIds {
		consumer, err := impl.consumerDb.GetById(consumerId.ConsumerId)
		if err != nil {
			return nil, err
		}
		if consumer == nil {
			continue
		}
		consumers = append(consumers, *consumer)
	}
	return consumers, nil
}

func (impl GatewayOpenapiConsumerServiceImpl) GetConsumersOfPackageApi(packageId, packageApiId string) ([]orm.GatewayConsumer, error) {
	consumerIds, err := impl.packageApiInDb.SelectByPackageApi(packageId, packageApiId)
	if err != nil {
		return nil, err
	}
	var consumers []orm.GatewayConsumer
	for _, consumerId := range consumerIds {
		consumer, err := impl.consumerDb.GetById(consumerId.ConsumerId)
		if err != nil {
			return nil, err
		}
		if consumer == nil {
			continue
		}
		consumers = append(consumers, *consumer)
	}
	packageConsumers, err := impl.GetConsumersOfPackage(packageId)
	if err != nil {
		return nil, err
	}
	for _, consumer := range packageConsumers {
		if consumer.Type == orm.APIM_CLIENT_CONSUMER {
			consumers = append(consumers, consumer)
		}
	}
	return consumers, nil
}
