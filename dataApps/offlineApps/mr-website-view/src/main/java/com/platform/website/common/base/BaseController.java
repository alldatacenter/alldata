package com.platform.website.common.base;

import com.platform.website.common.MyException;
import com.platform.website.utils.StringUtil;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class BaseController {
	
	private static Logger logger=LoggerFactory.getLogger(BaseController.class);
	 /**
     * 获取用户ID，用户ID可能为NULL,需自行判断
     * @param request
     * @return
     */
    protected Integer getUserId(HttpServletRequest request){

        String sId = request.getHeader("userId");

        if(!StringUtil.isEmpty(sId)){
            try {
                Integer userId = StringUtil.toInt(sId);
                return userId;
            } catch (NumberFormatException e) {
                logger.warn("请求头userId参数格式错误:{}",sId);
            }

        }

        return null;
    }

	 /**
     * 获取用户ID,当userId为空的时候抛出异常
     * @param request
     * @return
     * @throws MyException 用户ID不能为空
     */
    protected Integer getNotNullUserId(HttpServletRequest request) throws MyException {
        Integer userId = getUserId(request);
        if(userId == null){
            throw new MyException("用户ID不能为空");
        }
        return userId;
    }
}

