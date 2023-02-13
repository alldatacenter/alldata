package controllers

import (
	appmanagerabmiov1 "appmanager-operator/api/v1"
	"appmanager-operator/helper"
	"appmanager-operator/lib/hashstructure"
	"context"
	"errors"
	"fmt"
	"github.com/go-logr/logr"
	kruiseapps "github.com/openkruise/kruise-api/apps/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"strconv"
)

// MicroserviceReconciler reconciles a MicroService object
type MicroserviceReconciler struct {
	helper.ReconcilerBase
	client.Client
	Log    logr.Logger
	Scheme *runtime.Scheme
}

const controllerName = "microservice.finalizer.apps.abm.io"

var apiGVStr = appmanagerabmiov1.GroupVersion.String()
var log = ctrl.Log.WithName(controllerName)

// +kubebuilder:rbac:groups=apps.abm.io,resources=microservices,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=apps.abm.io,resources=microservices/status,verbs=get;update;patch
// +kubebuilder:rbac:groups=core,resources=services,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=core,resources=services,verbs=get;list;watch;create;update;patch;delete

func (r *MicroserviceReconciler) manageOperatorLogic(context context.Context, req ctrl.Request,
	instance *appmanagerabmiov1.Microservice) error {

	targetRevision, err := helper.GetRevision(&instance.ObjectMeta)
	if err != nil {
		return err
	}

	// Reconcile
	switch instance.Spec.Kind {
	case appmanagerabmiov1.KIND_DEPLOYMENT:
		return r.ReconcileMicroserviceDeployment(context, req, instance, targetRevision)
	case appmanagerabmiov1.KIND_STATEFULSET:
		return r.ReconcileMicroserviceStatefulSet(context, req, instance, targetRevision)
	case appmanagerabmiov1.KIND_CLONESET:
		return r.ReconcileMicroserviceCloneSet(context, req, instance, targetRevision)
	case appmanagerabmiov1.KIND_ADVANCEDSTATEFULSET:
		return r.ReconcileMicroserviceAdvancedStatefulSet(context, req, instance, targetRevision)
	default:
		return errors.New(fmt.Sprintf("not valid microservice kind, detail=%+v", instance))
	}
}

func (r *MicroserviceReconciler) IsValid(instance *appmanagerabmiov1.Microservice) (bool, error) {
	return true, nil
}

// IsInitialized 判断当前是否已经完成初始化动作，如果没有，增加 finalizer 和 revision annotations，并返回 false 触发更新
func (r *MicroserviceReconciler) IsInitialized(instance *appmanagerabmiov1.Microservice) bool {
	if instance.Spec.Initialized {
		return true
	}
	// helper.AddFinalizer(instance, controllerName)
	instance.Spec.Initialized = true
	return false
}

// calculateRevision 用于计算当前 Microservice CR 的 Spec 对应的 Hash 值
func (r *MicroserviceReconciler) calculateRevision(instance *appmanagerabmiov1.Microservice) (string, error) {
	var revision uint64
	var err error
	switch instance.Spec.Kind {
	case appmanagerabmiov1.KIND_DEPLOYMENT:
		revision, err = hashstructure.Hash(instance.Spec, hashstructure.FormatV2, nil)
	case appmanagerabmiov1.KIND_STATEFULSET:
		revision, err = hashstructure.Hash(instance.Spec, hashstructure.FormatV2, nil)
	case appmanagerabmiov1.KIND_CLONESET:
		revision, err = hashstructure.Hash(instance.Spec.CloneSet, hashstructure.FormatV2, nil)
	case appmanagerabmiov1.KIND_ADVANCEDSTATEFULSET:
		revision, err = hashstructure.Hash(instance.Spec.AdvancedStatefulSet, hashstructure.FormatV2, nil)
	default:
		return "", errors.New(fmt.Sprintf("invalid microservice kind %s", instance.Spec.Kind))
	}
	if err != nil {
		return "", err
	}
	return strconv.FormatUint(revision, 10), nil
}

