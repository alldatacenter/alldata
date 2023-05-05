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
	"sync"
	"time"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	kongDto "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/legacy_upstream_lb"
)

type GatewayUpstreamLbServiceImpl struct {
	kongDb             db.GatewayKongInfoService
	upstreamLbDb       db.GatewayUpstreamLbService
	upstreamLbTargetDb db.GatewayUpstreamLbTargetService
	engine             *orm.OrmEngine
	reqCtx             context.Context
}

var once sync.Once

func NewGatewayUpstreamLbServiceImpl() error {
	once.Do(
		func() {
			kongDb, _ := db.NewGatewayKongInfoServiceImpl()
			upstreamLbDb, _ := db.NewGatewayUpstreamLbServiceImpl()
			upstreamLbTargetDb, _ := db.NewGatewayUpstreamLbTargetServiceImpl()
			engine, _ := orm.GetSingleton()
			legacy_upstream_lb.Service = &GatewayUpstreamLbServiceImpl{
				kongDb:             kongDb,
				upstreamLbDb:       upstreamLbDb,
				upstreamLbTargetDb: upstreamLbTargetDb,
				engine:             engine,
			}
		})
	return nil
}

func (impl GatewayUpstreamLbServiceImpl) Clone(ctx context.Context) legacy_upstream_lb.GatewayUpstreamLbService {
	newService := impl
	newService.reqCtx = ctx
	return newService
}

func (impl GatewayUpstreamLbServiceImpl) touchUpstreamLb(kongAdapter kong.KongAdapter, lb *orm.GatewayUpstreamLb) (*orm.GatewayUpstreamLb, string, string, error) {
	if lb == nil {
		return nil, "", "", errors.New(ERR_INVALID_ARG)
	}
	cond := &orm.GatewayUpstreamLb{
		OrgId:     lb.OrgId,
		ProjectId: lb.ProjectId,
		Env:       lb.Env,
		Az:        lb.Az,
		LbName:    lb.LbName,
	}
	existLb, err := impl.upstreamLbDb.Get(cond)
	if err != nil {
		return nil, "", "", err
	}
	if existLb != nil {
		err = impl.upstreamLbDb.UpdateDeploymentId(existLb.Id, lb.LastDeploymentId)
		if err != nil {
			return nil, "", "", err
		}
		return existLb, existLb.Id, existLb.KongUpstreamId, nil
	}
	transSucc := false
	session := impl.engine.NewSession()
	defer func() {
		if transSucc {
			_ = session.Commit()
		} else {
			_ = session.Rollback()
		}
		session.Close()
	}()
	err = session.Begin()
	if err != nil {
		return nil, "", "", errors.WithStack(err)
	}
	log.Debug("befor GetForUpdate")
	existLb, _ = impl.upstreamLbDb.GetForUpdate(session, cond)
	if existLb != nil {
		transSucc = true
		_ = session.Commit()
		err = impl.upstreamLbDb.UpdateDeploymentId(existLb.Id, lb.LastDeploymentId)
		if err != nil {
			return nil, "", "", err
		}
		log.Debug("find after GetForUpdate")
		return existLb, existLb.Id, existLb.KongUpstreamId, nil
	}
	log.Debug("not find after GetForUpdate")
	kongResp, err := kongAdapter.CreateUpstream(&kongDto.KongUpstreamDto{
		Name:         lb.LbName,
		Healthchecks: kongDto.NewHealthchecks(lb.HealthcheckPath),
	})
	if err != nil {
		return nil, "", "", err
	}
	if kongResp.Id == "" {
		return nil, "", "", errors.Errorf("invalid kongResp:%+v", *kongResp)
	}
	lb.KongUpstreamId = kongResp.Id
	upstreamCfgJson, err := json.Marshal(kongResp)
	if err != nil {
		return nil, "", "", errors.Wrapf(err, "marshal kongresp failed, resp:%+v",
			*kongResp)
	}
	lb.Config = upstreamCfgJson
	err = impl.upstreamLbDb.Insert(session, lb)
	if err != nil {
		return nil, "", "", err
	}
	transSucc = true
	return nil, lb.Id, lb.KongUpstreamId, nil
}

