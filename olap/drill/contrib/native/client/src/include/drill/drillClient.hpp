/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef DRILL_CLIENT_H
#define DRILL_CLIENT_H

#include <vector>
#include <boost/thread.hpp>
#include "drill/common.hpp"
#include "drill/collections.hpp"
#include "drill/protobuf/Types.pb.h"

namespace exec{
    namespace shared{
        class DrillPBError;
    };
};

namespace Drill{

//struct UserServerEndPoint;
class  DrillClientConfig;
class  DrillClientError;
class  DrillClientImplBase;
class  DrillClientImpl;
class  DrillClientQueryResult;
class  DrillUserProperties;
class  FieldMetadata;
class  PreparedStatement;
class  RecordBatch;
class  SchemaDef;

enum QueryType{
    SQL = 1,
    LOGICAL = 2,
    PHYSICAL = 3
};

// Only one instance of this class exists. A static member of DrillClientImpl;
class DECLSPEC_DRILL_CLIENT DrillClientInitializer{
    public:
        DrillClientInitializer();
        ~DrillClientInitializer();
};

/*
 * Handle to the Query submitted for execution.
 * */
typedef void* QueryHandle_t;

/*
 * Query Results listener callback. This function is called for every record batch after it has
 * been received and decoded. The listener function should return a status.
 * If the listener returns failure, the query will be canceled.
 * The listener is also called one last time when the query is completed or gets an error. In that
 * case the RecordBatch Parameter is NULL. The DrillClientError parameter is NULL is there was no
 * error oterwise it will have a valid DrillClientError object.
 * DrillClientQueryResult will hold a listener & listener contxt for the call back function
 */
typedef status_t (*pfnQueryResultsListener)(QueryHandle_t ctx, RecordBatch* b, DrillClientError* err);

/*
 * The schema change listener callback. This function is called if the record batch detects a
 * change in the schema. The client application can call getColDefs in the RecordIterator or
 * get the field information from the RecordBatch itself and handle the change appropriately.
 */
typedef status_t (*pfnSchemaListener)(void* ctx, FieldDefPtr f, DrillClientError* err);

/**
 * The prepared statement creation listener
 *
 * This function is called when a prepared statement is created, or if an error occurs during the prepared statement creation.
 * This callback is only invoked once. 
 * @param[in] ctx the listener context provided to getColumns
 * @param[in] pstmt the prepared statement handle, NULL in case of error
 * @param[in] err an error object, NULL in case of success
 */
typedef status_t (*pfnPreparedStatementListener)(void* ctx, PreparedStatement* pstmt, DrillClientError* err);

/*
 * A Record Iterator instance is returned by the SubmitQuery class. Calls block until some data
 * is available, or until all data has been returned.
 */

class DECLSPEC_DRILL_CLIENT RecordIterator{
    friend class DrillClient;
    public:

    ~RecordIterator();
    /*
     * Returns a vector of column(i.e. field) definitions. The returned reference is guaranteed to be valid till the
     * end of the query or until a schema change event is received. If a schema change event is received by the
     * application, the application should discard the reference it currently holds and call this function again.
     */
    FieldDefPtr getColDefs();

    /* Move the current pointer to the next record. */
    status_t next();

    /* Gets the ith column in the current record. */
    status_t getCol(size_t i, void** b, size_t* sz);

    /* true if ith column in the current record is NULL */
    bool isNull(size_t i);

    /* Cancels the query. */
    status_t cancel();

    /*  Returns true is the schem has changed from the previous record. Returns false for the first record. */
    bool hasSchemaChanged();

    void registerSchemaChangeListener(pfnSchemaListener l);

    bool hasError();
    /*
     * Returns the last error message
     */
    const std::string& getError();

    private:
    RecordIterator(DrillClientQueryResult* pResult){
        this->m_currentRecord=-1;
        this->m_pCurrentRecordBatch=NULL;
        this->m_pQueryResult=pResult;
        //m_pColDefs=NULL;
    }

    DrillClientQueryResult* m_pQueryResult;
    size_t m_currentRecord;
    RecordBatch* m_pCurrentRecordBatch;
    boost::mutex m_recordBatchMutex;
    FieldDefPtr m_pColDefs; // Copy of the latest column defs made from the
    // first record batch with this definition
};

namespace meta {
  // Set of template functions to create bitmasks
  template<typename T>
  inline T
  operator&(T __a, T __b)
  { return T(static_cast<int>(__a) & static_cast<int>(__b)); }
  template<typename T>
  inline T
  operator|(T __a, T __b)
  { return T(static_cast<int>(__a) | static_cast<int>(__b)); }
  template<typename T>
  inline T
  operator^(T __a, T __b)
  { return T(static_cast<int>(__a) ^ static_cast<int>(__b)); }
  template<typename T>
  inline T&
  operator|=(T& __a, T __b)
  { return __a = __a | __b; }
  template<typename T>
  inline T&
  operator&=(T& __a, T __b)
  { return __a = __a & __b; }
  template<typename T>
  inline T&
  operator^=(T& __a, T __b)
  { return __a = __a ^ __b; }
  template<typename T>
  inline T
  operator~(T __a)
  { return T(~static_cast<int>(__a)); }

