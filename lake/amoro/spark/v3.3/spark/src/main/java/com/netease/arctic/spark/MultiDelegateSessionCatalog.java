/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark;

import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableSet;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.spark.sql.catalyst.analysis.NamespaceAlreadyExistsException;
import org.apache.spark.sql.catalyst.analysis.NoSuchFunctionException;
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException;
import org.apache.spark.sql.catalyst.analysis.NoSuchProcedureException;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.apache.spark.sql.catalyst.analysis.NonEmptyNamespaceException;
import org.apache.spark.sql.catalyst.analysis.TableAlreadyExistsException;
import org.apache.spark.sql.connector.catalog.CatalogExtension;
import org.apache.spark.sql.connector.catalog.CatalogPlugin;
import org.apache.spark.sql.connector.catalog.Identifier;
import org.apache.spark.sql.connector.catalog.NamespaceChange;
import org.apache.spark.sql.connector.catalog.StagedTable;
import org.apache.spark.sql.connector.catalog.StagingTableCatalog;
import org.apache.spark.sql.connector.catalog.SupportsNamespaces;
import org.apache.spark.sql.connector.catalog.Table;
import org.apache.spark.sql.connector.catalog.TableCatalog;
import org.apache.spark.sql.connector.catalog.TableChange;
import org.apache.spark.sql.connector.catalog.functions.UnboundFunction;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.connector.iceberg.catalog.Procedure;
import org.apache.spark.sql.connector.iceberg.catalog.ProcedureCatalog;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * this catalog is used for spark_catalog when multi session catalog is need.
 */
