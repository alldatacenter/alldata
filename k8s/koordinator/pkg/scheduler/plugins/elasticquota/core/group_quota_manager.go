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
	"fmt"
	"reflect"
	"sync"

	v1 "k8s.io/api/core/v1"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/api/v1/resource"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

type GroupQuotaManager struct {
	// hierarchyUpdateLock used for resourceKeys/quotaInfoMap/quotaTreeWrapper change
	hierarchyUpdateLock sync.RWMutex
	// totalResource without systemQuotaGroup and DefaultQuotaGroup's used Quota
	totalResourceExceptSystemAndDefaultUsed v1.ResourceList
	// totalResource with systemQuotaGroup and DefaultQuotaGroup's used Quota
	totalResource v1.ResourceList
	// resourceKeys helps to store runtimeQuotaCalculators' resourceKey
	resourceKeys map[v1.ResourceName]struct{}
	// quotaInfoMap stores all the nodes, it can help get all parents conveniently
	quotaInfoMap map[string]*QuotaInfo
	// runtimeQuotaCalculatorMap helps calculate the subGroups' runtimeQuota in one quotaGroup
	runtimeQuotaCalculatorMap map[string]*RuntimeQuotaCalculator
	// quotaTopoNodeMap only stores the topology of the quota
	quotaTopoNodeMap     map[string]*QuotaTopoNode
	scaleMinQuotaEnabled bool
	// scaleMinQuotaManager is used when overRootResource
	scaleMinQuotaManager *ScaleMinQuotaManager
	once                 sync.Once
}

func NewGroupQuotaManager(systemGroupMax, defaultGroupMax v1.ResourceList) *GroupQuotaManager {
	quotaManager := &GroupQuotaManager{
		totalResourceExceptSystemAndDefaultUsed: v1.ResourceList{},
		totalResource:                           v1.ResourceList{},
		resourceKeys:                            make(map[v1.ResourceName]struct{}),
		quotaInfoMap:                            make(map[string]*QuotaInfo),
		runtimeQuotaCalculatorMap:               make(map[string]*RuntimeQuotaCalculator),
		quotaTopoNodeMap:                        make(map[string]*QuotaTopoNode),
		scaleMinQuotaManager:                    NewScaleMinQuotaManager(),
	}
	quotaManager.quotaInfoMap[extension.SystemQuotaName] = NewQuotaInfo(false, true, extension.SystemQuotaName, extension.RootQuotaName)
	quotaManager.quotaInfoMap[extension.SystemQuotaName].setMaxQuotaNoLock(systemGroupMax)
	quotaManager.quotaInfoMap[extension.DefaultQuotaName] = NewQuotaInfo(false, true, extension.DefaultQuotaName, extension.RootQuotaName)
	quotaManager.quotaInfoMap[extension.DefaultQuotaName].setMaxQuotaNoLock(defaultGroupMax)
	quotaManager.runtimeQuotaCalculatorMap[extension.RootQuotaName] = NewRuntimeQuotaCalculator(extension.RootQuotaName)
	quotaManager.setScaleMinQuotaEnabled(true)
	return quotaManager
}

func (gqm *GroupQuotaManager) setScaleMinQuotaEnabled(flag bool) {
	gqm.hierarchyUpdateLock.Lock()
	defer gqm.hierarchyUpdateLock.Unlock()

	gqm.scaleMinQuotaEnabled = flag
	klog.V(5).Infof("Set ScaleMinQuotaEnabled, flag:%v", gqm.scaleMinQuotaEnabled)
}

func (gqm *GroupQuotaManager) UpdateClusterTotalResource(deltaRes v1.ResourceList) {
	gqm.hierarchyUpdateLock.Lock()
	defer gqm.hierarchyUpdateLock.Unlock()

	klog.V(5).Infof("UpdateClusterResource deltaRes:%v", deltaRes)
	gqm.getQuotaInfoByNameNoLock(extension.DefaultQuotaName).lock.Lock()
	defer gqm.getQuotaInfoByNameNoLock(extension.DefaultQuotaName).lock.Unlock()

	gqm.getQuotaInfoByNameNoLock(extension.SystemQuotaName).lock.Lock()
	defer gqm.getQuotaInfoByNameNoLock(extension.SystemQuotaName).lock.Unlock()

	gqm.updateClusterTotalResourceNoLock(deltaRes)
}

