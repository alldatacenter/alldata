package v1

//
//import (
//	"k8s.io/apimachinery/pkg/runtime"
//	ctrl "sigs.k8s.io/controller-runtime"
//	logf "sigs.k8s.io/controller-runtime/pkg/log"
//	"sigs.k8s.io/controller-runtime/pkg/webhook"
//)
//
//// log is for logging in this package.
//var joblog = logf.Log.WithName("job-resource")
//
//func (r *Job) SetupWebhookWithManager(mgr ctrl.Manager) error {
//	return ctrl.NewWebhookManagedBy(mgr).
//		For(r).
//		Complete()
//}
//
//// +kubebuilder:webhook:path=/mutate-apps-abm-io-v1-job,mutating=true,failurePolicy=fail,groups=apps.abm.io,resources=jobs,verbs=create;update,versions=v1,name=mjob.kb.io,sideEffects=none
//
//var _ webhook.Defaulter = &Job{}
//
//// Default implements webhook.Defaulter so a webhook will be registered for the type
//func (r *Job) Default() {
//	joblog.Info("default", "name", r.Name)
//}
//
//// +kubebuilder:webhook:verbs=create;update,path=/validate-apps-abm-io-v1-job,mutating=false,failurePolicy=fail,groups=apps.abm.io,resources=jobs,versions=v1,name=vjob.kb.io,sideEffects=none
//
//var _ webhook.Validator = &Job{}
//
//// ValidateCreate implements webhook.Validator so a webhook will be registered for the type
//func (r *Job) ValidateCreate() error {
//	joblog.Info("validate create", "name", r.Name)
//	return nil
//}
//
//// ValidateUpdate implements webhook.Validator so a webhook will be registered for the type
//func (r *Job) ValidateUpdate(old runtime.Object) error {
//	joblog.Info("validate update", "name", r.Name)
//	return nil
//}
//
//// ValidateDelete implements webhook.Validator so a webhook will be registered for the type
//func (r *Job) ValidateDelete() error {
//	joblog.Info("validate delete", "name", r.Name)
//	return nil
//}
