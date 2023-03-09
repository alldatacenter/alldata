package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.feathr.offline.TestFeathr
import org.testng.annotations.{BeforeClass, Test}
import com.linkedin.feathr.offline.source.dataloader.jdbc.SnowflakeDataLoader
import org.testng.Assert.assertEquals

/**
 * unit tests for [[SnowflakeDataLoader]]
 */
class TestSnowflakeDataLoader extends TestFeathr {

  @BeforeClass
  def ssVarSetUp(): Unit = {
    ss.conf.set("sfURL", "snowflake_account")
    ss.conf.set("sfUser", "snowflake_usr")
    ss.conf.set("sfRole", "snowflake_role")
    ss.conf.set("sfWarehouse", "snowflake_warehouse")
    ss.conf.set("sfPassword", "snowflake_password")
  }

  @Test(description = "Test Extract SF Options")
  def testExtractSfOptions() : Unit = {
    val snowflakeUrl = "snowflake://snowflake_account/?sfDatabase=DATABASE&sfSchema=SCHEMA&dbtable=TABLE"
    val dataloader = new SnowflakeDataLoader(ss)
    val actualOptions = dataloader.extractSFOptions(ss, snowflakeUrl)
    val expectedOptions = Map[String, String](
      "sfURL" -> "snowflake_account",
      "sfUser" -> "snowflake_usr",
      "sfRole" -> "snowflake_role",
      "sfWarehouse" -> "snowflake_warehouse",
      "sfPassword" -> "snowflake_password",
      "sfSchema" -> "SCHEMA",
      "sfDatabase" -> "DATABASE",
      "dbtable" -> "TABLE"
    )
    assertEquals(actualOptions, expectedOptions)
  }
}