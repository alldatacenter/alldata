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

package service

import (
	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type GatewayUpstreamLbTargetServiceImpl struct {
	engine *orm.OrmEngine
}

func NewGatewayUpstreamLbTargetServiceImpl() (*GatewayUpstreamLbTargetServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayUpstreamLbTargetServiceImpl failed")
	}
	return &GatewayUpstreamLbTargetServiceImpl{engine}, nil
}

func (impl GatewayUpstreamLbTargetServiceImpl) Insert(dao *orm.GatewayUpstreamLbTarget) error {
	if dao == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.engine, dao)
	if err != nil {
		return errors.WithStack(err)
	}
	return nil
}

func (impl GatewayUpstreamLbTargetServiceImpl) Select(lbId, target string) ([]orm.GatewayUpstreamLbTarget, error) {
	if lbId == "" || target == "" {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	var result []orm.GatewayUpstreamLbTarget
	err := orm.Select(impl.engine.Desc("create_time"), &result, "lb_id = ? and target = ?", lbId, target)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	return result, nil
}

func (impl GatewayUpstreamLbTargetServiceImpl) Delete(id string) error {
	if id == "" {
		return errors.New(ERR_INVALID_ARG)
	}
	affected, err := orm.Delete(impl.engine, &orm.GatewayUpstreamLbTarget{}, "id = ?", id)
	if err != nil {
		return errors.WithStack(err)
	}
	if affected < 1 {
		log.Debugf("%+v", errors.New("maybe already deleted"))
	}
	return nil
}

func (impl GatewayUpstreamLbTargetServiceImpl) SelectByDeploymentId(id int) ([]orm.GatewayUpstreamLbTarget, error) {
	var result []orm.GatewayUpstreamLbTarget
	err := orm.Select(impl.engine, &result, "deployment_id = ?", id)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	return result, nil
}
