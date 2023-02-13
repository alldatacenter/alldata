package cn.datax.service.data.masterdata.utils;

import cn.datax.service.data.masterdata.api.query.Condition;
import cn.datax.service.data.masterdata.api.query.ModelDataQuery;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.List;

public class SearchUtil {

    public static QueryWrapper parseWhereSql(ModelDataQuery modelDataQuery) {
        QueryWrapper queryWrapper = new QueryWrapper();
        List<Condition> conditionList = modelDataQuery.getConditions();
        if(CollUtil.isNotEmpty(conditionList)){
            for(Condition condition : conditionList){
                switch (condition.getQueryType()) {
                    case "eq":
                        queryWrapper.eq(StrUtil.isNotBlank(condition.getValue()), condition.getColumn(), condition.getValue());
                        break;
                    case "ne":
                        queryWrapper.ne(StrUtil.isNotBlank(condition.getValue()), condition.getColumn(), condition.getValue());
                        break;
                    case "like":
                        queryWrapper.like(StrUtil.isNotBlank(condition.getValue()), condition.getColumn(), condition.getValue());
                        break;
                    case "gt":
                        queryWrapper.gt(StrUtil.isNotBlank(condition.getValue()), condition.getColumn(), condition.getValue());
                        break;
                    case "ge":
                        queryWrapper.ge(StrUtil.isNotBlank(condition.getValue()), condition.getColumn(), condition.getValue());
                        break;
                    case "lt":
                        queryWrapper.lt(StrUtil.isNotBlank(condition.getValue()), condition.getColumn(), condition.getValue());
                        break;
                    case "le":
                        queryWrapper.le(StrUtil.isNotBlank(condition.getValue()), condition.getColumn(), condition.getValue());
                        break;
                    case "between":
                        queryWrapper.between(StrUtil.isNotBlank(condition.getLeftValue()) && StrUtil.isNotBlank(condition.getRightValue()), condition.getColumn(), condition.getLeftValue(), condition.getRightValue());
                        break;
                    default:
                        break;
                }
            }
        }
        return queryWrapper;
    }
}
