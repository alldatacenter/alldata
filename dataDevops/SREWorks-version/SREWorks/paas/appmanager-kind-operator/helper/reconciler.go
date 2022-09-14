package helper

import (
	"context"
	"errors"
	"io/ioutil"
	"os"
	ctrl "sigs.k8s.io/controller-runtime"
	"strings"
	"time"

	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/discovery"
	"k8s.io/client-go/dynamic"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/record"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

var log = ctrl.Log.WithName("helper")

// ReconcilerBase is a base struct from which all reconcilers can be derived from. By doing so your reconcilers will also inherit a set of utility functions
// To inherit from reconciler just build your finalizer this way:
// type MyReconciler struct {
//   helper.ReconcilerBase
//   ... other optional fields ...
// }
type ReconcilerBase struct {
	apireader  client.Reader
	client     client.Client
	scheme     *runtime.Scheme
	restConfig *rest.Config
	recorder   record.EventRecorder
}

func NewReconcilerBase(client client.Client, scheme *runtime.Scheme, restConfig *rest.Config, recorder record.EventRecorder, apireader client.Reader) ReconcilerBase {
	return ReconcilerBase{
		apireader:  apireader,
		client:     client,
		scheme:     scheme,
		restConfig: restConfig,
		recorder:   recorder,
	}
}

// NewReconcilerBase is a contruction function to create a new ReconcilerBase.
func NewFromManager(mgr manager.Manager, recorder record.EventRecorder) ReconcilerBase {
	return NewReconcilerBase(mgr.GetClient(), mgr.GetScheme(), mgr.GetConfig(), recorder, mgr.GetAPIReader())
}

//IsValid determines if a CR instance is valid. this implementation returns always true, should be overridden
func (r *ReconcilerBase) IsValid(obj metav1.Object) (bool, error) {
	return true, nil
}

//IsInitialized determines if a CR instance is initialized. this implementation returns always true, should be overridden
func (r *ReconcilerBase) IsInitialized(obj metav1.Object) bool {
	return true
}

// Reconcile is a stub function to have ReconsicerBase match the Reconciler interface. You must redefine this function
func (r *ReconcilerBase) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	return reconcile.Result{}, nil
}

// GetClient returns the underlying client
func (r *ReconcilerBase) GetClient() client.Client {
	return r.client
}

//GetRestConfig returns the undelying rest config
func (r *ReconcilerBase) GetRestConfig() *rest.Config {
	return r.restConfig
}

// GetRecorder returns the underlying recorder
func (r *ReconcilerBase) GetRecorder() record.EventRecorder {
	return r.recorder
}

// GetScheme returns the scheme
func (r *ReconcilerBase) GetScheme() *runtime.Scheme {
	return r.scheme
}

// GetDiscoveryClient returns a discovery client for the current reconciler
func (r *ReconcilerBase) GetDiscoveryClient() (*discovery.DiscoveryClient, error) {
	return discovery.NewDiscoveryClientForConfig(r.GetRestConfig())
}

// GetDynamicClientOnAPIResource returns a dynamic client on an APIResource. This client can be further namespaced.
func (r *ReconcilerBase) GetDynamicClientOnAPIResource(resource metav1.APIResource) (dynamic.NamespaceableResourceInterface, error) {
	return r.getDynamicClientOnGVR(schema.GroupVersionResource{
		Group:    resource.Group,
		Version:  resource.Version,
		Resource: resource.Name,
	})
}

func (r *ReconcilerBase) getDynamicClientOnGVR(gvr schema.GroupVersionResource) (dynamic.NamespaceableResourceInterface, error) {
	intf, err := dynamic.NewForConfig(r.GetRestConfig())
	if err != nil {
		log.Error(err, "Unable to get dynamic client")
		return nil, err
	}
	res := intf.Resource(gvr)
	return res, nil
}

// GetDynamicClientOnUnstructured returns a dynamic client on an Unstructured type. This client can be further namespaced.
// TODO consider refactoring using apimachinery.RESTClientForGVK in controller-runtime
func (r *ReconcilerBase) GetDynamicClientOnUnstructured(obj unstructured.Unstructured) (dynamic.ResourceInterface, error) {
	apiRes, err := r.getAPIReourceForUnstructured(obj)
	if err != nil {
		log.Error(err, "Unable to get apiresource from unstructured", "unstructured", obj)
		return nil, err
	}
	dc, err := r.GetDynamicClientOnAPIResource(apiRes)
	if err != nil {
		log.Error(err, "Unable to get namespaceable dynamic client from ", "resource", apiRes)
		return nil, err
	}
	if apiRes.Namespaced {
		return dc.Namespace(obj.GetNamespace()), nil
	}
	return dc, nil
}

