// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package resourcegc

import (
	"context"
	"fmt"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/bmizerany/assert"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/jsonstore"
)

func TestReconciler_getNeedGCPipeline(t *testing.T) {
	type args struct {
		pipelines []spec.Pipeline
		err       error
	}

	var gcTime = func() uint64 {
		return 1800
	}

	now := time.Now()
	tests := []struct {
		name    string
		args    args
		wantLen int
		wantErr bool
	}{
		{
			name: "test_empty",
			args: args{
				pipelines: nil,
				err:       nil,
			},
			wantLen: 0,
			wantErr: false,
		},
		{
			name: "test_return_error",
			args: args{
				pipelines: nil,
				err:       fmt.Errorf("have error"),
			},
			wantLen: 0,
			wantErr: true,
		},
		{
			name: "completeReconcilerGC_pipeline",
			args: args{
				pipelines: []spec.Pipeline{
					{
						PipelineBase: spec.PipelineBase{
							Status: apistructs.PipelineStatusSuccess,
							TimeEnd: func() *time.Time {
								now := now.Add(-190*time.Second - bufferTime*time.Second)
								return &now
							}(),
						},
						PipelineExtra: spec.PipelineExtra{
							Extra: spec.PipelineExtraInfo{
								CompleteReconcilerGC:       true,
								CompleteReconcilerTeardown: false,
								GC: apistructs.PipelineGC{
									ResourceGC: apistructs.PipelineResourceGC{
										FailedTTLSecond:  &[]uint64{200}[0],
										SuccessTTLSecond: &[]uint64{200}[0],
									},
								},
							},
						},
					},
				},
			},
			wantErr: false,
			wantLen: 0,
		},
		{
			name: "completeReconcilerTeardown_pipeline",
			args: args{
				pipelines: []spec.Pipeline{
					{
						PipelineBase: spec.PipelineBase{
							Status: apistructs.PipelineStatusSuccess,
							TimeEnd: func() *time.Time {
								now := time.Now().Add(-190*time.Second - bufferTime*time.Second)
								return &now
							}(),
						},
						PipelineExtra: spec.PipelineExtra{
							Extra: spec.PipelineExtraInfo{
								CompleteReconcilerGC:       false,
								CompleteReconcilerTeardown: true,
								GC: apistructs.PipelineGC{
									ResourceGC: apistructs.PipelineResourceGC{
										FailedTTLSecond:  &[]uint64{200}[0],
										SuccessTTLSecond: &[]uint64{200}[0],
									},
								},
							},
						},
					},
				},
			},
			wantErr: false,
			wantLen: 0,
		},
		{
			name: "completeReconcilerTeardownNotFoundInETCD_pipeline",
			args: args{
				pipelines: []spec.Pipeline{
					{
						PipelineBase: spec.PipelineBase{
							Status: apistructs.PipelineStatusSuccess,
							TimeEnd: func() *time.Time {
								now := time.Now().Add(-300*time.Second - bufferTime*time.Second)
								return &now
							}(),
						},
						PipelineExtra: spec.PipelineExtra{
							Extra: spec.PipelineExtraInfo{
								CompleteReconcilerGC:       false,
								CompleteReconcilerTeardown: true,
								GC: apistructs.PipelineGC{
									ResourceGC: apistructs.PipelineResourceGC{
										FailedTTLSecond:  &[]uint64{200}[0],
										SuccessTTLSecond: &[]uint64{200}[0],
									},
								},
								Namespace: "pipeline-1",
							},
						},
					},
				},
			},
			wantErr: false,
			wantLen: 1,
		},
		{
			name: "time_over_gc",
			args: args{
				pipelines: []spec.Pipeline{
					{
						PipelineBase: spec.PipelineBase{
							Status: apistructs.PipelineStatusSuccess,
							TimeEnd: func() *time.Time {
								addTime := now.Add(-300*time.Second - bufferTime*time.Second)
								return &addTime
							}(),
						},
						PipelineExtra: spec.PipelineExtra{
							Extra: spec.PipelineExtraInfo{
								CompleteReconcilerGC:       false,
								CompleteReconcilerTeardown: false,
								GC: apistructs.PipelineGC{
									ResourceGC: apistructs.PipelineResourceGC{
										FailedTTLSecond:  &[]uint64{200}[0],
										SuccessTTLSecond: &[]uint64{200}[0],
									},
								},
							},
						},
					},
				},
			},
			wantErr: false,
			wantLen: 1,
		},
		{
			name: "time_not_over_gc",
			args: args{
				pipelines: []spec.Pipeline{
					{
						PipelineBase: spec.PipelineBase{
							Status: apistructs.PipelineStatusFailed,
							TimeEnd: func() *time.Time {
								now := now.Add(-190*time.Second - bufferTime*time.Second)
								return &now
							}(),
						},
						PipelineExtra: spec.PipelineExtra{
							Extra: spec.PipelineExtraInfo{
								CompleteReconcilerGC:       false,
								CompleteReconcilerTeardown: false,
								GC: apistructs.PipelineGC{
									ResourceGC: apistructs.PipelineResourceGC{
										FailedTTLSecond:  &[]uint64{200}[0],
										SuccessTTLSecond: &[]uint64{200}[0],
									},
								},
							},
						},
					},
				},
			},
			wantErr: false,
			wantLen: 0,
		},
		{
			name: "over_default_gc_time",
			args: args{
				pipelines: []spec.Pipeline{
					{
						PipelineBase: spec.PipelineBase{
							Status: apistructs.PipelineStatusStopByUser,
							TimeEnd: func() *time.Time {
								now := now.Add(-time.Duration(gcTime())*time.Second - bufferTime*time.Second - 20*time.Second)
								return &now
							}(),
						},
						PipelineExtra: spec.PipelineExtra{
							Extra: spec.PipelineExtraInfo{
								CompleteReconcilerGC:       false,
								CompleteReconcilerTeardown: false,
								GC: apistructs.PipelineGC{
									ResourceGC: apistructs.PipelineResourceGC{
										FailedTTLSecond:  &[]uint64{gcTime()}[0],
										SuccessTTLSecond: &[]uint64{gcTime()}[0],
									},
								},
							},
						},
					},
				},
			},
			wantErr: false,
			wantLen: 1,
		},
		{
			name: "not_over_default_gc_time",
			args: args{
				pipelines: []spec.Pipeline{
					{
						PipelineBase: spec.PipelineBase{
							Status: apistructs.PipelineStatusStopByUser,
							TimeEnd: func() *time.Time {
								now := now.Add(-time.Duration(gcTime())*time.Second - bufferTime*time.Second + 20*time.Second)
								return &now
							}(),
						},
						PipelineExtra: spec.PipelineExtra{
							Extra: spec.PipelineExtraInfo{
								CompleteReconcilerGC:       false,
								CompleteReconcilerTeardown: false,
								GC: apistructs.PipelineGC{
									ResourceGC: apistructs.PipelineResourceGC{
										FailedTTLSecond:  &[]uint64{gcTime()}[0],
										SuccessTTLSecond: &[]uint64{gcTime()}[0],
									},
								},
							},
						},
					},
				},
			},
			wantErr: false,
			wantLen: 0,
		},
		{
			name: "not_end_status",
			args: args{
				pipelines: []spec.Pipeline{
					{
						PipelineBase: spec.PipelineBase{
							Status: apistructs.PipelineStatusAnalyzed,
							TimeEnd: func() *time.Time {
								now := now.Add(-time.Duration(gcTime())*time.Second - 20*time.Second - bufferTime*time.Second)
								return &now
							}(),
						},
						PipelineExtra: spec.PipelineExtra{
							Extra: spec.PipelineExtraInfo{
								CompleteReconcilerGC:       false,
								CompleteReconcilerTeardown: false,
							},
						},
					},
				},
			},
			wantErr: false,
			wantLen: 0,
		},
	}

	logrus.Infof("start test pipeline_gc_compensator_test")
	defer logrus.Infof("end test pipeline_gc_compensator_test")

	for _, tt := range tests {

		var db *dbclient.Client
		monkey.PatchInstanceMethod(reflect.TypeOf(db), "PageListPipelines", func(client *dbclient.Client, req apistructs.PipelinePageListRequest, ops ...dbclient.SessionOption) (*dbclient.PageListPipelinesResult, error) {
			return &dbclient.PageListPipelinesResult{
				Pipelines:         tt.args.pipelines,
				PagingPipelineIDs: nil,
				Total:             1,
				CurrentPageSize:   0,
			}, tt.args.err
		})

		js := &jsonstore.JsonStoreImpl{}
		monkey.PatchInstanceMethod(reflect.TypeOf(js), "Notfound", func(j *jsonstore.JsonStoreImpl, ctx context.Context, key string) (bool, error) {
			if key == "/devops/pipeline/gc/reconciler/pipeline-1/" {
				return false, nil
			}
			return true, nil
		})

		t.Run(tt.name, func(t *testing.T) {
			p := &provider{dbClient: db, js: js}
			got, _, err := p.getNeedGCPipelines(0, true)
			if (err != nil) != tt.wantErr {
				t.Errorf("getNeedGCPipeline() error = %v, wantErr %v", err, tt.wantErr)
				return
			}

			assert.Equal(t, tt.wantLen, len(got))
		})

	}
}

func TestReconciler_doWaitGCCompensate(t *testing.T) {
	var db *dbclient.Client

	pm := monkey.PatchInstanceMethod(reflect.TypeOf(db), "PageListPipelines", func(client *dbclient.Client, req apistructs.PipelinePageListRequest, ops ...dbclient.SessionOption) (*dbclient.PageListPipelinesResult, error) {
		return &dbclient.PageListPipelinesResult{
			Pipelines:         []spec.Pipeline{},
			PagingPipelineIDs: []uint64{},
			Total:             0,
			CurrentPageSize:   0,
		}, nil
	})
	defer pm.Unpatch()

	t.Run("TestReconciler_doWaitGCCompensate", func(t *testing.T) {
		p := &provider{dbClient: db}
		p.doWaitGCCompensate(true)
	})
}
