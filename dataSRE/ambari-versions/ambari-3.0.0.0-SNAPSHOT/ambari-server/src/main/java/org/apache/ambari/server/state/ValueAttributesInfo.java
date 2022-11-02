/*
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

package org.apache.ambari.server.state;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;

import org.apache.ambari.server.controller.ApiModel;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.type.TypeReference;

import io.swagger.annotations.ApiModelProperty;

@XmlAccessorType(XmlAccessType.FIELD)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ValueAttributesInfo implements ApiModel {
  public static final String EMPTY_VALUE_VALID = "empty_value_valid";
  public static final String UI_ONLY_PROPERTY = "ui_only_property";
  public static final String READ_ONLY = "read_only";
  public static final String EDITABLE_ONLY_AT_INSTALL = "editable_only_at_install";
  public static final String SHOW_PROPERTY_NAME = "show_property_name";
  public static final String INCREMENT_STEP = "increment_step";
  public static final String SELECTION_CARDINALITY = "selection_cardinality";
  public static final String PROPERTY_FILE_NAME = "property-file-name";
  public static final String PROPERTY_FILE_TYPE = "property-file-type";
  public static final String ENTRIES = "entries";
  public static final String HIDDEN = "hidden";
  public static final String ENTRIES_EDITABLE = "entries_editable";
  public static final String USER_GROUPS = "user-groups";
  public static final String KEYSTORE = "keystore";
  private String type;
  private String maximum;
  private String minimum;
  private String unit;
  private String delete;
  private Boolean visible;
  private Boolean overridable;
  private String copy;

  @XmlElement(name = "empty-value-valid")
  @JsonProperty(EMPTY_VALUE_VALID)
  private Boolean emptyValueValid;

  @XmlElement(name = "ui-only-property")
  @JsonProperty(UI_ONLY_PROPERTY)
  private Boolean uiOnlyProperty;

  @XmlElement(name = "read-only")
  @JsonProperty(READ_ONLY)
  private Boolean readOnly;

  @XmlElement(name = "editable-only-at-install")
  @JsonProperty(EDITABLE_ONLY_AT_INSTALL)
  private Boolean editableOnlyAtInstall;

  @XmlElement(name = "show-property-name")
  @JsonProperty(SHOW_PROPERTY_NAME)
  private Boolean showPropertyName;

  @XmlElement(name = "increment-step")
  @JsonProperty(INCREMENT_STEP)
  private String incrementStep;

  @XmlElementWrapper(name = ENTRIES)
  @XmlElements(@XmlElement(name = "entry"))
  private Collection<ValueEntryInfo> entries;

  @XmlElement(name = HIDDEN)
  private String hidden;

  @XmlElement(name = ENTRIES_EDITABLE)
  private Boolean entriesEditable;

  @XmlElement(name = "selection-cardinality")
  @JsonProperty(SELECTION_CARDINALITY)
  private String selectionCardinality;

  @XmlElement(name = PROPERTY_FILE_NAME)
  @JsonProperty(PROPERTY_FILE_NAME)
  private String propertyFileName;

  @XmlElement(name = PROPERTY_FILE_TYPE)
  @JsonProperty(PROPERTY_FILE_TYPE)
  private String propertyFileType;

  @XmlElementWrapper(name = USER_GROUPS)
  @XmlElements(@XmlElement(name = "property"))
  private Collection<UserGroupInfo> userGroupEntries;

  @XmlElement(name = KEYSTORE)
  private boolean keyStore;

  public ValueAttributesInfo() {

  }

  @ApiModelProperty(name = "type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @ApiModelProperty(name = "maximum")
  public String getMaximum() {
    return maximum;
  }

  public void setMaximum(String maximum) {
    this.maximum = maximum;
  }

  @ApiModelProperty(name = "minimum")
  public String getMinimum() {
    return minimum;
  }

  public void setMinimum(String minimum) {
    this.minimum = minimum;
  }

  @ApiModelProperty(name = "unit")
  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  @ApiModelProperty(name = ENTRIES)
  public Collection<ValueEntryInfo> getEntries() {
    return entries;
  }

  public void setEntries(Collection<ValueEntryInfo> entries) {
    this.entries = entries;
  }

  @ApiModelProperty(name = "user-group-entries")
  public Collection<UserGroupInfo> getUserGroupEntries() {
    return userGroupEntries;
  }

  public void setUserGroupEntries(Collection<UserGroupInfo> userGroupEntries) {
    this.userGroupEntries = userGroupEntries;
  }

  @ApiModelProperty(name = HIDDEN)
  public String getHidden() {
    return hidden;
  }

  public void setHidden(String hidden) {
    this.hidden = hidden;
  }

  @ApiModelProperty(name = ENTRIES_EDITABLE)
  public Boolean getEntriesEditable() {
    return entriesEditable;
  }

  public void setEntriesEditable(Boolean entriesEditable) {
    this.entriesEditable = entriesEditable;
  }

  @ApiModelProperty(name = SELECTION_CARDINALITY)
  public String getSelectionCardinality() {
    return selectionCardinality;
  }

  public void setSelectionCardinality(String selectionCardinality) {
    this.selectionCardinality = selectionCardinality;
  }

  @ApiModelProperty(name = PROPERTY_FILE_NAME)
  public String getPropertyFileName() {
    return propertyFileName;
  }

  public void setPropertyFileName(String propertyFileName) {
    this.propertyFileName = propertyFileName;
  }

  @ApiModelProperty(name = PROPERTY_FILE_TYPE)
  public String getPropertyFileType() {
    return propertyFileType;
  }

  public void setPropertyFileType(String propertyFileType) {
    this.propertyFileType = propertyFileType;
  }

  @ApiModelProperty(name = INCREMENT_STEP)
  public String getIncrementStep() {
    return incrementStep;
  }

  public void setIncrementStep(String incrementStep) {
    this.incrementStep = incrementStep;
  }

  @ApiModelProperty(name = "delete")
  public String getDelete() {
    return delete;
  }

  public void setDelete(String delete) {
    this.delete = delete;
  }

  @ApiModelProperty(name = EMPTY_VALUE_VALID)
  public Boolean getEmptyValueValid() {
    return emptyValueValid;
  }

  public void setEmptyValueValid(Boolean isEmptyValueValid) {
    this.emptyValueValid = isEmptyValueValid;
  }

  @ApiModelProperty(name = "visible")
  public Boolean getVisible() {
    return visible;
  }

  public void setVisible(Boolean isVisible) {
    this.visible = isVisible;
  }

  @ApiModelProperty(name = READ_ONLY)
  public Boolean getReadOnly() {
    return readOnly;
  }

  public void setReadOnly(Boolean isReadOnly) {
    this.readOnly = isReadOnly;
  }

  @ApiModelProperty(name = EDITABLE_ONLY_AT_INSTALL)
  public Boolean getEditableOnlyAtInstall() {
    return editableOnlyAtInstall;
  }

  public void setEditableOnlyAtInstall(Boolean isEditableOnlyAtInstall) {
    this.editableOnlyAtInstall = isEditableOnlyAtInstall;
  }

  @ApiModelProperty(name = "overridable")
  public Boolean getOverridable() {
    return overridable;
  }

  public void setOverridable(Boolean isOverridable) {
    this.overridable = isOverridable;
  }

  @ApiModelProperty(name = SHOW_PROPERTY_NAME)
  public Boolean getShowPropertyName() {
    return showPropertyName;
  }

  public void setShowPropertyName(Boolean isPropertyNameVisible) {
    this.showPropertyName = isPropertyNameVisible;
  }

  @ApiModelProperty(name = UI_ONLY_PROPERTY)
  public Boolean getUiOnlyProperty() {
    return uiOnlyProperty;
  }

  public void setUiOnlyProperty(Boolean isUiOnlyProperty) {
    this.uiOnlyProperty = isUiOnlyProperty;
  }

  @ApiModelProperty(name = "copy")
  public String getCopy() {
    return copy;
  }

  public void setCopy(String copy) {
    this.copy = copy;
  }

  /**
   * Get the keystore element, indicating whether this
   * password property is to be encrypted in a keystore
   * when credential store use is enabled
   *
   * @return "true", "false"
   */
  @ApiModelProperty(name = KEYSTORE)
  public boolean isKeyStore() {
    return keyStore;
  }

  /**
   * Set the keystore element.
   *
   * @param keyStore - "true", "false"
   */
  public void setKeyStore(boolean keyStore) {
    this.keyStore = keyStore;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ValueAttributesInfo that = (ValueAttributesInfo) o;

    if (entries != null ? !entries.equals(that.entries) : that.entries != null) return false;
    if (entriesEditable != null ? !entriesEditable.equals(that.entriesEditable) : that.entriesEditable != null)
      return false;
    if (emptyValueValid != null ? !emptyValueValid.equals(that.emptyValueValid) : that.emptyValueValid != null)
      return false;
    if (visible != null ? !visible.equals(that.visible) : that.visible != null)
      return false;
    if (readOnly != null ? !readOnly.equals(that.readOnly) : that.readOnly != null)
      return false;
    if (editableOnlyAtInstall != null ? !editableOnlyAtInstall.equals(that.editableOnlyAtInstall) : that.editableOnlyAtInstall != null)
      return false;
    if (overridable != null ? !overridable.equals(that.overridable) : that.overridable != null)
      return false;
    if (hidden != null ? !hidden.equals(that.hidden) : that.hidden != null)
      return false;
    if (showPropertyName != null ? !showPropertyName.equals(that.showPropertyName) : that.showPropertyName != null)
      return false;
    if (uiOnlyProperty != null ? !uiOnlyProperty.equals(that.uiOnlyProperty) : that.uiOnlyProperty != null)
      return false;
    if (copy != null ? !copy.equals(that.copy) : that.copy != null)
      return false;
    if (maximum != null ? !maximum.equals(that.maximum) : that.maximum != null) return false;
    if (minimum != null ? !minimum.equals(that.minimum) : that.minimum != null) return false;
    if (selectionCardinality != null ? !selectionCardinality.equals(that.selectionCardinality) : that.selectionCardinality != null)
      return false;
    if (propertyFileName != null ? !propertyFileName.equals(that.propertyFileName) : that.propertyFileName != null)
      return false;
    if (propertyFileType != null ? !propertyFileType.equals(that.propertyFileType) : that.propertyFileType != null)
      return false;
    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (unit != null ? !unit.equals(that.unit) : that.unit != null) return false;
    if (delete != null ? !delete.equals(that.delete) : that.delete != null) return false;
    if (incrementStep != null ? !incrementStep.equals(that.incrementStep) : that.incrementStep != null) return false;
    if (userGroupEntries != null ? !userGroupEntries.equals(that.userGroupEntries) : that.userGroupEntries != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (hidden != null ? hidden.hashCode() : 0);
    result = 31 * result + (maximum != null ? maximum.hashCode() : 0);
    result = 31 * result + (minimum != null ? minimum.hashCode() : 0);
    result = 31 * result + (unit != null ? unit.hashCode() : 0);
    result = 31 * result + (delete != null ? delete.hashCode() : 0);
    result = 31 * result + (entries != null ? entries.hashCode() : 0);
    result = 31 * result + (entriesEditable != null ? entriesEditable.hashCode() : 0);
    result = 31 * result + (selectionCardinality != null ? selectionCardinality.hashCode() : 0);
    result = 31 * result + (propertyFileName != null ? propertyFileName.hashCode() : 0);
    result = 31 * result + (propertyFileType != null ? propertyFileType.hashCode() : 0);
    result = 31 * result + (incrementStep != null ? incrementStep.hashCode() : 0);
    result = 31 * result + (emptyValueValid != null ? emptyValueValid.hashCode() : 0);
    result = 31 * result + (visible != null ? visible.hashCode() : 0);
    result = 31 * result + (readOnly != null ? readOnly.hashCode() : 0);
    result = 31 * result + (editableOnlyAtInstall != null ? editableOnlyAtInstall.hashCode() : 0);
    result = 31 * result + (overridable != null ? overridable.hashCode() : 0);
    result = 31 * result + (showPropertyName != null ? showPropertyName.hashCode() : 0);
    result = 31 * result + (uiOnlyProperty != null ? uiOnlyProperty.hashCode() : 0);
    result = 31 * result + (copy != null ? copy.hashCode() : 0);
    result = 31 * result + (userGroupEntries != null ? userGroupEntries.hashCode() : 0);
    return result;
  }

  public Map<String, String> toMap(Optional<ObjectMapper> mapper) {
    Map<String, String> map =
      mapper.orElseGet(ObjectMapper::new).convertValue(this, new TypeReference<Map<String, String>>(){});
    if ( !Boolean.parseBoolean(map.get("keyStore")) ) { // keyStore is declared as a primitive value instead of Boolean -> treat false as unset
      map.remove("keyStore");
    }
    return map;
  }

  public static ValueAttributesInfo fromMap(Map<String, String> attributes, Optional<ObjectMapper> mapper) {
    return mapper.orElseGet(ObjectMapper::new).convertValue(attributes, ValueAttributesInfo.class);
  }

  @Override
  public String toString() {
    return "ValueAttributesInfo{" +
      "entries=" + entries +
      ", type='" + type + '\'' +
      ", maximum='" + maximum + '\'' +
      ", minimum='" + minimum + '\'' +
      ", unit='" + unit + '\'' +
      ", delete='" + delete + '\'' +
      ", emptyValueValid='" + emptyValueValid + '\'' +
      ", visible='" + visible + '\'' +
      ", readOnly='" + readOnly + '\'' +
      ", editableOnlyAtInstall='" + editableOnlyAtInstall + '\'' +
      ", overridable='" + overridable + '\'' +
      ", showPropertyName='" + showPropertyName + '\'' +
      ", uiOnlyProperty='" + uiOnlyProperty + '\'' +
      ", incrementStep='" + incrementStep + '\'' +
      ", entriesEditable=" + entriesEditable +
      ", selectionCardinality='" + selectionCardinality + '\'' +
      ", propertyFileName='" + propertyFileName + '\'' +
      ", propertyFileType='" + propertyFileType + '\'' +
      ", copy='" + copy + '\'' +
      ", userGroupEntries='" + userGroupEntries + '\'' +
      '}';
  }
}

