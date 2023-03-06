from __future__ import annotations

import logging
import os

from helpers.data_source_fixture import DataSourceFixture
from helpers.test_table import TestTable

logger = logging.getLogger(__name__)


class SQLServerDataSourceFixture(DataSourceFixture):
    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source sqlserver": {
                "type": "sqlserver",
                "host": os.getenv("SQLSERVER_HOST", "localhost"),
                "username": os.getenv("SQLSERVER_USERNAME", "SA"),
                "password": os.getenv("SQLSERVER_PASSWORD", "Password1!"),
                "database": os.getenv("SQLSERVER_DATABASE", "master"),
                # Local docker compose has self-signed certificate
                "trust_server_certificate": "true",
                "schema": schema_name or os.getenv("SQLSERVER_SCHEMA", "dbo"),
            }
        }

    def _get_dataset_id(self):
        return f"{self.schema_data_source.project_id}.{self.schema_name}"

    def _create_schema_if_not_exists_sql(self):
        return f"""
        IF NOT EXISTS ( SELECT  *
                        FROM    sys.schemas
                        WHERE   name = N'{self.schema_name}' )
        EXEC('CREATE SCHEMA [{self.schema_name}]');
        """

    def _drop_schema_if_exists(self):
        return f"""
        /* Drop all non-system stored procs */
        DECLARE @name VARCHAR(128)
        DECLARE @SQL VARCHAR(254)

        SELECT @name = (SELECT TOP 1 [name] FROM sysobjects WHERE [type] = 'P' AND category = 0 ORDER BY [name])

        WHILE @name is not null
        BEGIN
            SELECT @SQL = 'DROP PROCEDURE [{self.schema_name}].[' + RTRIM(@name) +']'
            EXEC (@SQL)
            PRINT 'Dropped Procedure: ' + @name
            SELECT @name = (SELECT TOP 1 [name] FROM sysobjects WHERE [type] = 'P' AND category = 0 AND [name] > @name ORDER BY [name])
        END
        GO

        /* Drop all views */
        DECLARE @name VARCHAR(128)
        DECLARE @SQL VARCHAR(254)

        SELECT @name = (SELECT TOP 1 [name] FROM sysobjects WHERE [type] = 'V' AND category = 0 ORDER BY [name])

        WHILE @name IS NOT NULL
        BEGIN
            SELECT @SQL = 'DROP VIEW [{self.schema_name}].[' + RTRIM(@name) +']'
            EXEC (@SQL)
            PRINT 'Dropped View: ' + @name
            SELECT @name = (SELECT TOP 1 [name] FROM sysobjects WHERE [type] = 'V' AND category = 0 AND [name] > @name ORDER BY [name])
        END
        GO

        /* Drop all functions */
        DECLARE @name VARCHAR(128)
        DECLARE @SQL VARCHAR(254)

        SELECT @name = (SELECT TOP 1 [name] FROM sysobjects WHERE [type] IN (N'FN', N'IF', N'TF', N'FS', N'FT') AND category = 0 ORDER BY [name])

        WHILE @name IS NOT NULL
        BEGIN
            SELECT @SQL = 'DROP FUNCTION [{self.schema_name}].[' + RTRIM(@name) +']'
            EXEC (@SQL)
            PRINT 'Dropped Function: ' + @name
            SELECT @name = (SELECT TOP 1 [name] FROM sysobjects WHERE [type] IN (N'FN', N'IF', N'TF', N'FS', N'FT') AND category = 0 AND [name] > @name ORDER BY [name])
        END
        GO

        /* Drop all Foreign Key constraints */
        DECLARE @name VARCHAR(128)
        DECLARE @constraint VARCHAR(254)
        DECLARE @SQL VARCHAR(254)

        SELECT @name = (SELECT TOP 1 TABLE_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE constraint_catalog=DB_NAME() AND CONSTRAINT_TYPE = 'FOREIGN KEY' ORDER BY TABLE_NAME)

        WHILE @name is not null
        BEGIN
            SELECT @constraint = (SELECT TOP 1 CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE constraint_catalog=DB_NAME() AND CONSTRAINT_TYPE = 'FOREIGN KEY' AND TABLE_NAME = @name ORDER BY CONSTRAINT_NAME)
            WHILE @constraint IS NOT NULL
            BEGIN
                SELECT @SQL = 'ALTER TABLE [{self.schema_name}].[' + RTRIM(@name) +'] DROP CONSTRAINT [' + RTRIM(@constraint) +']'
                EXEC (@SQL)
                PRINT 'Dropped FK Constraint: ' + @constraint + ' on ' + @name
                SELECT @constraint = (SELECT TOP 1 CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE constraint_catalog=DB_NAME() AND CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_NAME <> @constraint AND TABLE_NAME = @name ORDER BY CONSTRAINT_NAME)
            END
        SELECT @name = (SELECT TOP 1 TABLE_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE constraint_catalog=DB_NAME() AND CONSTRAINT_TYPE = 'FOREIGN KEY' ORDER BY TABLE_NAME)
        END
        GO

        /* Drop all Primary Key constraints */
        DECLARE @name VARCHAR(128)
        DECLARE @constraint VARCHAR(254)
        DECLARE @SQL VARCHAR(254)

        SELECT @name = (SELECT TOP 1 TABLE_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE constraint_catalog=DB_NAME() AND CONSTRAINT_TYPE = 'PRIMARY KEY' ORDER BY TABLE_NAME)

        WHILE @name IS NOT NULL
        BEGIN
            SELECT @constraint = (SELECT TOP 1 CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE constraint_catalog=DB_NAME() AND CONSTRAINT_TYPE = 'PRIMARY KEY' AND TABLE_NAME = @name ORDER BY CONSTRAINT_NAME)
            WHILE @constraint is not null
            BEGIN
                SELECT @SQL = 'ALTER TABLE [{self.schema_name}].[' + RTRIM(@name) +'] DROP CONSTRAINT [' + RTRIM(@constraint)+']'
                EXEC (@SQL)
                PRINT 'Dropped PK Constraint: ' + @constraint + ' on ' + @name
                SELECT @constraint = (SELECT TOP 1 CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE constraint_catalog=DB_NAME() AND CONSTRAINT_TYPE = 'PRIMARY KEY' AND CONSTRAINT_NAME <> @constraint AND TABLE_NAME = @name ORDER BY CONSTRAINT_NAME)
            END
        SELECT @name = (SELECT TOP 1 TABLE_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE constraint_catalog=DB_NAME() AND CONSTRAINT_TYPE = 'PRIMARY KEY' ORDER BY TABLE_NAME)
        END
        GO

        /* Drop all tables */
        DECLARE @name VARCHAR(128)
        DECLARE @SQL VARCHAR(254)

        SELECT @name = (SELECT TOP 1 [name] FROM sysobjects WHERE [type] = 'U' AND category = 0 ORDER BY [name])

        WHILE @name IS NOT NULL
        BEGIN
            SELECT @SQL = 'DROP TABLE [{self.schema_name}].[' + RTRIM(@name) +']'
            EXEC (@SQL)
            PRINT 'Dropped Table: ' + @name
            SELECT @name = (SELECT TOP 1 [name] FROM sysobjects WHERE [type] = 'U' AND category = 0 AND [name] > @name ORDER BY [name])
        END
        GO
        """

    def _drop_test_table_sql(self, table_name):
        # @TOOD
        pass

    def _create_view_from_table_sql(self, test_table: TestTable):
        # @TOOD
        pass
