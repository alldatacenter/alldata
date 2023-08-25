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

package reservation

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/equality"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/uuid"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

func TestBeforePreFilter(t *testing.T) {
	node := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node",
		},
	}
	suit := newPluginTestSuitWith(t, nil, []*corev1.Node{node})
	p, err := suit.pluginFactory()
	assert.NoError(t, err)
	pl := p.(*Plugin)

	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			UID:       uuid.NewUUID(),
			Namespace: "default",
			Name:      "test-pod",
		},
	}

	matchedReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "matchedReservation",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						UID:       pod.UID,
						Namespace: pod.Namespace,
						Name:      pod.Name,
					},
				},
			},
			Template: &corev1.PodTemplateSpec{},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
		},
	}
	pl.reservationCache.updateReservation(matchedReservation)

	unmatchedReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "unmatchedReservation",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						UID: uuid.NewUUID(),
					},
				},
			},
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("2"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
			}},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
		},
	}
	pl.reservationCache.updateReservation(unmatchedReservation)
	pl.reservationCache.assumePod(unmatchedReservation.UID, &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			UID:       uuid.NewUUID(),
			Namespace: "default",
			Name:      "pod-1",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("2"),
							corev1.ResourceMemory: resource.MustParse("4Gi"),
						},
					},
				},
			},
		},
	})

	cycleState := framework.NewCycleState()
	pl.BeforePreFilter(nil, cycleState, pod)
	expectState := &stateData{
		matched: map[string][]*reservationInfo{
			"test-node": {
				pl.reservationCache.getReservationInfoByUID(matchedReservation.UID),
			},
		},
		unmatched: map[string][]*reservationInfo{
			"test-node": {
				pl.reservationCache.getReservationInfoByUID(unmatchedReservation.UID),
			},
		},
	}
	assert.Equal(t, expectState, getStateData(cycleState))
}

