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

package syncer

import (
	"context"
	"reflect"
	"time"

	"golang.org/x/sync/errgroup"
	admissionv1 "k8s.io/api/admissionregistration/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/manager"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
	webhookconstants "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/constants"
)

var _ ConfigSyncer = &configSyncer{}

// ConfigSyncer syncs ValidatingWebhookConfigurations and MutatingWebhookConfigurations.
type ConfigSyncer interface {
	manager.Runnable
}

// NewConfigSyncer creates a ConfigSyncer.
func NewConfigSyncer(caCert []byte, externalService string,
	kubeClient kubernetes.Interface) ConfigSyncer {
	return newConfigSyncer(caCert, externalService, kubeClient)
}

// newConfigSyncer creates a configSyncer.
func newConfigSyncer(caCert []byte, externalService string,
	kubeClient kubernetes.Interface) *configSyncer {
	return &configSyncer{
		caCert:          caCert,
		externalService: externalService,
		kubeClient:      kubeClient,
	}
}

// configSyncer implements the ConfigSyncer interface.
type configSyncer struct {
	caCert          []byte
	externalService string
	kubeClient      kubernetes.Interface
}

// Start starts the ConfigSyncer.
func (cs *configSyncer) Start(ctx context.Context) error {
	klog.V(2).Info("config syncer started")
	for {
		select {
		case <-ctx.Done():
			klog.V(3).Info("stop syncing webhook configurations")
			return nil
		default:
		}
		if err := cs.syncWebhookCfg(); err != nil {
			klog.Errorf("sync webhook configuration failed: %v", err)
		}
		time.Sleep(time.Minute)
	}
}

// syncWebhookCfg synchronizes the validatingWebhookConfiguration and mutatingWebhookConfiguration objects.
func (cs *configSyncer) syncWebhookCfg() error {
	currentVWC, currentMWC := cs.generateWebhookCfg()
	eg := errgroup.Group{}
	eg.Go(func() error {
		return cs.syncValidatingWebhookCfg(currentVWC)
	})
	eg.Go(func() error {
		return cs.syncMutatingWebhookCfg(currentMWC)
	})
	return eg.Wait()
}

// syncValidatingWebhookCfg synchronizes the validatingWebhookConfiguration object.
func (cs *configSyncer) syncValidatingWebhookCfg(
	currentVWC *admissionv1.ValidatingWebhookConfiguration) error {
	vwc, err := cs.kubeClient.AdmissionregistrationV1().ValidatingWebhookConfigurations().
		Get(context.Background(), webhookconstants.ComponentName, metav1.GetOptions{})
	if err != nil {
		if errors.IsNotFound(err) {
			_, err = cs.kubeClient.AdmissionregistrationV1().ValidatingWebhookConfigurations().
				Create(context.Background(), currentVWC, metav1.CreateOptions{})
		}
		return err
	}
	if reflect.DeepEqual(vwc.Webhooks, currentVWC.Webhooks) {
		return nil
	}
	vwc.Webhooks = currentVWC.Webhooks
	_, err = cs.kubeClient.AdmissionregistrationV1().ValidatingWebhookConfigurations().
		Update(context.Background(), vwc, metav1.UpdateOptions{})
	return err
}

// syncMutatingWebhookCfg synchronizes the mutatingWebhookConfiguration object.
func (cs *configSyncer) syncMutatingWebhookCfg(
	currentMWC *admissionv1.MutatingWebhookConfiguration) error {
	vwc, err := cs.kubeClient.AdmissionregistrationV1().MutatingWebhookConfigurations().
		Get(context.Background(), webhookconstants.ComponentName, metav1.GetOptions{})
	if err != nil {
		if errors.IsNotFound(err) {
			_, err = cs.kubeClient.AdmissionregistrationV1().MutatingWebhookConfigurations().
				Create(context.Background(), currentMWC, metav1.CreateOptions{})
		}
		return err
	}
	if reflect.DeepEqual(vwc.Webhooks, currentMWC.Webhooks) {
		return nil
	}
	vwc.Webhooks = currentMWC.Webhooks
	_, err = cs.kubeClient.AdmissionregistrationV1().MutatingWebhookConfigurations().
		Update(context.Background(), vwc, metav1.UpdateOptions{})
	return err
}

