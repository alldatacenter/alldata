package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;


/**
 * Represents the configuration for an Espresso source
 */
public final class EspressoConfig extends SourceConfig {
  private final String _database;
  private final String _table;
  private final String _d2Uri;
  private final String _keyExpr;
  private final String _name;

  public static final String DATABASE = "database";
  public static final String TABLE = "table";
  public static final String D2_URI = "d2Uri";
  public static final String KEY_EXPR = "keyExpr";

  /**
   * Constructor with full parameters
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param database Name of the database
   * @param table Name of the table
   * @param d2Uri D2 URI
   * @param keyExpr key expression
   */
  public EspressoConfig(String sourceName, String database, String table, String d2Uri, String keyExpr) {
    super(sourceName);
    _database = database;
    _table = table;
    _d2Uri = d2Uri;
    _keyExpr = keyExpr;
    _name = database + "/" + table;
  }

  public String getDatabase() {
    return _database;
  }

  public String getTable() {
    return _table;
  }

  public String getD2Uri() {
    return _d2Uri;
  }

  public String getKeyExpr() {
    return _keyExpr;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.ESPRESSO;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    EspressoConfig that = (EspressoConfig) o;
    return Objects.equals(_database, that._database) && Objects.equals(_table, that._table) && Objects.equals(_d2Uri,
        that._d2Uri) && Objects.equals(_keyExpr, that._keyExpr) && Objects.equals(_name, that._name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _database, _table, _d2Uri, _keyExpr, _name);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("EspressoConfig{");
    sb.append("_database='").append(_database).append('\'');
    sb.append(", _table='").append(_table).append('\'');
    sb.append(", _d2Uri='").append(_d2Uri).append('\'');
    sb.append(", _keyExpr=").append(_keyExpr);
    sb.append(", _name='").append(_name).append('\'');
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
