package datart.core.common;

public interface Cache {

    void put(String key, Object object);

    void put(String key, Object object, int ttl);

    boolean delete(String key);

    <T> T get(String key);

}
