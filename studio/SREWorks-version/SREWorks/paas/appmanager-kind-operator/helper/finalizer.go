package helper

import (
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

// IsBeingDeleted 返回当前对象是否已经被请求删除
func IsBeingDeleted(obj client.Object) bool {
	return !obj.GetDeletionTimestamp().IsZero()
}

// HasFinalizer 判断当前 object 是否有指定的 finalizer
func HasFinalizer(obj client.Object, finalizer string) bool {
	for _, fin := range obj.GetFinalizers() {
		if fin == finalizer {
			return true
		}
	}
	return false
}

// AddFinalizer 为指定对象增加 finalizer
func AddFinalizer(obj client.Object, finalizer string) {
	controllerutil.AddFinalizer(obj, finalizer)
}

// RemoveFinalizer 为指定对象删除 finalizer
func RemoveFinalizer(obj client.Object, finalizer string) {
	controllerutil.RemoveFinalizer(obj, finalizer)
}
