/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.state;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class Module {
  public enum Category {
    @SerializedName("SERVER")
    SERVER,
    @SerializedName("CLIENT")
    CLIENT,
    @SerializedName("LIBRARY")
    LIBRARY
  }

  @SerializedName("id")
  private String id;
  @SerializedName("displayName")
  private String displayName;
  @SerializedName("description")
  private String description;
  @SerializedName("category")
  private Category category;
  @SerializedName("name")
  private String name;
  @SerializedName("version")
  private String version;
  @SerializedName("definition")
  private String definition;
  @SerializedName("dependencies")
  private List<ModuleDependency> dependencies;
  @SerializedName("components")
  private List<ModuleComponent> components;

  private HashMap<String, ModuleComponent> componentHashMap;

  public Category getCategory() {
    return category;
  }

  public void setType(Category category) {
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public List<ModuleDependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<ModuleDependency> dependencies) {
    this.dependencies = dependencies;
  }

  public List<ModuleComponent> getComponents() {
    return components;
  }

  public void setComponents(List<ModuleComponent> components) {
    this.components = components;
  }

  /**
   * Fetch a particular module component by the component name.
   * @param moduleComponentName
   * @return
   */
  public ModuleComponent getModuleComponent(String moduleComponentName) {
    return componentHashMap.get(moduleComponentName);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Module module = (Module) o;

    return Objects.equals(id, module.id) && Objects.equals(displayName, module.displayName) &&
            Objects.equals(description, module.description) && Objects.equals(category, module.category) &&
            Objects.equals(name, module.name) && Objects.equals(version, module.version) && Objects.equals(definition, module.definition)
            && Objects.equals(dependencies, module.dependencies) && Objects.equals(components, module.components);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, displayName, description, category, name, version, definition, dependencies, components);
  }

  @Override
  public String toString() {
    return "Module{" +
            "id='" + id + '\'' +
            ", displayName='" + displayName + '\'' +
            ", description='" + description + '\'' +
            ", category=" + category +
            ", name='" + name + '\'' +
            ", version='" + version + '\'' +
            ", definition='" + definition + '\'' +
            ", dependencies=" + dependencies +
            ", components=" + components +
            '}';
  }

  /**
   * Loads the components into a map (component name, component) for ease of access.
   */
  public void populateComponentMap() {
    componentHashMap = new HashMap<>();
    for (ModuleComponent moduleComponent : getComponents()){
      // set reverse lookup
      moduleComponent.setModule(this);

      componentHashMap.put(moduleComponent.getName(), moduleComponent);
    }
  }
}
