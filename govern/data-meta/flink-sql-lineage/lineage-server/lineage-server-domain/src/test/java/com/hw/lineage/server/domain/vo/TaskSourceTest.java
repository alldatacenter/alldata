package com.hw.lineage.server.domain.vo;


import com.hw.lineage.common.util.Base64Utils;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @description: TaskSourceTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class TaskSourceTest {

    /**
     * Semicolons enclosed by single quotes
     */
    @Test
    public void testFirstSplitSource() {
        String firstSource = "SELECT * FROM t1;SELECT SPLIT_INDEX(str, ';', 0) FROM t2;SELECT SPLIT_INDEX(str, 'm;2', 0) FROM t3";
        firstSource = Base64Utils.encode(firstSource);
        TaskSource firstTaskSource = new TaskSource(firstSource);
        String[] firstResults = firstTaskSource.splitSource();
        assertThat(firstResults).isNotNull().hasSize(3);
        assertThat(firstResults[0]).isEqualTo("SELECT * FROM t1");
        assertThat(firstResults[1]).isEqualTo("SELECT SPLIT_INDEX(str, ';', 0) FROM t2");
        assertThat(firstResults[2]).isEqualTo("SELECT SPLIT_INDEX(str, 'm;2', 0) FROM t3");
    }

    /**
     * Semicolons enclosed by double quotes
     */
    @Test
    public void testSecondSplitSource() {
        String secondSource = "SELECT * FROM t1;SELECT SPLIT_INDEX(str, \";\", 0) FROM t2;SELECT SPLIT_INDEX(str, \"m;2\", 0) FROM t3";
        secondSource = Base64Utils.encode(secondSource);
        TaskSource secondTaskSource = new TaskSource(secondSource);
        String[] secondResults = secondTaskSource.splitSource();
        assertThat(secondResults).isNotNull().hasSize(3);
        assertThat(secondResults[0]).isEqualTo("SELECT * FROM t1");
        assertThat(secondResults[1]).isEqualTo("SELECT SPLIT_INDEX(str, \";\", 0) FROM t2");
        assertThat(secondResults[2]).isEqualTo("SELECT SPLIT_INDEX(str, \"m;2\", 0) FROM t3");
    }

    /**
     * Semicolons enclosed by both single quotes and double quotes
     */
    @Test
    public void testThirdSplitSource() {
        String thirdSource = "SELECT * FROM t1;SELECT SPLIT_INDEX(str,':', 0) FROM t2;SELECT SPLIT_INDEX(str, \"m;2\", 0) FROM t3";
        thirdSource = Base64Utils.encode(thirdSource);
        TaskSource thirdTaskSource = new TaskSource(thirdSource);
        String[] thirdResults = thirdTaskSource.splitSource();
        assertThat(thirdResults).isNotNull().hasSize(3);
        assertThat(thirdResults[0]).isEqualTo("SELECT * FROM t1");
        assertThat(thirdResults[1]).isEqualTo("SELECT SPLIT_INDEX(str,':', 0) FROM t2");
        assertThat(thirdResults[2]).isEqualTo("SELECT SPLIT_INDEX(str, \"m;2\", 0) FROM t3");
    }

    /**
     * Source has a semicolon at the end
     */
    @Test
    public void testFourthSplitSource() {
        String fourthSource = "SELECT * FROM t1;SELECT SPLIT_INDEX(str,':', 0) FROM t2;";
        fourthSource = Base64Utils.encode(fourthSource);
        TaskSource fourthTaskSource = new TaskSource(fourthSource);
        String[] fourthResults = fourthTaskSource.splitSource();
        assertThat(fourthResults).isNotNull().hasSize(2);
        assertThat(fourthResults[0]).isEqualTo("SELECT * FROM t1");
        assertThat(fourthResults[1]).isEqualTo("SELECT SPLIT_INDEX(str,':', 0) FROM t2");
    }

    /**
     * SQL lines contain multiple semicolons
     */
    @Test
    public void testFifthSplitSource() {
        String fifthSource = "SELECT * FROM t1;;SELECT SPLIT_INDEX(str, ';', 0) FROM t2;;;SELECT SPLIT_INDEX(str, 'm;2', 0) FROM t3";
        fifthSource = Base64Utils.encode(fifthSource);
        TaskSource fifthTaskSource = new TaskSource(fifthSource);
        String[] fifthResults = fifthTaskSource.splitSource();
        assertThat(fifthResults).isNotNull().hasSize(3);
        assertThat(fifthResults[0]).isEqualTo("SELECT * FROM t1");
        assertThat(fifthResults[1]).isEqualTo("SELECT SPLIT_INDEX(str, ';', 0) FROM t2");
        assertThat(fifthResults[2]).isEqualTo("SELECT SPLIT_INDEX(str, 'm;2', 0) FROM t3");
    }

    /**
     * Remove comments and line break
     */
    @Test
    public void testSixthSplitSource() {
        String sixthSource = "-- first\n" +
                "SELECT * FROM t1;\n" +
                "\n" +
                "\n" +
                "SELECT SPLIT_INDEX(str, ';', 0)\n" +
                "FROM t2;  -- second \n" +
                "\n" +
                "--\n" +
                "----third\n" +
                "SELECT SPLIT_INDEX(str, 'm;2', 0) FROM t3";

        sixthSource = Base64Utils.encode(sixthSource);
        TaskSource sixthTaskSource = new TaskSource(sixthSource);
        String[] sixthResults = sixthTaskSource.splitSource();
        assertThat(sixthResults).isNotNull().hasSize(3);
        assertThat(sixthResults[0]).isEqualTo("SELECT * FROM t1");
        assertThat(sixthResults[1]).isEqualTo("SELECT SPLIT_INDEX(str, ';', 0) FROM t2");
        assertThat(sixthResults[2]).isEqualTo("SELECT SPLIT_INDEX(str, 'm;2', 0) FROM t3");
    }
}