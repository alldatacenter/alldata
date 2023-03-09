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
	"flag"
	"strings"

	"k8s.io/apimachinery/pkg/util/runtime"
	cliflag "k8s.io/component-base/cli/flag"
	"k8s.io/component-base/featuregate"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/qosmanager/plugins"
)

var (
	DefaultMutableQoSManagerFG featuregate.MutableFeatureGate = featuregate.NewFeatureGate()
	DefaultQoSManagerFG        featuregate.FeatureGate        = DefaultMutableQoSManagerFG

	defaultQoSManagerFG = map[featuregate.Feature]featuregate.FeatureSpec{}

	QoSPluginFactories = map[featuregate.Feature]plugins.PluginFactoryFn{}
)

type Config struct {
	FeatureGates       map[string]bool
	PluginExtraConfigs map[string]string
}

func NewDefaultConfig() *Config {
	return &Config{
		FeatureGates:       map[string]bool{},
		PluginExtraConfigs: map[string]string{},
	}
}

func (c *Config) InitFlags(fs *flag.FlagSet) {
	fs.Var(cliflag.NewMapStringBool(&c.FeatureGates), "qos-plugins",
		"A set of key=value pairs that describe feature gates for QoS Manager plugins alpha/experimental features. "+
			"Options are:\n"+strings.Join(DefaultQoSManagerFG.KnownFeatures(), "\n"))
}

func init() {
	runtime.Must(DefaultMutableQoSManagerFG.Add(defaultQoSManagerFG))
}