// Deprecated - please use getAPIResourceForUnstructured instead.
func (r *ReconcilerBase) getAPIReourceForUnstructured(obj unstructured.Unstructured) (metav1.APIResource, error) {
	return r.getAPIResourceForUnstructured(obj)
}

func (r *ReconcilerBase) getAPIResourceForUnstructured(obj unstructured.Unstructured) (metav1.APIResource, error) {
	gvk := obj.GetObjectKind().GroupVersionKind()
	res := metav1.APIResource{}
	discoveryClient, err := r.GetDiscoveryClient()
	if err != nil {
		log.Error(err, "Unable to create discovery client")
		return res, err
	}
	resList, err := discoveryClient.ServerResourcesForGroupVersion(gvk.GroupVersion().String())
	if err != nil {
		log.Error(err, "Unable to retrieve resouce list for:", "groupversion", gvk.GroupVersion().String())
		return res, err
	}
	for _, resource := range resList.APIResources {
		if resource.Kind == gvk.Kind && !strings.Contains(resource.Name, "/") {
			res = resource
			res.Group = gvk.Group
			res.Version = gvk.Version
			break
		}
	}
	return res, nil
}

// CreateOrUpdateResource creates a resource if it doesn't exist, and updates (overwrites it), if it exist
// if owner is not nil, the owner field os set
// if namespace is not "", the namespace field of the object is overwritten with the passed value
func (r *ReconcilerBase) CreateOrUpdateResource(context context.Context, owner client.Object, namespace string, obj client.Object) error {

	if owner != nil {
		_ = controllerutil.SetControllerReference(owner, obj, r.GetScheme())
	}
	if namespace != "" {
		obj.SetNamespace(namespace)
	}

	obj2 := &unstructured.Unstructured{}
	obj2.SetGroupVersionKind(obj.GetObjectKind().GroupVersionKind())

	err := r.GetClient().Get(context, types.NamespacedName{
		Namespace: obj.GetNamespace(),
		Name:      obj.GetName(),
	}, obj2)

	if apierrors.IsNotFound(err) {
		err = r.GetClient().Create(context, obj)
		if err != nil {
			log.Error(err, "unable to create object", "object", obj)
			return err
		}
		return nil
	}
	if err == nil {
		obj.SetResourceVersion(obj2.GetResourceVersion())
		err = r.GetClient().Update(context, obj)
		if err != nil {
			log.Error(err, "unable to update object", "object", obj)
			return err
		}
		return nil

	}
	log.Error(err, "unable to lookup object", "object", obj)
	return err
}

// CreateOrUpdateResources operates as CreateOrUpdate, but on an array of resources
func (r *ReconcilerBase) CreateOrUpdateResources(context context.Context, owner client.Object, namespace string, objs []client.Object) error {
	for _, obj := range objs {
		err := r.CreateOrUpdateResource(context, owner, namespace, obj)
		if err != nil {
			return err
		}
	}
	return nil
}

// CreateOrUpdateUnstructuredResources operates as CreateOrUpdate, but on an array of unstructured.Unstructured
func (r *ReconcilerBase) CreateOrUpdateUnstructuredResources(context context.Context, owner client.Object, namespace string, objs []unstructured.Unstructured) error {
	for _, obj := range objs {
		err := r.CreateOrUpdateResource(context, owner, namespace, &obj)
		if err != nil {
			return err
		}
	}
	return nil
}

// DeleteResourceIfExists deletes an existing resource. It doesn't fail if the resource does not exist
func (r *ReconcilerBase) DeleteResourceIfExists(context context.Context, obj client.Object) error {
	err := r.GetClient().Delete(context, obj)
	if err != nil && !apierrors.IsNotFound(err) {
		log.Error(err, "unable to delete object ", "object", obj)
		return err
	}
	return nil
}

// DeleteResourcesIfExist operates like DeleteResources, but on an arrays of resources
func (r *ReconcilerBase) DeleteResourcesIfExist(context context.Context, objs []client.Object) error {
	for _, obj := range objs {
		err := r.DeleteResourceIfExists(context, obj)
		if err != nil {
			return err
		}
	}
	return nil
}

