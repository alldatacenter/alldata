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
	"encoding/json"
	"fmt"
	"math/rand"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/klog/v2"
	schetesting "k8s.io/kubernetes/pkg/scheduler/testing"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

const (
	GigaByte = 1024 * 1048576
)

func TestGroupQuotaManager_QuotaAdd(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.UpdateClusterTotalResource(createResourceList(10000000, 3000*GigaByte))
	AddQuotaToManager(t, gqm, "test1", extension.RootQuotaName, 96000, 160*GigaByte, 50000, 50*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "test2", extension.RootQuotaName, 96000, 160*GigaByte, 80000, 80*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "test3", extension.RootQuotaName, 96000, 160*GigaByte, 40000, 40*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "test4", extension.RootQuotaName, 96000, 160*GigaByte, 0, 0*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "test5", extension.RootQuotaName, 96000, 160*GigaByte, 96000, 96*GigaByte, true, false)

	quotaInfo := gqm.GetQuotaInfoByName("test1")
	if quotaInfo.CalculateInfo.SharedWeight.Name(v1.ResourceCPU, resource.DecimalSI).Value() != 96000 {
		t.Errorf("error")
	}

	assert.Equal(t, 6, len(gqm.runtimeQuotaCalculatorMap))
	assert.Equal(t, 7, len(gqm.quotaInfoMap))
	assert.Equal(t, 2, len(gqm.resourceKeys))
}

func TestGroupQuotaManager_QuotaAdd_AutoMakeUpSharedWeight(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.UpdateClusterTotalResource(createResourceList(1000000, 200*GigaByte))

	quota := CreateQuota("test", extension.RootQuotaName, 96000, 160*GigaByte, 50000, 80*GigaByte, true, false)
	delete(quota.Annotations, extension.AnnotationSharedWeight)

	err := gqm.UpdateQuota(quota, false)
	assert.Nil(t, err)

	quotaInfo := gqm.quotaInfoMap["test"]
	if quotaInfo.CalculateInfo.SharedWeight.Cpu().Value() != 96000 || quotaInfo.CalculateInfo.SharedWeight.Memory().Value() != 160*GigaByte {
		t.Errorf("error:%v", quotaInfo.CalculateInfo.SharedWeight)
	}
}

func TestGroupQuotaManager_QuotaAdd_AutoMakeUpScaleRatio2(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	quota := &v1alpha1.ElasticQuota{
		TypeMeta: metav1.TypeMeta{
			Kind: "ElasticQuota",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:        "test",
			Namespace:   "",
			Annotations: make(map[string]string),
			Labels:      make(map[string]string),
		},
		Spec: v1alpha1.ElasticQuotaSpec{
			Max: v1.ResourceList{
				v1.ResourceCPU:    *resource.NewQuantity(96, resource.DecimalSI),
				v1.ResourceMemory: *resource.NewQuantity(1000, resource.DecimalSI),
				v1.ResourcePods:   *resource.NewQuantity(1000, resource.DecimalSI),
			},
		},
	}

	quota.Annotations[extension.AnnotationSharedWeight] = fmt.Sprintf("{\"cpu\":%v, \"memory\":\"%v\"}", 0, 0)
	quota.Labels[extension.LabelQuotaParent] = extension.RootQuotaName
	quota.Labels[extension.LabelQuotaIsParent] = "false"
	gqm.UpdateQuota(quota, false)

	quotaInfo := gqm.GetQuotaInfoByName("test")
	if quotaInfo.CalculateInfo.SharedWeight.Cpu().Value() != 96 ||
		quotaInfo.CalculateInfo.SharedWeight.Memory().Value() != 1000 ||
		quotaInfo.CalculateInfo.SharedWeight.Pods().Value() != 1000 {
		t.Errorf("error:%v", quotaInfo.CalculateInfo.SharedWeight)
	}
}

func TestGroupQuotaManager_UpdateQuotaInternal(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	AddQuotaToManager(t, gqm, "test1", extension.RootQuotaName, 96, 160*GigaByte, 50, 80*GigaByte, true, false)

	quota := CreateQuota("test1", extension.RootQuotaName, 64, 100*GigaByte, 50, 80*GigaByte, true, false)
	gqm.UpdateQuota(quota, false)
	quotaInfo := gqm.quotaInfoMap["test1"]
	assert.True(t, quotaInfo != nil)
	assert.False(t, quotaInfo.IsParent)
	assert.Equal(t, createResourceList(64, 100*GigaByte), quotaInfo.CalculateInfo.Max)
	assert.Equal(t, createResourceList(50, 80*GigaByte), quotaInfo.CalculateInfo.AutoScaleMin)
	assert.Equal(t, int64(64), quotaInfo.CalculateInfo.SharedWeight.Cpu().Value())
	assert.Equal(t, int64(100*GigaByte), quotaInfo.CalculateInfo.SharedWeight.Memory().Value())

	AddQuotaToManager(t, gqm, "test2", extension.RootQuotaName, 96, 160*GigaByte, 80, 80*GigaByte, true, false)

	quota = CreateQuota("test1", extension.RootQuotaName, 84, 120*GigaByte, 60, 100*GigaByte, true, false)
	quota.Labels[extension.LabelQuotaIsParent] = "true"
	err := gqm.UpdateQuota(quota, false)
	assert.Nil(t, err)
	quotaInfo = gqm.quotaInfoMap["test1"]
	assert.True(t, quotaInfo != nil)
	assert.True(t, quotaInfo.IsParent)
	assert.Equal(t, createResourceList(84, 120*GigaByte), quotaInfo.CalculateInfo.Max)
	assert.Equal(t, createResourceList(60, 100*GigaByte), quotaInfo.CalculateInfo.AutoScaleMin)
	assert.Equal(t, int64(84), quotaInfo.CalculateInfo.SharedWeight.Cpu().Value())
	assert.Equal(t, int64(120*GigaByte), quotaInfo.CalculateInfo.SharedWeight.Memory().Value())
}

func TestGroupQuotaManager_UpdateQuota(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	quota := CreateQuota("test1", "test-parent", 64, 100*GigaByte, 50, 80*GigaByte, true, false)
	gqm.UpdateQuota(quota, false)
	assert.Equal(t, len(gqm.quotaInfoMap), 3)
}

