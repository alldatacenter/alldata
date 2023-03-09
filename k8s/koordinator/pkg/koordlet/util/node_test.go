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
	"path"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_GetKubeQosRelativePath(t *testing.T) {
	guaranteedPathSystemd := GetKubeQosRelativePath(corev1.PodQOSGuaranteed)
	assert.Equal(t, path.Clean(system.KubeRootNameSystemd), guaranteedPathSystemd)

	burstablePathSystemd := GetKubeQosRelativePath(corev1.PodQOSBurstable)
	assert.Equal(t, path.Join(system.KubeRootNameSystemd, system.KubeBurstableNameSystemd), burstablePathSystemd)

	besteffortPathSystemd := GetKubeQosRelativePath(corev1.PodQOSBestEffort)
	assert.Equal(t, path.Join(system.KubeRootNameSystemd, system.KubeBesteffortNameSystemd), besteffortPathSystemd)

	system.SetupCgroupPathFormatter(system.Cgroupfs)
	guaranteedPathCgroupfs := GetKubeQosRelativePath(corev1.PodQOSGuaranteed)
	assert.Equal(t, path.Clean(system.KubeRootNameCgroupfs), guaranteedPathCgroupfs)

	burstablePathCgroupfs := GetKubeQosRelativePath(corev1.PodQOSBurstable)
	assert.Equal(t, path.Join(system.KubeRootNameCgroupfs, system.KubeBurstableNameCgroupfs), burstablePathCgroupfs)

	besteffortPathCgroupfs := GetKubeQosRelativePath(corev1.PodQOSBestEffort)
	assert.Equal(t, path.Join(system.KubeRootNameCgroupfs, system.KubeBesteffortNameCgroupfs), besteffortPathCgroupfs)
}