// updateClusterTotalResourceNoLock no need to lock gqm.hierarchyUpdateLock and system/defaultQuotaGroup's lock
func (gqm *GroupQuotaManager) updateClusterTotalResourceNoLock(deltaRes v1.ResourceList) {
	gqm.totalResource = quotav1.Add(gqm.totalResource, deltaRes)

	sysAndDefaultUsed := gqm.quotaInfoMap[extension.DefaultQuotaName].CalculateInfo.Used.DeepCopy()
	sysAndDefaultUsed = quotav1.Add(sysAndDefaultUsed, gqm.quotaInfoMap[extension.SystemQuotaName].CalculateInfo.Used.DeepCopy())
	totalResNoSysOrDefault := quotav1.Subtract(gqm.totalResource, sysAndDefaultUsed)

	diffRes := quotav1.Subtract(totalResNoSysOrDefault, gqm.totalResourceExceptSystemAndDefaultUsed)

	if !quotav1.IsZero(diffRes) {
		gqm.totalResourceExceptSystemAndDefaultUsed = totalResNoSysOrDefault.DeepCopy()
		gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].setClusterTotalResource(totalResNoSysOrDefault)
		klog.V(5).Infof("UpdateClusterResource finish totalResourceExceptSystemAndDefaultUsed:%v", gqm.totalResourceExceptSystemAndDefaultUsed)
	}
}

func (gqm *GroupQuotaManager) GetClusterTotalResource() v1.ResourceList {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	return gqm.totalResource.DeepCopy()
}

// updateGroupDeltaRequestNoLock no need lock gqm.lock
func (gqm *GroupQuotaManager) updateGroupDeltaRequestNoLock(quotaName string, deltaReq v1.ResourceList) {
	curToAllParInfos := gqm.getCurToAllParentGroupQuotaInfoNoLock(quotaName)
	allQuotaInfoLen := len(curToAllParInfos)
	if allQuotaInfoLen <= 0 {
		return
	}

	defer gqm.scopedLockForQuotaInfo(curToAllParInfos)()

	gqm.recursiveUpdateGroupTreeWithDeltaRequest(deltaReq, curToAllParInfos)
}

// recursiveUpdateGroupTreeWithDeltaRequest update the quota of a node, also need update all parentNode, the lock operation
// of all quotaInfo is done by gqm. scopedLockForQuotaInfo, so just get treeWrappers' lock when calling treeWrappers' function
func (gqm *GroupQuotaManager) recursiveUpdateGroupTreeWithDeltaRequest(deltaReq v1.ResourceList, curToAllParInfos []*QuotaInfo) {
	for i := 0; i < len(curToAllParInfos); i++ {
		curQuotaInfo := curToAllParInfos[i]
		oldSubLimitReq := curQuotaInfo.getLimitRequestNoLock()
		curQuotaInfo.addRequestNonNegativeNoLock(deltaReq)
		if curQuotaInfo.Name == extension.SystemQuotaName || curQuotaInfo.Name == extension.DefaultQuotaName {
			return
		}
		newSubLimitReq := curQuotaInfo.getLimitRequestNoLock()
		deltaReq = quotav1.Subtract(newSubLimitReq, oldSubLimitReq)

		directParRuntimeCalculatorPtr := gqm.getRuntimeQuotaCalculatorByNameNoLock(curQuotaInfo.ParentName)
		if directParRuntimeCalculatorPtr == nil {
			klog.Errorf("treeWrapper not exist! quotaName:%v  parentName:%v", curQuotaInfo.Name, curQuotaInfo.ParentName)
			return
		}
		if directParRuntimeCalculatorPtr.needUpdateOneGroupRequest(curQuotaInfo) {
			directParRuntimeCalculatorPtr.updateOneGroupRequest(curQuotaInfo)
		}
	}
}

