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

App.WizardMiscPropertyChecker = Em.Mixin.create({
  showConfirmationDialogIfShouldChangeProps : function (changedProperty, stepConfigs, serviceId) {
    var serviceConfigModificationHandler = null;
    this.affectedProperties = [];
    try{
      serviceConfigModificationHandler = require('utils/configs/modification_handlers/'+serviceId.toLowerCase());
    }catch (e) {
      console.log("Unable to load modification handler for ", serviceId);
    }
    if (serviceConfigModificationHandler != null) {
      var securityEnabled = App.router.get('mainAdminKerberosController.securityEnabled');
      this.affectedProperties = serviceConfigModificationHandler.getDependentConfigChanges(changedProperty, this.get("controller.selectedServiceNames"), stepConfigs, securityEnabled);
    }
    changedProperty.set("editDone", false); // Turn off flag

    if (this.affectedProperties.length > 0 && !this.get("controller.miscModalVisible")) {
      this.newAffectedProperties = this.affectedProperties;
      var self = this;
      return App.ModalPopup.show({
        classNames: ['modal-690px-width'],
        modalDialogClasses: ['modal-lg'],
        showCloseButton: false,
        primary: Em.I18n.t('common.apply'),
        secondary: serviceId == 'MISC' ? Em.I18n.t('common.ignore') : null,
        third: Em.I18n.t('common.cancel'),
        secondaryClass: 'btn-warning',
        header: "Warning: you must also change these Service properties",
        onPrimary: function () {
          self.get("newAffectedProperties").forEach(function(item) {
            if (item.isNewProperty) {
              self.createProperty({
                name: item.propertyName,
                displayName: item.propertyDisplayName,
                value: item.newValue,
                categoryName: item.categoryName,
                serviceName: item.serviceName,
                filename: item.filename
              });
            } else {
              self.get("controller.stepConfigs").findProperty("serviceName", item.serviceName).get("configs").find(function(config) {
                return item.propertyName == config.get('name') && (item.filename == null || item.filename == config.get('filename'));
              }).set("value", item.newValue);
            }
          });
          self.get("controller").set("miscModalVisible", false);
          this.hide();
        },
        onSecondary: function () {
          self.get("controller").set("miscModalVisible", false);
          this.hide();
        },
        onThird: function () {
          var affected = self.get("newAffectedProperties").objectAt(0),
            changedProperty = self.get("controller.stepConfigs").findProperty("serviceName", affected.sourceServiceName)
              .get("configs").findProperty("name", affected.changedPropertyName);
          changedProperty.set('value', changedProperty.get('savedValue') || changedProperty.get('initialValue'));
          self.get("controller").set("miscModalVisible", false);
          this.hide();
        },
        bodyClass: Em.View.extend({
          templateName: require('templates/common/configs/propertyDependence'),
          controller: this,
          propertyChange: self.get("newAffectedProperties"),
          didInsertElement: function () {
            self.get("controller").set("miscModalVisible", true);
          }
        })
      });
    }
  }
});