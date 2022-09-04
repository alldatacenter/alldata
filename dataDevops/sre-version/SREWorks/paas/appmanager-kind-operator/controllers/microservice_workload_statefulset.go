package controllers

import (
	appmanagerabmiov1 "appmanager-operator/api/v1"
	"appmanager-operator/helper"
	"context"
	"fmt"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// +kubebuilder:rbac:groups=apps,resources=statefulsets,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=apps,resources=statefulsets/status,verbs=get

// 构造一个新的 StatefulSet
func (r *MicroserviceReconciler) constructForStatefulSet(raw *appmanagerabmiov1.Microservice) (*appsv1.StatefulSet, error) {
	var statefulset *appsv1.StatefulSet
	statefulset = &appsv1.StatefulSet{
		ObjectMeta: metav1.ObjectMeta{
			Name:        raw.Name,
			Namespace:   raw.Namespace,
			Labels:      raw.ObjectMeta.Labels,
			Annotations: raw.ObjectMeta.Annotations,
		},
		Spec: appsv1.StatefulSetSpec{
			ServiceName: raw.Name,
			Replicas:    raw.Spec.Replicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: raw.ObjectMeta.Labels,
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:        raw.Name,
					Namespace:   raw.Namespace,
					Labels:      raw.ObjectMeta.Labels,
					Annotations: raw.ObjectMeta.Annotations,
				},
				Spec: corev1.PodSpec{
					InitContainers: raw.Spec.InitContainers,
					Containers:     raw.Spec.Containers,
					Volumes:        raw.Spec.Volumes,
					Affinity:       raw.Spec.Affinity,
					Tolerations:    raw.Spec.Tolerations,
				},
			},
		},
	}

	// set owner reference
	if err := ctrl.SetControllerReference(raw, statefulset, r.Scheme); err != nil {
		return nil, err
	}
	return statefulset, nil
}

func (r *MicroserviceReconciler) ReconcileMicroserviceStatefulSet(
	ctx context.Context, req ctrl.Request, microservice *appmanagerabmiov1.Microservice,
	targetRevision string) error {

	log := r.Log.WithValues("microservice", req.NamespacedName)

	var statefulSetList appsv1.StatefulSetList
	if err := helper.ListChildren(r, ctx, &statefulSetList, &req, helper.StatefulsetOwnerKey); err != nil {
		log.Error(err, "list children failed")
		return nil
	}

	// 当进行 kind 变换的时候，删除所有非当前类型的其他资源
	if err := helper.RemoveUselessKindResource(r.Client, ctx, &req, helper.StatefulsetOwnerKey); err != nil {
		log.Error(err, "remove useless kind resource failed")
		return nil
	}

	itemCount := len(statefulSetList.Items)
	discardCount := 0
	deleted := false
	for i := range statefulSetList.Items {
		current := &statefulSetList.Items[i]
		currentRevision, err := helper.GetRevision(&current.ObjectMeta)
		if err != nil || targetRevision != currentRevision {
			if err := r.Delete(ctx, current, client.PropagationPolicy(metav1.DeletePropagationBackground)); client.IgnoreNotFound(err) != nil {
				log.Error(err, "unable to delete useless statefulset", "statefulset", current)
			} else {
				log.V(0).Info("deleted useless statefulset", "statefulset", current)
			}
			discardCount++
			deleted = true
			continue
		}
	}
	if deleted {
		return nil
	}

	// 已经全部删除或不存在可用 StatefulSet 了
	if discardCount == itemCount {
		statefulset, err := r.constructForStatefulSet(microservice)
		if err != nil {
			log.Error(err, "unable to construct statefulset in kubernetes context")
			return err
		}
		if err := r.Create(ctx, statefulset); err != nil {
			log.Error(err, "unable to create statefulset in kubernetes context",
				"statefulset", statefulset)
			return err
		}
		log.V(1).Info("created statefulset for appmanager microservice",
			"statefulset", statefulset)

		// 更新状态
		microservice.Status.Condition = appmanagerabmiov1.MicroserviceProgressing
		if err := r.Status().Update(ctx, microservice); err != nil {
			return err
		}
		log.V(1).Info(fmt.Sprintf("update microservice %s status to %s",
			microservice.Name, appmanagerabmiov1.MicroserviceProgressing))
		return nil
	}

	// 计算状态
	finalCondition := appmanagerabmiov1.MicroserviceUnknown
	for i := range statefulSetList.Items {
		current := &statefulSetList.Items[i]
		if current.Status.ReadyReplicas < *microservice.Spec.Replicas {
			finalCondition = appmanagerabmiov1.MicroserviceProgressing
		} else if current.Status.ReadyReplicas == *microservice.Spec.Replicas {
			finalCondition = appmanagerabmiov1.MicroserviceAvailable
		} else {
			log.Error(nil, "unknown readyReplicas number", "current", current)
		}
	}

	// 更新当前 microservice 的状态
	if microservice.Status.Condition != finalCondition {
		microservice.Status.Condition = finalCondition
		if err := r.Status().Update(ctx, microservice); err != nil {
			return err
		}
		log.V(1).Info(fmt.Sprintf("update microservice %s status to %s", microservice.Name, finalCondition))
	}
	return nil
}
