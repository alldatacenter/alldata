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
	"strconv"
	"sync"
	"time"

	v1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling/util"
)

var (
	timeNowFn = time.Now
)

const (
	GangFromPodGroupCrd       string = "GangFromPodGroupCrd"
	GangFromPodAnnotation     string = "GangFromPodAnnotation"
	PodGroupFromPodAnnotation string = "PodGroupFromPodAnnotation"
)

// Gang  basic podGroup info recorded in gangCache:
type Gang struct {
	Name       string
	WaitTime   time.Duration
	CreateTime time.Time

	// strict-mode or non-strict-mode
	Mode              string
	MinRequiredNumber int
	TotalChildrenNum  int
	GangGroupId       string
	GangGroup         []string
	Children          map[string]*v1.Pod
	// pods that have already assumed(waiting in Permit stage)
	WaitingForBindChildren map[string]*v1.Pod
	// pods that have already bound
	BoundChildren map[string]*v1.Pod
	// OnceResourceSatisfied indicates whether the gang has ever reached the ResourceSatisfied state，which means the
	// children number has reached the minNum in the early step,
	// once this variable is set true, it is irreversible.
	OnceResourceSatisfied bool

	// if the podGroup should be passed at PreFilter stage(Strict-Mode)
	ScheduleCycleValid bool
	// these fields used to count the cycle
	// For example, at the beginning, `scheduleCycle` is 1, and each pod's cycle in `childrenScheduleRoundMap` is 0. When each pod comes to PreFilter,
	// we will check if the pod's value in `childrenScheduleRoundMap` is smaller than Gang's `scheduleCycle`, If result is positive,
	// we set the pod's cycle in `childrenScheduleRoundMap` equal with `scheduleCycle` and pass the check. If result is negative, means
	// the pod has been scheduled in this cycle, so we should reject it. With `totalChildrenNum`'s help, when the last pod comes to make all
	// `childrenScheduleRoundMap`'s values equal to `scheduleCycle`, Gang's `scheduleCycle` will be added by 1, which means a new schedule cycle.
	ScheduleCycle            int
	ChildrenScheduleRoundMap map[string]int

	GangFrom    string
	HasGangInit bool

	lock sync.Mutex
}

func NewGang(gangName string) *Gang {
	return &Gang{
		Name:                     gangName,
		CreateTime:               timeNowFn(),
		WaitTime:                 0,
		GangGroupId:              gangName,
		GangGroup:                []string{gangName},
		Mode:                     extension.GangModeStrict,
		Children:                 make(map[string]*v1.Pod),
		WaitingForBindChildren:   make(map[string]*v1.Pod),
		BoundChildren:            make(map[string]*v1.Pod),
		ScheduleCycleValid:       true,
		ScheduleCycle:            1,
		ChildrenScheduleRoundMap: make(map[string]int),
		GangFrom:                 GangFromPodAnnotation,
		HasGangInit:              false,
	}
}

