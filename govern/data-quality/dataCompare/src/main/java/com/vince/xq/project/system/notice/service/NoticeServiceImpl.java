package com.vince.xq.project.system.notice.service;

import java.util.List;

import com.vince.xq.common.utils.security.ShiroUtils;
import com.vince.xq.common.utils.text.Convert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.vince.xq.project.system.notice.mapper.NoticeMapper;
import com.vince.xq.project.system.notice.domain.Notice;

/**
 * 公告 服务层实现
 * 
 * @author ruoyi
 * @date 2018-06-25
 */
@Service
public class NoticeServiceImpl implements INoticeService
{
    @Autowired
    private NoticeMapper noticeMapper;

    /**
     * 查询公告信息
     * 
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Override
    public Notice selectNoticeById(Long noticeId)
    {
        return noticeMapper.selectNoticeById(noticeId);
    }

    /**
     * 查询公告列表
     * 
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    public List<Notice> selectNoticeList(Notice notice)
    {
        return noticeMapper.selectNoticeList(notice);
    }

    /**
     * 新增公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int insertNotice(Notice notice)
    {
        notice.setCreateBy(ShiroUtils.getLoginName());
        return noticeMapper.insertNotice(notice);
    }

    /**
     * 修改公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int updateNotice(Notice notice)
    {
        notice.setUpdateBy(ShiroUtils.getLoginName());
        return noticeMapper.updateNotice(notice);
    }

    /**
     * 删除公告对象
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteNoticeByIds(String ids)
    {
        return noticeMapper.deleteNoticeByIds(Convert.toStrArray(ids));
    }
}
