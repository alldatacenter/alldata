/*
Copyright 2022 The Koordinator Authors.
Copyright 2019 The Kubernetes Authors.

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

package scheduling

import (
	"context"
	"time"

	"github.com/onsi/ginkgo"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	imageutils "k8s.io/kubernetes/test/utils/image"

	"github.com/koordinator-sh/koordinator/test/e2e/framework"
	e2epod "github.com/koordinator-sh/koordinator/test/e2e/framework/pod"
	e2ereplicaset "github.com/koordinator-sh/koordinator/test/e2e/framework/replicaset"
)

type pausePodConfig struct {
	Name                              string
	Namespace                         string
	Affinity                          *corev1.Affinity
	Annotations, Labels, NodeSelector map[string]string
	Resources                         *corev1.ResourceRequirements
	RuntimeClassHandler               *string
	Tolerations                       []corev1.Toleration
	NodeName                          string
	Ports                             []corev1.ContainerPort
	OwnerReferences                   []metav1.OwnerReference
	PriorityClassName                 string
	DeletionGracePeriodSeconds        *int64
	TopologySpreadConstraints         []corev1.TopologySpreadConstraint
	SchedulerName                     string
}

func initPausePod(f *framework.Framework, conf pausePodConfig) *corev1.Pod {
	var gracePeriod = int64(1)
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:            conf.Name,
			Namespace:       conf.Namespace,
			Labels:          map[string]string{},
			Annotations:     map[string]string{},
			OwnerReferences: conf.OwnerReferences,
		},
		Spec: corev1.PodSpec{
			NodeSelector:              conf.NodeSelector,
			Affinity:                  conf.Affinity,
			TopologySpreadConstraints: conf.TopologySpreadConstraints,
			RuntimeClassName:          conf.RuntimeClassHandler,
			Containers: []corev1.Container{
				{
					Name:  conf.Name,
					Image: imageutils.GetPauseImageName(),
					Ports: conf.Ports,
				},
			},
			Tolerations:                   conf.Tolerations,
			PriorityClassName:             conf.PriorityClassName,
			TerminationGracePeriodSeconds: &gracePeriod,
			SchedulerName:                 conf.SchedulerName,
		},
	}
	for key, value := range conf.Labels {
		pod.ObjectMeta.Labels[key] = value
	}
	for key, value := range conf.Annotations {
		pod.ObjectMeta.Annotations[key] = value
	}
	// TODO: setting the Pod's nodeAffinity instead of setting .spec.nodeName works around the
	// Preemption e2e flake (#88441), but we should investigate deeper to get to the bottom of it.
	if len(conf.NodeName) != 0 {
		e2epod.SetNodeAffinity(&pod.Spec, conf.NodeName)
	}
	if conf.Resources != nil {
		pod.Spec.Containers[0].Resources = *conf.Resources
	}
	if conf.DeletionGracePeriodSeconds != nil {
		pod.ObjectMeta.DeletionGracePeriodSeconds = conf.DeletionGracePeriodSeconds
	}
	return pod
}

func createPausePod(f *framework.Framework, conf pausePodConfig) *corev1.Pod {
	namespace := conf.Namespace
	if len(namespace) == 0 {
		namespace = f.Namespace.Name
	}
	pod, err := f.ClientSet.CoreV1().Pods(namespace).Create(context.TODO(), initPausePod(f, conf), metav1.CreateOptions{})
	framework.ExpectNoError(err)
	return pod
}

func runPausePod(f *framework.Framework, conf pausePodConfig) *corev1.Pod {
	return runPausePodWithTimeout(f, conf, framework.PollShortTimeout)
}

func runPausePodWithTimeout(f *framework.Framework, conf pausePodConfig, timeout time.Duration) *corev1.Pod {
	pod := createPausePod(f, conf)
	framework.ExpectNoError(e2epod.WaitTimeoutForPodRunningInNamespace(f.ClientSet, pod.Name, pod.Namespace, timeout))
	pod, err := f.ClientSet.CoreV1().Pods(pod.Namespace).Get(context.TODO(), conf.Name, metav1.GetOptions{})
	framework.ExpectNoError(err)
	return pod
}

func runPodAndGetNodeName(f *framework.Framework, conf pausePodConfig) string {
	// launch a pod to find a node which can launch a pod. We intentionally do
	// not just take the node list and choose the first of them. Depending on the
	// cluster and the scheduler it might be that a "normal" pod cannot be
	// scheduled onto it.
	pod := runPausePod(f, conf)

	ginkgo.By("Explicitly delete pod here to free the resource it takes.")
	err := f.ClientSet.CoreV1().Pods(f.Namespace.Name).Delete(context.TODO(), pod.Name, *metav1.NewDeleteOptions(0))
	framework.ExpectNoError(err)

	return pod.Spec.NodeName
}

// GetNodeThatCanRunPod trying to launch a pod without a label to get a node which can launch it
func GetNodeThatCanRunPod(f *framework.Framework) string {
	ginkgo.By("Trying to launch a pod without a label to get a node which can launch it.")
	return runPodAndGetNodeName(f, pausePodConfig{Name: "without-label"})
}

// Get2NodesThatCanRunPod return a 2-node slice where can run pod.
func Get2NodesThatCanRunPod(f *framework.Framework) []string {
	firstNode := GetNodeThatCanRunPod(f)
	ginkgo.By("Trying to launch a pod without a label to get a node which can launch it.")
	pod := pausePodConfig{
		Name: "without-label",
		Affinity: &corev1.Affinity{
			NodeAffinity: &corev1.NodeAffinity{
				RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
					NodeSelectorTerms: []corev1.NodeSelectorTerm{
						{
							MatchFields: []corev1.NodeSelectorRequirement{
								{Key: "metadata.name", Operator: corev1.NodeSelectorOpNotIn, Values: []string{firstNode}},
							},
						},
					},
				},
			},
		},
	}
	secondNode := runPodAndGetNodeName(f, pod)
	return []string{firstNode, secondNode}
}

type pauseRSConfig struct {
	Replicas  int32
	PodConfig pausePodConfig
}

func initPauseRS(f *framework.Framework, conf pauseRSConfig) *appsv1.ReplicaSet {
	pausePod := initPausePod(f, conf.PodConfig)
	pauseRS := &appsv1.ReplicaSet{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "rs-" + pausePod.Name,
			Namespace: pausePod.Namespace,
		},
		Spec: appsv1.ReplicaSetSpec{
			Replicas: &conf.Replicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: pausePod.Labels,
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{Labels: pausePod.ObjectMeta.Labels},
				Spec:       pausePod.Spec,
			},
		},
	}
	return pauseRS
}

func createPauseRS(f *framework.Framework, conf pauseRSConfig) *appsv1.ReplicaSet {
	namespace := conf.PodConfig.Namespace
	if len(namespace) == 0 {
		namespace = f.Namespace.Name
	}
	rs, err := f.ClientSet.AppsV1().ReplicaSets(namespace).Create(context.TODO(), initPauseRS(f, conf), metav1.CreateOptions{})
	framework.ExpectNoError(err)
	return rs
}

func runPauseRS(f *framework.Framework, conf pauseRSConfig) *appsv1.ReplicaSet {
	rs := createPauseRS(f, conf)
	framework.ExpectNoError(e2ereplicaset.WaitForReplicaSetTargetAvailableReplicasWithTimeout(f.ClientSet, rs, conf.Replicas, framework.PodGetTimeout))
	return rs
}
