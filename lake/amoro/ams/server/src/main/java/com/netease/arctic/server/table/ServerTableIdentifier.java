package com.netease.arctic.server.table;

import com.netease.arctic.ams.api.TableIdentifier;

import java.util.Objects;

public class ServerTableIdentifier {

  private Long id;
  private String catalog;
  private String database;
  private String tableName;

  //used by the MyBatis framework.
  private ServerTableIdentifier() {
  }

  private ServerTableIdentifier(TableIdentifier tableIdentifier) {
    this.catalog = tableIdentifier.getCatalog();
    this.database = tableIdentifier.getDatabase();
    this.tableName = tableIdentifier.getTableName();
  }

  private ServerTableIdentifier(String catalog, String database, String tableName) {
    this.catalog = catalog;
    this.database = database;
    this.tableName = tableName;
  }

  private ServerTableIdentifier(Long id, String catalog, String database, String tableName) {
    this.id = id;
    this.catalog = catalog;
    this.database = database;
    this.tableName = tableName;
  }

  public Long getId() {
    return id;
  }

  public String getCatalog() {
    return catalog;
  }

  public String getDatabase() {
    return database;
  }

  public String getTableName() {
    return tableName;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setCatalog(String catalog) {
    this.catalog = catalog;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerTableIdentifier that = (ServerTableIdentifier) o;
    return Objects.equals(id, that.id) && Objects.equals(catalog, that.catalog) &&
        Objects.equals(database, that.database) && Objects.equals(tableName, that.tableName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, catalog, database, tableName);
  }

  @Override
  public String toString() {
    return String.format("%s.%s.%s(tableId=%d)", catalog, database, tableName, id);
  }

  public static ServerTableIdentifier of(TableIdentifier tableIdentifier) {
    return new ServerTableIdentifier(tableIdentifier);
  }

  public static ServerTableIdentifier of(String catalog, String database, String tableName) {
    return new ServerTableIdentifier(catalog, database, tableName);
  }

  public static ServerTableIdentifier of(Long id, String catalog, String database, String tableName) {
    return new ServerTableIdentifier(id, catalog, database, tableName);
  }

  public TableIdentifier getIdentifier() {
    return new TableIdentifier(catalog, database, tableName);
  }

}
