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

package helper

import (
	"context"
	"fmt"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/util/uuid"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/client-go/tools/cache"
)

func TestSyncedEventHandler(t *testing.T) {
	var objects []runtime.Object
	for i := 0; i < 10; i++ {
		node := &corev1.Node{
			ObjectMeta: metav1.ObjectMeta{
				UID:             uuid.NewUUID(),
				Name:            fmt.Sprintf("node-%d", i),
				ResourceVersion: fmt.Sprintf("%d", i+1),
			},
		}
		objects = append(objects, node)
	}
	fakeClientSet := kubefake.NewSimpleClientset(objects...)
	sharedInformerFactory := informers.NewSharedInformerFactory(fakeClientSet, 0)
	nodeInformer := sharedInformerFactory.Core().V1().Nodes()
	addTimes := map[string]int{}
	var wg sync.WaitGroup
	wg.Add(10)
	ForceSyncFromInformer(context.TODO().Done(), sharedInformerFactory, nodeInformer.Informer(), cache.ResourceEventHandlerFuncs{
		AddFunc: func(obj interface{}) {
			node := obj.(*corev1.Node)
			addTimes[node.Name]++
			wg.Done()
		},
	})
	wg.Wait()
	for _, v := range addTimes {
		if v > 1 {
			t.Errorf("unexpected add times, want 1 but got %d", v)
			break
		}
	}
	node, err := nodeInformer.Lister().Get("node-0")
	assert.NoError(t, err)
	assert.NotNil(t, node)
	node = node.DeepCopy()
	node.ResourceVersion = "100"
	_, err = fakeClientSet.CoreV1().Nodes().Update(context.TODO(), node, metav1.UpdateOptions{})
	assert.NoError(t, err)
	err = wait.PollUntil(1*time.Second, func() (done bool, err error) {
		node, err := nodeInformer.Lister().Get("node-0")
		assert.NoError(t, err)
		assert.NotNil(t, node)
		return node.ResourceVersion == "100", nil
	}, wait.NeverStop)
	assert.NoError(t, err)
}