func (r *MicroserviceReconciler) Reconcile(context context.Context, req ctrl.Request) (ctrl.Result, error) {
	log := r.Log
	log.Info("enter microservice reconcile progress", "Request", req)

	instance := &appmanagerabmiov1.Microservice{}
	if err := r.GetAPIReader().Get(context, req.NamespacedName, instance); client.IgnoreNotFound(err) != nil {
		log.Error(err, "unable to fetch current instance, requeue")
		return ctrl.Result{}, err
	}
	log = log.WithValues("name", instance.ObjectMeta.Name, "namespace", instance.ObjectMeta.Namespace, "labels",
		instance.ObjectMeta.Labels, "resourceVersion", instance.ObjectMeta.ResourceVersion)
	if instance.GetDeletionTimestamp() != nil {
		log = log.WithValues("deletionTimestamp", instance.GetDeletionTimestamp())
	}
	log.Info("get microservice cr from apiserver")
	if instance.ObjectMeta.Name == "" {
		return ctrl.Result{}, nil
	}

	if ok, err := r.IsValid(instance); !ok {
		return r.ManageError(context, instance, err)
	}
	log.Info("validate microservice cr success")

	// 填充字段，符合 k8s 要求
	if err := r.refill(instance); err != nil {
		return r.ManageError(context, instance, err)
	}

	// 将当前的 Spec 对应的 hash revision 计算出来并设置到 annotations 中
	targetRevision, err := r.calculateRevision(instance)
	if err != nil {
		return r.ManageError(context, instance, err)
	}
	currentRevision, err := helper.GetRevision(&instance.ObjectMeta)
	if err != nil {
		_ = helper.SetRevision(&instance.ObjectMeta, targetRevision)
		instance.Spec.Initialized = false
		log.Info(fmt.Sprintf("no revision found in microservice cr, revision %s has put into microservice cr", targetRevision))
	} else if targetRevision != currentRevision {
		_ = helper.SetRevision(&instance.ObjectMeta, targetRevision)
		instance.Spec.Initialized = false
		log.Info(fmt.Sprintf("cr has changed spec, revision %s has put into microservice cr, origin is %s", targetRevision, currentRevision))
	}

	// 初始化
	if ok := r.IsInitialized(instance); !ok {
		log.Info("cr is not initialized, prepare to update it")
		err := r.GetClient().Update(context, instance)
		if err != nil {
			log.Error(err, "unable to update instance", "instance", instance)
			return r.ManageError(context, instance, err)
		}
		log.Info("cr is initialzied, exit")
		return reconcile.Result{}, nil
	}
	log.Info("check initialization finished")

	if err = r.manageOperatorLogic(context, req, instance); err != nil {
		return r.ManageError(context, instance, err)
	}
	log.Info("cr operator logic has finished")
	return r.ManageSuccess(context, instance)
}

func (r *MicroserviceReconciler) SetupWithManager(mgr ctrl.Manager) error {
	if err := mgr.GetFieldIndexer().IndexField(context.Background(), &appsv1.Deployment{}, helper.DeploymentOwnerKey, func(rawObj client.Object) []string {
		deployment := rawObj.(*appsv1.Deployment)
		owner := metav1.GetControllerOf(deployment)
		if owner == nil {
			return nil
		}
		if owner.APIVersion != apiGVStr || owner.Kind != "Microservice" {
			return nil
		}
		return []string{owner.Name}
	}); err != nil {
		return err
	}

	if err := mgr.GetFieldIndexer().IndexField(context.Background(), &appsv1.StatefulSet{}, helper.StatefulsetOwnerKey, func(rawObj client.Object) []string {
		statefulSet := rawObj.(*appsv1.StatefulSet)
		owner := metav1.GetControllerOf(statefulSet)
		if owner == nil {
			return nil
		}
		if owner.APIVersion != apiGVStr || owner.Kind != "Microservice" {
			return nil
		}
		return []string{owner.Name}
	}); err != nil {
		return err
	}

	if err := mgr.GetFieldIndexer().IndexField(context.Background(), &kruiseapps.CloneSet{}, helper.ClonesetOwnerKey, func(rawObj client.Object) []string {
		cloneSet := rawObj.(*kruiseapps.CloneSet)
		owner := metav1.GetControllerOf(cloneSet)
		if owner == nil {
			return nil
		}
		if owner.APIVersion != apiGVStr || owner.Kind != "Microservice" {
			return nil
		}
		return []string{owner.Name}
	}); err != nil {
		return err
	}

	if err := mgr.GetFieldIndexer().IndexField(context.Background(), &kruiseapps.StatefulSet{}, helper.AdvancedstatefulsetOwnerKey, func(rawObj client.Object) []string {
		advancedStatefulSet := rawObj.(*kruiseapps.StatefulSet)
		owner := metav1.GetControllerOf(advancedStatefulSet)
		if owner == nil {
			return nil
		}
		if owner.APIVersion != apiGVStr || owner.Kind != "Microservice" {
			return nil
		}
		return []string{owner.Name}
	}); err != nil {
		return err
	}

	return ctrl.NewControllerManagedBy(mgr).
		For(&appmanagerabmiov1.Microservice{}).
		Owns(&appsv1.Deployment{}).
		Owns(&appsv1.StatefulSet{}).
		Owns(&kruiseapps.CloneSet{}).
		Owns(&kruiseapps.StatefulSet{}).
		Complete(r)
}
