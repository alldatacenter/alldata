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
package org.apache.atlas;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.discovery.AtlasDiscoveryService;
import org.apache.atlas.discovery.AtlasLineageService;
import org.apache.atlas.discovery.EntityDiscoveryService;
import org.apache.atlas.discovery.EntityLineageService;
import org.apache.atlas.glossary.GlossaryService;
import org.apache.atlas.graph.GraphSandboxUtil;
import org.apache.atlas.listener.EntityChangeListener;
import org.apache.atlas.listener.EntityChangeListenerV2;
import org.apache.atlas.listener.TypeDefChangeListener;
import org.apache.atlas.repository.audit.EntityAuditListener;
import org.apache.atlas.repository.audit.EntityAuditListenerV2;
import org.apache.atlas.repository.audit.EntityAuditRepository;
import org.apache.atlas.repository.graph.FullTextMapperV2;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graph.IFullTextMapper;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.GraphDBMigrator;
import org.apache.atlas.repository.graphdb.janus.migration.GraphDBGraphSONMigrator;
import org.apache.atlas.repository.impexp.ExportService;
import org.apache.atlas.repository.ogm.AtlasAuditEntryDTO;
import org.apache.atlas.repository.ogm.AtlasServerDTO;
import org.apache.atlas.repository.ogm.DTORegistry;
import org.apache.atlas.repository.ogm.DataAccess;
import org.apache.atlas.repository.ogm.DataTransferObject;
import org.apache.atlas.repository.ogm.ExportImportAuditEntryDTO;
import org.apache.atlas.repository.ogm.glossary.AtlasGlossaryCategoryDTO;
import org.apache.atlas.repository.ogm.glossary.AtlasGlossaryDTO;
import org.apache.atlas.repository.ogm.glossary.AtlasGlossaryTermDTO;
import org.apache.atlas.repository.ogm.metrics.AtlasMetricsStatDTO;
import org.apache.atlas.repository.ogm.profiles.AtlasSavedSearchDTO;
import org.apache.atlas.repository.ogm.profiles.AtlasUserProfileDTO;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.AtlasRelationshipStore;
import org.apache.atlas.repository.store.graph.BulkImporter;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityChangeNotifier;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.repository.store.graph.v2.AtlasRelationshipStoreV2;
import org.apache.atlas.repository.store.graph.v2.AtlasTypeDefGraphStoreV2;
import org.apache.atlas.repository.store.graph.v2.BulkImporterImpl;
import org.apache.atlas.repository.store.graph.v2.EntityGraphMapper;
import org.apache.atlas.repository.store.graph.v2.IAtlasEntityChangeNotifier;
import org.apache.atlas.repository.store.graph.v2.tasks.ClassificationPropagateTaskFactory;
import org.apache.atlas.runner.LocalSolrRunner;
import org.apache.atlas.service.Service;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.tasks.TaskManagement;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.util.AtlasRepositoryConfiguration;
import org.apache.atlas.util.SearchTracker;
import org.apache.commons.configuration.Configuration;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.apache.atlas.graph.GraphSandboxUtil.useLocalSolr;

@Test(enabled = false)
public class TestModules {

    static class MockNotifier implements Provider<AtlasEntityChangeNotifier> {
        @Override
        public AtlasEntityChangeNotifier get() {
            return Mockito.mock(AtlasEntityChangeNotifier.class);
        }
    }

    // Test only DI modules
    public static class TestOnlyModule extends AbstractModule {

        private static final Logger LOG = LoggerFactory.getLogger(TestOnlyModule.class);

        static class AtlasConfigurationProvider implements Provider<Configuration> {

