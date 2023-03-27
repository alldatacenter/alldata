package cn.datax.service.system.service;

import cn.datax.service.system.api.dto.PostDto;
import cn.datax.service.system.api.entity.PostEntity;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yuwei
 * @date 2022-09-11
 */
public interface PostService extends BaseService<PostEntity> {

    PostEntity savePost(PostDto post);

    PostEntity updatePost(PostDto post);

    void deletePostById(String id);

    void deletePostBatch(List<String> ids);
}
