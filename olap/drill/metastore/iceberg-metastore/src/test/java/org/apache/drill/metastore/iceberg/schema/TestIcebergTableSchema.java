/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore.iceberg.schema;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.MetastoreFieldDefinition;
import org.apache.drill.metastore.iceberg.IcebergBaseTest;
import org.apache.drill.metastore.iceberg.exceptions.IcebergMetastoreException;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.junit.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestIcebergTableSchema extends IcebergBaseTest {

  @Test
  public void testAllTypes() {
    Class<?> clazz = new ClassGenerator(getClass().getSimpleName() + "AllTypes") {

      @Override
      void addFields(ClassWriter classWriter) {
        FieldVisitor stringField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.STORAGE_PLUGIN, String.class);
        annotate(stringField, MetastoreColumn.STORAGE_PLUGIN, MetadataType.ALL);

        FieldVisitor intField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.COLUMN, int.class);
        annotate(intField, MetastoreColumn.COLUMN, MetadataType.TABLE);

        FieldVisitor integerField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.ROW_GROUP_INDEX, Integer.class);
        annotate(integerField, MetastoreColumn.ROW_GROUP_INDEX, MetadataType.SEGMENT);

        FieldVisitor longField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.LAST_MODIFIED_TIME, Long.class);
        annotate(longField, MetastoreColumn.LAST_MODIFIED_TIME, MetadataType.FILE);

        FieldVisitor floatField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.COLUMNS_STATISTICS, Float.class);
        annotate(floatField, MetastoreColumn.COLUMNS_STATISTICS, MetadataType.ALL);

        FieldVisitor doubleField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.ADDITIONAL_METADATA, Double.class);
        annotate(doubleField, MetastoreColumn.ADDITIONAL_METADATA, MetadataType.TABLE);

        FieldVisitor booleanField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.INTERESTING_COLUMNS, Boolean.class);
        annotate(booleanField, MetastoreColumn.INTERESTING_COLUMNS, MetadataType.SEGMENT);

        FieldVisitor listField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.LOCATIONS, List.class, String.class);
        annotate(listField, MetastoreColumn.LOCATIONS, MetadataType.PARTITION);

        FieldVisitor mapField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.HOST_AFFINITY, Map.class, String.class, Float.class);
        annotate(mapField, MetastoreColumn.HOST_AFFINITY, MetadataType.ROW_GROUP);
      }

    }.generate();

    IcebergTableSchema schema = IcebergTableSchema.of(clazz, Collections.emptyList());

    int schemaIndex = IcebergTableSchema.STARTING_SCHEMA_INDEX;
    int complexTypesIndex = IcebergTableSchema.STARTING_COMPLEX_TYPES_INDEX;

    Schema expectedSchema = new Schema(
      Types.NestedField.optional(schemaIndex++, MetastoreColumn.STORAGE_PLUGIN.columnName(), Types.StringType.get()),
      Types.NestedField.optional(schemaIndex++, MetastoreColumn.COLUMN.columnName(), Types.IntegerType.get()),
      Types.NestedField.optional(schemaIndex++, MetastoreColumn.ROW_GROUP_INDEX.columnName(), Types.IntegerType.get()),
      Types.NestedField.optional(schemaIndex++, MetastoreColumn.LAST_MODIFIED_TIME.columnName(), Types.LongType.get()),
      Types.NestedField.optional(schemaIndex++, MetastoreColumn.COLUMNS_STATISTICS.columnName(), Types.FloatType.get()),
      Types.NestedField.optional(schemaIndex++, MetastoreColumn.ADDITIONAL_METADATA.columnName(), Types.DoubleType.get()),
      Types.NestedField.optional(schemaIndex++, MetastoreColumn.INTERESTING_COLUMNS.columnName(), Types.BooleanType.get()),
      Types.NestedField.optional(schemaIndex++, MetastoreColumn.LOCATIONS.columnName(),
        Types.ListType.ofOptional(complexTypesIndex++, Types.StringType.get())),
      Types.NestedField.optional(schemaIndex, MetastoreColumn.HOST_AFFINITY.columnName(),
        Types.MapType.ofOptional(complexTypesIndex++, complexTypesIndex, Types.StringType.get(), Types.FloatType.get())));

    assertEquals(expectedSchema.asStruct(), schema.tableSchema().asStruct());
  }

  @Test
  public void testIgnoreUnannotatedFields() {
    Class<?> clazz = new ClassGenerator(getClass().getSimpleName() + "IgnoreUnannotatedFields") {

      @Override
      void addFields(ClassWriter classWriter) {
        FieldVisitor stringField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.STORAGE_PLUGIN, String.class);
        annotate(stringField, MetastoreColumn.STORAGE_PLUGIN, MetadataType.ALL);

        addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.ADDITIONAL_METADATA, Integer.class);
      }
    }.generate();

    IcebergTableSchema schema = IcebergTableSchema.of(clazz, Collections.emptyList());
    assertNotNull(schema.tableSchema().findField(MetastoreColumn.STORAGE_PLUGIN.columnName()));
    assertNull(schema.tableSchema().findField(MetastoreColumn.ADDITIONAL_METADATA.columnName()));
  }

  @Test
  public void testNestedComplexType() {
    Class<?> clazz = new ClassGenerator(getClass().getSimpleName() + "NestedComplexType") {

      @Override
      void addFields(ClassWriter classWriter) {
        String descriptor = Type.getType(List.class).getDescriptor();

        String signature = FieldSignatureBuilder.builder()
            .declareType(List.class)
            .startGeneric()
                .declareType(List.class)
                .startGeneric()
                    .declareType(String.class)
                    .endType()
                .endGeneric()
                .endType()
            .endGeneric()
            .endType()
            .buildSignature();

        FieldVisitor listField =
            classWriter.visitField(Opcodes.ACC_PRIVATE, MetastoreColumn.ADDITIONAL_METADATA.columnName(), descriptor, signature, null);
        annotate(listField, MetastoreColumn.ADDITIONAL_METADATA, MetadataType.ALL);
      }
    }.generate();

    thrown.expect(IcebergMetastoreException.class);

    IcebergTableSchema.of(clazz, Collections.emptyList());
  }

  @Test
  public void testUnpartitionedPartitionSpec() {
    Class<?> clazz = new ClassGenerator(getClass().getSimpleName() + "UnpartitionedPartitionSpec") {

      @Override
      void addFields(ClassWriter classWriter) {
        FieldVisitor stringField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.STORAGE_PLUGIN, String.class);
        annotate(stringField, MetastoreColumn.STORAGE_PLUGIN, MetadataType.ALL);
      }
    }.generate();

    IcebergTableSchema schema = IcebergTableSchema.of(clazz, Collections.emptyList());
    assertNotNull(schema.tableSchema().findField(MetastoreColumn.STORAGE_PLUGIN.columnName()));

    assertEquals(PartitionSpec.unpartitioned(), schema.partitionSpec());
  }

  @Test
  public void testPartitionedPartitionSpec() {
    Class<?> clazz = new ClassGenerator(getClass().getSimpleName() + "PartitionedPartitionSpec") {

      @Override
      void addFields(ClassWriter classWriter) {
        FieldVisitor partKey1 = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.STORAGE_PLUGIN, String.class);
        annotate(partKey1, MetastoreColumn.STORAGE_PLUGIN, MetadataType.ALL);

        FieldVisitor partKey2 = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.WORKSPACE, String.class);
        annotate(partKey2, MetastoreColumn.WORKSPACE, MetadataType.ALL);

        FieldVisitor partKey3 = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.TABLE_NAME, String.class);
        annotate(partKey3, MetastoreColumn.TABLE_NAME, MetadataType.ALL);

        FieldVisitor integerField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.ROW_GROUP_INDEX, Integer.class);
        annotate(integerField, MetastoreColumn.ROW_GROUP_INDEX, MetadataType.ROW_GROUP);

        FieldVisitor stringField = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.OWNER, Boolean.class);
        annotate(stringField, MetastoreColumn.OWNER, MetadataType.TABLE);
      }
    }.generate();

    IcebergTableSchema schema = IcebergTableSchema.of(clazz,
      Arrays.asList(MetastoreColumn.STORAGE_PLUGIN, MetastoreColumn.WORKSPACE, MetastoreColumn.TABLE_NAME));

    Types.NestedField partKey1 = schema.tableSchema().findField(MetastoreColumn.STORAGE_PLUGIN.columnName());
    assertNotNull(partKey1);

    Types.NestedField partKey2 = schema.tableSchema().findField(MetastoreColumn.WORKSPACE.columnName());
    assertNotNull(partKey2);

    Types.NestedField partKey3 = schema.tableSchema().findField(MetastoreColumn.TABLE_NAME.columnName());
    assertNotNull(partKey3);

    assertNotNull(schema.tableSchema().findField(MetastoreColumn.ROW_GROUP_INDEX.columnName()));
    assertNotNull(schema.tableSchema().findField(MetastoreColumn.OWNER.columnName()));

    Schema partitionSchema = new Schema(partKey1, partKey2, partKey3);
    PartitionSpec expectedPartitionSpec = PartitionSpec.builderFor(partitionSchema)
      .identity(partKey1.name())
      .identity(partKey2.name())
      .identity(partKey3.name())
      .build();

    assertEquals(expectedPartitionSpec, schema.partitionSpec());
  }

  @Test
  public void testUnMatchingPartitionSpec() {
    Class<?> clazz = new ClassGenerator(getClass().getSimpleName() + "UnMatchingPartitionSpec") {

      @Override
      void addFields(ClassWriter classWriter) {
        FieldVisitor storagePlugin = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.STORAGE_PLUGIN, String.class);
        annotate(storagePlugin, MetastoreColumn.STORAGE_PLUGIN, MetadataType.ALL);

        FieldVisitor tableName = addField(classWriter, Opcodes.ACC_PRIVATE, MetastoreColumn.TABLE_NAME, Integer.class);
        annotate(tableName, MetastoreColumn.TABLE_NAME, MetadataType.ALL);
      }
    }.generate();

    thrown.expect(IcebergMetastoreException.class);

    IcebergTableSchema.of(clazz, Arrays.asList(MetastoreColumn.STORAGE_PLUGIN, MetastoreColumn.WORKSPACE));
  }

  /**
   * Generates and loads class at the runtime with specified fields.
   * Fields may or may not be annotated.
   */
  private static abstract class ClassGenerator {

    private final String name;

    ClassGenerator(String name) {
      this.name = name;
    }

    Class<?> generate() {
      ClassWriter classWriter = generateClass();

      byte[] bytes = classWriter.toByteArray();
      return new ClassLoader() {
        public Class<?> injectClass(String name, byte[] classBytes) {
          return defineClass(name, classBytes, 0, classBytes.length);
        }
      }.injectClass(name, bytes);
    }

    public FieldVisitor addField(ClassWriter classWriter, int access, MetastoreColumn column, Class<?> clazz, Class<?>... genericTypes) {
      String descriptor = Type.getType(clazz).getDescriptor();

      String signature = null;

      if (genericTypes.length > 0) {
        FieldSignatureBuilder fieldSignatureBuilder = FieldSignatureBuilder.builder()
            .declareType(clazz)
            .startGeneric();
        for (Class<?> genericType : genericTypes) {
          fieldSignatureBuilder
              .declareType(genericType)
              .endType();
        }
        signature = fieldSignatureBuilder
            .endGeneric()
            .endType()
            .buildSignature();
      }

      return classWriter.visitField(access, column.columnName(), descriptor, signature, null);
    }

    void annotate(FieldVisitor field, MetastoreColumn column, MetadataType... scopes) {
      String annotationDescriptor = Type.getType(MetastoreFieldDefinition.class).getDescriptor();
      AnnotationNode annotationNode = new AnnotationNode(annotationDescriptor);

      annotationNode.visitEnum("column", Type.getType(MetastoreColumn.class).getDescriptor(), column.name());
      annotationNode.visitEnd();

      AnnotationVisitor scopesNode = annotationNode.visitArray("scopes");
      Arrays.stream(scopes)
        .forEach(scope -> scopesNode.visitEnum("scopes", Type.getType(MetadataType.class).getDescriptor(), scope.name()));
      scopesNode.visitEnd();

      AnnotationVisitor annotationVisitor = field.visitAnnotation(annotationDescriptor, true);
      annotationNode.accept(annotationVisitor);
    }

    private ClassWriter generateClass() {
      ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
      classWriter.visit(V1_8, ACC_PUBLIC, name, null, Type.getInternalName(Object.class), null);
      addFields(classWriter);
      classWriter.visitEnd();

      return classWriter;
    }

    abstract void addFields(ClassWriter classWriter);
  }

  /**
   * Helper class for constructing field type signature string.
   * <p>
   * Example of usage:
   * <p>
   * Desired type: {@code List<Map<String, List<Integer>>>}
   * <pre><code>
   *         String signature = FieldSignatureBuilder.builder()
   *           .declareType(List.class)
   *           .startGeneric()
   *               .declareType(Map.class)
   *               .startGeneric()
   *                   .declareType(String.class)
   *                   .endType()
   *                   .declareType(List.class)
   *                   .startGeneric()
   *                       .declareType(Integer.class)
   *                       .endType()
   *                   .endGeneric()
   *                   .endType()
   *               .endGeneric()
   *               .endType()
   *           .endGeneric()
   *           .endType()
   *           .buildSignature();
   * </code></pre>
   */
  private static class FieldSignatureBuilder {
    private final SignatureVisitor signatureVisitor = new SignatureWriter();

    public FieldSignatureBuilder declareType(Class<?> clazz) {
      signatureVisitor.visitClassType(Type.getInternalName(clazz));
      return this;
    }

    public FieldSignatureBuilder startGeneric() {
      signatureVisitor.visitTypeArgument('=');
      return this;
    }

    public FieldSignatureBuilder endGeneric() {
      signatureVisitor.visitSuperclass();
      return this;
    }

    public FieldSignatureBuilder endType() {
      signatureVisitor.visitEnd();
      return this;
    }

    public String buildSignature() {
      return signatureVisitor.toString();
    }

    public static FieldSignatureBuilder builder() {
      return new FieldSignatureBuilder();
    }
  }
}
