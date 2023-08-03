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

package migration

import (
	"fmt"
	"strconv"
	"sync"

	"k8s.io/apimachinery/pkg/api/meta"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/klog/v2"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

type assumedCache struct {
	lock  sync.Mutex
	items map[types.UID]*sev1alpha1.PodMigrationJob
}

func newAssumedCache() *assumedCache {
	return &assumedCache{
		items: map[types.UID]*sev1alpha1.PodMigrationJob{},
	}
}

func (c *assumedCache) assume(job *sev1alpha1.PodMigrationJob) {
	c.lock.Lock()
	c.items[job.UID] = job
	c.lock.Unlock()
}

func (c *assumedCache) delete(job *sev1alpha1.PodMigrationJob) {
	c.lock.Lock()
	delete(c.items, job.UID)
	c.lock.Unlock()
}

func (c *assumedCache) isNewOrSameObj(job *sev1alpha1.PodMigrationJob) bool {
	c.lock.Lock()
	preObj, ok := c.items[job.UID]
	c.lock.Unlock()
	if !ok {
		return true
	}
	newVersion, err := getObjVersion(job.Name, job)
	if err != nil {
		klog.Errorf("couldn't get object version: %v", err)
		return false
	}

	storedVersion, err := getObjVersion(job.Name, preObj)
	if err != nil {
		klog.Errorf("couldn't get stored object version: %v", err)
		return false
	}

	if newVersion < storedVersion {
		klog.V(4).Infof("Skip %v because version %v is not newer than %v", job.Name, newVersion, storedVersion)
		return false
	}
	return true
}

func getObjVersion(name string, obj interface{}) (int64, error) {
	objAccessor, err := meta.Accessor(obj)
	if err != nil {
		return -1, err
	}

	objResourceVersion, err := strconv.ParseInt(objAccessor.GetResourceVersion(), 10, 64)
	if err != nil {
		return -1, fmt.Errorf("error parsing ResourceVersion %q for %v %q", objAccessor.GetResourceVersion(), name, err)
	}
	return objResourceVersion, nil
}