func TestGroupQuotaManager_UpdateQuotaInternalAndRequest(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	deltaRes := createResourceList(96, 160*GigaByte)
	gqm.UpdateClusterTotalResource(deltaRes)
	assert.Equal(t, deltaRes, gqm.totalResource)
	totalRes := gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource.DeepCopy()
	assert.Equal(t, deltaRes, totalRes)

	AddQuotaToManager(t, gqm, "test1", extension.RootQuotaName, 96, 160*GigaByte, 50, 80*GigaByte, true, false)

	// test1 request[120, 290]  runtime == maxQuota
	request := createResourceList(120, 290*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("test1", request)
	runtime := gqm.RefreshRuntime("test1")
	assert.Equal(t, deltaRes, runtime)

	quota1 := CreateQuota("test1", extension.RootQuotaName, 64, 100*GigaByte, 60, 90*GigaByte, true, false)
	quota1.Labels[extension.LabelQuotaIsParent] = "false"
	err := gqm.UpdateQuota(quota1, false)
	assert.Nil(t, err)
	quotaInfo := gqm.GetQuotaInfoByName("test1")
	assert.Equal(t, createResourceList(64, 100*GigaByte), quotaInfo.CalculateInfo.Max)

	runtime = gqm.RefreshRuntime("test1")
	assert.Equal(t, createResourceList(64, 100*GigaByte), runtime)
}

func TestGroupQuotaManager_DeleteOneGroup(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.UpdateClusterTotalResource(createResourceList(1000, 1000*GigaByte))
	quota1 := AddQuotaToManager(t, gqm, "test1", extension.RootQuotaName, 96, 160*GigaByte, 50, 80*GigaByte, true, false)
	quota2 := AddQuotaToManager(t, gqm, "test2", extension.RootQuotaName, 96, 160*GigaByte, 80, 80*GigaByte, true, false)
	quota3 := AddQuotaToManager(t, gqm, "test3", extension.RootQuotaName, 96, 160*GigaByte, 40, 40*GigaByte, true, false)
	assert.Equal(t, 4, len(gqm.runtimeQuotaCalculatorMap))
	assert.Equal(t, 5, len(gqm.quotaInfoMap))

	err := gqm.UpdateQuota(quota1, true)
	assert.Nil(t, err)
	quotaInfo := gqm.GetQuotaInfoByName("test1")
	assert.True(t, quotaInfo == nil)

	err = gqm.UpdateQuota(quota2, true)
	assert.Nil(t, err)
	quotaInfo = gqm.GetQuotaInfoByName("test2")
	assert.True(t, quotaInfo == nil)

	err = gqm.UpdateQuota(quota3, true)
	assert.Nil(t, err)
	quotaInfo = gqm.GetQuotaInfoByName("test3")
	assert.True(t, quotaInfo == nil)

	assert.Equal(t, 1, len(gqm.runtimeQuotaCalculatorMap))
	assert.Equal(t, 2, len(gqm.quotaInfoMap))

	AddQuotaToManager(t, gqm, "youku", extension.RootQuotaName, 96, 160*GigaByte, 70, 70*GigaByte, true, false)
	quotaInfo = gqm.GetQuotaInfoByName("youku")
	assert.NotNil(t, quotaInfo)
	assert.Equal(t, 2, len(gqm.runtimeQuotaCalculatorMap))
	assert.Equal(t, 3, len(gqm.quotaInfoMap))
}

func TestGroupQuotaManager_UpdateQuotaDeltaRequest(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	deltaRes := createResourceList(96, 160*GigaByte)
	gqm.UpdateClusterTotalResource(deltaRes)
	assert.Equal(t, deltaRes, gqm.totalResource)
	totalRes := gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource.DeepCopy()
	assert.Equal(t, deltaRes, totalRes)

	AddQuotaToManager(t, gqm, "test1", extension.RootQuotaName, 96, 160*GigaByte, 50, 80*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "test2", extension.RootQuotaName, 96, 160*GigaByte, 40, 80*GigaByte, true, false)

	// test1 request[120, 290]  runtime == maxQuota
	request := createResourceList(120, 200*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("test1", request)
	runtime := gqm.RefreshRuntime("test1")
	assert.Equal(t, deltaRes, runtime)

	// test2 request[150, 210]  runtime
	request = createResourceList(150, 210*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("test2", request)
	runtime = gqm.RefreshRuntime("test1")
	assert.Equal(t, createResourceList(53, 80*GigaByte), runtime)
	runtime = gqm.RefreshRuntime("test2")
	assert.Equal(t, createResourceList(43, 80*GigaByte), runtime)
}

func TestGroupQuotaManager_NotAllowLentResource(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	deltaRes := createResourceList(100, 0)
	gqm.UpdateClusterTotalResource(deltaRes)

	AddQuotaToManager(t, gqm, "test1", extension.RootQuotaName, 96, 0, 60, 0, true, false)
	AddQuotaToManager(t, gqm, "test2", extension.RootQuotaName, 96, 0, 40, 0, false, false)

	request := createResourceList(120, 0)
	gqm.updateGroupDeltaRequestNoLock("test1", request)
	runtime := gqm.RefreshRuntime("test1")
	assert.Equal(t, int64(60), runtime.Cpu().Value())
	runtime = gqm.RefreshRuntime("test2")
	assert.Equal(t, int64(40), runtime.Cpu().Value())
}

func TestGroupQuotaManager_UpdateQuotaRequest(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	deltaRes := createResourceList(96, 160*GigaByte)
	gqm.UpdateClusterTotalResource(deltaRes)
	assert.Equal(t, deltaRes, gqm.totalResource)
	totalRes := gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource.DeepCopy()
	assert.Equal(t, deltaRes, totalRes)

	AddQuotaToManager(t, gqm, "test1", extension.RootQuotaName, 96, 160*GigaByte, 50, 80*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "test2", extension.RootQuotaName, 96, 160*GigaByte, 40, 80*GigaByte, true, false)

	// 1. initial test1 request [60, 100]
	request := createResourceList(60, 100*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("test1", request)
	runtime := gqm.RefreshRuntime("test1")
	assert.Equal(t, request, runtime)

	// test1 request[120, 290]  runtime == maxQuota
	newRequest := createResourceList(120, 200*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("test1", newRequest)
	runtime = gqm.RefreshRuntime("test1")
	assert.Equal(t, deltaRes, runtime)

	// test2 request[150, 210]  runtime
	request = createResourceList(150, 210*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("test2", request)
	runtime = gqm.RefreshRuntime("test1")
	assert.Equal(t, createResourceList(53, 80*GigaByte), runtime)
	runtime = gqm.RefreshRuntime("test2")
	assert.Equal(t, createResourceList(43, 80*GigaByte), runtime)
}

func TestGroupQuotaManager_UpdateGroupDeltaUsed(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	AddQuotaToManager(t, gqm, "test1", "root", 96, 160*GigaByte, 50, 80*GigaByte, true, false)

	// 1. test1 used[120, 290]  runtime == maxQuota
	used := createResourceList(120, 290*GigaByte)
	gqm.updateGroupDeltaUsedNoLock("test1", used)
	quotaInfo := gqm.GetQuotaInfoByName("test1")
	assert.NotNil(t, quotaInfo)
	assert.Equal(t, used, quotaInfo.CalculateInfo.Used)

	// 2. used increases to [130,300]
	used = createResourceList(10, 10*GigaByte)
	gqm.updateGroupDeltaUsedNoLock("test1", used)
	quotaInfo = gqm.GetQuotaInfoByName("test1")
	assert.NotNil(t, quotaInfo)
	assert.Equal(t, createResourceList(130, 300*GigaByte), quotaInfo.CalculateInfo.Used)

	// 3. used decreases to [90,100]
	used = createResourceList(-40, -200*GigaByte)
	gqm.updateGroupDeltaUsedNoLock("test1", used)
	quotaInfo = gqm.GetQuotaInfoByName("test1")
	assert.NotNil(t, quotaInfo)
	assert.Equal(t, createResourceList(90, 100*GigaByte), quotaInfo.CalculateInfo.Used)
}

func TestGroupQuotaManager_MultiQuotaAdd(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	AddQuotaToManager(t, gqm, "test1", extension.RootQuotaName, 96, 160*GigaByte, 60, 100*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "test1-sub1", "test1", 96, 160*GigaByte, 10, 30*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "test1-sub2", "test1", 96, 160*GigaByte, 20, 30*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "test1-sub3", "test1", 96, 160*GigaByte, 30, 40*GigaByte, true, false)

	AddQuotaToManager(t, gqm, "test2", extension.RootQuotaName, 300, 400*GigaByte, 200, 300*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "test2-sub1", "test2", 96, 160*GigaByte, 96, 160*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "test2-sub2", "test2", 82, 100*GigaByte, 80, 80*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "test2-sub2-1", "test2-sub2", 96, 160*GigaByte, 0, 0*GigaByte, true, false)

	assert.Equal(t, 9, len(gqm.runtimeQuotaCalculatorMap))
	assert.Equal(t, 10, len(gqm.quotaInfoMap))
	assert.Equal(t, 2, len(gqm.resourceKeys))
	assert.Equal(t, 2, len(gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].quotaTree[v1.ResourceCPU].quotaNodes))
	assert.Equal(t, 2, len(gqm.runtimeQuotaCalculatorMap["test2"].quotaTree[v1.ResourceCPU].quotaNodes))
	assert.Equal(t, 1, len(gqm.runtimeQuotaCalculatorMap["test2-sub2"].quotaTree[v1.ResourceCPU].quotaNodes))
}

// TestGroupQuotaManager_MultiUpdateQuotaRequest  test the relationship of request and max
// parentQuotaGroup's request is the sum of limitedRequest of its child.
func TestGroupQuotaManager_MultiUpdateQuotaRequest(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	deltaRes := createResourceList(96, 160*GigaByte)
	gqm.UpdateClusterTotalResource(deltaRes)
	assert.Equal(t, deltaRes, gqm.totalResource)
	totalRes := gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource.DeepCopy()
	assert.Equal(t, deltaRes, totalRes)

	// test1 Max[96, 160]  Min[50,80] request[96,130]
	//   |-- test1-a Max[96, 160]  Min[50,80] request[96,130]
	//         |-- a-123 Max[96, 160]  Min[50,80] request[96,130]
	AddQuotaToManager(t, gqm, "test1", extension.RootQuotaName, 96, 160*GigaByte, 50, 80*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "test1-a", "test1", 96, 160*GigaByte, 50, 80*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "a-123", "test1-a", 96, 160*GigaByte, 50, 80*GigaByte, true, false)

	request := createResourceList(96, 130*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("a-123", request)
	runtime := gqm.RefreshRuntime("a-123")
	assert.Equal(t, request, runtime)
	runtime = gqm.RefreshRuntime("test1-a")
	assert.Equal(t, request, runtime)
	runtime = gqm.RefreshRuntime("test1")
	assert.Equal(t, request, runtime)

	// decrease a-123 Max[64, 128]  Min[50,80] request[96,130]
	quota1 := CreateQuota("a-123", "test1-a", 64, 128*GigaByte, 50, 80*GigaByte, true, false)
	gqm.UpdateQuota(quota1, false)
	quotaInfo := gqm.GetQuotaInfoByName("test1-a")
	assert.Equal(t, createResourceList(96, 160*GigaByte), quotaInfo.CalculateInfo.Max)
	runtime = gqm.RefreshRuntime("a-123")
	assert.Equal(t, runtime, quotaInfo.CalculateInfo.Request)
	assert.Equal(t, createResourceList(64, 128*GigaByte), runtime)
	quotaInfo = gqm.GetQuotaInfoByName("a-123")
	assert.Equal(t, request, quotaInfo.CalculateInfo.Request)

	// increase
	quota1 = CreateQuota("a-123", "test1-a", 100, 200*GigaByte, 90, 160*GigaByte, true, false)
	gqm.UpdateQuota(quota1, false)
	quotaInfo = gqm.GetQuotaInfoByName("test1-a")
	assert.Equal(t, createResourceList(96, 160*GigaByte), quotaInfo.CalculateInfo.Max)
	assert.Equal(t, createResourceList(96, 130*GigaByte), quotaInfo.CalculateInfo.Request)
	runtime = gqm.RefreshRuntime("a-123")
	assert.Equal(t, runtime, quotaInfo.CalculateInfo.Request)
	assert.Equal(t, request, runtime)
	quotaInfo = gqm.GetQuotaInfoByName("a-123")
	assert.Equal(t, request, quotaInfo.CalculateInfo.Request)
}

func TestGroupQuotaManager_UpdateDefaultQuotaGroup(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	gqm.UpdateQuota(&v1alpha1.ElasticQuota{
		ObjectMeta: metav1.ObjectMeta{
			Name: "default",
		},
		Spec: v1alpha1.ElasticQuotaSpec{
			Max: createResourceList(100, 1000),
		},
	}, false)
	assert.Equal(t, gqm.GetQuotaInfoByName("default").CalculateInfo.Max, createResourceList(100, 1000))
	runtime := gqm.RefreshRuntime("default")
	assert.Equal(t, runtime, createResourceList(100, 1000))
}

// TestGroupQuotaManager_MultiUpdateQuotaRequest2 test the relationship of min/max and request
// increase the request to test the case request < min, min < request < max, max < request
func TestGroupQuotaManager_MultiUpdateQuotaRequest2(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	deltaRes := createResourceList(96, 160*GigaByte)
	gqm.UpdateClusterTotalResource(deltaRes)
	assert.Equal(t, deltaRes, gqm.totalResource)
	totalRes := gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource.DeepCopy()
	assert.Equal(t, deltaRes, totalRes)

	// test1 Max[96, 160]  Min[80,80]
	//   |-- test1-a Max[60, 120]  Min[50,80]
	//         |-- a-123 Max[30, 60]  Min[20,40]
	AddQuotaToManager(t, gqm, "test1", extension.RootQuotaName, 96, 160*GigaByte, 80, 80*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "test1-a", "test1", 60, 80*GigaByte, 50, 80*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "a-123", "test1-a", 30, 60*GigaByte, 20, 40*GigaByte, true, false)

	// a-123 request[10,30]  request < min
	request := createResourceList(10, 30*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("a-123", request)
	runtime := gqm.RefreshRuntime("a-123")
	assert.Equal(t, request, runtime)
	runtime = gqm.RefreshRuntime("test1-a")
	assert.Equal(t, request, runtime)
	runtime = gqm.RefreshRuntime("test1")
	assert.Equal(t, request, runtime)

	// a-123 add request[15,15]  totalRequest[25,45] request > min
	request = createResourceList(15, 15*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("a-123", request)
	runtime = gqm.RefreshRuntime("a-123")
	assert.Equal(t, createResourceList(25, 45*GigaByte), runtime)
	quotaInfo := gqm.GetQuotaInfoByName("test1-a")
	assert.Equal(t, createResourceList(25, 45*GigaByte), quotaInfo.CalculateInfo.Request)
	quotaInfo = gqm.GetQuotaInfoByName("test1")
	assert.Equal(t, createResourceList(25, 45*GigaByte), quotaInfo.CalculateInfo.Request)

	// a-123 add request[30,30]  totalRequest[55,75] request > max
	request = createResourceList(30, 30*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("a-123", request)
	runtime = gqm.RefreshRuntime("a-123")
	assert.Equal(t, createResourceList(30, 60*GigaByte), runtime)
	quotaInfo = gqm.GetQuotaInfoByName("test1-a")
	assert.Equal(t, createResourceList(30, 60*GigaByte), quotaInfo.CalculateInfo.Request)
	quotaInfo = gqm.GetQuotaInfoByName("test1")
	assert.Equal(t, createResourceList(30, 60*GigaByte), quotaInfo.CalculateInfo.Request)
}

// TestGroupQuotaManager_MultiUpdateQuotaRequest_WithScaledMinQuota1 test scaledMinQuota when quotaGroup's sum of the minQuota
// is larger than totalRes; and enlarge the totalRes to test whether gqm can recover the oriMinQuota or not.
func TestGroupQuotaManager_MultiUpdateQuotaRequest_WithScaledMinQuota1(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.scaleMinQuotaEnabled = true

	AddQuotaToManager(t, gqm, "p", "root", 1000, 1000*GigaByte, 300, 300*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "a", "p", 1000, 1000*GigaByte, 100, 100*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "b", "p", 1000, 1000*GigaByte, 100, 100*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "c", "p", 1000, 1000*GigaByte, 100, 100*GigaByte, true, false)

	request := createResourceList(200, 200*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("a", request)
	gqm.updateGroupDeltaRequestNoLock("b", request)
	gqm.updateGroupDeltaRequestNoLock("c", request)

	deltaRes := createResourceList(200, 200*GigaByte)
	gqm.UpdateClusterTotalResource(deltaRes)

	runtime := gqm.RefreshRuntime("p")
	assert.Equal(t, createResourceList(200, 200*GigaByte), runtime)
	gqm.RefreshRuntime("a")
	gqm.RefreshRuntime("b")
	gqm.RefreshRuntime("c")
	runtime = gqm.RefreshRuntime("a")
	assert.Equal(t, createResourceList2(66667, 200*GigaByte/3+1), runtime)
	// after group "c" refreshRuntime, the runtime of "a" and "b" has changed
	assert.Equal(t, createResourceList2(66667, 200*GigaByte/3+1), gqm.RefreshRuntime("a"))
	assert.Equal(t, createResourceList2(66667, 200*GigaByte/3+1), gqm.RefreshRuntime("b"))

	quotaInfo := gqm.GetQuotaInfoByName("p")
	assert.Equal(t, createResourceList(200, 200*GigaByte), quotaInfo.CalculateInfo.AutoScaleMin)

	quotaInfo = gqm.GetQuotaInfoByName("a")
	assert.Equal(t, createResourceList2(66666, 200*GigaByte/3), quotaInfo.CalculateInfo.AutoScaleMin)

	quotaInfo = gqm.GetQuotaInfoByName("b")
	assert.Equal(t, createResourceList2(66666, 200*GigaByte/3), quotaInfo.CalculateInfo.AutoScaleMin)

	quotaInfo = gqm.GetQuotaInfoByName("c")
	assert.Equal(t, createResourceList2(66666, 200*GigaByte/3), quotaInfo.CalculateInfo.AutoScaleMin)

	// large
	deltaRes = createResourceList(400, 400*GigaByte)
	gqm.UpdateClusterTotalResource(deltaRes)

	runtime = gqm.RefreshRuntime("p")
	assert.Equal(t, createResourceList(600, 600*GigaByte), runtime)

	runtime = gqm.RefreshRuntime("a")
	assert.Equal(t, createResourceList(200, 200*GigaByte), runtime)

	runtime = gqm.RefreshRuntime("b")
	assert.Equal(t, createResourceList(200, 200*GigaByte), runtime)

	runtime = gqm.RefreshRuntime("c")
	assert.Equal(t, createResourceList(200, 200*GigaByte), runtime)

	quotaInfo = gqm.GetQuotaInfoByName("p")
	assert.Equal(t, createResourceList(300, 300*GigaByte), quotaInfo.CalculateInfo.AutoScaleMin)

	quotaInfo = gqm.GetQuotaInfoByName("a")
	assert.Equal(t, createResourceList(100, 100*GigaByte), quotaInfo.CalculateInfo.AutoScaleMin)

	quotaInfo = gqm.GetQuotaInfoByName("b")
	assert.Equal(t, createResourceList(100, 100*GigaByte), quotaInfo.CalculateInfo.AutoScaleMin)

	quotaInfo = gqm.GetQuotaInfoByName("c")
	assert.Equal(t, createResourceList(100, 100*GigaByte), quotaInfo.CalculateInfo.AutoScaleMin)
}

// TestGroupQuotaManager_MultiUpdateQuotaRequest_WithScaledMinQuota1 test scaledMinQuota when quotaGroup's sum of the
// minQuota is larger than totalRes, with one of the quotaGroup's request is zero.
func TestGroupQuotaManager_MultiUpdateQuotaRequest_WithScaledMinQuota2(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.scaleMinQuotaEnabled = true
	deltaRes := createResourceList(1, 1*GigaByte)
	gqm.UpdateClusterTotalResource(deltaRes)

	AddQuotaToManager(t, gqm, "p", "root", 1000, 1000*GigaByte, 300, 300*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "a", "p", 1000, 1000*GigaByte, 100, 100*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "b", "p", 1000, 1000*GigaByte, 100, 100*GigaByte, true, false)
	AddQuotaToManager(t, gqm, "c", "p", 1000, 1000*GigaByte, 100, 100*GigaByte, true, false)

	request := createResourceList(200, 200*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("a", request)
	gqm.updateGroupDeltaRequestNoLock("b", createResourceList(0, 0))
	gqm.updateGroupDeltaRequestNoLock("c", request)
	gqm.UpdateClusterTotalResource(createResourceList(199, 199*GigaByte))
	runtime := gqm.RefreshRuntime("p")
	assert.Equal(t, createResourceList(200, 200*GigaByte), runtime)

	gqm.RefreshRuntime("a")
	gqm.RefreshRuntime("b")
	gqm.RefreshRuntime("c")

	runtime = gqm.RefreshRuntime("a")
	assert.Equal(t, createResourceList(100, 100*GigaByte), runtime)

	runtime = gqm.RefreshRuntime("b")
	assert.Equal(t, createResourceList(0, 0*GigaByte), runtime)

	runtime = gqm.RefreshRuntime("c")
	assert.Equal(t, createResourceList(100, 100*GigaByte), runtime)

	quotaInfo := gqm.GetQuotaInfoByName("p")
	assert.Equal(t, createResourceList(200, 200*GigaByte), quotaInfo.CalculateInfo.AutoScaleMin)

	quotaInfo = gqm.GetQuotaInfoByName("a")
	assert.Equal(t, createResourceList2(66666, 200*GigaByte/3), quotaInfo.CalculateInfo.AutoScaleMin)

	quotaInfo = gqm.GetQuotaInfoByName("b")
	assert.Equal(t, createResourceList2(66666, 200*GigaByte/3), quotaInfo.CalculateInfo.AutoScaleMin)

	quotaInfo = gqm.GetQuotaInfoByName("c")
	assert.Equal(t, createResourceList2(66666, 200*GigaByte/3), quotaInfo.CalculateInfo.AutoScaleMin)
}

func TestGroupQuotaManager_MultiUpdateQuotaUsed(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	AddQuotaToManager(t, gqm, "test1", "root", 96, 160*GigaByte, 50, 80*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "test1-sub1", "test1", 96, 160*GigaByte, 50, 80*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "test1-sub1-1", "test1-sub1", 96, 160*GigaByte, 50, 80*GigaByte, true, false)

	used := createResourceList(120, 290*GigaByte)
	gqm.updateGroupDeltaUsedNoLock("test1-sub1", used)
	quotaInfo := gqm.GetQuotaInfoByName("test1-sub1")
	assert.True(t, quotaInfo != nil)
	assert.Equal(t, used, quotaInfo.CalculateInfo.Used)

	quotaInfo = gqm.GetQuotaInfoByName("test1-sub1")
	assert.True(t, quotaInfo != nil)
	assert.Equal(t, used, quotaInfo.CalculateInfo.Used)

	quotaInfo = gqm.GetQuotaInfoByName("test1")
	assert.True(t, quotaInfo != nil)
	assert.Equal(t, used, quotaInfo.CalculateInfo.Used)
}

func TestGroupQuotaManager_UpdateQuotaParentName(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	deltaRes := createResourceList(96, 160*GigaByte)
	gqm.UpdateClusterTotalResource(deltaRes)
	assert.Equal(t, deltaRes, gqm.totalResource)
	totalRes := gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource.DeepCopy()
	assert.Equal(t, deltaRes, totalRes)

	// test2 Max[96, 160]  Min[50,80] request[20,40]
	//   `-- test2-a Max[96, 160]  Min[50,80] request[20,40]
	// test1 Max[96, 160]  Min[50,80] request[60,100]
	//   `-- test1-a Max[96, 160]  Min[50,80] request[60,100]
	//         `-- a-123 Max[96, 160]  Min[50,80] request[60,100]
	AddQuotaToManager(t, gqm, "test1", "root", 96, 160*GigaByte, 100, 160*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "test1-a", "test1", 96, 160*GigaByte, 50, 80*GigaByte, true, true)
	changeQuota := AddQuotaToManager(t, gqm, "a-123", "test1-a", 96, 160*GigaByte, 50, 80*GigaByte, true, false)

	AddQuotaToManager(t, gqm, "test2", "root", 96, 160*GigaByte, 100, 160*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "test2-a", "test2", 96, 160*GigaByte, 50, 80*GigaByte, true, false)

	// a-123 request [60,100]
	request := createResourceList(60, 100*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("a-123", request)
	gqm.updateGroupDeltaUsedNoLock("a-123", request)
	runtime := gqm.RefreshRuntime("a-123")
	assert.Equal(t, request, runtime)

	runtime = gqm.RefreshRuntime("test1-a")
	assert.Equal(t, request, runtime)

	runtime = gqm.RefreshRuntime("test1")
	assert.Equal(t, request, runtime)

	// test2-a request [20,40]
	request = createResourceList(20, 40*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("test2-a", request)
	gqm.updateGroupDeltaUsedNoLock("test2-a", request)
	runtime = gqm.RefreshRuntime("test2-a")
	assert.Equal(t, request, runtime)

	runtime = gqm.RefreshRuntime("test2")
	assert.Equal(t, request, runtime)

	// a-123 mv test2
	// test2 Max[96, 160]  Min[100,160] request[80,140]
	//   `-- test2-a Max[96, 160]  Min[50,80] request[20,40]
	//   `-- a-123 Max[96, 160]  Min[50,80] request[60,100]
	// test1 Max[96, 160]  Min[100,160] request[0,0]
	//   `-- test1-a Max[96, 160]  Min[50,80] request[0,0]
	changeQuota.Labels[extension.LabelQuotaParent] = "test2"
	gqm.UpdateQuota(changeQuota, false)

	quotaInfo := gqm.GetQuotaInfoByName("test1-a")
	gqm.RefreshRuntime("test1-a")
	assert.Equal(t, v1.ResourceList{}, quotaInfo.CalculateInfo.Request)
	assert.Equal(t, v1.ResourceList{}, quotaInfo.CalculateInfo.Used)
	assert.Equal(t, createResourceList(0, 0), quotaInfo.CalculateInfo.Runtime)

	quotaInfo = gqm.GetQuotaInfoByName("test1")
	gqm.RefreshRuntime("test1")
	assert.Equal(t, v1.ResourceList{}, quotaInfo.CalculateInfo.Request)
	assert.Equal(t, v1.ResourceList{}, quotaInfo.CalculateInfo.Used)
	assert.Equal(t, createResourceList(0, 0), quotaInfo.CalculateInfo.Runtime)

	quotaInfo = gqm.GetQuotaInfoByName("a-123")
	gqm.RefreshRuntime("a-123")
	assert.Equal(t, createResourceList(60, 100*GigaByte), quotaInfo.CalculateInfo.Request)
	assert.Equal(t, createResourceList(60, 100*GigaByte), quotaInfo.CalculateInfo.Used)
	assert.Equal(t, createResourceList(60, 100*GigaByte), quotaInfo.CalculateInfo.Runtime)
	assert.Equal(t, "test2", quotaInfo.ParentName)

	quotaInfo = gqm.GetQuotaInfoByName("test2-a")
	gqm.RefreshRuntime("test2-a")
	assert.Equal(t, createResourceList(20, 40*GigaByte), quotaInfo.CalculateInfo.Request)
	assert.Equal(t, createResourceList(20, 40*GigaByte), quotaInfo.CalculateInfo.Used)
	assert.Equal(t, createResourceList(20, 40*GigaByte), quotaInfo.CalculateInfo.Runtime)

	quotaInfo = gqm.GetQuotaInfoByName("test2")
	gqm.RefreshRuntime("test2")
	assert.Equal(t, createResourceList(80, 140*GigaByte), quotaInfo.CalculateInfo.Request)
	assert.Equal(t, createResourceList(80, 140*GigaByte), quotaInfo.CalculateInfo.Used)
	assert.Equal(t, createResourceList(80, 140*GigaByte), quotaInfo.CalculateInfo.Runtime)
}

func TestGroupQuotaManager_UpdateClusterTotalResource(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	totalRes := createResourceList(96, 160*GigaByte)
	gqm.UpdateClusterTotalResource(totalRes)
	assert.Equal(t, totalRes, gqm.totalResource)
	assert.Equal(t, totalRes, gqm.totalResourceExceptSystemAndDefaultUsed)
	quotaTotalRes := gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource.DeepCopy()
	assert.Equal(t, totalRes, quotaTotalRes)

	totalRes = createResourceList(64, 360*GigaByte)
	gqm.UpdateClusterTotalResource(createResourceList(-32, 200*GigaByte))
	assert.Equal(t, totalRes, gqm.totalResource)
	assert.Equal(t, totalRes, gqm.totalResourceExceptSystemAndDefaultUsed)
	quotaTotalRes = gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource.DeepCopy()
	assert.Equal(t, totalRes, quotaTotalRes)

	totalRes = createResourceList(100, 540*GigaByte)
	gqm.UpdateClusterTotalResource(createResourceList(36, 180*GigaByte))
	assert.Equal(t, totalRes, gqm.totalResource)
	assert.Equal(t, totalRes, gqm.totalResourceExceptSystemAndDefaultUsed)
	quotaTotalRes = gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource.DeepCopy()
	assert.Equal(t, totalRes, quotaTotalRes)

	sysUsed := createResourceList(10, 30*GigaByte)
	gqm.updateGroupDeltaUsedNoLock(extension.SystemQuotaName, sysUsed)
	assert.Equal(t, sysUsed, gqm.GetQuotaInfoByName(extension.SystemQuotaName).GetUsed())

	// 90, 510
	delta := totalRes.DeepCopy()
	delta = quotav1.SubtractWithNonNegativeResult(delta, sysUsed)
	assert.Equal(t, totalRes, gqm.totalResource)
	assert.Equal(t, delta, gqm.totalResourceExceptSystemAndDefaultUsed)
	quotaTotalRes = gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource.DeepCopy()
	assert.Equal(t, delta, quotaTotalRes)

	// 80, 480
	gqm.updateGroupDeltaUsedNoLock(extension.SystemQuotaName, createResourceList(10, 30))
	delta = quotav1.Subtract(delta, createResourceList(10, 30))
	assert.Equal(t, totalRes, gqm.totalResource)
	assert.Equal(t, delta, gqm.totalResourceExceptSystemAndDefaultUsed)

	// 70, 450
	defaultUsed := createResourceList(10, 30)
	gqm.updateGroupDeltaUsedNoLock(extension.DefaultQuotaName, defaultUsed)
	assert.Equal(t, defaultUsed, gqm.GetQuotaInfoByName(extension.DefaultQuotaName).GetUsed())
	delta = quotav1.Subtract(delta, defaultUsed)
	assert.Equal(t, totalRes, gqm.totalResource)
	assert.Equal(t, delta, gqm.totalResourceExceptSystemAndDefaultUsed)

	// 60 420
	gqm.updateGroupDeltaUsedNoLock(extension.DefaultQuotaName, defaultUsed)
	delta = quotav1.Subtract(delta, defaultUsed)
	assert.Equal(t, totalRes, gqm.totalResource)
	assert.Equal(t, delta, gqm.totalResourceExceptSystemAndDefaultUsed)
}

func AddQuotaToManager(t *testing.T,
	gqm *GroupQuotaManager,
	quotaName string,
	parent string,
	maxCpu, maxMem, minCpu, minMem int64, allowLentResource bool, isParent bool) *v1alpha1.ElasticQuota {
	quota := CreateQuota(quotaName, parent, maxCpu, maxMem, minCpu, minMem, allowLentResource, isParent)
	err := gqm.UpdateQuota(quota, false)
	assert.Nil(t, err)
	quotaInfo := gqm.quotaInfoMap[quotaName]
	assert.True(t, quotaInfo != nil)
	assert.Equal(t, quotaName, quotaInfo.Name)
	assert.Equal(t, quotaInfo.CalculateInfo.Max, createResourceList(maxCpu, maxMem))
	assert.Equal(t, quotaInfo.CalculateInfo.Min, createResourceList(minCpu, minMem))
	assert.Equal(t, quotaInfo.CalculateInfo.SharedWeight.Memory().Value(), maxMem)
	assert.Equal(t, quotaInfo.CalculateInfo.SharedWeight.Cpu().Value(), maxCpu)

	// assert whether the parent quotaTree has the sub quota group
	quotaTreeWrapper := gqm.runtimeQuotaCalculatorMap[quotaName]
	assert.NotNil(t, quotaTreeWrapper)
	quotaTreeWrapper = gqm.runtimeQuotaCalculatorMap[parent]
	assert.NotNil(t, quotaTreeWrapper)
	find, nodeInfo := quotaTreeWrapper.quotaTree[v1.ResourceCPU].find(quotaName)
	assert.True(t, find)
	assert.Equal(t, nodeInfo.quotaName, quotaName)
	return quota
}

func CreateQuota(quotaName string, parent string, maxCpu, maxMem, minCpu, minMem int64, allowLentResource bool, isParent bool) *v1alpha1.ElasticQuota {
	quota := &v1alpha1.ElasticQuota{
		ObjectMeta: metav1.ObjectMeta{
			Name:        quotaName,
			Annotations: make(map[string]string),
			Labels:      make(map[string]string),
		},
		Spec: v1alpha1.ElasticQuotaSpec{
			Max: createResourceList(maxCpu, maxMem),
			Min: createResourceList(minCpu, minMem),
		},
	}

	quota.Annotations[extension.AnnotationSharedWeight] = fmt.Sprintf("{\"cpu\":%v, \"memory\":\"%v\"}", maxCpu, maxMem)
	quota.Labels[extension.LabelQuotaParent] = parent
	if allowLentResource {
		quota.Labels[extension.LabelAllowLentResource] = "true"
	} else {
		quota.Labels[extension.LabelAllowLentResource] = "false"
	}

	if isParent {
		quota.Labels[extension.LabelQuotaIsParent] = "true"
	} else {
		quota.Labels[extension.LabelQuotaIsParent] = "false"
	}
	return quota
}

func TestGroupQuotaManager_MultiChildMaxGreaterParentMax_MaxGreaterThanTotalRes(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	deltaRes := createResourceList(300, 8000)
	gqm.UpdateClusterTotalResource(deltaRes)

	// test1 Max[600, 4096]  Min[100, 100]
	//   |-- test1-sub1 Max[500, 2048]  Min[100, 100]  Req[500, 4096]
	AddQuotaToManager(t, gqm, "test1", "root", 600, 4096, 100, 100, true, true)
	AddQuotaToManager(t, gqm, "test1-sub1", "test1", 500, 2048, 100, 100, true, false)

	// test1 Request [500, 4096] limitRequest [500, 2048]
	// test1-sub Request [500,2048] limitedRequest [500, 2048] limited by rootRes [300, 8000] -> [300,2048]
	request := createResourceList(500, 4096)
	gqm.updateGroupDeltaRequestNoLock("test1-sub1", request)
	runtime := gqm.RefreshRuntime("test1-sub1")
	assert.Equal(t, createResourceList(300, 2048), runtime)
	fmt.Printf("quota1 runtime:%v\n", runtime)

	// test1 Request [1050, 8192] limitRequest [500, 2048]
	// test1-sub1 Request [500,2048] limitedRequest [500, 2048] limited by rootRes [300, 8000] -> [300,2048]
	request = createResourceList(550, 4096)
	gqm.updateGroupDeltaRequestNoLock("test1-sub1", request)
	runtime = gqm.RefreshRuntime("test1-sub")
	fmt.Printf("quota1 runtime:%v\n", runtime)

	quotaInfo := gqm.GetQuotaInfoByName("test1")
	assert.Equal(t, quotaInfo.CalculateInfo.Request, createResourceList(500, 2048))
	assert.Equal(t, quotaInfo.getLimitRequestNoLock(), createResourceList(500, 2048))
	assert.Equal(t, quotaInfo.CalculateInfo.Max, createResourceList(600, 4096))
	assert.Equal(t, quotaInfo.CalculateInfo.Runtime, createResourceList(300, 2048))
	quotaInfo = gqm.GetQuotaInfoByName("test1-sub1")
	assert.Equal(t, quotaInfo.CalculateInfo.Request, createResourceList(1050, 8192))
	assert.Equal(t, quotaInfo.getLimitRequestNoLock(), createResourceList(500, 2048))
	assert.Equal(t, quotaInfo.CalculateInfo.Max, createResourceList(500, 2048))
	assert.Equal(t, quotaInfo.CalculateInfo.Runtime, createResourceList(300, 2048))
}

func TestGroupQuotaManager_MultiChildMaxGreaterParentMax(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	deltaRes := createResourceList(350, 1800*GigaByte)
	gqm.UpdateClusterTotalResource(deltaRes)

	// test1 Max[300, 1024]  Min[176, 756]
	//   |-- test1-sub1 Max[500, 2048]  Min[100, 512]
	AddQuotaToManager(t, gqm, "test1", "root", 300, 1024*GigaByte, 176, 756*GigaByte, true, true)
	AddQuotaToManager(t, gqm, "test1-sub1", "test1", 500, 2048*GigaByte, 100, 512*GigaByte, true, false)

	// test1 max < request < test1-sub1 max
	// test1-sub1 Request[400, 1500] limitedRequest [400, 1500]
	// test1 Request [400,1500] limitedRequest [300, 1024]
	request := createResourceList(400, 1500*GigaByte)
	gqm.updateGroupDeltaRequestNoLock("test1-sub1", request)
	quotaInfo := gqm.GetQuotaInfoByName("test1")
	assert.Equal(t, quotaInfo.CalculateInfo.Request, createResourceList(400, 1500*GigaByte))
	quotaInfo = gqm.GetQuotaInfoByName("test1-sub1")
	assert.Equal(t, quotaInfo.CalculateInfo.Request, createResourceList(400, 1500*GigaByte))
	runtime := gqm.RefreshRuntime("test1-sub1")
	assert.Equal(t, createResourceList(300, 1024*GigaByte), runtime)
	fmt.Printf("quota1 runtime:%v\n", runtime)

	// test1 max < test1-sub1 max < request
	// test1-sub1 Request[800, 3000] limitedRequest [500, 2048]
	// test1 Request [500, 2048] limitedRequest [300, 1024]
	gqm.updateGroupDeltaRequestNoLock("test1-sub1", request)
	runtime = gqm.RefreshRuntime("test1-sub1")
	assert.Equal(t, createResourceList(300, 1024*GigaByte), runtime)
	fmt.Printf("quota1 runtime:%v\n", runtime)
}

func TestGroupQuotaManager_UpdateQuotaTreeDimension_UpdateQuota(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	info3 := CreateQuota("3", extension.RootQuotaName, 1000, 10000, 100, 1000, true, false)
	info3.Spec.Max["tmp"] = *resource.NewQuantity(1, resource.DecimalSI)
	res := createResourceList(1000, 10000)
	gqm.UpdateClusterTotalResource(res)
	err := gqm.UpdateQuota(info3, false)
	if err != nil {
		klog.Infof("%v", err)
	}
	assert.Equal(t, len(gqm.resourceKeys), 3)
}

func createQuota(name, parent string, cpuMax, memMax, cpuMin, memMin int64) *v1alpha1.ElasticQuota {
	eq := CreateQuota(name, parent, cpuMax, memMax, cpuMin, memMin, true, false)
	return eq
}

func TestGroupQuotaManager_RefreshAndGetRuntimeQuota_UpdateQuota(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()

	gqm.scaleMinQuotaEnabled = true
	cluRes := createResourceList(50, 50)
	gqm.UpdateClusterTotalResource(cluRes)

	qi1 := createQuota("1", extension.RootQuotaName, 40, 40, 10, 10)
	qi2 := createQuota("2", extension.RootQuotaName, 40, 40, 0, 0)

	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)

	assert.Equal(t, len(gqm.resourceKeys), 2)
	// case1: there is no request, runtime quota is zero
	runtime := gqm.RefreshRuntime("1")
	assert.Equal(t, runtime, createResourceList(0, 0))

	// case2: no existed group
	assert.Nil(t, gqm.RefreshRuntime("5"))
	gqm.updateGroupDeltaRequestNoLock("1", createResourceList(5, 5))
	gqm.updateGroupDeltaRequestNoLock("2", createResourceList(5, 5))
	gq1 := gqm.GetQuotaInfoByName("1")
	gq2 := gqm.GetQuotaInfoByName("2")

	// case3: version is same, should not update
	gq1.RuntimeVersion = gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].globalRuntimeVersion
	gq2.RuntimeVersion = gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].globalRuntimeVersion
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(0, 0))
	assert.Equal(t, gqm.RefreshRuntime("2"), v1.ResourceList{})

	// case4: version is different, should update
	gq1.RuntimeVersion = 0
	gq2.RuntimeVersion = 0
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(5, 5))
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(5, 5))

	// case5: request is larger than min
	gqm.updateGroupDeltaRequestNoLock("1", createResourceList(25, 25))
	gqm.updateGroupDeltaRequestNoLock("2", createResourceList(25, 25))
	// 1 min [10,10] -> toPartitionRes [40,40] -> runtime [30,30]
	// 2 min [0,0]	-> toPartitionRes [40,40] -> runtime [20,20]
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(30, 30))
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(20, 20))
}

func TestGroupQuotaManager_UpdateSharedWeight_UpdateQuota(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.scaleMinQuotaEnabled = true

	gqm.UpdateClusterTotalResource(createResourceList(60, 60))

	// case1: if not config SharedWeight, equal to maxQuota
	qi1 := createQuota("1", extension.RootQuotaName, 40, 40, 10, 10)
	qi2 := createQuota("2", extension.RootQuotaName, 40, 40, 0, 0)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)

	assert.Equal(t, gqm.quotaInfoMap["1"].CalculateInfo.SharedWeight.Cpu().Value(), int64(40))
	assert.Equal(t, gqm.quotaInfoMap["2"].CalculateInfo.SharedWeight.Cpu().Value(), int64(40))

	// case2: if config SharedWeight
	sharedWeight1, _ := json.Marshal(createResourceList(30, 30))
	qi1.Annotations[extension.AnnotationSharedWeight] = string(sharedWeight1)
	sharedWeight2, _ := json.Marshal(createResourceList(10, 10))
	qi2.Annotations[extension.AnnotationSharedWeight] = string(sharedWeight2)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)
	result1, _ := json.Marshal(gqm.quotaInfoMap["1"].CalculateInfo.SharedWeight)
	result2, _ := json.Marshal(gqm.quotaInfoMap["2"].CalculateInfo.SharedWeight)
	assert.Equal(t, result1, sharedWeight1)
	assert.Equal(t, result2, sharedWeight2)
}

func TestGroupQuotaManager_UpdateOneGroupMaxQuota_UpdateQuota(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.scaleMinQuotaEnabled = true

	gqm.UpdateClusterTotalResource(createResourceList(50, 50))

	qi1 := createQuota("1", extension.RootQuotaName, 40, 40, 10, 10)
	qi2 := createQuota("2", extension.RootQuotaName, 40, 40, 10, 10)

	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)

	// case1: min < req < max
	gqm.updateGroupDeltaRequestNoLock("1", createResourceList(35, 35))
	gqm.updateGroupDeltaRequestNoLock("2", createResourceList(35, 35))
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(25, 25))
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(25, 25))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["1"], createResourceList(35, 35))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["2"], createResourceList(35, 35))

	// case2: decrease max, min < max < request
	qi1.Spec.Max = createResourceList(30, 30)
	qi2.Spec.Max = createResourceList(30, 30)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(25, 25))
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(25, 25))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["1"], createResourceList(30, 30))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["2"], createResourceList(30, 30))

	// case3: increase max, min < req < max
	qi1.Spec.Max = createResourceList(50, 50)
	qi2.Spec.Max = createResourceList(50, 50)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(25, 25))
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(25, 25))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["1"], createResourceList(35, 35))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["2"], createResourceList(35, 35))
}

