package com.datasophon.api.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.NoticeGroupUserMapper;
import com.datasophon.dao.entity.NoticeGroupUserEntity;
import com.datasophon.api.service.NoticeGroupUserService;


@Service("noticeGroupUserService")
public class NoticeGroupUserServiceImpl extends ServiceImpl<NoticeGroupUserMapper, NoticeGroupUserEntity> implements NoticeGroupUserService {


}
