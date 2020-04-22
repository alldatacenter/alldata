package com.platform.realtime.view.controller;

import com.platform.realtime.view.common.base.BaseController;
import com.platform.realtime.view.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class UserController extends BaseController {
	@Autowired
	private IUserService userService;

	@RequestMapping(value = "/hello", method = RequestMethod.POST)
	public String getByController(String id) {
		return "hello";
	}
//
//	@RequestMapping(value = "/user", method = RequestMethod.POST)
//	public ModelAndView getTeemo(Integer id) {
//		ModelAndView modelAndView = new ModelAndView();
//		modelAndView.addObject("userName", userService.getOneUser(id)
//				.getName());
//		User user = new User();
//
//		modelAndView.addObject("userAge", userService.getOneUser(id)
//				.getAge());
//		modelAndView.setViewName("user");
//		return modelAndView;
//	}




}
