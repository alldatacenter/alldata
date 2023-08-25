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
	"encoding/json"
	"errors"
	"sort"
	"strconv"
	"strings"
	"time"

	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/util/strategicpatch"
	"k8s.io/klog/v2"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

func GetGangGroupId(s []string) string {
	sort.Strings(s)
	return strings.Join(s, ",")
}

func GetId(namespace, name string) string {
	return namespace + "/" + name
}

func GetGangNameByPod(pod *v1.Pod) string {
	if pod == nil {
		return ""
	}
	var gangName string
	if gangName = pod.Labels[v1alpha1.PodGroupLabel]; gangName == "" {
		// nolint:staticcheck // SA1019: extension.LabelLightweightCoschedulingPodGroupName is deprecated
		if gangName = pod.Labels[extension.LabelLightweightCoschedulingPodGroupName]; gangName == "" {
			gangName = extension.GetGangName(pod)
		}
	}
	return gangName
}

func GetGangMinNumFromPod(pod *v1.Pod) (minNum int, err error) {
	// nolint:staticcheck // SA1019: extension.LabelLightweightCoschedulingPodGroupMinAvailable is deprecated
	if s := pod.Labels[extension.LabelLightweightCoschedulingPodGroupMinAvailable]; s != "" {
		val, err := strconv.ParseInt(pod.Labels[extension.LabelLightweightCoschedulingPodGroupMinAvailable], 10, 32)
		return int(val), err
	}
	if _, ok := pod.Annotations[extension.AnnotationGangMinNum]; ok {
		return extension.GetMinNum(pod)
	}
	return 0, errors.New("missing min available")
}

func IsPodNeedGang(pod *v1.Pod) bool {
	return GetGangNameByPod(pod) != ""
}

// GetWaitTimeDuration returns a wait timeout based on the following precedences:
// 1. spec.scheduleTimeoutSeconds of the given pg, if specified
// 2. fall back to defaultTimeout
func GetWaitTimeDuration(pg *v1alpha1.PodGroup, defaultTimeout time.Duration) time.Duration {
	if pg != nil && pg.Spec.ScheduleTimeoutSeconds != nil {
		if *pg.Spec.ScheduleTimeoutSeconds >= 0 {
			return time.Duration(*pg.Spec.ScheduleTimeoutSeconds) * time.Second
		} else {
			klog.Errorf("podGroup's ScheduleTimeoutSeconds illegal, podGroupName: %s", klog.KObj(pg))
		}
	}

	return defaultTimeout
}

// StringToGangGroupSlice
// Parse gang group's annotation like :"["nsA/gangA","nsB/gangB"]"  => goLang slice : []string{"nsA/gangA"."nsB/gangB"}
func StringToGangGroupSlice(s string) ([]string, error) {
	gangGroup := make([]string, 0)
	err := json.Unmarshal([]byte(s), &gangGroup)
	if err != nil {
		return gangGroup, err
	}
	return gangGroup, nil
}

// CreateMergePatch return patch generated from original and new interfaces
func CreateMergePatch(original, new interface{}) ([]byte, error) {
	pvByte, err := json.Marshal(original)
	if err != nil {
		return nil, err
	}
	cloneByte, err := json.Marshal(new)
	if err != nil {
		return nil, err
	}
	patch, err := strategicpatch.CreateTwoWayMergePatch(pvByte, cloneByte, original)
	if err != nil {
		return nil, err
	}
	return patch, nil
}
