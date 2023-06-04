/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { getSemVer } from './utils';

/**
 * Semantic Version Model
 * https://semver.org/lang/zh-CN/
 * @export
 * @class SemVer
 */
export class SemVer {
  private _versionStr;
  private _semverObj;

  public major?: string;
  public minor?: string;
  public patch?: string;
  public preRelease?: string;
  public buildMetaData?: string;

  constructor(version) {
    this._versionStr = version;
    this._semverObj = getSemVer(this._versionStr);
    this.major = this._semverObj?.[1];
    this.minor = this._semverObj?.[2];
    this.patch = this._semverObj?.[3];
    this.preRelease = this._semverObj?.[4];
    this.buildMetaData = this._semverObj?.[5];
  }
}

export default SemVer;
