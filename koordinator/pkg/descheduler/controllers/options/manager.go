/*
Copyright 2022 The Koordinator Authors.
Copyright 2022 The Kubernetes Authors.

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

package options

import (
	ctrl "sigs.k8s.io/controller-runtime"
)

// Manager is set when initializing descheduler.
// Some descheduling scenarios need to be implemented as Controller
// via controller-runtime to simplify the implementation.
var Manager ctrl.Manager