// updateGroupDeltaUsedNoLock updates the usedQuota of a node, it also updates all parent nodes
// no need to lock gqm.hierarchyUpdateLock
func (gqm *GroupQuotaManager) updateGroupDeltaUsedNoLock(quotaName string, delta v1.ResourceList) {
	curToAllParInfos := gqm.getCurToAllParentGroupQuotaInfoNoLock(quotaName)
	allQuotaInfoLen := len(curToAllParInfos)
	if allQuotaInfoLen <= 0 {
		return
	}

	defer gqm.scopedLockForQuotaInfo(curToAllParInfos)()
	for i := 0; i < allQuotaInfoLen; i++ {
		quotaInfo := curToAllParInfos[i]
		quotaInfo.addUsedNonNegativeNoLock(delta)
	}

	// if systemQuotaGroup or DefaultQuotaGroup's used change, update cluster total resource.
	if quotaName == extension.SystemQuotaName || quotaName == extension.DefaultQuotaName {
		gqm.updateClusterTotalResourceNoLock(v1.ResourceList{})
	}
}

func (gqm *GroupQuotaManager) RefreshRuntime(quotaName string) v1.ResourceList {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	return gqm.RefreshRuntimeNoLock(quotaName)
}

func (gqm *GroupQuotaManager) RefreshRuntimeNoLock(quotaName string) v1.ResourceList {
	quotaInfo := gqm.getQuotaInfoByNameNoLock(quotaName)
	if quotaInfo == nil {
		return nil
	}

	if quotaName == extension.SystemQuotaName || quotaName == extension.DefaultQuotaName {
		return quotaInfo.getMax()
	}

	curToAllParInfos := gqm.getCurToAllParentGroupQuotaInfoNoLock(quotaInfo.Name)

	defer gqm.scopedLockForQuotaInfo(curToAllParInfos)()

	totalRes := gqm.totalResourceExceptSystemAndDefaultUsed.DeepCopy()
	for i := len(curToAllParInfos) - 1; i >= 0; i-- {
		quotaInfo = curToAllParInfos[i]
		parRuntimeQuotaCalculator := gqm.getRuntimeQuotaCalculatorByNameNoLock(quotaInfo.ParentName)
		if parRuntimeQuotaCalculator == nil {
			klog.Errorf("treeWrapper not exist! parentQuotaName:%v", quotaInfo.ParentName)
			return nil
		}
		subTreeWrapper := gqm.getRuntimeQuotaCalculatorByNameNoLock(quotaInfo.Name)
		if subTreeWrapper == nil {
			klog.Errorf("treeWrapper not exist! parentQuotaName:%v", quotaInfo.Name)
			return nil
		}

		// 1. execute scaleMin logic with totalRes and update scaledMin if needed
		if gqm.scaleMinQuotaEnabled {
			needScale, newMinQuota := gqm.scaleMinQuotaManager.getScaledMinQuota(
				totalRes, quotaInfo.ParentName, quotaInfo.Name)
			if needScale {
				gqm.updateOneGroupAutoScaleMinQuotaNoLock(quotaInfo, newMinQuota)
			}
		}

		// 2. update parent's runtimeQuota
		if quotaInfo.RuntimeVersion != parRuntimeQuotaCalculator.getVersion() {
			parRuntimeQuotaCalculator.updateOneGroupRuntimeQuota(quotaInfo)
		}
		newSubGroupsTotalRes := quotaInfo.CalculateInfo.Runtime.DeepCopy()

		// 3. update subGroup's cluster resource  when i >= 1 (still has children)
		if i >= 1 {
			subTreeWrapper.setClusterTotalResource(newSubGroupsTotalRes)
		}

		// 4. update totalRes
		totalRes = newSubGroupsTotalRes
	}

	return curToAllParInfos[0].getMaskedRuntimeNoLock()
}

