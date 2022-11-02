package controllers

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/go-logr/logr"
	kbatch "k8s.io/api/batch/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"

	abmiov1 "appmanager-operator/api/v1"
)

// JobReconciler reconciles a Job object
type JobReconciler struct {
	client.Client
	Log    logr.Logger
	Scheme *runtime.Scheme
}

var (
	scheduledTimeAnnotation = "job.apps.abm.io/scheduled-at"
	jobOwnerKey             = "abm.microservice.job"
)

// +kubebuilder:rbac:groups=apps.abm.io,resources=jobs,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=apps.abm.io,resources=jobs/status,verbs=get;update;patch
// +kubebuilder:rbac:groups=batch,resources=jobs,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=batch,resources=jobs/status,verbs=get

func (r *JobReconciler) isJobFinished(job *kbatch.Job) (bool, kbatch.JobConditionType) {
	for _, c := range job.Status.Conditions {
		if (c.Type == kbatch.JobComplete || c.Type == kbatch.JobFailed) && c.Status == corev1.ConditionTrue {
			return true, c.Type
		}
	}
	return false, ""
}

func (r *JobReconciler) getScheduledTimeForJob(job *kbatch.Job) (*time.Time, error) {
	timeRaw := job.Annotations[scheduledTimeAnnotation]
	if len(timeRaw) == 0 {
		return nil, nil
	}

	timeParsed, err := time.Parse(time.RFC3339, timeRaw)
	if err != nil {
		return nil, err
	}
	return &timeParsed, nil
}

// 构建新的 Job 并运行
func (r *JobReconciler) constructJobForjob(abmJob *abmiov1.Job, scheduledTime *time.Time) (*kbatch.Job, error) {
	// Job name 加个 UNIX 时间戳
	name := fmt.Sprintf("%s-%d", abmJob.Name, scheduledTime.Unix())
	backoffLimit := int32(100)
	activeDeadlineSeconds := int64(604800)

	job := &kbatch.Job{
		ObjectMeta: metav1.ObjectMeta{
			Labels:      abmJob.Labels,
			Annotations: abmJob.Annotations,
			Name:        name,
			Namespace:   abmJob.Namespace,
		},
		Spec: kbatch.JobSpec{
			BackoffLimit: &backoffLimit,
			ActiveDeadlineSeconds: &activeDeadlineSeconds,
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:        abmJob.Name,
					Namespace:   abmJob.Namespace,
					Labels:      abmJob.Labels,
					Annotations: abmJob.ObjectMeta.Annotations,
				},
				Spec: corev1.PodSpec{
					RestartPolicy: corev1.RestartPolicyOnFailure,
					Containers:    []corev1.Container{abmJob.Spec.Job},
					Affinity:      abmJob.Spec.Affinity,
					Tolerations:   abmJob.Spec.Tolerations,
				},
			},
		},
	}

	if err := ctrl.SetControllerReference(abmJob, job, r.Scheme); err != nil {
		return nil, err
	}
	return job, nil
}

// 根据当前 Spec 中的字段
func (r *JobReconciler) refillSpec(job *abmiov1.Job) error {
	// put env to containers
	for name, value := range job.Spec.Env {
		if job.Spec.Job.Env == nil {
			job.Spec.Job.Env = []corev1.EnvVar{}
		}
		job.Spec.Job.Env = append(job.Spec.Job.Env, corev1.EnvVar{
			Name:  name,
			Value: value,
		})
	}
	// put name to labels
	job.ObjectMeta.Labels["name"] = job.Name
	return nil
}

