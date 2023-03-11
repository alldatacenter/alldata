package com.platform.backend.controller;

import com.platform.backend.entity.User;
import com.platform.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/login", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ModelMap login(@RequestParam("username") String username, @RequestParam("password") String password) {
        ModelMap model = new ModelMap();
        // 查询用户数据
        User user = new User(username, password);
        ModelMap query = userService.login(user);
        if(Boolean.parseBoolean(query.get("result").toString())) {
            model.addAttribute("success", true);
            model.addAttribute("user", query.get("user"));
        } else {
            model.addAttribute("success", false);
            model.addAttribute("msg", query.get("msg"));
        }
        return model;
    }

    @RequestMapping(value = "/register", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ModelMap register(@RequestParam("username") String username, @RequestParam("password") String password) {
        ModelMap model = new ModelMap();
        // 查询用户数据
        User user = new User(username, password);
        User user2 = userService.findByName(username);
        if(user2 != null) {
            model.addAttribute("success", false);
            model.addAttribute("msg", "用户已存在");
        } else {
            model.addAttribute("success", true);
            user.setTimestamp(System.currentTimeMillis());
            User res = userService.add(user);
            model.addAttribute("user", res);
            model.addAttribute("msg", "注册成功");
        }
        return model;
    }
}
