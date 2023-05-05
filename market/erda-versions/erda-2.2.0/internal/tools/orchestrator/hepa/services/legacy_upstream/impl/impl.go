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
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	log "github.com/sirupsen/logrus"
	"github.com/xormplus/xorm"

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/legacy_consumer"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/legacy_upstream"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/micro_api"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/zone"
)

type GatewayUpstreamServiceImpl struct {
	upstreamDb       db.GatewayUpstreamService
	upstreamApiDb    db.GatewayUpstreamApiService
	upstreamRecordDb db.GatewayUpstreamRegisterRecordService
	azDb             db.GatewayAzInfoService
	consumerDb       db.GatewayConsumerService
	consumerBiz      *legacy_consumer.GatewayConsumerService
	apiBiz           *micro_api.GatewayApiService
	zoneBiz          *zone.GatewayZoneService
	kongDb           db.GatewayKongInfoService
	runtimeDb        db.GatewayRuntimeServiceService
	engine           *orm.OrmEngine
	reqCtx           context.Context
}

var once sync.Once

func NewGatewayUpstreamServiceImpl() error {
	once.Do(
		func() {
			upstreamDb, _ := db.NewGatewayUpstreamServiceImpl()
			upstreamApiDb, _ := db.NewGatewayUpstreamApiServiceImpl()
			upstreamRecordDb, _ := db.NewGatewayUpstreamRegisterRecordServiceImpl()
			azDb, _ := db.NewGatewayAzInfoServiceImpl()
			kongDb, _ := db.NewGatewayKongInfoServiceImpl()
			consumerDb, _ := db.NewGatewayConsumerServiceImpl()
			runtimeDb, _ := db.NewGatewayRuntimeServiceServiceImpl()
			engine, _ := orm.GetSingleton()

			legacy_upstream.Service = &GatewayUpstreamServiceImpl{
				upstreamDb:       upstreamDb,
				upstreamApiDb:    upstreamApiDb,
				upstreamRecordDb: upstreamRecordDb,
				azDb:             azDb,
				kongDb:           kongDb,
				consumerDb:       consumerDb,
				consumerBiz:      &legacy_consumer.Service,
				apiBiz:           &micro_api.Service,
				zoneBiz:          &zone.Service,
				engine:           engine,
				runtimeDb:        runtimeDb,
			}
		})
	return nil
}

func (impl GatewayUpstreamServiceImpl) Clone(ctx context.Context) legacy_upstream.GatewayUpstreamService {
	newService := impl
	newService.reqCtx = ctx
	return &newService
}

func (impl GatewayUpstreamServiceImpl) getUpstreamMissingApis(upstreamId string, registerId string) (map[string]orm.GatewayUpstreamApi, error) {
	res := map[string]orm.GatewayUpstreamApi{}
	record, err := impl.upstreamRecordDb.Get(upstreamId, registerId)
	if err != nil {
		return res, err
	}
	if record == nil {
		return res, errors.Errorf("record of upstreamId[%s] and registerId[%s] not exists", upstreamId, registerId)
	}
	if len(record.UpstreamApis) == 0 {
		return res, nil
	}
	apiIdList := []string{}
	err = json.Unmarshal(record.UpstreamApis, &apiIdList)
	if err != nil {
		return res, errors.Wrapf(err, "json unmarshal [%s] failed", record.UpstreamApis)
	}
	apiList, err := impl.upstreamApiDb.SelectInIdsAndDeleted(apiIdList)
	if err != nil {
		return res, err
	}
	for _, api := range apiList {
		// 兼容逻辑
		if api.ApiName == "root" {
			res["/"] = api
		} else {
			res[api.ApiName] = api
		}
	}
	return res, nil
}

func (impl GatewayUpstreamServiceImpl) getUpstreamApis(upstreamId string, registerId string) (map[string]orm.GatewayUpstreamApi, error) {
	res := map[string]orm.GatewayUpstreamApi{}
	record, err := impl.upstreamRecordDb.Get(upstreamId, registerId)
	if err != nil {
		return res, err
	}
	if record == nil {
		return res, errors.Errorf("record of upstreamId[%s] and registerId[%s] not exists", upstreamId, registerId)
	}
	if len(record.UpstreamApis) == 0 {
		return res, nil
	}
	apiIdList := []string{}
	err = json.Unmarshal(record.UpstreamApis, &apiIdList)
	if err != nil {
		return res, errors.Wrapf(err, "json unmarshal [%s] failed", record.UpstreamApis)
	}
	apiList, err := impl.upstreamApiDb.SelectInIds(apiIdList)
	if err != nil {
		return res, err
	}
	for _, api := range apiList {
		// 兼容逻辑
		if api.ApiName == "root" {
			res["/"] = api
		} else {
			res[api.ApiName] = api
		}
	}
	return res, nil
}