  /*
   * Internal type for Date/Time literals support
   */
  enum _DateTimeLiteralSupport {
    _DL_NONE                      = 0,
    _DL_DATE                      = 1 << 1L,
    _DL_TIME                      = 1 << 2L,
    _DL_TIMESTAMP                 = 1 << 3L,
    _DL_INTERVAL_YEAR             = 1 << 4L,
    _DL_INTERVAL_MONTH            = 1 << 5L,
    _DL_INTERVAL_DAY              = 1 << 6L,
    _DL_INTERVAL_HOUR             = 1 << 7L,
    _DL_INTERVAL_MINUTE           = 1 << 8L,
    _DL_INTERVAL_SECOND           = 1 << 9L,
    _DL_INTERVAL_YEAR_TO_MONTH    = 1 << 10L,
    _DL_INTERVAL_DAY_TO_HOUR      = 1 << 11L,
    _DL_INTERVAL_DAY_TO_MINUTE    = 1 << 12L,
    _DL_INTERVAL_DAY_TO_SECOND    = 1 << 13L,
    _DL_INTERVAL_HOUR_TO_MINUTE   = 1 << 14L,
    _DL_INTERVAL_HOUR_TO_SECOND   = 1 << 15L,
    _DL_INTERVAL_MINUTE_TO_SECOND = 1 << 16L
  };

  template _DateTimeLiteralSupport operator&(_DateTimeLiteralSupport __a, _DateTimeLiteralSupport __b);
  template _DateTimeLiteralSupport operator|(_DateTimeLiteralSupport __a, _DateTimeLiteralSupport __b);
  template _DateTimeLiteralSupport operator^(_DateTimeLiteralSupport __a, _DateTimeLiteralSupport __b);

  template _DateTimeLiteralSupport& operator&=(_DateTimeLiteralSupport& __a, _DateTimeLiteralSupport __b);
  template _DateTimeLiteralSupport& operator|=(_DateTimeLiteralSupport& __a, _DateTimeLiteralSupport __b);
  template _DateTimeLiteralSupport& operator^=(_DateTimeLiteralSupport& __a, _DateTimeLiteralSupport __b);

  template _DateTimeLiteralSupport operator~(_DateTimeLiteralSupport __a);

  /**
   * Date time literal support flags
   */
  typedef _DateTimeLiteralSupport DateTimeLiteralSupport;

  /** Does not support Date/Time literals */
  static const DateTimeLiteralSupport DL_NONE = _DL_NONE;
  /** Supports DATE literal */
  static const DateTimeLiteralSupport DL_DATE = _DL_DATE;
  /** Supports TIME literal */
  static const DateTimeLiteralSupport DL_TIME = _DL_TIME;
  /** Supports TIMESTAMP literal */
  static const DateTimeLiteralSupport DL_TIMESTAMP = _DL_TIMESTAMP;
  /** Supports INTERVAL YEAR literal */
  static const DateTimeLiteralSupport DL_INTERVAL_YEAR = _DL_INTERVAL_YEAR;
  /** Supports INTERVAL MONTH literal */
  static const DateTimeLiteralSupport DL_INTERVAL_MONTH = _DL_INTERVAL_MONTH;
  /** Supports INTERVAL DAY literal */
  static const DateTimeLiteralSupport DL_INTERVAL_DAY = _DL_INTERVAL_DAY;
  /** Supports INTERVAL HOUR literal */
  static const DateTimeLiteralSupport DL_INTERVAL_HOUR = _DL_INTERVAL_HOUR;
  /** Supports INTERVAL MINUTE literal */
  static const DateTimeLiteralSupport DL_INTERVAL_MINUTE = _DL_INTERVAL_MINUTE;
  /** Supports INTERVAL SECOND literal */
  static const DateTimeLiteralSupport DL_INTERVAL_SECOND = _DL_INTERVAL_SECOND;
  /** Supports INTERVAL YEAR TO MONTH literal */
  static const DateTimeLiteralSupport DL_INTERVAL_YEAR_TO_MONTH = _DL_INTERVAL_YEAR_TO_MONTH;
  /** Supports INTERVAL DAY TO HOUR literal */
  static const DateTimeLiteralSupport DL_INTERVAL_DAY_TO_HOUR = _DL_INTERVAL_DAY_TO_HOUR;
  /** Supports INTERVAL DAY TO MINUTE literal */
  static const DateTimeLiteralSupport DL_INTERVAL_DAY_TO_MINUTE = _DL_INTERVAL_DAY_TO_MINUTE;
  /** Supports INTERVAL DAY TO SECOND literal */
  static const DateTimeLiteralSupport DL_INTERVAL_DAY_TO_SECOND = _DL_INTERVAL_DAY_TO_SECOND;
  /** Supports INTERVAL HOUR TO MINUTE literal */
  static const DateTimeLiteralSupport DL_INTERVAL_HOUR_TO_MINUTE = _DL_INTERVAL_HOUR_TO_MINUTE;
  /** Supports INTERVAL HOUR TO SECOND literal */
  static const DateTimeLiteralSupport DL_INTERVAL_HOUR_TO_SECOND = _DL_INTERVAL_HOUR_TO_SECOND;
  /** Supports INTERVAL MINUTE TO SECOND literal */
  static const DateTimeLiteralSupport DL_INTERVAL_MINUTE_TO_SECOND = _DL_INTERVAL_MINUTE_TO_SECOND;

  /*
   * Internal type for COLLATE support
   */
  enum _CollateSupport {
      _C_NONE       = 0,
      _C_GROUPBY    = 1 << 1L
  };

