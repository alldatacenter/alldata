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
	"os"
	"sort"
	"strconv"
	"strings"

	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

var _ ResourceUpdater = &ResctrlSchemataResourceUpdater{}

type ResctrlSchemataResourceUpdater struct {
	DefaultResourceUpdater
	schemataRaw *sysutil.ResctrlSchemataRaw
}

func (r *ResctrlSchemataResourceUpdater) Key() string {
	return r.schemataRaw.Prefix() + r.file
}

func (r *ResctrlSchemataResourceUpdater) Update() error {
	return r.DefaultResourceUpdater.updateFunc(r)
}

func (r *ResctrlSchemataResourceUpdater) Clone() ResourceUpdater {
	return &ResctrlSchemataResourceUpdater{
		DefaultResourceUpdater: *(r.DefaultResourceUpdater.Clone().(*DefaultResourceUpdater)),
		schemataRaw:            r.schemataRaw.DeepCopy(),
	}
}

func NewResctrlL3SchemataResource(group, schemataDelta string, l3Num int) ResourceUpdater {
	schemataFile := sysutil.ResctrlSchemata.Path(group)
	l3SchemataKey := sysutil.L3SchemataPrefix + ":" + schemataFile
	schemata := sysutil.NewResctrlSchemataRaw().WithL3Num(l3Num).WithL3Mask(schemataDelta)
	klog.V(6).Infof("generate new resctrl l3 schemata resource, file %s, key %s, value %s",
		schemataFile, l3SchemataKey, schemata.L3String())

	return &ResctrlSchemataResourceUpdater{
		DefaultResourceUpdater: DefaultResourceUpdater{
			key:        l3SchemataKey,
			file:       schemataFile,
			value:      schemata.L3String(),
			updateFunc: UpdateResctrlSchemataFunc,
		},
		schemataRaw: schemata,
	}
}

func NewResctrlMbSchemataResource(group, schemataDelta string, l3Num int) ResourceUpdater {
	schemataFile := sysutil.ResctrlSchemata.Path(group)
	mbSchemataKey := sysutil.MbSchemataPrefix + ":" + schemataFile
	schemata := sysutil.NewResctrlSchemataRaw().WithL3Num(l3Num).WithMBPercent(schemataDelta)
	klog.V(6).Infof("generate new resctrl mba schemata resource, file %s, key %s, value %s",
		schemataFile, mbSchemataKey, schemata.MBString())

	return &ResctrlSchemataResourceUpdater{
		DefaultResourceUpdater: DefaultResourceUpdater{
			key:        mbSchemataKey,
			file:       schemataFile,
			value:      schemata.MBString(),
			updateFunc: UpdateResctrlSchemataFunc,
		},
		schemataRaw: schemata,
	}
}

func CalculateResctrlL3TasksResource(group string, taskIds []int32) (ResourceUpdater, error) {
	// join ids into updater value and make the id updates one by one
	tasksPath := sysutil.GetResctrlTasksFilePath(group)

	// use ordered slice
	sort.Slice(taskIds, func(i, j int) bool {
		return taskIds[i] < taskIds[j]
	})
	var builder strings.Builder
	for _, id := range taskIds {
		builder.WriteString(strconv.FormatInt(int64(id), 10))
		builder.WriteByte('\n')
	}

	return NewCommonDefaultUpdaterWithUpdateFunc(tasksPath, tasksPath, builder.String(), UpdateResctrlTasksFunc)
}

func UpdateResctrlSchemataFunc(u ResourceUpdater) error {
	r, ok := u.(*ResctrlSchemataResourceUpdater)
	if !ok {
		return fmt.Errorf("not a ResctrlSchemataResourceUpdater")
	}

	schemataFile := r.Path()

	oldR, err := sysutil.ReadResctrlSchemataRaw(schemataFile, r.schemataRaw.L3Number())
	if err != nil {
		return fmt.Errorf("failed to read current resctrl schemata, path %s, err: %v", schemataFile, err)
	}

	isEqual, msg := oldR.Equal(r.schemataRaw)
	if isEqual { // schemata unchanged, no need to update
		klog.V(6).Infof("skip update resctrl schemata, old l3 %s, mba %s, new %s, l3Num %v",
			oldR.L3String(), oldR.MBString(), r.Value(), r.schemataRaw.L3Number())
		return nil
	}
	klog.V(5).Infof("need to update resctrl schemata, old l3 %s, mba %s, new %s, l3Num %v, msg: %s",
		oldR.L3String(), oldR.MBString(), r.Value(), r.schemataRaw.L3Number(), msg)

	// NOTE: currently, only l3 and mba schemata are to update, so do not read or compare before the write
	// eg.
	// $ cat /sys/fs/resctrl/schemata/BE/schemata
	// L3:0=7ff;1=7ff
	// MB:0=100;1=100
	// $ echo "L3:0=3f;1=3f" > /sys/fs/resctrl/BE/schemata
	// $ cat /sys/fs/resctrl/BE/schemata
	// L3:0=03f;1=03f
	// MB:0=100;1=100
	_ = audit.V(5).Reason(ReasonUpdateResctrl).Message("update %v to %v", u.Key(), u.Value()).Do()
	return sysutil.CommonFileWrite(u.Path(), u.Value())
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
	_ = audit.V(5).Reason(ReasonUpdateResctrl).Message("update %v to %v", resource.Key(), resource.Value()).Do()

	f, err := os.OpenFile(resource.Path(), os.O_RDWR|os.O_APPEND, 0644)
	if err != nil {
		return err
	}

	success, total := 0, 0

	ids := strings.Split(strings.Trim(resource.Value(), "\n"), "\n")
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
		klog.V(6).Infof("failed to write resctrl task id %v for dir %s, err: %s", id,
			resource.Key(), err)
	}

	klog.V(5).Infof("write Cat L3 task ids for dir %s finished: %v succeed, %v total",
		resource.Key(), success, total)

	return f.Close()
}