func TestAfterPreFilter(t *testing.T) {
	node := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node",
		},
		Status: corev1.NodeStatus{
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("32"),
				corev1.ResourceMemory: resource.MustParse("64Gi"),
			},
		},
	}
	// normal pods allocated 12C24Gi and ports 8080/9090
	pods := []*corev1.Pod{
		{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: "default",
				Name:      "pod-1",
				UID:       uuid.NewUUID(),
			},
			Spec: corev1.PodSpec{
				NodeName: node.Name,
				Affinity: &corev1.Affinity{
					PodAntiAffinity: &corev1.PodAntiAffinity{
						RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
							{
								LabelSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"app": "test-app-1",
									},
								},
								TopologyKey: corev1.LabelHostname,
							},
						},
					},
				},
				Containers: []corev1.Container{
					{
						Resources: corev1.ResourceRequirements{
							Requests: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("4"),
								corev1.ResourceMemory: resource.MustParse("8Gi"),
							},
						},
						Ports: []corev1.ContainerPort{
							{
								HostIP:   "0.0.0.0",
								Protocol: corev1.ProtocolTCP,
								HostPort: 8080,
							},
						},
					},
				},
			},
		},
		{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: "default",
				Name:      "pod-2",
				UID:       uuid.NewUUID(),
			},
			Spec: corev1.PodSpec{
				NodeName: node.Name,
				Affinity: &corev1.Affinity{
					PodAntiAffinity: &corev1.PodAntiAffinity{
						RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
							{
								LabelSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"app": "test-app-2",
									},
								},
								TopologyKey: corev1.LabelHostname,
							},
						},
					},
				},
				Containers: []corev1.Container{
					{
						Resources: corev1.ResourceRequirements{
							Requests: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("8"),
								corev1.ResourceMemory: resource.MustParse("16Gi"),
							},
						},
						Ports: []corev1.ContainerPort{
							{
								HostIP:   "0.0.0.0",
								Protocol: corev1.ProtocolTCP,
								HostPort: 9090,
							},
						},
					},
				},
			},
		},
	}

	// unmatched reservation allocated 12C24Gi, but assigned Pod with 4C8Gi, remaining 8C16Gi
	unmatchedReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "reservation4C8G",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Affinity: &corev1.Affinity{
						PodAntiAffinity: &corev1.PodAntiAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
								{
									LabelSelector: &metav1.LabelSelector{
										MatchLabels: map[string]string{
											"app": "test-app-2",
										},
									},
									TopologyKey: corev1.LabelHostname,
								},
							},
						},
					},
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("12"),
									corev1.ResourceMemory: resource.MustParse("24Gi"),
								},
							},
							Ports: []corev1.ContainerPort{
								{
									HostIP:   "0.0.0.0",
									Protocol: corev1.ProtocolTCP,
									HostPort: 8080,
								},
							},
						},
					},
				},
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
		},
	}
	pods = append(pods, reservationutil.NewReservePod(unmatchedReservation))

	// matchedReservation allocated 8C16Gi
	matchedReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "reservation2C4G",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Affinity: &corev1.Affinity{
						PodAntiAffinity: &corev1.PodAntiAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
								{
									LabelSelector: &metav1.LabelSelector{
										MatchLabels: map[string]string{
											"app": "test-app-3",
										},
									},
									TopologyKey: corev1.LabelHostname,
								},
							},
						},
					},
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("8"),
									corev1.ResourceMemory: resource.MustParse("16Gi"),
								},
							},
							Ports: []corev1.ContainerPort{
								{
									HostIP:   "0.0.0.0",
									Protocol: corev1.ProtocolTCP,
									HostPort: 7070,
								},
							},
						},
					},
				},
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
		},
	}
	pods = append(pods, reservationutil.NewReservePod(matchedReservation))

	suit := newPluginTestSuitWith(t, pods, []*corev1.Node{node})
	p, err := suit.pluginFactory()
	assert.NoError(t, err)
	pl := p.(*Plugin)

	nodeInfo, err := suit.fw.SnapshotSharedLister().NodeInfos().Get(node.Name)
	assert.NoError(t, err)
	expectedRequestedResources := &framework.Resource{
		MilliCPU: 32000,
		Memory:   64 * 1024 * 1024 * 1024,
	}
	assert.Equal(t, expectedRequestedResources, nodeInfo.Requested)

	pl.reservationCache.updateReservation(unmatchedReservation)
	pl.reservationCache.updateReservation(matchedReservation)

	unmatchedRInfo := pl.reservationCache.getReservationInfoByUID(unmatchedReservation.UID)
	unmatchedRInfo.allocated = corev1.ResourceList{
		corev1.ResourceCPU:    resource.MustParse("4"),
		corev1.ResourceMemory: resource.MustParse("8Gi"),
	}

	matchRInfo := pl.reservationCache.getReservationInfoByUID(matchedReservation.UID)

	cycleState := framework.NewCycleState()
	cycleState.Write(stateKey, &stateData{
		matched: map[string][]*reservationInfo{
			node.Name: {matchRInfo},
		},
		unmatched: map[string][]*reservationInfo{
			node.Name: {unmatchedRInfo},
		},
	})

	err = pl.AfterPreFilter(nil, cycleState, &corev1.Pod{})
	assert.NoError(t, err)

	nodeInfo, err = suit.fw.SnapshotSharedLister().NodeInfos().Get(node.Name)
	assert.NoError(t, err)
	assert.NotNil(t, nodeInfo)
	nodeInfo.Generation = 0

	unmatchedReservePod := pods[2].DeepCopy()
	unmatchedReservePod.Spec.Containers[0].Resources.Requests = corev1.ResourceList{}
	unmatchedReservePod.Spec.Containers = append(unmatchedReservePod.Spec.Containers, corev1.Container{
		Resources: corev1.ResourceRequirements{
			Requests: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("8000m"),
				corev1.ResourceMemory: resource.MustParse("16Gi"),
			},
		},
	})
	expectNodeInfo := framework.NewNodeInfo(pods[0], pods[1], unmatchedReservePod)
	expectNodeInfo.SetNode(node)
	expectNodeInfo.Generation = 0
	assert.Equal(t, expectNodeInfo.Requested, nodeInfo.Requested)
	assert.Equal(t, expectNodeInfo.UsedPorts, nodeInfo.UsedPorts)
	assert.True(t, equality.Semantic.DeepEqual(expectNodeInfo, nodeInfo))
}

