// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package k8s

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func newServiceAccount(name, namespace string, imageSecrets []string) *corev1.ServiceAccount {
	sa := &corev1.ServiceAccount{
		TypeMeta: metav1.TypeMeta{
			Kind:       "ServiceAccount",
			APIVersion: "v1",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      name,
			Namespace: namespace,
		},
	}

	for _, is := range imageSecrets {
		sa.ImagePullSecrets = append(sa.ImagePullSecrets, corev1.LocalObjectReference{
			Name: is,
		})
	}

	return sa
}

func (k *Kubernetes) updateDefaultServiceAccountForImageSecret(namespace, secretName string) error {
	var err error

	// Try to create first, then update after failure
	// k8s will automatically create the default serviceaccount, but there will be a delay, resulting in failure to update the probability.
	if err = k.sa.Create(newServiceAccount(defaultServiceAccountName, namespace, []string{secretName})); err != nil {
		for {
			serviceaccount, err := k.sa.Get(namespace, defaultServiceAccountName)
			if err != nil {
				return err
			}

			serviceaccount.ImagePullSecrets = append(serviceaccount.ImagePullSecrets, corev1.LocalObjectReference{
				Name: secretName,
			})
			err = k.sa.Patch(serviceaccount)
			if err == nil {
				break
			}
			if err.Error() != "Conflict" {
				return err
			}
		}
	}

	return nil
}
