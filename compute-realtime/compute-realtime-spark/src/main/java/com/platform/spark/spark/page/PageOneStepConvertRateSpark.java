package com.platform.spark.spark.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.platform.spark.constant.Constants;
import com.platform.spark.dao.ITaskDAO;
import com.platform.spark.dao.factory.DAOFactory;
import com.platform.spark.domain.PageSplitConvertRate;
import com.platform.spark.util.SparkUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;

import scala.Tuple2;

import com.alibaba.fastjson.JSONObject;
import com.platform.spark.dao.IPageSplitConvertRateDAO;
import com.platform.spark.domain.Task;
import com.platform.spark.util.DateUtils;
import com.platform.spark.util.NumberUtils;
import com.platform.spark.util.ParamUtils;

/**
 * 页面单跳转化率模块spark作业
 * @author wulinhao
 *
 */
public class PageOneStepConvertRateSpark {
	
	public static void main(String[] args) {
		// 1、构造Spark上下文
		SparkConf conf = new SparkConf()
				.setAppName(Constants.SPARK_APP_NAME_PAGE);
		SparkUtils.setMaster(conf);
		
		JavaSparkContext sc = new JavaSparkContext(conf);
		SQLContext sqlContext = SparkUtils.getSQLContext(sc.sc());
		
		// 2、生成模拟数据
		SparkUtils.mockData(sc, sqlContext);  
		
		// 3、查询任务，获取任务的参数
		Long taskid = ParamUtils.getTaskIdFromArgs(args, Constants.SPARK_LOCAL_TASKID_PAGE);
		
		ITaskDAO taskDAO = DAOFactory.getTaskDAO();
		Task task = taskDAO.findById(taskid);
		if(task == null) {
			System.out.println(new Date() + ": cannot find this task with id [" + taskid + "].");  
			return;
		}
		
		JSONObject taskParam = JSONObject.parseObject(task.getTaskParam());
		
		// 4、查询指定日期范围内的用户访问行为数据
		JavaRDD<Row> actionRDD = SparkUtils.getActionRDDByDateRange(
				sqlContext, taskParam);
		
		// 对用户访问行为数据做一个映射，将其映射为<sessionid,访问行为>的格式
		// 咱们的用户访问页面切片的生成，是要基于每个session的访问数据，来进行生成的
		// 脱离了session，生成的页面访问切片，是没有意义的
		// 举例，比如用户A，访问了页面3和页面5
		// 用于B，访问了页面4和页面6
		// 漏了一个前提，使用者指定的页面流筛选条件，比如页面3->页面4->页面7
		// 你能不能说，是将页面3->页面4，串起来，作为一个页面切片，来进行统计呢
		// 当然不行
		// 所以说呢，页面切片的生成，肯定是要基于用户session粒度的
		
		JavaPairRDD<String, Row> sessionid2actionRDD = getSessionid2actionRDD(actionRDD);
		sessionid2actionRDD = sessionid2actionRDD.cache(); // persist(StorageLevel.MEMORY_ONLY)
		
		// 对<sessionid,访问行为> RDD，做一次groupByKey操作
		// 因为我们要拿到每个session对应的访问行为数据，才能够去生成切片
		JavaPairRDD<String, Iterable<Row>> sessionid2actionsRDD = sessionid2actionRDD.groupByKey();
		
		// 最核心的一步，每个session的单跳页面切片的生成，以及页面流的匹配，算法
		JavaPairRDD<String, Integer> pageSplitRDD = generateAndMatchPageSplit(
				sc, sessionid2actionsRDD, taskParam);
		Map<String, Long> pageSplitPvMap = pageSplitRDD.countByKey();

		// 使用者指定的页面流是3,2,5,8,6
		// 咱们现在拿到的这个pageSplitPvMap，3->2，2->5，5->8，8->6
		Long startPagePv = getStartPagePv(taskParam, sessionid2actionsRDD);
		
		// 计算目标页面流的各个页面切片的转化率
		Map<String, Double> convertRateMap = computePageSplitConvertRate(
				taskParam, pageSplitPvMap, startPagePv);
		
		// 持久化页面切片转化率
		persistConvertRate(taskid, convertRateMap);  
	}
	
