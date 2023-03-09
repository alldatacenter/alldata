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

package testing

import (
	"k8s.io/apimachinery/pkg/runtime/schema"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/scheme"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/v1alpha2"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework/runtime"
)

var configDecoder = scheme.Codecs.UniversalDecoder()

// NewFramework creates a Framework from the register functions and options.
func NewFramework(fns []RegisterPluginFunc, profileName string, opts ...runtime.Option) (framework.Handle, error) {
	registry := runtime.Registry{}
	profile := &deschedulerconfig.DeschedulerProfile{
		Name:    profileName,
		Plugins: &deschedulerconfig.Plugins{},
	}
	for _, f := range fns {
		f(&registry, profile)
	}
	return runtime.NewFramework(registry, profile, opts...)
}

type RegisterPluginFunc func(reg *runtime.Registry, profile *deschedulerconfig.DeschedulerProfile)

func RegisterDeschedulePlugin(pluginName string, pluginNewFunc runtime.PluginFactory) RegisterPluginFunc {
	return RegisterPluginAsExtensions(pluginName, pluginNewFunc, "Deschedule")
}

func RegisterBalancePlugin(pluginName string, pluginNewFunc runtime.PluginFactory) RegisterPluginFunc {
	return RegisterPluginAsExtensions(pluginName, pluginNewFunc, "Balance")
}

func RegisterEvictorPlugin(pluginName string, pluginNewFunc runtime.PluginFactory) RegisterPluginFunc {
	return RegisterPluginAsExtensions(pluginName, pluginNewFunc, "Evictor")
}

// RegisterPluginAsExtensions returns a function to register a Plugin as given extensionPoints to a given registry.
func RegisterPluginAsExtensions(pluginName string, pluginNewFunc runtime.PluginFactory, extensions ...string) RegisterPluginFunc {
	return RegisterPluginAsExtensionsWithWeight(pluginName, 1, pluginNewFunc, extensions...)
}

// RegisterPluginAsExtensionsWithWeight returns a function to register a Plugin as given extensionPoints with weight to a given registry.
func RegisterPluginAsExtensionsWithWeight(pluginName string, weight int32, pluginNewFunc runtime.PluginFactory, extensions ...string) RegisterPluginFunc {
	return func(reg *runtime.Registry, profile *deschedulerconfig.DeschedulerProfile) {
		reg.Register(pluginName, pluginNewFunc)
		for _, extension := range extensions {
			ps := getPluginSetByExtension(profile.Plugins, extension)
			if ps == nil {
				continue
			}
			ps.Enabled = append(ps.Enabled, deschedulerconfig.Plugin{Name: pluginName})
		}
		// Use defaults from latest config API version.
		var gvk schema.GroupVersionKind
		gvk = v1alpha2.SchemeGroupVersion.WithKind(pluginName + "Args")
		if args, _, err := configDecoder.Decode(nil, &gvk, nil); err == nil {
			profile.PluginConfig = append(profile.PluginConfig, deschedulerconfig.PluginConfig{
				Name: pluginName,
				Args: args,
			})
		}
	}
}

func getPluginSetByExtension(plugins *deschedulerconfig.Plugins, extension string) *deschedulerconfig.PluginSet {
	switch extension {
	case "Deschedule":
		return &plugins.Deschedule
	case "Balance":
		return &plugins.Balance
	case "Evictor":
		return &plugins.Evictor
	default:
		return nil
	}
}
