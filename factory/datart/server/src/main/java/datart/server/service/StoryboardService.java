package datart.server.service;

import datart.core.entity.Storyboard;
import datart.core.mappers.ext.StoryboardMapperExt;
import datart.server.base.dto.StoryboardDetail;
import datart.server.base.params.StoryboardBaseUpdateParam;

import java.util.List;

public interface StoryboardService extends VizCRUDService<Storyboard, StoryboardMapperExt> {

    List<Storyboard> listStoryBoards(String orgId);

    StoryboardDetail getStoryboard(String storyboardId);

    boolean updateBase(StoryboardBaseUpdateParam updateParam);

    boolean unarchive(String id, String newName, String parentId, double index);

    boolean checkUnique(String orgId, String parentId, String name);

}
