package datart.server.service;

import datart.core.entity.Storypage;
import datart.core.mappers.StorypageMapper;

import java.util.List;

public interface StorypageService extends BaseCRUDService<Storypage, StorypageMapper> {

    List<Storypage> listByStoryboard(String storyboardId);

    boolean deleteByStoryboard(String storyboardId);

}