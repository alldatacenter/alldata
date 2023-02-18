package com.datasophon.api.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.NoticeGroupUserEntity;
import com.datasophon.api.service.NoticeGroupUserService;
import com.datasophon.common.utils.Result;


/**
 * 通知组-用户中间表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@RestController
@RequestMapping("api/notice/group/user")
public class NoticeGroupUserController {
    @Autowired
    private NoticeGroupUserService noticeGroupUserService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(@RequestParam Map<String, Object> params) {


        return Result.success();
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id) {
        NoticeGroupUserEntity noticeGroupUser = noticeGroupUserService.getById(id);

        return Result.success().put("noticeGroupUser", noticeGroupUser);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody NoticeGroupUserEntity noticeGroupUser) {
        noticeGroupUserService.save(noticeGroupUser);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody NoticeGroupUserEntity noticeGroupUser) {
        noticeGroupUserService.updateById(noticeGroupUser);

        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids) {
        noticeGroupUserService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