// DeleteUnstructuredResources operates like DeleteResources, but on an arrays of unstructured.Unstructured
func (r *ReconcilerBase) DeleteUnstructuredResources(context context.Context, objs []unstructured.Unstructured) error {
	for _, obj := range objs {
		err := r.DeleteResourceIfExists(context, &obj)
		if err != nil {
			return err
		}
	}
	return nil
}

// CreateResourceIfNotExists create a resource if it doesn't already exists. If the resource exists it is left untouched and the functin does not fails
// if owner is not nil, the owner field os set
// if namespace is not "", the namespace field of the object is overwritten with the passed value
func (r *ReconcilerBase) CreateResourceIfNotExists(context context.Context, owner client.Object, namespace string, obj client.Object) error {
	if owner != nil {
		_ = controllerutil.SetControllerReference(owner, obj, r.GetScheme())
	}
	if namespace != "" {
		obj.SetNamespace(namespace)
	}

	err := r.GetClient().Create(context, obj)
	if err != nil && !apierrors.IsAlreadyExists(err) {
		log.Error(err, "unable to create object ", "object", obj)
		return err
	}
	return nil
}

// CreateResourcesIfNotExist operates as CreateResourceIfNotExists, but on an array of resources
func (r *ReconcilerBase) CreateResourcesIfNotExist(context context.Context, owner client.Object, namespace string, objs []client.Object) error {
	for _, obj := range objs {
		err := r.CreateResourceIfNotExists(context, owner, namespace, obj)
		if err != nil {
			return err
		}
	}
	return nil
}

// CreateUnstructuredResourcesIfNotExist operates as CreateResourceIfNotExists, but on an array of unstructured.Unstructured
func (r *ReconcilerBase) CreateUnstructuredResourcesIfNotExist(context context.Context, owner client.Object, namespace string, objs []unstructured.Unstructured) error {
	for _, obj := range objs {
		err := r.CreateResourceIfNotExists(context, owner, namespace, &obj)
		if err != nil {
			return err
		}
	}
	return nil
}

// ManageOutcomeWithRequeue is a convenience function to call either ManageErrorWithRequeue if issue is non-nil, else ManageSuccessWithRequeue
func (r *ReconcilerBase) ManageOutcomeWithRequeue(context context.Context, obj client.Object, issue error, requeueAfter time.Duration) (reconcile.Result, error) {
	if issue != nil {
		return r.ManageErrorWithRequeue(context, obj, issue, requeueAfter)
	}
	return r.ManageSuccessWithRequeue(context, obj, requeueAfter)
}

//ManageErrorWithRequeue will take care of the following:
// 1. generate a warning event attached to the passed CR
// 2. set the status of the passed CR to a error condition if the object implements the apis.ConditionsStatusAware interface
// 3. return a reconcile status with with the passed requeueAfter and error
func (r *ReconcilerBase) ManageErrorWithRequeue(context context.Context, obj client.Object, issue error, requeueAfter time.Duration) (reconcile.Result, error) {
	r.GetRecorder().Event(obj, "Warning", "ProcessingError", issue.Error())
	if conditionsAware, updateStatus := (obj).(ConditionsAware); updateStatus {
		message := issue.Error()
		if len(message) == 0 {
			message = "no reason"
		}
		condition := metav1.Condition{
			Type:               ReconcileError,
			LastTransitionTime: metav1.Now(),
			ObservedGeneration: obj.GetGeneration(),
			Message:            message,
			Reason:             ReconcileErrorReason,
			Status:             metav1.ConditionTrue,
		}
		conditionsAware.SetConditions(AddOrReplaceCondition(condition, conditionsAware.GetConditions()))
		err := r.GetClient().Status().Update(context, obj)
		if err != nil {
			log.Error(err, "unable to update status")
			return reconcile.Result{RequeueAfter: requeueAfter}, err
		}
	} else {
		log.V(1).Info("object is not ConditionsAware, not setting status")
	}
	return reconcile.Result{RequeueAfter: requeueAfter}, issue
}

//ManageError will take care of the following:
// 1. generate a warning event attached to the passed CR
// 2. set the status of the passed CR to a error condition if the object implements the apis.ConditionsStatusAware interface
// 3. return a reconcile status with the passed error
func (r *ReconcilerBase) ManageError(context context.Context, obj client.Object, issue error) (reconcile.Result, error) {
	return r.ManageErrorWithRequeue(context, obj, issue, 0)
}

