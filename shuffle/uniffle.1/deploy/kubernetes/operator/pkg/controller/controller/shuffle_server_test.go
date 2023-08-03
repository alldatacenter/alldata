/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controller

import (
	"context"
	"sort"
	"testing"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	kubefake "k8s.io/client-go/kubernetes/fake"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/config"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/clientset/versioned/fake"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
)

func buildUpgradingRssObjWithTargetKeys(shuffleServerKey string) *unifflev1alpha1.RemoteShuffleService {
	rss := utils.BuildRSSWithDefaultValue()
	rss.Status = unifflev1alpha1.RemoteShuffleServiceStatus{
		Phase:      unifflev1alpha1.RSSUpgrading,
		TargetKeys: []string{shuffleServerKey},
	}
	return rss
}

func buildTestShuffleServerPod() *corev1.Pod {
	return &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:      testShuffleServerPodName,
			Namespace: testNamespace,
			Annotations: map[string]string{
				constants.AnnotationShuffleServerPort: "8080",
			},
			Labels: map[string]string{
				appsv1.ControllerRevisionHashLabelKey: "test-revision-1",
			},
		},
		Status: corev1.PodStatus{
			PodIP: "xxx.xxx.xxx.xxx",
		},
	}
}

func buildTestExcludeNodeConfigMap(rss *unifflev1alpha1.RemoteShuffleService,
	shuffleServerKey string) *corev1.ConfigMap {
	return &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      utils.GenerateCoordinatorName(rss),
			Namespace: testNamespace,
		},
		Data: map[string]string{
			testExcludeNodeFileKey: shuffleServerKey,
		},
	}
}

func TestUpdateTargetAndDeletedKeys(t *testing.T) {
	testShuffleServerPod := buildTestShuffleServerPod()
	shuffleServerKey := utils.BuildShuffleServerKey(testShuffleServerPod)
	rss := buildUpgradingRssObjWithTargetKeys(shuffleServerKey)
	cm := buildTestExcludeNodeConfigMap(rss, shuffleServerKey)

	rssClient := fake.NewSimpleClientset(rss)
	kubeClient := kubefake.NewSimpleClientset(cm)

	rc := newRSSController(&config.Config{
		GenericConfig: utils.GenericConfig{
			KubeClient: kubeClient,
			RSSClient:  rssClient,
		},
	})
	if err := rc.cmInformer.GetIndexer().Add(cm); err != nil {
		t.Fatalf("update fake cm lister failed: %v", err)
	}

	for _, tt := range []struct {
		name                 string
		expectedDeletedKeys  []string
		expectedExcludeNodes string
	}{
		{
			name:                 "update target and deleted keys when a shuffle server pod has been deleted",
			expectedDeletedKeys:  []string{shuffleServerKey},
			expectedExcludeNodes: "",
		},
	} {
		t.Run(tt.name, func(tc *testing.T) {
			if err := rc.updateTargetAndDeletedKeys(rss, shuffleServerKey); err != nil {
				t.Errorf("update target and deleted keys failed: %v", err)
				return
			}

			curRss, err := rssClient.UniffleV1alpha1().RemoteShuffleServices(rss.Namespace).Get(context.TODO(),
				rss.Name, metav1.GetOptions{})
			if err != nil {
				t.Errorf("get updated rss object failed: %v", err)
				return
			}
			if !stringSliceIsEqual(curRss.Status.DeletedKeys, tt.expectedDeletedKeys) {
				t.Errorf("unexpected deleted keys: %+v, expected: %+v", curRss.Status.DeletedKeys,
					tt.expectedDeletedKeys)
				return
			}

			var curCM *corev1.ConfigMap
			curCM, err = kubeClient.CoreV1().ConfigMaps(cm.Namespace).Get(context.TODO(), cm.Name, metav1.GetOptions{})
			if err != nil {
				t.Errorf("get updated rss object failed: %v", err)
				return
			}
			if curCM.Data[testExcludeNodeFileKey] != tt.expectedExcludeNodes {
				t.Errorf("unexpected deleted keys: %v, expected: %v", curCM.Data[testExcludeNodeFileKey],
					tt.expectedExcludeNodes)
				return
			}
		})
	}
}

func stringSliceIsEqual(current []string, target []string) bool {
	if len(current) != len(target) {
		return false
	}
	sort.Strings(current)
	sort.Strings(target)
	for i := range current {
		if current[i] != target[i] {
			return false
		}
	}
	return true
}
