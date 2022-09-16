package com.platform.spark.test;

import java.util.Arrays;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

public class CaseWhenTest {
	
	public static void main(String[] args) {
		SparkConf conf = new SparkConf()
				.setMaster("local") 
				.setAppName("CaseWhenTest");
		JavaSparkContext sc = new JavaSparkContext(conf);
		SQLContext sqlContext = new SQLContext(sc.sc());
		
		List<Integer> grades = Arrays.asList(85, 90, 60, 73);
		JavaRDD<Integer> gradesRDD = sc.parallelize(grades);
		JavaRDD<Row> gradeRowsRDD = gradesRDD.map(new Function<Integer, Row>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Row call(Integer grade) throws Exception {
				return RowFactory.create(grade);
			}
			
		});
		
		StructType schema = DataTypes.createStructType(Arrays.asList(
				DataTypes.createStructField("grade", DataTypes.IntegerType, true)));
		Dataset<Row> gradesDF = sqlContext.createDataFrame(gradeRowsRDD, schema);
		gradesDF.registerTempTable("grades");

		Dataset<Row>  gradeLevelDF = sqlContext.sql(
				"SELECT CASE "
					+ "WHEN grade>=90 THEN 'A' "
					+ "WHEN grade>=80 THEN 'B' "
					+ "WHEN grade>=70 THEN 'C' "
					+ "WHEN grade>=60 THEN 'D' "
					+ "ELSE 'E' "
					+ "END gradeLevel "
				+ "FROM grades");
		
		gradeLevelDF.show();
		
		sc.close(); 
	}
	
}