func TestGroupQuotaManager_UpdateOneGroupMinQuota_UpdateQuota(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.scaleMinQuotaEnabled = true

	gqm.UpdateClusterTotalResource(createResourceList(50, 50))

	qi1 := createQuota("1", extension.RootQuotaName, 40, 40, 10, 10)
	qi2 := createQuota("2", extension.RootQuotaName, 40, 40, 10, 10)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)

	// case1:  min < req < max
	gqm.updateGroupDeltaRequestNoLock("1", createResourceList(15, 15))
	gqm.updateGroupDeltaRequestNoLock("2", createResourceList(15, 15))
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(15, 15))
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(15, 15))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource, createResourceList(50, 50))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["1"], createResourceList(15, 15))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["2"], createResourceList(15, 15))

	// case2: increase min, req = min
	qi1.Spec.Min = createResourceList(15, 15)
	qi2.Spec.Min = createResourceList(15, 15)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(15, 15))
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(15, 15))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource, createResourceList(50, 50))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["1"], createResourceList(15, 15))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["2"], createResourceList(15, 15))

	// case3: increase min, req < min
	qi1.Spec.Min = createResourceList(20, 20)
	qi2.Spec.Min = createResourceList(20, 20)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(15, 15))
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(15, 15))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource, createResourceList(50, 50))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["1"], createResourceList(15, 15))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["2"], createResourceList(15, 15))

	// case4: decrease min, min < req < max
	qi1.Spec.Min = createResourceList(5, 5)
	qi2.Spec.Min = createResourceList(5, 5)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(15, 15))
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(15, 15))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource, createResourceList(50, 50))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["1"], createResourceList(15, 15))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["2"], createResourceList(15, 15))

	// case5: update min, min == max, request > max
	qi1.Spec.Min = createResourceList(40, 40)
	qi2.Spec.Min = createResourceList(5, 5)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)
	gqm.updateGroupDeltaRequestNoLock("1", createResourceList(100, 100))
	gqm.updateGroupDeltaRequestNoLock("2", createResourceList(100, 100))
	assert.Equal(t, gqm.RefreshRuntime("1"), createResourceList(40, 40))
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(10, 10))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource, createResourceList(50, 50))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["1"], createResourceList(40, 40))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].groupReqLimit["2"], createResourceList(40, 40))
}

