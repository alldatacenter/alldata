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
package com.qlangtech.tis.util;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.tis.IPluginEnum;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.util.GroovyShellEvaluate;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.manage.servlet.BasicServlet;
import com.qlangtech.tis.offline.module.action.OfflineDatasourceAction;
import com.qlangtech.tis.plugin.*;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.PostedDSProp;
import com.qlangtech.tis.workflow.dao.IWorkflowDAOFacade;
import com.qlangtech.tis.workflow.pojo.DatasourceDbCriteria;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020-02-10 12:24
 */
public class PluginItems {

  private final IPluginEnum heteroEnum;
  private final UploadPluginMeta pluginMeta;
  private final IPluginContext pluginContext;

  public List<AttrValMap> items;

  private static final Set<Descriptor> dbUpdateEventObservers = Sets.newHashSet();

  private static final PluginItemsSaveObservable observable = new PluginItemsSaveObservable();

  public static void addPluginItemsSaveObserver(PluginItemsSaveObserver obsv) {
    observable.addObserver(obsv);
  }

  public PluginItems(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
    this.heteroEnum = pluginMeta.getHeteroEnum();
    this.pluginMeta = pluginMeta;
    this.pluginContext = pluginMeta.isDisableBizSet() ? new AdapterPluginContext(pluginContext) {
      @Override
      public void setBizResult(Context context, Object result) {
        //super.setBizResult(context, result);
      }
    } : pluginContext;


  }

  /**
   * datax中显示已由数据源使用 <br>
   * must call form Descriptor
   *
   * @param extendClass
   * @return
   */
  public static List<Option> getExistDbs(String... extendClass) {
    if (OfflineDatasourceAction.existDbs != null) {
      return OfflineDatasourceAction.existDbs;
    }
    if (extendClass == null || extendClass.length < 1) {
      throw new IllegalArgumentException("param extendClass can not be null");
    }

    Descriptor descriptor = GroovyShellEvaluate.descriptorThreadLocal.get();
    Objects.requireNonNull(descriptor, "descriptor can not be null");
    if (dbUpdateEventObservers.add(descriptor)) {
      // 当有数据源更新时需要将descriptor的属性重新更新一下
      addPluginItemsSaveObserver(new PluginItemsSaveObserver() {
        @Override
        public void afterSaved(PluginItemsSaveEvent event) {
          if (event.heteroEnum == HeteroEnum.DATASOURCE) {
            descriptor.cleanPropertyTypes();
          }
        }
      });
    }


    IWorkflowDAOFacade wfFacade = BasicServlet.getBeanByType(ServletActionContext.getServletContext(), IWorkflowDAOFacade.class);
    Objects.requireNonNull(wfFacade, "wfFacade can not be null");
    DatasourceDbCriteria dbCriteria = new DatasourceDbCriteria();
    List<String> extendClazzs = Lists.newArrayList(); // Lists.newArrayList(extendClass).stre;
    for (String type : extendClass) {
      extendClazzs.add(StringUtils.lowerCase(type));
    }
    dbCriteria.createCriteria().andExtendClassIn(extendClazzs);
    List<com.qlangtech.tis.workflow.pojo.DatasourceDb> dbs = wfFacade.getDatasourceDbDAO().selectByExample(dbCriteria);
    return dbs.stream().map((db) -> new Option(db.getName(), db.getName())).collect(Collectors.toList());
  }