	/**
	 * 获取<sessionid,用户访问行为>格式的数据
	 * @param actionRDD 用户访问行为RDD
	 * @return <sessionid,用户访问行为>格式的数据
	 */
	private static JavaPairRDD<String, Row> getSessionid2actionRDD(
			JavaRDD<Row> actionRDD) {
		return actionRDD.mapToPair(new PairFunction<Row, String, Row>() {

			private  final Long serialVersionUID = 1L;

			@Override
			public Tuple2<String, Row> call(Row row) throws Exception {
				String sessionid = row.getString(2);
				return new Tuple2<String, Row>(sessionid, row);   
			}
			
		});
	}
	
	/**
	 * 页面切片生成与匹配算法
	 * @param sc 
	 * @param sessionid2actionsRDD
	 * @param taskParam
	 * @return
	 */
	private static JavaPairRDD<String, Integer> generateAndMatchPageSplit(
			JavaSparkContext sc,
			JavaPairRDD<String, Iterable<Row>> sessionid2actionsRDD,
			JSONObject taskParam) {
		String targetPageFlow = ParamUtils.getParam(taskParam, Constants.PARAM_TARGET_PAGE_FLOW);
		final Broadcast<String> targetPageFlowBroadcast = sc.broadcast(targetPageFlow);
		
		return sessionid2actionsRDD.flatMapToPair(
				
				new PairFlatMapFunction<Tuple2<String,Iterable<Row>>, String, Integer>() {

					private  final Long serialVersionUID = 1L;

					@Override
					public Iterator<Tuple2<String, Integer>> call(
							Tuple2<String, Iterable<Row>> tuple)
							throws Exception {
						// 定义返回list
						List<Tuple2<String, Integer>> list = 
								new ArrayList<Tuple2<String, Integer>>();
						// 获取到当前session的访问行为的迭代器
						Iterator<Row> iterator = tuple._2.iterator();
						// 获取使用者指定的页面流
						// 使用者指定的页面流，1,2,3,4,5,6,7
						// 1->2的转化率是多少？2->3的转化率是多少？
						String[] targetPages = targetPageFlowBroadcast.value().split(",");  
						
						// 这里，我们拿到的session的访问行为，默认情况下是乱序的
						// 比如说，正常情况下，我们希望拿到的数据，是按照时间顺序排序的
						// 但是问题是，默认是不排序的
						// 所以，我们第一件事情，对session的访问行为数据按照时间进行排序
						
						// 举例，反例
						// 比如，3->5->4->10->7
						// 3->4->5->7->10
						// 排序
						
						List<Row> rows = new ArrayList<Row>();
						while(iterator.hasNext()) {
							rows.add(iterator.next());  
						}
						
						Collections.sort(rows, new Comparator<Row>() {
							
							@Override
							public int compare(Row o1, Row o2) {
								String actionTime1 = o1.getString(4);
								String actionTime2 = o2.getString(4);
								
								Date date1 = DateUtils.parseTime(actionTime1);
								Date date2 = DateUtils.parseTime(actionTime2);
								
								return (int)(date1.getTime() - date2.getTime());
							}
							
						});
						
						// 页面切片的生成，以及页面流的匹配
						Long lastPageId = null;
						
						for(Row row : rows) {
							Long pageid = row.getLong(3);
							
							if(lastPageId == null) {
								lastPageId = pageid;
								continue;
							}
							
							// 生成一个页面切片
							// 3,5,2,1,8,9
							// lastPageId=3
							// 5，切片，3_5
							
							String pageSplit = lastPageId + "_" + pageid;
							
							// 对这个切片判断一下，是否在用户指定的页面流中
							for(int i = 1; i < targetPages.length; i++) {
								// 比如说，用户指定的页面流是3,2,5,8,1
								// 遍历的时候，从索引1开始，就是从第二个页面开始
								// 3_2
								String targetPageSplit = targetPages[i - 1] + "_" + targetPages[i];
								
								if(pageSplit.equals(targetPageSplit)) {
									list.add(new Tuple2<String, Integer>(pageSplit, 1));  
									break;
								}
							}
							
							lastPageId = pageid;
						}
						
						return list.iterator();
					}
					
				});
	}
	