func TestGroupQuotaManager_DeleteOneGroup_UpdateQuota(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.scaleMinQuotaEnabled = true

	gqm.UpdateClusterTotalResource(createResourceList(50, 50))

	qi1 := createQuota("1", extension.RootQuotaName, 40, 40, 10, 10)
	q1 := CreateQuota("1", extension.RootQuotaName, 40, 40, 10, 10, true, false)
	qi2 := createQuota("2", extension.RootQuotaName, 40, 40, 10, 10)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)
	gqm.updateGroupDeltaRequestNoLock("1", createResourceList(15, 15))
	gqm.updateGroupDeltaRequestNoLock("2", createResourceList(15, 15))

	// delete one group
	gqm.UpdateQuota(q1, true)
	assert.Equal(t, gqm.RefreshRuntime("2"), createResourceList(15, 15))
	assert.Equal(t, gqm.runtimeQuotaCalculatorMap[extension.RootQuotaName].totalResource, createResourceList(50, 50))
	assert.Nil(t, gqm.quotaInfoMap["1"])
}

func NewGroupQuotaManager4Test() *GroupQuotaManager {
	quotaManager := &GroupQuotaManager{
		totalResourceExceptSystemAndDefaultUsed: v1.ResourceList{},
		totalResource:                           v1.ResourceList{},
		resourceKeys:                            make(map[v1.ResourceName]struct{}),
		quotaInfoMap:                            make(map[string]*QuotaInfo),
		runtimeQuotaCalculatorMap:               make(map[string]*RuntimeQuotaCalculator),
		scaleMinQuotaManager:                    NewScaleMinQuotaManager(),
		quotaTopoNodeMap:                        make(map[string]*QuotaTopoNode),
	}
	quotaManager.quotaInfoMap[extension.SystemQuotaName] = NewQuotaInfo(false, true, extension.SystemQuotaName, "")
	quotaManager.quotaInfoMap[extension.DefaultQuotaName] = NewQuotaInfo(false, true, extension.DefaultQuotaName, "")
	quotaManager.runtimeQuotaCalculatorMap[extension.RootQuotaName] = NewRuntimeQuotaCalculator(extension.RootQuotaName)
	return quotaManager
}

