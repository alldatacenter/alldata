package helper

import (
	"context"
	"errors"
	"fmt"
	kruiseapps "github.com/openkruise/kruise-api/apps/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

const ClonesetOwnerKey = "abm.microservice.cloneset"
const AdvancedstatefulsetOwnerKey = "abm.microservice.advancedstatefulset"
const DeploymentOwnerKey = "abm.microservice.deployment"
const StatefulsetOwnerKey = "abm.microservice.statefulset"

// RevisionAnnotationKey 作为 annotations 中的 revision 的固定 key
const RevisionAnnotationKey = "annotations.appmanager.oam.dev/revision"

// GetRevision 获取 meta 中设置的 revision 字段，即当前服务的唯一版本标识
func GetRevision(meta *metav1.ObjectMeta) (string, error) {
	if meta == nil {
		return "", errors.New("cannot get revision because of nil meta reference")
	}

	// 标准字段 revision
	revision, ok := meta.Annotations[RevisionAnnotationKey]
	if ok {
		return revision, nil
	}

	// 兼容字段 id
	id, ok := meta.Annotations["id"]
	if ok {
		return id, nil
	}
	return "", errors.New("cannot find revision field in meta annotations")
}

// SetRevision 在 meta 中设置指定的 revision 字段内容
func SetRevision(meta *metav1.ObjectMeta, revision string) error {
	meta.Annotations[RevisionAnnotationKey] = revision
	return nil
}

// ListChildren 获取指定 ownerKey 下的所有儿子引用节点并返回引用列表到 children 对象中
func ListChildren(r client.Reader, ctx context.Context, children client.ObjectList, req *ctrl.Request, ownerKey string) error {
	if err := r.List(ctx, children, client.InNamespace(req.Namespace), client.MatchingFields{
		ownerKey: req.Name,
	}); client.IgnoreNotFound(err) != nil {
		return errors.New(fmt.Sprintf("unable to list children for %s: %s", ownerKey, req.Name))
	}
	return nil
}

// RemoveUselessKindResource 删除所有非 ownerKey 的其他所有关联资源，用于 kind 变换时的清理
func RemoveUselessKindResource(r client.Client, ctx context.Context, req *ctrl.Request, ownerKey string) error {
	// CloneSet
	var cloneSetList kruiseapps.CloneSetList
	if ownerKey != ClonesetOwnerKey {
		if err := r.List(ctx, &cloneSetList, client.InNamespace(req.Namespace), client.MatchingFields{ClonesetOwnerKey: req.Name}); err == nil {
			for i := range cloneSetList.Items {
				current := &cloneSetList.Items[i]
				if err := r.Delete(ctx, current, client.PropagationPolicy(metav1.DeletePropagationForeground)); client.IgnoreNotFound(err) != nil {
					log.Error(err, "unable to delete useless cloneset", "CloneSet", current)
				} else {
					log.V(1).Info("deleted useless cloneset", "CloneSet", current)
				}
			}
		}
	}

	// AdvancedStatefulSet
	var advancedStatefulSetList kruiseapps.StatefulSetList
	if ownerKey != AdvancedstatefulsetOwnerKey {
		if err := r.List(ctx, &advancedStatefulSetList, client.InNamespace(req.Namespace), client.MatchingFields{AdvancedstatefulsetOwnerKey: req.Name}); err == nil {
			for i := range advancedStatefulSetList.Items {
				current := &advancedStatefulSetList.Items[i]
				if err := r.Delete(ctx, current, client.PropagationPolicy(metav1.DeletePropagationForeground)); client.IgnoreNotFound(err) != nil {
					log.Error(err, "unable to delete useless advancedStatefulSet", "AdvancedStatefulSet", current)
				} else {
					log.V(1).Info("deleted useless advancedStatefulSet", "AdvancedStatefulSet", current)
				}
			}
		}
	}

	// Deployment
	var deploymentList appsv1.DeploymentList
	if ownerKey != DeploymentOwnerKey {
		if err := r.List(ctx, &deploymentList, client.InNamespace(req.Namespace), client.MatchingFields{DeploymentOwnerKey: req.Name}); err == nil {
			for i := range deploymentList.Items {
				current := &deploymentList.Items[i]
				if err := r.Delete(ctx, current, client.PropagationPolicy(metav1.DeletePropagationForeground)); client.IgnoreNotFound(err) != nil {
					log.Error(err, "unable to delete useless deployment", "Deployment", current)
				} else {
					log.V(1).Info("deleted useless deployment", "Deployment", current)
				}
			}
		}
	}

	// StatefulSet
	var statefulSetList appsv1.StatefulSetList
	if ownerKey != StatefulsetOwnerKey {
		if err := r.List(ctx, &statefulSetList, client.InNamespace(req.Namespace), client.MatchingFields{StatefulsetOwnerKey: req.Name}); err == nil {
			for i := range statefulSetList.Items {
				current := &statefulSetList.Items[i]
				if err := r.Delete(ctx, current, client.PropagationPolicy(metav1.DeletePropagationForeground)); client.IgnoreNotFound(err) != nil {
					log.Error(err, "unable to delete useless statefulset", "Statefulset", current)
				} else {
					log.V(1).Info("deleted useless statefulset", "Statefulset", current)
				}
			}
		}
	}
	return nil
}