  public ItemsSaveResult save(Context context) {
    Objects.requireNonNull(this.pluginContext, "pluginContext can not be null");
    if (items == null) {
      throw new IllegalStateException("prop items can not be null");
    }
    Descriptor.ParseDescribable describable = null;
    AttrValMap attrValMap = null;
    List<Descriptor.ParseDescribable<?>> dlist = Lists.newArrayList();
    List<Describable> describableList = Lists.newArrayList();

    if (this.pluginMeta.isAppend()) {
      IPluginStore pluginStore = heteroEnum.getPluginStore(this.pluginContext, this.pluginMeta);
      if (pluginStore != null) {
        List<Describable> plugins = pluginStore.getPlugins();
        boolean firstSkip = false;
        for (Describable p : plugins) {
          if (!firstSkip) {
            firstSkip = true;
            Descriptor.ParseDescribable describablesWithMeta = PluginStore.getDescribablesWithMeta(pluginStore, p);
            dlist.add(describablesWithMeta);
          } else {
            dlist.add(new Descriptor.ParseDescribable(p));
          }
        }
      }
    }
    for (int i = 0; i < this.items.size(); i++) {
      attrValMap = this.items.get(i);
      /**====================================================
       * 将客户端POST数据包装
       ======================================================*/
      describable = attrValMap.createDescribable(pluginContext);
      dlist.add(describable);
      if (!describable.subFormFields) {
        describableList.add((Describable) describable.getInstance());
      }
    }
    IPluginStoreSave<?> store = null;
    if (heteroEnum == HeteroEnum.APP_SOURCE) {

      for (Descriptor.ParseDescribable<?> d : dlist) {
        if (d.getInstance() instanceof IdentityName) {
          store = IAppSource.getPluginStore(pluginContext, ((IdentityName) d.getInstance()).identityValue());
          break;
        }
      }

      Objects.requireNonNull(store, "plugin type:" + heteroEnum.getIdentity() + " can not find relevant Store");

    } else if (this.pluginContext.isDataSourceAware()) {

      store = new IPluginStoreSave<DataSourceFactory>() {
        @Override
        public SetPluginsResult setPlugins(IPluginContext pluginContext, Optional<Context> context
          , List<Descriptor.ParseDescribable<DataSourceFactory>> dlist, boolean update) {
          SetPluginsResult finalResult = new SetPluginsResult(true, false);
          for (Descriptor.ParseDescribable<DataSourceFactory> plugin : dlist) {
            if (StringUtils.isEmpty(pluginMeta.getExtraParam(PostedDSProp.KEY_DB_NAME))) {
              pluginMeta.putExtraParams(PostedDSProp.KEY_DB_NAME, ((IdentityName) plugin.getInstance()).identityValue());
            }
            PostedDSProp dbExtraProps = PostedDSProp.parse(pluginMeta);

            SetPluginsResult result = TIS.getDataBasePluginStore(dbExtraProps)
              .setPlugins(pluginContext, context, Collections.singletonList(plugin), dbExtraProps.isUpdate());
            if (!result.success) {
              return result;
            }
            if (result.cfgChanged) {
              finalResult.cfgChanged = true;
            }
          }
          return finalResult;
        }
      };
    } else if (heteroEnum == HeteroEnum.DATAX_WRITER || heteroEnum == HeteroEnum.DATAX_READER) {
      //final String dataxName = pluginMeta.getExtraParam(DataxUtils.DATAX_NAME);

//      if (StringUtils.isEmpty(dataxName)) {
//        String saveDbName = pluginMeta.getExtraParam(DataxUtils.DATAX_DB_NAME);
//        if (StringUtils.isNotBlank(saveDbName)) {
//          store = DataxReader.getPluginStore(this.pluginContext, true, saveDbName);
//        } else {
//          throw new IllegalArgumentException("plugin extra param " + DataxUtils.DATAX_NAME + " can not be null");
//        }
//      } else {
//        KeyedPluginStore<?> keyStore = (heteroEnum == HeteroEnum.DATAX_READER)
//          ? DataxReader.getPluginStore(this.pluginContext, dataxName) : DataxWriter.getPluginStore(this.pluginContext, dataxName);
//        store = keyStore;
//      }
//      if ((heteroEnum == HeteroEnum.DATAX_READER)) {
//        for (Descriptor.ParseDescribable<?> dataXReader : dlist) {
//          DataSourceMeta sourceMeta = (DataSourceMeta) dataXReader.instance;
//          pluginContext.setBizResult(context, sourceMeta.getTablesInDB());
//        }
//      }
      store = HeteroEnum.getDataXReaderAndWriterStore(this.pluginContext
        , this.heteroEnum == HeteroEnum.DATAX_READER, this.pluginMeta, pluginMeta.getSubFormFilter());

      //Optional<IPropertyType.SubFormFilter> subFormFilter = pluginMeta.getSubFormFilter();

//      if (subFormFilter.isPresent()) {
//        IPropertyType.SubFormFilter filter = subFormFilter.get();
//        Optional<Descriptor> firstDesc = heteroEnum.descriptors().stream()
//          .filter((des) -> filter.match((Descriptor) des)).map((des) -> (Descriptor) des).findFirst();
//        if (!firstDesc.isPresent()) {
//          throw new IllegalStateException("can not find relevant descriptor:" + filter.uploadPluginMeta.toString());
//        }
//
//        PluginFormProperties pluginProps = firstDesc.get().getPluginFormPropertyTypes(subFormFilter);
//
//        store = pluginProps.accept(new PluginFormProperties.IVisitor() {
//          @Override
//          public IPluginStoreSave<?> visit(BaseSubFormProperties props) {
//            // 为了在更新插件时候不把plugin上的@SubForm标记的属性覆盖掉，需要先将老的plugin上的值覆盖到新http post过来的反序列化之后的plugin上
//            Class<Describable> clazz = (Class<Describable>) heteroEnum.getExtensionPoint();
//
//            DataxReader.SubFieldFormAppKey<Describable> key = HeteroEnum.createDataXReaderAndWriterRelevant(pluginContext, pluginMeta
//              , new HeteroEnum.DataXReaderAndWriterRelevantCreator<DataxReader.SubFieldFormAppKey<Describable>>() {
//                @Override
//                public DataxReader.SubFieldFormAppKey<Describable> dbRelevant(IPluginContext pluginContext, String saveDbName) {
//                  return new DataxReader.SubFieldFormAppKey<>(pluginContext, true, saveDbName, props, clazz);
//                }
//
//                @Override
//                public DataxReader.SubFieldFormAppKey<Describable> appRelevant(IPluginContext pluginContext, String dataxName) {
//                  return new DataxReader.SubFieldFormAppKey<>(pluginContext, false, dataxName, props, clazz);
//                }
//              });
//
//            return KeyedPluginStore.getPluginStore(key);
//          }
//        });
//      }

    } else if (heteroEnum == HeteroEnum.PARAMS_CONFIG) {
      store = heteroEnum.getPluginStore(this.pluginContext, pluginMeta);// new ParamsConfigPluginStore(this.pluginMeta);
    } else if (heteroEnum == HeteroEnum.DATAX_WORKER) {
//      if (!this.pluginContext.isCollectionAware()) {
//        throw new IllegalStateException("must be collection aware");
//      }
//      store = DataXJobWorker.getJobWorkerStore(new TargetResName(this.pluginContext.getCollectionName()));
      store = heteroEnum.getPluginStore(this.pluginContext, pluginMeta);
    } else {
      if (heteroEnum.isAppNameAware()) {
        if (!this.pluginContext.isCollectionAware()) {
          throw new IllegalStateException(heteroEnum.getExtensionPoint().getName() + " must be collection aware");
        }
        store = heteroEnum.getPluginStore(this.pluginContext, pluginMeta);// TIS.getPluginStore(this.pluginContext.getCollectionName(), heteroEnum.getExtensionPoint());
      } else {
        store = TIS.getPluginStore(heteroEnum.getExtensionPoint());
      }
    }
    //dlist
    SetPluginsResult result = store.setPlugins(pluginContext, Optional.of(context), convert(dlist));
    if (!result.success) {
      return new ItemsSaveResult(Collections.emptyList(), result);
    }
    observable.notifyObservers(new PluginItemsSaveEvent(this.pluginContext, this.heteroEnum, describableList, result.cfgChanged));
    return new ItemsSaveResult(describableList, result);
  }