// updateOneGroupAutoScaleMinQuotaNoLock no need to lock gqm.lock
func (gqm *GroupQuotaManager) updateOneGroupAutoScaleMinQuotaNoLock(quotaInfo *QuotaInfo, newMinRes v1.ResourceList) {
	if !quotav1.Equals(quotaInfo.CalculateInfo.AutoScaleMin, newMinRes) {
		quotaInfo.setAutoScaleMinQuotaNoLock(newMinRes)
		gqm.runtimeQuotaCalculatorMap[quotaInfo.ParentName].updateOneGroupMinQuota(quotaInfo)
	}
}

func (gqm *GroupQuotaManager) getCurToAllParentGroupQuotaInfoNoLock(quotaName string) []*QuotaInfo {
	curToAllParInfos := make([]*QuotaInfo, 0)
	quotaInfo := gqm.getQuotaInfoByNameNoLock(quotaName)
	if quotaInfo == nil {
		return curToAllParInfos
	}

	for true {
		curToAllParInfos = append(curToAllParInfos, quotaInfo)
		if quotaInfo.ParentName == extension.RootQuotaName {
			break
		}

		quotaInfo = gqm.getQuotaInfoByNameNoLock(quotaInfo.ParentName)
		if quotaInfo == nil {
			return curToAllParInfos
		}
	}

	return curToAllParInfos
}

func (gqm *GroupQuotaManager) GetQuotaInfoByName(quotaName string) *QuotaInfo {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	return gqm.getQuotaInfoByNameNoLock(quotaName)
}

func (gqm *GroupQuotaManager) getQuotaInfoByNameNoLock(quotaName string) *QuotaInfo {
	return gqm.quotaInfoMap[quotaName]
}

func (gqm *GroupQuotaManager) getRuntimeQuotaCalculatorByNameNoLock(quotaName string) *RuntimeQuotaCalculator {
	return gqm.runtimeQuotaCalculatorMap[quotaName]
}

func (gqm *GroupQuotaManager) scopedLockForQuotaInfo(quotaList []*QuotaInfo) func() {
	listLen := len(quotaList)
	for i := listLen - 1; i >= 0; i-- {
		quotaList[i].lock.Lock()
	}

	return func() {
		for i := 0; i < listLen; i++ {
			quotaList[i].lock.Unlock()
		}
	}
}

func (gqm *GroupQuotaManager) UpdateQuota(quota *v1alpha1.ElasticQuota, isDelete bool) error {
	gqm.hierarchyUpdateLock.Lock()
	defer gqm.hierarchyUpdateLock.Unlock()

	quotaName := quota.Name
	if isDelete {
		_, exist := gqm.quotaInfoMap[quotaName]
		if !exist {
			return fmt.Errorf("get quota info failed, quotaName:%v", quotaName)
		}
		delete(gqm.quotaInfoMap, quotaName)
	} else {
		newQuotaInfo := NewQuotaInfoFromQuota(quota)
		// update the local quotaInfo's crd
		if localQuotaInfo, exist := gqm.quotaInfoMap[quotaName]; exist {
			// if the quotaMeta doesn't change, only runtime/used/request change causes update,
			// no need to call updateQuotaGroupConfigNoLock.
			if !localQuotaInfo.isQuotaMetaChange(newQuotaInfo) {
				return nil
			}
			localQuotaInfo.updateQuotaInfoFromRemote(newQuotaInfo)
		} else {
			gqm.quotaInfoMap[quotaName] = newQuotaInfo
		}
	}
	gqm.updateQuotaGroupConfigNoLock()

	return nil
}

func (gqm *GroupQuotaManager) updateQuotaGroupConfigNoLock() {
	// rebuild gqm.quotaTopoNodeMap
	gqm.buildSubParGroupTopoNoLock()
	// reset gqm.runtimeQuotaCalculator
	gqm.resetAllGroupQuotaNoLock()
}

