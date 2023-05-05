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

package legacy

import (
	"bytes"
	"fmt"

	"github.com/sirupsen/logrus"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/addon"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders"
	"github.com/erda-project/erda/pkg/strutil"
)

type RedisOperator struct {
	k8s         addon.K8SUtil
	deployment  addon.DeploymentUtil
	statefulset addon.StatefulsetUtil
	ns          addon.NamespaceUtil
	service     addon.ServiceUtil
	overcommit  addon.OvercommitUtil
	client      *httpclient.HTTPClient
}

func New(k8sutil addon.K8SUtil,
	deploy addon.DeploymentUtil,
	sts addon.StatefulsetUtil,
	service addon.ServiceUtil,
	ns addon.NamespaceUtil,
	overcommit addon.OvercommitUtil,
	client *httpclient.HTTPClient) *RedisOperator {
	return &RedisOperator{
		k8s:         k8sutil,
		deployment:  deploy,
		statefulset: sts,
		service:     service,
		ns:          ns,
		overcommit:  overcommit,
		client:      client,
	}
}

func (ro *RedisOperator) IsSupported() bool {
	resp, err := ro.client.Get(ro.k8s.GetK8SAddr()).
		Path("/apis/storage.spotahome.com/v1alpha2").
		Do().
		DiscardBody()
	if err != nil {
		logrus.Errorf("failed to query /apis/storage.spotahome.com/v1alpha2, host: %v, err: %v",
			ro.k8s.GetK8SAddr(), err)
		return false
	}
	if !resp.IsOK() {
		return false
	}
	return true
}

// Validate
func (ro *RedisOperator) Validate(sg *apistructs.ServiceGroup) error {
	operator, ok := sg.Labels["USE_OPERATOR"]
	if !ok {
		return fmt.Errorf("[BUG] sg need USE_OPERATOR label")
	}
	if strutil.ToLower(operator) != svcNameRedis {
		return fmt.Errorf("[BUG] value of label USE_OPERATOR should be 'redis'")
	}
	if len(sg.Services) != 2 {
		return fmt.Errorf("illegal services num: %d", len(sg.Services))
	}
	if sg.Services[0].Name != svcNameRedis && sg.Services[0].Name != svcNameSentinel {
		return fmt.Errorf("illegal service: %+v, should be one of [redis, sentinel]", sg.Services[0])
	}
	if sg.Services[1].Name != svcNameRedis && sg.Services[1].Name != svcNameSentinel {
		return fmt.Errorf("illegal service: %+v, should be one of [redis, sentinel]", sg.Services[1])
	}
	var redis apistructs.Service
	if sg.Services[0].Name == svcNameRedis {
		redis = sg.Services[0]
	}
	// if sg.Services[0].Name == svcNameSentinel {
	// 	sentinel = sg.Services[0]
	// }
	if sg.Services[1].Name == svcNameRedis {
		redis = sg.Services[1]
	}
	// if sg.Services[1].Name == svcNameSentinel {
	// 	sentinel = sg.Services[1]
	// }
	if _, ok := redis.Env["requirepass"]; !ok {
		return fmt.Errorf("redis service not provide 'requirepass' env")
	}
	return nil
}

func (ro *RedisOperator) Convert(sg *apistructs.ServiceGroup) interface{} {
	svc0 := sg.Services[0]
	svc1 := sg.Services[1]
	var redis RedisSettings
	var sentinel SentinelSettings
	switch svc0.Name {
	case svcNameRedis:
		redis = ro.convertRedis(svc0)
	case svcNameSentinel:
		sentinel = convertSentinel(svc0)
	}
	switch svc1.Name {
	case svcNameRedis:
		redis = ro.convertRedis(svc1)
	case svcNameSentinel:
		sentinel = convertSentinel(svc1)
	}
	scheinfo := sg.ScheduleInfo2
	scheinfo.Stateful = true
	affinity := constraintbuilders.K8S(&scheinfo, nil, nil, nil).Affinity.NodeAffinity

	labels := make(map[string]string)
	annotations := make(map[string]string)
	addon.SetAddonLabelsAndAnnotations(svc0, labels, annotations)
	addon.SetAddonLabelsAndAnnotations(svc1, labels, annotations)

	return RedisFailover{
		TypeMeta: metav1.TypeMeta{
			APIVersion: "storage.spotahome.com/v1alpha2",
			Kind:       "RedisFailover",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:        sg.ID,
			Namespace:   genK8SNamespace(sg.Type, sg.ID),
			Labels:      labels,
			Annotations: annotations,
		},
		Spec: RedisFailoverSpec{
			Redis:        redis,
			Sentinel:     sentinel,
			NodeAffinity: affinity,
		},
	}
}