	/**
	 * 获取页面流中初始页面的pv
	 * @param taskParam
	 * @param sessionid2actionsRDD
	 * @return
	 */
	private static Long getStartPagePv(JSONObject taskParam,
			JavaPairRDD<String, Iterable<Row>> sessionid2actionsRDD) {
		String targetPageFlow = ParamUtils.getParam(taskParam, 
				Constants.PARAM_TARGET_PAGE_FLOW);
		final Long startPageId = Long.valueOf(targetPageFlow.split(",")[0]);
		
		JavaRDD<Long> startPageRDD = sessionid2actionsRDD.flatMap(
				
				new FlatMapFunction<Tuple2<String,Iterable<Row>>, Long>() {

					private  final Long serialVersionUID = 1L;

					@Override
					public Iterator<Long> call(
							Tuple2<String, Iterable<Row>> tuple)
							throws Exception {
						List<Long> list = new ArrayList<Long>();
						
						Iterator<Row> iterator = tuple._2.iterator();
						
						while(iterator.hasNext()) {
							Row row = iterator.next();
							Long pageid = row.getLong(3);
							
							if(pageid == startPageId) {
								list.add(pageid);
							}
						}
						
						return list.iterator();
					}  
					
				});
		
		return startPageRDD.count();
	}
	
	/**
	 * 计算页面切片转化率
	 * @param pageSplitPvMap 页面切片pv
	 * @param startPagePv 起始页面pv
	 * @return
	 */
	private static Map<String, Double> computePageSplitConvertRate(
			JSONObject taskParam,
			Map<String, Long> pageSplitPvMap,
			Long startPagePv) {
		Map<String, Double> convertRateMap = new HashMap<String, Double>();
		
		String[] targetPages = ParamUtils.getParam(taskParam, 
				Constants.PARAM_TARGET_PAGE_FLOW).split(","); 
		
		Long lastPageSplitPv = 0L;
		
		// 3,5,2,4,6
		// 3_5
		// 3_5 pv / 3 pv
		// 5_2 rate = 5_2 pv / 3_5 pv
		
		// 通过for循环，获取目标页面流中的各个页面切片（pv）
		for(int i = 1; i < targetPages.length; i++) {
			String targetPageSplit = targetPages[i - 1] + "_" + targetPages[i];
			Long targetPageSplitPv = Long.valueOf(String.valueOf(
					pageSplitPvMap.get(targetPageSplit)));
			
			double convertRate = 0.0;
			
			if(i == 1) {
				convertRate = NumberUtils.formatDouble(
						(double)targetPageSplitPv / (double)startPagePv, 2);  
			} else {
				convertRate = NumberUtils.formatDouble(
						(double)targetPageSplitPv / (double)lastPageSplitPv, 2);
			}
			
			convertRateMap.put(targetPageSplit, convertRate);
			
			lastPageSplitPv = targetPageSplitPv;
		}
		
		return convertRateMap;
	}
	
	/**
	 * 持久化转化率
	 * @param convertRateMap
	 */
	private static void persistConvertRate(Long taskid,
			Map<String, Double> convertRateMap) {
		StringBuffer buffer = new StringBuffer("");
		
		for(Map.Entry<String, Double> convertRateEntry : convertRateMap.entrySet()) {
			String pageSplit = convertRateEntry.getKey();
			double convertRate = convertRateEntry.getValue();
			buffer.append(pageSplit + "=" + convertRate + "|");
		}
		
		String convertRate = buffer.toString();
		convertRate = convertRate.substring(0, convertRate.length() - 1);
		
		PageSplitConvertRate pageSplitConvertRate = new PageSplitConvertRate();
		pageSplitConvertRate.setTaskid(taskid); 
		pageSplitConvertRate.setConvertRate(convertRate); 
		
		IPageSplitConvertRateDAO pageSplitConvertRateDAO = DAOFactory.getPageSplitConvertRateDAO();
		pageSplitConvertRateDAO.insert(pageSplitConvertRate); 
	}
	
}
