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

package plugins

import (
	"flag"
	"fmt"
	"strings"

	"k8s.io/apimachinery/pkg/util/runtime"
	clientset "k8s.io/client-go/kubernetes"
	cliflag "k8s.io/component-base/cli/flag"
	"k8s.io/component-base/featuregate"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
)

var (
	DefaultMutableQOSExtPluginFG featuregate.MutableFeatureGate = featuregate.NewFeatureGate()
	DefaultQOSExtPluginsFG       featuregate.FeatureGate        = DefaultMutableQOSExtPluginFG

	defaultQOSExtPluginsFG = map[featuregate.Feature]featuregate.FeatureSpec{}

	globalExtensionPlugins = map[featuregate.Feature]ExtensionPlugin{}
)

func RegisterQOSExtPlugin(feature featuregate.Feature, featureSpec featuregate.FeatureSpec,
	plugin ExtensionPlugin) error {
	if _, exist := globalExtensionPlugins[feature]; exist {
		return fmt.Errorf("%v extension plugin already registered", feature)
	}
	globalExtensionPlugins[feature] = plugin
	defaultQOSExtPluginsFG[feature] = featureSpec
	klog.V(4).Infof("qos extension plugin %v registered", feature)
	return nil
}

type ExtensionPlugin interface {
	InitFlags(fs *flag.FlagSet)
	Setup(client clientset.Interface, metricCache metriccache.MetricCache, statesInformer statesinformer.StatesInformer)
	Run(stopCh <-chan struct{})
}

type QOSExtensionConfig struct {
	FeatureGates map[string]bool
}

func (c *QOSExtensionConfig) InitFlags(fs *flag.FlagSet) {
	for _, plugin := range globalExtensionPlugins {
		plugin.InitFlags(fs)
	}
	runtime.Must(DefaultMutableQOSExtPluginFG.Add(defaultQOSExtPluginsFG))
	fs.Var(cliflag.NewMapStringBool(&c.FeatureGates), "qos-extension-plugins",
		"A set of key=value pairs that describe feature gates for qos extensions plugins alpha/experimental features. "+
			"Options are:\n"+strings.Join(DefaultMutableQOSExtPluginFG.KnownFeatures(), "\n"))
}

func SetupPlugins(client clientset.Interface, metricCache metriccache.MetricCache, statesInformer statesinformer.StatesInformer) {
	for f, plugin := range globalExtensionPlugins {
		plugin.Setup(client, metricCache, statesInformer)
		klog.V(4).Infof("setup for qos extension plugin %v", f)
	}
}

func StartPlugins(cfg *QOSExtensionConfig, stopCh <-chan struct{}) error {
	if err := DefaultMutableQOSExtPluginFG.SetFromMap(cfg.FeatureGates); err != nil {
		return err
	}
	klog.V(4).Infof("there are %v qos extension plugins registered", len(globalExtensionPlugins))
	for feature, plugin := range globalExtensionPlugins {
		if DefaultQOSExtPluginsFG.Enabled(feature) {
			plugin.Run(stopCh)
			klog.V(4).Infof("qos extension plugin %v started", feature)
		} else {
			klog.V(4).Infof("qos extension plugin %v not enabled, no need to start", feature)
		}
	}
	return nil
}
