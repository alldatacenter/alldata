/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package config

import (
	"reflect"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/util/workqueue"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/handler"
)

var _ handler.EventHandler = &EnqueueRequestForConfigMap{}

type EnqueueRequestForConfigMap struct {
	EnqueueRequest     func(q *workqueue.RateLimitingInterface)
	SyncCacheIfChanged func(configMap *corev1.ConfigMap) bool
}

func (p *EnqueueRequestForConfigMap) Create(evt event.CreateEvent, q workqueue.RateLimitingInterface) {
	configMap, ok := evt.Object.(*corev1.ConfigMap)
	if !ok {
		return
	}
	if configMap.Namespace != ConfigNameSpace || configMap.Name != SLOCtrlConfigMap {
		return
	}

	if p.SyncCacheIfChanged != nil && !p.SyncCacheIfChanged(configMap) {
		return
	}
	p.EnqueueRequest(&q)
}

func (p *EnqueueRequestForConfigMap) Delete(evt event.DeleteEvent, q workqueue.RateLimitingInterface) {
}

func (p *EnqueueRequestForConfigMap) Generic(evt event.GenericEvent, q workqueue.RateLimitingInterface) {
}

func (p *EnqueueRequestForConfigMap) Update(evt event.UpdateEvent, q workqueue.RateLimitingInterface) {
	newConfigMap := evt.ObjectNew.(*corev1.ConfigMap)
	oldConfigMap := evt.ObjectOld.(*corev1.ConfigMap)
	if reflect.DeepEqual(newConfigMap.Data, oldConfigMap.Data) {
		return
	}
	if newConfigMap.Namespace != ConfigNameSpace || newConfigMap.Name != SLOCtrlConfigMap {
		return
	}
	if p.SyncCacheIfChanged != nil && !p.SyncCacheIfChanged(newConfigMap) {
		return
	}

	p.EnqueueRequest(&q)
}
