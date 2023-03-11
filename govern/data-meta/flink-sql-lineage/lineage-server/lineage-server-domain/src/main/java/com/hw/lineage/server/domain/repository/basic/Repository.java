package com.hw.lineage.server.domain.repository.basic;

/**
 * @description: Basic Repository interface
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface Repository<T extends Entity, ID extends Identifier> {

    /**
     * Attach an aggregate to a repository to make it traceable
     */
    default void attach(T entity) {
    }

    /**
     * Detach an aggregate
     */
    default void detach(T entity) {
    }

    T find(ID id);

    T save(T entity);

    void remove(ID id);

    default boolean check(String name) {
        return false;
    }
}
