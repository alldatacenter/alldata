package utils

import (
	"testing"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"
)

func TestGetResourceRequest(t *testing.T) {
	tests := []struct {
		name         string
		pod          *corev1.Pod
		resourceName corev1.ResourceName
		want         int64
	}{
		{
			name: "cpu",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceCPU: resource.MustParse("32"),
								},
							},
						},
					},
				},
			},
			resourceName: corev1.ResourceCPU,
			want:         32000,
		},
		{
			name: "memory",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			resourceName: corev1.ResourceMemory,
			want:         32 * 1024 * 1024 * 1024,
		},
		{
			name:         "pods",
			pod:          &corev1.Pod{},
			resourceName: corev1.ResourcePods,
			want:         1,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetResourceRequest(tt.pod, tt.resourceName); got != tt.want {
				t.Errorf("GetResourceRequest() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetResourceRequestQuantity(t *testing.T) {
	tests := []struct {
		name         string
		pod          *corev1.Pod
		resourceName corev1.ResourceName
		want         resource.Quantity
	}{
		{
			name: "full pods",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
					InitContainers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceMemory: resource.MustParse("64Gi"),
								},
							},
						},
					},
					Overhead: map[corev1.ResourceName]resource.Quantity{
						corev1.ResourceMemory: resource.MustParse("1Gi"),
					},
				},
			},
			resourceName: corev1.ResourceMemory,
			want:         resource.MustParse("65Gi"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetResourceRequestQuantity(tt.pod, tt.resourceName); !got.Equal(tt.want) {
				t.Errorf("GetResourceRequestQuantity() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsMirrorPod(t *testing.T) {
	tests := []struct {
		name string
		pod  *corev1.Pod
		want bool
	}{
		{
			name: "mirror pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Annotations: map[string]string{
						corev1.MirrorPodAnnotationKey: "true",
					},
				},
			},
			want: true,
		},
		{
			name: "normal pod",
			pod:  &corev1.Pod{},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsMirrorPod(tt.pod); got != tt.want {
				t.Errorf("IsMirrorPod() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsPodTerminating(t *testing.T) {
	tests := []struct {
		name string
		pod  *corev1.Pod
		want bool
	}{
		{
			name: "terminating pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					DeletionTimestamp: &metav1.Time{Time: time.Now()},
				},
			},
			want: true,
		},
		{
			name: "normal pod",
			pod:  &corev1.Pod{},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsPodTerminating(tt.pod); got != tt.want {
				t.Errorf("IsPodTerminating() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsStaticPod(t *testing.T) {
	tests := []struct {
		name string
		pod  *corev1.Pod
		want bool
	}{
		{
			name: "static pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Annotations: map[string]string{
						"kubernetes.io/config.source": "test",
					},
				},
			},
			want: true,
		},
		{
			name: "normal pod",
			pod:  &corev1.Pod{},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsStaticPod(tt.pod); got != tt.want {
				t.Errorf("IsStaticPod() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsCriticalPriorityPod(t *testing.T) {
	tests := []struct {
		name string
		pod  *corev1.Pod
		want bool
	}{
		{
			name: "critical priority pod",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Priority: pointer.Int32(SystemCriticalPriority),
				},
			},
			want: true,
		},
		{
			name: "normal pod",
			pod:  &corev1.Pod{},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsCriticalPriorityPod(tt.pod); got != tt.want {
				t.Errorf("IsCriticalPriorityPod() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsDaemonsetPod(t *testing.T) {
	tests := []struct {
		name string
		pod  *corev1.Pod
		want bool
	}{
		{
			name: "daemonset pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					OwnerReferences: []metav1.OwnerReference{
						{
							Kind: "DaemonSet",
						},
					},
				},
			},
			want: true,
		},
		{
			name: "normal pod",
			pod:  &corev1.Pod{},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsDaemonsetPod(tt.pod.OwnerReferences); got != tt.want {
				t.Errorf("IsDaemonsetPod() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsPodWithLocalStorage(t *testing.T) {
	tests := []struct {
		name string
		pod  *corev1.Pod
		want bool
	}{
		{
			name: "pod with local storage",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Volumes: []corev1.Volume{
						{
							VolumeSource: corev1.VolumeSource{
								EmptyDir: &corev1.EmptyDirVolumeSource{},
							},
						},
					},
				},
			},
			want: true,
		},
		{
			name: "normal pod",
			pod:  &corev1.Pod{},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsPodWithLocalStorage(tt.pod); got != tt.want {
				t.Errorf("IsPodWithLocalStorage() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsPodWithPVC(t *testing.T) {
	tests := []struct {
		name string
		pod  *corev1.Pod
		want bool
	}{
		{
			name: "pod with pvc",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Volumes: []corev1.Volume{
						{
							VolumeSource: corev1.VolumeSource{
								PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{},
							},
						},
					},
				},
			},
			want: true,
		},
		{
			name: "normal pod",
			pod:  &corev1.Pod{},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsPodWithPVC(tt.pod); got != tt.want {
				t.Errorf("IsPodWithPVC() = %v, want %v", got, tt.want)
			}
		})
	}
}