func (gang *Gang) tryInitByPodConfig(pod *v1.Pod, args *config.CoschedulingArgs) bool {
	gang.lock.Lock()
	defer gang.lock.Unlock()
	if gang.HasGangInit {
		return false
	}
	minRequiredNumber, err := extension.GetMinNum(pod)
	if err != nil {
		klog.Errorf("pod's annotation MinRequiredNumber illegal, gangName: %v, value: %v",
			gang.Name, pod.Annotations[extension.AnnotationGangMinNum])
		return false
	}
	gang.MinRequiredNumber = minRequiredNumber

	totalChildrenNum, err := strconv.ParseInt(pod.Annotations[extension.AnnotationGangTotalNum], 10, 32)
	if err != nil {
		klog.Errorf("pod's annotation totalNumber illegal, gangName: %v, value: %v",
			gang.Name, pod.Annotations[extension.AnnotationGangTotalNum])
		totalChildrenNum = int64(minRequiredNumber)
	} else if totalChildrenNum != 0 && totalChildrenNum < int64(minRequiredNumber) {
		klog.Errorf("pod's annotation totalNumber cannot less than minRequiredNumber, gangName: %v, totalNumber: %v,minRequiredNumber: %v",
			gang.Name, pod.Annotations[extension.AnnotationGangTotalNum], minRequiredNumber)
		totalChildrenNum = int64(minRequiredNumber)
	}
	gang.TotalChildrenNum = int(totalChildrenNum)

	mode := pod.Annotations[extension.AnnotationGangMode]
	if mode != extension.GangModeStrict && mode != extension.GangModeNonStrict {
		klog.Errorf("pod's annotation GangModeAnnotation illegal, gangName: %v, value: %v",
			gang.Name, pod.Annotations[extension.AnnotationGangMode])
		mode = extension.GangModeStrict
	}
	gang.Mode = mode

	// here we assume that Coscheduling's CreateTime equal with the pod's CreateTime
	gang.CreateTime = pod.CreationTimestamp.Time

	waitTime, err := time.ParseDuration(pod.Annotations[extension.AnnotationGangWaitTime])
	if err != nil || waitTime <= 0 {
		klog.Errorf("pod's annotation GangWaitTimeAnnotation illegal, gangName: %v, value: %v",
			gang.Name, pod.Annotations[extension.AnnotationGangWaitTime])
		if args.DefaultTimeout != nil {
			waitTime = args.DefaultTimeout.Duration
		} else {
			klog.Errorf("gangArgs DefaultTimeoutSeconds is nil")
			waitTime = 0
		}
	}
	gang.WaitTime = waitTime

	groupSlice, err := util.StringToGangGroupSlice(pod.Annotations[extension.AnnotationGangGroups])
	if err != nil {
		klog.Errorf("pod's annotation GangGroupsAnnotation illegal, gangName: %v, value: %v",
			gang.Name, pod.Annotations[extension.AnnotationGangGroups])
	}
	if len(groupSlice) == 0 {
		groupSlice = append(groupSlice, gang.Name)
	}
	gang.GangGroup = groupSlice
	gang.GangGroupId = util.GetGangGroupId(groupSlice)
	gang.GangFrom = GangFromPodAnnotation

	gang.HasGangInit = true

	klog.Infof("TryInitByPodConfig done, gangName: %v, minRequiredNumber: %v, totalChildrenNum: %v, "+
		"mode: %v, waitTime: %v, groupSlice: %v", gang.Name, gang.MinRequiredNumber, gang.TotalChildrenNum,
		gang.Mode, gang.WaitTime, gang.GangGroup)
	return true
}

