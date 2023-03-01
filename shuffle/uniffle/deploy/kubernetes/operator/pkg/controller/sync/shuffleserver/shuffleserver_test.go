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

package shuffleserver

import (
	"encoding/json"
	"fmt"
	"reflect"
	"strconv"
	"testing"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/utils/pointer"

	uniffleapi "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	controllerconstants "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/sync/coordinator"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
)

const (
	testRuntimeClassName       = "test-runtime"
	testRPCPort          int32 = 19998
	testHTTPPort         int32 = 19999
)

// IsValidSts checks generated statefulSet, returns whether it is valid and error message.
type IsValidSts func(*appsv1.StatefulSet, *uniffleapi.RemoteShuffleService) (bool, error)

var (
	testLabels = map[string]string{
		"key1": "value1",
		"key2": "value2",
		"key3": "value3",
	}
	testENVs = []corev1.EnvVar{
		{
			Name:  "ENV1",
			Value: "Value1",
		},
		{
			Name:  "ENV2",
			Value: "Value2",
		},
		{
			Name:  "ENV3",
			Value: "Value3",
		},
		{
			Name:  controllerconstants.XmxSizeEnv,
			Value: "1G",
		},
	}
	testVolumeName = "test-volume"
	testVolumes    = []corev1.Volume{
		{
			Name: testVolumeName,
			VolumeSource: corev1.VolumeSource{
				ConfigMap: &corev1.ConfigMapVolumeSource{
					LocalObjectReference: corev1.LocalObjectReference{
						Name: "test-config",
					},
				},
			},
		},
	}
	testTolerationName = "test-toleration"
	testTolerations    = []corev1.Toleration{
		{
			Key:      testTolerationName,
			Operator: corev1.TolerationOperator("In"),
			Value:    "",
			Effect:   corev1.TaintEffectNoSchedule,
		},
	}
	testAffinity = &corev1.Affinity{
		NodeAffinity: &corev1.NodeAffinity{
			RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
				NodeSelectorTerms: []corev1.NodeSelectorTerm{
					{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "key2",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"value1", "value2"},
							},
						},
						MatchFields: []corev1.NodeSelectorRequirement{
							{
								Key:      "metadata.name",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"host1"},
							},
						},
					},
				},
			},
			PreferredDuringSchedulingIgnoredDuringExecution: []corev1.PreferredSchedulingTerm{
				{
					Weight: 10,
					Preference: corev1.NodeSelectorTerm{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "foo",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"bar"},
							},
						},
					},
				},
			},
		},
	}
)

func buildRssWithLabels() *uniffleapi.RemoteShuffleService {
	rss := utils.BuildRSSWithDefaultValue()
	rss.Spec.ShuffleServer.Labels = map[string]string{
		"uniffle.apache.org/shuffle-server": "change-test",
	}
	for k := range testLabels {
		rss.Spec.ShuffleServer.Labels[k] = testLabels[k]
	}
	return rss
}

func buildRssWithRuntimeClassName() *uniffleapi.RemoteShuffleService {
	rss := utils.BuildRSSWithDefaultValue()
	rss.Spec.ShuffleServer.RuntimeClassName = pointer.String(testRuntimeClassName)
	return rss
}

func buildRssWithCustomENVs() *uniffleapi.RemoteShuffleService {
	rss := utils.BuildRSSWithDefaultValue()
	rss.Spec.ShuffleServer.Env = testENVs
	return rss
}

func withCustomVolumes(volumes []corev1.Volume) *uniffleapi.RemoteShuffleService {
	rss := utils.BuildRSSWithDefaultValue()
	rss.Spec.ShuffleServer.Volumes = volumes
	return rss
}

func buildRssWithCustomRPCPort() *uniffleapi.RemoteShuffleService {
	rss := utils.BuildRSSWithDefaultValue()
	rss.Spec.ShuffleServer.RPCPort = pointer.Int32(testRPCPort)
	return rss
}

func buildRssWithCustomHTTPPort() *uniffleapi.RemoteShuffleService {
	rss := utils.BuildRSSWithDefaultValue()
	rss.Spec.ShuffleServer.HTTPPort = pointer.Int32(testHTTPPort)
	return rss
}

func withCustomTolerations(tolerations []corev1.Toleration) *uniffleapi.RemoteShuffleService {
	rss := utils.BuildRSSWithDefaultValue()
	rss.Spec.ShuffleServer.Tolerations = tolerations
	return rss
}

func withCustomAffinity(affinity *corev1.Affinity) *uniffleapi.RemoteShuffleService {
	rss := utils.BuildRSSWithDefaultValue()
	rss.Spec.ShuffleServer.Affinity = affinity
	return rss
}

