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

package util

import (
	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

// ParsePodID parse pod ID from the pod base path.
// e.g. 7712555c_ce62_454a_9e18_9ff0217b8941 from kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice
func ParsePodID(basename string) (string, error) {
	return system.CgroupPathFormatter.PodIDParser(basename)
}

func GetPIDsInPod(podParentDir string, cs []corev1.ContainerStatus) ([]uint32, error) {
	pids := make([]uint32, 0)
	for i := range cs {
		p, err := GetPIDsInContainer(podParentDir, &cs[i])
		if err != nil {
			return nil, err
		}
		pids = append(pids, p...)
	}
	return pids, nil
}
