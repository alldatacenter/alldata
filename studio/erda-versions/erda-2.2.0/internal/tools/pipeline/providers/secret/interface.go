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

package secret

import (
	"context"

	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type Interface interface {
	FetchSecrets(ctx context.Context, p *spec.Pipeline) (secrets, cmsDiceFiles map[string]string, holdOnKeys, encryptSecretKeys []string, err error)
	FetchPlatformSecrets(ctx context.Context, p *spec.Pipeline, ignoreKeys []string) (map[string]string, error)
}
