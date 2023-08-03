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

package defaultevictor

import (
	"context"
	"fmt"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/client-go/kubernetes/scheme"
	k8sdeschedulerframework "sigs.k8s.io/descheduler/pkg/framework"
	"sigs.k8s.io/descheduler/pkg/framework/plugins/defaultevictor"

	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/v1alpha2"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/evictions"
	evictutils "github.com/koordinator-sh/koordinator/pkg/descheduler/evictions/utils"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework/plugins/kubernetes/adaptor"
)

const (
	PluginName = defaultevictor.PluginName
)

type DefaultEvictorArgs = defaultevictor.DefaultEvictorArgs

type DefaultEvictor struct {
	handle        framework.Handle
	evictorFilter k8sdeschedulerframework.EvictorPlugin
	evictor       *evictions.PodEvictor
}

var _ framework.EvictPlugin = &DefaultEvictor{}
var _ framework.FilterPlugin = &DefaultEvictor{}
var _ k8sdeschedulerframework.EvictorPlugin = &DefaultEvictor{}

func New(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	defaultArgs := &defaultevictor.DefaultEvictorArgs{}
	defaultevictor.SetDefaults_DefaultEvictorArgs(defaultArgs)
	if args == nil {
		args = defaultArgs
	} else {
		switch t := args.(type) {
		case *DefaultEvictorArgs:
			args = t
		case *runtime.Unknown:
			unknownObj := t
			decoder := scheme.Codecs.UniversalDecoder()
			var gvk schema.GroupVersionKind
			gvk = v1alpha2.SchemeGroupVersion.WithKind(defaultevictor.PluginName + "Args")
			obj, _, err := decoder.Decode(unknownObj.Raw, &gvk, defaultArgs)
			if err != nil {
				return nil, err
			}
			args = obj
		default:
			return nil, fmt.Errorf("got args of type %T, want *%sArgs", args, defaultevictor.PluginName)
		}
	}

	evictor, err := defaultevictor.New(args, adaptor.NewFrameworkHandleAdaptor(handle))
	if err != nil {
		return nil, err
	}

	policyGroupVersion, err := evictutils.SupportEviction(handle.ClientSet())
	if err != nil {
		return nil, fmt.Errorf("failed to fetch eviction groupVersion: %v", err)
	}
	if len(policyGroupVersion) == 0 {
		return nil, fmt.Errorf("server does not support eviction policy")
	}

	podEvictor := evictions.NewPodEvictor(
		handle.ClientSet(),
		handle.EventRecorder(),
		policyGroupVersion,
		false,
		nil,
		nil,
	)

	return &DefaultEvictor{
		handle:        handle,
		evictorFilter: evictor.(k8sdeschedulerframework.EvictorPlugin),
		evictor:       podEvictor,
	}, nil
}

func (d *DefaultEvictor) Name() string {
	return PluginName
}

func (d *DefaultEvictor) Filter(pod *corev1.Pod) bool {
	return d.evictorFilter.Filter(pod)
}

func (d *DefaultEvictor) PreEvictionFilter(pod *corev1.Pod) bool {
	return d.evictorFilter.PreEvictionFilter(pod)
}

func (d *DefaultEvictor) Evict(ctx context.Context, pod *corev1.Pod, evictOptions framework.EvictOptions) bool {
	return d.evictor.Evict(ctx, pod, evictOptions)
}