public class MultiDelegateSessionCatalog<T extends TableCatalog & SupportsNamespaces>
    implements StagingTableCatalog, SupportsNamespaces, CatalogExtension, ProcedureCatalog {

  public static final String PARAM_DELEGATES = "delegates";

  private static final Set<String> INNER_OPTIONS = ImmutableSet.of(PARAM_DELEGATES);

  private CaseInsensitiveStringMap options;

  private CatalogHolder delegateCatalog;

  @Override
  public void initialize(String name, CaseInsensitiveStringMap options) {
    Preconditions.checkArgument(
        "spark_catalog".equalsIgnoreCase(name),
        MultiDelegateSessionCatalog.class.getName() + " can only be used for spark_catalog");
    Preconditions.checkArgument(
        options.containsKey(PARAM_DELEGATES),
        "lack require parameter " + PARAM_DELEGATES);
    this.options = options;
  }

  @Override
  public void setDelegateCatalog(CatalogPlugin delegate) {
    T sessionCatalog;
    if (delegate instanceof TableCatalog && delegate instanceof SupportsNamespaces) {
      sessionCatalog = (T) delegate;
    } else {
      throw new IllegalArgumentException("delegate catalog must be CatalogHolder");
    }

    List<CatalogHolder> delegates = getCatalogs(this.options);
    Preconditions.checkArgument(
        delegates.size() > 0,
        "delegates can not be empty");

    Iterator<CatalogHolder> iterator = delegates.iterator();
    CatalogHolder delegateCatalog = iterator.next();
    CatalogHolder catalog = delegateCatalog;

    while (iterator.hasNext()) {
      CatalogHolder nextCatalog = iterator.next();
      catalog.setDelegateCatalog(nextCatalog);
      catalog = nextCatalog;
    }
    catalog.setDelegateCatalog(new CatalogHolder<>(sessionCatalog));
    this.delegateCatalog = delegateCatalog;
  }

  @Override
  public StagedTable stageCreate(
      Identifier ident,
      StructType schema,
      Transform[] partitions,
      Map<String, String> properties) throws TableAlreadyExistsException, NoSuchNamespaceException {
    return this.delegateCatalog.stageCreate(ident, schema, partitions, properties);
  }

  @Override
  public StagedTable stageReplace(
      Identifier ident,
      StructType schema,
      Transform[] partitions,
      Map<String, String> properties) throws NoSuchNamespaceException, NoSuchTableException {
    return this.delegateCatalog.stageReplace(ident, schema, partitions, properties);
  }

  @Override
  public StagedTable stageCreateOrReplace(
      Identifier ident,
      StructType schema,
      Transform[] partitions,
      Map<String, String> properties) throws NoSuchNamespaceException {
    return this.delegateCatalog.stageCreateOrReplace(ident, schema, partitions, properties);
  }

  @Override
  public String[][] listNamespaces() throws NoSuchNamespaceException {
    return this.delegateCatalog.listNamespaces();
  }

  @Override
  public String[][] listNamespaces(String[] namespace) throws NoSuchNamespaceException {
    return this.delegateCatalog.listNamespaces();
  }

  @Override
  public boolean namespaceExists(String[] namespace) {
    return this.delegateCatalog.namespaceExists(namespace);
  }

  @Override
  public Map<String, String> loadNamespaceMetadata(String[] namespace) throws NoSuchNamespaceException {
    return this.delegateCatalog.loadNamespaceMetadata(namespace);
  }

  @Override
  public void createNamespace(String[] namespace, Map<String, String> metadata) throws NamespaceAlreadyExistsException {
    this.delegateCatalog.createNamespace(namespace, metadata);
  }

  @Override
  public void alterNamespace(String[] namespace, NamespaceChange... changes) throws NoSuchNamespaceException {
    this.delegateCatalog.alterNamespace(namespace, changes);
  }

  @Override
  public boolean dropNamespace(String[] namespace, boolean cascade)
      throws NoSuchNamespaceException, NonEmptyNamespaceException {
    return this.delegateCatalog.dropNamespace(namespace, cascade);
  }

  @Override
  public Identifier[] listTables(String[] namespace) throws NoSuchNamespaceException {
    return this.delegateCatalog.listTables(namespace);
  }

  @Override
  public Table loadTable(Identifier ident) throws NoSuchTableException {
    return this.delegateCatalog.loadTable(ident);
  }

  @Override
  public void invalidateTable(Identifier ident) {
    this.delegateCatalog.invalidateTable(ident);
  }

  @Override
  public boolean tableExists(Identifier ident) {
    return this.delegateCatalog.tableExists(ident);
  }

  @Override
  public Table createTable(Identifier ident, StructType schema, Transform[] partitions, Map<String, String> properties)
      throws TableAlreadyExistsException, NoSuchNamespaceException {
    return this.delegateCatalog.createTable(ident, schema, partitions, properties);
  }

  @Override
  public Table alterTable(Identifier ident, TableChange... changes) throws NoSuchTableException {
    return this.delegateCatalog.alterTable(ident, changes);
  }

  @Override
  public boolean dropTable(Identifier ident) {
    return this.delegateCatalog.dropTable(ident);
  }

  @Override
  public boolean purgeTable(Identifier ident) throws UnsupportedOperationException {
    return this.delegateCatalog.purgeTable(ident);
  }

  @Override
  public void renameTable(Identifier oldIdent, Identifier newIdent)
      throws NoSuchTableException, TableAlreadyExistsException {
    this.delegateCatalog.renameTable(oldIdent, newIdent);
  }

  @Override
  public String name() {
    return "spark_catalog";
  }

  @Override
  public String[] defaultNamespace() {
    return this.delegateCatalog.defaultNamespace();
  }

  @Override
  public Procedure loadProcedure(Identifier ident) throws NoSuchProcedureException {
    return delegateCatalog.loadProcedure(ident);
  }

  private List<CatalogHolder> getCatalogs(CaseInsensitiveStringMap options) {
    Map<String, Map<String, String>> catalogOptions = Maps.newHashMap();
    Map<String, String> catalogClassName = Maps.newHashMap();
    List<String> catalogs = Lists.newArrayList(options.get(PARAM_DELEGATES).split(","));

    for (String catalog : catalogs) {
      catalogOptions.put(catalog, Maps.newHashMap());
      String className = options.get(catalog);
      Preconditions.checkArgument(
          className != null,
          "lack implement class for catalog: " + catalog);
      catalogClassName.put(catalog, className);
    }

    for (String key : options.keySet()) {
      if (INNER_OPTIONS.contains(key)) {
        continue;
      } else if (key.contains(".")) {
        String catalog = key.split("\\.")[0];
        String property = key.substring(key.indexOf(".") + 1);
        Preconditions.checkArgument(
            catalogOptions.containsKey(catalog),
            "catalog " + catalog + " is not defined");
        catalogOptions.get(catalog).put(property, options.get(key));
      }
    }

    return catalogs.stream()
        .map(catalog -> {
          Map<String, String> option = catalogOptions.get(catalog);
          String className = catalogClassName.get(catalog);
          return loadCatalog(catalog, className, option);
        })
        .map(CatalogHolder::new)
        .collect(Collectors.toList());
  }

  private CatalogExtension loadCatalog(String catalogName, String className, Map<String, String> options) {
    ClassLoader loader = getClassLoader();
    try {
      Class<?> pluginClass = Class.forName(className, true, loader);
      if (!CatalogExtension.class.isAssignableFrom(pluginClass)) {
        throw new IllegalStateException(
            String.format("Plugin class for %s does not implement CatalogExtension: %s",
                catalogName, className));
      }
      CatalogExtension catalog = (CatalogExtension) pluginClass.getDeclaredConstructor().newInstance();
      catalog.initialize(this.name(), new CaseInsensitiveStringMap(options));
      return catalog;
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Cannot find delegate catalog plugin class for catalog " + catalogName +
          ": " + className, e);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(
          "Cannot find a no-arg constructor for delegate catalog plugin class for catalog " +
              catalogName + ": " + className, e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(
          "Failed during call to no-arg constructor for delegate catalog plugin class for catalog " +
              catalogName + ": " + className, e);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(
          "Failed to call public no-arg constructor for delegate catalog plugin class for catalog " +
              catalogName + ": " + className, e);
    }
  }

  private ClassLoader getClassLoader() {
    return Optional.of(Thread.currentThread().getContextClassLoader())
        .orElseGet(() -> getClass().getClassLoader());
  }

  @Override
  public Identifier[] listFunctions(String[] namespace) throws NoSuchNamespaceException {
    return new Identifier[0];
  }

  @Override
  public UnboundFunction loadFunction(Identifier ident) throws NoSuchFunctionException {
    return null;
  }

  private static class CatalogHolder<T extends TableCatalog & SupportsNamespaces>
      implements CatalogExtension, ProcedureCatalog, StagingTableCatalog {

    private final T holder;

    private CatalogHolder(T catalog) {
      this.holder = catalog;
    }

    private CatalogHolder<T> delegate;

    @Override
    public void setDelegateCatalog(CatalogPlugin delegate) {
      Preconditions.checkArgument(
          delegate instanceof CatalogHolder,
          "delegate catalog must be CatalogHolder");
      ((CatalogExtension) holder).setDelegateCatalog(delegate);
      this.delegate = (CatalogHolder<T>) delegate;
    }

    @Override
    public String[][] listNamespaces() throws NoSuchNamespaceException {
      return holder.listNamespaces();
    }

    @Override
    public String[][] listNamespaces(String[] namespace) throws NoSuchNamespaceException {
      return holder.listNamespaces(namespace);
    }

    @Override
    public boolean namespaceExists(String[] namespace) {
      return holder.namespaceExists(namespace);
    }

    @Override
    public Map<String, String> loadNamespaceMetadata(String[] namespace) throws NoSuchNamespaceException {
      return holder.loadNamespaceMetadata(namespace);
    }

    @Override
    public void createNamespace(String[] namespace, Map<String, String> metadata)
        throws NamespaceAlreadyExistsException {
      holder.createNamespace(namespace, metadata);
    }

    @Override
    public void alterNamespace(String[] namespace, NamespaceChange... changes) throws NoSuchNamespaceException {
      holder.alterNamespace(namespace, changes);
    }

    @Override
    public boolean dropNamespace(String[] namespace, boolean cascade)
        throws NoSuchNamespaceException, NonEmptyNamespaceException {
      return holder.dropNamespace(namespace, cascade);
    }

    @Override
    public Identifier[] listTables(String[] namespace) throws NoSuchNamespaceException {
      return holder.listTables(namespace);
    }

    @Override
    public Table loadTable(Identifier ident) throws NoSuchTableException {
      return holder.loadTable(ident);
    }

    @Override
    public void invalidateTable(Identifier ident) {
      holder.invalidateTable(ident);
    }

    @Override
    public boolean tableExists(Identifier ident) {
      return holder.tableExists(ident);
    }

    @Override
    public Table createTable(
        Identifier ident,
        StructType schema,
        Transform[] partitions,
        Map<String, String> properties) throws TableAlreadyExistsException, NoSuchNamespaceException {
      return holder.createTable(ident, schema, partitions, properties);
    }

    @Override
    public Table alterTable(Identifier ident, TableChange... changes) throws NoSuchTableException {
      return holder.alterTable(ident, changes);
    }

    @Override
    public boolean dropTable(Identifier ident) {
      return holder.dropTable(ident);
    }

    @Override
    public boolean purgeTable(Identifier ident) throws UnsupportedOperationException {
      return holder.purgeTable(ident);
    }

    @Override
    public void renameTable(Identifier oldIdent, Identifier newIdent)
        throws NoSuchTableException, TableAlreadyExistsException {
      holder.renameTable(oldIdent, newIdent);
    }

    @Override
    public void initialize(String name, CaseInsensitiveStringMap options) {
      holder.initialize(name, options);
    }

    @Override
    public String name() {
      return holder.name();
    }

    @Override
    public String[] defaultNamespace() {
      return holder.defaultNamespace();
    }

    // ======================= expend holder interface =======================

    @Override
    public StagedTable stageCreate(
        Identifier ident,
        StructType schema,
        Transform[] partitions,
        Map<String, String> properties) throws TableAlreadyExistsException, NoSuchNamespaceException {
      if (holder instanceof StagingTableCatalog) {
        return ((StagingTableCatalog) holder).stageCreate(ident, schema, partitions, properties);
      } else if (delegate != null) {
        return delegate.stageCreate(ident, schema, partitions, properties);
      } else {
        throw new UnsupportedOperationException("stageCreate is not supported");
      }
    }

    @Override
    public StagedTable stageReplace(
        Identifier ident,
        StructType schema,
        Transform[] partitions,
        Map<String, String> properties) throws NoSuchNamespaceException, NoSuchTableException {
      if (holder instanceof StagingTableCatalog) {
        return ((StagingTableCatalog) holder).stageReplace(ident, schema, partitions, properties);
      } else if (delegate != null) {
        return delegate.stageReplace(ident, schema, partitions, properties);
      } else {
        throw new UnsupportedOperationException("stageReplace is not supported");
      }
    }

    @Override
    public StagedTable stageCreateOrReplace(
        Identifier ident,
        StructType schema,
        Transform[] partitions,
        Map<String, String> properties) throws NoSuchNamespaceException {
      if (holder instanceof StagingTableCatalog) {
        return ((StagingTableCatalog) holder).stageCreateOrReplace(ident, schema, partitions, properties);
      } else if (delegate != null) {
        return delegate.stageCreateOrReplace(ident, schema, partitions, properties);
      } else {
        throw new UnsupportedOperationException("stageCreateOrReplace is not supported");
      }
    }

    @Override
    public Procedure loadProcedure(Identifier ident) throws NoSuchProcedureException {
      if (holder instanceof ProcedureCatalog) {
        return ((ProcedureCatalog) holder).loadProcedure(ident);
      } else if (delegate != null) {
        return delegate.loadProcedure(ident);
      } else {
        throw new UnsupportedOperationException("loadProcedure is not supported");
      }
    }

    @Override
    public Identifier[] listFunctions(String[] namespace) throws NoSuchNamespaceException {
      return new Identifier[0];
    }

    @Override
    public UnboundFunction loadFunction(Identifier ident) throws NoSuchFunctionException {
      return null;
    }
  }
}
