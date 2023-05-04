package apache.flink.table.catalog.hive;

import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.table.catalog.hive.HiveCatalog;
import org.apache.hadoop.hive.conf.HiveConf;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 * Test utils for Hive connector.
 *
 * @description: HiveTestUtils
 * @author: HamaWhite
 */
public class HiveTestUtils {
    private static final String HIVE_WAREHOUSE_URI_FORMAT = "jdbc:derby:;databaseName=%s;create=true";

    private static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    public static HiveCatalog createHiveCatalog(String catalogName, String defaultDatabase, String hiveVersion) {
        return new HiveCatalog(
                catalogName,
                defaultDatabase,
                createHiveConf(),
                hiveVersion
                , true);
    }


    public static HiveConf createHiveConf() {
        ClassLoader classLoader = HiveTestUtils.class.getClassLoader();

        try {
            TEMPORARY_FOLDER.create();
            String warehouseDir = TEMPORARY_FOLDER.newFolder().getAbsolutePath() + "/metastore_db";
            String warehouseUri = String.format(HIVE_WAREHOUSE_URI_FORMAT, warehouseDir);

            HiveConf.setHiveSiteLocation(classLoader.getResource(HiveCatalog.HIVE_SITE_FILE));
            HiveConf hiveConf = new HiveConf();
            hiveConf.setVar(
                    HiveConf.ConfVars.METASTOREWAREHOUSE,
                    TEMPORARY_FOLDER.newFolder("hive_warehouse").getAbsolutePath());
            hiveConf.setVar(HiveConf.ConfVars.METASTORECONNECTURLKEY, warehouseUri);
            return hiveConf;
        } catch (IOException e) {
            throw new CatalogException("Failed to create test HiveConf to HiveCatalog.", e);
        }
    }

    public static void deleteTemporaryFolder() {
        TEMPORARY_FOLDER.delete();
    }
}
