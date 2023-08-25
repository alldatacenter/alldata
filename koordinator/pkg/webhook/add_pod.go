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

package webhook

import (
	"github.com/koordinator-sh/koordinator/pkg/features"
	utilfeature "github.com/koordinator-sh/koordinator/pkg/util/feature"
	"github.com/koordinator-sh/koordinator/pkg/webhook/pod/mutating"
	"github.com/koordinator-sh/koordinator/pkg/webhook/pod/validating"
)

func init() {
	addHandlersWithGate(mutating.HandlerMap, func() (enabled bool) {
		return utilfeature.DefaultFeatureGate.Enabled(features.PodMutatingWebhook)
	})

	addHandlersWithGate(validating.HandlerMap, func() (enabled bool) {
		return utilfeature.DefaultFeatureGate.Enabled(features.PodValidatingWebhook)
	})
}