            @Override
            public Configuration get() {
                try {
                    return ApplicationProperties.get();
                } catch (AtlasException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        static class AtlasGraphProvider implements Provider<AtlasGraph> {
            @Override
            public AtlasGraph get() {
                return org.apache.atlas.repository.graph.AtlasGraphProvider.getGraphInstance();
            }
        }

        @Override
        protected void configure() {
            GraphSandboxUtil.create();

            if (useLocalSolr()) {
                try {
                    LocalSolrRunner.start();
                } catch (Exception e) {
                    //ignore
                }
            }

            bindAuditRepository(binder());

            bind(AtlasGraph.class).toProvider(AtlasGraphProvider.class);

            // allow for dynamic binding of graph service
            bind(Configuration.class).toProvider(AtlasConfigurationProvider.class).in(Singleton.class);

            // bind the AtlasTypeDefStore interface to an implementation
            bind(AtlasTypeDefStore.class).to(AtlasTypeDefGraphStoreV2.class).asEagerSingleton();

            bind(AtlasTypeRegistry.class).asEagerSingleton();
            bind(EntityGraphMapper.class).asEagerSingleton();
            bind(ExportService.class).asEagerSingleton();

            // New typesdef/instance change listener should also be bound to the corresponding implementation
            Multibinder<TypeDefChangeListener> typeDefChangeListenerMultibinder =
                    Multibinder.newSetBinder(binder(), TypeDefChangeListener.class);
            typeDefChangeListenerMultibinder.addBinding().to(GraphBackedSearchIndexer.class).asEagerSingleton();

            bind(SearchTracker.class).asEagerSingleton();

            bind(AtlasEntityStore.class).to(AtlasEntityStoreV2.class);
            bind(AtlasRelationshipStore.class).to(AtlasRelationshipStoreV2.class);
            bind(IAtlasEntityChangeNotifier.class).to(AtlasEntityChangeNotifier.class);
            bind(IFullTextMapper.class).to(FullTextMapperV2.class);

            // bind the DiscoveryService interface to an implementation
            bind(AtlasDiscoveryService.class).to(EntityDiscoveryService.class).asEagerSingleton();

            bind(AtlasLineageService.class).to(EntityLineageService.class).asEagerSingleton();
            bind(BulkImporter.class).to(BulkImporterImpl.class).asEagerSingleton();
            bind(GraphDBMigrator.class).to(GraphDBGraphSONMigrator.class).asEagerSingleton();

            //Add EntityAuditListener as EntityChangeListener
            Multibinder<EntityChangeListener> entityChangeListenerBinder =
                    Multibinder.newSetBinder(binder(), EntityChangeListener.class);
            entityChangeListenerBinder.addBinding().to(EntityAuditListener.class);

            Multibinder<EntityChangeListenerV2> entityChangeListenerV2Binder =
                    Multibinder.newSetBinder(binder(), EntityChangeListenerV2.class);
            entityChangeListenerV2Binder.addBinding().to(EntityAuditListenerV2.class);

            // OGM related mappings
            Multibinder<DataTransferObject> availableDTOs = Multibinder.newSetBinder(binder(), DataTransferObject.class);
            availableDTOs.addBinding().to(AtlasUserProfileDTO.class);
            availableDTOs.addBinding().to(AtlasSavedSearchDTO.class);
            availableDTOs.addBinding().to(AtlasGlossaryDTO.class);
            availableDTOs.addBinding().to(AtlasGlossaryTermDTO.class);
            availableDTOs.addBinding().to(AtlasGlossaryCategoryDTO.class);
            availableDTOs.addBinding().to(AtlasServerDTO.class);
            availableDTOs.addBinding().to(ExportImportAuditEntryDTO.class);
            availableDTOs.addBinding().to(AtlasAuditEntryDTO.class);
            availableDTOs.addBinding().to(AtlasMetricsStatDTO.class);

            bind(DTORegistry.class).asEagerSingleton();
            bind(DataAccess.class).asEagerSingleton();

            // Glossary related bindings
            bind(GlossaryService.class).asEagerSingleton();

            // TaskManagement
            bind(TaskManagement.class).asEagerSingleton();
            bind(ClassificationPropagateTaskFactory.class).asEagerSingleton();

            final GraphTransactionInterceptor graphTransactionInterceptor = new GraphTransactionInterceptor(new AtlasGraphProvider().get(), null);
            requestInjection(graphTransactionInterceptor);
            bindInterceptor(Matchers.any(), Matchers.annotatedWith(GraphTransaction.class), graphTransactionInterceptor);
        }

        protected void bindAuditRepository(Binder binder) {

            Class<? extends EntityAuditRepository> auditRepoImpl = AtlasRepositoryConfiguration.getAuditRepositoryImpl();

            //Map EntityAuditRepository interface to configured implementation
            binder.bind(EntityAuditRepository.class).to(auditRepoImpl).asEagerSingleton();

            if(Service.class.isAssignableFrom(auditRepoImpl)) {
                Class<? extends Service> auditRepoService = (Class<? extends Service>)auditRepoImpl;
                //if it's a service, make sure that it gets properly closed at shutdown
                Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder, Service.class);
                serviceBinder.addBinding().to(auditRepoService);
            }
        }
    }
}
