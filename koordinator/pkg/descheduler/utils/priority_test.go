package utils

import (
	"context"
	"testing"

	schedulingv1 "k8s.io/api/scheduling/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes/fake"
	"k8s.io/utils/pointer"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
)

func TestGetPriorityValueFromPriorityThreshold(t *testing.T) {
	tests := []struct {
		name              string
		priorityClasses   []*schedulingv1.PriorityClass
		priorityThreshold *deschedulerconfig.PriorityThreshold
		wantPriority      int32
		wantErr           bool
	}{
		{
			name: "custom threshold",
			priorityThreshold: &deschedulerconfig.PriorityThreshold{
				Value: pointer.Int32(1024),
			},
			wantPriority: 1024,
			wantErr:      false,
		},
		{
			name: "custom threshold bigger than SystemCriticalPriority",
			priorityThreshold: &deschedulerconfig.PriorityThreshold{
				Value: pointer.Int32(SystemCriticalPriority + 100),
			},
			wantErr: true,
		},
		{
			name: "test with priority class",
			priorityClasses: []*schedulingv1.PriorityClass{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-priority-class",
					},
					Value: 1024,
				},
			},
			priorityThreshold: &deschedulerconfig.PriorityThreshold{
				Name: "test-priority-class",
			},
			wantPriority: 1024,
			wantErr:      false,
		},
		{
			name: "test with non-exist priority class",
			priorityThreshold: &deschedulerconfig.PriorityThreshold{
				Name: "test-priority-class",
			},
			wantErr: true,
		},
		{
			name:         "non custom threshold with default priority threshold",
			wantPriority: SystemCriticalPriority,
			wantErr:      false,
		},
		{
			name:              "custom empty threshold with default priority threshold",
			priorityThreshold: &deschedulerconfig.PriorityThreshold{},
			wantPriority:      SystemCriticalPriority,
			wantErr:           false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var objs []runtime.Object
			for _, priorityClass := range tt.priorityClasses {
				objs = append(objs, priorityClass)
			}
			fakeClient := fake.NewSimpleClientset(objs...)

			sharedInformerFactory := informers.NewSharedInformerFactory(fakeClient, 0)
			priorityClassLister := sharedInformerFactory.Scheduling().V1().PriorityClasses().Lister()

			sharedInformerFactory.Start(context.TODO().Done())
			sharedInformerFactory.WaitForCacheSync(context.TODO().Done())

			gotPriority, err := GetPriorityValueFromPriorityThreshold(priorityClassLister, tt.priorityThreshold)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetPriorityValueFromPriorityThreshold() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if gotPriority != tt.wantPriority {
				t.Errorf("GetPriorityValueFromPriorityThreshold() gotPriority = %v, want %v", gotPriority, tt.wantPriority)
			}
		})
	}
}
