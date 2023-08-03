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

package core

import (
	"sync"

	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/klog/v2"
)

// ScaleMinQuotaManager The child nodes under each node will be divided into two categories, one allows
// scaling min quota, and the other does not allow. When our total resources are insufficient, the min quota of child
// nodes will be proportionally reduced. In order to calculate the scaling of the min quota, we will count the sum of
// the min quota of the child nodes of each node in advance. The quota of the child nodes that allow scaling is stored
// in enableScaleSubsSumMinQuotaMap, and the quota of child nodes that do not allow scaling is stored in
// disableScaleSubsSumMinQuotaMap
type ScaleMinQuotaManager struct {
	lock sync.RWMutex
	// enableScaleSubsSumMinQuotaMap key: quotaName, val: sum of its enableScale children's minQuota
	enableScaleSubsSumMinQuotaMap map[string]v1.ResourceList
	// disableScaleSubsSumMinQuotaMap key: quotaName, val: sum of its disableScale children's minQuota
	disableScaleSubsSumMinQuotaMap map[string]v1.ResourceList
	// originalMinQuotaMap stores the original minQuota, when children's sum minQuota is smaller than the
	// totalRes, just return the originalMinQuota.
	originalMinQuotaMap         map[string]v1.ResourceList
	quotaEnableMinQuotaScaleMap map[string]bool
}

func NewScaleMinQuotaManager() *ScaleMinQuotaManager {
	info := &ScaleMinQuotaManager{
		originalMinQuotaMap:            make(map[string]v1.ResourceList),
		enableScaleSubsSumMinQuotaMap:  make(map[string]v1.ResourceList),
		disableScaleSubsSumMinQuotaMap: make(map[string]v1.ResourceList),
		quotaEnableMinQuotaScaleMap:    make(map[string]bool),
	}
	return info
}

func (s *ScaleMinQuotaManager) update(parQuotaName, subQuotaName string, subMinQuota v1.ResourceList, enableScaleMinQuota bool) {
	s.lock.Lock()
	defer s.lock.Unlock()

	if _, ok := s.enableScaleSubsSumMinQuotaMap[parQuotaName]; !ok {
		s.enableScaleSubsSumMinQuotaMap[parQuotaName] = v1.ResourceList{}
	}
	if _, ok := s.disableScaleSubsSumMinQuotaMap[parQuotaName]; !ok {
		s.disableScaleSubsSumMinQuotaMap[parQuotaName] = v1.ResourceList{}
	}

	// step1: delete the oldMinQuota if present
	if enable, ok := s.quotaEnableMinQuotaScaleMap[subQuotaName]; ok {
		if enable {
			s.enableScaleSubsSumMinQuotaMap[parQuotaName] = quotav1.SubtractWithNonNegativeResult(s.enableScaleSubsSumMinQuotaMap[parQuotaName],
				s.originalMinQuotaMap[subQuotaName])
		} else {
			s.disableScaleSubsSumMinQuotaMap[parQuotaName] = quotav1.SubtractWithNonNegativeResult(s.disableScaleSubsSumMinQuotaMap[parQuotaName],
				s.originalMinQuotaMap[subQuotaName])
		}
	}

	// step2: add the newMinQuota
	if enableScaleMinQuota {
		s.enableScaleSubsSumMinQuotaMap[parQuotaName] = quotav1.Add(s.enableScaleSubsSumMinQuotaMap[parQuotaName], subMinQuota)
	} else {
		s.disableScaleSubsSumMinQuotaMap[parQuotaName] = quotav1.Add(s.disableScaleSubsSumMinQuotaMap[parQuotaName], subMinQuota)
	}

	klog.V(5).Infof("UpdateScaleMinQuota, quota:%v originalMinQuota change from :%v to %v,"+
		"enableMinQuotaScale change from :%v to :%v", subQuotaName, s.originalMinQuotaMap[subQuotaName],
		subMinQuota, s.quotaEnableMinQuotaScaleMap[subQuotaName], enableScaleMinQuota)

	// step3: record the newMinQuota
	s.originalMinQuotaMap[subQuotaName] = subMinQuota
	s.quotaEnableMinQuotaScaleMap[subQuotaName] = enableScaleMinQuota
}

func (s *ScaleMinQuotaManager) getScaledMinQuota(newTotalRes v1.ResourceList, parQuotaName, subQuotaName string) (bool, v1.ResourceList) {
	s.lock.Lock()
	defer s.lock.Unlock()

	if newTotalRes == nil || s.originalMinQuotaMap[subQuotaName] == nil {
		return false, nil
	}
	if s.disableScaleSubsSumMinQuotaMap[parQuotaName] == nil || s.enableScaleSubsSumMinQuotaMap[parQuotaName] == nil {
		return false, nil
	}

	if !s.quotaEnableMinQuotaScaleMap[subQuotaName] {
		return false, nil
	}

	// get the dimensions where children's minQuota sum is larger than newTotalRes
	needScaleDimensions := make([]v1.ResourceName, 0)
	for resName := range newTotalRes {
		sum := quotav1.Add(s.disableScaleSubsSumMinQuotaMap[parQuotaName], s.enableScaleSubsSumMinQuotaMap[parQuotaName])
		if newTotalRes.Name(resName, resource.DecimalSI).Cmp(*sum.Name(resName, resource.DecimalSI)) == -1 {
			needScaleDimensions = append(needScaleDimensions, resName)
		}
	}

	//  children's minQuota sum is smaller than totalRes in all dimensions
	if len(needScaleDimensions) == 0 {
		return true, s.originalMinQuotaMap[subQuotaName].DeepCopy()
	}

	// ensure the disableScale children's minQuota first
	newMinQuota := s.originalMinQuotaMap[subQuotaName].DeepCopy()
	for _, resourceDimension := range needScaleDimensions {
		needScaleTotal := *newTotalRes.Name(resourceDimension, resource.DecimalSI)
		disableTotal := s.disableScaleSubsSumMinQuotaMap[parQuotaName]
		needScaleTotal.Sub(*disableTotal.Name(resourceDimension, resource.DecimalSI))

		if needScaleTotal.Value() <= 0 {
			newMinQuota[resourceDimension] = *resource.NewQuantity(0, resource.DecimalSI)
		} else {
			// if still has minQuota left, enableScaleMinQuota children partition it according to their minQuotaValue.
			originalMinQuota := s.originalMinQuotaMap[subQuotaName]
			originalMinQuotaValue := originalMinQuota.Name(resourceDimension, resource.DecimalSI)

			enableTotal := s.enableScaleSubsSumMinQuotaMap[parQuotaName]
			enableTotalValue := enableTotal.Name(resourceDimension, resource.DecimalSI)

			newMinQuotaValue := int64(0)
			if enableTotalValue.Value() > 0 {
				newMinQuotaValue = int64(float64(getQuantityValue(needScaleTotal, resourceDimension)) *
					float64(getQuantityValue(*originalMinQuotaValue, resourceDimension)) / float64(getQuantityValue(*enableTotalValue, resourceDimension)))
			}

			newMinQuota[resourceDimension] = createQuantity(newMinQuotaValue, resourceDimension)
		}
	}
	return true, newMinQuota
}