// generateWebhookCfg generates the validatingWebhookConfiguration and mutatingWebhookConfiguration objects.
func (cs *configSyncer) generateWebhookCfg() (
	*admissionv1.ValidatingWebhookConfiguration, *admissionv1.MutatingWebhookConfiguration) {
	validatingWebhooks := cs.generateValidatingWebhooks()
	mutatingWebhooks := cs.generateMutatingWebhooks()
	return &admissionv1.ValidatingWebhookConfiguration{
			ObjectMeta: metav1.ObjectMeta{
				Name: webhookconstants.ComponentName,
			},
			Webhooks: validatingWebhooks,
		}, &admissionv1.MutatingWebhookConfiguration{
			ObjectMeta: metav1.ObjectMeta{
				Name: webhookconstants.ComponentName,
			},
			Webhooks: mutatingWebhooks,
		}
}

// generateValidatingWebhooks generates validatingWebhooks of validatingWebhookConfiguration.
func (cs *configSyncer) generateValidatingWebhooks() []admissionv1.ValidatingWebhook {
	failurePolicy := admissionv1.Fail
	sideEffects := admissionv1.SideEffectClassNone
	return []admissionv1.ValidatingWebhook{
		{
			Name: "webhook.for.shuffle.server",
			Rules: []admissionv1.RuleWithOperations{
				{
					Operations: []admissionv1.OperationType{admissionv1.Delete},
					Rule: admissionv1.Rule{
						APIGroups:   []string{corev1.GroupName},
						APIVersions: []string{"v1"},
						Resources:   []string{"pods"},
					},
				},
			},
			FailurePolicy: &failurePolicy,
			ClientConfig: admissionv1.WebhookClientConfig{
				CABundle: cs.caCert,
				Service: &admissionv1.ServiceReference{
					Name:      cs.externalService,
					Namespace: utils.GetCurrentNamespace(),
					Path:      pointer.StringPtr(webhookconstants.ValidatingPodPath),
				},
			},
			ObjectSelector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					constants.LabelShuffleServer: "true",
				},
			},
			SideEffects:             &sideEffects,
			AdmissionReviewVersions: []string{"v1", "v1beta1"},
			TimeoutSeconds:          pointer.Int32(30),
		},
		{
			Name:          "webhook.for.rss",
			Rules:         buildRules(),
			FailurePolicy: &failurePolicy,
			ClientConfig: admissionv1.WebhookClientConfig{
				CABundle: cs.caCert,
				Service: &admissionv1.ServiceReference{
					Name:      cs.externalService,
					Namespace: utils.GetCurrentNamespace(),
					Path:      pointer.StringPtr(webhookconstants.ValidatingRssPath),
				},
			},
			SideEffects:             &sideEffects,
			AdmissionReviewVersions: []string{"v1", "v1beta1"},
		},
	}
}

// generateMutatingWebhooks generates mutatingWebhooks of mutatingWebhookConfiguration.
func (cs *configSyncer) generateMutatingWebhooks() []admissionv1.MutatingWebhook {
	failurePolicy := admissionv1.Fail
	sideEffects := admissionv1.SideEffectClassNone
	return []admissionv1.MutatingWebhook{
		{
			Name:          "webhook.for.rss",
			Rules:         buildRules(),
			FailurePolicy: &failurePolicy,
			ClientConfig: admissionv1.WebhookClientConfig{
				CABundle: cs.caCert,
				Service: &admissionv1.ServiceReference{
					Name:      cs.externalService,
					Namespace: utils.GetCurrentNamespace(),
					Path:      pointer.StringPtr(webhookconstants.MutatingRssPath),
				},
			},
			SideEffects:             &sideEffects,
			AdmissionReviewVersions: []string{"v1", "v1beta1"},
		},
	}
}

func buildRules() []admissionv1.RuleWithOperations {
	return []admissionv1.RuleWithOperations{
		{
			Operations: []admissionv1.OperationType{admissionv1.Create, admissionv1.Update},
			Rule: admissionv1.Rule{
				APIGroups:   []string{unifflev1alpha1.SchemeGroupVersion.Group},
				APIVersions: []string{unifflev1alpha1.SchemeGroupVersion.Version},
				Resources:   []string{"remoteshuffleservices"},
			},
		},
	}
}
