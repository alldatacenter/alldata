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
	"path/filepath"
	"testing"
	"time"

	. "github.com/onsi/ginkgo/v2"
	. "github.com/onsi/gomega"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/envtest"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/log/zap"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/config"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/sync/coordinator"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/sync/shuffleserver"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/clientset/versioned"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
)

var (
	testEnv        *envtest.Environment
	testKubeClient kubernetes.Interface
	testRssClient  versioned.Interface
	testCM         *corev1.ConfigMap
	testRSS        *unifflev1alpha1.RemoteShuffleService
	stopCtx        context.Context
	ctxCancel      context.CancelFunc
)

func TestRssController(t *testing.T) {
	RegisterFailHandler(Fail)
	suiteCfg, reporterCfg := GinkgoConfiguration()
	reporterCfg.VeryVerbose = true
	reporterCfg.FullTrace = true
	RunSpecs(t, "rss controller suite", suiteCfg, reporterCfg)
}

var _ = BeforeSuite(
	func() {
		logf.SetLogger(zap.New(zap.WriteTo(GinkgoWriter), zap.UseDevMode(true)))
		By("bootstrapping test environment")
		testEnv = &envtest.Environment{
			CRDDirectoryPaths: []string{filepath.Join("../../..", "config", "crd", "bases")},
		}
		restConfig, err := testEnv.Start()
		Expect(err).To(BeNil())
		Expect(restConfig).ToNot(BeNil())

		testCM, testRSS = initTestRss()

		testKubeClient, err = kubernetes.NewForConfig(restConfig)
		Expect(err).ToNot(HaveOccurred())
		Expect(testKubeClient).ToNot(BeNil())

		testRssClient, err = versioned.NewForConfig(restConfig)
		Expect(err).ToNot(HaveOccurred())
		Expect(testRssClient).ToNot(BeNil())

		err = unifflev1alpha1.AddToScheme(scheme.Scheme)
		Expect(err).NotTo(HaveOccurred())

		// +kubebuilder:scaffold:scheme

		cfg := &config.Config{
			Workers: 1,
			GenericConfig: utils.GenericConfig{
				RESTConfig: restConfig,
				KubeClient: testKubeClient,
				RSSClient:  testRssClient,
			},
		}
		rc := newRSSController(cfg)
		stopCtx, ctxCancel = context.WithCancel(context.TODO())
		go func() {
			err = rc.Start(stopCtx)
			Expect(err).ToNot(HaveOccurred())
		}()
	},
)

var _ = AfterSuite(func() {
	By("stopping rss controller")
	ctxCancel()
	By("tearing down the test environment")
	Expect(testEnv.Stop()).To(Succeed())
})

