package controllers

import (
	appmanagerabmiov1 "appmanager-operator/api/v1"
	"appmanager-operator/helper"
	"context"
	"errors"
	"fmt"
	kruiseapps "github.com/openkruise/kruise-api/apps/v1alpha1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// +kubebuilder:rbac:groups=apps.kruise.io,resources=statefulsets,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=apps.kruise.io,resources=statefulsets/status,verbs=get

// constructForAdvancedStatefulSet 用于通过 Microservice 对象构建 AdvancedStatefulSet 对象
func (r *MicroserviceReconciler) constructForAdvancedStatefulSet(raw *appmanagerabmiov1.Microservice) (*kruiseapps.StatefulSet, error) {
	advancedStatefulSet := &kruiseapps.StatefulSet{
		ObjectMeta: metav1.ObjectMeta{
			Name:        raw.Name,
			Namespace:   raw.Namespace,
			Labels:      raw.ObjectMeta.Labels,
			Annotations: raw.ObjectMeta.Annotations,
		},
		Spec: *raw.Spec.AdvancedStatefulSet,
	}

	// set AdvancedStatefulSet metadata
	advancedStatefulSet.Spec.Template.ObjectMeta = metav1.ObjectMeta{
		Name:        raw.Name,
		Namespace:   raw.Namespace,
		Labels:      raw.ObjectMeta.Labels,
		Annotations: raw.ObjectMeta.Annotations,
	}

	// set owner reference
	if err := ctrl.SetControllerReference(raw, advancedStatefulSet, r.Scheme); err != nil {
		return nil, err
	}
	return advancedStatefulSet, nil
}

func (r *MicroserviceReconciler) ReconcileMicroserviceAdvancedStatefulSet(
	ctx context.Context, req ctrl.Request, microservice *appmanagerabmiov1.Microservice, targetRevision string) error {

	log := r.Log.WithValues("microservice", req.NamespacedName)

	// 前置检查
	if microservice.Spec.AdvancedStatefulSet == nil {
		log.Error(errors.New("AdvancedStatefulSet spec is not set, abort"), "cannot reconcile microservice advancedStatefulSet")
		return nil
	}

	// 获取当前 CR 下所有附属的 AdvancedStatefulSet 列表
	var advancedStatefulSetList kruiseapps.StatefulSetList
	if err := helper.ListChildren(r, ctx, &advancedStatefulSetList, &req, helper.AdvancedstatefulsetOwnerKey); err != nil {
		log.Error(err, "list children failed")
		return nil
	}

	// 当进行 kind 变换的时候，删除所有非当前类型的其他资源
	if err := helper.RemoveUselessKindResource(r.Client, ctx, &req, helper.AdvancedstatefulsetOwnerKey); err != nil {
		log.Error(err, "remove useless kind resource failed")
		return nil
	}

	length := len(advancedStatefulSetList.Items)
	if length == 0 {
		// 当不存在 AdvancedStatefulSet 的时候直接新建
		advancedStatefulSet, err := r.constructForAdvancedStatefulSet(microservice)
		if err != nil {
			log.Error(err, "unable to construct AdvancedStatefulSet in kubernetes context")
			return err
		}
		if err := r.Create(ctx, advancedStatefulSet); err != nil {
			log.Error(err, "unable to create AdvancedStatefulSet in kubernetes context", "AdvancedStatefulSet", advancedStatefulSet)
			return err
		}
		log.V(1).Info("created AdvancedStatefulSet for appmanager microservice", "AdvancedStatefulSet", advancedStatefulSet)
		return nil
	} else if length > 1 {
		// 当存在超过 1 个附属资源时删除到只剩一个
		log.Error(errors.New("multiple AdvancedStatefulSets found"), "unexpected error", "AdvancedStatefulSetList", advancedStatefulSetList)

		for i := range advancedStatefulSetList.Items {
			if i >= length-1 {
				break
			}
			current := &advancedStatefulSetList.Items[i]
			if err := r.Delete(ctx, current, client.PropagationPolicy(metav1.DeletePropagationForeground)); client.IgnoreNotFound(err) != nil {
				log.Error(err, "unable to delete useless advancedStatefulSet", "AdvancedStatefulSet", current)
			} else {
				log.V(1).Info("deleted useless advancedStatefulSet", "AdvancedStatefulSet", current)
			}
		}
		return nil
	}

	instance := &advancedStatefulSetList.Items[0]
	currentRevision, err := helper.GetRevision(&instance.ObjectMeta)
	if err != nil {
		log.Error(err, fmt.Sprintf("unable to get current hash in AdvancedStatefulSet %+v", instance.ObjectMeta))
		return err
	}

	// 计算 microservice 状态
	finalCondition := appmanagerabmiov1.MicroserviceUnknown
	if currentRevision != targetRevision ||
		instance.Status.ReadyReplicas < *microservice.Spec.Replicas {
		finalCondition = appmanagerabmiov1.MicroserviceProgressing
	} else if instance.Status.ReadyReplicas == *microservice.Spec.Replicas {
		finalCondition = appmanagerabmiov1.MicroserviceAvailable
	}
	if microservice.Status.Condition != finalCondition {
		microservice.Status.Condition = finalCondition
		if err := r.Status().Update(ctx, microservice); err != nil {
			return err
		}
		log.V(1).Info(fmt.Sprintf("update microservice %s status to %s", microservice.Name, finalCondition))
	}

	// 计算 hash 是否一致
	if currentRevision == targetRevision {
		log.Info("current microservice revision is equal to AdvancedStatefulSet revision, skip")
		return nil
	}

	// 进行 Spec 替换
	*instance.Spec.Replicas = *microservice.Spec.AdvancedStatefulSet.Replicas
	instance.Spec.Template = microservice.Spec.AdvancedStatefulSet.Template
	instance.Spec.Template.ObjectMeta = metav1.ObjectMeta{
		Name:        microservice.Name,
		Namespace:   microservice.Namespace,
		Labels:      microservice.ObjectMeta.Labels,
		Annotations: microservice.ObjectMeta.Annotations,
	}
	instance.Spec.UpdateStrategy = microservice.Spec.AdvancedStatefulSet.UpdateStrategy
	instance.Spec.VolumeClaimTemplates = microservice.Spec.AdvancedStatefulSet.VolumeClaimTemplates
	_ = helper.SetRevision(&instance.ObjectMeta, targetRevision)
	if err := r.GetClient().Update(ctx, instance); err != nil {
		log.Error(err, "unable to update advancedstatefulsets, prepare to delete", "AdvancedStatefulSet", instance)
		if err := r.Delete(ctx, instance, client.PropagationPolicy(metav1.DeletePropagationForeground)); client.IgnoreNotFound(err) != nil {
			log.Error(err, "unable to delete conflict advancedStatefulSet", "AdvancedStatefulSet", instance)
			return err
		}
		log.V(1).Info("deleted conflict advancedStatefulSet", "AdvancedStatefulSet", instance)
		return nil
	}
	log.V(1).Info(fmt.Sprintf("update AdvancedStatefulSet %s spec to %+v", microservice.Name, instance.Spec))
	return nil
}