func safeChange(apis []orm.GatewayUpstreamApi, change func(orm.GatewayUpstreamApi), errorHappened *bool) {
	if *errorHappened {
		return
	}
	size := len(apis)
	batchCount := size / config.ServerConf.RegisterSliceSize
	leaves := size % config.ServerConf.RegisterSliceSize
	log.Debugf("size:%d batchCont:%d leaves:%d", size, batchCount, leaves)
	for i := 0; i < batchCount; i++ {
		for j := 0; j < config.ServerConf.RegisterSliceSize; j++ {
			change(apis[i*config.ServerConf.RegisterSliceSize+j])
			if *errorHappened {
				return
			}
		}
		time.Sleep(time.Duration(config.ServerConf.RegisterInterval) * time.Second)
	}
	for i := size - leaves; i < size; i++ {
		change(apis[i])
		if *errorHappened {
			return
		}
	}
}

func (impl GatewayUpstreamServiceImpl) UpstreamValidAsync(context map[string]interface{}, dto *gw.UpstreamRegisterDto) (err error) {
	consumerI, ok := context["register_consumer"]
	if !ok {
		err = errors.New("can't find consumer from context")
		return
	}
	consumer, ok := consumerI.(*orm.GatewayConsumer)
	if !ok {
		err = errors.New("acquire consumer failed")
		return
	}
	daoI, ok := context["register_dao"]
	if !ok {
		err = errors.New("can't find dao from context")
		return
	}
	dao, ok := daoI.(*orm.GatewayUpstream)
	if !ok {
		err = errors.New("acquire dao failed")
		return
	}
	return impl.UpstreamValid(consumer, dao, dto)

}

func (impl GatewayUpstreamServiceImpl) UpstreamValid(consumer *orm.GatewayConsumer, dao *orm.GatewayUpstream, dto *gw.UpstreamRegisterDto) (err error) {
	prefix := ""
	if dto.PathPrefix != nil {
		prefix = *dto.PathPrefix
	}
	err = impl.upstreamValid(consumer, dao, dto.RegisterId, prefix)
	if err != nil {
		return
	}
	return
}

func (impl GatewayUpstreamServiceImpl) apiNeedUpdate(old, new orm.GatewayUpstreamApi) bool {
	return old.Path != new.Path || old.Method != new.Method || old.Address != new.Address || old.GatewayPath != new.GatewayPath
}

