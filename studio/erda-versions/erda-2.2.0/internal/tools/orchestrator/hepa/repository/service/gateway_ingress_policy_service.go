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
	"encoding/json"
	"fmt"
	"strings"

	"github.com/pkg/errors"
	"github.com/xormplus/xorm"

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type GatewayIngressPolicyServiceImpl struct {
	engine *orm.OrmEngine
	*SessionHelper
	executor xorm.Interface
}

func NewGatewayIngressPolicyServiceImpl() (*GatewayIngressPolicyServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new GatewayIngressPolicyServiceImpl failed")
	}
	return &GatewayIngressPolicyServiceImpl{
		engine:   engine,
		executor: engine,
	}, nil
}

func (impl *GatewayIngressPolicyServiceImpl) NewSession(helper ...*SessionHelper) (GatewayIngressPolicyService, error) {
	var session *SessionHelper
	var err error
	if len(helper) == 0 {
		session, err = NewSessionHelper()
		if err != nil {
			return nil, err
		}
	} else if helper[0] == nil {
		return &GatewayIngressPolicyServiceImpl{
			engine:   impl.engine,
			executor: impl.engine,
		}, nil
	} else {
		session = helper[0]
	}
	return &GatewayIngressPolicyServiceImpl{
		engine:        impl.engine,
		executor:      session.session,
		SessionHelper: session,
	}, nil
}

func (impl *GatewayIngressPolicyServiceImpl) CreateOrUpdate(dao *orm.GatewayIngressPolicy) error {
	if dao == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	policy := &orm.GatewayIngressPolicy{}
	exist, err := orm.Get(impl.executor, policy, "az = ? and zone_id = ? and name = ?", dao.Az, dao.ZoneId, dao.Name)
	if err != nil {
		return errors.WithStack(err)
	}
	if !exist {
		_, err = orm.Insert(impl.executor, dao)
		if err != nil {
			return errors.WithStack(err)
		}
		return nil
	}
	dao.Id = policy.Id
	_, err = orm.Update(impl.executor, dao)
	if err != nil {
		return errors.WithStack(err)
	}
	return nil
}

func (impl *GatewayIngressPolicyServiceImpl) Insert(ingressPolicy *orm.GatewayIngressPolicy) error {
	if ingressPolicy == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Insert(impl.executor, ingressPolicy)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayIngressPolicyServiceImpl) GetByAny(cond *orm.GatewayIngressPolicy) (*orm.GatewayIngressPolicy, error) {
	if cond == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	ingressPolicy := &orm.GatewayIngressPolicy{}
	bCond, err := orm.BuildConds(impl.engine, cond, cond.GetMustCondCols())
	if err != nil {
		return nil, errors.Wrap(err, "buildConds failed")
	}
	succ, err := orm.GetByAnyI(impl.executor, bCond, ingressPolicy)
	if err != nil {
		return nil, errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return nil, nil
	}
	return ingressPolicy, nil
}

func (impl *GatewayIngressPolicyServiceImpl) SelectByAny(cond *orm.GatewayIngressPolicy) ([]orm.GatewayIngressPolicy, error) {
	var result []orm.GatewayIngressPolicy
	if cond == nil {
		return result, errors.New(ERR_INVALID_ARG)
	}
	bCond, err := orm.BuildConds(impl.engine, cond, cond.GetMustCondCols())
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	err = orm.SelectByAnyI(impl.executor, bCond, &result)
	if err != nil {
		return result, errors.Wrap(err, ERR_SQL_FAIL)
	}
	return result, nil
}

func (impl *GatewayIngressPolicyServiceImpl) Update(policy *orm.GatewayIngressPolicy) error {
	if policy == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	_, err := orm.Update(impl.executor, policy)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayIngressPolicyServiceImpl) UpdatePartial(policy *orm.GatewayIngressPolicy, fields ...string) error {
	if policy == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	dao := &orm.GatewayIngressPolicy{}
	exist, err := orm.Get(impl.executor, dao, "az = ? and zone_id = ? and name = ?", policy.Az, policy.ZoneId, policy.Name)
	if err != nil {
		return errors.WithStack(err)
	}
	if !exist {
		return errors.Errorf("policy not exists, cond:%+v", policy)
	}
	policy.Id = dao.Id
	_, err = orm.Update(impl.executor, policy, fields...)
	if err != nil {
		return errors.Wrap(err, ERR_SQL_FAIL)
	}
	return nil
}

