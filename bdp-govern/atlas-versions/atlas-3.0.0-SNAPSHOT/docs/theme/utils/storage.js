/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define(["require", "exports"], function(require, exports) {
  "use strict";
  Object.defineProperty(exports, "__esModule", { value: true });
  var Storage = /** @class */ (function() {
    function Storage(name) {
      this.name = name;
    }
    Storage.prototype.get = function() {
      if (typeof window !== "undefined") {
        try {
          var item = window.localStorage.getItem(this.name);
          return typeof item === "string" ? JSON.parse(item) : null;
        } catch (err) {
          return {};
        }
      }
    };
    Storage.prototype.set = function(value) {
      if (typeof window !== "undefined") {
        window.localStorage.setItem(this.name, JSON.stringify(value));
      }
    };
    Storage.prototype.delete = function() {
      if (typeof window !== "undefined") {
        window.localStorage.removeItem(this.name);
      }
    };
    return Storage;
  })();
  exports.Storage = Storage;
});

/*export class Storage {
  public name: string
  constructor(name: string) {
    this.name = name
  }

  public get(): any {
    if (typeof window !== 'undefined') {
      try {
        const item = window.localStorage.getItem(this.name)
        return typeof item === 'string' ? JSON.parse(item) : null
      } catch (err) {
        return {}
      }
    }
  }

  public set(value: any): void {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(this.name, JSON.stringify(value))
    }
  }

  public delete(): void {
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem(this.name)
    }
  }
}
*/
