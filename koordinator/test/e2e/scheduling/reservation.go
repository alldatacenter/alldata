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

package scheduling

import (
	"context"
	"fmt"
	"sort"
	"time"

	"github.com/onsi/ginkgo"
	"github.com/onsi/gomega"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/equality"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/uuid"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	k8spodutil "k8s.io/kubernetes/pkg/api/v1/pod"
	resourceapi "k8s.io/kubernetes/pkg/api/v1/resource"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
	"github.com/koordinator-sh/koordinator/test/e2e/framework"
	"github.com/koordinator-sh/koordinator/test/e2e/framework/manifest"
	e2enode "github.com/koordinator-sh/koordinator/test/e2e/framework/node"
)

var _ = SIGDescribe("Reservation", func() {
	f := framework.NewDefaultFramework("reservation")
	var nodeList *corev1.NodeList

	ginkgo.BeforeEach(func() {
		nodeList = &corev1.NodeList{}
		var err error

		framework.AllNodesReady(f.ClientSet, time.Minute)

		nodeList, err = e2enode.GetReadySchedulableNodes(f.ClientSet)
		if err != nil {
			framework.Logf("Unexpected error occurred: %v", err)
		}
	})

	ginkgo.AfterEach(func() {
		ls := metav1.SetAsLabelSelector(map[string]string{
			"e2e-test-reservation": "true",
		})
		reservationList, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().List(context.TODO(), metav1.ListOptions{
			LabelSelector: metav1.FormatLabelSelector(ls),
		})
		framework.ExpectNoError(err)
		for _, v := range reservationList.Items {
			err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Delete(context.TODO(), v.Name, metav1.DeleteOptions{})
			framework.ExpectNoError(err)
		}
	})

	framework.KoordinatorDescribe("Basic Reservation functionality", func() {
		framework.ConformanceIt("Create Reservation enables AllocateOnce and reserves CPU and Memory for Pod", func() {
			ginkgo.By("Create reservation")
			reservation, err := manifest.ReservationFromManifest("scheduling/simple-reservation.yaml")
			framework.ExpectNoError(err, "unable to load reservation")

			_, err = f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), reservation, metav1.CreateOptions{})
			framework.ExpectNoError(err, "unable to create reservation")

			ginkgo.By("Wait for reservation scheduled")
			gomega.Eventually(func() bool {
				r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
				framework.ExpectNoError(err)
				return reservationutil.IsReservationAvailable(r)
			}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true), "unable to schedule Reservation")

			ginkgo.By("Create pod to consume reservation")
			pod, err := manifest.PodFromManifest("scheduling/simple-pod-with-reservation.yaml")
			framework.ExpectNoError(err, "unable to load pod")
			pod.Namespace = f.Namespace.Name

			f.PodClient().Create(pod)
			gomega.Eventually(func() bool {
				p, err := f.PodClient().Get(context.TODO(), pod.Name, metav1.GetOptions{})
				gomega.Expect(err).NotTo(gomega.HaveOccurred())
				_, podCondition := k8spodutil.GetPodCondition(&p.Status, corev1.PodScheduled)
				return podCondition != nil && podCondition.Status == corev1.ConditionTrue
			}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

			ginkgo.By("Check pod and reservation status")
			r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
			framework.ExpectNoError(err, "unable to get reservation")

			p, err := f.PodClient().Get(context.TODO(), pod.Name, metav1.GetOptions{})
			framework.ExpectNoError(err)
			gomega.Expect(p.Spec.NodeName).Should(gomega.Equal(r.Status.NodeName),
				fmt.Sprintf("reservation is scheduled to node %v but pod is scheduled to node %v", r.Status.NodeName, pod.Spec.NodeName))

			reservationAllocated, err := apiext.GetReservationAllocated(p)
			framework.ExpectNoError(err)
			gomega.Expect(reservationAllocated).Should(gomega.Equal(&apiext.ReservationAllocated{
				Name: r.Name,
				UID:  r.UID,
			}), "pod is not using the expected reservation")

			gomega.Eventually(func() bool {
				r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
				framework.ExpectNoError(err)
				return r.Status.Phase == schedulingv1alpha1.ReservationSucceeded
			}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

			r, err = f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
			framework.ExpectNoError(err)
			reservationRequests := reservationutil.ReservationRequests(r)
			gomega.Expect(r.Status.Allocatable).Should(gomega.Equal(reservationRequests))

			podRequests, _ := resourceapi.PodRequestsAndLimits(p)
			podRequests = quotav1.Mask(podRequests, quotav1.ResourceNames(r.Status.Allocatable))
			gomega.Expect(r.Status.Allocated).Should(gomega.Equal(podRequests))
			gomega.Expect(r.Status.CurrentOwners).Should(gomega.Equal([]corev1.ObjectReference{
				{
					Namespace: p.Namespace,
					Name:      p.Name,
					UID:       p.UID,
				},
			}), "reservation.status.currentOwners is not as expected")
		})

		framework.ConformanceIt("Create Reservation disables AllocateOnce and reserves CPU and Memory for tow Pods", func() {
			ginkgo.By("Create reservation")
			reservation, err := manifest.ReservationFromManifest("scheduling/simple-reservation.yaml")
			framework.ExpectNoError(err, "unable to load reservation")

			// disable allocateOnce
			reservation.Spec.AllocateOnce = false
			// reserve resources for two Pods
			for k, v := range reservation.Spec.Template.Spec.Containers[0].Resources.Requests {
				vv := v.DeepCopy()
				vv.Add(v)
				reservation.Spec.Template.Spec.Containers[0].Resources.Requests[k] = vv
				reservation.Spec.Template.Spec.Containers[0].Resources.Limits[k] = vv
			}

			reservation, err = f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), reservation, metav1.CreateOptions{})
			framework.ExpectNoError(err, "unable to create reservation")

			ginkgo.By("Wait for reservation scheduled")
			gomega.Eventually(func() bool {
				r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
				framework.ExpectNoError(err)
				return reservationutil.IsReservationAvailable(r)
			}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

			ginkgo.By("Loading pod from manifest")
			pod, err := manifest.PodFromManifest("scheduling/simple-pod-with-reservation.yaml")
			framework.ExpectNoError(err)
			pod.Namespace = f.Namespace.Name

			ginkgo.By("Create Pod to consume Reservation")
			for i := 0; i < 2; i++ {
				testPod := pod.DeepCopy()
				testPod.Name = fmt.Sprintf("%s-%d", pod.Name, i)
				f.PodClient().Create(testPod)
				gomega.Eventually(func() bool {
					p, err := f.PodClient().Get(context.TODO(), testPod.Name, metav1.GetOptions{})
					gomega.Expect(err).NotTo(gomega.HaveOccurred())
					_, podCondition := k8spodutil.GetPodCondition(&p.Status, corev1.PodScheduled)
					return podCondition != nil && podCondition.Status == corev1.ConditionTrue
				}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))
			}

			ginkgo.By("Check pod and reservation Status")

			r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
			framework.ExpectNoError(err, "unable to get reservation")

			var owners []corev1.ObjectReference
			for i := 0; i < 2; i++ {
				name := fmt.Sprintf("%s-%d", pod.Name, i)
				p, err := f.PodClient().Get(context.TODO(), name, metav1.GetOptions{})
				framework.ExpectNoError(err)
				gomega.Expect(p.Spec.NodeName).Should(gomega.Equal(r.Status.NodeName),
					fmt.Sprintf("reservation is scheduled to node %v but pod is scheduled to node %v", r.Status.NodeName, p.Spec.NodeName))

				reservationAllocated, err := apiext.GetReservationAllocated(p)
				framework.ExpectNoError(err)
				gomega.Expect(reservationAllocated).Should(gomega.Equal(&apiext.ReservationAllocated{
					Name: r.Name,
					UID:  r.UID,
				}))

				owners = append(owners, corev1.ObjectReference{
					Namespace: p.Namespace,
					Name:      p.Name,
					UID:       p.UID,
				})
			}

			gomega.Eventually(func() bool {
				r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
				framework.ExpectNoError(err)
				return len(r.Status.CurrentOwners) == 2
			}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

			r, err = f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
			framework.ExpectNoError(err)
			reservationRequests := reservationutil.ReservationRequests(r)
			gomega.Expect(r.Status.Allocatable).Should(gomega.Equal(reservationRequests))

			podRequests, _ := resourceapi.PodRequestsAndLimits(pod)
			podRequests = quotav1.Mask(podRequests, quotav1.ResourceNames(r.Status.Allocatable))
			for k, v := range podRequests {
				vv := v.DeepCopy()
				vv.Add(v)
				podRequests[k] = vv
			}
			gomega.Expect(equality.Semantic.DeepEqual(r.Status.Allocated, podRequests)).Should(gomega.Equal(true))
			sort.Slice(owners, func(i, j int) bool {
				return owners[i].UID < owners[j].UID
			})
			sort.Slice(r.Status.CurrentOwners, func(i, j int) bool {
				return r.Status.CurrentOwners[i].UID < r.Status.CurrentOwners[j].UID
			})
			gomega.Expect(r.Status.CurrentOwners).Should(gomega.Equal(owners))
		})

		ginkgo.Context("validates resource fit with reservations", func() {
			var testNodeName string
			var fakeResourceName corev1.ResourceName = "koordinator.sh/fake-resource"

			ginkgo.BeforeEach(func() {
				ginkgo.By("Add fake resource")
				// find a node which can run a pod:
				testNodeName = GetNodeThatCanRunPod(f)

				// Get node object:
				node, err := f.ClientSet.CoreV1().Nodes().Get(context.TODO(), testNodeName, metav1.GetOptions{})
				framework.ExpectNoError(err, "unable to get node object for node %v", testNodeName)

				// update Node API object with a fake resource
				nodeCopy := node.DeepCopy()
				nodeCopy.ResourceVersion = "0"

				nodeCopy.Status.Capacity[fakeResourceName] = resource.MustParse("1000")
				nodeCopy.Status.Allocatable[fakeResourceName] = resource.MustParse("1000")
				_, err = f.ClientSet.CoreV1().Nodes().UpdateStatus(context.TODO(), nodeCopy, metav1.UpdateOptions{})
				framework.ExpectNoError(err, "unable to apply fake resource to %v", testNodeName)
			})

			ginkgo.AfterEach(func() {
				ginkgo.By("Remove fake resource")
				// remove fake resource:
				if testNodeName != "" {
					node, err := f.ClientSet.CoreV1().Nodes().Get(context.TODO(), testNodeName, metav1.GetOptions{})
					framework.ExpectNoError(err, "unable to get node object for node %v", testNodeName)

					nodeCopy := node.DeepCopy()
					// force it to update
					nodeCopy.ResourceVersion = "0"
					delete(nodeCopy.Status.Capacity, fakeResourceName)
					delete(nodeCopy.Status.Allocatable, fakeResourceName)
					_, err = f.ClientSet.CoreV1().Nodes().UpdateStatus(context.TODO(), nodeCopy, metav1.UpdateOptions{})
					framework.ExpectNoError(err, "unable to update node %v", testNodeName)
				}
			})

			framework.ConformanceIt("reserve all remaining resources to prevent other pods from being scheduled", func() {
				ginkgo.By("Create Reservation")
				reservation, err := manifest.ReservationFromManifest("scheduling/simple-reservation.yaml")
				framework.ExpectNoError(err, "unable to load reservation")

				reservation.Spec.Template.Spec.NodeName = testNodeName
				reservation.Spec.Template.Spec.Containers = append(reservation.Spec.Template.Spec.Containers, corev1.Container{
					Name: "fake-resource-container",
					Resources: corev1.ResourceRequirements{
						Limits: corev1.ResourceList{
							fakeResourceName: resource.MustParse("1000"),
						},
						Requests: corev1.ResourceList{
							fakeResourceName: resource.MustParse("1000"),
						},
					},
				})
				_, err = f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), reservation, metav1.CreateOptions{})
				framework.ExpectNoError(err, "unable to create reservation: %v", reservation.Name)

				ginkgo.By("Wait for Reservation Scheduled")
				gomega.Eventually(func() bool {
					r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
					framework.ExpectNoError(err)
					return reservationutil.IsReservationAvailable(r)
				}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

				ginkgo.By("Create Pod")
				pod := createPausePod(f, pausePodConfig{
					Name: string(uuid.NewUUID()),
					Resources: &corev1.ResourceRequirements{
						Limits: corev1.ResourceList{
							fakeResourceName: resource.MustParse("1000"),
						},
						Requests: corev1.ResourceList{
							fakeResourceName: resource.MustParse("1000"),
						},
					},
					NodeName:      testNodeName,
					SchedulerName: reservation.Spec.Template.Spec.SchedulerName,
				})

				ginkgo.By("Wait for Pod schedule failed")
				gomega.Eventually(func() bool {
					p, err := f.PodClient().Get(context.TODO(), pod.Name, metav1.GetOptions{})
					framework.ExpectNoError(err)
					_, scheduledCondition := k8spodutil.GetPodCondition(&p.Status, corev1.PodScheduled)
					return scheduledCondition != nil && scheduledCondition.Status == corev1.ConditionFalse
				}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))
			})

			framework.ConformanceIt("after a Pod uses a Reservation, the remaining resources of the node can be reserved by another reservation", func() {
				ginkgo.By("Create reservation")
				reservation, err := manifest.ReservationFromManifest("scheduling/simple-reservation.yaml")
				framework.ExpectNoError(err, "unable to load reservation")

				reservation.Spec.AllocateOnce = false
				reservation.Spec.Template.Spec.NodeName = testNodeName
				reservation.Spec.Template.Spec.Containers = append(reservation.Spec.Template.Spec.Containers, corev1.Container{
					Name: "fake-resource-container",
					Resources: corev1.ResourceRequirements{
						Limits: corev1.ResourceList{
							fakeResourceName: resource.MustParse("500"),
						},
						Requests: corev1.ResourceList{
							fakeResourceName: resource.MustParse("500"),
						},
					},
				})
				reservation.Spec.Owners = []schedulingv1alpha1.ReservationOwner{
					{
						LabelSelector: &metav1.LabelSelector{
							MatchLabels: map[string]string{
								"e2e-reserve-resource": "true",
							},
						},
					},
				}
				_, err = f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), reservation, metav1.CreateOptions{})
				framework.ExpectNoError(err, "unable to create reservation: %v", reservation.Name)

				ginkgo.By("Wait for reservation scheduled")
				gomega.Eventually(func() bool {
					r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
					framework.ExpectNoError(err)
					return reservationutil.IsReservationAvailable(r)
				}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

				ginkgo.By("Create pod and wait for scheduled")
				pod := createPausePod(f, pausePodConfig{
					Name: string(uuid.NewUUID()),
					Labels: map[string]string{
						"e2e-reserve-resource": "true",
					},
					Resources: &corev1.ResourceRequirements{
						Limits: corev1.ResourceList{
							fakeResourceName: resource.MustParse("500"),
						},
						Requests: corev1.ResourceList{
							fakeResourceName: resource.MustParse("500"),
						},
					},
					NodeName:      testNodeName,
					SchedulerName: reservation.Spec.Template.Spec.SchedulerName,
				})
				gomega.Eventually(func() bool {
					p, err := f.PodClient().Get(context.TODO(), pod.Name, metav1.GetOptions{})
					gomega.Expect(err).NotTo(gomega.HaveOccurred())
					_, podCondition := k8spodutil.GetPodCondition(&p.Status, corev1.PodScheduled)
					return podCondition != nil && podCondition.Status == corev1.ConditionTrue
				}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

				p, err := f.PodClient().Get(context.TODO(), pod.Name, metav1.GetOptions{})
				gomega.Expect(err).NotTo(gomega.HaveOccurred())

				ginkgo.By("Create other reservation reserves the remaining fakeResource")
				otherReservation := reservation.DeepCopy()
				otherReservation.Name = "e2e-other-reservation"
				otherReservation.Spec.Owners = []schedulingv1alpha1.ReservationOwner{
					{
						LabelSelector: &metav1.LabelSelector{
							MatchLabels: map[string]string{
								"e2e-reserve-resource": "false",
							},
						},
					},
				}
				_, err = f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), otherReservation, metav1.CreateOptions{})
				framework.ExpectNoError(err, "unable to create reservation: %v", otherReservation.Name)

				ginkgo.By("Wait for Reservation Scheduled")
				gomega.Eventually(func() bool {
					r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), otherReservation.Name, metav1.GetOptions{})
					framework.ExpectNoError(err)
					return reservationutil.IsReservationAvailable(r)
				}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

				r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), otherReservation.Name, metav1.GetOptions{})
				framework.ExpectNoError(err)
				gomega.Expect(r.Status.NodeName).Should(gomega.Equal(p.Spec.NodeName))
			})
		})

		framework.ConformanceIt("validates PodAntiAffinity with reservation", func() {
			ginkgo.By("Create reservation")
			reservation, err := manifest.ReservationFromManifest("scheduling/simple-reservation.yaml")
			framework.ExpectNoError(err, "unable to load reservation")

			reservation.Spec.Template.Labels = map[string]string{
				"e2e-reservation-interpodaffinity": "true",
			}

			reservation.Spec.Template.Spec.Affinity = &corev1.Affinity{
				PodAntiAffinity: &corev1.PodAntiAffinity{
					RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
						{
							LabelSelector: &metav1.LabelSelector{
								MatchLabels: map[string]string{
									"e2e-reservation-interpodaffinity": "true",
								},
							},
							TopologyKey: corev1.LabelHostname,
						},
					},
				},
			}

			_, err = f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), reservation, metav1.CreateOptions{})
			framework.ExpectNoError(err, "unable to create reservation")

			ginkgo.By("Wait for reservation scheduled")
			gomega.Eventually(func() bool {
				r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
				framework.ExpectNoError(err)
				return reservationutil.IsReservationAvailable(r)
			}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true), "unable to schedule Reservation")

			r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
			framework.ExpectNoError(err)

			ginkgo.By("Create pod and wait for scheduled")
			pod := createPausePod(f, pausePodConfig{
				Name: string(uuid.NewUUID()),
				Labels: map[string]string{
					"e2e-reservation-interpodaffinity": "true",
				},
				Affinity:      reservation.Spec.Template.Spec.Affinity,
				NodeName:      r.Status.NodeName,
				SchedulerName: reservation.Spec.Template.Spec.SchedulerName,
			})
			gomega.Eventually(func() bool {
				p, err := f.PodClient().Get(context.TODO(), pod.Name, metav1.GetOptions{})
				gomega.Expect(err).NotTo(gomega.HaveOccurred())
				_, podCondition := k8spodutil.GetPodCondition(&p.Status, corev1.PodScheduled)
				return podCondition != nil && podCondition.Status == corev1.ConditionTrue
			}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

			p, err := f.PodClient().Get(context.TODO(), pod.Name, metav1.GetOptions{})
			gomega.Expect(err).NotTo(gomega.HaveOccurred())
			gomega.Expect(p.Spec.NodeName).Should(gomega.Equal(r.Status.NodeName))
		})

		ginkgo.Context("PodTopologySpread Filtering With Reservation", func() {
			var nodeNames []string
			topologyKey := "kubernetes.io/e2e-pts-filter"

			ginkgo.BeforeEach(func() {
				if len(nodeList.Items) < 2 {
					ginkgo.Skip("At least 2 nodes are required to run the test")
				}
				ginkgo.By("Trying to get 2 available nodes which can run pod")
				nodeNames = Get2NodesThatCanRunPod(f)
				ginkgo.By(fmt.Sprintf("Apply dedicated topologyKey %v for this test on the 2 nodes.", topologyKey))
				for _, nodeName := range nodeNames {
					framework.AddOrUpdateLabelOnNode(f.ClientSet, nodeName, topologyKey, nodeName)
				}
			})
			ginkgo.AfterEach(func() {
				for _, nodeName := range nodeNames {
					framework.RemoveLabelOffNode(f.ClientSet, nodeName, topologyKey)
				}
			})

			ginkgo.It("validates 4 pods with MaxSkew=1 are evenly distributed into 2 nodes", func() {
				ginkgo.By("Create Reservation")
				podLabel := "e2e-pts-filter"

				reservation, err := manifest.ReservationFromManifest("scheduling/simple-reservation.yaml")
				framework.ExpectNoError(err, "unable load reservation from manifest")

				reservation.Spec.AllocateOnce = false
				reservation.Spec.Template.Namespace = f.Namespace.Name
				if reservation.Spec.Template.Labels == nil {
					reservation.Spec.Template.Labels = map[string]string{}
				}
				reservation.Spec.Template.Labels[podLabel] = ""
				reservation.Spec.Template.Spec.Affinity = &corev1.Affinity{
					NodeAffinity: &corev1.NodeAffinity{
						RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
							NodeSelectorTerms: []corev1.NodeSelectorTerm{
								{
									MatchExpressions: []corev1.NodeSelectorRequirement{
										{
											Key:      topologyKey,
											Operator: corev1.NodeSelectorOpIn,
											Values:   nodeNames,
										},
									},
								},
							},
						},
					},
				}
				reservation.Spec.Template.Spec.TopologySpreadConstraints = []corev1.TopologySpreadConstraint{
					{
						MaxSkew:           1,
						TopologyKey:       topologyKey,
						WhenUnsatisfiable: corev1.DoNotSchedule,
						LabelSelector: &metav1.LabelSelector{
							MatchExpressions: []metav1.LabelSelectorRequirement{
								{
									Key:      podLabel,
									Operator: metav1.LabelSelectorOpExists,
								},
							},
						},
					},
				}

				_, err = f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), reservation, metav1.CreateOptions{})
				framework.ExpectNoError(err, "unable to create reservation: %v", reservation.Name)

				ginkgo.By("Wait for Reservation Scheduled")
				gomega.Eventually(func() bool {
					r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
					framework.ExpectNoError(err)
					return reservationutil.IsReservationAvailable(r)
				}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

				ginkgo.By("Create 4 pods")
				requests := reservationutil.ReservationRequests(reservation)
				replicas := 4
				rsConfig := pauseRSConfig{
					Replicas: int32(replicas),
					PodConfig: pausePodConfig{
						Name:      podLabel,
						Namespace: f.Namespace.Name,
						Labels: map[string]string{
							podLabel: "",
							"app":    "e2e-test-reservation",
						},
						Resources: &corev1.ResourceRequirements{
							Limits:   requests,
							Requests: requests,
						},
						SchedulerName: reservation.Spec.Template.Spec.SchedulerName,
						Affinity: &corev1.Affinity{
							NodeAffinity: &corev1.NodeAffinity{
								RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
									NodeSelectorTerms: []corev1.NodeSelectorTerm{
										{
											MatchExpressions: []corev1.NodeSelectorRequirement{
												{
													Key:      topologyKey,
													Operator: corev1.NodeSelectorOpIn,
													Values:   nodeNames,
												},
											},
										},
									},
								},
							},
						},
						TopologySpreadConstraints: []corev1.TopologySpreadConstraint{
							{
								MaxSkew:           1,
								TopologyKey:       topologyKey,
								WhenUnsatisfiable: corev1.DoNotSchedule,
								LabelSelector: &metav1.LabelSelector{
									MatchExpressions: []metav1.LabelSelectorRequirement{
										{
											Key:      podLabel,
											Operator: metav1.LabelSelectorOpExists,
										},
									},
								},
							},
						},
					},
				}
				runPauseRS(f, rsConfig)
				podList, err := f.ClientSet.CoreV1().Pods(f.Namespace.Name).List(context.TODO(), metav1.ListOptions{})
				framework.ExpectNoError(err)
				numInNode1, numInNode2 := 0, 0
				for _, pod := range podList.Items {
					if pod.Spec.NodeName == nodeNames[0] {
						numInNode1++
					} else if pod.Spec.NodeName == nodeNames[1] {
						numInNode2++
					}
				}
				expected := replicas / len(nodeNames)
				framework.ExpectEqual(numInNode1, expected, fmt.Sprintf("Pods are not distributed as expected on node %q", nodeNames[0]))
				framework.ExpectEqual(numInNode2, expected, fmt.Sprintf("Pods are not distributed as expected on node %q", nodeNames[1]))

				ginkgo.By("Check Reservation Status")
				gomega.Eventually(func() bool {
					r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
					framework.ExpectNoError(err)
					return len(r.Status.CurrentOwners) == 1
				}, 60*time.Second, 1*time.Second).Should(gomega.Equal(true))

				r, err := f.KoordinatorClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
				framework.ExpectNoError(err)
				gomega.Expect(len(r.Status.Allocatable) > 0).Should(gomega.Equal(true))
				gomega.Expect(len(r.Status.CurrentOwners) == 1).Should(gomega.Equal(true))
			})
		})
	})
})