func buildCommonExpectedENVs(rss *uniffleapi.RemoteShuffleService) []corev1.EnvVar {
	return []corev1.EnvVar{
		{
			Name:  controllerconstants.ShuffleServerRPCPortEnv,
			Value: strconv.FormatInt(int64(*rss.Spec.ShuffleServer.RPCPort), 10),
		},
		{
			Name:  controllerconstants.ShuffleServerHTTPPortEnv,
			Value: strconv.FormatInt(int64(*rss.Spec.ShuffleServer.HTTPPort), 10),
		},
		{
			Name:  controllerconstants.RSSCoordinatorQuorumEnv,
			Value: coordinator.GenerateAddresses(rss),
		},
		{
			Name:  controllerconstants.XmxSizeEnv,
			Value: rss.Spec.ShuffleServer.XmxSize,
		},
		{
			Name:  controllerconstants.ServiceNameEnv,
			Value: controllerconstants.ShuffleServerServiceName,
		},
		{
			Name: controllerconstants.NodeNameEnv,
			ValueFrom: &corev1.EnvVarSource{
				FieldRef: &corev1.ObjectFieldSelector{
					APIVersion: "v1",
					FieldPath:  "spec.nodeName",
				},
			},
		},
		{
			Name: controllerconstants.RssIPEnv,
			ValueFrom: &corev1.EnvVarSource{
				FieldRef: &corev1.ObjectFieldSelector{
					APIVersion: "v1",
					FieldPath:  "status.podIP",
				},
			},
		},
	}
}