func BenchmarkGroupQuotaManager_RefreshRuntime(b *testing.B) {
	b.StopTimer()
	gqm := NewGroupQuotaManager4Test()
	random := rand.New(rand.NewSource(time.Now().UnixNano()))
	for i := 0; i < 2000; i++ {
		AddQuotaToManager2(gqm, fmt.Sprintf("%v", i), extension.RootQuotaName, 96, 160*GigaByte, 0, 0, true, false)
	}

	totalReqMem, totalReqCpu := float64(0), float64(0)
	for j := 0; j < 2000; j++ {
		reqMem := int64(random.Int() % 2000)
		reqCpu := int64(random.Int() % 2000)
		request := createResourceList(reqCpu, reqMem)
		totalReqMem += float64(reqMem)
		totalReqCpu += float64(reqCpu)
		gqm.updateGroupDeltaRequestNoLock(fmt.Sprintf("%v", j), request)
	}
	totalRes := createResourceList(int64(totalReqCpu/1.5), int64(totalReqMem/1.5))
	gqm.UpdateClusterTotalResource(totalRes)
	b.StartTimer()
	for i := 0; i < b.N; i++ {
		gqm.RefreshRuntime("0")
		gqm.getQuotaInfoByNameNoLock("0").RuntimeVersion = 0
	}
}

