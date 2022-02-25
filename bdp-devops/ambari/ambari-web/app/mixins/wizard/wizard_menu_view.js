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

var App = require('app');

function isStepDisabled(index) {
  return Em.computed('controller.isStepDisabled.@each.{step,value}', function () {
    return this.isStepDisabled(index);
  }).cacheable();
}

function isStepCompleted(index) {
  return Em.computed('controller.{currentStep,isStepDisabled.@each.value}', function () {
    return this.isStepCompleted(index);
  }).cacheable();
}

App.WizardMenuMixin = Em.Mixin.create({

  isStepDisabled: function (index) {
    return this.get('controller.isStepDisabled').findProperty('step', index).get('value');
  },

  isStepCompleted(index) {
    return this.get('controller.currentStep') > index;
  },

  isStep0Disabled: isStepDisabled(0),
  isStep1Disabled: isStepDisabled(1),
  isStep2Disabled: isStepDisabled(2),
  isStep3Disabled: isStepDisabled(3),
  isStep4Disabled: isStepDisabled(4),
  isStep5Disabled: isStepDisabled(5),
  isStep6Disabled: isStepDisabled(6),
  isStep7Disabled: isStepDisabled(7),
  isStep8Disabled: isStepDisabled(8),
  isStep9Disabled: isStepDisabled(9),
  isStep10Disabled: isStepDisabled(10),

  isStep0Completed: isStepCompleted(0),
  isStep1Completed: isStepCompleted(1),
  isStep2Completed: isStepCompleted(2),
  isStep3Completed: isStepCompleted(3),
  isStep4Completed: isStepCompleted(4),
  isStep5Completed: isStepCompleted(5),
  isStep6Completed: isStepCompleted(6),
  isStep7Completed: isStepCompleted(7),
  isStep8Completed: isStepCompleted(8),
  isStep9Completed: isStepCompleted(9),
  isStep10Completed: isStepCompleted(10)

});