func (impl GatewayUpstreamLbServiceImpl) deleteTarget(kongAdapter kong.KongAdapter, kongUpstreamId string, targetDao orm.GatewayUpstreamLbTarget, force bool) error {
	// err := kongAdapter.DeleteUpstreamTarget(kongUpstreamId, targetDao.KongTargetId)
	// safe check
	if !force {
		upstreamLb, err := impl.upstreamLbDb.GetByKongId(kongUpstreamId)
		if err != nil {
			return err
		}
		if upstreamLb == nil {
			return errors.New("upstream not exist")
		}
		useTargets, err := impl.upstreamLbTargetDb.SelectByDeploymentId(upstreamLb.LastDeploymentId)
		if err != nil {
			return err
		}

		for _, target := range useTargets {
			if targetDao.Target == target.Target {
				log.Warnf("target[%s] used by last deployment", targetDao.Target)
				return nil
			}
		}
	}
	err := kongAdapter.DeleteUpstreamTarget(kongUpstreamId, targetDao.Target)

	if err != nil {
		log.Errorf("delete target from kong failed, targetDao:%+v, err:%+v",
			targetDao, err)
		return err
	}
	err = impl.upstreamLbTargetDb.Delete(targetDao.Id)
	if err != nil {
		log.Errorf("delete target from db failed, targetDao:%+v, err:%+v",
			targetDao, err)
		return err
	}
	return nil
}

func (impl GatewayUpstreamLbServiceImpl) clearStaleOnNewDeploy(kongAdapter kong.KongAdapter, lbId string, deploymentId int, freshTime int64, count int) error {
	upstreamLb, err := impl.upstreamLbDb.GetById(lbId)
	if err != nil {
		return err
	}
	if upstreamLb == nil {
		return errors.Errorf("get upstreamLb failed, lbId:%s", lbId)
	}
	if upstreamLb.LastDeploymentId != deploymentId {
		log.Infof("new deployment come, stop old clear job, newId:%d oldId:%d",
			upstreamLb.LastDeploymentId, deploymentId)
		return nil
	}
	resp, err := kongAdapter.GetUpstreamStatus(upstreamLb.KongUpstreamId)
	if err != nil {
		log.Errorf("clearStaleOnNewDeploy failed, err:%+v", err)
		return err
	}
	var readyToDels []orm.GatewayUpstreamLbTarget
	var freshAllHealthy *bool
	for _, targetDto := range resp.Data {
		if targetDto.CreatedAt > freshTime {
			log.Infof("target is newer than fresh, targetDto:%+v freshTime:%d",
				targetDto, freshTime)
			continue
		}
		targetDaos, err := impl.upstreamLbTargetDb.Select(lbId, targetDto.Target)
		if err != nil || len(targetDaos) == 0 {
			log.Errorf("can't find from db, targetDto:%+v, lbId:%s, err:%+v",
				targetDto, lbId, err)
			continue
		}
		for _, targetDao := range targetDaos {
			if targetDao.DeploymentId == deploymentId {
				if targetDto.Health == "UNHEALTHY" {
					if freshAllHealthy != nil {
						*freshAllHealthy = false
					}
				} else if freshAllHealthy == nil {
					status := true
					freshAllHealthy = &status
				}
			} else if targetDto.Health == "UNHEALTHY" {
				err = impl.deleteTarget(kongAdapter, upstreamLb.KongUpstreamId, targetDao, false)
				if err != nil {
					log.Errorf("delete target failed, err:%+v", err)
					continue
				}
				log.Infof("delete unhealthy stale target with old deployment id, lbName:%s, target:%s, old deploy:%d, new deploy:%d",
					upstreamLb.LbName, targetDao.Target, targetDao.DeploymentId, deploymentId)
			} else {
				readyToDels = append(readyToDels, targetDao)
			}
		}
	}
	if count <= 0 {
		finalCount := count*config.ServerConf.StaleTargetCheckInterval <= -config.ServerConf.StaleTargetKeepTime
		if freshAllHealthy != nil && (*freshAllHealthy || finalCount) {
			for i := len(readyToDels) - 1; i >= 0; i-- {
				targetDao := readyToDels[i]
				err = impl.deleteTarget(kongAdapter, upstreamLb.KongUpstreamId, targetDao, false)
				if err != nil {
					log.Errorf("delete target failed, err:%+v", err)
					continue
				}
				log.Infof("delete stale target ignore healthy status, lbName:%s, targetDao:%+v",
					upstreamLb.LbName, targetDao)
				readyToDels = append(readyToDels[:i], readyToDels[i+1:]...)
			}
		}
		if len(readyToDels) == 0 || finalCount {
			return nil
		}
	}
	time.AfterFunc(time.Duration(config.ServerConf.StaleTargetCheckInterval)*time.Second,
		func() {
			count--
			_ = impl.clearStaleOnNewDeploy(kongAdapter, lbId, deploymentId, freshTime, count)
		})
	return nil
}