func AddQuotaToManager2(gqm *GroupQuotaManager, quotaName string, parent string,
	maxCpu, maxMem, minCpu, minMem int64, allowLentResource bool, isParent bool) *v1alpha1.ElasticQuota {
	quota := CreateQuota(quotaName, parent, maxCpu, maxMem, minCpu, minMem, allowLentResource, isParent)
	gqm.UpdateQuota(quota, false)
	return quota
}

func TestGroupQuotaManager_GetAllQuotaNames(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.scaleMinQuotaEnabled = true

	gqm.UpdateClusterTotalResource(createResourceList(50, 50))

	qi1 := createQuota("1", extension.RootQuotaName, 40, 40, 10, 10)
	qi2 := createQuota("2", extension.RootQuotaName, 40, 40, 10, 10)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)
	quotaNames := gqm.GetAllQuotaNames()

	assert.NotNil(t, quotaNames["1"])
	assert.NotNil(t, quotaNames["2"])
}

func TestGroupQuotaManager_UpdatePodCache_UpdatePodIsAssigned_GetPodIsAssigned_UpdatePodRequest_UpdatePodUsed(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	gqm.scaleMinQuotaEnabled = true

	gqm.UpdateClusterTotalResource(createResourceList(50, 50))

	qi1 := createQuota("1", extension.RootQuotaName, 40, 40, 10, 10)
	qi2 := createQuota("2", extension.RootQuotaName, 40, 40, 10, 10)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateQuota(qi2, false)

	pod1 := schetesting.MakePod().Name("1").Obj()
	pod1.Spec.Containers = []v1.Container{
		{
			Resources: v1.ResourceRequirements{
				Requests: createResourceList(10, 10),
			},
		},
	}
	gqm.updatePodCacheNoLock("1", pod1, true)
	assert.Equal(t, 1, len(gqm.GetQuotaInfoByName("1").GetPodCache()))
	gqm.updatePodCacheNoLock("1", pod1, false)
	assert.Equal(t, 0, len(gqm.GetQuotaInfoByName("1").GetPodCache()))
	gqm.updatePodCacheNoLock("2", pod1, true)
	assert.False(t, gqm.getPodIsAssignedNoLock("2", pod1))
	gqm.updatePodIsAssignedNoLock("2", pod1, true)
	assert.True(t, gqm.getPodIsAssignedNoLock("2", pod1))

	gqm.updatePodRequestNoLock("1", nil, pod1)
	assert.Equal(t, createResourceList(10, 10), gqm.GetQuotaInfoByName("1").GetRequest())
	gqm.updatePodUsedNoLock("2", nil, pod1)
	assert.Equal(t, createResourceList(10, 10), gqm.GetQuotaInfoByName("2").GetUsed())
	gqm.updatePodRequestNoLock("1", pod1, nil)
	assert.Equal(t, createResourceList(0, 0), gqm.GetQuotaInfoByName("1").GetRequest())
	gqm.updatePodUsedNoLock("2", pod1, nil)
	assert.Equal(t, createResourceList(0, 0), gqm.GetQuotaInfoByName("2").GetUsed())
}

