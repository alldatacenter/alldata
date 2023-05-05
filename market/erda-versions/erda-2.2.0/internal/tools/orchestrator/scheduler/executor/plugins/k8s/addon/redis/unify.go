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

package redis

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/addon"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/addon/redis/legacy"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

type UnifiedRedisOperator struct {
	redisoperator       *RedisOperator
	legacyRedisoperator *legacy.RedisOperator
	useLegacy           bool
}

func New(k8sutil addon.K8SUtil,
	deploy addon.DeploymentUtil,
	sts addon.StatefulsetUtil,
	service addon.ServiceUtil,
	ns addon.NamespaceUtil,
	overcommit addon.OvercommitUtil,
	secret addon.SecretUtil,
	client *httpclient.HTTPClient) *UnifiedRedisOperator {
	return &UnifiedRedisOperator{
		redisoperator:       NewRedisOperator(k8sutil, deploy, sts, service, ns, overcommit, secret, client),
		legacyRedisoperator: legacy.New(k8sutil, deploy, sts, service, ns, overcommit, client),
		useLegacy:           false,
	}
}

func (ro *UnifiedRedisOperator) IsSupported() bool {
	if ro.redisoperator.IsSupported() {
		ro.useLegacy = false
		return true
	}
	if ro.legacyRedisoperator.IsSupported() {
		ro.useLegacy = true
		return true
	}
	return false
}

func (ro *UnifiedRedisOperator) Validate(sg *apistructs.ServiceGroup) error {
	if ro.useLegacy {
		return ro.legacyRedisoperator.Validate(sg)
	}
	return ro.redisoperator.Validate(sg)
}
func (ro *UnifiedRedisOperator) Convert(sg *apistructs.ServiceGroup) interface{} {
	if ro.useLegacy {
		return ro.legacyRedisoperator.Convert(sg)
	}
	return ro.redisoperator.Convert(sg)
}
func (ro *UnifiedRedisOperator) Create(k8syml interface{}) error {
	if ro.useLegacy {
		return ro.legacyRedisoperator.Create(k8syml)
	}
	return ro.redisoperator.Create(k8syml)
}

func (ro *UnifiedRedisOperator) Inspect(sg *apistructs.ServiceGroup) (*apistructs.ServiceGroup, error) {
	if ro.useLegacy {
		return ro.legacyRedisoperator.Inspect(sg)
	}
	return ro.redisoperator.Inspect(sg)
}
func (ro *UnifiedRedisOperator) Remove(sg *apistructs.ServiceGroup) error {
	if ro.useLegacy {
		return ro.legacyRedisoperator.Remove(sg)
	}
	if err := ro.redisoperator.Remove(sg); err != nil {
		return ro.legacyRedisoperator.Remove(sg)
	}
	return nil
}

func (ro *UnifiedRedisOperator) Update(k8syml interface{}) error {
	if ro.useLegacy {
		return ro.legacyRedisoperator.Update(k8syml)
	}
	return ro.redisoperator.Update(k8syml)
}