func (ro *RedisOperator) Create(k8syml interface{}) error {
	redis, ok := k8syml.(RedisFailover)
	if !ok {
		return fmt.Errorf("[BUG] this k8syml should be RedisFailover")
	}
	if err := ro.ns.Exists(redis.Namespace); err != nil {
		if err := ro.ns.Create(redis.Namespace, nil); err != nil {
			return err
		}
	}

	var b bytes.Buffer
	resp, err := ro.client.Post(ro.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("/apis/storage.spotahome.com/v1alpha2/namespaces/%s/redisfailovers", redis.Namespace)).
		JSONBody(redis).
		Do().
		Body(&b)
	if err != nil {
		return fmt.Errorf("failed to create redisfailover, %s/%s, err: %v", redis.Namespace, redis.Name, err)
	}
	if !resp.IsOK() {
		return fmt.Errorf("failed to create redisfailover, %s/%s, statuscode: %v, body: %v",
			redis.Namespace, redis.Name, resp.StatusCode(), b.String())
	}
	return nil
}

func (ro *RedisOperator) Inspect(sg *apistructs.ServiceGroup) (*apistructs.ServiceGroup, error) {
	deploylist, err := ro.deployment.List(genK8SNamespace(sg.Type, sg.ID), nil)
	if err != nil {
		return nil, err
	}
	stslist, err := ro.statefulset.List(genK8SNamespace(sg.Type, sg.ID))
	if err != nil {
		return nil, err
	}
	svclist, err := ro.service.List(genK8SNamespace(sg.Type, sg.ID), nil)
	if err != nil {
		return nil, err
	}
	var redis, sentinel *apistructs.Service
	if sg.Services[0].Name == svcNameRedis {
		redis = &(sg.Services[0])
	}
	if sg.Services[1].Name == svcNameRedis {
		redis = &(sg.Services[1])
	}
	if sg.Services[0].Name == svcNameSentinel {
		sentinel = &(sg.Services[0])
	}
	if sg.Services[1].Name == svcNameSentinel {
		sentinel = &(sg.Services[1])
	}
	for _, deploy := range deploylist.Items {
		for _, cond := range deploy.Status.Conditions {
			if cond.Type == appsv1.DeploymentAvailable {
				if cond.Status == corev1.ConditionTrue {
					sentinel.Status = apistructs.StatusHealthy
				} else {
					sentinel.Status = apistructs.StatusUnHealthy
				}
			}
		}
	}
	for _, sts := range stslist.Items {
		if sts.Spec.Replicas == nil {
			redis.Status = apistructs.StatusUnknown
		} else if *sts.Spec.Replicas == sts.Status.ReadyReplicas {
			redis.Status = apistructs.StatusHealthy
		} else {
			redis.Status = apistructs.StatusUnHealthy
		}
	}

	for _, svc := range svclist.Items {
		sentinel.Vip = strutil.Join([]string{svc.Name, svc.Namespace, "svc.cluster.local"}, ".")
	}
	if redis.Status == apistructs.StatusHealthy && sentinel.Status == apistructs.StatusHealthy {
		sg.Status = apistructs.StatusHealthy
	} else {
		sg.Status = apistructs.StatusUnHealthy
	}
	return sg, nil
}

func (ro *RedisOperator) Remove(sg *apistructs.ServiceGroup) error {
	k8snamespace := genK8SNamespace(sg.Type, sg.ID)
	var b bytes.Buffer
	resp, err := ro.client.Delete(ro.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("/apis/storage.spotahome.com/v1alpha2/namespaces/%s/redisfailovers/%s", k8snamespace, sg.ID)).
		JSONBody(k8sapi.DeleteOptions).
		Do().
		Body(&b)
	if err != nil {
		return fmt.Errorf("failed to delele redisfailover: %s/%s, err: %v", sg.Type, sg.ID, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil
		}
		return fmt.Errorf("failed to delete redisfailover: %s/%s, statuscode: %v, body: %v",
			sg.Type, sg.ID, resp.StatusCode(), b.String())
	}

	if err := ro.ns.Delete(k8snamespace); err != nil {
		logrus.Errorf("failed to delete namespace: %s: %v", k8snamespace, err)
		return nil
	}
	return nil
}

