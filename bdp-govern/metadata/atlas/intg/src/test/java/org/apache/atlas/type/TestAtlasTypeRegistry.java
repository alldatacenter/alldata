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
package org.apache.atlas.type;


import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.*;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.type.AtlasTypeRegistry.AtlasTransientTypeRegistry;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

public class TestAtlasTypeRegistry {

    /*
     *             L0
     *          /      \
     *         /         \
     *      L1_1----      L1_2
     *      /  \    \    /   \
     *     /    \    \  /     \
     *   L2_1  L2_2   L2_3   L2_4
     */
    @Test
    public void testClassificationDefValidHierarchy() throws AtlasBaseException {
        AtlasClassificationDef classifiL0   = new AtlasClassificationDef("L0");
        AtlasClassificationDef classifiL1_1 = new AtlasClassificationDef("L1-1");
        AtlasClassificationDef classifiL1_2 = new AtlasClassificationDef("L1-2");
        AtlasClassificationDef classifiL2_1 = new AtlasClassificationDef("L2-1");
        AtlasClassificationDef classifiL2_2 = new AtlasClassificationDef("L2-2");
        AtlasClassificationDef classifiL2_3 = new AtlasClassificationDef("L2-3");
        AtlasClassificationDef classifiL2_4 = new AtlasClassificationDef("L2-4");

        classifiL1_1.addSuperType(classifiL0.getName());
        classifiL1_2.addSuperType(classifiL0.getName());
        classifiL2_1.addSuperType(classifiL1_1.getName());
        classifiL2_2.addSuperType(classifiL1_1.getName());
        classifiL2_3.addSuperType(classifiL1_1.getName());
        classifiL2_3.addSuperType(classifiL1_2.getName());
        classifiL2_4.addSuperType(classifiL1_2.getName());

        classifiL0.addAttribute(new AtlasAttributeDef("L0_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        classifiL1_1.addAttribute(new AtlasAttributeDef("L1-1_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        classifiL1_2.addAttribute(new AtlasAttributeDef("L1-2_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        classifiL2_1.addAttribute(new AtlasAttributeDef("L2-1_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        classifiL2_2.addAttribute(new AtlasAttributeDef("L2-2_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        classifiL2_3.addAttribute(new AtlasAttributeDef("L2-3_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        classifiL2_4.addAttribute(new AtlasAttributeDef("L2-4_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));

        AtlasTypesDef typesDef = new AtlasTypesDef();

        typesDef.getClassificationDefs().add(classifiL0);
        typesDef.getClassificationDefs().add(classifiL1_1);
        typesDef.getClassificationDefs().add(classifiL1_2);
        typesDef.getClassificationDefs().add(classifiL2_1);
        typesDef.getClassificationDefs().add(classifiL2_2);
        typesDef.getClassificationDefs().add(classifiL2_3);
        typesDef.getClassificationDefs().add(classifiL2_4);

        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        String                     failureMsg   = null;

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(typesDef);

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNull(failureMsg);


        validateAllSuperTypes(typeRegistry, "L0", new HashSet<String>());
        validateAllSuperTypes(typeRegistry, "L1-1", new HashSet<>(Arrays.asList("L0")));
        validateAllSuperTypes(typeRegistry, "L1-2", new HashSet<>(Arrays.asList("L0")));
        validateAllSuperTypes(typeRegistry, "L2-1", new HashSet<>(Arrays.asList("L1-1", "L0")));
        validateAllSuperTypes(typeRegistry, "L2-2", new HashSet<>(Arrays.asList("L1-1", "L0")));
        validateAllSuperTypes(typeRegistry, "L2-3", new HashSet<>(Arrays.asList("L1-1", "L0", "L1-2")));
        validateAllSuperTypes(typeRegistry, "L2-4", new HashSet<>(Arrays.asList("L1-2", "L0")));

        validateSubTypes(typeRegistry, "L0", new HashSet<>(Arrays.asList("L1-1", "L1-2")));
        validateSubTypes(typeRegistry, "L1-1", new HashSet<>(Arrays.asList("L2-1", "L2-2", "L2-3")));
        validateSubTypes(typeRegistry, "L1-2", new HashSet<>(Arrays.asList("L2-3", "L2-4")));
        validateSubTypes(typeRegistry, "L2-1", new HashSet<String>());
        validateSubTypes(typeRegistry, "L2-2", new HashSet<String>());
        validateSubTypes(typeRegistry, "L2-3", new HashSet<String>());
        validateSubTypes(typeRegistry, "L2-4", new HashSet<String>());

        validateAllSubTypes(typeRegistry, "L0", new HashSet<>(Arrays.asList("L1-1", "L1-2", "L2-1", "L2-2", "L2-3", "L2-4")));
        validateAllSubTypes(typeRegistry, "L1-1", new HashSet<>(Arrays.asList("L2-1", "L2-2", "L2-3")));
        validateAllSubTypes(typeRegistry, "L1-2", new HashSet<>(Arrays.asList("L2-3", "L2-4")));
        validateAllSubTypes(typeRegistry, "L2-1", new HashSet<String>());
        validateAllSubTypes(typeRegistry, "L2-2", new HashSet<String>());
        validateAllSubTypes(typeRegistry, "L2-3", new HashSet<String>());
        validateAllSubTypes(typeRegistry, "L2-4", new HashSet<String>());

        validateAttributeNames(typeRegistry, "L0", new HashSet<>(Arrays.asList("L0_a1")));
        validateAttributeNames(typeRegistry, "L1-1", new HashSet<>(Arrays.asList("L0_a1", "L1-1_a1")));
        validateAttributeNames(typeRegistry, "L1-2", new HashSet<>(Arrays.asList("L0_a1", "L1-2_a1")));
        validateAttributeNames(typeRegistry, "L2-1", new HashSet<>(Arrays.asList("L0_a1", "L1-1_a1", "L2-1_a1")));
        validateAttributeNames(typeRegistry, "L2-2", new HashSet<>(Arrays.asList("L0_a1", "L1-1_a1", "L2-2_a1")));
        validateAttributeNames(typeRegistry, "L2-3", new HashSet<>(Arrays.asList("L0_a1", "L1-1_a1", "L1-2_a1", "L2-3_a1")));
        validateAttributeNames(typeRegistry, "L2-4", new HashSet<>(Arrays.asList("L0_a1", "L1-2_a1", "L2-4_a1")));
    }

    @Test
    public void testClassificationDefInvalidHierarchy_Self() throws AtlasBaseException {
        AtlasClassificationDef classifiDef1 = new AtlasClassificationDef("classifiDef-1");

        classifiDef1.addSuperType(classifiDef1.getName());

        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        String                     failureMsg   = null;

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addType(classifiDef1);

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNotNull(failureMsg, "expected invalid supertype failure");
    }

    /*
     *       L2_3
     *           \
     *             L0
     *          /      \
     *         /         \
     *      L1_1----      L1_2
     *      /  \    \    /   \
     *     /    \    \  /     \
     *   L2_1  L2_2   L2_3   L2_4
     */
    @Test
    public void testClassificationDefInvalidHierarchy_CircularRef() throws AtlasBaseException {
        AtlasClassificationDef classifiL0   = new AtlasClassificationDef("L0");
        AtlasClassificationDef classifiL1_1 = new AtlasClassificationDef("L1-1");
        AtlasClassificationDef classifiL1_2 = new AtlasClassificationDef("L1-2");
        AtlasClassificationDef classifiL2_1 = new AtlasClassificationDef("L2-1");
        AtlasClassificationDef classifiL2_2 = new AtlasClassificationDef("L2-2");
        AtlasClassificationDef classifiL2_3 = new AtlasClassificationDef("L2-3");
        AtlasClassificationDef classifiL2_4 = new AtlasClassificationDef("L2-4");

        classifiL1_1.addSuperType(classifiL0.getName());
        classifiL1_2.addSuperType(classifiL0.getName());
        classifiL2_1.addSuperType(classifiL1_1.getName());
        classifiL2_2.addSuperType(classifiL1_1.getName());
        classifiL2_3.addSuperType(classifiL1_1.getName());
        classifiL2_3.addSuperType(classifiL1_2.getName());
        classifiL2_4.addSuperType(classifiL1_2.getName());
        classifiL0.addSuperType(classifiL2_3.getName()); // circular-ref

        AtlasTypesDef typesDef = new AtlasTypesDef();

        typesDef.getClassificationDefs().add(classifiL0);
        typesDef.getClassificationDefs().add(classifiL1_1);
        typesDef.getClassificationDefs().add(classifiL1_2);
        typesDef.getClassificationDefs().add(classifiL2_1);
        typesDef.getClassificationDefs().add(classifiL2_2);
        typesDef.getClassificationDefs().add(classifiL2_3);
        typesDef.getClassificationDefs().add(classifiL2_4);

        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        String                     failureMsg   = null;

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(typesDef);

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNotNull(failureMsg, "expected invalid supertype failure");
    }

    /*
     *             L0        L0_1
     *          /      \
     *         /         \
     *      L1_1----      L1_2
     *      /  \    \    /   \
     *     /    \    \  /     \
     *   L2_1  L2_2   L2_3   L2_4
     */
    @Test
    public void testEntityDefValidHierarchy() throws AtlasBaseException {
        AtlasEntityDef entL0   = new AtlasEntityDef("L0");
        AtlasEntityDef entL0_1 = new AtlasEntityDef("L0-1");
        AtlasEntityDef entL1_1 = new AtlasEntityDef("L1-1");
        AtlasEntityDef entL1_2 = new AtlasEntityDef("L1-2");
        AtlasEntityDef entL2_1 = new AtlasEntityDef("L2-1");
        AtlasEntityDef entL2_2 = new AtlasEntityDef("L2-2");
        AtlasEntityDef entL2_3 = new AtlasEntityDef("L2-3");
        AtlasEntityDef entL2_4 = new AtlasEntityDef("L2-4");

        entL1_1.addSuperType(entL0.getName());
        entL1_2.addSuperType(entL0.getName());
        entL2_1.addSuperType(entL1_1.getName());
        entL2_2.addSuperType(entL1_1.getName());
        entL2_3.addSuperType(entL1_1.getName());
        entL2_3.addSuperType(entL1_2.getName());
        entL2_4.addSuperType(entL1_2.getName());

        entL0.addAttribute(new AtlasAttributeDef("L0_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        entL1_1.addAttribute(new AtlasAttributeDef("L1-1_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        entL1_2.addAttribute(new AtlasAttributeDef("L1-2_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        entL2_1.addAttribute(new AtlasAttributeDef("L2-1_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        entL2_2.addAttribute(new AtlasAttributeDef("L2-2_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        entL2_3.addAttribute(new AtlasAttributeDef("L2-3_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        entL2_4.addAttribute(new AtlasAttributeDef("L2-4_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));

        // set displayNames in L0, L1_1, L2_1
        entL0.setOption(AtlasEntityDef.OPTION_DISPLAY_TEXT_ATTRIBUTE, "L0_a1");
        entL1_1.setOption(AtlasEntityDef.OPTION_DISPLAY_TEXT_ATTRIBUTE, "L1-1_a1");
        entL2_1.setOption(AtlasEntityDef.OPTION_DISPLAY_TEXT_ATTRIBUTE, "L2-1_a1");
        entL2_4.setOption(AtlasEntityDef.OPTION_DISPLAY_TEXT_ATTRIBUTE, "non-existing-attr");

        AtlasTypesDef typesDef = new AtlasTypesDef();

        typesDef.getEntityDefs().add(entL0);
        typesDef.getEntityDefs().add(entL0_1);
        typesDef.getEntityDefs().add(entL1_1);
        typesDef.getEntityDefs().add(entL1_2);
        typesDef.getEntityDefs().add(entL2_1);
        typesDef.getEntityDefs().add(entL2_2);
        typesDef.getEntityDefs().add(entL2_3);
        typesDef.getEntityDefs().add(entL2_4);

        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        String                     failureMsg   = null;

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(typesDef);

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNull(failureMsg);

        validateAllSuperTypes(typeRegistry, "L0", new HashSet<String>());
        validateAllSuperTypes(typeRegistry, "L1-1", new HashSet<>(Arrays.asList("L0")));
        validateAllSuperTypes(typeRegistry, "L1-2", new HashSet<>(Arrays.asList("L0")));
        validateAllSuperTypes(typeRegistry, "L2-1", new HashSet<>(Arrays.asList("L1-1", "L0")));
        validateAllSuperTypes(typeRegistry, "L2-2", new HashSet<>(Arrays.asList("L1-1", "L0")));
        validateAllSuperTypes(typeRegistry, "L2-3", new HashSet<>(Arrays.asList("L1-1", "L0", "L1-2")));
        validateAllSuperTypes(typeRegistry, "L2-4", new HashSet<>(Arrays.asList("L1-2", "L0")));

        validateSubTypes(typeRegistry, "L0", new HashSet<>(Arrays.asList("L1-1", "L1-2")));
        validateSubTypes(typeRegistry, "L1-1", new HashSet<>(Arrays.asList("L2-1", "L2-2", "L2-3")));
        validateSubTypes(typeRegistry, "L1-2", new HashSet<>(Arrays.asList("L2-3", "L2-4")));
        validateSubTypes(typeRegistry, "L2-1", new HashSet<String>());
        validateSubTypes(typeRegistry, "L2-2", new HashSet<String>());
        validateSubTypes(typeRegistry, "L2-3", new HashSet<String>());
        validateSubTypes(typeRegistry, "L2-4", new HashSet<String>());

        validateAllSubTypes(typeRegistry, "L0", new HashSet<>(Arrays.asList("L1-1", "L1-2", "L2-1", "L2-2", "L2-3", "L2-4")));
        validateAllSubTypes(typeRegistry, "L1-1", new HashSet<>(Arrays.asList("L2-1", "L2-2", "L2-3")));
        validateAllSubTypes(typeRegistry, "L1-2", new HashSet<>(Arrays.asList("L2-3", "L2-4")));
        validateAllSubTypes(typeRegistry, "L2-1", new HashSet<String>());
        validateAllSubTypes(typeRegistry, "L2-2", new HashSet<String>());
        validateAllSubTypes(typeRegistry, "L2-3", new HashSet<String>());
        validateAllSubTypes(typeRegistry, "L2-4", new HashSet<String>());

        validateAttributeNames(typeRegistry, "L0", new HashSet<>(Arrays.asList("L0_a1")));
        validateAttributeNames(typeRegistry, "L1-1", new HashSet<>(Arrays.asList("L0_a1", "L1-1_a1")));
        validateAttributeNames(typeRegistry, "L1-2", new HashSet<>(Arrays.asList("L0_a1", "L1-2_a1")));
        validateAttributeNames(typeRegistry, "L2-1", new HashSet<>(Arrays.asList("L0_a1", "L1-1_a1", "L2-1_a1")));
        validateAttributeNames(typeRegistry, "L2-2", new HashSet<>(Arrays.asList("L0_a1", "L1-1_a1", "L2-2_a1")));
        validateAttributeNames(typeRegistry, "L2-3", new HashSet<>(Arrays.asList("L0_a1", "L1-1_a1", "L1-2_a1", "L2-3_a1")));
        validateAttributeNames(typeRegistry, "L2-4", new HashSet<>(Arrays.asList("L0_a1", "L1-2_a1", "L2-4_a1")));

        validateDisplayNameAttribute(typeRegistry, "L0", "L0_a1");     // directly assigned for this type
        validateDisplayNameAttribute(typeRegistry, "L0-1");            // not assigned for this type
        validateDisplayNameAttribute(typeRegistry, "L1-1", "L1-1_a1"); // directly assigned for this type
        validateDisplayNameAttribute(typeRegistry, "L1-2", "L0_a1");   // inherits from L0
        validateDisplayNameAttribute(typeRegistry, "L2-1", "L2-1_a1"); // directly assigned for this type
        validateDisplayNameAttribute(typeRegistry, "L2-2", "L1-1_a1"); // inherits from L1-1
        validateDisplayNameAttribute(typeRegistry, "L2-3", "L1-1_a1"); // inherits from L1-1 or L0
        validateDisplayNameAttribute(typeRegistry, "L2-4", "L0_a1");   // invalid-name ignored, inherits from L0
    }

    @Test
    public void testEntityDefInvalidHierarchy_Self() throws AtlasBaseException {
        AtlasEntityDef entDef1 = new AtlasEntityDef("entDef-1");

        entDef1.addSuperType(entDef1.getName());

        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        String                     failureMsg   = null;

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addType(entDef1);

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNotNull(failureMsg, "expected invalid supertype failure");
    }

    /*
     *       L2_3
     *           \
     *             L0
     *          /      \
     *         /         \
     *      L1_1----      L1_2
     *      /  \    \    /   \
     *     /    \    \  /     \
     *   L2_1  L2_2   L2_3   L2_4
     */
    @Test
    public void testEntityDefInvalidHierarchy_CircularRef() throws AtlasBaseException {
        AtlasEntityDef entL0   = new AtlasEntityDef("L0");
        AtlasEntityDef entL1_1 = new AtlasEntityDef("L1-1");
        AtlasEntityDef entL1_2 = new AtlasEntityDef("L1-2");
        AtlasEntityDef entL2_1 = new AtlasEntityDef("L2-1");
        AtlasEntityDef entL2_2 = new AtlasEntityDef("L2-2");
        AtlasEntityDef entL2_3 = new AtlasEntityDef("L2-3");
        AtlasEntityDef entL2_4 = new AtlasEntityDef("L2-4");

        entL1_1.addSuperType(entL0.getName());
        entL1_2.addSuperType(entL0.getName());
        entL2_1.addSuperType(entL1_1.getName());
        entL2_2.addSuperType(entL1_1.getName());
        entL2_3.addSuperType(entL1_1.getName());
        entL2_3.addSuperType(entL1_2.getName());
        entL2_4.addSuperType(entL1_2.getName());
        entL0.addSuperType(entL2_3.getName()); // circular-ref

        AtlasTypesDef typesDef = new AtlasTypesDef();

        typesDef.getEntityDefs().add(entL0);
        typesDef.getEntityDefs().add(entL1_1);
        typesDef.getEntityDefs().add(entL1_2);
        typesDef.getEntityDefs().add(entL2_1);
        typesDef.getEntityDefs().add(entL2_2);
        typesDef.getEntityDefs().add(entL2_3);
        typesDef.getEntityDefs().add(entL2_4);

        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        String                     failureMsg   = null;

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(typesDef);

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNotNull(failureMsg, "expected invalid supertype failure");
    }

    @Test
    public void testNestedUpdates() throws AtlasBaseException {
        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        String                     failureMsg   = null;
        AtlasClassificationDef     testTag1     = new AtlasClassificationDef("testTag1");
        AtlasClassificationDef     testTag2     = new AtlasClassificationDef("testTag2");

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addType(testTag1);

            // changes should not be seen in typeRegistry until lock is released
            assertFalse(typeRegistry.isRegisteredType(testTag1.getName()),
                        "type added should be seen in typeRegistry only after commit");

            boolean isNestedUpdateSuccess = addType(typeRegistry, testTag2);

            assertTrue(isNestedUpdateSuccess);

            // changes made in nested commit, inside addType(), should not be seen in typeRegistry until lock is released here
            assertFalse(typeRegistry.isRegisteredType(testTag2.getName()),
                        "type added within nested commit should be seen in typeRegistry only after outer commit");

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNull(failureMsg);
        assertTrue(typeRegistry.isRegisteredType(testTag1.getName()));
        assertTrue(typeRegistry.isRegisteredType(testTag2.getName()));
    }

    @Test
    public void testParallelUpdates() throws AtlasBaseException {
        final int    numOfThreads         =  3;
        final int    numOfTypesPerKind    = 30;
        final String enumTypePrefix       = "testEnum-";
        final String structTypePrefix     = "testStruct-";
        final String classificationPrefix = "testTag-";
        final String entityTypePrefix     = "testEntity-";

        ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);

        final AtlasTypeRegistry typeRegistry = new AtlasTypeRegistry();

        // update typeRegistry simultaneously in multiple threads
        for (int threadIdx = 0; threadIdx < numOfThreads; threadIdx++) {
            executor.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    for (int i = 0; i < numOfTypesPerKind; i++) {
                        addType(typeRegistry, new AtlasEnumDef(enumTypePrefix + i));
                    }

                    for (int i = 0; i < numOfTypesPerKind; i++) {
                        addType(typeRegistry, new AtlasStructDef(structTypePrefix + i));
                    }

                    for (int i = 0; i < numOfTypesPerKind; i++) {
                        addType(typeRegistry, new AtlasClassificationDef(classificationPrefix + i));
                    }

                    for (int i = 0; i < numOfTypesPerKind; i++) {
                        addType(typeRegistry, new AtlasEntityDef(entityTypePrefix + i));
                    }

                    return null;
                }
            });
        }

        executor.shutdown();

        try {
            boolean isCompleted = executor.awaitTermination(60, TimeUnit.SECONDS);

            assertTrue(isCompleted, "threads did not complete updating types");
        } catch (InterruptedException excp) {
            // ignore?
        }

        // verify that all types added are present in the typeRegistry
        for (int i = 0; i < numOfTypesPerKind; i++) {
            String enumType           = enumTypePrefix + i;
            String structType         = structTypePrefix + i;
            String classificationType = classificationPrefix + i;
            String entityType         = entityTypePrefix + i;

            assertNotNull(typeRegistry.getEnumDefByName(enumType), enumType + ": enum not found");
            assertNotNull(typeRegistry.getStructDefByName(structType), structType + ": struct not found");
            assertNotNull(typeRegistry.getClassificationDefByName(classificationType), classificationType + ": classification not found");
            assertNotNull(typeRegistry.getEntityDefByName(entityType), entityType + ": entity not found");
        }
    }

    /* create 2 entity types: L0 and L1, with L0 as superType of L1
     * add entity type L2, with L0, L1 and L2 as super-types - this should fail due to L2 self-referencing itself in super-types
     * verify that after the update failure, the registry still has correct super-type/sub-type information for L0 and L1
     */
    @Test
    public void testRegistryValidityOnInvalidUpdate() throws AtlasBaseException {
        AtlasEntityDef entL0 = new AtlasEntityDef("L0");
        AtlasEntityDef entL1 = new AtlasEntityDef("L1");

        entL1.addSuperType(entL0.getName());

        entL0.addAttribute(new AtlasAttributeDef("L0_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        entL1.addAttribute(new AtlasAttributeDef("L1_a1", AtlasBaseTypeDef.ATLAS_TYPE_INT));

        AtlasTypesDef typesDef = new AtlasTypesDef();

        typesDef.getEntityDefs().add(entL0);
        typesDef.getEntityDefs().add(entL1);

        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        String                     failureMsg   = null;

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(typesDef);

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNull(failureMsg);

        validateAllSuperTypes(typeRegistry, "L0", new HashSet<String>());
        validateAllSubTypes(typeRegistry, "L0", new HashSet<>(Arrays.asList("L1")));

        validateAllSuperTypes(typeRegistry, "L1", new HashSet<>(Arrays.asList("L0")));
        validateAllSubTypes(typeRegistry, "L1", new HashSet<String>());


        // create a circular reference
        AtlasEntityDef entL2 = new AtlasEntityDef("L2");
        entL2.addSuperType(entL0.getName());
        entL2.addSuperType(entL1.getName());
        entL2.addSuperType(entL2.getName());

        typesDef.clear();
        typesDef.getEntityDefs().add(entL2);

        try {
            commit = false;

            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.updateTypes(typesDef);

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNotNull(failureMsg);

        assertNull(typeRegistry.getEntityTypeByName("L2"));

        validateAllSuperTypes(typeRegistry, "L0", new HashSet<String>());
        validateAllSubTypes(typeRegistry, "L0", new HashSet<>(Arrays.asList("L1")));

        validateAllSuperTypes(typeRegistry, "L1", new HashSet<>(Arrays.asList("L0")));
        validateAllSubTypes(typeRegistry, "L1", new HashSet<String>());
    }

    private boolean addType(AtlasTypeRegistry typeRegistry, AtlasBaseTypeDef typeDef) {
        boolean                    ret = false;
        AtlasTransientTypeRegistry ttr = null;

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addType(typeDef);

            ret = true;
        } catch (AtlasBaseException excp) {
            // ignore
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, ret);
        }

        return ret;
    }

    private void validateAllSuperTypes(AtlasTypeRegistry typeRegistry, String typeName, Set<String> expectedSuperTypes) {
        AtlasType type = null;

        try {
            type = typeRegistry.getType(typeName);
        } catch (AtlasBaseException excp) {
        }

        Set<String> superTypes = null;

        if (type != null) {
            if (type instanceof AtlasEntityType) {
                superTypes = ((AtlasEntityType) type).getAllSuperTypes();
            } else if (type instanceof AtlasClassificationType) {
                superTypes = ((AtlasClassificationType) type).getAllSuperTypes();
            }
        }

        assertEquals(superTypes, expectedSuperTypes);
    }

    private void validateAllSubTypes(AtlasTypeRegistry typeRegistry, String typeName, Set<String> expectedSubTypes) {
        AtlasType type = null;

        try {
            type = typeRegistry.getType(typeName);
        } catch (AtlasBaseException excp) {
        }

        Set<String> subTypes = null;

        if (type != null) {
            if (type instanceof AtlasEntityType) {
                subTypes = ((AtlasEntityType) type).getAllSubTypes();
            } else if (type instanceof AtlasClassificationType) {
                subTypes = ((AtlasClassificationType) type).getAllSubTypes();
            }
        }

        assertEquals(subTypes, expectedSubTypes);
    }

    private void validateSubTypes(AtlasTypeRegistry typeRegistry, String typeName, Set<String> expectedSubTypes) {
        AtlasType type = null;

        try {
            type = typeRegistry.getType(typeName);
        } catch (AtlasBaseException excp) {
        }

        Set<String> subTypes = null;

        if (type != null) {
            if (type instanceof AtlasEntityType) {
                subTypes = ((AtlasEntityType) type).getSubTypes();
            } else if (type instanceof AtlasClassificationType) {
                subTypes = ((AtlasClassificationType) type).getSubTypes();
            }
        }

        assertEquals(subTypes, expectedSubTypes);
    }

    private void validateAttributeNames(AtlasTypeRegistry typeRegistry, String typeName, Set<String> attributeNames) {
        AtlasType type = null;

        try {
            type = typeRegistry.getType(typeName);
        } catch (AtlasBaseException excp) {
        }

        Map<String, AtlasStructType.AtlasAttribute> attributes = null;

        if (type != null) {
            if (type instanceof AtlasEntityType) {
                attributes = ((AtlasEntityType) type).getAllAttributes();
            } else if (type instanceof AtlasClassificationType) {
                attributes = ((AtlasClassificationType) type).getAllAttributes();
            }
        }

        assertNotNull(attributes);
        assertEquals(attributes.keySet(), attributeNames);
    }

    private void validateDisplayNameAttribute(AtlasTypeRegistry typeRegistry, String entityTypeName, String... displayNameAttributes) {
        AtlasEntityType entityType = typeRegistry.getEntityTypeByName(entityTypeName);

        if (displayNameAttributes == null || displayNameAttributes.length == 0) {
            assertNull(entityType.getDisplayTextAttribute());
        } else {
            List<String> validValues = Arrays.asList(displayNameAttributes);

            assertTrue(validValues.contains(entityType.getDisplayTextAttribute()), entityTypeName + ": invalid displayNameAttribute " + entityType.getDisplayTextAttribute() + ". Valid values: " + validValues);
        }
    }
}
