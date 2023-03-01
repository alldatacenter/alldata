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

package inspector

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	admissionv1 "k8s.io/api/admission/v1"
	nodev1 "k8s.io/api/node/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/kubernetes"
	kubefake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/utils/pointer"

	uniffleapi "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/config"
)

const (
	testRuntimeClassName = "test-runtime"
)

type wrapper func(rss *uniffleapi.RemoteShuffleService)

func wrapRssObj(wrapperFunc wrapper) *uniffleapi.RemoteShuffleService {
	rss := utils.BuildRSSWithDefaultValue()
	wrapperFunc(rss)
	return rss
}

// convertRssToRawExtension converts a rss object to runtime.RawExtension for testing.
func convertRssToRawExtension(rss *uniffleapi.RemoteShuffleService) (runtime.RawExtension, error) {
	if rss == nil {
		return convertRssToRawExtension(&uniffleapi.RemoteShuffleService{})
	}
	body, err := json.Marshal(rss)
	if err != nil {
		return runtime.RawExtension{}, err
	}
	return runtime.RawExtension{
		Raw:    body,
		Object: rss,
	}, nil
}

// buildTestAdmissionReview builds an AdmissionReview object for testing.
func buildTestAdmissionReview(op admissionv1.Operation,
	oldRss, newRss *uniffleapi.RemoteShuffleService) *admissionv1.AdmissionReview {
	oldObject, err := convertRssToRawExtension(oldRss)
	if err != nil {
		panic(err)
	}
	var object runtime.RawExtension
	object, err = convertRssToRawExtension(newRss)
	if err != nil {
		panic(err)
	}
	return &admissionv1.AdmissionReview{
		Request: &admissionv1.AdmissionRequest{
			Operation: op,
			Object:    object,
			OldObject: oldObject,
		},
	}
}