// buildSubParGroupTopoNoLock reBuild a nodeTree from root, no need to lock gqm.lock
func (gqm *GroupQuotaManager) buildSubParGroupTopoNoLock() {
	// rebuild QuotaTopoNodeMap
	gqm.quotaTopoNodeMap = make(map[string]*QuotaTopoNode)
	rootNode := NewQuotaTopoNode(NewQuotaInfo(false, true, extension.RootQuotaName, extension.RootQuotaName))
	gqm.quotaTopoNodeMap[extension.RootQuotaName] = rootNode

	// add node according to the quotaInfoMap
	for quotaName, quotaInfo := range gqm.quotaInfoMap {
		if quotaName == extension.SystemQuotaName || quotaName == extension.DefaultQuotaName {
			continue
		}
		gqm.quotaTopoNodeMap[quotaName] = NewQuotaTopoNode(quotaInfo)
	}

	// build tree according to the parGroupName
	for _, topoNode := range gqm.quotaTopoNodeMap {
		if topoNode.name == extension.RootQuotaName {
			continue
		}
		parQuotaTopoNode := gqm.quotaTopoNodeMap[topoNode.quotaInfo.ParentName]
		// incase load child before its parent
		if parQuotaTopoNode == nil {
			parQuotaTopoNode = NewQuotaTopoNode(&QuotaInfo{
				Name: topoNode.quotaInfo.ParentName,
			})
		}
		topoNode.parQuotaTopoNode = parQuotaTopoNode
		parQuotaTopoNode.addChildGroupQuotaInfo(topoNode)
	}
}

// ResetAllGroupQuotaNoLock no need to lock gqm.lock
func (gqm *GroupQuotaManager) resetAllGroupQuotaNoLock() {
	childRequestMap, childUsedMap := make(quotaResMapType), make(quotaResMapType)
	for quotaName, topoNode := range gqm.quotaTopoNodeMap {
		if quotaName == extension.RootQuotaName {
			continue
		}
		topoNode.quotaInfo.lock.Lock()
		if !topoNode.quotaInfo.IsParent {
			childRequestMap[quotaName] = topoNode.quotaInfo.CalculateInfo.Request.DeepCopy()
			childUsedMap[quotaName] = topoNode.quotaInfo.CalculateInfo.Used.DeepCopy()
		}
		topoNode.quotaInfo.clearForResetNoLock()
		topoNode.quotaInfo.lock.Unlock()
	}

	// clear old runtimeQuotaCalculator
	gqm.runtimeQuotaCalculatorMap = make(map[string]*RuntimeQuotaCalculator)
	// reset runtimeQuotaCalculator
	gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName] = NewRuntimeQuotaCalculator(extension.RootQuotaName)
	gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].setClusterTotalResource(gqm.totalResourceExceptSystemAndDefaultUsed)
	rootNode := gqm.quotaTopoNodeMap[extension.RootQuotaName]
	gqm.resetAllGroupQuotaRecursiveNoLock(rootNode)
	gqm.updateResourceKeyNoLock()

	// subGroup's topo relation may change; refresh the request/used from bottom to top
	for quotaName, topoNode := range gqm.quotaTopoNodeMap {
		if !topoNode.quotaInfo.IsParent {
			gqm.updateGroupDeltaRequestNoLock(quotaName, childRequestMap[quotaName])
			gqm.updateGroupDeltaUsedNoLock(quotaName, childUsedMap[quotaName])
		}
	}
}

// ResetAllGroupQuotaRecursiveNoLock no need to lock gqm.lock
func (gqm *GroupQuotaManager) resetAllGroupQuotaRecursiveNoLock(rootNode *QuotaTopoNode) {
	childGroupQuotaInfos := rootNode.getChildGroupQuotaInfos()
	for subName, topoNode := range childGroupQuotaInfos {
		gqm.runtimeQuotaCalculatorMap[subName] = NewRuntimeQuotaCalculator(subName)

		gqm.updateOneGroupMaxQuotaNoLock(topoNode.quotaInfo)
		gqm.updateMinQuotaNoLock(topoNode.quotaInfo)
		gqm.updateOneGroupSharedWeightNoLock(topoNode.quotaInfo)

		gqm.resetAllGroupQuotaRecursiveNoLock(topoNode)
	}
}

