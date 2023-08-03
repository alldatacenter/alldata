package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.feathr.offline.TestFeathr
import org.testng.Assert.assertTrue
import org.testng.annotations.Test

/**
 * unit tests for DataLoaderFactory
 */
class TestDataLoaderFactory extends TestFeathr {

  @Test(description = "test create different DataLoader with LocalDataLoaderFactory")
  def testLocalDataLoaderFactory() : Unit = {
    val localDataLoaderFactory = new LocalDataLoaderFactory(ss, dataLoaderHandlers=List())
    val csvLoader = localDataLoaderFactory.create("anchor1-source.csv")
    assertTrue(csvLoader.isInstanceOf[CsvDataLoader])
    val sqlContext = ss.sqlContext
    sqlContext.setConf("spark.feathr.inputFormat.csvOptions.sep", "\t")
    val csvLoaderWithDelimiter = localDataLoaderFactory.create("anchor1-source.tsv")
    assertTrue(csvLoaderWithDelimiter.isInstanceOf[BatchDataLoader])
    sqlContext.setConf("spark.feathr.inputFormat.csvOptions.sep", "")
    val avroJsonLoader = localDataLoaderFactory.create("anchor5-source.avro.json")
    assertTrue(avroJsonLoader.isInstanceOf[AvroJsonDataLoader])
    val jsonWithSchemaLoader = localDataLoaderFactory.create("simple-obs2") // the mock data folder exists.
    assertTrue(jsonWithSchemaLoader.isInstanceOf[JsonWithSchemaDataLoader])
  }
}