// TestValidateRSS tests rss objects' validation in rss-webhook.
func TestValidateRSS(t *testing.T) {
	testInspector := newInspector(&config.Config{}, nil)

	rssWithCooNodePort := wrapRssObj(func(rss *uniffleapi.RemoteShuffleService) {
		rss.Spec.Coordinator.Count = pointer.Int32(2)
		rss.Spec.Coordinator.RPCNodePort = []int32{30001, 30002}
		rss.Spec.Coordinator.HTTPNodePort = []int32{30011, 30012}
		rss.Spec.Coordinator.ExcludeNodesFilePath = ""
	})

	rssWithoutLogInCooMounts := rssWithCooNodePort.DeepCopy()
	rssWithoutLogInCooMounts.Spec.Coordinator.ExcludeNodesFilePath = "/exclude_nodes"
	rssWithoutLogInCooMounts.Spec.Coordinator.CommonConfig = &uniffleapi.CommonConfig{
		RSSPodSpec: &uniffleapi.RSSPodSpec{
			LogHostPath:    "/data/logs",
			HostPathMounts: map[string]string{},
		},
	}

	rssWithoutLogInServerMounts := rssWithoutLogInCooMounts.DeepCopy()
	rssWithoutLogInServerMounts.Spec.Coordinator.CommonConfig.RSSPodSpec.HostPathMounts["/data/logs"] = "/data/logs"
	rssWithoutLogInServerMounts.Spec.ShuffleServer = &uniffleapi.ShuffleServerConfig{
		CommonConfig: &uniffleapi.CommonConfig{
			RSSPodSpec: &uniffleapi.RSSPodSpec{
				LogHostPath:    "/data/logs",
				HostPathMounts: map[string]string{},
			},
		},
	}

	rssWithoutPartition := rssWithoutLogInServerMounts.DeepCopy()
	rssWithoutPartition.Spec.ShuffleServer.CommonConfig.RSSPodSpec.HostPathMounts["/data/logs"] = "/data/logs"
	rssWithoutPartition.Spec.ShuffleServer.UpgradeStrategy = &uniffleapi.ShuffleServerUpgradeStrategy{
		Type: uniffleapi.PartitionUpgrade,
	}

	rssWithInvalidPartition := rssWithoutLogInServerMounts.DeepCopy()
	rssWithInvalidPartition.Spec.ShuffleServer.CommonConfig.RSSPodSpec.HostPathMounts["/data/logs"] = "/data/logs"
	rssWithInvalidPartition.Spec.ShuffleServer.UpgradeStrategy = &uniffleapi.ShuffleServerUpgradeStrategy{
		Type:      uniffleapi.PartitionUpgrade,
		Partition: pointer.Int32(-1),
	}

	rssWithoutSpecificNames := rssWithoutLogInServerMounts.DeepCopy()
	rssWithoutSpecificNames.Spec.ShuffleServer.CommonConfig.RSSPodSpec.HostPathMounts["/data/logs"] = "/data/logs"
	rssWithoutSpecificNames.Spec.ShuffleServer.UpgradeStrategy = &uniffleapi.ShuffleServerUpgradeStrategy{
		Type: uniffleapi.SpecificUpgrade,
	}

	rssWithoutUpgradeStrategyType := rssWithoutLogInServerMounts.DeepCopy()
	rssWithoutUpgradeStrategyType.Spec.ShuffleServer.CommonConfig.RSSPodSpec.HostPathMounts["/data/logs"] = "/data/logs"
	rssWithoutUpgradeStrategyType.Spec.ShuffleServer.UpgradeStrategy = &uniffleapi.ShuffleServerUpgradeStrategy{}

	for _, tt := range []struct {
		name    string
		ar      *admissionv1.AdmissionReview
		allowed bool
	}{
		{
			name: "try to modify a upgrading rss object",
			ar: buildTestAdmissionReview(admissionv1.Update, wrapRssObj(
				func(rss *uniffleapi.RemoteShuffleService) {
					rss.Status = uniffleapi.RemoteShuffleServiceStatus{
						Phase: uniffleapi.RSSUpgrading,
					}
				}), nil),
			allowed: false,
		},
		{
			name: "invalid rpc node port number in a rss object",
			ar: buildTestAdmissionReview(admissionv1.Create, nil,
				wrapRssObj(func(rss *uniffleapi.RemoteShuffleService) {
					rss.Spec.Coordinator.Count = pointer.Int32(2)
					rss.Spec.Coordinator.RPCNodePort = []int32{30001}
				})),
			allowed: false,
		},
		{
			name: "invalid http node port number in a rss object",
			ar: buildTestAdmissionReview(admissionv1.Create, nil,
				wrapRssObj(func(rss *uniffleapi.RemoteShuffleService) {
					rss.Spec.Coordinator.Count = pointer.Int32(2)
					rss.Spec.Coordinator.RPCNodePort = []int32{30001, 30002}
					rss.Spec.Coordinator.HTTPNodePort = []int32{30011}
				})),
			allowed: false,
		},
		{
			name:    "empty exclude nodes file path field in a rss object",
			ar:      buildTestAdmissionReview(admissionv1.Create, nil, rssWithCooNodePort),
			allowed: false,
		},
		{
			name:    "can not find log host path in coordinators' host path mounts field in a rss object",
			ar:      buildTestAdmissionReview(admissionv1.Create, nil, rssWithoutLogInCooMounts),
			allowed: false,
		},
		{
			name:    "can not find log host path in shuffle server' host path mounts field in a rss object",
			ar:      buildTestAdmissionReview(admissionv1.Create, nil, rssWithoutLogInServerMounts),
			allowed: false,
		},
		{
			name:    "empty partition field when shuffler server of a rss object need partition upgrade",
			ar:      buildTestAdmissionReview(admissionv1.Create, nil, rssWithoutPartition),
			allowed: false,
		},
		{
			name:    "invalid partition field when shuffler server of a rss object need partition upgrade",
			ar:      buildTestAdmissionReview(admissionv1.Create, nil, rssWithInvalidPartition),
			allowed: false,
		},
		{
			name:    "empty specific names field when shuffler server of a rss object need specific upgrade",
			ar:      buildTestAdmissionReview(admissionv1.Create, nil, rssWithoutSpecificNames),
			allowed: false,
		},
		{
			name:    "empty upgrade strategy type in shuffler server of a rss object",
			ar:      buildTestAdmissionReview(admissionv1.Create, nil, rssWithoutUpgradeStrategyType),
			allowed: false,
		},
	} {
		t.Run(tt.name, func(tc *testing.T) {
			updatedAR := testInspector.validateRSS(tt.ar)
			if !updatedAR.Response.Allowed {
				tc.Logf("==> message in result: %+v", updatedAR.Response.Result.Message)
			}
			if updatedAR.Response.Allowed != tt.allowed {
				tc.Errorf("invalid 'allowed' field in response: %v <> %v", updatedAR.Response.Allowed, tt.allowed)
			}
		})
	}
}