// updateOneGroupMaxQuotaNoLock no need to lock gqm.lock
func (gqm *GroupQuotaManager) updateOneGroupMaxQuotaNoLock(quotaInfo *QuotaInfo) {
	quotaInfo.lock.Lock()
	defer quotaInfo.lock.Unlock()

	runtimeQuotaCalculator := gqm.getRuntimeQuotaCalculatorByNameNoLock(quotaInfo.ParentName)
	runtimeQuotaCalculator.updateOneGroupMaxQuota(quotaInfo)
}

// updateMinQuotaNoLock no need to lock gqm.lock
func (gqm *GroupQuotaManager) updateMinQuotaNoLock(quotaInfo *QuotaInfo) {
	gqm.updateOneGroupOriginalMinQuotaNoLock(quotaInfo)
	gqm.scaleMinQuotaManager.update(quotaInfo.ParentName, quotaInfo.Name,
		quotaInfo.CalculateInfo.Min, gqm.scaleMinQuotaEnabled)
}

// updateOneGroupOriginalMinQuotaNoLock no need to lock gqm.lock
func (gqm *GroupQuotaManager) updateOneGroupOriginalMinQuotaNoLock(quotaInfo *QuotaInfo) {
	quotaInfo.lock.Lock()
	defer quotaInfo.lock.Unlock()

	quotaInfo.setAutoScaleMinQuotaNoLock(quotaInfo.CalculateInfo.Min)
	gqm.runtimeQuotaCalculatorMap[quotaInfo.ParentName].updateOneGroupMinQuota(quotaInfo)
}

// updateOneGroupSharedWeightNoLock no need to lock gqm.lock
func (gqm *GroupQuotaManager) updateOneGroupSharedWeightNoLock(quotaInfo *QuotaInfo) {
	quotaInfo.lock.Lock()
	defer quotaInfo.lock.Unlock()

	gqm.runtimeQuotaCalculatorMap[quotaInfo.ParentName].updateOneGroupSharedWeight(quotaInfo)
}

func (gqm *GroupQuotaManager) updateResourceKeyNoLock() {
	// collect all dimensions
	resourceKeys := make(map[v1.ResourceName]struct{})
	for quotaName, quotaInfo := range gqm.quotaInfoMap {
		if quotaName == extension.DefaultQuotaName || quotaName == extension.SystemQuotaName {
			continue
		}
		for resName := range quotaInfo.CalculateInfo.Max {
			resourceKeys[resName] = struct{}{}
		}
	}

	if !reflect.DeepEqual(resourceKeys, gqm.resourceKeys) {
		gqm.resourceKeys = resourceKeys
		for _, runtimeQuotaCalculator := range gqm.runtimeQuotaCalculatorMap {
			runtimeQuotaCalculator.updateResourceKeys(resourceKeys)
		}
	}
}

func (gqm *GroupQuotaManager) GetAllQuotaNames() map[string]struct{} {
	quotaInfoMap := make(map[string]struct{})
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	for name := range gqm.quotaInfoMap {
		quotaInfoMap[name] = struct{}{}
	}
	return quotaInfoMap
}

func (gqm *GroupQuotaManager) updatePodRequestNoLock(quotaName string, oldPod, newPod *v1.Pod) {
	var oldPodReq, newPodReq v1.ResourceList
	if oldPod != nil {
		oldPodReq, _ = resource.PodRequestsAndLimits(oldPod)
	} else {
		oldPodReq = make(v1.ResourceList)
	}

	if newPod != nil {
		newPodReq, _ = resource.PodRequestsAndLimits(newPod)
	} else {
		newPodReq = make(v1.ResourceList)
	}

	deltaReq := quotav1.Subtract(newPodReq, oldPodReq)
	if quotav1.IsZero(deltaReq) {
		return
	}
	gqm.updateGroupDeltaRequestNoLock(quotaName, deltaReq)
}