func (impl GatewayUpstreamServiceImpl) upstreamValid(consumer *orm.GatewayConsumer, upstream *orm.GatewayUpstream, validId string, aliasPath string) error {
	if consumer == nil || upstream == nil || len(upstream.Id) == 0 || validId == "" {
		log.Debugf("consumer:%+v upstream:%+v validId:%s", consumer, upstream, validId) // output for debug
		return errors.New(ERR_INVALID_ARG)
	}
	// upstream db transcation start: get old validId
	session := impl.engine.NewSession()
	defer func() {
		session.Close()
	}()
	err := session.Begin()
	if err != nil {
		return err
	}
	oldValidId, err := impl.upstreamDb.GetValidIdForUpdate(upstream.Id, session)
	if err != nil {
		return err
	}
	err = impl.upstreamDb.UpdateValidId(&orm.GatewayUpstream{
		BaseRow:         orm.BaseRow{Id: upstream.Id},
		ValidRegisterId: validId,
	}, session)
	if err != nil {
		_ = session.Rollback()
		return err
	}
	// extract diff: updates, adds, dels
	var updates, adds, dels []orm.GatewayUpstreamApi
	oldApis := map[string]orm.GatewayUpstreamApi{}
	newApis := map[string]orm.GatewayUpstreamApi{}
	backupApis := map[string]orm.GatewayUpstreamApi{}
	if oldValidId != "" && oldValidId != validId {
		oldApis, err = impl.getUpstreamApis(upstream.Id, oldValidId)
		if err != nil {
			return err
		}
	}
	if oldValidId != validId {
		newApis, err = impl.getUpstreamApis(upstream.Id, validId)
		if err != nil {
			return err
		}
	} else {
		newApis, err = impl.getUpstreamMissingApis(upstream.Id, validId)
		if err != nil {
			return err
		}
		if len(newApis) == 0 {
			log.Infof("no need to update api, since valid id is latest, and no api missing")
			return nil
		}
		defer func() {
			if err == nil {
				for _, api := range newApis {
					err = impl.upstreamApiDb.Recover(api.Id)
					if err != nil {
						log.Errorf("api recover failed, id:%s", api.Id)
					} else {
						log.Infof("api recover success, id:%s", api.Id)
					}
				}
			}
		}()
	}
	for name, upApi := range newApis {
		var oldUpApi orm.GatewayUpstreamApi
		exist := false
		name = strings.TrimSuffix(name, "/")
		if oldUpApi, exist = oldApis[name]; !exist {
			name += "/"
			oldUpApi, exist = oldApis[name]
		}
		if exist {
			upApi.ApiId = oldUpApi.ApiId
			// 兼容老的问题数据
			// 补偿机制：获取最近一次有apiid的记录
			if upApi.ApiId == "" {
				upApi.ApiId = impl.upstreamApiDb.GetLastApiId(&upApi)
			}
			// 二次补偿机制：如果找不到apiid，尝试创建
			if upApi.ApiId == "" {
				adds = append(adds, upApi)
				continue
			}
			if impl.apiNeedUpdate(oldUpApi, upApi) {
				updates = append(updates, upApi)
				backupApis[name] = oldUpApi
			} else {
				err = impl.upstreamApiDb.UpdateApiId(&upApi)
				if err != nil {
					log.Errorf("upstream api update failed!: %+v", upApi)
				}
			}
			delete(oldApis, name)
			continue
		}
		adds = append(adds, upApi)
	}
	for _, upApi := range oldApis {
		if upApi.ApiId != "" {
			dels = append(dels, upApi)
		}
	}
	log.Debugf(`upstream valid debug, upstreamId:%s, upstreamName:%s, oldValidId:%s, newValidId:%s, adds:%+v, updates:%+v, dels:%+v`,
		upstream.Id, upstream.UpstreamName, oldValidId, validId, adds, updates, dels)
	log.Infof("upstream valid info, upstreamId:%s, upstreamName:%s, validId:%s, adds:%d, updates:%d, dels:%d",
		upstream.Id, upstream.UpstreamName, validId, len(adds), len(updates), len(dels))
	var added, deled, updated []orm.GatewayUpstreamApi
	errHappened := false
	// for range adds create api, update upstreamApi db
	addFunc := func(upApi orm.GatewayUpstreamApi) {
		if errHappened {
			return
		}
		apiId, err := (*impl.apiBiz).CreateUpstreamBindApi(consumer, upstream.DiceApp, upstream.DiceService, upstream.RuntimeServiceId, &upApi, aliasPath)
		if err == kong.ErrInvalidReq {
			log.Errorf("invalid api ignored, upApi:%+v", upApi)
			return
		}
		if err != nil {
			log.Errorf("create upstream api failed: %+v, upApi:%+v, exist apiId:%s", err, upApi, apiId)
			errHappened = true
			return
		}
		upApi.ApiId = apiId
		added = append(added, upApi)
		err = impl.upstreamApiDb.UpdateApiId(&upApi)
		if err != nil {
			log.Errorf("upstream api update failed!: %+v", upApi)
			errHappened = true
		}
	}
	safeChange(adds, addFunc, &errHappened)
	// for range updates update api, use kong put method will create if not exist
	updateFunc := func(upApi orm.GatewayUpstreamApi) {
		if errHappened {
			return
		}
		err = (*impl.apiBiz).UpdateUpstreamBindApi(consumer, upstream.DiceApp, upstream.DiceService, &upApi, aliasPath)
		if err == kong.ErrInvalidReq {
			log.Errorf("invalid api ignored, upApi:%+v", upApi)
			return
		}
		if err != nil {
			log.Errorf("update upstream api failed: %+v, upApi:%+v", err, upApi)
			errHappened = true
			return
		}
		updated = append(updated, upApi)
		err = impl.upstreamApiDb.UpdateApiId(&upApi)
		if err != nil {
			log.Errorf("upstream api update failed!: %+v", upApi)
			errHappened = true
		}
	}
	safeChange(updates, updateFunc, &errHappened)
	// for range dels delete api
	delFunc := func(upApi orm.GatewayUpstreamApi) {
		if errHappened {
			return
		}
		err = (*impl.apiBiz).DeleteUpstreamBindApi(&upApi)
		if err == kong.ErrInvalidReq {
			log.Errorf("invalid api ignored, upApi:%+v", upApi)
			return
		}
		if err != nil {
			log.Errorf("delete upstream api failed: %+v, upApi:%+v", err, upApi)
			errHappened = true
			return
		}
		deled = append(deled, upApi)
	}
	safeChange(dels, delFunc, &errHappened)
	if errHappened {
		// recover:
		for i := len(deled) - 1; i >= 0; i-- {
			upApi := deled[i]
			apiId, err := (*impl.apiBiz).CreateUpstreamBindApi(consumer, upstream.DiceApp, upstream.DiceService, upstream.RuntimeServiceId, &upApi, aliasPath)
			if err != nil {
				log.Errorf("recover: create upstream api[%+v] failed: %+v",
					upApi, err)
				continue
			}
			upApi.ApiId = apiId
			err = impl.upstreamApiDb.UpdateApiId(&upApi)
			if err != nil {
				log.Errorf("recover: upstream api[%+v] update failed!: %+v",
					upApi, err)
			}
			deled = append(deled[:i], deled[i+1:]...)
		}
		for i := len(updated) - 1; i >= 0; i-- {
			upApi := updated[i]
			if backupApi, ok := backupApis[upApi.ApiName]; ok {
				err = (*impl.apiBiz).UpdateUpstreamBindApi(consumer, upstream.DiceApp, upstream.DiceService, &backupApi, aliasPath)
				if err != nil {
					log.Errorf("recover: update upstream api[%+v] failed: %+v", backupApi, err)
					continue
				}
				err = impl.upstreamApiDb.UpdateApiId(&backupApi)
				if err != nil {
					log.Errorf("recover: upstream api[%+v] update failed!: %+v", backupApi, err)
				}
			} else {
				log.Errorf("recover: can't find backup old api [%+v]", upApi)
			}
			updated = append(updated[:i], updated[i+1:]...)
		}
		for i := len(added) - 1; i >= 0; i-- {
			upApi := added[i]
			err = (*impl.apiBiz).DeleteUpstreamBindApi(&upApi)
			if err != nil {
				log.Errorf("recover: delete upstream api[%+v] failed: %+v",
					upApi, err)
			}
			added = append(added[:i], added[i+1:]...)
		}
		_ = session.Rollback()
	} else {
		_ = session.Commit()
	}
	log.Infof("upstream valid done, errorHappened:%t upstreamId:%s, upstreamName:%s, validId:%s, added:%d, updated:%d, deled:%d",
		errHappened, upstream.Id, upstream.UpstreamName, validId, len(added), len(updated), len(deled))
	return err
}

