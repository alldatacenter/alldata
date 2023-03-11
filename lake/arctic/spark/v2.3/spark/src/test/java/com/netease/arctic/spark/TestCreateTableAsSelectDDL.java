package com.netease.arctic.spark;

import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TestCreateTableAsSelectDDL extends SparkTestBase {
    private final String database = "db_def";
    private final String table = "testA";
    private final String sourceTable = "test_table";
    private final TableIdentifier identifier = TableIdentifier.of(catalogName, database, table);

    @Before
    public void prepare() {
        sql("create database if not exists " + database);
        sql("create table {0}.{1} ( \n" +
                " id int , data string, pt string , primary key(id)) using arctic \n" +
                " partitioned by (pt) \n" , database, sourceTable);

        sql("insert overwrite table {0}.{1} values \n" +
                        "( 1, ''aaaa'', ''0001''), \n" +
                        "( 2, ''aaaa'', ''0001''), \n" +
                        "( 3, ''aaaa'', ''0001''), \n" +
                        "( 4, ''aaaa'', ''0001''), \n" +
                        "( 5, ''aaaa'', ''0002''), \n" +
                        "( 6, ''aaaa'', ''0002''), \n" +
                        "( 7, ''aaaa'', ''0002''), \n" +
                        "( 8, ''aaaa'', ''0002'') \n" ,
                database, sourceTable);
    }

    @After
    public void removeTables() {
        sql("DROP TABLE IF EXISTS {0}.{1}", database, sourceTable);
        sql("DROP TABLE IF EXISTS {0}.{1}", database, table);
    }

    @Test
    public void testKeyedUnPartitioned() {
        sql("create table {0}.{1} primary key(id) using arctic AS SELECT * from {2}.{3}",
                database, table, database, sourceTable);
        assertTableExist(identifier);
        Schema expectedSchema = new Schema(
                Types.NestedField.required(1, "id", Types.IntegerType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "pt", Types.StringType.get())
        );
        Assert.assertEquals("Should have expected nullable schema",
                expectedSchema.asStruct(), loadTable(identifier).schema().asStruct());


        Assert.assertEquals("Should be an unpartitioned table",
                0, loadTable(identifier).spec().fields().size());
        assertEquals("Should have rows matching the source table",
                sql("SELECT * FROM {0}.{1} ORDER BY id", database, table),
                sql("SELECT * FROM {0}.{1} ORDER BY id", database, sourceTable));
    }

    @Test
    public void testUnKeyedUnPartitioned() {
        sql("create table {0}.{1} using arctic AS SELECT * from {2}.{3}",
            database, table, database, sourceTable);
        assertTableExist(identifier);
        Schema expectedSchema = new Schema(
            Types.NestedField.required(1, "id", Types.IntegerType.get()),
            Types.NestedField.optional(2, "data", Types.StringType.get()),
            Types.NestedField.optional(3, "pt", Types.StringType.get())
        );
        Assert.assertEquals("Should have expected nullable schema",
            expectedSchema.asStruct(), loadTable(identifier).schema().asStruct());
        Assert.assertEquals("Should be an unpartitioned table",
            0, loadTable(identifier).spec().fields().size());

        assertEquals("Should have rows matching the source table",
            sql("SELECT * FROM {0}.{1} ORDER BY id", database, table),
            sql("SELECT * FROM {0}.{1} ORDER BY id", database, sourceTable));
    }

    @Test
    public void testUnKeyedPartitioned() {
        sql("CREATE TABLE {0}.{1} USING arctic PARTITIONED BY (pt) AS SELECT * FROM {2}.{3} ORDER BY id",
                database, table, database, sourceTable);

        Schema expectedSchema = new Schema(
                Types.NestedField.required(1, "id", Types.IntegerType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "pt", Types.StringType.get())
        );

        PartitionSpec expectedSpec = PartitionSpec.builderFor(expectedSchema)
                .identity("pt")
                .build();


        Assert.assertEquals("Should have expected nullable schema",
                expectedSchema.asStruct(), loadTable(identifier).schema().asStruct());
        Assert.assertEquals("Should be partitioned by id",
                expectedSpec, loadTable(identifier).spec());
        assertEquals("Should have rows matching the source table",
                sql("SELECT * FROM {0}.{1} ORDER BY id", database, table),
                sql("SELECT * FROM {0}.{1} ORDER BY id", database, sourceTable));
    }


    @Test
    public void testKeyedPartitioned() {
        sql("CREATE TABLE {0}.{1} primary key(id) USING arctic PARTITIONED BY (pt)" +
                "AS SELECT * FROM {2}.{3}", database, table, database, sourceTable);

        assertEquals("Should have rows matching the source table",
                sql("SELECT * FROM {0}.{1} ORDER BY id", database, table),
                sql("SELECT * FROM {0}.{1} ORDER BY id", database, sourceTable));

        Schema expectedSchema = new Schema(
                Types.NestedField.required(1, "id", Types.IntegerType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "pt", Types.StringType.get())
        );

        Assert.assertEquals("Should have expected nullable schema",
                expectedSchema.asStruct(), loadTable(identifier).schema().asStruct());

        PartitionSpec expectedSpec = PartitionSpec.builderFor(expectedSchema)
                .identity("pt")
                .build();
        Assert.assertEquals("Should be partitioned by pt",
                expectedSpec, loadTable(identifier).spec());
    }

    @Test
    public void testCTASProperties() {
        sql("CREATE TABLE {0}.{1} USING arctic TBLPROPERTIES (''prop1''=''val1'', ''prop2''=''val2'')" +
            "AS SELECT * FROM {2}.{3}", database, table, database, sourceTable);

        assertEquals("Should have rows matching the source table",
            sql("SELECT * FROM {0}.{1} ORDER BY id", database, table),
            sql("SELECT * FROM {0}.{1} ORDER BY id", database, sourceTable));

        Schema expectedSchema = new Schema(
            Types.NestedField.required(1, "id", Types.IntegerType.get()),
            Types.NestedField.optional(2, "data", Types.StringType.get()),
            Types.NestedField.optional(3, "pt", Types.StringType.get())
        );

        Assert.assertEquals("Should have expected nullable schema",
            expectedSchema.asStruct(), loadTable(identifier).schema().asStruct());

        Assert.assertEquals("Should have updated table property",
            "val1", loadTable(identifier).properties().get("prop1"));
        Assert.assertEquals("Should have preserved table property",
            "val2", loadTable(identifier).properties().get("prop2"));
    }
}