  template _CollateSupport operator&(_CollateSupport __a, _CollateSupport __b);
  template _CollateSupport operator|(_CollateSupport __a, _CollateSupport __b);
  template _CollateSupport operator^(_CollateSupport __a, _CollateSupport __b);

  template _CollateSupport& operator&=(_CollateSupport& __a, _CollateSupport __b);
  template _CollateSupport& operator|=(_CollateSupport& __a, _CollateSupport __b);
  template _CollateSupport& operator^=(_CollateSupport& __a, _CollateSupport __b);

  template _CollateSupport operator~(_CollateSupport __a);


  /**
   * COLLATE support flags
   */
  typedef _CollateSupport CollateSupport;
  static const CollateSupport C_NONE = _C_NONE;       /**< COLLATE clauses are not supported */
  static const CollateSupport C_GROUPBY = _C_GROUPBY; /**< a COLLATE clause can be added after each grouping column */

  /**
   * Correlation names support flags
   */
  enum CorrelationNamesSupport {
    CN_NONE            = 1, /**< Correlation names are not supported */
    CN_DIFFERENT_NAMES = 2, /**< Correlation names are supported, but names have to be different
    							 from the tables they represent */
    CN_ANY_NAMES       = 3  /**< Correlation names are supported with no restriction on names */
  };

  /**
   * Group by support
   */
  enum GroupBySupport {
      GB_NONE,         /**< Do not support GROUP BY */
      GB_SELECT_ONLY,  /**< Only support GROUP BY clause with non aggregated columns in the select list */
      GB_BEYOND_SELECT,/**< Support GROUP BY clauses with columns absent from the select list
      	  	  	  	  	    if all the non-aggregated column from the select list are also added. */
      GB_UNRELATED     /** Support GROUP BY clauses with columns absent from the select list */
  };

  /**
   * Identified case support
   */
  enum IdentifierCase {
	  IC_UNKNOWN      = -1, /**< Unknown support */
      IC_STORES_LOWER = 0,  /**< Mixed case unquoted SQL identifier are treated as
	  	  	  	  	  	         case insensitive and stored in lower case */
      IC_STORES_MIXED = 1,  /**< Mixed case unquoted SQL identifier are treated as
	  	  	  	  	  	    	 case insensitive and stored in mixed case */
      IC_STORES_UPPER = 2,  /**< Mixed case unquoted SQL identifier are treated as
	  	  	  	  	  	    	 case insensitive and stored in upper case */
      IC_SUPPORTS_MIXED =3  /**< Mixed case unquoted SQL identifier are treated as
	  	  	  	  	  	     	 case sensitive and stored in mixed case */
  };

  /**
   * Null collation support
   */
  enum NullCollation {
	  NC_UNKNOWN = -1,  /**< Unknown support */
      NC_AT_START = 0,	/**< NULL values are sorted at the start regardless of the order*/
      NC_AT_END   = 1,  /**< NULL values are sorted at the end regardless of the order*/
      NC_HIGH     = 2,  /**< NULL is the highest value */
      NC_LOW      = 3	/**< NULL is the lowest value */
  };


  /*
   * Internal type for Outer join support flags
   */
  enum _OuterJoinSupport {
      _OJ_NONE                  = 0,      //!< _OJ_NONE
      _OJ_LEFT                  = 1 << 1L,//!< _OJ_LEFT
      _OJ_RIGHT                 = 1 << 2L,//!< _OJ_RIGHT
      _OJ_FULL                  = 1 << 3L,//!< _OJ_FULL
      _OJ_NESTED                = 1 << 4L,//!< _OJ_NESTED
      _OJ_NOT_ORDERED           = 1 << 5L,//!< _OJ_NOT_ORDERED
      _OJ_INNER                 = 1 << 6L,//!< _OJ_INNER
      _OJ_ALL_COMPARISON_OPS    = 1 << 7L //!< _OJ_ALL_COMPARISON_OPS
  };

  template _OuterJoinSupport operator&(_OuterJoinSupport __a, _OuterJoinSupport __b);
  template _OuterJoinSupport operator|(_OuterJoinSupport __a, _OuterJoinSupport __b);
  template _OuterJoinSupport operator^(_OuterJoinSupport __a, _OuterJoinSupport __b);

  template _OuterJoinSupport& operator&=(_OuterJoinSupport& __a, _OuterJoinSupport __b);
  template _OuterJoinSupport& operator|=(_OuterJoinSupport& __a, _OuterJoinSupport __b);
  template _OuterJoinSupport& operator^=(_OuterJoinSupport& __a, _OuterJoinSupport __b);

  template _OuterJoinSupport operator~(_OuterJoinSupport __a);

  /**
   * Outer join support flags
   */
  typedef _OuterJoinSupport OuterJoinSupport;
  /** Outer join is not supported */
  static const OuterJoinSupport OJ_NONE                 = _OJ_NONE;
  /** Left outer join is supported */
  static const OuterJoinSupport OJ_LEFT                 = _OJ_LEFT;
  /** Right outer join is supported */
  static const OuterJoinSupport OJ_RIGHT                = _OJ_RIGHT;
  /** Full outer join is supported */
  static const OuterJoinSupport OJ_FULL                 = _OJ_FULL;
  /** Nested outer join is supported */
  static const OuterJoinSupport OJ_NESTED               = _OJ_NESTED;
  /**
   * The columns names in the ON clause of a outer join don't have to share the same
   * order as their respective table names in the OUTER JOIN clause
   */
  static const OuterJoinSupport OJ_NOT_ORDERED          = _OJ_NOT_ORDERED;
  /**
   * The inner table can also be used in an inner join
   */
  static const OuterJoinSupport OJ_INNER                = _OJ_INNER;
  /**
   * Any comparison operator in supported in the ON clause.
   */
  static const OuterJoinSupport OJ_ALL_COMPARISON_OPS   = _OJ_ALL_COMPARISON_OPS;

