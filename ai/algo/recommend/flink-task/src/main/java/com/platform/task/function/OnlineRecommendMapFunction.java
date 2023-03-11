package com.platform.schedule.function;

import com.platform.schedule.entity.HbaseClient;
import com.platform.schedule.entity.RedisClient;
import com.platform.schedule.entity.RecommendEntity;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.*;

public class OnlineRecommendMapFunction implements MapFunction<String, String> {

    @Override
    public String map(String s) throws Exception {
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        // 494,235,3,1598247968   userId, productId, score, timestamp
        String[] r = s.split(",");
        String userId = r[0];
        String productId = r[1];
        String score = r[2];
        String timestamp = r[3];
        System.out.println("userId: " + userId + "\tproductId: " + productId);
        HbaseClient.increamColumn("userProduct", userId, "product", productId);

        // 1.先从 redis 查询热点数据：用户历史/最近评分列表， 再插入数据
        Jedis jedis = RedisClient.jedis;
        String REDIS_PREFIX = "ONLINE_PREFIX_";
        jedis.rpush(REDIS_PREFIX + userId, productId + ":" + score);
        List<String> userHistoryRatingList = jedis.lrange(REDIS_PREFIX + userId, 0, -1);
        // 将评分列表转换成 RecommendEntity set，sim 和 score 是相同数据类型，所以这里直接使用 RecommendEntity
        HashSet<RecommendEntity> recentRatingSet = new HashSet<>(userHistoryRatingList.size());
        for (String str : userHistoryRatingList) {
            String[] tmp = str.split(":");
            RecommendEntity recommendEntity = new RecommendEntity(tmp[0], Double.parseDouble(tmp[1]));
            System.out.println("近期评分：\t" + recommendEntity);
            recentRatingSet.add(recommendEntity);
        }

        // 2. 从 hbase 中获取当前产品的相似度列表
        // table：itemCFRecommend  familyName: p rowKey: productId
        List<Map.Entry> entryList = HbaseClient.getRow("itemCFRecommend", productId);
        if(entryList == null){
            return String.format("productId : %s not found in itemCFRecommend", productId);
        }
        // 转换成 dataset 使用
        HashSet<RecommendEntity> recommendEntitySet = new HashSet<>();
        for (Map.Entry<String, Double> entry : entryList) {
            RecommendEntity recommendEntity = new RecommendEntity(entry.getKey(), entry.getValue());
            recommendEntitySet.add(recommendEntity);
            System.out.println(recommendEntity);
        }
        DataSet<RecommendEntity> dataSet = env.fromCollection(recommendEntitySet);

        // 3.从 hbase 中获取用户评论过的产品列表，用于过滤推荐结果
        List<Map.Entry> ratedProductList = HbaseClient.getRow("userProduct", userId);
        List<String> recommendCandidateList = new ArrayList<>();
        if (ratedProductList != null) {
            HashSet<String> historyRatingSet = new HashSet<>();
            for (Map.Entry<String, Double> entry : ratedProductList) {
                System.out.println("已经评分商品：" + entry.getKey());
                historyRatingSet.add(entry.getKey());
            }

            recommendCandidateList = dataSet.filter(new FilterFunction<RecommendEntity>() {
                @Override
                public boolean filter(RecommendEntity recommendEntity) throws Exception {
                    return !historyRatingSet.contains(recommendEntity.getProductId());
                }
            }).sortPartition("sim", Order.DESCENDING)
                    .first(10)
                    .map(new MapFunction<RecommendEntity, String>() {
                        @Override
                        public String map(RecommendEntity recommendEntity) throws Exception {
                            return recommendEntity.getProductId();
                        }
                    }).collect();
        }


        if (recentRatingSet.size() == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("===================\n推荐结果{userId: }")
                    .append(userId)
                    .append("\n")
                    .append("productList: ");
            for (String str : recommendCandidateList) {
                HbaseClient.increamColumn("onlineRecommend", userId, "p", str);
                sb.append(str).append("\t");
            }
            sb.append("\n");
            return sb.toString();
        }

        // 4. 根据用户最近评分产品对推荐列表的产品进行相关度计算并重新评分
        Map<String, Double> resMap = new HashMap<>();
        Integer incre = 1;
        Integer decre = 1;
        for (String candidate : recommendCandidateList) {
            int sum = 0;
            for (RecommendEntity recent : recentRatingSet) {
                Double sim = getSim(candidate, recent.getProductId());
//                sim > 0.4
                if (true) {
                    sum += recent.getSim() * sim;
                    if (recent.getSim() > 3.0) {
                        incre += 1;
                    } else {
                        decre += 1;
                    }
                }
            }
            Double resultScore = sum / recentRatingSet.size() + Math.log(incre) - Math.log(decre);
            resMap.put(candidate, resultScore);
        }

        // 对 resMap 按照 value 进行排序,转换成 list 操作
        List<Map.Entry<String, Double>> reMapEntryList = new ArrayList<>(resMap.entrySet());
        Collections.sort(reMapEntryList, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        // 将结果写入 hbase 表 onlineRecommend 中
        // rowKey ：userId, familyName: p
        StringBuilder sb = new StringBuilder();
        sb.append("===================\n推荐结果{userId: }")
                .append(userId)
                .append("\n")
                .append("productList:\n");
        for (Map.Entry<String, Double> entry : reMapEntryList) {
            HbaseClient.increamColumn("onlineRecommend", userId, "p", entry.getKey());
            sb.append("productId: ").append(entry.getKey()).append("\t")
                    .append("score: ").append(entry.getValue()).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }


    public Double getSim(String prodcut1, String product2) throws IOException {
        List<Map.Entry> res = HbaseClient.getRow("itemCFRecommend", prodcut1);
        if (res == null) return 0.0;
        for (Map.Entry<String, Double> entry : res) {
            if (entry.getKey() == product2) {
                return entry.getValue();
            }
        }
        return 0.0;
    }
}