// ManageSuccessWithRequeue will update the status of the CR and return a successful reconcile result with requeueAfter set
func (r *ReconcilerBase) ManageSuccessWithRequeue(context context.Context, obj client.Object, requeueAfter time.Duration) (reconcile.Result, error) {
	if conditionsAware, updateStatus := (obj).(ConditionsAware); updateStatus {
		lastCondition, exists := GetLastCondition(conditionsAware.GetConditions())
		if exists && lastCondition.Type == ReconcileSuccess && lastCondition.Status == metav1.ConditionTrue {
			log.V(1).Info("current latest condition is success, skip")
		} else {
			condition := metav1.Condition{
				Type:               ReconcileSuccess,
				LastTransitionTime: metav1.Now(),
				ObservedGeneration: obj.GetGeneration(),
				Reason:             ReconcileSuccessReason,
				Status:             metav1.ConditionTrue,
				Message:            "OK",
			}
			conditionsAware.SetConditions(AddOrReplaceCondition(condition, conditionsAware.GetConditions()))
			err := r.GetClient().Status().Update(context, obj)
			if err != nil {
				log.Error(err, "unable to update status")
				return reconcile.Result{RequeueAfter: requeueAfter}, err
			}
		}
	} else {
		log.V(1).Info("object is not ConditionsAware, not setting status")
	}
	return reconcile.Result{RequeueAfter: requeueAfter}, nil
}

// ManageSuccess will update the status of the CR and return a successful reconcile result
func (r *ReconcilerBase) ManageSuccess(context context.Context, obj client.Object) (reconcile.Result, error) {
	return r.ManageSuccessWithRequeue(context, obj, 0)
}

//IsAPIResourceAvailable checks of a give GroupVersionKind is available in the running apiserver
func (r *ReconcilerBase) IsAPIResourceAvailable(GVK schema.GroupVersionKind) (bool, error) {
	discoveryClient, _ := r.GetDiscoveryClient()

	// Query for known OpenShift API resource to verify it is available
	apiResources, err := discoveryClient.ServerResourcesForGroupVersion(GVK.GroupVersion().String())

	if err != nil {
		if apierrors.IsNotFound(err) {
			return false, nil
		}
		log.Error(err, "Unable to retrive resources for", "GVK", GVK)
		return false, err
	}
	for _, resource := range apiResources.APIResources {
		if resource.Kind == GVK.Kind {
			return true, nil
		}
	}
	return false, nil
}

// GetDirectClient returns a non cached client
func (r *ReconcilerBase) GetDirectClient() (client.Client, error) {
	return r.GetDirectClientWithSchemeBuilders()
}

// GetDirectClientWithSchemeBuilders returns a non cached client initialized with the scheme.buidlers passed as parameters
func (r *ReconcilerBase) GetDirectClientWithSchemeBuilders(addToSchemes ...func(s *runtime.Scheme) error) (client.Client, error) {
	scheme := runtime.NewScheme()
	for _, addToscheme := range append(addToSchemes, clientgoscheme.AddToScheme) {
		err := addToscheme(scheme)
		if err != nil {
			log.Error(err, "unable to add scheme with", "adder", addToscheme)
			return nil, err
		}
	}
	client, err := client.New(r.GetRestConfig(), client.Options{
		Scheme: scheme,
	})
	if err != nil {
		log.Error(err, "unable to create client", "with restconfig", r.GetRestConfig())
		return nil, err
	}
	return client, nil
}

// GetAPIReader returns a non cached reader
func (r *ReconcilerBase) GetAPIReader() client.Reader {
	return r.apireader
}

// GetOperatorNamespace tries to infer the operator namespace. I first looks for the /var/run/secrets/kubernetes.io/serviceaccount/namespace file.
// Then it looks for a NAMESPACE environment variable (useful when running in local mode).
func (r *ReconcilerBase) GetOperatorNamespace() (string, error) {
	var namespaceFilePath = "/var/run/secrets/kubernetes.io/serviceaccount/namespace"
	b, err := ioutil.ReadFile(namespaceFilePath)
	if err != nil {
		namespace, ok := os.LookupEnv("NAMESPACE")
		if !ok {
			return "", errors.New("unable to infer namespace in which operator is running")
		}
		return namespace, nil
	}
	return string(b), nil
}
