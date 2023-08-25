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
	"k8s.io/component-base/featuregate"
)

// Plugin interface contains methods which must be implemented by all plugins.
type Plugin interface {
	// Name is the name of plugin, must be unique.
	Name() string
	// Start is called to run this plugin.
	Start() error
	// Stop is called to stop this plugin.
	Stop() error
	// Feature returns feature name of this plugin.
	Feature() featuregate.Feature
}

type PluginFactoryFn func(ctx *PluginContext) Plugin
