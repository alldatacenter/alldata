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

package resourceexecutor

import (
	"fmt"
	"math"
	"strconv"
	"sync"
	"time"

	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

var DefaultCgroupUpdaterFactory = NewCgroupUpdaterFactory()

func init() {
	// register the update logic for system resources
	// NOTE: should exclude the read-only resources, e.g. `cpu.stat`.
	// common
	DefaultCgroupUpdaterFactory.Register(NewUnlimitedCgroupUpdater,
		sysutil.CPUCFSQuotaName,
		sysutil.CPUCFSPeriodName,
		sysutil.MemoryLimitName,
	)
	DefaultCgroupUpdaterFactory.Register(NewCommonCgroupUpdater,
		sysutil.CPUBurstName,
		sysutil.CPUBVTWarpNsName,
		sysutil.CPUTasksName,
		sysutil.CPUProcsName,
		sysutil.MemoryWmarkRatioName,
		sysutil.MemoryWmarkScaleFactorName,
		sysutil.MemoryWmarkMinAdjName,
		sysutil.MemoryPriorityName,
		sysutil.MemoryUsePriorityOomName,
		sysutil.MemoryOomGroupName,
		sysutil.BlkioTRIopsName,
		sysutil.BlkioTRBpsName,
		sysutil.BlkioTWIopsName,
		sysutil.BlkioTWBpsName,
	)
	// special cases
	DefaultCgroupUpdaterFactory.Register(NewCPUSharesCgroupUpdater, sysutil.CPUSharesName)
	DefaultCgroupUpdaterFactory.Register(NewMergeableCgroupUpdaterIfValueLarger,
		sysutil.MemoryMinName,
		sysutil.MemoryLowName,
		sysutil.MemoryHighName,
	)
	DefaultCgroupUpdaterFactory.Register(NewMergeableCgroupUpdaterIfCPUSetLooser,
		sysutil.CPUSetCPUSName,
	)
}

type UpdateFunc func(resource ResourceUpdater) error

type MergeUpdateFunc func(resource ResourceUpdater) (ResourceUpdater, error)

type ResourceUpdater interface {
	ResourceType() sysutil.ResourceType
	Key() string
	Path() string
	Value() string
	Update() error
	MergeUpdate() (ResourceUpdater, error)
	Clone() ResourceUpdater
	GetLastUpdateTimestamp() time.Time
	UpdateLastUpdateTimestamp(time time.Time)
}

type CgroupResourceUpdater struct {
	file      sysutil.Resource
	parentDir string
	value     string

	lastUpdateTimestamp time.Time
	updateFunc          UpdateFunc
	// MergeableResourceUpdater implementation (used by LeveledCacheExecutor):
	// For cgroup interfaces like `cpuset.cpus` and `memory.min`, reconciliation from top to bottom should keep the
	// upper value larger/broader than the lower. Thus a Leveled updater is implemented as follows:
	// 1. update batch of cgroup resources group by cgroup interface, i.e. cgroup filename.
	// 2. update each cgroup resource by the order of layers: firstly update resources from upper to lower by merging
	//    the new value with old value; then update resources from lower to upper with the new value.
	mergeUpdateFunc MergeUpdateFunc
}

func (u *CgroupResourceUpdater) ResourceType() sysutil.ResourceType {
	return u.file.ResourceType()
}

func (u *CgroupResourceUpdater) Key() string {
	return u.file.Path(u.parentDir)
}

func (u *CgroupResourceUpdater) Path() string {
	return u.file.Path(u.parentDir)
}

func (u *CgroupResourceUpdater) Value() string {
	return u.value
}

func (u *CgroupResourceUpdater) Update() error {
	return u.updateFunc(u)
}

func (u *CgroupResourceUpdater) MergeUpdate() (ResourceUpdater, error) {
	if u.mergeUpdateFunc == nil {
		return nil, u.updateFunc(u)
	}
	return u.mergeUpdateFunc(u)
}

func (u *CgroupResourceUpdater) Clone() ResourceUpdater {
	return &CgroupResourceUpdater{
		file:                u.file,
		parentDir:           u.parentDir,
		value:               u.value,
		lastUpdateTimestamp: u.lastUpdateTimestamp,
		updateFunc:          u.updateFunc,
		mergeUpdateFunc:     u.mergeUpdateFunc,
	}
}

func (u *CgroupResourceUpdater) GetLastUpdateTimestamp() time.Time {
	return u.lastUpdateTimestamp
}

func (u *CgroupResourceUpdater) UpdateLastUpdateTimestamp(time time.Time) {
	u.lastUpdateTimestamp = time
}

func (u *CgroupResourceUpdater) SetUpdateFunc(updateFunc UpdateFunc, mergeUpdateFunc MergeUpdateFunc) {
	u.updateFunc = updateFunc
	u.mergeUpdateFunc = mergeUpdateFunc
}

type DefaultResourceUpdater struct {
	key                 string // the cache key to identify the updater (can be the filepath or other custom key)
	value               string
	file                string // the real filepath
	lastUpdateTimestamp time.Time
	updateFunc          UpdateFunc
}

func (u *DefaultResourceUpdater) ResourceType() sysutil.ResourceType {
	return sysutil.ResourceType(u.file)
}

func (u *DefaultResourceUpdater) Key() string {
	return u.key
}

func (u *DefaultResourceUpdater) Path() string {
	return u.file // no additional parent dir here
}

func (u *DefaultResourceUpdater) Value() string {
	return u.value
}

func (u *DefaultResourceUpdater) Update() error {
	return u.updateFunc(u)
}

func (u *DefaultResourceUpdater) MergeUpdate() (ResourceUpdater, error) {
	return nil, u.updateFunc(u)
}

func (u *DefaultResourceUpdater) Clone() ResourceUpdater {
	return &DefaultResourceUpdater{
		key:                 u.key,
		file:                u.file,
		value:               u.value,
		lastUpdateTimestamp: u.lastUpdateTimestamp,
		updateFunc:          u.updateFunc,
	}
}

func (u *DefaultResourceUpdater) GetLastUpdateTimestamp() time.Time {
	return u.lastUpdateTimestamp
}

func (u *DefaultResourceUpdater) UpdateLastUpdateTimestamp(time time.Time) {
	u.lastUpdateTimestamp = time
}

// NewCommonDefaultUpdater returns a DefaultResourceUpdater for update general files.
func NewCommonDefaultUpdater(key string, file string, value string) (ResourceUpdater, error) {
	return NewCommonDefaultUpdaterWithUpdateFunc(key, file, value, CommonDefaultUpdateFunc)
}

// NewCommonDefaultUpdaterWithUpdateFunc returns a DefaultResourceUpdater for update general files with the given update function.
func NewCommonDefaultUpdaterWithUpdateFunc(key string, file string, value string, updateFunc UpdateFunc) (ResourceUpdater, error) {
	return &DefaultResourceUpdater{
		key:        key,
		file:       file,
		value:      value,
		updateFunc: updateFunc,
	}, nil
}

type GuestCgroupResourceUpdater struct {
	*CgroupResourceUpdater
	sandboxID string // for execute the file operation inside the sandbox
}

type NewResourceUpdaterFunc func(resourceType sysutil.ResourceType, parentDir string, value string) (ResourceUpdater, error)

type ResourceUpdaterFactory interface {
	Register(g NewResourceUpdaterFunc, resourceTypes ...sysutil.ResourceType)
	New(resourceType sysutil.ResourceType, parentDir string, value string) (ResourceUpdater, error)
}

func NewCgroupUpdater(resourceType sysutil.ResourceType, parentDir string, value string, updateFunc UpdateFunc) (ResourceUpdater, error) {
	r, err := sysutil.GetCgroupResource(resourceType)
	if err != nil {
		return nil, err
	}
	return &CgroupResourceUpdater{
		file:       r,
		parentDir:  parentDir,
		value:      value,
		updateFunc: updateFunc,
	}, nil
}

// NewCommonCgroupUpdater returns a CgroupResourceUpdater for updating known cgroup resources.
func NewCommonCgroupUpdater(resourceType sysutil.ResourceType, parentDir string, value string) (ResourceUpdater, error) {
	return NewCgroupUpdater(resourceType, parentDir, value, CommonCgroupUpdateFunc)
}

func NewUnlimitedCgroupUpdater(resourceType sysutil.ResourceType, parentDir string, value string) (ResourceUpdater, error) {
	return NewCgroupUpdater(resourceType, parentDir, value, CgroupUpdateWithUnlimitedFunc)
}

func NewCPUSharesCgroupUpdater(resourceType sysutil.ResourceType, parentDir string, value string) (ResourceUpdater, error) {
	return NewCgroupUpdater(resourceType, parentDir, value, CgroupUpdateCPUSharesFunc)
}

func NewMergeableCgroupUpdaterWithCondition(resourceType sysutil.ResourceType, parentDir string, value string, mergeCondition MergeConditionFunc) (ResourceUpdater, error) {
	r, err := sysutil.GetCgroupResource(resourceType)
	if err != nil {
		return nil, err
	}
	return &CgroupResourceUpdater{
		file:       r,
		parentDir:  parentDir,
		value:      value,
		updateFunc: CommonCgroupUpdateFunc,
		mergeUpdateFunc: func(resource ResourceUpdater) (ResourceUpdater, error) {
			return MergeFuncUpdateCgroup(resource, mergeCondition)
		},
	}, nil
}

func NewMergeableCgroupUpdaterIfValueLarger(resourceType sysutil.ResourceType, parentDir string, value string) (ResourceUpdater, error) {
	return NewMergeableCgroupUpdaterWithCondition(resourceType, parentDir, value, MergeConditionIfValueIsLarger)
}

func NewMergeableCgroupUpdaterIfCPUSetLooser(resourceType sysutil.ResourceType, parentDir string, value string) (ResourceUpdater, error) {
	return NewMergeableCgroupUpdaterWithCondition(resourceType, parentDir, value, MergeConditionIfCPUSetIsLooser)
}

// NewDetailCgroupUpdater returns a new *CgroupResourceUpdater according to the given Resource, which is generally used
// for backwards compatibility. It it not guaranteed for updating successfully since it does not retrieve from the
// known cgroup resources.
func NewDetailCgroupUpdater(resource sysutil.Resource, parentDir string, value string, updateFunc UpdateFunc) (ResourceUpdater, error) {
	return &CgroupResourceUpdater{
		file:       resource,
		parentDir:  parentDir,
		value:      value,
		updateFunc: updateFunc,
	}, nil
}

type CgroupUpdaterFactoryImpl struct {
	lock     sync.RWMutex
	registry map[sysutil.ResourceType]NewResourceUpdaterFunc
}

func NewCgroupUpdaterFactory() ResourceUpdaterFactory {
	return &CgroupUpdaterFactoryImpl{
		registry: map[sysutil.ResourceType]NewResourceUpdaterFunc{},
	}
}

func (f *CgroupUpdaterFactoryImpl) Register(g NewResourceUpdaterFunc, resourceTypes ...sysutil.ResourceType) {
	f.lock.Lock()
	defer f.lock.Unlock()
	for _, t := range resourceTypes {
		_, ok := f.registry[t]
		if ok {
			klog.Warningf("resource type %s already registered, ignored", t)
			continue
		}
		f.registry[t] = g
	}
}

func (f *CgroupUpdaterFactoryImpl) New(resourceType sysutil.ResourceType, parentDir string, value string) (ResourceUpdater, error) {
	f.lock.RLock()
	defer f.lock.RUnlock()
	g, ok := f.registry[resourceType]
	if !ok {
		return nil, fmt.Errorf("resource type %s not registered", resourceType)
	}
	return g(resourceType, parentDir, value)
}

func CommonCgroupUpdateFunc(resource ResourceUpdater) error {
	c := resource.(*CgroupResourceUpdater)
	_ = audit.V(5).Reason(ReasonUpdateCgroups).Message("update %v to %v", resource.Path(), resource.Value()).Do()
	return sysutil.CgroupFileWriteIfDifferent(c.parentDir, c.file, c.value)
}

func CommonDefaultUpdateFunc(resource ResourceUpdater) error {
	c := resource.(*DefaultResourceUpdater)
	_ = audit.V(5).Reason(ReasonUpdateSystemConfig).Message("update %v to %v", resource.Path(), resource.Value()).Do()
	return sysutil.CommonFileWriteIfDifferent(c.Path(), c.value)
}

func CgroupUpdateWithUnlimitedFunc(resource ResourceUpdater) error {
	c := resource.(*CgroupResourceUpdater)
	// NOTE: convert "-1" to "max", since some cgroups-v2 files only accept "max" to unlimit resource instead of "-1".
	//       DO NOT use it on the cgroups which has a valid value of "-1".
	if c.value == "-1" && sysutil.GetCurrentCgroupVersion() == sysutil.CgroupVersionV2 {
		c.value = "max"
	}
	_ = audit.V(5).Reason(ReasonUpdateCgroups).Message("update %v to %v", resource.Path(), resource.Value()).Do()
	return sysutil.CgroupFileWriteIfDifferent(c.parentDir, c.file, c.value)
}

func CgroupUpdateCPUSharesFunc(resource ResourceUpdater) error {
	c := resource.(*CgroupResourceUpdater)
	// convert values in `cpu.shares` (v1) into values in `cpu.weight` (v2)
	if sysutil.GetCurrentCgroupVersion() == sysutil.CgroupVersionV2 {
		v, err := sysutil.ConvertCPUSharesToWeight(c.value)
		if err != nil {
			return err
		}
		c.value = strconv.FormatInt(v, 10)
	}
	_ = audit.V(5).Reason(ReasonUpdateCgroups).Message("update %v to %v", resource.Path(), resource.Value()).Do()
	return sysutil.CgroupFileWriteIfDifferent(c.parentDir, c.file, c.value)
}

type MergeConditionFunc func(oldValue, newValue string) (mergedValue string, needMerge bool, err error)

func MergeFuncUpdateCgroup(resource ResourceUpdater, mergeCondition MergeConditionFunc) (ResourceUpdater, error) {
	c := resource.(*CgroupResourceUpdater)

	isValid, msg := c.file.IsValid(c.value)
	if !isValid {
		klog.V(6).Infof("failed to merge update cgroup %v, read new value err: %s", c.Path(), msg)
		return resource, fmt.Errorf("parse new value failed, err: %v", msg)
	}

	oldStr, err := sysutil.CgroupFileRead(c.parentDir, c.file)
	if err != nil {
		klog.V(6).Infof("failed to merge update cgroup %v, read old value err: %s", c.Path(), err)
		return resource, err
	}

	mergedValue, needMerge, err := mergeCondition(oldStr, c.value)
	if err != nil {
		klog.V(6).Infof("failed to merge update cgroup %v, check merge condition err: %s", c.Path(), err)
		return resource, err
	}
	// skip the write when merge condition is not meet
	if !needMerge {
		merged := resource.Clone().(*CgroupResourceUpdater)
		merged.value = oldStr
		klog.V(6).Infof("skip merge update cgroup %v since no need to merge new value[%v] with old[%v]",
			c.Path(), c.value, oldStr)
		return merged, nil
	}

	// otherwise, do write for the current value
	_ = audit.V(5).Reason(ReasonUpdateCgroups).Message("update %v to %v", resource.Path(), resource.Value()).Do()
	klog.V(6).Infof("merge update cgroup %v with merged value[%v], original new[%v], old[%v]",
		c.Path(), mergedValue, c.value, oldStr)
	// suppose current value is different
	return resource, sysutil.CgroupFileWrite(c.parentDir, c.file, mergedValue)
}

// MergeConditionIfValueIsLarger returns a merge condition where only do update when the new value is larger.
func MergeConditionIfValueIsLarger(oldValue, newValue string) (string, bool, error) {
	var newV, oldV int64
	var err error
	if newValue == sysutil.CgroupMaxSymbolStr {
		newV = int64(math.MaxInt64)
	} else {
		newV, err = strconv.ParseInt(newValue, 10, 64)
		if err != nil {
			return newValue, false, fmt.Errorf("new value is not int64, err: %v", err)
		}
	}
	if oldValue == sysutil.CgroupMaxSymbolStr { // compatible with cgroup valued "max"
		oldV = int64(math.MaxInt64)
	} else {
		oldV, err = strconv.ParseInt(oldValue, 10, 64)
		if err != nil {
			return newValue, false, fmt.Errorf("old value is not int64, err: %v", err)
		}
	}
	return newValue, newV > oldV, nil
}

// MergeConditionIfCPUSetIsLooser returns a merge condition where only do update when the new cpuset value is looser.
func MergeConditionIfCPUSetIsLooser(oldValue, newValue string) (string, bool, error) {
	v, err := cpuset.Parse(newValue)
	if err != nil {
		return newValue, false, fmt.Errorf("new value is not valid cpuset, err: %v", err)
	}
	old, err := cpuset.Parse(oldValue)
	if err != nil {
		return newValue, false, fmt.Errorf("old value is not valid cpuset, err: %v", err)
	}

	// no need to merge if new cpuset is a subset of the old
	if v.IsSubsetOf(old) {
		return newValue, false, nil
	}

	// need to update with the merged of old and new cpuset values
	merged := v.Union(old)
	return merged.String(), true, nil
}