func (gang *Gang) tryInitByPodGroup(pg *v1alpha1.PodGroup, args *config.CoschedulingArgs) {
	gang.lock.Lock()
	defer gang.lock.Unlock()
	if gang.HasGangInit {
		return
	}
	minRequiredNumber := pg.Spec.MinMember
	gang.MinRequiredNumber = int(minRequiredNumber)

	totalChildrenNum, err := strconv.ParseInt(pg.Annotations[extension.AnnotationGangTotalNum], 10, 32)
	if err != nil {
		klog.Errorf("podGroup's annotation totalNumber illegal, gangName: %v, value: %v",
			gang.Name, pg.Annotations[extension.AnnotationGangTotalNum])
		totalChildrenNum = int64(minRequiredNumber)
	} else if totalChildrenNum != 0 && totalChildrenNum < int64(minRequiredNumber) {
		klog.Errorf("podGroup's annotation totalNumber cannot less than minRequiredNumber, gangName:%v, totalNumber: %v,minRequiredNumber: %v",
			gang.Name, pg.Annotations[extension.AnnotationGangTotalNum], minRequiredNumber)
		totalChildrenNum = int64(minRequiredNumber)
	}
	gang.TotalChildrenNum = int(totalChildrenNum)

	mode := pg.Annotations[extension.AnnotationGangMode]
	if mode != extension.GangModeStrict && mode != extension.GangModeNonStrict {
		klog.Errorf("podGroup's annotation GangModeAnnotation illegal, gangName: %v, value: %v",
			gang.Name, pg.Annotations[extension.AnnotationGangMode])
		mode = extension.GangModeStrict
	}
	gang.Mode = mode

	// here we assume that Coscheduling's CreateTime equal with the podGroup CRD CreateTime
	gang.CreateTime = pg.CreationTimestamp.Time

	waitTime, err := util.ParsePgTimeoutSeconds(*pg.Spec.ScheduleTimeoutSeconds)
	if err != nil {
		klog.Errorf("podGroup's ScheduleTimeoutSeconds illegal, gangName: %v, value: %v",
			gang.Name, pg.Spec.ScheduleTimeoutSeconds)
		if args.DefaultTimeout != nil {
			waitTime = args.DefaultTimeout.Duration
		} else {
			klog.Errorf("gangArgs DefaultTimeoutSeconds is nil")
			waitTime = 0
		}
	}
	gang.WaitTime = waitTime

	groupSlice, err := util.StringToGangGroupSlice(pg.Annotations[extension.AnnotationGangGroups])
	if err != nil {
		klog.Errorf("podGroup's annotation GangGroupsAnnotation illegal, gangName: %v, value: %v",
			gang.Name, pg.Annotations[extension.AnnotationGangGroups])
	}
	if len(groupSlice) == 0 {
		groupSlice = append(groupSlice, gang.Name)
	}
	gang.GangGroup = groupSlice
	gang.GangGroupId = util.GetGangGroupId(groupSlice)

	gang.GangFrom = GangFromPodGroupCrd

	gang.HasGangInit = true

	klog.Infof("TryInitByPodGroup done, gangName: %v, minRequiredNumber: %v, totalChildrenNum: %v, "+
		"mode: %v, waitTime: %v, groupSlice: %v", gang.Name, gang.MinRequiredNumber, gang.TotalChildrenNum,
		gang.Mode, gang.WaitTime, gang.GangGroup)
}

func (gang *Gang) deletePod(pod *v1.Pod) bool {
	if pod == nil {
		return false
	}

	gang.lock.Lock()
	defer gang.lock.Unlock()

	podId := util.GetId(pod.Namespace, pod.Name)
	klog.Infof("Delete pod from gang: %v, podName: %v", gang.Name, podId)

	delete(gang.Children, podId)
	delete(gang.WaitingForBindChildren, podId)
	delete(gang.BoundChildren, podId)
	delete(gang.ChildrenScheduleRoundMap, podId)
	if gang.GangFrom == GangFromPodAnnotation {
		if len(gang.Children) == 0 {
			return true
		}
	}
	return false
}

func (gang *Gang) getGangWaitTime() time.Duration {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return gang.WaitTime
}

func (gang *Gang) getChildrenNum() int {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return len(gang.Children)
}

func (gang *Gang) getGangMinNum() int {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return gang.MinRequiredNumber
}

func (gang *Gang) getGangTotalNum() int {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return gang.TotalChildrenNum
}

func (gang *Gang) getBoundPodNum() int {
	gang.lock.Lock()
	defer gang.lock.Unlock()
	return len(gang.BoundChildren)
}

func (gang *Gang) getGangMode() string {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return gang.Mode
}

func (gang *Gang) getGangAssumedPods() int {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return len(gang.WaitingForBindChildren) + len(gang.BoundChildren)
}

func (gang *Gang) getScheduleCycle() int {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return gang.ScheduleCycle
}

func (gang *Gang) getChildScheduleCycle(pod *v1.Pod) int {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	podId := util.GetId(pod.Namespace, pod.Name)
	return gang.ChildrenScheduleRoundMap[podId]
}

func (gang *Gang) getCreateTime() time.Time {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return gang.CreateTime
}

func (gang *Gang) getGangGroup() []string {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return gang.GangGroup
}

func (gang *Gang) isGangOnceResourceSatisfied() bool {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return gang.OnceResourceSatisfied
}

func (gang *Gang) isScheduleCycleValid() bool {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	return gang.ScheduleCycleValid
}