func (impl *GatewayIngressPolicyServiceImpl) GetChangesByRegions(az, regions string, zoneId ...string) (*IngressChanges, error) {
	return impl.GetChangesByRegionsImpl(az, regions, zoneId...)
}

func (impl *GatewayIngressPolicyServiceImpl) GetChangesByRegionsImpl(az, regions string, zoneId ...string) (*IngressChanges, error) {
	if az == "" {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	regionArr := strings.Split(regions, "|")
	var globalMatchRegions []string
	var zoneMatchRegions []string
	for _, region := range regionArr {
		for _, match := range GLOBAL_REGIONS {
			if region == match {
				globalMatchRegions = append(globalMatchRegions, match)
			}
		}
		for _, match := range ZONE_REGIONS {
			if region == match {
				zoneMatchRegions = append(zoneMatchRegions, match)
			}
		}
	}
	res := &IngressChanges{}
	if len(zoneMatchRegions) != 0 {
		if len(zoneId) == 0 {
			return nil, errors.New("zoneId is empty")
		}
		var cond []string
		for _, region := range zoneMatchRegions {
			cond = append(cond, fmt.Sprintf("regions like '%%%s%%'", region))
		}
		if strings.Contains(regions, "location") {
			res.LocationSnippets = &[]string{}
		}
		annoNeedUpdate := false
		if strings.Contains(regions, "annotation") {
			annoNeedUpdate = true
		}
		zonePolicies := []orm.GatewayIngressPolicy{}
		err := orm.Select(impl.executor.Where("zone_id = ?", zoneId[0]), &zonePolicies, strings.Join(cond, " or "))
		if err != nil {
			return nil, errors.WithStack(err)
		}
		for _, policy := range zonePolicies {
			if len(policy.LocationSnippet) > 0 && res.LocationSnippets != nil {
				*res.LocationSnippets = append(*res.LocationSnippets, string(policy.LocationSnippet))
			}
			if len(policy.Annotations) > 0 && annoNeedUpdate {
				annotation := map[string]*string{}
				err = json.Unmarshal(policy.Annotations, &annotation)
				if err != nil {
					return nil, errors.Errorf("json unmarshal annotation failed, annotation:%+v",
						annotation)
				}
				res.Annotations = append(res.Annotations, annotation)
			}
		}
	}
	if len(globalMatchRegions) != 0 {
		var cond []string
		for _, region := range globalMatchRegions {
			cond = append(cond, fmt.Sprintf("regions like '%%%s%%'", region))
		}
		globalPolicies := []orm.GatewayIngressPolicy{}
		err := orm.Select(impl.executor.Where("az = ?", az), &globalPolicies, strings.Join(cond, " or "))
		if err != nil {
			return nil, errors.WithStack(err)
		}
		if strings.Contains(regions, "main") {
			res.MainSnippets = &[]string{}
		}
		if strings.Contains(regions, "http") {
			res.HttpSnippets = &[]string{}
		}
		if strings.Contains(regions, "server") {
			res.ServerSnippets = &[]string{}
		}
		optionNeedUpdate := false
		if strings.Contains(regions, "option") {
			optionNeedUpdate = true
		}
		for _, policy := range globalPolicies {
			if len(policy.ConfigmapOption) > 0 && optionNeedUpdate {
				option := map[string]*string{}
				err = json.Unmarshal(policy.ConfigmapOption, &option)
				if err != nil {
					return nil, errors.Errorf("json unmarshal option failed, option:%+v",
						option)
				}
				res.ConfigmapOptions = append(res.ConfigmapOptions, option)
			}
			if len(policy.MainSnippet) > 0 && res.MainSnippets != nil {
				*res.MainSnippets = append(*res.MainSnippets, string(policy.MainSnippet))
			}
			if len(policy.HttpSnippet) > 0 && res.HttpSnippets != nil {
				*res.HttpSnippets = append(*res.HttpSnippets, string(policy.HttpSnippet))
			}
			if len(policy.ServerSnippet) > 0 && res.ServerSnippets != nil {
				*res.ServerSnippets = append(*res.ServerSnippets, string(policy.ServerSnippet))
			}
		}
	}
	return res, nil
}
