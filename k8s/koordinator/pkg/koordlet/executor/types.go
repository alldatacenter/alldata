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

package executor

import (
	"os"
	"strconv"
	"strings"
	"time"

	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

var _ ResourceUpdater = &CommonResourceUpdater{}

var _ ResourceUpdater = &CgroupResourceUpdater{}

var _ MergeableResourceUpdater = &CgroupResourceUpdater{}

type UpdateFunc func(resource ResourceUpdater) error

type MergeUpdateFunc func(resource MergeableResourceUpdater) (MergeableResourceUpdater, error)

// ResourceUpdater is the resource updater.
// DEPRECATED: use resourceexecutor.ResourceUpdater instead.
type ResourceUpdater interface {
	// RefObject reference to the object
	Owner() *OwnerRef
	Key() string
	Value() string
	GetLastUpdateTimestamp() time.Time
	SetValue(value string)
	UpdateLastUpdateTimestamp(time time.Time)
	Clone() ResourceUpdater
	Update() error
}

// MergeableResourceUpdater is the resource updater with merge update function.
// DEPRECATED: use resourceexecutor.ResourceUpdater instead.
type MergeableResourceUpdater interface {
	ResourceUpdater
	MergeUpdate() (MergeableResourceUpdater, error)
	NeedMerge() bool
}

type OwnerType int

const (
	NodeType = iota
	PodType
	ContainerType
	GroupType
	OthersType
)

// OwnerRef is used to record the object that needs to be modified
// or the source object that triggers the modification
type OwnerRef struct {
	Type      OwnerType
	Namespace string
	Name      string
	Container string
}

type CommonResourceUpdater struct {
	owner               *OwnerRef
	value               string
	key                 string
	file                string
	lastUpdateTimestamp time.Time
	updateFunc          UpdateFunc
}

func (c *CommonResourceUpdater) Owner() *OwnerRef {
	return c.owner
}

func (c *CommonResourceUpdater) Key() string {
	return c.file
}

func (c *CommonResourceUpdater) Value() string {
	return c.value
}

func (c *CommonResourceUpdater) GetLastUpdateTimestamp() time.Time {
	return c.lastUpdateTimestamp
}

func (c *CommonResourceUpdater) SetValue(value string) {
	c.value = value
}

func (c *CommonResourceUpdater) UpdateLastUpdateTimestamp(time time.Time) {
	c.lastUpdateTimestamp = time
}

func (c *CommonResourceUpdater) Clone() ResourceUpdater {
	return &CommonResourceUpdater{owner: c.owner, file: c.file, value: c.value, lastUpdateTimestamp: c.lastUpdateTimestamp, updateFunc: c.updateFunc}
}

func (c *CommonResourceUpdater) Update() error {
	return c.updateFunc(c)
}

func CommonUpdateFunc(resource ResourceUpdater) error {
	info := resource.(*CommonResourceUpdater)
	audit.V(5).Node().Reason(updateSystemConfig).Message("update %v to %v", info.file, info.value).Do()
	return system.CommonFileWriteIfDifferent(info.file, info.Value())
}

func NewCommonResourceUpdater(file string, value string) *CommonResourceUpdater {
	return &CommonResourceUpdater{key: file, file: file, value: value, updateFunc: CommonUpdateFunc}
}

func NewDetailCommonResourceUpdater(key, file, value string, owner *OwnerRef, updateFunc UpdateFunc) *CommonResourceUpdater {
	return &CommonResourceUpdater{
		owner:      owner,
		key:        key,
		file:       file,
		value:      value,
		updateFunc: updateFunc,
	}
}

type CgroupResourceUpdater struct {
	owner               *OwnerRef
	value               string
	ParentDir           string
	resource            system.Resource
	lastUpdateTimestamp time.Time
	updateFunc          UpdateFunc

	// MergeableResourceUpdater implementation (used by LeveledCacheExecutor):
	// For cgroup interfaces like `cpuset.cpus` and `memory.min`, reconciliation from top to bottom should keep the
	// upper value larger/broader than the lower. Thus a Leveled updater is implemented as follows:
	// 1. update batch of cgroup resources group by cgroup interface, i.e. cgroup filename.
	// 2. update each cgroup resource by the order of layers: firstly update resources from upper to lower by merging
	//    the new value with old value; then update resources from lower to upper with the new value.
	mergeUpdateFunc MergeUpdateFunc
	needMerge       bool // compatible to resource which just need to update once
}

func (c *CgroupResourceUpdater) ClearUpdateFunc() {
	c.updateFunc = nil
	c.mergeUpdateFunc = nil
}

func (c *CgroupResourceUpdater) Owner() *OwnerRef {
	return c.owner
}

func (c *CgroupResourceUpdater) Key() string {
	return system.GetCgroupFilePath(c.ParentDir, c.resource)
}

func (c *CgroupResourceUpdater) Value() string {
	return c.value
}

func (c *CgroupResourceUpdater) GetLastUpdateTimestamp() time.Time {
	return c.lastUpdateTimestamp
}

func (c *CgroupResourceUpdater) SetValue(value string) {
	c.value = value
}

func (c *CgroupResourceUpdater) UpdateLastUpdateTimestamp(time time.Time) {
	c.lastUpdateTimestamp = time
}

func (c *CgroupResourceUpdater) Update() error {
	return c.updateFunc(c)
}

func (c *CgroupResourceUpdater) Clone() ResourceUpdater {
	return &CgroupResourceUpdater{owner: c.owner, resource: c.resource, ParentDir: c.ParentDir, value: c.value, lastUpdateTimestamp: c.lastUpdateTimestamp, updateFunc: c.updateFunc}
}

func (c *CgroupResourceUpdater) MergeUpdate() (MergeableResourceUpdater, error) {
	return c.mergeUpdateFunc(c)
}

func (c *CgroupResourceUpdater) NeedMerge() bool {
	return c.needMerge
}

func CommonCgroupUpdateFunc(resource ResourceUpdater) error {
	info := resource.(*CgroupResourceUpdater)
	if info.owner != nil {
		switch info.owner.Type {
		case PodType:
			audit.V(5).Pod(info.owner.Namespace, info.owner.Name).Reason(updateCgroups).Message("update %v to %v", info.resource.ResourceType(), info.value).Do()
		case ContainerType:
			audit.V(5).Pod(info.owner.Namespace, info.owner.Name).Container(info.owner.Container).Reason(updateCgroups).Message("update %v to %v", info.resource.ResourceType(), info.value).Do()
		case NodeType:
			audit.V(5).Node().Reason(updateCgroups).Message("update %v to %v", info.resource.ResourceType(), info.value).Do()
		case GroupType:
			audit.V(5).Group(info.owner.Name).Reason(updateCgroups).Message("update %v to %v", info.resource.ResourceType(), info.value).Do()
		default:
			audit.V(5).Unknown(info.owner.Name).Reason(updateCgroups).Message("update %v to %v", info.resource.ResourceType(), info.value).Do()
		}
	}
	return system.CgroupFileWriteIfDifferent(info.ParentDir, info.resource, info.value)
}

func NewCommonCgroupResourceUpdater(owner *OwnerRef, parentDir string, resource system.Resource, value string) *CgroupResourceUpdater {
	return &CgroupResourceUpdater{owner: owner, resource: resource, ParentDir: parentDir, value: value, updateFunc: CommonCgroupUpdateFunc, needMerge: false}
}

// NewMergeableCgroupResourceUpdater returns a leveled CgroupResourceUpdater which firstly MergeUpdate from top
// to bottom and then Update from bottom to top.
func NewMergeableCgroupResourceUpdater(owner *OwnerRef, parentDir string, resource system.Resource, value string, mergeUpdateFunc MergeUpdateFunc) *CgroupResourceUpdater {
	return &CgroupResourceUpdater{owner: owner, resource: resource, ParentDir: parentDir, value: value, updateFunc: CommonCgroupUpdateFunc, mergeUpdateFunc: mergeUpdateFunc, needMerge: true}
}

func GroupOwnerRef(name string) *OwnerRef {
	return &OwnerRef{Type: GroupType, Name: name}
}

func PodOwnerRef(ns string, name string) *OwnerRef {
	return &OwnerRef{Type: PodType, Namespace: ns, Name: name}
}

func ContainerOwnerRef(ns string, name string, container string) *OwnerRef {
	return &OwnerRef{Type: PodType, Namespace: ns, Name: name, Container: container}
}

func UpdateResctrlSchemataFunc(resource ResourceUpdater) error {
	// NOTE: currently, only l3 schemata is to update, so do not read or compare before the write
	// eg.
	// $ cat /sys/fs/resctrl/schemata/BE/schemata
	// L3:0=7ff;1=7ff
	// MB:0=100;1=100
	// $ echo "L3:0=3f;1=3f" > /sys/fs/resctrl/BE/schemata
	// $ cat /sys/fs/resctrl/BE/schemata
	// L3:0=03f;1=03f
	// MB:0=100;1=100
	info := resource.(*CommonResourceUpdater)
	audit.V(5).Group(info.owner.Name).Reason(updateResctrlSchemata).Message("update %v with value %v",
		resource.Key(), resource.Value()).Do()
	return system.CommonFileWrite(info.file, info.value)
}

func UpdateResctrlTasksFunc(resource ResourceUpdater) error {
	// NOTE: resctrl/{...}/tasks file is required to appending write a task id once a time, and any duplicate would be
	//       dropped automatically without an exception
	// eg.
	// $ echo 123 > /sys/fs/resctrl/BE/tasks
	// $ echo 124 > /sys/fs/resctrl/BE/tasks
	// $ echo 123 > /sys/fs/resctrl/BE/tasks
	// $ echo 122 > /sys/fs/resctrl/BE/tasks
	// $ tail -n 3 /sys/fs/resctrl/BE/tasks
	// 122
	// 123
	// 124
	info := resource.(*CommonResourceUpdater)
	audit.V(5).Group(info.owner.Name).Reason(updateResctrlTasks).Message("update %v with value %v",
		resource.Key(), resource.Value()).Do()

	f, err := os.OpenFile(info.file, os.O_RDWR|os.O_APPEND, 0644)
	if err != nil {
		return err
	}

	success, total := 0, 0

	ids := strings.Split(strings.Trim(info.Value(), "\n"), "\n")
	for _, id := range ids {
		if strings.TrimSpace(id) == "" {
			continue
		}
		total++
		_, err = f.WriteString(id)
		// any thread can exit before the writing
		if err == nil {
			success++
			continue
		}
		klog.V(6).Infof("failed to write resctrl task id %v for group %s, err: %s", id,
			info.Owner().Name, err)
	}

	klog.V(5).Infof("write Cat L3 task ids for group %s finished: %v succeed, %v total",
		info.Owner().Name, success, total)

	return f.Close()
}

func MergeFuncUpdateCgroupIfLarger(resource MergeableResourceUpdater) (MergeableResourceUpdater, error) {
	info := resource.(*CgroupResourceUpdater)

	cur, err := strconv.ParseInt(info.value, 10, 64)
	if err != nil {
		klog.V(6).Infof("failed to merge update cgroup %v, read current value err: %s", info.resource.ResourceType(), err)
		return resource, err
	}
	oldPtr, err := system.CgroupFileReadInt(info.ParentDir, info.resource)
	if err != nil {
		klog.V(6).Infof("failed to merge update cgroup %v, read old value err: %s", info.resource.ResourceType(), err)
		return resource, err
	}

	// if old value is larger, skip the write since merged value does not change
	if cur <= *oldPtr {
		merged := resource.Clone().(*CgroupResourceUpdater)
		merged.value = strconv.FormatInt(*oldPtr, 10)
		klog.V(6).Infof("skip merge update cgroup %v since current value is smaller", info.resource.ResourceType())
		return merged, nil
	}
	// otherwise, do write for the current value
	if info.owner != nil {
		switch info.owner.Type {
		case PodType:
			audit.V(5).Pod(info.owner.Namespace, info.owner.Name).Reason(updateCgroups).Message("update %v to %v", info.resource.ResourceType(), info.value).Do()
		case ContainerType:
			audit.V(5).Pod(info.owner.Namespace, info.owner.Name).Container(info.owner.Container).Reason(updateCgroups).Message("update %v to %v", info.resource.ResourceType(), info.value).Do()
		case NodeType:
			audit.V(5).Node().Reason(updateCgroups).Message("update %v to %v", info.resource.ResourceType(), info.value).Do()
		case GroupType:
			audit.V(5).Group(info.owner.Name).Reason(updateCgroups).Message("update %v to %v", info.resource.ResourceType(), info.value).Do()
		default:
			audit.V(5).Unknown(info.owner.Name).Reason(updateCgroups).Message("update %v to %v", info.resource.ResourceType(), info.value).Do()
		}
	}
	// current value must be different
	return resource, system.CgroupFileWrite(info.ParentDir, info.resource, info.value)
}
