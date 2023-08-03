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
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	kubefake "k8s.io/client-go/kubernetes/fake"

	uniffleapi "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/config"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/clientset/versioned/fake"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
)

const (
	extraFinalizer = "extra-finalizer"
)

// TestProcessEmptyPhaseRss tests rss objects' process of rss-controller
func TestProcessEmptyPhaseRss(t *testing.T) {
	rss := utils.BuildRSSWithDefaultValue()

	rssClient := fake.NewSimpleClientset(rss)
	kubeClient := kubefake.NewSimpleClientset()

	rc := newRSSController(&config.Config{
		GenericConfig: utils.GenericConfig{
			KubeClient: kubeClient,
			RSSClient:  rssClient,
		},
	})

	for _, tt := range []struct {
		name              string
		expectedRssStatus uniffleapi.RemoteShuffleServiceStatus
		expectedNeedRetry bool
		expectedError     error
	}{
		{
			name: "process rss object which has just been created, and whose status phase is empty",
			expectedRssStatus: uniffleapi.RemoteShuffleServiceStatus{
				Phase: uniffleapi.RSSPending,
			},
			expectedNeedRetry: false,
		},
	} {
		t.Run(tt.name, func(tc *testing.T) {
			needRetry, err := rc.processNormal(rss)
			if err != nil {
				tc.Errorf("process rss object failed: %v", err)
				return
			}
			if needRetry != tt.expectedNeedRetry {
				tc.Errorf("unexpected result indicates whether to retrys: %v, expected: %v",
					needRetry, tt.expectedNeedRetry)
				return
			}
			updatedRss, getErr := rssClient.UniffleV1alpha1().RemoteShuffleServices(rss.Namespace).
				Get(context.TODO(), rss.Name, metav1.GetOptions{})
			if getErr != nil {
				tc.Errorf("get updated rss object failed: %v", err)
				return
			}
			if !reflect.DeepEqual(updatedRss.Status, tt.expectedRssStatus) {
				tc.Errorf("unexpected status of updated rss object: %+v, expected: %+v",
					updatedRss.Status, tt.expectedRssStatus)
				return
			}
		})
	}
}

// TestRemoveFinalizer tests removing finalizer of rss.
func TestRemoveFinalizer(t *testing.T) {
	for _, tt := range []struct {
		name               string
		oldFinalizers      []string
		expectedFinalizers []string
	}{
		{
			name:               "with extra finalizer",
			oldFinalizers:      []string{constants.RSSFinalizerName, extraFinalizer},
			expectedFinalizers: []string{extraFinalizer},
		},
		{
			name:               "without extra finalizer",
			oldFinalizers:      []string{constants.RSSFinalizerName},
			expectedFinalizers: nil,
		},
	} {
		t.Run(tt.name, func(tc *testing.T) {
			assertion := assert.New(tc)

			rss := utils.BuildRSSWithDefaultValue()
			now := metav1.Now()
			rss.DeletionTimestamp = &now
			rss.Finalizers = tt.oldFinalizers

			rssClient := fake.NewSimpleClientset(rss)
			kubeClient := kubefake.NewSimpleClientset()

			rc := newRSSController(&config.Config{
				GenericConfig: utils.GenericConfig{
					KubeClient: kubeClient,
					RSSClient:  rssClient,
				},
			})
			err := rc.removeFinalizer(rss)
			assertion.Empty(err, nil)

			rss, err = rssClient.UniffleV1alpha1().RemoteShuffleServices(rss.Namespace).Get(context.TODO(), rss.Name,
				metav1.GetOptions{})
			assertion.Empty(err, nil)
			assertion.Equal(rss.Finalizers, tt.expectedFinalizers)
		})
	}
}
