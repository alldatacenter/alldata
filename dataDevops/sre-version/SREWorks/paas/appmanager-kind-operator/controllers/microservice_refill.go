package controllers

import (
	appmanagerabmiov1 "appmanager-operator/api/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// refillCompatibleSpec 用于填充老版本的兼容字段，已弃用
func (r *MicroserviceReconciler) refillCompatibleSpec(instance *appmanagerabmiov1.Microservice) error {
	if instance.Spec.ProgressDeadlineSeconds == nil || *instance.Spec.ProgressDeadlineSeconds == 0 {
		v := int32(300)
		instance.Spec.ProgressDeadlineSeconds = &v
	}

	// put env to containers
	for i := range instance.Spec.InitContainers {
		if instance.Spec.InitContainers[i].Env == nil {
			instance.Spec.InitContainers[i].Env = []corev1.EnvVar{}
		}
		existSet := make(map[string]bool)
		if instance.Spec.InitContainers[i].Env != nil {
			for _, envVar := range instance.Spec.InitContainers[i].Env {
				existSet[envVar.Name] = true
			}
		}
		for name, value := range instance.Spec.Env {
			if _, exists := existSet[name]; !exists {
				instance.Spec.InitContainers[i].Env = append(instance.Spec.InitContainers[i].Env, corev1.EnvVar{
					Name:  name,
					Value: value,
				})
			}
		}
	}
	for i := range instance.Spec.Containers {
		if instance.Spec.Containers[i].Env == nil {
			instance.Spec.Containers[i].Env = []corev1.EnvVar{}
		}
		existSet := make(map[string]bool)
		if instance.Spec.Containers[i].Env != nil {
			for _, envVar := range instance.Spec.Containers[i].Env {
				existSet[envVar.Name] = true
			}
		}
		for name, value := range instance.Spec.Env {
			if _, exists := existSet[name]; !exists {
				instance.Spec.Containers[i].Env = append(instance.Spec.Containers[i].Env, corev1.EnvVar{
					Name:  name,
					Value: value,
				})
			}
		}
	}

	// put name to labels
	instance.ObjectMeta.Labels["name"] = instance.Name
	return nil
}

// refillCloneSet 用于填充 CloneSet 中的 matchLabels 和 spec 下的 metadata.labels
func (r *MicroserviceReconciler) refillCloneSet(instance *appmanagerabmiov1.Microservice) error {
	if instance.Spec.CloneSet == nil {
		return nil
	}

	spec := instance.Spec.CloneSet
	spec.Selector = &metav1.LabelSelector{MatchLabels: instance.ObjectMeta.Labels}
	spec.Template.ObjectMeta.Labels = instance.ObjectMeta.Labels
	if instance.Spec.Replicas != nil {
		spec.Replicas = new(int32)
		*spec.Replicas = *instance.Spec.Replicas
	}
	return nil
}

// refillAdvancedStatefulSet 用于填充 AdvancedStatefulSet 中的 matchLabels 和 spec 下的 metadata.labels
func (r *MicroserviceReconciler) refillAdvancedStatefulSet(instance *appmanagerabmiov1.Microservice) error {
	if instance.Spec.AdvancedStatefulSet == nil {
		return nil
	}

	spec := instance.Spec.AdvancedStatefulSet
	spec.Selector = &metav1.LabelSelector{MatchLabels: instance.ObjectMeta.Labels}
	spec.Template.ObjectMeta.Labels = instance.ObjectMeta.Labels
	if instance.Spec.Replicas != nil {
		spec.Replicas = new(int32)
		*spec.Replicas = *instance.Spec.Replicas
	}
	return nil
}

// refill 根据已有内容重新填充当前 CR 中的字段
func (r *MicroserviceReconciler) refill(instance *appmanagerabmiov1.Microservice) error {
	if err := r.refillCompatibleSpec(instance); err != nil {
		return err
	}
	if err := r.refillCloneSet(instance); err != nil {
		return err
	}
	if err := r.refillAdvancedStatefulSet(instance); err != nil {
		return err
	}
	return nil
}