func TestGenerateSts(t *testing.T) {
	for _, tt := range []struct {
		name string
		rss  *uniffleapi.RemoteShuffleService
		IsValidSts
	}{
		{
			name: "add custom labels",
			rss:  buildRssWithLabels(),
			IsValidSts: func(sts *appsv1.StatefulSet, rss *uniffleapi.RemoteShuffleService) (valid bool, err error) {
				valid = true

				expectedLabels := map[string]string{
					"app":                               "rss-shuffle-server-rss",
					"uniffle.apache.org/shuffle-server": "true",
				}
				for k := range testLabels {
					expectedLabels[k] = testLabels[k]
				}

				currentLabels := sts.Spec.Template.Labels
				if len(expectedLabels) != len(currentLabels) {
					valid = false
				} else {
					for k := range currentLabels {
						if expectedLabels[k] != currentLabels[k] {
							valid = false
							break
						}
					}
				}
				if !valid {
					err = fmt.Errorf("unexpected labels: %+v, expected: %+v", currentLabels, expectedLabels)
				}
				return valid, err
			},
		},
		{
			name: "set custom runtime class name",
			rss:  buildRssWithRuntimeClassName(),
			IsValidSts: func(sts *appsv1.StatefulSet, rss *uniffleapi.RemoteShuffleService) (valid bool, err error) {
				currentRuntimeClassName := sts.Spec.Template.Spec.RuntimeClassName
				if currentRuntimeClassName == nil {
					return false, fmt.Errorf("unexpected empty runtime class, expected: %v",
						testRuntimeClassName)
				}
				if *currentRuntimeClassName != testRuntimeClassName {
					return false, fmt.Errorf("unexpected runtime class name: %v, expected: %v",
						*currentRuntimeClassName, testRuntimeClassName)
				}
				return true, nil
			},
		},
		{
			name: "set custom environment variables",
			rss:  buildRssWithCustomENVs(),
			IsValidSts: func(sts *appsv1.StatefulSet, rss *uniffleapi.RemoteShuffleService) (valid bool, err error) {
				expectENVs := buildCommonExpectedENVs(rss)
				defaultEnvNames := sets.NewString()
				for i := range expectENVs {
					defaultEnvNames.Insert(expectENVs[i].Name)
				}
				for i := range testENVs {
					if !defaultEnvNames.Has(testENVs[i].Name) {
						expectENVs = append(expectENVs, testENVs[i])
					}
				}

				actualENVs := sts.Spec.Template.Spec.Containers[0].Env
				valid = reflect.DeepEqual(expectENVs, actualENVs)
				if !valid {
					actualEnvBody, _ := json.Marshal(actualENVs)
					expectEnvBody, _ := json.Marshal(expectENVs)
					err = fmt.Errorf("unexpected ENVs:\n%v,\nexpected:\n%v",
						string(actualEnvBody), string(expectEnvBody))
				}
				return
			},
		},
		{
			name: "set custom rpc port used by shuffle server",
			rss:  buildRssWithCustomRPCPort(),
			IsValidSts: func(sts *appsv1.StatefulSet, rss *uniffleapi.RemoteShuffleService) (
				valid bool, err error) {
				// check envs
				expectENVs := buildCommonExpectedENVs(rss)
				for i := range expectENVs {
					if expectENVs[i].Name == controllerconstants.ShuffleServerRPCPortEnv {
						expectENVs[i].Value = strconv.FormatInt(int64(testRPCPort), 10)
					}
				}
				actualENVs := sts.Spec.Template.Spec.Containers[0].Env
				valid = reflect.DeepEqual(expectENVs, actualENVs)
				if !valid {
					actualEnvBody, _ := json.Marshal(actualENVs)
					expectEnvBody, _ := json.Marshal(expectENVs)
					err = fmt.Errorf("unexpected ENVs:\n%v,\nexpected:\n%v",
						string(actualEnvBody), string(expectEnvBody))
					return
				}

				// check ports
				expectPorts := []corev1.ContainerPort{
					{
						ContainerPort: testRPCPort,
						Protocol:      corev1.ProtocolTCP,
					},
					{
						ContainerPort: *rss.Spec.ShuffleServer.HTTPPort,
						Protocol:      corev1.ProtocolTCP,
					},
				}
				actualPorts := sts.Spec.Template.Spec.Containers[0].Ports
				valid = reflect.DeepEqual(expectPorts, actualPorts)
				if !valid {
					actualPortsBody, _ := json.Marshal(actualPorts)
					expectPortsBody, _ := json.Marshal(expectPorts)
					err = fmt.Errorf("unexpected Ports:\n%v,\nexpected:\n%v",
						string(actualPortsBody), string(expectPortsBody))
				}
				return
			},
		},
		{
			name: "set custom http port used by shuffle server",
			rss:  buildRssWithCustomHTTPPort(),
			IsValidSts: func(sts *appsv1.StatefulSet, rss *uniffleapi.RemoteShuffleService) (
				valid bool, err error) {
				// check envs
				expectENVs := buildCommonExpectedENVs(rss)
				for i := range expectENVs {
					if expectENVs[i].Name == controllerconstants.ShuffleServerHTTPPortEnv {
						expectENVs[i].Value = strconv.FormatInt(int64(testHTTPPort), 10)
					}
				}
				actualENVs := sts.Spec.Template.Spec.Containers[0].Env
				valid = reflect.DeepEqual(expectENVs, actualENVs)
				if !valid {
					actualEnvBody, _ := json.Marshal(actualENVs)
					expectEnvBody, _ := json.Marshal(expectENVs)
					err = fmt.Errorf("unexpected ENVs:\n%v,\nexpected:\n%v",
						string(actualEnvBody), string(expectEnvBody))
					return
				}

				// check ports
				expectPorts := []corev1.ContainerPort{
					{
						ContainerPort: *rss.Spec.ShuffleServer.RPCPort,
						Protocol:      corev1.ProtocolTCP,
					},
					{
						ContainerPort: testHTTPPort,
						Protocol:      corev1.ProtocolTCP,
					},
				}
				actualPorts := sts.Spec.Template.Spec.Containers[0].Ports
				valid = reflect.DeepEqual(expectPorts, actualPorts)
				if !valid {
					actualPortsBody, _ := json.Marshal(actualPorts)
					expectPortsBody, _ := json.Marshal(expectPorts)
					err = fmt.Errorf("unexpected Ports:\n%v,\nexpected:\n%v",
						string(actualPortsBody), string(expectPortsBody))
				}
				return
			},
		},
		{
			name: "test custom volumes",
			rss:  withCustomVolumes(testVolumes),
			IsValidSts: func(sts *appsv1.StatefulSet, rss *uniffleapi.RemoteShuffleService) (valid bool, err error) {
				for _, volume := range sts.Spec.Template.Spec.Volumes {
					if volume.Name == testVolumeName {
						expectedVolume := testVolumes[0]
						equal := reflect.DeepEqual(expectedVolume, volume)
						if equal {
							return true, nil
						}
						volumeJSON, _ := json.Marshal(expectedVolume)
						return false, fmt.Errorf("generated sts doesn't contain expected volumn: %s", volumeJSON)
					}
				}
				return false, fmt.Errorf("generated sts should include volume: %s", testVolumeName)
			},
		},
		{
			name: "test custom tolerations",
			rss:  withCustomTolerations(testTolerations),
			IsValidSts: func(sts *appsv1.StatefulSet, rss *uniffleapi.RemoteShuffleService) (valid bool, err error) {
				for _, toleration := range sts.Spec.Template.Spec.Tolerations {
					if toleration.Key == testTolerationName {
						expectedToleration := testTolerations[0]
						equal := reflect.DeepEqual(expectedToleration, toleration)
						if equal {
							return true, nil
						}
						tolerationJSON, _ := json.Marshal(expectedToleration)
						return false, fmt.Errorf("generated sts doesn't contain expected toleration: %s", tolerationJSON)
					}
				}
				return false, fmt.Errorf("generated sts should include toleration: %s", testTolerationName)
			},
		},
		{
			name: "test custom affinity",
			rss:  withCustomAffinity(testAffinity),
			IsValidSts: func(sts *appsv1.StatefulSet, rss *uniffleapi.RemoteShuffleService) (valid bool, err error) {
				if sts.Spec.Template.Spec.Affinity != nil {
					sts.Spec.Template.Spec.Affinity = rss.Spec.ShuffleServer.Affinity
					equal := reflect.DeepEqual(sts.Spec.Template.Spec.Affinity, testAffinity)
					if equal {
						return true, nil
					}
				}
				return false, fmt.Errorf("generated sts should include affinity: %v", testAffinity)
			},
		},
	} {
		t.Run(tt.name, func(tc *testing.T) {
			sts := GenerateSts(tt.rss)
			if valid, err := tt.IsValidSts(sts, tt.rss); !valid {
				tc.Error(err)
			}
		})
	}
}