  /**
   * Quoted Identified case support
   */
  enum QuotedIdentifierCase {
	  QIC_UNKNOWN = -1,		 /**< Unknown support */
      QIC_STORES_LOWER = 0,	 /**< Mixed case quoted SQL identifier are treated as
	  	  	  	  	  	         case insensitive and stored in lower case */
      QIC_STORES_MIXED = 1,  /**< Mixed case quoted SQL identifier are treated as
	  	  	  	  	  	          case insensitive and stored in mixed case */
      QIC_STORES_UPPER = 2,  /**< Mixed case quoted SQL identifier are treated as
	  	  	  	  	  	          case insensitive and stored in upper case */
      QIC_SUPPORTS_MIXED =3  /**< Mixed case quoted SQL identifier are treated as
	  	  	  	  	  	          case sensitive and stored in mixed case */
  };

  /*
   * Internal Subquery support flags type
   */
  enum _SubQuerySupport {
      _SQ_NONE          = 0,
      _SQ_CORRELATED    = 1 << 1L,
      _SQ_IN_COMPARISON = 1 << 2L,
      _SQ_IN_EXISTS     = 1 << 3L,
      _SQ_IN_INSERT     = 1 << 4L,
      _SQ_IN_QUANTIFIED = 1 << 5L
  };

  template _SubQuerySupport operator&(_SubQuerySupport __a, _SubQuerySupport __b);
  template _SubQuerySupport operator|(_SubQuerySupport __a, _SubQuerySupport __b);
  template _SubQuerySupport operator^(_SubQuerySupport __a, _SubQuerySupport __b);

  template _SubQuerySupport& operator&=(_SubQuerySupport& __a, _SubQuerySupport __b);
  template _SubQuerySupport& operator|=(_SubQuerySupport& __a, _SubQuerySupport __b);
  template _SubQuerySupport& operator^=(_SubQuerySupport& __a, _SubQuerySupport __b);

  template _SubQuerySupport operator~(_SubQuerySupport __a);

  /**
   * SubQuery support flags
   */
  typedef _SubQuerySupport SubQuerySupport;
  /**
   * Subqueries are not supported
   */
  static const SubQuerySupport SQ_NONE             = _SQ_NONE;
  /** Correlated subqueries are supported */
  static const SubQuerySupport SQ_CORRELATED       = _SQ_CORRELATED;
  /** Subqueries in comparison expressions are supported */
  static const SubQuerySupport SQ_IN_COMPARISON    = _SQ_IN_COMPARISON;
  /** Subqueries in EXISTS expressions are supported */
  static const SubQuerySupport SQ_IN_EXISTS        = _SQ_IN_EXISTS;
  /** Subqueries in INSERT expressions are supported */
  static const SubQuerySupport SQ_IN_INSERT        = _SQ_IN_INSERT;
  /** Subqueries in quantified expressions are supported */
  static const SubQuerySupport SQ_IN_QUANTIFIED    = _SQ_IN_QUANTIFIED;

  /*
   * Internal Union support flags type
   */
  enum _UnionSupport {
      _U_NONE       = 0,      //!< _U_NONE
      _U_UNION      = 1 << 1L,//!< _U_UNION
      _U_UNION_ALL  = 1 << 2L //!< _U_UNION_ALL
  };

  template _UnionSupport operator&(_UnionSupport __a, _UnionSupport __b);
  template _UnionSupport operator|(_UnionSupport __a, _UnionSupport __b);
  template _UnionSupport operator^(_UnionSupport __a, _UnionSupport __b);

  template _UnionSupport& operator&=(_UnionSupport& __a, _UnionSupport __b);
  template _UnionSupport& operator|=(_UnionSupport& __a, _UnionSupport __b);
  template _UnionSupport& operator^=(_UnionSupport& __a, _UnionSupport __b);

  template _UnionSupport operator~(_UnionSupport __a);

  /**
   * Union support flags
   */
  typedef _UnionSupport UnionSupport;
  /** Union is not supported */
  static const UnionSupport U_NONE       = _U_NONE;
  /** UNION is supported */
  static const UnionSupport U_UNION      = _U_UNION;
  /** UNION ALL is supported */
  static const UnionSupport U_UNION_ALL  = _U_UNION_ALL;

  class DECLSPEC_DRILL_CLIENT CatalogMetadata {
      protected:
      CatalogMetadata() {};
      public:
      virtual ~CatalogMetadata() {};

      virtual bool               hasCatalogName() const = 0;
      virtual const std::string& getCatalogName() const = 0;

      virtual bool               hasDescription() const = 0;
      virtual const std::string& getDescription() const = 0;

      virtual bool               hasConnect() const = 0;
      virtual const std::string& getConnect() const = 0;
  };

  class DECLSPEC_DRILL_CLIENT SchemaMetadata {
      protected:
      SchemaMetadata() {};

      public:
      virtual ~SchemaMetadata() {};

