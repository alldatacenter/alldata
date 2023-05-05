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

package assetsvc_test

import (
	"testing"

	"github.com/erda-project/erda/internal/apps/dop/services/assetsvc"
)

const spec = `
openapi: 3.0.1
info:
    title: database-schemas
    version: "1.0"
paths: {}
components:
    schemas:
        base_model:
            type: object
            properties:
                createdAt:
                    type: string
                    example: created_at_example
                    description: ""
                    x-dice-raw: created_at
                    x-dice-source: base_model
                id:
                    type: integer
                    example: 0
                    description: ""
                    x-dice-raw: id
                    x-dice-source: base_model
                updatedAt:
                    type: string
                    example: updated_at_example
                    description: ""
                    x-dice-raw: updated_at
                    x-dice-source: base_model
        dice_api_assets:
            type: object
            properties:
                appID:
                    type: integer
                    example: 0
                    description: ""
                    x-dice-raw: app_id
                    x-dice-source: dice_api_assets
                assetID:
                    type: string
                    example: asset_id_example
                    description: this is asset id
                    x-dice-raw: asset_id
                    x-dice-source: dice_api_assets
                assetName:
                    type: string
                    example: asset_name_2_example
                    description: asset name
                    x-dice-raw: asset_name_2
                    x-dice-source: dice_api_assets
                assetName2:
                    type: string
                    example: asset_name_2_example
                    description: asset name
                    x-dice-raw: asset_name_2
                    x-dice-source: dice_api_assets
                creatorID:
                    type: integer
                    example: 0
                    description: ""
                    x-dice-raw: creator_id
                    x-dice-source: dice_api_assets
                logo:
                    type: string
                    example: logo_example
                    description: ""
                    x-dice-raw: logo
                    x-dice-source: dice_api_assets
                orgID:
                    type: integer
                    example: 0
                    description: ""
                    x-dice-raw: org_id
                    x-dice-source: dice_api_assets
                projectID:
                    type: integer
                    example: 0
                    description: ""
                    x-dice-raw: project_id
                    x-dice-source: dice_api_assets
                public:
                    type: boolean
                    example: true
                    description: public
                    x-dice-raw: public
                    x-dice-source: dice_api_assets
                updaterID:
                    type: string
                    example: updater_id_example
                    description: ""
                    x-dice-raw: updater_id
                    x-dice-source: dice_api_assets

`

func TestYaml2Json(t *testing.T) {
	data := assetsvc.Yaml2Json([]byte(spec))
	assetsvc.Json2Yaml(data)

	assetsvc.Oas2Json([]byte(spec))
	assetsvc.Oas2Yaml([]byte(spec))
	assetsvc.Oas3Json([]byte(spec))
	assetsvc.Oas3Yaml([]byte(spec))
}
