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

package adaptor

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/informers"
	clientset "k8s.io/client-go/kubernetes"
	podutil "sigs.k8s.io/descheduler/pkg/descheduler/pod"
	k8sdeschedulerframework "sigs.k8s.io/descheduler/pkg/framework"

	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
)

var _ k8sdeschedulerframework.Handle = &frameworkHandleAdaptor{}

type frameworkHandleAdaptor struct {
	handle framework.Handle
}

func NewFrameworkHandleAdaptor(handle framework.Handle) k8sdeschedulerframework.Handle {
	return &frameworkHandleAdaptor{
		handle: handle,
	}
}

// ClientSet returns a kubernetes clientSet.
func (a *frameworkHandleAdaptor) ClientSet() clientset.Interface {
	return a.handle.ClientSet()
}

func (a *frameworkHandleAdaptor) Evictor() k8sdeschedulerframework.Evictor {
	return &evictorAdaptor{
		evictor: a.handle.Evictor(),
	}
}

func (a *frameworkHandleAdaptor) GetPodsAssignedToNodeFunc() podutil.GetPodsAssignedToNodeFunc {
	return func(s string, filterFunc podutil.FilterFunc) ([]*corev1.Pod, error) {
		fn := a.handle.GetPodsAssignedToNodeFunc()
		return fn(s, func(pod *corev1.Pod) bool {
			return filterFunc(pod)
		})
	}
}

func (a *frameworkHandleAdaptor) SharedInformerFactory() informers.SharedInformerFactory {
	return a.handle.SharedInformerFactory()
}
