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

package dbclient

const DistinctAssetIDFromAccess = `
SELECT SQL_CALC_FOUND_ROWS asset_id
FROM dice_api_access
where org_id = ?
	AND ('' = ? OR asset_id = ? OR asset_name LIKE ?)
	AND (asset_id IN (?))
ORDER BY updated_at DESC
LIMIT ? OFFSET ?
`

const SelectFoundRows = `
SELECT FOUND_ROWS()
`
