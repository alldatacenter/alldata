package cn.datax.common.mapstruct;

import java.util.List;

/**
 *
 * Mapper文件基类
 * 更多的用法需自行实现
 * @param <D> 目标对象，一般为DTO对象
 * @param <E> 源对象，一般为需要转换的对象
 * @param <V> 目标对象，一般为VO对象
 */
public interface EntityMapper<D, E, V> {

    /**
     * 将源对象转换为DTO对象
     * @param e
     * @return D
     */
    D toDTO(E e);

    /**
     * 将源对象集合转换为DTO对象集合
     * @param es
     * @return List<D>
     */
    List<D> toDTO(List<E> es);

    /**
     * 将源对象转换为VO对象
     * @param e
     * @return D
     */
    V toVO(E e);

    /**
     * 将源对象集合转换为VO对象集合
     * @param es
     * @return List<D>
     */
    List<V> toVO(List<E> es);

    /**
     * 将目标对象转换为源对象
     * @param d
     * @return E
     */
    E toEntity(D d);

    /**
     * 将目标对象集合转换为源对象集合
     * @param ds
     * @return List<E>
     */
    List<E> toEntity(List<D> ds);
}
