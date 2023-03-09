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
	"fmt"
	"sort"
	"strings"
	"time"

	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/util/strategicpatch"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"encoding/json"

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
	gangName = pod.Labels[v1alpha1.PodGroupLabel]
	if gangName == "" {
		gangName = extension.GetGangName(pod)
	}
	return gangName
}

func IsPodNeedGang(pod *v1.Pod) bool {
	return GetGangNameByPod(pod) != ""
}

func ParsePgTimeoutSeconds(timeoutSeconds int32) (time.Duration, error) {
	if timeoutSeconds <= 0 {
		return 0, fmt.Errorf("podGroup timeout value is illegal,timeout Value:%v", timeoutSeconds)
	}
	return time.Duration(timeoutSeconds) * time.Second, nil
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
