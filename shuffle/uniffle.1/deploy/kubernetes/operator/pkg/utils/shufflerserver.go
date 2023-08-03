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

package utils

import (
	"strings"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
)

// GetShuffleServerNode returns shuffle server node name by pod.
func GetShuffleServerNode(pod *corev1.Pod) string {
	return pod.Status.PodIP + ":" + GetShuffleServerPort(pod)
}

// GetShuffleServerPort returns shuffle server port by pod.
func GetShuffleServerPort(pod *corev1.Pod) string {
	return pod.Annotations[constants.AnnotationShuffleServerPort]
}

// GetMetricsServerPort returns metrics server port by pod.
func GetMetricsServerPort(pod *corev1.Pod) string {
	return pod.Annotations[constants.AnnotationMetricsServerPort]
}

// BuildShuffleServerKey returns shuffler server key used in rss object's status.
func BuildShuffleServerKey(pod *corev1.Pod) string {
	return GetRevisionFromPod(pod) + "/" + pod.Name + "/" + GetShuffleServerNode(pod)
}

// ParseShuffleServerKey parses shuffler server key used in rss object's status.
func ParseShuffleServerKey(key string) (revision, podName, node string) {
	values := strings.Split(key, "/")
	if len(values) == 3 {
		revision = values[0]
		podName = values[1]
		node = values[2]
	}
	return
}

// ConvertShuffleServerKeysToNodes converts shuffle server keys to nodes.
func ConvertShuffleServerKeysToNodes(keys sets.String) sets.String {
	values := keys.List()
	nodes := sets.NewString()
	for _, v := range values {
		_, _, node := ParseShuffleServerKey(v)
		nodes.Insert(node)
	}
	return nodes
}

// GetRevisionFromPod returns revision of the pod belongs to a statefulSet.
func GetRevisionFromPod(pod *corev1.Pod) string {
	return pod.Labels[appsv1.ControllerRevisionHashLabelKey]
}

// GenerateShuffleServerName returns workload or nodePort service name of shuffle servers.
func GenerateShuffleServerName(rss *unifflev1alpha1.RemoteShuffleService) string {
	return constants.RSSShuffleServer + "-" + rss.Name
}

// GenerateShuffleServerLabels returns labels used by statefulSets or pods of shuffle servers.
func GenerateShuffleServerLabels(rss *unifflev1alpha1.RemoteShuffleService) map[string]string {
	return map[string]string{
		"app":                        GenerateShuffleServerName(rss),
		constants.LabelShuffleServer: "true",
	}
}

// BuildShuffleServerInformerFactory builds an informer factory for shuffle servers.
func BuildShuffleServerInformerFactory(kubeClient kubernetes.Interface) informers.SharedInformerFactory {
	option := func(options *metav1.ListOptions) {
		options.LabelSelector = constants.LabelShuffleServer + "=true"
	}
	return informers.NewSharedInformerFactoryWithOptions(
		kubeClient, 0, informers.WithTweakListOptions(option))
}
