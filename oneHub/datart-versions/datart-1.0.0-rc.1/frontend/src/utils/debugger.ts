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

export class Debugger {
  private static _instance: Debugger;
  private _enableDebug = false;

  public static get instance() {
    if (!Debugger._instance) {
      Debugger._instance = new Debugger();
    }
    return Debugger._instance;
  }

  public setEnable(enable?: boolean) {
    this._enableDebug = !!enable;
  }

  public measure(info: string, fn: Function, forceEnable: boolean = true) {
    if (!this._enableDebug || !forceEnable) {
      return fn();
    }

    const start = performance.now();
    const result = fn();
    const end = performance.now();
    console.info(`Performance - ${info} - `, `${end - start} ms`);
    return result;
  }

  /**
   * Delay Function
   * @Example
   *  await Debugger.instance.delay(5000);
   * @param {number} milliseconds
   * @return {*}
   * @memberof Debugger
   */
  public async delay(ms: number = 500) {
    return await new Promise(resolve =>
      setTimeout(() => {
        resolve({});
      }, ms),
    );
  }
}
