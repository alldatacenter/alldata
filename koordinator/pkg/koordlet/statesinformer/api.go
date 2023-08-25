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

package statesinformer

import corev1 "k8s.io/api/core/v1"

type PodMeta struct {
	Pod       *corev1.Pod
	CgroupDir string
}

func (in *PodMeta) DeepCopy() *PodMeta {
	out := new(PodMeta)
	out.Pod = in.Pod.DeepCopy()
	out.CgroupDir = in.CgroupDir
	return out
}