func (r *JobReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	log := r.Log.WithValues("job", req.NamespacedName)
	log.Info("Enter reconcile")
	var abmJob abmiov1.Job

	if err := r.Get(ctx, req.NamespacedName, &abmJob); client.IgnoreNotFound(err) != nil {
		log.Error(err, "unable to fetch appmanager job")
		return ctrl.Result{}, client.IgnoreNotFound(err)
	}

	// delete 事件忽略，已经都有 owner references 了，会自动清理
	if abmJob.ObjectMeta.Name == "" {
		return ctrl.Result{}, nil
	}

	// 获取当前 Job ID，用于确认系统中的历史数据
	id, ok := abmJob.ObjectMeta.Annotations["id"]
	if !ok {
		return ctrl.Result{}, errors.New("cannot find id field in annotations, abort!")
	}

	// 扫描当前自己拥有的所有 Job
	var childJobs kbatch.JobList
	if err := r.List(ctx, &childJobs, client.InNamespace(req.Namespace), client.MatchingFields{
		jobOwnerKey: req.Name,
	}); err != nil {
		log.Error(err, "unable to list child jobs of appmanager job")
		return ctrl.Result{}, nil
	}

	// 填充字段，符合 k8s 要求
	if err := r.refillSpec(&abmJob); err != nil {
		log.Error(err, "unable to refill appmanager job fields")
		return ctrl.Result{}, nil
	}

	// 当前正在活跃运行的 Job 列表
	var activeJobs []*kbatch.Job
	// 已成功的 Job 列表
	var successfulJobs []*kbatch.Job
	// 失败的 Job 列表
	var failedJobs []*kbatch.Job
	// 废弃的 Job 列表（和当前 CR 的 image tag 不同）
	var discardJobs []*kbatch.Job
	for i, job := range childJobs.Items {
		if len(job.Spec.Template.Spec.Containers) != 1 {
			log.Error(errors.New("job containers can only put 1 container"), "")
			return ctrl.Result{}, nil
		}
		targetId, ok := job.ObjectMeta.Annotations["id"]
		if !ok {
			log.Error(nil, "cannot find id field in sub job resources, skip", "job", job)
			continue
		}
		if id == targetId {
			_, finishedType := r.isJobFinished(&job)
			switch finishedType {
			case "":
				activeJobs = append(activeJobs, &childJobs.Items[i])
			case kbatch.JobFailed:
				failedJobs = append(failedJobs, &childJobs.Items[i])
			case kbatch.JobComplete:
				successfulJobs = append(failedJobs, &childJobs.Items[i])
			}
		} else {
			discardJobs = append(discardJobs, &childJobs.Items[i])
			continue
		}
	}

	// 更新自身的 Status 状态记录
	log.V(1).Info("job count",
		"active jobs", len(activeJobs),
		"successful jobs", len(successfulJobs),
		"failed jobs", len(failedJobs),
		"discard jobs", len(discardJobs))

	// 删除所有需要废弃的 Job
	for _, job := range discardJobs {
		if err := r.Delete(ctx, job, client.PropagationPolicy(metav1.DeletePropagationBackground)); client.IgnoreNotFound(err) != nil {
			log.Error(err, "unable to delete discard job", "job", job)
		} else {
			log.V(0).Info("deleted discard job", "job", job)
		}
	}

	// 如果当前有运行中/成功/失败的任务，reconcile 都不需要做事情，直接退出
	if len(activeJobs) > 0 {
		log.Info(fmt.Sprintf("job is running now, wait for finished, skip"))
		return ctrl.Result{}, nil
	} else if len(successfulJobs) > 0 {
		log.Info(fmt.Sprintf("job is successful, no need to do, skip"))
		return ctrl.Result{}, nil
	} else if len(failedJobs) > 0 {
		log.Info(fmt.Sprintf("job is failed status, no need to do, skip"))
		return ctrl.Result{}, nil
	}

	// 允许执行，发起一次新的 Job 部署
	now := time.Now()
	job, err := r.constructJobForjob(&abmJob, &now)
	if err != nil {
		log.Error(err, "unable to construct job from template")
		return ctrl.Result{}, nil
	}
	if err := r.Create(ctx, job); err != nil {
		if strings.Contains(err.Error(), "already exists") {
			return ctrl.Result{}, nil
		}
		log.Error(err, "unable to create job for appmanager job", "job", job)
		return ctrl.Result{}, err
	}
	log.V(1).Info("created job for appmanager", "job", job)
	return ctrl.Result{}, nil
}

func (r *JobReconciler) SetupWithManager(mgr ctrl.Manager) error {
	if err := mgr.GetFieldIndexer().IndexField(context.Background(), &kbatch.Job{}, jobOwnerKey, func(rawObj client.Object) []string {
		job := rawObj.(*kbatch.Job)
		owner := metav1.GetControllerOf(job)
		if owner == nil {
			return nil
		}
		if owner.APIVersion != apiGVStr || owner.Kind != "Job" {
			return nil
		}
		return []string{owner.Name}
	}); err != nil {
		return err
	}
	return ctrl.NewControllerManagedBy(mgr).
		For(&abmiov1.Job{}).
		Owns(&kbatch.Job{}).
		Complete(r)
}
