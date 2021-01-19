package com.platform.backend.controller;

import com.platform.backend.entity.ProductEntity;
import com.platform.backend.service.RecommendService;
import com.platform.backend.util.CustomKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private String HISTORY_HOT_PRODCUTS = "historyHotProducts";
    private String GOOD_PRODUCTS = "goodProducts";
    private String ITEM_CF_RECOMMEND = "itemCFRecommend";
    private String ONLINE_RECOMMEND = "onlineRecommend";
    private String ONLINE_HOT = "onlineHot";
    private Integer ONLINE_HOT_NUMS = 10;
    @Autowired
    private RecommendService recommendService;

    /**
    * 热门推荐
    * */
    @RequestMapping(value = "/historyhot", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ModelMap getHistoryHotProducts(@RequestParam("num") int num) {
        ModelMap model = new ModelMap();
        List<ProductEntity> recommendations = null;
        try {
            recommendations = recommendService.getHistoryHotOrGoodProducts(num, HISTORY_HOT_PRODCUTS);
            model.addAttribute("success", true);
            model.addAttribute("products", recommendations);
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("msg", e.getMessage());
        }
        StringBuilder sb = new StringBuilder();
        if(recommendations != null) {
            for (ProductEntity product : recommendations) {
                sb.append(product).append(" ");
            }
        } else {
            sb.append("数据为空");
        }
        logger.info(sb.toString());
        return model;
    }

    /**
     * 优质商品推荐
     * @param num
     * @return
     */
    @RequestMapping(value = "/goodproducts", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ModelMap getGoodProducts(@RequestParam("num") int num) {
        ModelMap model = new ModelMap();
        List<ProductEntity> recommendations = null;
        try {
            recommendations = recommendService.getHistoryHotOrGoodProducts(num, GOOD_PRODUCTS);
            model.addAttribute("success", true);
            model.addAttribute("products", recommendations);
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("msg", e.getMessage());
        }
        StringBuilder sb = new StringBuilder();
        if(recommendations != null) {
            for (ProductEntity product : recommendations) {
                sb.append(product).append("\t");
            }
        } else {
            sb.append("数据为空");
        }
        logger.info(sb.toString());
        return model;
    }

    /**
     * 基于物品的推荐
     * @param productId
     * @return
     */
    @RequestMapping(value = "/itemcf/{productId}", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ModelMap getItemCFProducts(@PathVariable("productId") int productId) {
        ModelMap model = new ModelMap();
        List<ProductEntity> recommendatitons = null;
        try {
            recommendatitons = recommendService.getItemCFProducts(productId, ITEM_CF_RECOMMEND);
            model.addAttribute("success", true);
            model.addAttribute("products", recommendatitons);
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("success", false);
            model.addAttribute("msg", e.getMessage());
        }
        return model;
    }

    /**
     * 查询单个商品
     * @param productId
     * @return
     */
    @RequestMapping(value = "/query/{productId}", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ModelMap queryProductInfo(@PathVariable("productId") int productId) {
        ModelMap model = new ModelMap();
        try {
            model.addAttribute("success", true);
            model.addAttribute("products", recommendService.getProductEntity(productId));
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("success", false);
            model.addAttribute("msg", e.getMessage());
        }
        return model;
    }

    /**
     * 模糊查询商品
     * @param sql
     * @return
     */
    @RequestMapping(value = "/search", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ModelMap queryProductInfo(@RequestParam("sql") String sql) {
        ModelMap model = new ModelMap();
        try {
            model.addAttribute("success", true);
            model.addAttribute("products", recommendService.getProductBySql(sql));
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("success", false);
            model.addAttribute("msg", e.getMessage());
        }
        return model;
    }

    /**
     * 将评分数据发送到 kafka 'rating' Topic
     * @param productId
     * @param score
     * @param userId
     * @return
     */
    @RequestMapping(value = "/rate/{productId}", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ModelMap queryProductInfo(@PathVariable("productId") int productId,
                                     @RequestParam("score") Double score, @RequestParam("userId") int userId) {
        ModelMap model = new ModelMap();
        try {
            String msg = userId + "," + productId + "," + score + "," + System.currentTimeMillis() / 1000;
            CustomKafkaProducer.produce(msg);
            System.out.println(msg);
            model.addAttribute("success", true);
            model.addAttribute("message", "完成评分");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("success", false);
            model.addAttribute("msg", e.getMessage());
        }
        return model;
    }


    /**
     * 实时用户个性化推荐
     * @param userId
     * @return
     */
    @RequestMapping(value="/stream", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ModelMap onlineRecs(@RequestParam("userId") String userId) {
        ModelMap model = new ModelMap();
        try {
            List<ProductEntity> res = recommendService.getOnlineRecs(userId, ONLINE_RECOMMEND);
            model.addAttribute("success", true);
            model.addAttribute("products", res);
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("msg", "查询失败");
        }
        return model;
    }

    /**
     * 实时热门推荐
     * @return
     */
    @RequestMapping(value = "/onlinehot", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ModelMap onlineHot() {
        ModelMap model = new ModelMap();
        try {
            List<ProductEntity> res = recommendService.getOnlineHot(ONLINE_HOT, ONLINE_HOT_NUMS);
            model.addAttribute("success", true);
            model.addAttribute("products", res);
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("msg", e.getMessage());
        }
        return model;
    }
}
