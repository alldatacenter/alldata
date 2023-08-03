package com.linkedin.feathr.core.config.producer.anchors;

import java.util.Objects;
import java.util.Optional;


/**
 * Some feature datasets may contain feature values as an array of <name, term, vector> tuples. These are
 * typically the result of some aggregation operation. To perform further aggregation on these tuples, for
 * example, rollups from say, daily to weekly, the individual tuples have to be extracted, joined with
 * observation data, and aggregated.
 * <p>
 * The extraction can be performed by using Spark's lateral view in the FROM clause. The lateral view
 * can be used to generate zero or more output rows from a single input row which is exactly what we need.
 * This class specifies the parameters needed to construct the lateral view. A LateralViewParams is an
 * optional parameter, and if specified it's applicable only for Sliding-window aggregation features.
 * Further, it's specified once in the enclosing anchor.
 * </p>
 */
/*
 * Design doc: https://docs.google.com/document/d/1B_ahJC5AQ4lgZIIFkG6gZnzTvp4Ori7WwWj9yv7XTe0/edit?usp=sharing
 * RB: https://rb.corp.linkedin.com/r/1460513/
 */
public final class LateralViewParams {
  /*
   * Fields used in anchor config fragment
   */
  public static final String LATERAL_VIEW_DEF = "lateralViewDef";
  public static final String LATERAL_VIEW_ITEM_ALIAS = "lateralViewItemAlias";
  public static final String LATERAL_VIEW_FILTER = "lateralViewFilter";

  private final String _def;
  private final String _itemAlias;
  private final Optional<String> _filter;
  private String _configStr;

  /**
   * Constructor
   * @param def A table-generating function. Typically it's explode(...)
   * @param itemAlias User-defined alias for the generated table
   * @param filter A filter expression applied to the elements/tuples in the input row. Optional parameter.
   */
  public LateralViewParams(String def, String itemAlias, String filter) {
    _def = def;
    _itemAlias = itemAlias;
    _filter = Optional.ofNullable(filter);
  }

  /**
   * Constructor
   * @param def A table-generating function. Typically it's explode(...)
   * @param itemAlias User-defined alias for the generated table
   */
  public LateralViewParams(String def, String itemAlias) {
    this(def, itemAlias, null);
  }

  public String getDef() {
    return _def;
  }

  public String getItemAlias() {
    return _itemAlias;
  }

  public Optional<String> getFilter() {
    return _filter;
  }

  @Override
  public String toString() {
    if (_configStr == null) {
      _configStr = String.join("\n",
          LATERAL_VIEW_DEF + ": " + _def,
          LATERAL_VIEW_ITEM_ALIAS + ": " + _itemAlias);

      _filter.ifPresent(filter -> _configStr = String.join("\n", _configStr, LATERAL_VIEW_FILTER + ": " + filter));
    }

    return _configStr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LateralViewParams that = (LateralViewParams) o;
    return Objects.equals(_def, that._def) && Objects.equals(_itemAlias, that._itemAlias) && Objects.equals(_filter,
        that._filter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_def, _itemAlias, _filter);
  }
}