func TestBeforeFilter(t *testing.T) {
	node := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node",
		},
		Status: corev1.NodeStatus{
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("32"),
				corev1.ResourceMemory: resource.MustParse("64Gi"),
			},
		},
	}
	// normal pods allocated 12C24Gi and ports 8080/9090
	pods := []*corev1.Pod{
		{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: "default",
				Name:      "pod-1",
				UID:       uuid.NewUUID(),
			},
			Spec: corev1.PodSpec{
				NodeName: node.Name,
				Affinity: &corev1.Affinity{
					PodAntiAffinity: &corev1.PodAntiAffinity{
						RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
							{
								LabelSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"app": "test-app-1",
									},
								},
								TopologyKey: corev1.LabelHostname,
							},
						},
					},
				},
				Containers: []corev1.Container{
					{
						Resources: corev1.ResourceRequirements{
							Requests: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("4"),
								corev1.ResourceMemory: resource.MustParse("8Gi"),
							},
						},
						Ports: []corev1.ContainerPort{
							{
								HostIP:   "0.0.0.0",
								Protocol: corev1.ProtocolTCP,
								HostPort: 8080,
							},
						},
					},
				},
			},
		},
		{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: "default",
				Name:      "pod-2",
				UID:       uuid.NewUUID(),
			},
			Spec: corev1.PodSpec{
				NodeName: node.Name,
				Affinity: &corev1.Affinity{
					PodAntiAffinity: &corev1.PodAntiAffinity{
						RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
							{
								LabelSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"app": "test-app-2",
									},
								},
								TopologyKey: corev1.LabelHostname,
							},
						},
					},
				},
				Containers: []corev1.Container{
					{
						Resources: corev1.ResourceRequirements{
							Requests: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("8"),
								corev1.ResourceMemory: resource.MustParse("16Gi"),
							},
						},
						Ports: []corev1.ContainerPort{
							{
								HostIP:   "0.0.0.0",
								Protocol: corev1.ProtocolTCP,
								HostPort: 9090,
							},
						},
					},
				},
			},
		},
	}

	// unmatched reservation allocated 12C24Gi, but assigned Pod with 4C8Gi, remaining 8C16Gi
	unmatchedReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "reservation4C8G",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Affinity: &corev1.Affinity{
						PodAntiAffinity: &corev1.PodAntiAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
								{
									LabelSelector: &metav1.LabelSelector{
										MatchLabels: map[string]string{
											"app": "test-app-2",
										},
									},
									TopologyKey: corev1.LabelHostname,
								},
							},
						},
					},
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("12"),
									corev1.ResourceMemory: resource.MustParse("24Gi"),
								},
							},
							Ports: []corev1.ContainerPort{
								{
									HostIP:   "0.0.0.0",
									Protocol: corev1.ProtocolTCP,
									HostPort: 8080,
								},
							},
						},
					},
				},
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
		},
	}
	pods = append(pods, reservationutil.NewReservePod(unmatchedReservation))

	// matchedReservation allocated 8C16Gi
	matchedReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "reservation2C4G",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Affinity: &corev1.Affinity{
						PodAntiAffinity: &corev1.PodAntiAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
								{
									LabelSelector: &metav1.LabelSelector{
										MatchLabels: map[string]string{
											"app": "test-app-3",
										},
									},
									TopologyKey: corev1.LabelHostname,
								},
							},
						},
					},
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("8"),
									corev1.ResourceMemory: resource.MustParse("16Gi"),
								},
							},
							Ports: []corev1.ContainerPort{
								{
									HostIP:   "0.0.0.0",
									Protocol: corev1.ProtocolTCP,
									HostPort: 7070,
								},
							},
						},
					},
				},
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
		},
	}
	pods = append(pods, reservationutil.NewReservePod(matchedReservation))

	suit := newPluginTestSuitWith(t, pods, []*corev1.Node{node})
	p, err := suit.pluginFactory()
	assert.NoError(t, err)
	pl := p.(*Plugin)

	nodeInfo, err := suit.fw.SnapshotSharedLister().NodeInfos().Get(node.Name)
	assert.NoError(t, err)
	expectedRequestedResources := &framework.Resource{
		MilliCPU: 32000,
		Memory:   64 * 1024 * 1024 * 1024,
	}
	assert.Equal(t, expectedRequestedResources, nodeInfo.Requested)

	pl.reservationCache.updateReservation(unmatchedReservation)
	pl.reservationCache.updateReservation(matchedReservation)

	unmatchedRInfo := pl.reservationCache.getReservationInfoByUID(unmatchedReservation.UID)
	unmatchedRInfo.allocated = corev1.ResourceList{
		corev1.ResourceCPU:    resource.MustParse("4"),
		corev1.ResourceMemory: resource.MustParse("8Gi"),
	}

	matchRInfo := pl.reservationCache.getReservationInfoByUID(matchedReservation.UID)

	cycleState := framework.NewCycleState()
	cycleState.Write(stateKey, &stateData{
		matched: map[string][]*reservationInfo{
			node.Name: {matchRInfo},
		},
		unmatched: map[string][]*reservationInfo{
			node.Name: {unmatchedRInfo},
		},
	})

	nodeInfo, err = suit.fw.SnapshotSharedLister().NodeInfos().Get(node.Name)
	assert.NoError(t, err)
	assert.NotNil(t, nodeInfo)
	nodeInfo.Generation = 0

	pod, newNodeInfo, transformed := pl.BeforeFilter(nil, cycleState, &corev1.Pod{}, nodeInfo)
	assert.True(t, transformed)
	assert.NotNil(t, newNodeInfo)
	assert.NotNil(t, pod)

	unmatchedReservePod := pods[2].DeepCopy()
	unmatchedReservePod.Spec.Containers[0].Resources.Requests = corev1.ResourceList{}
	unmatchedReservePod.Spec.Containers = append(unmatchedReservePod.Spec.Containers, corev1.Container{
		Resources: corev1.ResourceRequirements{
			Requests: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("8000m"),
				corev1.ResourceMemory: resource.MustParse("16Gi"),
			},
		},
	})
	expectNodeInfo := framework.NewNodeInfo(pods[0], pods[1], unmatchedReservePod)
	expectNodeInfo.SetNode(node)
	expectNodeInfo.Generation = 0
	newNodeInfo.Generation = 0
	assert.Equal(t, expectNodeInfo.Requested, newNodeInfo.Requested)
	assert.Equal(t, expectNodeInfo.UsedPorts, newNodeInfo.UsedPorts)
	assert.True(t, equality.Semantic.DeepEqual(expectNodeInfo, newNodeInfo))
}

