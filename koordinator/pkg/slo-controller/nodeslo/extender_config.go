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

package nodeslo

import (
	"fmt"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

var (
	defaultExtensionCfg = extension.ExtensionCfgMap{}
)

func getDefaultExtensionCfg() *extension.ExtensionCfgMap {
	return defaultExtensionCfg.DeepCopy()
}

func RegisterDefaultExtension(key string, ext interface{}) error {
	if defaultExtensionCfg.Object == nil {
		defaultExtensionCfg.Object = map[string]extension.ExtensionCfg{}
	}
	if _, exist := defaultExtensionCfg.Object[key]; exist {
		return fmt.Errorf("extension %v of defaultExtensionStrategy already exist", key)
	}
	defaultExtensionCfg.Object[key] = extension.ExtensionCfg{ClusterStrategy: ext}
	return nil
}

func UnregisterDefaultExtension(key string) {
	delete(defaultExtensionCfg.Object, key)
	if len(defaultExtensionCfg.Object) == 0 {
		defaultExtensionCfg.Object = nil
	}
}