func TestValidateRuntimeClassNames(t *testing.T) {
	for _, tt := range []struct {
		name         string
		runtimeClass *nodev1.RuntimeClass
		ar           *admissionv1.AdmissionReview
		allowed      bool
	}{
		{
			name:         "create rss object with existent runtime class names",
			runtimeClass: buildTestRuntimeClass(),
			ar: buildTestAdmissionReview(admissionv1.Create, nil,
				wrapRssObj(func(rss *uniffleapi.RemoteShuffleService) {
					rss.Spec.Coordinator.RuntimeClassName = pointer.String(testRuntimeClassName)
					rss.Spec.ShuffleServer.RuntimeClassName = pointer.String(testRuntimeClassName)
				})),
			allowed: true,
		},
		{
			name:         "create rss object with empty runtime class name used by shuffle server",
			runtimeClass: buildTestRuntimeClass(),
			ar: buildTestAdmissionReview(admissionv1.Create, nil,
				wrapRssObj(func(rss *uniffleapi.RemoteShuffleService) {
					rss.Spec.Coordinator.RuntimeClassName = pointer.String(testRuntimeClassName)
				})),
			allowed: true,
		},
		{
			name: "create rss object with non existent runtime class name used by coordinator",
			ar: buildTestAdmissionReview(admissionv1.Create, nil,
				wrapRssObj(func(rss *uniffleapi.RemoteShuffleService) {
					rss.Spec.Coordinator.RuntimeClassName = pointer.String(testRuntimeClassName)
				})),
			allowed: false,
		},
		{
			name: "create rss object with non existent runtime class name used by shuffle server",
			ar: buildTestAdmissionReview(admissionv1.Create, nil,
				wrapRssObj(func(rss *uniffleapi.RemoteShuffleService) {
					rss.Spec.ShuffleServer.RuntimeClassName = pointer.String(testRuntimeClassName)
				})),
			allowed: false,
		},
	} {
		t.Run(tt.name, func(tc *testing.T) {
			var kubeClient kubernetes.Interface
			if tt.runtimeClass != nil {
				kubeClient = kubefake.NewSimpleClientset(tt.runtimeClass)
			} else {
				kubeClient = kubefake.NewSimpleClientset()
			}

			testInspector := newInspector(&config.Config{
				GenericConfig: utils.GenericConfig{
					KubeClient: kubeClient,
				},
			}, nil)

			updatedAR := testInspector.validateRSS(tt.ar)
			if !updatedAR.Response.Allowed {
				tc.Logf("==> message in result: %+v", updatedAR.Response.Result.Message)
			}
			if updatedAR.Response.Allowed != tt.allowed {
				tc.Errorf("invalid 'allowed' field in response: %v <> %v", updatedAR.Response.Allowed, tt.allowed)
			}
		})
	}
}

func buildTestRuntimeClass() *nodev1.RuntimeClass {
	return &nodev1.RuntimeClass{
		ObjectMeta: metav1.ObjectMeta{
			Name: testRuntimeClassName,
		},
		Handler: "/etc/runtime/bin",
	}
}

func TestValidateCoordinator(t *testing.T) {
	for _, tt := range []struct {
		name        string
		coordinator *uniffleapi.CoordinatorConfig
		allowed     bool
	}{
		{
			name: "empty RPCNodePort",
			coordinator: &uniffleapi.CoordinatorConfig{
				Count: pointer.Int32(2),
			},
			allowed: true,
		},
		{
			name: "same number of RPCNodePort and HTTPNodePort",
			coordinator: &uniffleapi.CoordinatorConfig{
				Count:        pointer.Int32(2),
				RPCNodePort:  []int32{19996, 19997},
				HTTPNodePort: []int32{19996, 19997},
			},
			allowed: true,
		},
		{
			name: "different number of RPCNodePort and HTTPNodePort",
			coordinator: &uniffleapi.CoordinatorConfig{
				Count:        pointer.Int32(2),
				RPCNodePort:  []int32{19996, 19997, 19998},
				HTTPNodePort: []int32{19991, 19992},
			},
			allowed: false,
		},
		{
			name: "same number of RPCNodePort and HTTPNodePort but with different coordinator count",
			coordinator: &uniffleapi.CoordinatorConfig{
				Count:        pointer.Int32(1),
				RPCNodePort:  []int32{19996, 19997},
				HTTPNodePort: []int32{19991, 19992},
			},
			allowed: false,
		},
	} {
		t.Run(tt.name, func(t *testing.T) {
			assertion := assert.New(t)
			tt.coordinator.ExcludeNodesFilePath = "/exclude_nodes"
			tt.coordinator.CommonConfig = &uniffleapi.CommonConfig{
				RSSPodSpec: &uniffleapi.RSSPodSpec{
					LogHostPath:    "",
					HostPathMounts: map[string]string{},
				},
			}
			err := validateCoordinator(tt.coordinator)
			if tt.allowed {
				assertion.Nil(err, "expected allowed, but got error: %v", err)
			} else {
				assertion.Error(err, "expected denied, but got accepted")
			}
		})
	}
}