      virtual bool               hasCatalogName() const = 0;
      virtual const std::string& getCatalogName() const = 0;

      virtual bool               hasSchemaName() const = 0;
      virtual const std::string& getSchemaName() const = 0;

      virtual bool               hasOwnerName() const = 0;
      virtual const std::string& getOwner() const = 0;

      virtual bool               hasType() const = 0;
      virtual const std::string& getType() const = 0;

      virtual bool               hasMutable() const = 0;
      virtual const std::string& getMutable() const = 0;
  };

  class DECLSPEC_DRILL_CLIENT TableMetadata {
      protected:
      TableMetadata() {};

      public:
      virtual ~TableMetadata() {};

      virtual bool               hasCatalogName() const = 0;
      virtual const std::string& getCatalogName() const = 0;

      virtual bool               hasSchemaName() const = 0;
      virtual const std::string& getSchemaName() const = 0;

      virtual bool               hasTableName() const = 0;
      virtual const std::string& getTableName() const = 0;

      virtual bool               hasType() const = 0;
      virtual const std::string& getType() const = 0;
  };

  class DECLSPEC_DRILL_CLIENT ColumnMetadata {
      protected:
      ColumnMetadata() {};

      public:
      virtual ~ColumnMetadata() {};

      virtual bool               hasCatalogName() const = 0;
      virtual const std::string& getCatalogName() const = 0;

      virtual bool               hasSchemaName() const = 0;
      virtual const std::string& getSchemaName() const = 0;

      virtual bool               hasTableName() const = 0;
      virtual const std::string& getTableName() const = 0;

      virtual bool               hasColumnName() const = 0;
      virtual const std::string& getColumnName() const = 0;

      virtual bool               hasOrdinalPosition() const = 0;
      virtual std::size_t        getOrdinalPosition() const = 0;

      virtual bool               hasDefaultValue() const = 0;
      virtual const std::string& getDefaultValue() const = 0;

      virtual bool               hasNullable() const = 0;
      virtual bool               isNullable() const = 0;

      virtual bool               hasDataType() const = 0;
      virtual const std::string& getDataType() const = 0;

      virtual bool               hasColumnSize() const = 0;
      virtual std::size_t        getColumnSize() const = 0;

      virtual bool               hasCharMaxLength() const = 0;
      virtual std::size_t        getCharMaxLength() const = 0;

      virtual bool               hasCharOctetLength() const = 0;
      virtual std::size_t        getCharOctetLength() const = 0;

      virtual bool               hasNumericPrecision() const = 0;
      virtual int32_t            getNumericPrecision() const = 0;

      virtual bool               hasNumericRadix() const = 0;
      virtual int32_t            getNumericRadix() const = 0;

      virtual bool               hasNumericScale() const = 0;
      virtual int32_t            getNumericScale() const = 0;

      virtual bool               hasIntervalType() const = 0;
      virtual const std::string& getIntervalType() const = 0;

      virtual bool               hasIntervalPrecision() const = 0;
      virtual int32_t            getIntervalPrecision() const = 0;
  };
}

class DECLSPEC_DRILL_CLIENT Metadata {
  public:
    virtual ~Metadata() {};

    /**
     * Returns the connector name
     *
     * @return the connector name
     */
    virtual const std::string& getConnectorName() const = 0;

    /**
     * Returns the connector version string
     *
     * @return the connector version string
     */
    virtual const std::string& getConnectorVersion() const = 0;

    /**
     * Returns the connector major version
     *
     * @return the connector major version
     */
    virtual uint32_t getConnectorMajorVersion() const = 0;

    /**
     * Returns the connector minor version
     *
     * @return the connector minor version
     */
    virtual uint32_t getConnectorMinorVersion() const = 0;

    /**
     * Returns the connector patch version
     *
     * @return the connector patch version
     */
    virtual uint32_t getConnectorPatchVersion() const = 0;

    /**
     * Returns the server name
     *
     * @return the server name
     */
    virtual const std::string& getServerName() const = 0;

    /**
     * Returns the server version string
     *
     * @return the server version string
     */
    virtual const std::string& getServerVersion() const = 0;

    /**
     * Returns the server major version
     *
     * @return the server major version
     */
    virtual uint32_t getServerMajorVersion() const = 0;

    /**
     * Returns the server minor version
     *
     * @return the server minor version
     */
    virtual uint32_t getServerMinorVersion() const = 0;

    /**
     * Returns the server patch version
     *
     * @return the server patch version
     */
    virtual uint32_t getServerPatchVersion() const = 0;

    /**
     * Callback function invoked by getCatalogs when receiving results
     *
     * This callback is only invoked once. 
     * @param[in] ctx the listener context provided to getCatalogs
     * @param[in] metadata the catalog metadata, or NULL in case of error
     * @param[in] err an error object, NULL in case of success
     */
    typedef status_t (*pfnCatalogMetadataListener)(void* ctx, const DrillCollection<meta::CatalogMetadata>* metadata, DrillClientError* err);

    /**
     * Get a list of catalogPattern available to the current connection.
     * Only catalogs matching the catalogPattern LIKE expression are returned.
     *
     * @param[in] catalogPattern a catalog pattern
     * @param[in] listener a metadata listener
     * @param[in] context to be passed to the listener
     * @param[out] the query handle
     */
    virtual status_t getCatalogs(const std::string& catalogPattern, pfnCatalogMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) = 0;

