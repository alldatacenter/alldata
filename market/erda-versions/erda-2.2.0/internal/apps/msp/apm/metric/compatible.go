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

package metric

import "fmt"

func (p *provider) loadCompatibleTKs() error {
	list, err := p.db.ListCompatibleTKs()
	if err != nil {
		return fmt.Errorf("fail to load compatible terminus keys")
	}
	compatibleTKs := make(map[string][]string)
	for _, item := range list {
		if len(item.TerminusKeyRuntime) <= 0 || len(item.TerminusKey) <= 0 || item.TerminusKeyRuntime == item.TerminusKey {
			continue
		}
		compatibleTKs[item.TerminusKey] = append(compatibleTKs[item.TerminusKey], item.TerminusKeyRuntime)
	}
	p.compatibleTKs = compatibleTKs
	if len(compatibleTKs) > 0 {
		p.Log.Debugf("compatible terminus keys: %v", compatibleTKs)
	}
	return nil
}

func (p *provider) getRuntimeTerminusKeys(tk string) (list []string) {
	for _, key := range p.compatibleTKs[tk] {
		list = append(list, key)
	}
	list = append(list, tk)
	return list
}