func Test_restorePVCRefCounts(t *testing.T) {
	normalPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
		},
		Spec: corev1.PodSpec{
			Volumes: []corev1.Volume{
				{
					VolumeSource: corev1.VolumeSource{
						PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{
							ClaimName: "claim-with-rwop",
						},
					},
				},
			},
		},
	}
	readWriteOncePodPVC := &corev1.PersistentVolumeClaim{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "claim-with-rwop",
		},
		Spec: corev1.PersistentVolumeClaimSpec{
			AccessModes: []corev1.PersistentVolumeAccessMode{corev1.ReadWriteOncePod},
		},
	}

	testNodeName := "test-node-0"
	testNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: testNodeName,
		},
	}
	testNodeInfo := framework.NewNodeInfo()
	testNodeInfo.SetNode(testNode)
	testNodeInfo.AddPod(normalPod)
	testNodeInfo.PodsWithRequiredAntiAffinity = []*framework.PodInfo{
		framework.NewPodInfo(normalPod),
	}
	assert.Equal(t, 1, testNodeInfo.PVCRefCounts["default/claim-with-rwop"])

	reservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "reserve-pod-1",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-1",
				},
				Spec: corev1.PodSpec{
					Volumes: []corev1.Volume{
						{
							VolumeSource: corev1.VolumeSource{
								PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{
									ClaimName: "claim-with-rwop",
								},
							},
						},
					},
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Name: "test-pod-1",
					},
				},
			},
			TTL: &metav1.Duration{Duration: 30 * time.Minute},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: testNodeName,
		},
	}

	cs := kubefake.NewSimpleClientset(readWriteOncePodPVC)
	informerFactory := informers.NewSharedInformerFactory(cs, 0)
	_ = informerFactory.Core().V1().PersistentVolumeClaims().Lister()

	informerFactory.Start(nil)
	informerFactory.WaitForCacheSync(nil)

	cache := newReservationCache(nil)
	cache.updateReservation(reservation)
	rInfo := cache.getReservationInfoByUID(reservation.UID)

	restorePVCRefCounts(informerFactory, testNodeInfo, normalPod, []*reservationInfo{rInfo})
	assert.Zero(t, testNodeInfo.PVCRefCounts["default/claim-with-rwop"])
}
