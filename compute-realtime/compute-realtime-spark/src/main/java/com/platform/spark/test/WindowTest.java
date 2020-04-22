package com.platform.spark.test;

import java.util.Vector;

public class WindowTest {

    @SuppressWarnings("unchecked")
//	public static void main(String[] args) {
//		SparkConf conf = new SparkConf()
//				.setMaster("local")
//				.setAppName("WindowTest");
//		JavaSparkContext sc = new JavaSparkContext(conf);
//		HiveContext sqlContext = new HiveContext(sc.sc());
//
//		List<Tuple2<String, Integer>> grades = Arrays.asList(
//				new Tuple2<String, Integer>("class1", 80),
//				new Tuple2<String, Integer>("class1", 75),
//				new Tuple2<String, Integer>("class1", 90),
//				new Tuple2<String, Integer>("class1", 60));
//		JavaPairRDD<String, Integer> gradesRDD = sc.parallelizePairs(grades);
//		JavaRDD<Row> gradeRowsRDD = gradesRDD.map(new Function<Tuple2<String,Integer>, Row>() {
//
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public Row call(Tuple2<String, Integer> tuple) throws Exception {
//				return RowFactory.create(tuple._1, tuple._2);
//			}
//
//		});
//
//		StructType schema = DataTypes.createStructType(Arrays.asList(
//				DataTypes.createStructField("class", DataTypes.StringType, true),
//				DataTypes.createStructField("grade", DataTypes.IntegerType, true)));
//		Dataset<Row> gradesDF = sqlContext.createDataFrame(gradeRowsRDD, schema);
//		gradesDF.registerTempTable("grades");
//
//		Dataset<Row> gradeLevelDF = sqlContext.sql(
//				"SELECT "
//				+ "class,"
//				+ "grade,"
//				+ "row_number() OVER(PARTITION BY class ORDER BY grade DESC) rank "
//				+ "FROM grades");
//
//		gradeLevelDF.show();
//
//		sc.close();
//	}

//    public static void main(String[] args) {
//        byte[] b = null;
//        for (int i = 0; i < 10; i++){
//            b = new byte[1 * 1024 * 1024];
//        }
//    }

    public static void main(String[] args) {
        Vector v = new Vector();
        for (int i = 0; i < 25; i++){
            v.add(new byte[1*1024*1024]);
        }
        System.gc();
        System.out.println("你的电脑cpu数量为："+Runtime.getRuntime().availableProcessors());
    }
}