// At present, it is not possible to simulate the real operation of Workload through EnvTest.
// TODO: more detailed tests will be added in the future.
var _ = Describe("RssController", func() {
	Context("Handle rss objects", func() {
		It("Create a rss object", func() {
			By("Create test configMap")
			_, err := testKubeClient.CoreV1().ConfigMaps(testCM.Namespace).
				Create(context.TODO(), testCM, metav1.CreateOptions{})
			Expect(err).ToNot(HaveOccurred())

			By("Create test rss object")
			_, err = testRssClient.UniffleV1alpha1().RemoteShuffleServices(corev1.NamespaceDefault).
				Create(context.TODO(), testRSS, metav1.CreateOptions{})
			Expect(err).ToNot(HaveOccurred())

			By("Wait rss object running")
			err = wait.Poll(time.Second, time.Second*5, func() (bool, error) {
				curRss, getErr := testRssClient.UniffleV1alpha1().RemoteShuffleServices(testNamespace).
					Get(context.TODO(), testRssName, metav1.GetOptions{})
				if getErr != nil {
					return false, getErr
				}

				if curRss.Status.Phase != unifflev1alpha1.RSSRunning || *curRss.Spec.ShuffleServer.Sync {
					return false, nil
				}
				return true, nil
			})
			Expect(err).ToNot(HaveOccurred())

			By("Check coordinator 0")
			coordinatorName0 := coordinator.GenerateNameByIndex(testRSS, 0)
			_, err = testKubeClient.AppsV1().Deployments(corev1.NamespaceDefault).
				Get(context.TODO(), coordinatorName0, metav1.GetOptions{})
			Expect(err).ToNot(HaveOccurred())

			By("Check coordinator 1")
			coordinatorName1 := coordinator.GenerateNameByIndex(testRSS, 1)
			_, err = testKubeClient.AppsV1().Deployments(corev1.NamespaceDefault).
				Get(context.TODO(), coordinatorName1, metav1.GetOptions{})
			Expect(err).ToNot(HaveOccurred())

			By("Check shuffle server")
			shuffleServerName := shuffleserver.GenerateName(testRSS)
			_, err = testKubeClient.AppsV1().StatefulSets(corev1.NamespaceDefault).
				Get(context.TODO(), shuffleServerName, metav1.GetOptions{})
			Expect(err).ToNot(HaveOccurred())
		})
		It("Update the rss object", func() {
			By("Get current rss object")
			curRSS, err := testRssClient.UniffleV1alpha1().RemoteShuffleServices(corev1.NamespaceDefault).
				Get(context.TODO(), testRssName, metav1.GetOptions{})
			Expect(err).ToNot(HaveOccurred())
			Expect(curRSS).ToNot(BeNil())
			Expect(*curRSS.Spec.ShuffleServer.Sync, false)

			shuffleServerName := shuffleserver.GenerateName(testRSS)
			var sts *appsv1.StatefulSet
			sts, err = testKubeClient.AppsV1().StatefulSets(corev1.NamespaceDefault).
				Get(context.TODO(), shuffleServerName, metav1.GetOptions{})
			Expect(err).ToNot(HaveOccurred())
			Expect(sts).ToNot(BeNil())
			Expect(*sts.Spec.Replicas).To(Equal(int32(1)))

			coordinatorName0 := coordinator.GenerateNameByIndex(testRSS, 0)
			var coordinator0 *appsv1.Deployment
			coordinator0, err = testKubeClient.AppsV1().Deployments(corev1.NamespaceDefault).
				Get(context.TODO(), coordinatorName0, metav1.GetOptions{})
			Expect(err).ToNot(HaveOccurred())
			Expect(coordinator0).ToNot(BeNil())
			Expect(coordinator0.Spec.Template.Spec.Containers[0].Image).To(Equal(testCoordinatorImage1))

			coordinatorName1 := coordinator.GenerateNameByIndex(testRSS, 0)
			var coordinator1 *appsv1.Deployment
			coordinator1, err = testKubeClient.AppsV1().Deployments(corev1.NamespaceDefault).
				Get(context.TODO(), coordinatorName1, metav1.GetOptions{})
			Expect(err).ToNot(HaveOccurred())
			Expect(coordinator1).ToNot(BeNil())
			Expect(coordinator1.Spec.Template.Spec.Containers[0].Image).To(Equal(testCoordinatorImage1))

			By("Update test rss object")
			curRSS.Spec.Coordinator.Image = testCoordinatorImage2
			curRSS.Spec.ShuffleServer.Sync = pointer.Bool(true)
			curRSS.Spec.ShuffleServer.Replicas = pointer.Int32(3)
			_, err = testRssClient.UniffleV1alpha1().RemoteShuffleServices(corev1.NamespaceDefault).
				Update(context.TODO(), curRSS, metav1.UpdateOptions{})
			Expect(err).ToNot(HaveOccurred())

			By("Wait rss object upgrading")
			err = wait.Poll(time.Second, time.Second*5, func() (bool, error) {
				curRss, getErr := testRssClient.UniffleV1alpha1().RemoteShuffleServices(testNamespace).
					Get(context.TODO(), testRssName, metav1.GetOptions{})
				if getErr != nil {
					return false, getErr
				}
				if curRss.Status.Phase != unifflev1alpha1.RSSUpgrading {
					return false, nil
				}
				return true, nil
			})
			Expect(err).ToNot(HaveOccurred())

			By("Check coordinator 0")
			coordinator0, err = testKubeClient.AppsV1().Deployments(corev1.NamespaceDefault).
				Get(context.TODO(), coordinatorName0, metav1.GetOptions{})
			Expect(err).ToNot(HaveOccurred())
			Expect(coordinator0).ToNot(BeNil())
			Expect(coordinator0.Spec.Template.Spec.Containers[0].Image).To(Equal(testCoordinatorImage2))

			By("Check coordinator 1")
			coordinator1, err = testKubeClient.AppsV1().Deployments(corev1.NamespaceDefault).
				Get(context.TODO(), coordinatorName1, metav1.GetOptions{})
			Expect(err).ToNot(HaveOccurred())
			Expect(coordinator1).ToNot(BeNil())
			Expect(coordinator1.Spec.Template.Spec.Containers[0].Image).To(Equal(testCoordinatorImage2))

			By("Check shuffle server")
			sts, err = testKubeClient.AppsV1().StatefulSets(corev1.NamespaceDefault).
				Get(context.TODO(), shuffleServerName, metav1.GetOptions{})
			Expect(err).ToNot(HaveOccurred())
			Expect(sts).ToNot(BeNil())
			Expect(*sts.Spec.Replicas).To(Equal(int32(3)))

			// since we are in the env test, the rss object may never transmit upgrading to running.
			By("Ensure rss object is still upgrading")
			err = wait.Poll(time.Second, time.Second*5, func() (bool, error) {
				curRss, getErr := testRssClient.UniffleV1alpha1().RemoteShuffleServices(testNamespace).
					Get(context.TODO(), testRssName, metav1.GetOptions{})
				if getErr != nil {
					return false, getErr
				}
				if curRss.Status.Phase != unifflev1alpha1.RSSUpgrading {
					return false, nil
				}
				return true, nil
			})
			Expect(err).ToNot(HaveOccurred())

			By("Delete the upgrading rss object")
			err = testRssClient.UniffleV1alpha1().RemoteShuffleServices(corev1.NamespaceDefault).
				Delete(context.TODO(), testRssName, metav1.DeleteOptions{})
			Expect(err).ToNot(HaveOccurred())

			By("Waiting the rss object being delete")
			err = wait.Poll(time.Second, time.Second*5, func() (done bool, err error) {
				_, getErr := testRssClient.UniffleV1alpha1().RemoteShuffleServices(testNamespace).
					Get(context.TODO(), testRssName, metav1.GetOptions{})
				if getErr != nil && errors.IsNotFound(getErr) {
					return true, nil
				}
				return false, nil
			})
			Expect(err).ToNot(HaveOccurred())
		})
	})
})

func initTestRss() (*corev1.ConfigMap, *unifflev1alpha1.RemoteShuffleService) {
	cm := &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      testRssName,
			Namespace: testNamespace,
		},
		Data: map[string]string{
			constants.CoordinatorConfigKey:   "",
			constants.ShuffleServerConfigKey: "",
			constants.Log4jPropertiesKey:     "",
		},
	}
	rss := utils.BuildRSSWithDefaultValue()
	rss.Spec.ConfigMapName = cm.Name
	rss.Name = testRssName
	rss.Namespace = testNamespace
	rss.Spec.Coordinator.Image = testCoordinatorImage1
	return cm, rss
}