func (impl GatewayUpstreamLbServiceImpl) clearUnhealthyOnUnexpectDeploy(kongAdapter kong.KongAdapter, lbId string, freshTime int64) error {
	upstreamLb, err := impl.upstreamLbDb.GetById(lbId)
	if err != nil {
		return err
	}
	if upstreamLb == nil {
		return errors.Errorf("get upstreamLb failed, lbId:%s", lbId)
	}

	resp, err := kongAdapter.GetUpstreamStatus(upstreamLb.KongUpstreamId)
	if err != nil {
		log.Errorf("clearStaleOnNewDeploy failed, err:%+v", err)
		return err
	}
	for _, targetDto := range resp.Data {
		if targetDto.CreatedAt > freshTime {
			log.Infof("target is newer than fresh, targetDto:%+v freshTime:%d",
				targetDto, freshTime)
			continue
		}
		targetDaos, err := impl.upstreamLbTargetDb.Select(lbId, targetDto.Target)
		if err != nil || len(targetDaos) == 0 {
			log.Errorf("can't find from db, targetDto:%+v, lbId:%s, err:%+v",
				targetDto, lbId, err)
			continue
		}
		for _, targetDao := range targetDaos {
			if targetDto.Health == "UNHEALTHY" {
				err = impl.deleteTarget(kongAdapter, upstreamLb.KongUpstreamId, targetDao, false)
				if err != nil {
					log.Errorf("delete target failed, err:%+v", err)
					continue
				}
				log.Infof("delete unhealthy target on unexpect deploy, lbName:%s, targetDao:%+v",
					upstreamLb.LbName, targetDao)
			}
		}
	}
	return nil
}