func (gqm *GroupQuotaManager) updatePodUsedNoLock(quotaName string, oldPod, newPod *v1.Pod) {
	quotaInfo := gqm.getQuotaInfoByNameNoLock(quotaName)
	if quotaInfo == nil {
		return
	}
	if !quotaInfo.GetPodIsAssigned(newPod) && !quotaInfo.GetPodIsAssigned(oldPod) {
		klog.V(5).Infof("updatePodUsed, isAssigned is false, quotaName:%v, podName:%v",
			quotaName, getPodName(oldPod, newPod))
		return
	}

	var oldPodUsed, newPodUsed v1.ResourceList
	if oldPod != nil {
		oldPodUsed, _ = resource.PodRequestsAndLimits(oldPod)
	} else {
		oldPodUsed = make(v1.ResourceList)
	}

	if newPod != nil {
		newPodUsed, _ = resource.PodRequestsAndLimits(newPod)
	} else {
		newPodUsed = make(v1.ResourceList)
	}

	deltaUsed := quotav1.Subtract(newPodUsed, oldPodUsed)
	if quotav1.IsZero(deltaUsed) {
		klog.V(5).Infof("updatePodUsed, deltaUsedIsZero, quotaName:%v, podName:%v, podUsed:%v",
			quotaName, getPodName(oldPod, newPod), newPodUsed)
		return
	}
	gqm.updateGroupDeltaUsedNoLock(quotaName, deltaUsed)
}

func (gqm *GroupQuotaManager) updatePodCacheNoLock(quotaName string, pod *v1.Pod, isAdd bool) {
	quotaInfo := gqm.getQuotaInfoByNameNoLock(quotaName)
	if quotaInfo == nil {
		return
	}

	if isAdd {
		quotaInfo.addPodIfNotPresent(pod)
	} else {
		quotaInfo.removePodIfPresent(pod)
	}
}

func (gqm *GroupQuotaManager) UpdatePodIsAssigned(quotaName string, pod *v1.Pod, isAssigned bool) error {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	return gqm.updatePodIsAssignedNoLock(quotaName, pod, isAssigned)
}

func (gqm *GroupQuotaManager) updatePodIsAssignedNoLock(quotaName string, pod *v1.Pod, isAssigned bool) error {
	quotaInfo := gqm.getQuotaInfoByNameNoLock(quotaName)
	return quotaInfo.UpdatePodIsAssigned(pod, isAssigned)
}

func (gqm *GroupQuotaManager) getPodIsAssignedNoLock(quotaName string, pod *v1.Pod) bool {
	quotaInfo := gqm.getQuotaInfoByNameNoLock(quotaName)
	if quotaInfo == nil {
		return false
	}
	return quotaInfo.GetPodIsAssigned(pod)
}

func (gqm *GroupQuotaManager) MigratePod(pod *v1.Pod, out, in string) {
	gqm.hierarchyUpdateLock.Lock()
	defer gqm.hierarchyUpdateLock.Unlock()

	isAssigned := gqm.getPodIsAssignedNoLock(out, pod)
	gqm.updatePodRequestNoLock(out, pod, nil)
	gqm.updatePodUsedNoLock(out, pod, nil)
	gqm.updatePodCacheNoLock(out, pod, false)

	gqm.updatePodCacheNoLock(in, pod, true)
	gqm.updatePodIsAssignedNoLock(in, pod, isAssigned)
	gqm.updatePodRequestNoLock(in, nil, pod)
	gqm.updatePodUsedNoLock(in, nil, pod)
	klog.V(5).Infof("migrate pod :%v from quota:%v to quota:%v, podPhase:%v", pod.Name, out, in, pod.Status.Phase)
}

func (gqm *GroupQuotaManager) GetQuotaSummary(quotaName string) (*QuotaInfoSummary, bool) {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	quotaInfo := gqm.getQuotaInfoByNameNoLock(quotaName)
	if quotaInfo == nil {
		return nil, false
	}

	quotaSummary := quotaInfo.GetQuotaSummary()
	runtime := gqm.RefreshRuntimeNoLock(quotaName)
	quotaSummary.Runtime = runtime.DeepCopy()
	return quotaSummary, true
}

