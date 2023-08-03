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
	"context"
	"fmt"
	"os"
	"sort"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/util/retry"
	"k8s.io/klog/v2"
	"k8s.io/utils/strings/slices"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
)

// GetCurrentNamespace returns current namespace.
func GetCurrentNamespace() string {
	namespace := os.Getenv(constants.PodNamespaceEnv)
	if namespace == "" {
		namespace = constants.DefaultNamespace
	}
	return namespace
}

// UpdateSecret updates Secret and retries when conflicts are encountered.
func UpdateSecret(kubeClient kubernetes.Interface, namespace, secretName string,
	updateFunc func(secret *corev1.Secret)) error {
	return retry.RetryOnConflict(retry.DefaultRetry, func() error {
		var err error
		var secret *corev1.Secret
		secret, err = kubeClient.CoreV1().Secrets(namespace).
			Get(context.Background(), secretName, metav1.GetOptions{})
		if err != nil {
			klog.Errorf("get secret %v/%v failed: %v", namespace, secretName, err)
			return err
		}
		updateFunc(secret)
		_, err = kubeClient.CoreV1().Secrets(namespace).Update(context.Background(), secret,
			metav1.UpdateOptions{})
		if err != nil {
			klog.Errorf("update configMap %v/%v failed: %v", namespace, secretName, err)
		}
		return err
	})
}

// UniqueName returns unique name of an object.
func UniqueName(object metav1.Object) string {
	return fmt.Sprintf("%v/%v/%v", object.GetNamespace(), object.GetName(), object.GetUID())
}

// GetRssNameByPod returns rss object name from a pod.
func GetRssNameByPod(pod *corev1.Pod) string {
	return pod.Annotations[constants.AnnotationRssName]
}

// GetSortedList returns sorted slice.
func GetSortedList(values sets.String) []string {
	sorted := slices.Filter(nil, values.List(), func(v string) bool {
		return len(v) > 0
	})
	sort.Slice(sorted, func(i, j int) bool {
		return sorted[i] < sorted[j]
	})
	return sorted
}
