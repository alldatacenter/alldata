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

}
