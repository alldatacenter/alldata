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

package elasticquota

import (
	"encoding/json"
	"fmt"

	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

func (qt *quotaTopology) validateQuotaSelfItem(quota *v1alpha1.ElasticQuota) error {
	// min and max's each dimension should not have negative value
	if resourceNames := quotav1.IsNegative(quota.Spec.Max); len(resourceNames) > 0 {
		return fmt.Errorf("%v quota.Spec.Max's value < 0, in dimensions :%v", quota.Name, resourceNames)
	}

	if resourceNames := quotav1.IsNegative(quota.Spec.Min); len(resourceNames) > 0 {
		return fmt.Errorf("%v quota.Spec.Min's value < 0, in dimensions :%v", quota.Name, resourceNames)
	}

	var sharedRatio v1.ResourceList
	if quota.Annotations[extension.AnnotationSharedWeight] != "" {
		if err := json.Unmarshal([]byte(quota.Annotations[extension.AnnotationSharedWeight]), &sharedRatio); err != nil {
			return err
		}

		if resourceNames := quotav1.IsNegative(sharedRatio); len(resourceNames) > 0 {
			return fmt.Errorf("%v quota.Annotation[%v]'s value < 0, in dimension :%v", quota.Name, extension.AnnotationSharedWeight, resourceNames)
		}
	}

	// minQuota <= maxQuota
	for key, val := range quota.Spec.Min {
		if maxVal, exist := quota.Spec.Max[key]; !exist || maxVal.Cmp(val) == -1 {
			return fmt.Errorf("%v min :%v > max,%v", quota.Name, quota.Spec.Min, quota.Spec.Max)
		}
	}
	return nil
}

// validateQuotaTopology checks the quotaInfo's topology with its parent and its children (when update calls the function).
func (qt *quotaTopology) validateQuotaTopology(oldQuotaInfo, quotaInfo *QuotaInfo) error {
	// interchange between parentQuotaGroup and childQuotaGroup is prohibited now.
	if oldQuotaInfo != nil && oldQuotaInfo.IsParent != quotaInfo.IsParent {
		return fmt.Errorf("IsParent is forbidden modify now, quotaName:%v", oldQuotaInfo.Name)
	}

	// if the quotaInfo's parent is root and its IsParent is false, the following checks will be true, just return nil.
	if quotaInfo.ParentName == extension.RootQuotaName && !quotaInfo.IsParent {
		return nil
	}

	if err := qt.checkParentQuotaInfo(quotaInfo.Name, quotaInfo.ParentName); err != nil {
		return err
	}

	if err := qt.checkSubAndParentGroupMaxQuotaKeySame(quotaInfo); err != nil {
		return err
	}

	if err := qt.checkMinQuotaSum(quotaInfo); err != nil {
		return err
	}

	return nil
}

func (qt *quotaTopology) checkParentQuotaInfo(quotaName, parentName string) error {
	if parentName != extension.RootQuotaName {
		parentInfo, find := qt.quotaInfoMap[parentName]
		if !find {
			return fmt.Errorf("%v has parentName %v but not find parentInfo in quotaInfoMap", quotaName, parentName)
		}
		if _, exist := qt.quotaHierarchyInfo[parentName]; !exist {
			return fmt.Errorf("%v has parentName %v but not find parentInfo in quotaHierarchyInfo", quotaName, parentName)
		}
		if !parentInfo.IsParent {
			return fmt.Errorf("%v has parentName %v but the parentQuotaInfo's IsParent is false", quotaName, parentName)
		}
	}
	return nil
}

func (qt *quotaTopology) checkSubAndParentGroupMaxQuotaKeySame(quotaInfo *QuotaInfo) error {
	if quotaInfo.ParentName != extension.RootQuotaName {
		parentInfo := qt.quotaInfoMap[quotaInfo.ParentName]
		if !checkQuotaKeySame(parentInfo.CalculateInfo.Max, quotaInfo.CalculateInfo.Max) {
			return fmt.Errorf("checkSubAndParentGroupMaxQuotaKeySame failed: %v's key is not the same with %v",
				quotaInfo.ParentName, quotaInfo.Name)
		}
	}

	children, find := qt.quotaHierarchyInfo[quotaInfo.Name]
	if !find || len(children) == 0 {
		return nil
	}

	for name := range children {
		if child, exist := qt.quotaInfoMap[name]; exist {
			if !checkQuotaKeySame(quotaInfo.CalculateInfo.Max, child.CalculateInfo.Max) {
				return fmt.Errorf("checkSubAndParentGroupMaxQuotaKeySame failed: %v's key is not the same with %v",
					quotaInfo.Name, name)
			}
		} else {
			return fmt.Errorf("BUG quotaInfoMap and quotaTree information out of sync, losed :%v", name)
		}
	}

	return nil
}

// checkMinQuotaSum parent's minQuota should be larger or equal than the sum of allChildren's minQuota.
func (qt *quotaTopology) checkMinQuotaSum(quotaInfo *QuotaInfo) error {
	if quotaInfo.ParentName != extension.RootQuotaName {
		childMinSumNotIncludeSelf, err := qt.getChildMinQuotaSumExceptSpecificChild(quotaInfo.ParentName, quotaInfo.Name)
		if err != nil {
			return fmt.Errorf("checkMinQuotaSum failed: %v", err)
		}

		childMinSumIncludeSelf := quotav1.Add(childMinSumNotIncludeSelf, quotaInfo.CalculateInfo.Min)
		if isLessEqual, _ := quotav1.LessThanOrEqual(childMinSumIncludeSelf, qt.quotaInfoMap[quotaInfo.ParentName].CalculateInfo.Min); !isLessEqual {
			return fmt.Errorf("checkMinQuotaSum allChildren SumMinQuota > parentMinQuota, parent: %v", quotaInfo.ParentName)
		}
	}

	children, exist := qt.quotaHierarchyInfo[quotaInfo.Name]
	if !exist || len(children) == 0 {
		return nil
	}

	childMinSum, err := qt.getChildMinQuotaSumExceptSpecificChild(quotaInfo.Name, "")
	if err != nil {
		return fmt.Errorf("checkMinQuotaSum failed:%v", err)
	}

	if isLessEqual, _ := quotav1.LessThanOrEqual(childMinSum, quotaInfo.CalculateInfo.Min); !isLessEqual {
		return fmt.Errorf("checkMinQuotaSum allChildrn SumMinQuota > MinQuota, parent: %v", quotaInfo.Name)
	}

	return nil
}

func (qt *quotaTopology) getChildMinQuotaSumExceptSpecificChild(parentName, skipQuota string) (allChildQuotaSum v1.ResourceList, err error) {
	allChildQuotaSum = v1.ResourceList{}
	if parentName == extension.RootQuotaName {
		return allChildQuotaSum, nil
	}

	childQuotaList, exist := qt.quotaHierarchyInfo[parentName]
	if !exist {
		return nil, fmt.Errorf("not found child quota list, parent: %v", parentName)
	}

	for childName := range childQuotaList {
		if childName == skipQuota {
			continue
		}

		if quotaInfo, exist := qt.quotaInfoMap[childName]; exist {
			allChildQuotaSum = quotav1.Add(allChildQuotaSum, quotaInfo.CalculateInfo.Min)
		} else {
			err = fmt.Errorf("BUG quotaInfoMap and quotaTree information out of sync, losed :%v", childName)
			return nil, err
		}
	}

	return allChildQuotaSum, nil
}

func toElasticQuota(obj interface{}) *v1alpha1.ElasticQuota {
	if obj == nil {
		return nil
	}

	var unstructuredObj *unstructured.Unstructured
	switch t := obj.(type) {
	case *v1alpha1.ElasticQuota:
		return obj.(*v1alpha1.ElasticQuota)
	case *unstructured.Unstructured:
		unstructuredObj = obj.(*unstructured.Unstructured)
	case cache.DeletedFinalStateUnknown:
		var ok bool
		unstructuredObj, ok = t.Obj.(*unstructured.Unstructured)
		if !ok {
			klog.Errorf("Fail to convert quota object %T to *unstructured.Unstructured", obj)
			return nil
		}
	default:
		klog.Errorf("Unable to handle quota object in %T", obj)
		return nil
	}

	quota := &v1alpha1.ElasticQuota{}
	err := scheme.Scheme.Convert(unstructuredObj, quota, nil)
	if err != nil {
		klog.Errorf("Fail to convert unstructed object %v to Quota: %v", obj, err)
		return nil
	}
	return quota
}

func quotaFieldsCopy(q *v1alpha1.ElasticQuota) v1alpha1.ElasticQuota {
	return v1alpha1.ElasticQuota{
		ObjectMeta: metav1.ObjectMeta{
			Labels: map[string]string{
				extension.LabelQuotaParent:   q.Labels[extension.LabelQuotaParent],
				extension.LabelQuotaIsParent: q.Labels[extension.LabelQuotaIsParent],
			},
		},
		Spec: *q.Spec.DeepCopy(),
	}
}

func checkQuotaKeySame(parent, child v1.ResourceList) bool {
	for k := range parent {
		if _, ok := child[k]; !ok {
			return false
		}
	}
	for k := range child {
		if _, ok := parent[k]; !ok {
			return false
		}
	}
	return true
}
