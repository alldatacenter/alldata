package evictor

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/fake"

	"github.com/koordinator-sh/koordinator/apis/extension"
	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func TestPodEvictorMark(t *testing.T) {
	ctx := context.Background()
	fakeClient := fake.NewSimpleClientset()
	softEvictor, err := NewSoftEvictor(fakeClient)
	assert.NoError(t, err)
	initiator := "test-initiator"
	reason := "test-reason"
	pod := &corev1.Pod{

		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node-1",
		},
	}
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Annotations: map[string]string{
				AnnotationEvictReason:  reason,
				AnnotationEvictTrigger: initiator,
			},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			DeleteOptions: &metav1.DeleteOptions{},
		},
	}
	pod, err = fakeClient.CoreV1().Pods(pod.Namespace).Create(ctx, pod, metav1.CreateOptions{})
	assert.NoError(t, err)
	assert.NoError(t, softEvictor.Evict(ctx, job, pod))
	newPo, err := fakeClient.CoreV1().Pods(pod.Namespace).Get(ctx, pod.Name, metav1.GetOptions{})
	assert.NoError(t, err)
	softEvictionSpec, err := extension.GetSoftEvictionSpec(newPo.Annotations)
	assert.NoError(t, err)
	assert.NotNil(t, softEvictionSpec.Timestamp)

	assert.Equal(t, initiator, softEvictionSpec.Initiator)
	assert.Equal(t, reason, softEvictionSpec.Reason)

}
