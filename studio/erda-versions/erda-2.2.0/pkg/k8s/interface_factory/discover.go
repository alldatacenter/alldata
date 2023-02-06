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

package interface_factory

import (
	"strings"

	"k8s.io/client-go/kubernetes"
)

func IsResourceExist(client kubernetes.Interface, resourceKind, groupVersion string) (bool, error) {
	groups, err := client.Discovery().ServerResourcesForGroupVersion(groupVersion)
	if err != nil {
		return false, err
	}
	for _, resource := range groups.APIResources {
		if strings.EqualFold(resourceKind, resource.Kind) {
			return true, nil
		}
	}
	return false, nil
}
