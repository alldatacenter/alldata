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

package webhook

import (
	"context"
	"os"
	"path/filepath"
	"testing"
	"time"

	. "github.com/onsi/ginkgo/v2"
	. "github.com/onsi/gomega"
	admissionv1 "k8s.io/api/admissionregistration/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/kubernetes/scheme"
	"sigs.k8s.io/controller-runtime/pkg/envtest"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/log/zap"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/clientset/versioned"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/config"
	webhookconstants "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/constants"
)

var (
	testEnv    *envtest.Environment
	kubeClient kubernetes.Interface
	rssClient  versioned.Interface
	stopCtx    context.Context
	ctxCancel  context.CancelFunc
)

func TestAdmissionManager(t *testing.T) {
	_ = os.Setenv(constants.PodNamespaceEnv, constants.DefaultNamespace)
	RegisterFailHandler(Fail)
	suiteCfg, reporterCfg := GinkgoConfiguration()
	reporterCfg.VeryVerbose = true
	reporterCfg.FullTrace = true
	RunSpecs(t, "admission manager suite", suiteCfg, reporterCfg)
}

var _ = BeforeSuite(
	func() {
		logf.SetLogger(zap.New(zap.WriteTo(GinkgoWriter), zap.UseDevMode(true)))
		By("bootstrapping test environment")
		testEnv = &envtest.Environment{
			CRDDirectoryPaths: []string{filepath.Join("../..", "config", "crd", "bases")},
		}
		restConfig, err := testEnv.Start()
		Expect(err).To(BeNil())
		Expect(restConfig).ToNot(BeNil())

		kubeClient, err = kubernetes.NewForConfig(restConfig)
		Expect(err).ToNot(HaveOccurred())
		Expect(kubeClient).ToNot(BeNil())

		rssClient, err = versioned.NewForConfig(restConfig)
		Expect(err).ToNot(HaveOccurred())
		Expect(rssClient).ToNot(BeNil())

		err = unifflev1alpha1.AddToScheme(scheme.Scheme)
		Expect(err).NotTo(HaveOccurred())

		// +kubebuilder:scaffold:scheme

		cfg := &config.Config{
			HTTPConfig: config.HTTPConfig{
				Port:            9876,
				ExternalService: webhookconstants.ComponentName,
			},
			GenericConfig: utils.GenericConfig{
				RESTConfig: restConfig,
				KubeClient: kubeClient,
				RSSClient:  rssClient,
			},
		}
		am := newAdmissionManager(cfg)
		stopCtx, ctxCancel = context.WithCancel(context.TODO())
		go func() {
			err = am.Start(stopCtx)
			Expect(err).ToNot(HaveOccurred())
		}()
	},
)

var _ = AfterSuite(func() {
	By("stopping admission manager")
	ctxCancel()
	By("tearing down the test environment")
	Expect(testEnv.Stop()).To(Succeed())
})

var _ = Describe("AdmissionManager", func() {
	Context("Setup syncer", func() {
		It("Generate validation and webhook configurations", func() {
			By("Wait validation configurations synced")
			var vwc *admissionv1.ValidatingWebhookConfiguration
			err := wait.Poll(time.Second, time.Second*5, func() (bool, error) {
				var getErr error
				vwc, getErr = kubeClient.AdmissionregistrationV1().ValidatingWebhookConfigurations().
					Get(context.TODO(), webhookconstants.ComponentName, metav1.GetOptions{})
				if getErr != nil {
					return false, getErr
				}
				return true, nil
			})
			Expect(err).ToNot(HaveOccurred())
			Expect(vwc).ToNot(BeNil())

			By("Wait mutating configurations synced")
			var mwc *admissionv1.MutatingWebhookConfiguration
			err = wait.Poll(time.Second, time.Second*5, func() (bool, error) {
				var getErr error
				mwc, getErr = kubeClient.AdmissionregistrationV1().MutatingWebhookConfigurations().
					Get(context.TODO(), webhookconstants.ComponentName, metav1.GetOptions{})
				if getErr != nil {
					return false, getErr
				}
				return true, nil
			})
			Expect(err).ToNot(HaveOccurred())
			Expect(mwc).ToNot(BeNil())
		})
	})
})
