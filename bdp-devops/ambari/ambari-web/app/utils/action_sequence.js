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


/**
 * Utility to define sequence of actions (sync or async either) and execute them in exact order
 * methods executed in order that described in sequence
 * each action obtain result of previous action <code>previousResponse</code>
 * For example:
 * loadMap: {
 *  '0': {
 *   type: 'async',
 *   callback: function (previousResponse) {
 *     //method1
 *   }
 *  },
 *  '1': {
 *    type: 'sync',
 *    callback: function (previousResponse) {
 *      //method2
 *    }
 *  }
 * }
 * However method1 is async, method2 will be executed only after method1 is completed
 * @type {*}
 */
App.actionSequence = Em.Object.extend({
  /**
   * List of actions which will be executed
   * format:
   * [
   *   {
   *     type: 'async',
   *     callback: function(previousResponse) {
   *       //async method should return Deferred object to continue sequence
   *     }
   *   },
   *   {
   *     type: 'sync',
   *     callback: function(previousResponse) {
   *       //code
   *     }
   *   }
   * ]
   * @type {Object[]}
   */
  sequence: [],
  /**
   * specific context for methods executed in actions
   */
  context: this,
  /**
   * Function called after sequence is completed
   * @type {callback}
   */
  finishedCallback: Em.K,
  /**
   * counter of actions
   */
  actionCounter: 0,
  /**
   * set sequence
   * @param sequence
   * @return {object}
   */
  setSequence: function (sequence) {
    if (Array.isArray(sequence)) {
      this.set('sequence', sequence);
    }
    return this;
  },
  /**
   * start executing sequence of actions
   */
  start: function () {
    this.set('actionCounter', this.get('sequence.length'));
    this.runNextAction(0, null);
  },
  /**
   * set <code>finishedCallback</code>
   * @param callback
   * @return {object}
   */
  onFinish: function (callback) {
    if (typeof(callback) === 'function') {
      this.set('finishedCallback', callback);
    }
    return this;
  },
  /**
   * run next action in sequence
   * @param index
   * @param previousResponse
   * @return {Boolean}
   */
  runNextAction: function (index, previousResponse) {
    var action = this.get('sequence')[index];
    var context = this.get('context');
    var self = this;

    index++;
    this.decrementProperty('actionCounter');

    if (this.get('actionCounter') >= 0 && !Em.isNone(action)) {
      if (action.type === 'sync') {
        this.runNextAction(index, action.callback.call(context, previousResponse));
        return true;
      } else if (action.type === 'async') {
        action.callback.call(context, previousResponse).done(function (response) {
          self.runNextAction(index, response);
        });
        return true;
      }
    } else {
      //if no more actions left then finish sequence
      this.finishedCallback();
    }
    return false;
  }
});