func (gqm *GroupQuotaManager) GetQuotaSummaries() map[string]*QuotaInfoSummary {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	result := make(map[string]*QuotaInfoSummary)
	for quotaName, quotaInfo := range gqm.quotaInfoMap {
		quotaSummary := quotaInfo.GetQuotaSummary()
		runtime := gqm.RefreshRuntimeNoLock(quotaName)
		quotaSummary.Runtime = runtime.DeepCopy()
		result[quotaName] = quotaSummary
	}

	return result
}

func (gqm *GroupQuotaManager) OnPodAdd(quotaName string, pod *v1.Pod) {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	quotaInfo := gqm.getQuotaInfoByNameNoLock(quotaName)
	if quotaInfo != nil && quotaInfo.isPodExist(pod) {
		return
	}

	gqm.updatePodCacheNoLock(quotaName, pod, true)
	gqm.updatePodRequestNoLock(quotaName, nil, pod)
	// in case failOver, update pod isAssigned explicitly according to its phase and NodeName.
	if pod.Spec.NodeName != "" && !util.IsPodTerminated(pod) {
		gqm.updatePodIsAssignedNoLock(quotaName, pod, true)
		gqm.updatePodUsedNoLock(quotaName, nil, pod)
	}
}

func (gqm *GroupQuotaManager) OnPodUpdate(newQuotaName, oldQuotaName string, newPod, oldPod *v1.Pod) {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	if oldQuotaName == newQuotaName {
		gqm.updatePodRequestNoLock(newQuotaName, oldPod, newPod)
		gqm.updatePodUsedNoLock(newQuotaName, oldPod, newPod)
	} else {
		isAssigned := gqm.getPodIsAssignedNoLock(oldQuotaName, oldPod)
		gqm.updatePodRequestNoLock(oldQuotaName, oldPod, nil)
		gqm.updatePodUsedNoLock(oldQuotaName, oldPod, nil)
		gqm.updatePodCacheNoLock(oldQuotaName, oldPod, false)

		gqm.updatePodCacheNoLock(newQuotaName, newPod, true)
		gqm.updatePodIsAssignedNoLock(newQuotaName, newPod, isAssigned)
		gqm.updatePodRequestNoLock(newQuotaName, nil, newPod)
		gqm.updatePodUsedNoLock(newQuotaName, nil, newPod)
	}
}

func (gqm *GroupQuotaManager) OnPodDelete(quotaName string, pod *v1.Pod) {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	gqm.updatePodRequestNoLock(quotaName, pod, nil)
	gqm.updatePodUsedNoLock(quotaName, pod, nil)
	gqm.updatePodCacheNoLock(quotaName, pod, false)
}

func (gqm *GroupQuotaManager) ReservePod(quotaName string, p *v1.Pod) {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	gqm.updatePodIsAssignedNoLock(quotaName, p, true)
	gqm.updatePodUsedNoLock(quotaName, nil, p)
}

func (gqm *GroupQuotaManager) UnreservePod(quotaName string, p *v1.Pod) {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	gqm.updatePodUsedNoLock(quotaName, p, nil)
	gqm.updatePodIsAssignedNoLock(quotaName, p, false)
}

func (gqm *GroupQuotaManager) GetQuotaInformationForSyncHandler(quotaName string) (used, request, runtime v1.ResourceList, err error) {
	gqm.hierarchyUpdateLock.RLock()
	defer gqm.hierarchyUpdateLock.RUnlock()

	quotaInfo := gqm.getQuotaInfoByNameNoLock(quotaName)
	if quotaInfo == nil {
		return nil, nil, nil, fmt.Errorf("groupQuotaManager doesn't have this quota:%v", quotaName)
	}

	runtime = gqm.RefreshRuntimeNoLock(quotaName)
	return quotaInfo.GetUsed(), quotaInfo.GetRequest(), runtime, nil
}

func getPodName(oldPod, newPod *v1.Pod) string {
	if oldPod != nil {
		return oldPod.Name
	}
	if newPod != nil {
		return newPod.Name
	}
	return ""
}
