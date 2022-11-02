package helper

import (
	"sort"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

const ReconcileError = "ReconcileError"
const ReconcileErrorReason = "LastReconcileCycleFailed"
const ReconcileSuccess = "ReconcileSuccess"
const ReconcileSuccessReason = "LastReconcileCycleSucceded"

// ConditionsAware represents a CRD type that has been enabled with metav1.Conditions, it can then benefit of a series of utility methods.
type ConditionsAware interface {
	GetConditions() []metav1.Condition
	SetConditions(conditions []metav1.Condition)
}

//AddOrReplaceCondition adds or replaces the passed condition in the passed array of conditions
func AddOrReplaceCondition(c metav1.Condition, conditions []metav1.Condition) []metav1.Condition {
	for i, condition := range conditions {
		if c.Type == condition.Type {
			conditions[i] = c
			return conditions
		}
	}
	conditions = append(conditions, c)
	return conditions
}

//GetCondition returns the condition with the given type, if it exists. If the condition does not exists it returns false.
func GetCondition(conditionType string, conditions []metav1.Condition) (metav1.Condition, bool) {
	for _, condition := range conditions {
		if condition.Type == conditionType {
			return condition, true
		}
	}
	return metav1.Condition{}, false
}

//GetLastCondition retruns the last condition based on the condition timestamp. if no condition is present it return false.
func GetLastCondition(conditions []metav1.Condition) (metav1.Condition, bool) {
	if len(conditions) == 0 {
		return metav1.Condition{}, false
	}
	//we need to make a copy of the slice
	copiedConditions := []metav1.Condition{}
	for _, condition := range conditions {
		copiedConditions = append(copiedConditions, condition)
	}
	sort.Slice(copiedConditions, func(i, j int) bool {
		return copiedConditions[i].LastTransitionTime.Before(&copiedConditions[j].LastTransitionTime)
	})
	return copiedConditions[len(copiedConditions)-1], true
}