  private <T extends Describable> List<Descriptor.ParseDescribable<T>> convert(List<Descriptor.ParseDescribable<?>> dlist) {
    return dlist.stream().map((r) -> (Descriptor.ParseDescribable<T>) r).collect(Collectors.toList());
  }

  public static class PluginItemsSaveObservable extends Observable {

    @Override
    public void notifyObservers(Object arg) {
      super.setChanged();
      super.notifyObservers(arg);
    }
  }

  public abstract static class PluginItemsSaveObserver implements Observer {

    @Override
    public final void update(Observable o, Object arg) {
      PluginItemsSaveEvent evt = (PluginItemsSaveEvent) arg;
      if (evt.cfgChanged) {
        this.afterSaved(evt);
      }
    }

    public abstract void afterSaved(PluginItemsSaveEvent event);
  }

  public static class PluginItemsSaveEvent {

    public final IPluginContext collectionName;

    public final IPluginEnum heteroEnum;

    public final List<Describable> dlist;

    public final boolean cfgChanged;

    public PluginItemsSaveEvent(IPluginContext collectionName, IPluginEnum heteroEnum, List<Describable> dlist, boolean cfgChanged) {
      this.collectionName = collectionName;
      this.heteroEnum = heteroEnum;
      this.dlist = dlist;
      this.cfgChanged = cfgChanged;
    }
  }
}
