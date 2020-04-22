package com.platform.website.controller;

import com.platform.website.common.base.BaseController;
import com.platform.website.service.IUserService;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UserController extends BaseController {
	@Autowired
	private IUserService userService;

	@RequestMapping(value = "/hello", method = RequestMethod.POST)
	public String getByController(String id) {
		return "hello";
	}

	@RequestMapping(value = "/user", method = RequestMethod.POST)
	public ModelAndView getTeemo(Integer id) {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("userName", userService.getOneUser(id)
				.getName());
		modelAndView.addObject("userAge", userService.getOneUser(id)
				.getAge());
		modelAndView.setViewName("user");
		return modelAndView;
	}

	@RequestMapping(value = "test/json", method = RequestMethod.GET)
	@ResponseBody
	public Object test3(){
		Map<String, Integer> map = new HashMap<String,Integer>();
		map.put("周一", 15);
		map.put("周二", 18);
		map.put("周三", 10);
		map.put("周四", 20);
		map.put("周五", 24);
		map.put("周六", 15);
		map.put("周日", 5);
		return map;
	}

}