func TestNewGroupQuotaManager(t *testing.T) {
	gqm := NewGroupQuotaManager(createResourceList(100, 100), createResourceList(300, 300))
	assert.Equal(t, createResourceList(100, 100), gqm.GetQuotaInfoByName("system").getMax())
	assert.Equal(t, createResourceList(300, 300), gqm.GetQuotaInfoByName("default").getMax())
	assert.True(t, gqm.scaleMinQuotaEnabled)
	gqm.UpdateClusterTotalResource(createResourceList(500, 500))
	assert.Equal(t, createResourceList(500, 500), gqm.GetClusterTotalResource())
}

func TestGroupQuotaManager_GetQuotaInformationForSyncHandler(t *testing.T) {
	gqm := NewGroupQuotaManager4Test()
	qi1 := createQuota("1", extension.RootQuotaName, 400, 400, 10, 10)
	gqm.UpdateQuota(qi1, false)
	gqm.UpdateClusterTotalResource(createResourceList(1000, 1000))
	gqm.updateGroupDeltaRequestNoLock("1", createResourceList(100, 100))
	gqm.RefreshRuntime("1")
	gqm.updateGroupDeltaUsedNoLock("1", createResourceList(10, 10))
	used, request, runtime, err := gqm.GetQuotaInformationForSyncHandler("1")
	assert.Nil(t, err)
	assert.Equal(t, used, createResourceList(10, 10))
	assert.Equal(t, request, createResourceList(100, 100))
	assert.Equal(t, runtime, createResourceList(100, 100))
}

func TestGetPodName(t *testing.T) {
	pod1 := schetesting.MakePod().Name("1").Obj()
	assert.Equal(t, pod1.Name, getPodName(pod1, nil))
	assert.Equal(t, pod1.Name, getPodName(nil, pod1))
}
