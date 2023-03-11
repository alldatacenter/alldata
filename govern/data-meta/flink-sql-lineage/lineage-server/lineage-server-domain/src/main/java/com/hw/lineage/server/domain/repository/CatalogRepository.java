package com.hw.lineage.server.domain.repository;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.domain.entity.Catalog;
import com.hw.lineage.server.domain.query.catalog.CatalogEntry;
import com.hw.lineage.server.domain.query.catalog.CatalogQuery;
import com.hw.lineage.server.domain.repository.basic.Repository;
import com.hw.lineage.server.domain.vo.CatalogId;

/**
 * @description: CatalogRepository
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface CatalogRepository extends Repository<Catalog, CatalogId> {
    PageInfo<Catalog> findAll(CatalogQuery catalogQuery);

    void setDefault(CatalogId catalogId);

    CatalogEntry findEntry(CatalogId catalogId);
}