    /**
     * Callback function invoked by getSchemas when receiving results
     *
     * This callback is only invoked once. 
     * @param[in] ctx the listener context provided to getSchemas
     * @param[in] metadata the schema metadata, or NULL in case of error
     * @param[in] err an error object, NULL in case of success
     */
    typedef status_t (*pfnSchemaMetadataListener)(void* ctx, const DrillCollection<meta::SchemaMetadata>* metadata, DrillClientError* err);

    /**
     * Get a list of schemas available to the current connection.
     * Only schemas matching the catalogPattern and schemaPattern LIKE expressions are returned.
     *
     * @param[in] catalogPattern a catalog pattern
     * @param[in] schemaPattern a schema pattern
     * @param[in] listener a metadata query listener
     * @param[in] context to be passed to the listener
     * @param[out] the query handle
     */
    virtual status_t getSchemas(const std::string& catalogPattern, const std::string& schemaPattern, pfnSchemaMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) = 0;

    /**
     * Callback function invoked by getTables when receiving results
     *
     * This callback is only invoked once. 
     * @param[in] ctx the listener context provided to getTables
     * @param[in] metadata the table metadata, or NULL in case of error
     * @param[in] err an error object, NULL in case of success
     */
    typedef status_t (*pfnTableMetadataListener)(void* ctx, const DrillCollection<meta::TableMetadata>* metadata, DrillClientError* err);

    /**
     * Get a list of tables available to the current connection.
     * Only tables matching the catalogPattern, schemaPattern and tablePattern LIKE expressions are returned.
     *
     * @param[in] catalogPattern a catalog pattern
     * @param[in] schemaPattern a schema pattern
     * @param[in] tablePattern a table pattern
     * @param[in] tableTypes a list of table types to look for. Pass NULL to not filter
     * @param[in] listener a metadata query listener
     * @param[in] context to be passed to the listener
     * @param[out] the query handle
     */
    virtual status_t getTables(const std::string& catalogPattern, const std::string& schemaPattern, const std::string& tablePattern, const std::vector<std::string>* tableTypes, 
                               pfnTableMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) = 0;

    /**
     * Callback function invoked by getColumns when receiving results
     *
     * This callback is only invoked once. 
     * @param[in] ctx the listener context provided to getColumns
     * @param[in] metadata the columns metadata, or NULL in case of error
     * @param[in] err an error object, NULL in case of success
     */
    typedef status_t (*pfnColumnMetadataListener)(void* ctx, const DrillCollection<meta::ColumnMetadata>* metadata, DrillClientError* err);

    /**
     * Get a list of columns available to the current connection.
     * Only columns matching the catalogPattern, schemaPattern, tablePattern and columnPattern LIKE expressions are returned.
     *
     * @param[in] catalogPattern a catalog pattern
     * @param[in] schemaPattern a schema pattern
     * @param[in] tablePattern a table pattern
     * @param[in] columnPattern a colum name pattern
     * @param[in] listener a metadata query listener
     * @param[in] context to be passed to the listener
     * @param[out] the query handle
     */
    virtual status_t getColumns(const std::string& catalogPattern, const std::string& schemaPattern, const std:: string& tablePattern, const std::string& columnPattern, pfnColumnMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) = 0;

    // Capabilities
    /**
     * Return if the current user can use all tables returned by the getTables method
     *
     * @result true if the user can select any table, false otherwise
     */
    virtual bool areAllTableSelectable() const = 0;

    /**
     * Return if the catalog name is at the start of a fully qualified table name
     *
     * @return true if the catalog name is at the start, false otherwise.
     */
    virtual bool isCatalogAtStart() const = 0;

    /**
     * Return the string used as a separator between the catalog and the table name
     *
     * @return the catalog separator
     */
    virtual const std::string& getCatalogSeparator() const = 0;

    /**
     * Return the term used by the server to designate a catalog
     *
     * @return the catalog term
     */
    virtual const std::string& getCatalogTerm() const = 0;

    /**
     * Return if the server supports column aliasing
     *
     * @return true if the server supports column aliasing, false otherwise
     */
    virtual bool isColumnAliasingSupported() const = 0;

    /**
     * Return if the result of a NULL and a non-NULL values concatenation is NULL
     *
     * @return true if the result is NULL, false otherwise
     */
    virtual bool isNullPlusNonNullNull() const = 0;

    /**
     * Return if the CONVERT function supports conversion for the given types
     *
     * @return true if the conversion is supported, false otherwise
     */
    virtual bool isConvertSupported(common::MinorType from, common::MinorType to) const = 0;

    /**
     * Return what kind of correlation name support the server provides
     *
     * @return the correlation name supported by the server
     */
    virtual meta::CorrelationNamesSupport getCorrelationNames() const = 0;

    /**
     * Returns if the connection to the server is read-only
     *
     * @return true if the connection is read-only, false otherwise
     */
    virtual bool isReadOnly() const = 0;

    /**
     * Return what kind of date time literals the server supports
     *
     * @return a bitmask of supported date/time literals
     */
    virtual meta::DateTimeLiteralSupport getDateTimeLiteralsSupport() const = 0;

    /**
     * Return what kind of COLLATE expressions are supported
     */
    virtual meta::CollateSupport getCollateSupport() const = 0;

    /**
     * Return what kind of GROUP BY support the server provides
     *
     * @return the group by support
     */
    virtual meta::GroupBySupport getGroupBySupport() const = 0;