// Update 支持镜像、环境变量、资源、副本数(不能为0) 等更新
// 副本数限制参考: https://github.com/spotahome/redis-operator/blob/0.5.8/api/redisfailover/v1alpha2/validate.go
func (ro *RedisOperator) Update(k8syml interface{}) error {
	// TODO:
	redis, ok := k8syml.(RedisFailover)
	if !ok {
		return fmt.Errorf("[BUG] 01 this k8syml should be RedisFailover")
	}

	// 更新  RedisFailover (副本数不能为 0，否则将)
	var oldRedis RedisFailover
	resp, err := ro.client.Get(ro.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("/apis/storage.spotahome.com/v1alpha2/namespaces/%s/redisfailovers/%s", redis.Namespace, redis.Name)).
		Do().JSON(&oldRedis)
	if err != nil {
		return fmt.Errorf("failed to update redisfailover, %s/%s, get redisfailover failed, err: %v, body: %v ", redis.Namespace, redis.Name, err, string(resp.Body()))
	}
	if !resp.IsOK() {
		return fmt.Errorf("failed to update redisfailover, %s/%s, get redisfailover is not OK, err: %v, body: %v ", redis.Namespace, redis.Name, err, string(resp.Body()))
	}

	// update redis
	if redis.Spec.Redis.Replicas >= 0 {
		oldRedis.Spec.Redis.Replicas = redis.Spec.Redis.Replicas
	}
	oldRedis.Spec.Redis.Resources = redis.Spec.Redis.Resources
	oldRedis.Spec.Redis.Image = redis.Spec.Redis.Image
	oldRedis.Spec.Redis.CustomConfig = redis.Spec.Redis.CustomConfig
	oldRedis.Spec.Redis.Command = redis.Spec.Redis.Command
	oldRedis.Spec.Redis.ShutdownConfigMap = redis.Spec.Redis.ShutdownConfigMap
	oldRedis.Spec.Redis.Exporter = redis.Spec.Redis.Exporter
	oldRedis.Spec.Redis.Envs = redis.Spec.Redis.Envs
	oldRedis.Spec.Redis.Exporter = redis.Spec.Redis.Exporter
	oldRedis.Spec.Redis.ExporterImage = redis.Spec.Redis.ExporterImage
	oldRedis.Spec.Redis.ExporterVersion = redis.Spec.Redis.ExporterVersion
	oldRedis.Spec.Redis.DisableExporterProbes = redis.Spec.Redis.DisableExporterProbes
	oldRedis.Spec.Redis.Version = redis.Spec.Redis.Version

	// update sentinels
	if redis.Spec.Sentinel.Replicas >= 0 {
		oldRedis.Spec.Sentinel.Replicas = redis.Spec.Sentinel.Replicas
	}
	oldRedis.Spec.Sentinel.Resources = redis.Spec.Sentinel.Resources
	oldRedis.Spec.Sentinel.Resources = redis.Spec.Sentinel.Resources
	oldRedis.Spec.Sentinel.CustomConfig = redis.Spec.Sentinel.CustomConfig
	oldRedis.Spec.Sentinel.Command = redis.Spec.Sentinel.Command
	oldRedis.Spec.Sentinel.Envs = redis.Spec.Sentinel.Envs

	var b bytes.Buffer
	resp, err = ro.client.Put(ro.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("/apis/storage.spotahome.com/v1alpha2/namespaces/%s/redisfailovers/%s", oldRedis.Namespace, oldRedis.Name)).
		JSONBody(oldRedis).
		Do().
		Body(&b)
	if err != nil {
		return fmt.Errorf("failed to update redisfailover, %s/%s, err: %v, body: %v ", oldRedis.Namespace, oldRedis.Name, err, b.String())
	}
	if !resp.IsOK() {
		return fmt.Errorf("failed to update redisfailover, %s/%s, statuscode: %v, body: %v", oldRedis.Namespace, oldRedis.Name, resp.StatusCode(), b.String())
	}

	return nil
}

func (ro *RedisOperator) convertRedis(svc apistructs.Service) RedisSettings {
	settings := RedisSettings{}
	settings.Version = "3.2.12"
	settings.Envs = svc.Env
	settings.Replicas = int32(svc.Scale)
	settings.Resources = RedisFailoverResources{
		Requests: CPUAndMem{
			CPU:    fmt.Sprintf("%dm", int(1000*ro.overcommit.CPUOvercommit(svc.Resources.Cpu))),
			Memory: fmt.Sprintf("%dMi", ro.overcommit.MemoryOvercommit(int(svc.Resources.Mem))),
		},
		Limits: CPUAndMem{
			CPU:    fmt.Sprintf("%dm", int(1000*svc.Resources.Cpu)),
			Memory: fmt.Sprintf("%dMi", int(svc.Resources.Mem)),
		},
	}
	settings.Image = svc.Image
	return settings
}

func convertSentinel(svc apistructs.Service) SentinelSettings {
	settings := SentinelSettings{}
	settings.Envs = svc.Env
	settings.Replicas = int32(svc.Scale)
	settings.Resources = RedisFailoverResources{
		Requests: CPUAndMem{ // sentinel Not over-provisioned, because it should already occupy very little resources
			CPU:    fmt.Sprintf("%dm", int(1000*svc.Resources.Cpu)),
			Memory: fmt.Sprintf("%dMi", int(svc.Resources.Mem)),
		},
		Limits: CPUAndMem{
			CPU:    fmt.Sprintf("%dm", int(1000*svc.Resources.Cpu)),
			Memory: fmt.Sprintf("%dMi", int(svc.Resources.Mem)),
		},
	}
	settings.CustomConfig = []string{
		fmt.Sprintf("auth-pass %s", svc.Env["requirepass"]),
		"down-after-milliseconds 12000",
		"failover-timeout 12000",
	}
	return settings
}

func genK8SNamespace(namespace, name string) string {
	return strutil.Concat(namespace, "--", name)
}
