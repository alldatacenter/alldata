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

package apps

import (
	"context"
	"time"

	"github.com/onsi/ginkgo"
	"github.com/onsi/gomega"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	k8spodutil "k8s.io/kubernetes/pkg/api/v1/pod"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/test/e2e/framework"
	"github.com/koordinator-sh/koordinator/test/e2e/framework/manifest"
)

var _ = SIGDescribe("NodeNUMAResource", func() {
	f := framework.NewDefaultFramework("nodenumaresource")

	ginkgo.BeforeEach(func() {

	})

	framework.KoordinatorDescribe("NodeNUMAResource CPUBindPolicy", func() {
		framework.ConformanceIt("bind with SpreadByPCPUs", func() {
			ginkgo.By("Loading Pod from manifest")
			pod, err := manifest.PodFromManifest("scheduling/simple-lsr-pod.yaml")
			gomega.Expect(err).NotTo(gomega.HaveOccurred())
			pod.Namespace = f.Namespace.Name

			ginkgo.By("Create Pod")
			pod = f.PodClient().Create(pod)

			ginkgo.By("Wait for Pod Scheduled")
			gomega.Eventually(func() bool {
				p, err := f.PodClient().Get(context.TODO(), pod.Name, metav1.GetOptions{})
				gomega.Expect(err).NotTo(gomega.HaveOccurred())
				_, podCondition := k8spodutil.GetPodCondition(&p.Status, corev1.PodScheduled)
				return podCondition != nil && podCondition.Status == corev1.ConditionTrue
			}, 60*time.Second, 3*time.Second).Should(gomega.Equal(true))

			ginkgo.By("Check Pod ResourceStatus")
			pod, err = f.PodClient().Get(context.TODO(), pod.Name, metav1.GetOptions{})
			gomega.Expect(err).NotTo(gomega.HaveOccurred())
			resourceStatus, err := extension.GetResourceStatus(pod.Annotations)
			gomega.Expect(err).NotTo(gomega.HaveOccurred())
			gomega.Expect(resourceStatus.CPUSet).NotTo(gomega.BeEmpty())
		})
	})
})
