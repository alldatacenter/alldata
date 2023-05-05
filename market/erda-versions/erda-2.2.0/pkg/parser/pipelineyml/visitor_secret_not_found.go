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

package pipelineyml

type SecretNotFoundSecret struct {
	data    []byte
	secrets map[string]string
}

func NewSecretNotFoundSecret(data []byte, secrets map[string]string) *SecretNotFoundSecret {
	return &SecretNotFoundSecret{
		data:    data,
		secrets: secrets,
	}
}

func (v *SecretNotFoundSecret) Visit(s *Spec) {
	if _, err := RenderSecrets(v.data, v.secrets); err != nil {
		s.appendError(err)
		return
	}
}
