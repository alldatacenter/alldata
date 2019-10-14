package com.platform.pc.search.mapper;

import com.platform.pc.manager.dto.front.SearchItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ItemMapper {

    List<SearchItem> getItemList();

    SearchItem getItemById(@Param("id") Long id);

}
