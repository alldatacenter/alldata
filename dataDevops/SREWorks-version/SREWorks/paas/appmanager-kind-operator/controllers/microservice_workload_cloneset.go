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

// +kubebuilder:rbac:groups=apps.kruise.io,resources=clonesets,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=apps.kruise.io,resources=clonesets/status,verbs=get

// constructForCloneSet 用于通过 Microservice 对象构建 CloneSet 对象
func (r *MicroserviceReconciler) constructForCloneSet(raw *appmanagerabmiov1.Microservice) (*kruiseapps.CloneSet, error) {
	cloneSet := &kruiseapps.CloneSet{
		ObjectMeta: metav1.ObjectMeta{
			Name:        raw.Name,
			Namespace:   raw.Namespace,
			Labels:      raw.ObjectMeta.Labels,
			Annotations: raw.ObjectMeta.Annotations,
		},
		Spec: *raw.Spec.CloneSet,
	}

	// set CloneSet metadata
	cloneSet.Spec.Template.ObjectMeta = metav1.ObjectMeta{
		Name:        raw.Name,
		Namespace:   raw.Namespace,
		Labels:      raw.ObjectMeta.Labels,
		Annotations: raw.ObjectMeta.Annotations,
	}

	// set owner reference
	if err := ctrl.SetControllerReference(raw, cloneSet, r.Scheme); err != nil {
		return nil, err
	}
	return cloneSet, nil
}

func (r *MicroserviceReconciler) ReconcileMicroserviceCloneSet(ctx context.Context, req ctrl.Request,
	microservice *appmanagerabmiov1.Microservice, targetRevision string) error {

	log := r.Log.WithValues("microservice", req.NamespacedName)

	// 前置检查
	if microservice.Spec.CloneSet == nil {
		log.Error(errors.New("CloneSet spec is not set, abort"), "cannot reconcile microservice cloneset")
		return nil
	}

	// 获取当前 CR 下所有附属的 CloneSet 列表
	var cloneSetList kruiseapps.CloneSetList
	if err := helper.ListChildren(r, ctx, &cloneSetList, &req, helper.ClonesetOwnerKey); err != nil {
		log.Error(err, "list children failed")
		return nil
	}

	// 当进行 kind 变换的时候，删除所有非当前类型的其他资源
	if err := helper.RemoveUselessKindResource(r.Client, ctx, &req, helper.ClonesetOwnerKey); err != nil {
		log.Error(err, "remove useless kind resource failed")
		return nil
	}

	length := len(cloneSetList.Items)
	if length == 0 {
		// 当不存在 CloneSet 的时候直接新建
		cloneSet, err := r.constructForCloneSet(microservice)
		if err != nil {
			log.Error(err, "unable to construct CloneSet in kubernetes context")
			return err
		}
		if err := r.Create(ctx, cloneSet); err != nil {
			log.Error(err, "unable to create CloneSet in kubernetes context", "CloneSet", cloneSet)
			return err
		}
		log.V(1).Info("created CloneSet for appmanager microservice", "CloneSet", cloneSet)
		return nil
	} else if length > 1 {
		// 当存在超过 1 个附属资源时删除到只剩一个
		log.Error(errors.New("multiple CloneSets found"), "unexpected error", "CloneSetList", cloneSetList)

		for i := range cloneSetList.Items {
			if i >= length-1 {
				break
			}
			current := &cloneSetList.Items[i]
			if err := r.Delete(ctx, current, client.PropagationPolicy(metav1.DeletePropagationForeground)); client.IgnoreNotFound(err) != nil {
				log.Error(err, "unable to delete useless cloneset", "CloneSet", current)
			} else {
				log.V(1).Info("deleted useless cloneset", "CloneSet", current)
			}
		}
		return nil
	}

	instance := &cloneSetList.Items[0]
	currentRevision, err := helper.GetRevision(&instance.ObjectMeta)
	if err != nil {
		log.Error(err, fmt.Sprintf("unable to get current hash in CloneSet %+v", instance.ObjectMeta))
		return err
	}

	// 更新当前状态
	finalCondition := appmanagerabmiov1.MicroserviceUnknown
	if currentRevision != targetRevision ||
		instance.Status.CurrentRevision != instance.Status.UpdateRevision ||
		instance.Status.UpdatedReadyReplicas < *instance.Spec.Replicas {
		finalCondition = appmanagerabmiov1.MicroserviceProgressing
	} else if instance.Status.UpdatedReadyReplicas == *instance.Spec.Replicas {
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
		log.Info("current microservice revision is equal to CloneSet revision")
		return nil
	}

	// 进行 Spec 替换
	*instance.Spec.Replicas = *microservice.Spec.CloneSet.Replicas
	instance.Spec.Template = microservice.Spec.CloneSet.Template
	instance.Spec.Template.ObjectMeta = metav1.ObjectMeta{
		Name:        microservice.Name,
		Namespace:   microservice.Namespace,
		Labels:      microservice.ObjectMeta.Labels,
		Annotations: microservice.ObjectMeta.Annotations,
	}
	instance.Spec.Lifecycle = microservice.Spec.CloneSet.Lifecycle
	instance.Spec.ScaleStrategy = microservice.Spec.CloneSet.ScaleStrategy
	instance.Spec.UpdateStrategy = microservice.Spec.CloneSet.UpdateStrategy
	instance.Spec.MinReadySeconds = microservice.Spec.CloneSet.MinReadySeconds
	if microservice.Spec.CloneSet.RevisionHistoryLimit != nil {
		*instance.Spec.RevisionHistoryLimit = *microservice.Spec.CloneSet.RevisionHistoryLimit
	}
	instance.Spec.VolumeClaimTemplates = microservice.Spec.CloneSet.VolumeClaimTemplates
	_ = helper.SetRevision(&instance.ObjectMeta, targetRevision)
	if err := r.GetClient().Update(ctx, instance); err != nil {
		log.Error(err, "unable to update cloneset, prepare to delete", "CloneSet", instance)
		if err := r.Delete(ctx, instance, client.PropagationPolicy(metav1.DeletePropagationForeground)); client.IgnoreNotFound(err) != nil {
			log.Error(err, "unable to delete conflict cloneset", "CloneSet", instance)
			return err
		}
		log.V(1).Info("deleted conflict cloneset", "CloneSet", instance)
		return nil
	}
	log.V(1).Info(fmt.Sprintf("update CloneSet %s spec to %+v", microservice.Name, instance.Spec))
	return nil
}
