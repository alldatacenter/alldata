/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/

import Ember from 'ember';
import { validator, buildValidations } from 'ember-cp-validations';

const Validations = buildValidations({
  'actionModel': {
    validators: [
      validator('decission-node-validator', {
        dependentKeys: ['actionModel.@each.condition']
      })
    ]
  }
});

export default Ember.Component.extend(Validations,{
  initialize : function(){
    this.sendAction('register','decision',this);
    this.set('targetNodes', Ember.A([]));
    this.get('targetNodes').pushObjects(this.get('currentNode.validOkToNodes'));
    this.get('targetNodes').pushObjects(this.get('killNodes'));
  }.on('init'),
  actions : {
    onTargetNodeChange(index){
      var node = this.get('targetNodes').findBy('id', this.$(`#target-node-select-${index}`).find(":selected").val());
      var config = this.get('actionModel').objectAt(index);
      Ember.set(config, 'node', node);
    }
  }
});