func (impl GatewayUpstreamLbServiceImpl) UpstreamTargetOnline(dto *gw.UpstreamLbDto) (result bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if dto == nil {
		errors.New("dto is nil")
		return
	}
	lbName := dto.LbName
	upstreamLb := orm.GatewayUpstreamLb{
		OrgId:            dto.OrgId,
		ProjectId:        dto.ProjectId,
		LbName:           lbName,
		Env:              dto.Env,
		Az:               dto.Az,
		LastDeploymentId: dto.DeploymentId,
		HealthcheckPath:  dto.HealthcheckPath,
	}
	kongAdapter := kong.NewKongAdapterForProject(dto.Az, dto.Env, dto.ProjectId)
	oldLb, lbId, kongUpstreamId, err := impl.touchUpstreamLb(kongAdapter, &upstreamLb)
	if err != nil {
		return
	}
	// 等待kong的状态同步,避免target添加时upstream找不到
	if oldLb == nil {
		time.Sleep(time.Duration(5) * time.Second)
	}
	var freshTime int64
	for _, target := range dto.Targets {
		kongTargetReq := &kongDto.KongTargetDto{
			Target: target,
		}
		var kongTargetResp *kongDto.KongTargetDto
		kongTargetResp, err = kongAdapter.AddUpstreamTarget(kongUpstreamId, kongTargetReq)
		if err != nil {
			return
		}
		freshTime = kongTargetResp.CreatedAt
		if freshTime == 0 {
			err = errors.Errorf("invalid kongTargetResp, resp:%+v", kongTargetResp)
			return
		}
		err = impl.upstreamLbTargetDb.Insert(&orm.GatewayUpstreamLbTarget{
			LbId:         lbId,
			DeploymentId: dto.DeploymentId,
			KongTargetId: kongTargetResp.Id,
			Target:       target,
		})
		if err != nil {
			return
		}
		log.Infof("add target success, lbName:%s, target:%s", lbName, target)
	}
	if oldLb != nil && config.ServerConf.TargetActiveOffline {
		// 执行target主动退场的判定机制
		if dto.DeploymentId != oldLb.LastDeploymentId {
			time.AfterFunc(time.Duration(config.ServerConf.StaleTargetCheckInterval)*time.Second,
				func() {
					_ = impl.clearStaleOnNewDeploy(kongAdapter, lbId, dto.DeploymentId, freshTime,
						config.ServerConf.StaleTargetKeepTime/config.ServerConf.StaleTargetCheckInterval)
				})
		} else if time.Since(oldLb.UpdateTime).Seconds() > float64(config.ServerConf.UnexpectDeployInterval) {
			log.Infof("unexpect deploy, now:%s, old time:%s, wait duration:%d seconds", time.Now().String(), oldLb.UpdateTime.String(), config.ServerConf.UnexpectDeployInterval)
			err = impl.clearUnhealthyOnUnexpectDeploy(kongAdapter, lbId, freshTime)
			if err != nil {
				log.Errorf("clearUnhealthyOnUnexpectDeploy failed, err:%+v", err)
			}
		}
	}
	result = true
	return
}

func (impl GatewayUpstreamLbServiceImpl) UpstreamTargetOffline(dto *gw.UpstreamLbDto) (result bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if dto == nil {
		err = errors.New("dto is nil")
		return
	}
	lbName := dto.LbName
	cond := &orm.GatewayUpstreamLb{
		OrgId:     dto.OrgId,
		ProjectId: dto.ProjectId,
		Env:       dto.Env,
		Az:        dto.Az,
		LbName:    lbName,
	}
	kongAdapter := kong.NewKongAdapterForProject(dto.Az, dto.Env, dto.ProjectId)
	existLb, err := impl.upstreamLbDb.Get(cond)
	if err != nil || existLb == nil {
		err = errors.Errorf("can't find upstreamLb, cond:%+v, err:%+v", cond, err)
		return
	}
	errorHappened := false
	for _, target := range dto.Targets {
		targetDaos, err := impl.upstreamLbTargetDb.Select(existLb.Id, target)
		if err != nil || len(targetDaos) == 0 {
			log.Errorf("get target failed, existLb:%+v, target:%+s, err:%+v", *existLb, target, err)
			errorHappened = true
			continue
		}
		for _, targetDao := range targetDaos {
			err = impl.deleteTarget(kongAdapter, existLb.KongUpstreamId, targetDao, true)
			if err != nil {
				log.Errorf("delete target failed, targetDao:%+v, err:%+v", targetDao, err)
				errorHappened = true
				continue
			}
			log.Infof("delete target on offline request, lbName:%s, target:%s", lbName, target)
		}
	}
	if !errorHappened {
		result = true
	}
	return
}
