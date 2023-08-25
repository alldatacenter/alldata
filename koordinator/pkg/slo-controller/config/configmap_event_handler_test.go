/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package config

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/util/workqueue"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

func cacheChangedTrue(configMap *corev1.ConfigMap) bool {
	return true
}

func cacheChangedFalse(configMap *corev1.ConfigMap) bool {
	return false
}

func enqueueRequest(q *workqueue.RateLimitingInterface) {
	(*q).Add(reconcile.Request{
		NamespacedName: types.NamespacedName{
			Name: "test",
		},
	})
}

func Test_common_Create(t *testing.T) {
	type args struct {
		evt                event.CreateEvent
		cacheChangedReturn func(configMap *corev1.ConfigMap) bool
	}
	type want struct {
		objs []interface{}
	}
	tests := []struct {
		name string
		args args
		want want
	}{
		{
			name: "normal-1-nocache",
			args: args{
				evt: event.CreateEvent{
					Object: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
					}),
				},
			},
			want: want{
				objs: []interface{}{
					reconcile.Request{NamespacedName: types.NamespacedName{Name: "test"}},
				},
			},
		},
		{
			name: "normal-1-cacheChanged-true",
			args: args{
				evt: event.CreateEvent{
					Object: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
					}),
				},
				cacheChangedReturn: cacheChangedTrue,
			},
			want: want{
				objs: []interface{}{
					reconcile.Request{NamespacedName: types.NamespacedName{Name: "test"}},
				},
			},
		},
		{
			name: "normal-1-cacheChanged-false",
			args: args{
				evt: event.CreateEvent{
					Object: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
					}),
				},
				cacheChangedReturn: cacheChangedFalse,
			},
			want: want{
				objs: []interface{}{},
			},
		},
		{
			name: "no-update-1",
			args: args{
				evt: event.CreateEvent{
					Object: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      "test-config-map",
						},
					}),
				},
			},
			want: want{
				objs: []interface{}{},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := EnqueueRequestForConfigMap{EnqueueRequest: enqueueRequest}
			if tt.args.cacheChangedReturn != nil {
				p.SyncCacheIfChanged = tt.args.cacheChangedReturn
			}
			q := workqueue.NewRateLimitingQueue(workqueue.DefaultControllerRateLimiter())
			p.Create(tt.args.evt, q)

			assert.Equal(t, q.Len(), len(tt.want.objs), "Create() test fail, len expect equal!")

			for i := 0; i < len(tt.want.objs); i++ {
				obj, _ := q.Get()
				assert.Equal(t, tt.want.objs[i], obj, "Create() test fail, obj expect equal")
			}

		})
	}
}

func Test_common_Update(t *testing.T) {
	type args struct {
		evt                event.UpdateEvent
		cacheChangedReturn func(configMap *corev1.ConfigMap) bool
	}
	type want struct {
		objs []interface{}
	}
	tests := []struct {
		name string
		args args
		want want
	}{
		{
			name: "normal-1-nocache",
			args: args{
				evt: event.UpdateEvent{
					ObjectOld: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
					}),
					ObjectNew: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
						Data: map[string]string{
							"a": "a",
						},
					}),
				},
			},
			want: want{
				objs: []interface{}{
					reconcile.Request{NamespacedName: types.NamespacedName{Name: "test"}},
				},
			},
		},
		{
			name: "normal-1-cacheChanged-true",
			args: args{
				evt: event.UpdateEvent{
					ObjectOld: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
					}),
					ObjectNew: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
						Data: map[string]string{
							"a": "a",
						},
					}),
				},
				cacheChangedReturn: cacheChangedTrue,
			},
			want: want{
				objs: []interface{}{
					reconcile.Request{NamespacedName: types.NamespacedName{Name: "test"}},
				},
			},
		},
		{
			name: "normal-1-cacheChanged-false",
			args: args{
				evt: event.UpdateEvent{
					ObjectOld: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
					}),
					ObjectNew: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
						Data: map[string]string{
							"a": "a",
						},
					}),
				},
				cacheChangedReturn: cacheChangedFalse,
			},
			want: want{
				objs: []interface{}{},
			},
		},
		{
			name: "no-update-other-configmap",
			args: args{
				evt: event.UpdateEvent{
					ObjectOld: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      "other-name",
						},
					}),
					ObjectNew: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      "test-config-map",
						},
					}),
				},
				cacheChangedReturn: cacheChangedTrue,
			},
			want: want{
				objs: []interface{}{},
			},
		},
		{
			name: "no-update-configmap-equal",
			args: args{
				evt: event.UpdateEvent{
					ObjectOld: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
					}),
					ObjectNew: client.Object(&corev1.ConfigMap{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: ConfigNameSpace,
							Name:      SLOCtrlConfigMap,
						},
					}),
				},
				cacheChangedReturn: cacheChangedTrue,
			},
			want: want{
				objs: []interface{}{},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := EnqueueRequestForConfigMap{EnqueueRequest: enqueueRequest}
			if tt.args.cacheChangedReturn != nil {
				p.SyncCacheIfChanged = tt.args.cacheChangedReturn
			}
			q := workqueue.NewRateLimitingQueue(workqueue.DefaultControllerRateLimiter())
			p.Update(tt.args.evt, q)

			assert.Equal(t, q.Len(), len(tt.want.objs), "update() test fail, len expect equal!")

			for i := 0; i < len(tt.want.objs); i++ {
				obj, _ := q.Get()
				assert.Equal(t, tt.want.objs[i], obj, "update() test fail, obj expect equal")
			}
		})
	}
}
