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

package configextensions

import (
	"fmt"

	corev1 "k8s.io/api/core/v1"
	clientset "k8s.io/client-go/kubernetes"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/util"
)

type QOSPolicyType string

const (
	QOSPolicyCPUBurst  QOSPolicyType = "CPUBurst"
	QOSPolicyMemoryQOS QOSPolicyType = "MemoryQOS"
)

var globalQOSControlPlugins = map[string]QOSGreyControlPlugin{}

func RegisterQOSGreyCtrlPlugin(name string, plugin QOSGreyControlPlugin) error {
	if _, exist := globalQOSControlPlugins[name]; exist {
		return fmt.Errorf("qos grep control plugin %v already exist", name)
	}
	globalQOSControlPlugins[name] = plugin
	return nil
}

func RunQOSGreyCtrlPlugins(client clientset.Interface, stopCh <-chan struct{}) {
	for name, plugin := range globalQOSControlPlugins {
		if err := plugin.Setup(client); err != nil {
			klog.Warningf("setup plugin %v failed, error %v", name, err)
			continue
		}
		plugin.Run(stopCh)
		klog.V(4).Infof("pod qos grey control plugin %v started", name)
	}
}

func InjectQOSGreyCtrlPlugins(pod *corev1.Pod, policyType QOSPolicyType, policy *interface{}) bool {
	injected := false
	for name, plugin := range globalQOSControlPlugins {
		if pluginInjected, err := plugin.InjectPodPolicy(pod, policyType, policy); err != nil {
			klog.Warningf("running qos grey control plugin %v for pod %v failed", name, util.GetPodKey(pod))
		} else if pluginInjected {
			klog.V(5).Infof("running qos grey control plugin %v for pod %v success, policy detail %v",
				name, util.GetPodKey(pod))
			injected = true
		}
	}
	return injected
}

func UnregisterQOSGreyCtrlPlugin(name string) {
	delete(globalQOSControlPlugins, name)
}

func ClearQOSGreyCtrlPlugin() {
	globalQOSControlPlugins = map[string]QOSGreyControlPlugin{}
}

type QOSGreyControlPlugin interface {
	Setup(kubeClient clientset.Interface) error
	Run(stopCh <-chan struct{})
	InjectPodPolicy(pod *corev1.Pod, policyType QOSPolicyType, greyCtlPolicy *interface{}) (bool, error)
}