    /**
     * Returns how unquoted identifier are stored
     *
     * @return the unquoted identifier storage policy
     */
    virtual meta::IdentifierCase getIdentifierCase() const = 0;

    /**
     * Returns the string used to quote SQL identifiers
     *
     * @return the quote string
     */
    virtual const std::string& getIdentifierQuoteString() const = 0;

    /**
     * Returns the list of SQL keywords supported by the database
     *
     * @return a list of keywords
     */
    virtual const std::vector<std::string>& getSQLKeywords() const = 0;

    /**
     * Returns if LIKE operator supports an escape clause
     *
     * @return true if escape claused are supported
     */
    virtual bool isLikeEscapeClauseSupported() const = 0;

    /**
     * Returns the maximum number of hexa characters supported for binary literals
     *
     * @return the length, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxBinaryLiteralLength() const = 0;

    /**
     * Returns the maximum length of catalog names
     *
     * @return the length, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxCatalogNameLength() const = 0;

    /**
     * Returns the maximum number of characters for string literals
     *
     * @return the length, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxCharLiteralLength() const = 0;

    /**
     * Returns the maximum length of column names
     *
     * @return the length, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxColumnNameLength() const = 0;

    /**
     * Returns the maximum number of columns in GROUP BY expressions
     *
     * @return the maximum number, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxColumnsInGroupBy() const = 0;

    /**
     * Returns the maximum number of columns in ORDER BY expressions
     *
     * @return the maximum number, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxColumnsInOrderBy() const = 0;

    /**
     * Returns the maximum number of columns in a SELECT list
     *
     * @return the maximum number, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxColumnsInSelect() const = 0;

    /**
     * Returns the maximum length for cursor names
     *
     * @return the maximum length, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxCursorNameLength() const = 0;

    /**
     * Returns the maximum logical size for LOB types
     *
     * @return the maximum size, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxLogicalLobSize() const = 0;

    /**
     * Returns the maximum number of statements
     *
     * @return the maximum number, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxStatements() const = 0;

    /**
     * Returns the maximum number of bytes for a single row
     * @return the maximum size, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxRowSize() const = 0;

    /**
     * Returns if BLOB types are included in the maximum row size
     *
     * @return true if BLOB are included
     */
    virtual bool isBlobIncludedInMaxRowSize() const = 0;

    /**
     * Returns the maximum length for schema names
     * @return the maximum length, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxSchemaNameLength() const = 0;

    /**
     * Returns the maximum length for statements
     * @return the maximum length, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxStatementLength() const = 0;

    /**
     * Returns the maximum length for table names
     * @return the maximum length, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxTableNameLength() const = 0;

    /**
     * Returns the maximum number of tables in a SELECT expression
     * @return the maximum number, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxTablesInSelect() const = 0;

    /**
     * Returns the maximum length for user names
     * @return the maximum length, 0 if unlimited or unknown
     */
    virtual std::size_t getMaxUserNameLength() const = 0;

    /**
     * Returns how NULL are sorted
     *
     * @return the NULL collation policy
     */
    virtual meta::NullCollation getNullCollation() const = 0;

    /**
     * Returns the list of supported numeric functions
     * @return a list of function names
     */
    virtual const std::vector<std::string>& getNumericFunctions() const = 0;

    /**
     * Returns how outer joins are supported
     * @return outer join support (as flags)
     */
    virtual meta::OuterJoinSupport getOuterJoinSupport() const = 0;

    /**
     * Returns if columns not in the SELECT column lists can be used
     * in the ORDER BY expression
     *
     * @return true if unrelated columns are supported in ORDER BY
     */
    virtual bool isUnrelatedColumnsInOrderBySupported() const = 0;

    /**
     * Returns how quoted identifier are stored
     *
     * @return the quoted identifier storage policy
     */
    virtual meta::QuotedIdentifierCase getQuotedIdentifierCase() const = 0;

    /**
     * Returns the term used to designate schemas
     *
     * @return the term
     */
    virtual const std::string& getSchemaTerm() const = 0;

    /**
     * Return the string for escaping patterns in metadata queries
     *
     * @return the characters for escaping, empty if not supported
     */
    virtual const std::string& getSearchEscapeString() const = 0;

    /**
     * Returns the list of extra characters that can be used in identifier names
     *
     * Extra characters are those characters beyond a-z, A-Z, 0-9 and '_' (underscore)
     *
     * @return a list of characters
     */
    virtual const std::string& getSpecialCharacters() const = 0;

    /**
     * Returns the list of supported string functions
     *
     * @return a list of function names
     */
    virtual const std::vector<std::string>& getStringFunctions() const = 0;

    /**
     * Returns how subqueries are supported
     *
     * @return the subqueries support (as flags)
     */
    virtual meta::SubQuerySupport getSubQuerySupport() const = 0;

    /**
     * Returns the list of supported system functions
     *
     * @return a list of function names
     */
    virtual const std::vector<std::string>& getSystemFunctions() const = 0;

    /**
     * Returns the term used to designate tables
     *
     * @return the term
     */
    virtual const std::string& getTableTerm() const = 0;

    /**
     * Returns the list of supported date/time functions
     *
     * @return a list of function names
     */
    virtual const std::vector<std::string>& getDateTimeFunctions() const = 0;

    /**
     * Returns if transactions are supported
     * @return true if transactions are supported
     */
    virtual bool isTransactionSupported() const = 0;