func (impl GatewayUpstreamServiceImpl) saveUpstreamApi(session *xorm.Session, dto *gw.UpstreamApiDto, registerId string, upId string) (string, error) {
	if session == nil || dto == nil {
		log.Errorf("invalid session:%+v or dto:%+v", session, dto)
		return "", errors.New(ERR_INVALID_ARG)
	}
	docJson, err := json.Marshal(dto.Doc)
	if err != nil {
		return "", errors.Wrapf(err, "json Marshal [%+v] failed", dto.Doc)
	}
	dao := &orm.GatewayUpstreamApi{
		UpstreamId:  upId,
		RegisterId:  registerId,
		ApiName:     dto.Name,
		Path:        dto.Path,
		GatewayPath: dto.GatewayPath,
		Method:      dto.Method,
		Address:     dto.Address,
		Doc:         docJson,
	}
	if dto.IsInner {
		dao.IsInner = 1
	}
	return impl.upstreamApiDb.Insert(session, dao)
}

func (impl GatewayUpstreamServiceImpl) saveUpstream(dao *orm.GatewayUpstream, dto *gw.UpstreamRegisterDto) (bool, error) {
	apiDtos := dto.ApiList
	registerId := dto.RegisterId
	transSucc := false
	if dao == nil {
		return transSucc, errors.New(ERR_INVALID_ARG)
	}
	session := impl.engine.NewSession()
	defer func(session **xorm.Session) {
		if transSucc {
			_ = (*session).Commit()
		} else {
			_ = (*session).Rollback()
		}
		(*session).Close()
	}(&session)
	err := session.Begin()
	if err != nil {
		return transSucc, errors.WithStack(err)
	}
	_, err = session.Exec("set innodb_lock_wait_timeout=600")
	if err != nil {
		return transSucc, errors.WithStack(err)
	}
	needSave, newCreated, upId, err := impl.upstreamDb.UpdateRegister(session, dao)
	if err != nil {
		return transSucc, err
	}
	if newCreated {
		// unlock table lock
		_ = session.Commit()
		session.Close()
		session = impl.engine.NewSession()
		err := session.Begin()
		if err != nil {
			return transSucc, errors.WithStack(err)
		}
		// get row lock
		_, _, _, err = impl.upstreamDb.UpdateRegister(session, dao)
		if err != nil {
			return transSucc, err
		}
	}
	if !needSave {
		return true, nil
	}
	upApiList := []string{}
	for _, apiDto := range apiDtos {
		upApiId, err := impl.saveUpstreamApi(session, &apiDto, registerId, upId)
		if err != nil {
			return transSucc, err
		}
		upApiList = append(upApiList, upApiId)
	}
	apisJson, err := json.Marshal(upApiList)
	if err != nil {
		return transSucc, errors.Wrapf(err, "json marshal [%+v] failed", upApiList)
	}
	err = impl.upstreamRecordDb.Insert(session, &orm.GatewayUpstreamRegisterRecord{
		UpstreamId:   upId,
		RegisterId:   registerId,
		UpstreamApis: apisJson,
	})
	if err != nil {
		return transSucc, err
	}
	transSucc = true
	return transSucc, nil
}

