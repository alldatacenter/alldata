package controllerfinder

import (
	"context"
	"fmt"
	"reflect"
	"testing"

	appsv1alpha1 "github.com/openkruise/kruise-api/apps/v1alpha1"
	appsv1beta1 "github.com/openkruise/kruise-api/apps/v1beta1"
	kruisepolicyv1alpha1 "github.com/openkruise/kruise-api/policy/v1alpha1"
	"github.com/stretchr/testify/assert"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func TestControllerFinder_GetPodsForRef(t *testing.T) {
	tests := []struct {
		name           string
		ownerReference *metav1.OwnerReference
		ns             string
	}{
		{
			name: "replicaset",
			ownerReference: &metav1.OwnerReference{
				APIVersion: ControllerKindRS.Group + "/" + ControllerKindRS.Version,
				Kind:       ControllerKindRS.Kind,
				Name:       "test",
			},
			ns: "default",
		},
		{
			name: "deployment",
			ownerReference: &metav1.OwnerReference{
				APIVersion: ControllerKindDep.Group + "/" + ControllerKindDep.Version,
				Kind:       ControllerKindDep.Kind,
				Name:       "test",
			},
			ns: "default",
		},
		{
			name: "Others",
			ownerReference: &metav1.OwnerReference{
				APIVersion: ControllerKruiseKindCS.Group + "/" + ControllerKruiseKindCS.Version,
				Kind:       ControllerKruiseKindCS.Kind,
				Name:       "test",
			},
			ns: "default",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			scheme := runtime.NewScheme()
			_ = sev1alpha1.AddToScheme(scheme)
			_ = clientgoscheme.AddToScheme(scheme)
			_ = appsv1alpha1.AddToScheme(scheme)
			_ = appsv1beta1.AddToScheme(scheme)
			_ = kruisepolicyv1alpha1.AddToScheme(scheme)
			runtimeClient := fake.NewClientBuilder().WithScheme(scheme).Build()
			deployment := &appsv1.Deployment{
				TypeMeta: metav1.TypeMeta{
					Kind:       ControllerKindDep.Kind,
					APIVersion: ControllerKindDep.Group + "/" + ControllerKindDep.Version,
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      "test",
					Namespace: "default",
				},
				Spec: appsv1.DeploymentSpec{
					Replicas: pointer.Int32(3),
					Selector: nil,
				},
			}
			assert.NoError(t, runtimeClient.Create(context.Background(), deployment))
			replicaSet := &appsv1.ReplicaSet{
				TypeMeta: metav1.TypeMeta{
					Kind:       ControllerKindRS.Kind,
					APIVersion: ControllerKindRS.Group + "/" + ControllerKindRS.Version,
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      "test",
					Namespace: "default",
					OwnerReferences: []metav1.OwnerReference{{
						APIVersion: ControllerKindDep.Group + "/" + ControllerKindDep.Version,
						Kind:       ControllerKindDep.Kind,
						Name:       "test",
						Controller: pointer.Bool(true),
					}},
				},
			}
			assert.NoError(t, runtimeClient.Create(context.Background(), replicaSet))
			cloneSet := &appsv1alpha1.CloneSet{
				TypeMeta: metav1.TypeMeta{
					Kind:       ControllerKruiseKindCS.Kind,
					APIVersion: ControllerKruiseKindCS.Group + "/" + ControllerKruiseKindCS.Version,
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      "test",
					Namespace: "default",
				},
				Spec: appsv1alpha1.CloneSetSpec{
					Replicas: pointer.Int32(3),
					Selector: nil,
				},
			}
			assert.NoError(t, runtimeClient.Create(context.Background(), cloneSet))
			var pods []*corev1.Pod
			for i := 0; i < 3; i++ {
				pod := &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name:      fmt.Sprintf("test-%d", i),
						Namespace: "default",
						OwnerReferences: []metav1.OwnerReference{{
							APIVersion: tt.ownerReference.APIVersion,
							Kind:       tt.ownerReference.Kind,
							Name:       tt.ownerReference.Name,
						}},
					},
				}
				assert.NoError(t, runtimeClient.Create(context.Background(), pod))
				pods = append(pods, pod)
			}
			r := &ControllerFinder{
				Client: runtimeClient,
				mapper: runtimeClient.RESTMapper(),
			}
			gotPods, gotReplicas, err := r.GetPodsForRef(tt.ownerReference, tt.ns, nil, false)
			assert.NoError(t, err)
			if !reflect.DeepEqual(gotPods, pods) {
				t.Errorf("GetPodsForRef() gotPods = %v, wantPods %v", gotPods, pods)
			}
			if gotReplicas != 3 {
				t.Errorf("GetPodsForRef() gotReplicas = %v, wantReplicas %v", gotReplicas, 3)
			}
		})
	}
}