func (gang *Gang) setChild(pod *v1.Pod) {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	podId := util.GetId(pod.Namespace, pod.Name)
	if _, ok := gang.Children[podId]; !ok {
		gang.Children[podId] = pod
		klog.Infof("SetChild, gangName: %v, childName: %v", gang.Name, podId)
	}
}

func (gang *Gang) setScheduleCycleValid(valid bool) {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	gang.ScheduleCycleValid = valid
	klog.Infof("SetScheduleCycleValid, gangName: %v, valid: %v", gang.Name, valid)
}

func (gang *Gang) setChildScheduleCycle(pod *v1.Pod, childCycle int) {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	podId := util.GetId(pod.Namespace, pod.Name)
	gang.ChildrenScheduleRoundMap[podId] = childCycle
	klog.Infof("setChildScheduleCycle, pod: %v, childCycle: %v", podId, childCycle)
}

func (gang *Gang) trySetScheduleCycleValid() {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	num := 0
	for _, childScheduleCycle := range gang.ChildrenScheduleRoundMap {
		if childScheduleCycle == gang.ScheduleCycle {
			num++
		}
	}

	if num == gang.TotalChildrenNum {
		gang.ScheduleCycleValid = true
		gang.ScheduleCycle += 1
		klog.Infof("trySetScheduleCycleTrue, gangName: %v, ScheduleCycle: %v, ScheduleCycleValid: %v",
			gang.Name, gang.ScheduleCycle, gang.ScheduleCycleValid)
	}
}

func (gang *Gang) addAssumedPod(pod *v1.Pod) {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	podId := util.GetId(pod.Namespace, pod.Name)
	if _, ok := gang.WaitingForBindChildren[podId]; !ok {
		gang.WaitingForBindChildren[podId] = pod
		klog.Infof("AddAssumedPod, gangName: %v, podName: %v", gang.Name, podId)
	}
}

func (gang *Gang) delAssumedPod(pod *v1.Pod) {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	podId := util.GetId(pod.Namespace, pod.Name)
	if _, ok := gang.WaitingForBindChildren[podId]; ok {
		delete(gang.WaitingForBindChildren, podId)
		klog.Infof("delAssumedPod, gangName: %v, podName: %v", gang.Name, podId)
	}
}

func (gang *Gang) getChildrenFromGang() (children []*v1.Pod) {
	gang.lock.Lock()
	defer gang.lock.Unlock()
	children = make([]*v1.Pod, 0)
	for _, pod := range gang.Children {
		children = append(children, pod)
	}
	return
}

func (gang *Gang) isGangFromAnnotation() bool {
	gang.lock.Lock()
	defer gang.lock.Unlock()
	return gang.GangFrom == GangFromPodAnnotation
}

func (gang *Gang) setResourceSatisfied() {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	if !gang.OnceResourceSatisfied {
		gang.OnceResourceSatisfied = true
		klog.Infof("Gang ResourceSatisfied, gangName: %v", gang.Name)
	}
}

func (gang *Gang) addBoundPod(pod *v1.Pod) {
	gang.lock.Lock()
	defer gang.lock.Unlock()

	podId := util.GetId(pod.Namespace, pod.Name)
	delete(gang.WaitingForBindChildren, podId)
	gang.BoundChildren[podId] = pod

	klog.Infof("AddBoundPod, gangName: %v, podName: %v", gang.Name, podId)
	if len(gang.BoundChildren) >= gang.MinRequiredNumber {
		gang.OnceResourceSatisfied = true
		klog.Infof("Gang ResourceSatisfied due to addBoundPod, gangName: %v", gang.Name)
	}
}

func (gang *Gang) isGangValidForPermit() bool {
	gang.lock.Lock()
	defer gang.lock.Unlock()
	if !gang.HasGangInit {
		klog.Infof("isGangValidForPermit find gang hasn't inited ,gang: %v", gang.Name)
		return false
	}
	return len(gang.WaitingForBindChildren) >= gang.MinRequiredNumber || gang.OnceResourceSatisfied == true
}
