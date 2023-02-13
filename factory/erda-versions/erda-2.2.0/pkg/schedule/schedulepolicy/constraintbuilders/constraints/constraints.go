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

package constraints

// PodLabelsForAffinity used to set the parameters required for podantiaffinity,
type PodLabelsForAffinity struct {
	PodLabels map[string]string
	// Required use 'required' or 'preferred'
	Required bool
}

// Constraints each executor's constraints should implement it
type Constraints interface {
	IsConstraints()
}

type HostnameUtil interface {
	IPToHostname(ip string) string
}
