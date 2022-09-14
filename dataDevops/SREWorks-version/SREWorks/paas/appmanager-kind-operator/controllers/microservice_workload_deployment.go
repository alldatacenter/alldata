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

// +kubebuilder:rbac:groups=apps,resources=deployments,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=apps,resources=deployments/status,verbs=get

// 构造一个新的 Deployment
func (r *MicroserviceReconciler) constructForDeployment(raw *appmanagerabmiov1.Microservice) (*appsv1.Deployment, error) {
	var deployment *appsv1.Deployment
	deployment = &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:        raw.Name,
			Namespace:   raw.Namespace,
			Labels:      raw.ObjectMeta.Labels,
			Annotations: raw.ObjectMeta.Annotations,
		},
		Spec: appsv1.DeploymentSpec{
			ProgressDeadlineSeconds: raw.Spec.ProgressDeadlineSeconds,
			Replicas:                raw.Spec.Replicas,
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
	if err := ctrl.SetControllerReference(raw, deployment, r.Scheme); err != nil {
		return nil, err
	}
	return deployment, nil
}

func (r *MicroserviceReconciler) ReconcileMicroserviceDeployment(
	ctx context.Context, req ctrl.Request, microservice *appmanagerabmiov1.Microservice,
	targetRevision string) error {

	log := r.Log.WithValues("microservice", req.NamespacedName)

	var deploymentList appsv1.DeploymentList
	if err := helper.ListChildren(r, ctx, &deploymentList, &req, helper.DeploymentOwnerKey); err != nil {
		log.Error(err, "list children failed")
		return nil
	}

	// 当进行 kind 变换的时候，删除所有非当前类型的其他资源
	if err := helper.RemoveUselessKindResource(r.Client, ctx, &req, helper.DeploymentOwnerKey); err != nil {
		log.Error(err, "remove useless kind resource failed")
		return nil
	}

	itemCount := len(deploymentList.Items)
	discardCount := 0
	deleted := false
	for i := range deploymentList.Items {
		current := &deploymentList.Items[i]
		currentRevision, err := helper.GetRevision(&current.ObjectMeta)
		if err != nil || targetRevision != currentRevision {
			if err := r.Delete(ctx, current, client.PropagationPolicy(metav1.DeletePropagationBackground)); client.IgnoreNotFound(err) != nil {
				log.Error(err, "unable to delete useless deployments", "deployment", current)
			} else {
				log.V(1).Info("deleted useless deployments", "deployment", current)
			}
			discardCount++
			deleted = true
			continue
		}
	}
	if deleted {
		return nil
	}

	// 已经全部删除或不存在可用 Deployment 了
	if discardCount == itemCount {
		deployment, err := r.constructForDeployment(microservice)
		if err != nil {
			log.Error(err, "unable to construct deployment in kubernetes context")
			return err
		}
		if err := r.Create(ctx, deployment); err != nil {
			log.Error(err, "unable to create deployment in kubernetes context",
				"deployment", deployment)
			return err
		}
		log.V(1).Info("created deployment for appmanager microservice", "deployment", deployment)

		// 更新状态
		microservice.Status.Condition = appmanagerabmiov1.MicroserviceProgressing
		if err := r.Status().Update(ctx, microservice); err != nil {
			return err
		}
		log.V(1).Info(fmt.Sprintf("update microservice %s status to %s",
			microservice.Name, appmanagerabmiov1.MicroserviceProgressing))
		return nil
	}

	// 检查当前状态
	finalCondition := appmanagerabmiov1.MicroserviceUnknown
	for i := range deploymentList.Items {
		current := &deploymentList.Items[i]
		finalCondition = appmanagerabmiov1.MicroserviceProgressing
		for _, c := range current.Status.Conditions {
			log.V(1).Info(fmt.Sprintf("deployment condition: %s", c.Type), "c", c)
			if c.Type == appsv1.DeploymentAvailable && c.Status == corev1.ConditionTrue {
				finalCondition = appmanagerabmiov1.MicroserviceAvailable
				break
			} else if c.Type == appsv1.DeploymentReplicaFailure && c.Status == corev1.ConditionTrue {
				finalCondition = appmanagerabmiov1.MicroserviceFailure
				break
			} else if c.Type == appsv1.DeploymentProgressing && c.Status == corev1.ConditionFalse {
				finalCondition = appmanagerabmiov1.MicroserviceFailure
				break
			}
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
