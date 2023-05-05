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

package release

import (
	"context"

	"github.com/erda-project/erda/pkg/common/apis"
)

// GetApplicationId .
func GetApplicationId(ctx context.Context) string {
	return apis.GetHeader(ctx, "applicationId")
}

// GetProjectID .
func GetProjectID(ctx context.Context) string {
	return apis.GetHeader(ctx, "projectID")
}

// GetVersion .
func GetVersion(ctx context.Context) string {
	return apis.GetHeader(ctx, "version")
}
