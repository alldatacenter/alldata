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

var stringUtils = require('utils/string_utils');

var types = {
  'get': function(prop) {
    return Object.prototype.toString.call(prop);
  },
  'object': '[object Object]',
  'array': '[object Array]',
  'string': '[object String]',
  'boolean': '[object Boolean]',
  'number': '[object Number]'
};

module.exports = {

  isChild: function(obj)
  {
    for (var k in obj) {
      if (obj.hasOwnProperty(k)) {
        if (obj[k] instanceof Object) {
          return false;
        }
      }
    }
    return true;
  },

  recursiveKeysCount: function(obj) {
    if (!(obj instanceof Object)) {
      return null;
    }
    var self = this;
    function r(obj) {
      var count = 0;
      for (var k in obj) {
        if(self.isChild(obj[k])){
          count++;
        } else {
          count += r(obj[k]);
        }
      }
      return count;
    }
    return r(obj);
  },

  deepEqual: function() {
    var i, l, leftChain, rightChain;
    var values = arguments;
    function compare2Objects (x, y) {
      var p;
      if (isNaN(x) && isNaN(y) && typeof x === 'number' && typeof y === 'number') {
        return true;
      }

      if (x === y) {
        return true;
      }

      if ((typeof x === 'function' && typeof y === 'function') ||
        (x instanceof Date && y instanceof Date) ||
        (x instanceof RegExp && y instanceof RegExp) ||
        (x instanceof String && y instanceof String) ||
        (x instanceof Number && y instanceof Number)) {
         return x.toString() === y.toString();
      }

      if (!(x instanceof Object && y instanceof Object)) {
        return false;
      }

      if (x.isPrototypeOf(y) || y.isPrototypeOf(x)) {
        return false;
      }

      if (x.constructor !== y.constructor) {
        return false;
      }

      if (x.prototype !== y.prototype) {
        return false;
      }

      if (leftChain.indexOf(x) > -1 || rightChain.indexOf(y) > -1) {
        return false;
      }

      for (p in y) {
        if (y.hasOwnProperty(p) !== x.hasOwnProperty(p)) {
            return false;
        }
        else if (typeof y[p] !== typeof x[p]) {
            return false;
        }
      }

      for (p in x) {
        if (y.hasOwnProperty(p) !== x.hasOwnProperty(p)) {
          return false;
        }
        else if (typeof y[p] !== typeof x[p]) {
          return false;
        }
        switch (typeof (x[p])) {
          case 'object':
          case 'function':
            leftChain.push(x);
            rightChain.push(y);
            if (!compare2Objects (x[p], y[p])) {
                return false;
            }
            leftChain.pop();
            rightChain.pop();
            break;
          default:
            if (x[p] !== y[p]) {
                return false;
            }
            break;
        }
      }

      return true;
    }

    if (arguments.length < 1) {
      return true;
    }

    for (i = 1, l = arguments.length; i < l; i++) {
      leftChain = [];
      rightChain = [];
      if (!compare2Objects(arguments[0], arguments[i])) {
        return false;
      }
    }

    return true;
  },

  recursiveTree: function(obj) {
    if (!(obj instanceof Object)) {
      return null;
    }
    var self = this;
    function r(obj,parent) {
      var leaf = '';
      for (var k in obj) {
        if(self.isChild(obj[k])){
          leaf += k + ' ('+parent+')' + '<br/>';
        } else {
          leaf += r(obj[k],parent +'/' + k);
        }
      }
      return leaf;
    }
    return r(obj,'');
  },

  /**
   *
   * @param {object|array|object[]} target
   * @param {object|array|object[]} source
   * @param {function} handler
   * @returns {object|array|object[]}
   */
  deepMerge: function(target, source, handler) {
    if (typeof target !== 'object' || typeof source !== 'object') return target;
    var handlerOpts = Array.prototype.slice.call(arguments, 3);
    var isArray = Em.isArray(source);
    var ret = handler && typeof handler.apply(this, [target, source].concat(handlerOpts)) !== 'undefined' ?
          handler(target, source) :
          isArray ? [] : {};
    var self = this;

    // handle array
    if (isArray) {
      target = target || [];
      ret = ret.concat(target);

      if (types.object === types.get(target[0])) {
        ret = self.smartArrayObjectMerge(target, source);
      } else {
        for(var i = 0; i < source.length; i++) {
          if (typeof ret[i] === 'undefined') {
            ret[i] = source[i];
          } else if (typeof source[i] === 'object') {
            ret[i] = this.deepMerge(target[i], source[i], handler, target, source);
          } else {
            if (target.indexOf(source[i]) === -1) {
              ret.push(source[i]);
            }
          }
        }
      }
    } else {
      if (target && typeof target === 'object') {
        Em.keys(target).forEach(function(key) {
          ret[key] = target[key];
        });
      }
      Em.keys(source).forEach(function(key) {
        // handle value which is not Array or Object
        if (typeof source[key] !== 'object' || !source[key]) {
          ret[key] = source[key];
        } else {
          if (!target[key]) {
            ret[key] = source[key];
          } else {
            ret[key] = self.deepMerge(target[key], source[key], handler, target, source);
          }
        }
      });
    }

    return ret;
  },

  /**
   * Find objects by index key (@see detectIndexedKey) and merge them.
   *
   * @param {object[]} target
   * @param {object[]} source
   * @returns {object[]}
   */
  smartArrayObjectMerge: function(target, source) {
    // keep the first object and take all keys that contains primitive value
    var id = this.detectIndexedKey(target);
    var self = this;
    // when uniq key not found let's merge items by the key itself
    if (!id) {
      source.forEach(function(obj) {
        Em.keys(obj).forEach(function(objKey) {
          var ret = self.objectByRoot(objKey, target);
          if (!Em.isNone(ret)) {
            if ([types.object, types.array].contains(types.get(ret))) {
              target[objKey] = self.deepMerge(obj[objKey], ret);
            } else {
              target[objKey] = ret;
            }
          } else {
            var _obj = {};
            _obj[objKey] = obj[objKey];
            target.push(_obj);
          }
        });
      });
      return target;
    }

    return target.mapProperty(id).concat(source.mapProperty(id)).uniq().map(function(value) {
      if (!target.someProperty(id, value)) {
        return source.findProperty(id, value);
      } else if (!source.someProperty(id, value)) {
        return target.findProperty(id, value);
      }
      return self.deepMerge(target.findProperty(id, value), source.findProperty(id, value));
    });
  },

  /**
   * Determines key with uniq value. This key will be used to find correct objects in target and source to merge.
   *
   * @param {object} target
   * @returns {string|undefined}
   */
  detectIndexedKey: function(target) {
    var keys = Em.keys(target[0]).map(function(key) {
      if ([types.object, types.array].contains(types.get(target[0][key]))) {
        return null;
      }
      return key;
    }).compact();
    return keys.filter(function(key) {
      var values = target.mapProperty(key);
      return values.length === values.uniq().length;
    })[0];
  },

  /**
   *
   * @param {string} rootKey
   * @param {object[]} target
   */
  objectByRoot: function(rootKey, target) {
    return target.map(function(item) {
      return item[rootKey] || null;
    }).compact()[0];
  }
};