func (impl GatewayUpstreamServiceImpl) upstreamRegister(dto *gw.UpstreamRegisterDto) (*orm.GatewayUpstream, *orm.GatewayConsumer, error) {
	var err error
	var az string
	if dto == nil {
		err = errors.Errorf("invalid dto:%+v", dto)
		return nil, nil, err
	}
	if len(dto.Az) == 0 {
		az, err = impl.azDb.GetAz(&orm.GatewayAzInfo{
			Env:       dto.Env,
			OrgId:     dto.OrgId,
			ProjectId: dto.ProjectId,
		})
		if err != nil {
			return nil, nil, err
		}
	} else {
		az = dto.Az
	}
	// upstream db transaction: get register, return if no need update
	dao := &orm.GatewayUpstream{
		OrgId:          dto.OrgId,
		ProjectId:      dto.ProjectId,
		Env:            dto.Env,
		Az:             az,
		DiceApp:        dto.AppName,
		DiceService:    dto.DiceService,
		UpstreamName:   dto.UpstreamName,
		LastRegisterId: dto.RegisterId,
	}
	// acquire default consumer
	consumer, err := impl.consumerDb.GetDefaultConsumer(&orm.GatewayConsumer{
		OrgId:     dto.OrgId,
		ProjectId: dto.ProjectId,
		Env:       dto.Env,
		Az:        az,
	})

	if err != nil {
		return nil, nil, err
	}
	if consumer == nil {
		consumer, _, _, err = (*impl.consumerBiz).CreateDefaultConsumer(dto.OrgId,
			dto.ProjectId, dto.Env, az)
		if err != nil {
			return nil, nil, err
		}
	}
	if dto.RuntimeName != "" {
		session := impl.engine.NewSession()
		_, _ = session.Exec("set innodb_lock_wait_timeout=600")
		err = session.Begin()
		if err != nil {
			session.Close()
			return nil, nil, err
		}
		runtimeService, err := impl.runtimeDb.CreateIfNotExist(session, &orm.GatewayRuntimeService{
			RuntimeName: dto.RuntimeName,
			AppId:       dto.DiceAppId,
			ServiceName: dto.DiceService,
			ProjectId:   dto.ProjectId,
			Workspace:   dto.Env,
			ClusterName: dto.Az,
		})
		if err != nil {
			_ = session.Rollback()
			session.Close()
			return nil, nil, err
		}
		_ = session.Commit()
		session.Close()
		dao.RuntimeServiceId = runtimeService.Id
	}
	_, err = impl.saveUpstream(dao, dto)
	if err != nil {
		return nil, nil, err
	}
	return dao, consumer, nil
}

func (impl GatewayUpstreamServiceImpl) UpstreamRegisterAsync(dto *gw.UpstreamRegisterDto) (result bool, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error Happened: %+v", err)
		}
	}()
	dao, consumer, err := impl.upstreamRegister(dto)
	if dao == nil || consumer == nil {
		err = errors.New("invalid params")
		return
	}
	if dao.AutoBind == 0 {
		log.Infof("dto[%+v] registerd without bind", dto)
		result = true
		return
	}
	context := map[string]interface{}{}
	context["register_consumer"] = consumer
	context["register_dao"] = dao
	go impl.UpstreamValidAsync(context, dto)
	result = true
	return
}

func (impl GatewayUpstreamServiceImpl) UpstreamRegister(dto *gw.UpstreamRegisterDto) (result bool, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error Happened: %+v", err)
		}
	}()
	dao, consumer, err := impl.upstreamRegister(dto)
	if dao == nil || consumer == nil {
		err = errors.New("invalid params")
		return
	}
	// check autoBind, return if false
	if dao.AutoBind == 0 {
		log.Infof("dto[%+v] registerd without bind", dto)
		result = true
		return
	}
	err = impl.UpstreamValid(consumer, dao, dto)
	if err != nil {
		return
	}
	result = true
	return
}
