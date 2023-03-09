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

package extension

import (
	"encoding/json"
	"fmt"

	corev1 "k8s.io/api/core/v1"
	v1 "k8s.io/apiserver/pkg/quota/v1"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"
)

//RootQuotaName means quotaTree's root\head.
const (
	SystemQuotaName        = "system"
	RootQuotaName          = "root"
	DefaultQuotaName       = "default"
	QuotaKoordinatorPrefix = "quota.scheduling.koordinator.sh"
	LabelQuotaIsParent     = QuotaKoordinatorPrefix + "/is-parent"
	LabelQuotaParent       = QuotaKoordinatorPrefix + "/parent"
	LabelAllowLentResource = QuotaKoordinatorPrefix + "/allow-lent-resource"
	LabelQuotaName         = QuotaKoordinatorPrefix + "/name"
	AnnotationSharedWeight = QuotaKoordinatorPrefix + "/shared-weight"
	AnnotationRuntime      = QuotaKoordinatorPrefix + "/runtime"
	AnnotationRequest      = QuotaKoordinatorPrefix + "/request"
)

func GetParentQuotaName(quota *v1alpha1.ElasticQuota) string {
	parentName := quota.Labels[LabelQuotaParent]
	if parentName == "" {
		return RootQuotaName //default return RootQuotaName
	}
	return parentName
}

func IsParentQuota(quota *v1alpha1.ElasticQuota) bool {
	return quota.Labels[LabelQuotaIsParent] == "true"
}

func IsAllowLentResource(quota *v1alpha1.ElasticQuota) bool {
	return quota.Labels[LabelAllowLentResource] != "false"
}

func GetSharedWeight(quota *v1alpha1.ElasticQuota) corev1.ResourceList {
	value, exist := quota.Annotations[AnnotationSharedWeight]
	if exist {
		resList := corev1.ResourceList{}
		err := json.Unmarshal([]byte(value), &resList)
		if err == nil && !v1.IsZero(resList) {
			return resList
		}
	}
	return quota.Spec.Max.DeepCopy() //default equals to max
}

func IsForbiddenModify(quota *v1alpha1.ElasticQuota) (bool, error) {
	if quota.Name == SystemQuotaName || quota.Name == RootQuotaName {
		// can't modify SystemQuotaGroup
		return true, fmt.Errorf("invalid quota %s", quota.Name)
	}

	return false, nil
}

var GetQuotaName = func(pod *corev1.Pod) string {
	return pod.Labels[LabelQuotaName]
}