    /**
     * Returns how unions are supported
     *
     * @return the union support (as flags)
     */
    virtual meta::UnionSupport getUnionSupport() const = 0;

    /**
     * Returns if SELECT FOR UPDATE expressions are supported
     *
     * @return true if SELECT FOR UPDATE is supported
     */
    virtual bool isSelectForUpdateSupported() const = 0;
};

class DECLSPEC_DRILL_CLIENT DrillClient{
    public:
        /*
         * Get the application context from query handle
         */
        static void* getApplicationContext(QueryHandle_t handle);

        /*
         * Get the query status from query handle
         */
        static status_t getQueryStatus(QueryHandle_t handle);


        DrillClient();
        ~DrillClient();

        // change the logging level
        static void initLogging(const char* path, logLevel_t l);

        /**
         * Connect the client to a Drillbit using connection string and default schema.
         *
         * @param[in] connectStr: connection string
         * @param[in] defaultSchema: default schema (set to NULL and ignore it
         * if not specified)
         * @return    connection status
         */
        DEPRECATED connectionStatus_t connect(const char* connectStr, const char* defaultSchema=NULL);

        /*  
         * Connect the client to a Drillbit using connection string and a set of user properties.
         * The connection string format can be found in comments of
         * [DRILL-780](https://issues.apache.org/jira/browse/DRILL-780)
         *
         * To connect via zookeeper, use the format:
         * "zk=zk1:port[,zk2:p2,...][/<path_to_drill_root>/<cluster_id>]".
         *
         * e.g.
         * ```
         * zk=localhost:2181
         * zk=localhost:2181/drill/drillbits1
         * zk=localhost:2181,zk2:2181/drill/drillbits1
         * ```
         *
         * To connect directly to a drillbit, use the format "local=host:port".
         *
         * e.g.
         * ```
         * local=127.0.0.1:31010
         * ```
         *
         * User properties is a set of name value pairs. The following properties are recognized:
         *     schema
         *     userName
         *     password
         *     useSSL [true|false]
         *     pemLocation
         *     pemFile
         *     (see drill/common.hpp for friendly defines and the latest list of supported properties)
         *
         * @param[in] connectStr: connection string
         * @param[in] properties
         * if not specified)
         * @return    connection status
         */
        connectionStatus_t connect(const char* connectStr, DrillUserProperties* properties);

        /* test whether the client is active */
        bool isActive();

        /*  close the connection. cancel any pending requests. */
        void close() ;

        /*
         * Submit a query asynchronously and wait for results to be returned through a callback. A query context handle is passed
         * back. The listener callback will return the handle in the ctx parameter.
         */
        status_t submitQuery(Drill::QueryType t, const std::string& plan, pfnQueryResultsListener listener, void* listenerCtx, QueryHandle_t* qHandle);

        /*
         * Submit a query asynchronously and wait for results to be returned through an iterator that returns
         * results synchronously. The client app needs to call freeQueryIterator on the iterator when done.
         */
        RecordIterator* submitQuery(Drill::QueryType t, const std::string& plan, DrillClientError* err);

        /**
         * Prepare a query.
         *
         * @param[in] sql the query to prepare
         * @param[in] listener a callback to be notified when the prepared statement is created, or if an error occured
         * @param[in] user context to provide to the callback
         * @param[out] a handle on the query
         */
        status_t prepareQuery(const std::string& sql, pfnPreparedStatementListener listener, void* listenerCtx, QueryHandle_t* qHandle);

        /*
         * Execute a prepared statement.
         *
         * @param[in] pstmt the prepared statement to execute
         * @param[in] listener a callback to be notified when results have arrived, or if an error occured
         * @param[in] user context to provide to the callback
         * @param[out] a handle on the query
         */
        status_t executeQuery(const PreparedStatement& pstmt, pfnQueryResultsListener listener, void* listenerCtx, QueryHandle_t* qHandle);

        /*
         * Cancel a query.
         *
         * @param[in] the handle of the query to cancel
         */
        void cancelQuery(QueryHandle_t handle);

        /*
         * The client application should call this function to wait for results if it has registered a
         * listener.
         */
        void waitForResults();

        /*
         * Returns the last error message
         */
        std::string& getError();

        /*
         * Returns the error message associated with the query handle
         */
        const std::string& getError(QueryHandle_t handle);
        /*
         * Applications using the async query submit method can register a listener for schema changes
         *
         */
        void registerSchemaChangeListener(QueryHandle_t* handle, pfnSchemaListener l);

        /*
         * Applications using the async query submit method should call freeQueryResources to free up resources
         * once the query is no longer being processed.
         */
        void freeQueryResources(QueryHandle_t* handle);

        /*
         * Applications using the sync query submit method should call freeQueryIterator to free up resources
         * once the RecordIterator is no longer being processed.
         */
        void freeQueryIterator(RecordIterator** pIter){ delete *pIter; *pIter=NULL;}

        /*
         * Applications using the async query submit method should call freeRecordBatch to free up resources
         * once the RecordBatch is processed and no longer needed.
         */
        void freeRecordBatch(RecordBatch* pRecordBatch);

        /**
         * Get access to the server metadata
         */
        Metadata* getMetadata();

        /**
         * Free resources associated with the metadata object
         */
        void freeMetadata(Metadata** metadata);

    private:
        static DrillClientInitializer s_init;
        static DrillClientConfig s_config;

        DrillClientImplBase * m_pImpl;
};

} // namespace Drill

#endif
