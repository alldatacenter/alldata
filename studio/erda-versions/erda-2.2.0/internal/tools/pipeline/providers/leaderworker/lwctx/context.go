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

package lwctx

import (
	"context"
)

const (
	ctxKeyTaskCancelChan       = "__lw__logic-task-cancel-chan"
	ctxKeyTaskCancelChanClosed = "__lw__logic-task-cancel-chan-closed"
)

func MakeCtxWithTaskCancelChan(ctx context.Context) context.Context {
	taskCancelCh := make(chan struct{})
	ctx = context.WithValue(ctx, ctxKeyTaskCancelChan, taskCancelCh)
	pointerClosed := &[]bool{false}[0]
	ctx = context.WithValue(ctx, ctxKeyTaskCancelChanClosed, pointerClosed)
	return ctx
}

func MustGetTaskCancelChanFromCtx(ctx context.Context) chan struct{} {
	taskCancelCh, ok := ctx.Value(ctxKeyTaskCancelChan).(chan struct{})
	if !ok {
		panic("context doesn't contains task-cancel-chan")
	}
	return taskCancelCh
}

func IdempotentCloseTaskCancelChan(ctx context.Context) {
	cancelChan := MustGetTaskCancelChanFromCtx(ctx)
	if pointerClosed := ctx.Value(ctxKeyTaskCancelChanClosed).(*bool); !*pointerClosed {
		close(cancelChan)
		*pointerClosed = true
	}
}
