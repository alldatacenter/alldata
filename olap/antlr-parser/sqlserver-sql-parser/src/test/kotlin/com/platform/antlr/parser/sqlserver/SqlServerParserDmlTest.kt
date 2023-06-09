package com.platform.antlr.parser.sqlserver

import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.TableId
import com.platform.antlr.parser.common.relational.dml.*
import com.platform.antlr.parser.sqlserver.SqlServerHelper
import org.junit.Assert
import org.junit.Test

/**
 * Created by libinsong on 2020/6/30 11:05 上午
 */
class SqlServerParserDmlTest {

    @Test
    fun queryTest0() {
        val sql = """
            SELECT product_name, list_price
            FROM production.products
            ORDER BY list_price, product_name 
            OFFSET 20 ROWS 
            FETCH NEXT 10 ROWS ONLY;
        """.trimIndent()

        val statement = SqlServerHelper.getStatement(sql)
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(20, statement.offset)
            Assert.assertEquals(10, statement.limit)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun queryTest1() {
        val sql = """
            WITH Sales_CTE (SalesPersonID, TotalSales, SalesYear)
            AS (
                SELECT SalesPersonID, SUM(TotalDue) AS TotalSales, YEAR(OrderDate) AS SalesYear
                FROM Sales.SalesOrderHeader
                WHERE SalesPersonID IS NOT NULL
                   GROUP BY SalesPersonID, YEAR(OrderDate)
            ),
            Sales_Quota_CTE (BusinessEntityID, SalesQuota, SalesQuotaYear)
            AS (
               SELECT BusinessEntityID, SUM(SalesQuota)AS SalesQuota, YEAR(QuotaDate) AS SalesQuotaYear
               FROM Sales.SalesPersonQuotaHistory
               GROUP BY BusinessEntityID, YEAR(QuotaDate)
            )
            SELECT SalesPersonID
              , SalesYear
              , FORMAT(TotalSales,'C','en-us') AS TotalSales
              , SalesQuotaYear
              , FORMAT (SalesQuota,'C','en-us') AS SalesQuota
              , FORMAT (TotalSales -SalesQuota, 'C','en-us') AS Amt_Above_or_Below_Quota
            FROM Sales_CTE
            JOIN Sales_Quota_CTE ON Sales_Quota_CTE.BusinessEntityID = Sales_CTE.SalesPersonID
                AND Sales_CTE.SalesYear = Sales_Quota_CTE.SalesQuotaYear
            ORDER BY SalesPersonID, SalesYear
        """.trimIndent()

        val statement = SqlServerHelper.getStatement(sql)
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(2, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    //@Test
    fun queryTest2() {
        val sql = """
            DECLARE @NAME VARCHAR(20)
            SET @NAME='kkk'
            SELECT * FROM demos.dbo.tab1 WHERsE name=@NAME
            SELECT name FROM demos.dbo.tab1 WHERE name=@NAME
        """.trimIndent()

        val statement = SqlServerHelper.getStatement(sql)
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(2, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deleteTest0() {
        val sql = """
            DELETE Production.ProductCostHistory  
            WHERE StandardCost BETWEEN 12.00 AND 14.00 AND EndDate IS NULL;  
        """.trimIndent()

        val statement = SqlServerHelper.getStatement(sql)
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals(TableId("Production", "ProductCostHistory"), statement.tableId)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deleteTest1() {
        val sql = """
            DELETE TOP (20)
            FROM Purchasing.PurchaseOrderDetail
            WHERE DueDate < '20020701';
        """.trimIndent()

        val statement = SqlServerHelper.getStatement(sql)
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals(TableId("Purchasing", "PurchaseOrderDetail"), statement.tableId)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deleteTest2() {
        val sql = """
            DELETE FROM dbo.FactInternetSales  
            WHERE ProductKey IN (   
                SELECT T1.ProductKey FROM dbo.DimProduct T1   
                JOIN dbo.DimProductSubcategory T2  
                ON T1.ProductSubcategoryKey = T2.ProductSubcategoryKey  
                WHERE T2.EnglishProductSubcategoryName = 'Road Bikes') 
        """.trimIndent()

        val statement = SqlServerHelper.getStatement(sql)
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals(TableId("dbo", "FactInternetSales"), statement.tableId)
            Assert.assertEquals(2, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun insertTest0() {
        val sql = """
            INSERT INTO Production.UnitMeasure  
            VALUES (N'FT2', N'Square Feet ', '20080923'), (N'Y', N'Yards', '20080923')
                , (N'Y3', N'Cubic Yards', '20080923');  
        """.trimIndent()

        val statement = SqlServerHelper.getStatement(sql)
        if (statement is InsertTable) {
            Assert.assertEquals(StatementType.INSERT, statement.statementType)
            Assert.assertEquals(TableId("Production", "UnitMeasure"), statement.tableId)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun updateTest0() {
        val sql = """
            UPDATE Cities  
            SET Location = CONVERT(Point, '12.3:46.2')  
            WHERE Name = 'Anchorage';  
        """.trimIndent()

        val statement = SqlServerHelper.getStatement(sql)
        if (statement is UpdateTable) {
            Assert.assertEquals(StatementType.UPDATE, statement.statementType)
            Assert.assertEquals(TableId("Cities"), statement.tableId)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun updateTest1() {
        val sql = """
            UPDATE HumanResources.Employee  
            SET VacationHours = VacationHours + 8
            FROM (SELECT TOP 10 BusinessEntityID FROM HumanResources.Employee1  
                 ORDER BY HireDate ASC) AS th  
            WHERE HumanResources.Employee.BusinessEntityID = th.BusinessEntityID;
        """.trimIndent()

        val statement = SqlServerHelper.getStatement(sql)
        if (statement is UpdateTable) {
            Assert.assertEquals(StatementType.UPDATE, statement.statementType)
            Assert.assertEquals(TableId("HumanResources", "Employee"), statement.tableId)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(TableId("HumanResources", "Employee1"), statement.inputTables.get(0))
        } else {
            Assert.fail()
        }
    }

    @Test
    fun mergeTest0() {
        val sql = """
            MERGE TargetProducts AS Target
            USING SourceProducts	AS Source
            ON Source.ProductID = Target.ProductID
            WHEN NOT MATCHED BY Target THEN
                INSERT (ProductID,ProductName, Price) 
                VALUES (Source.ProductID,Source.ProductName, Source.Price)
            WHEN MATCHED THEN UPDATE SET
                Target.ProductName	= Source.ProductName,
                Target.Price		= Source.Price
            WHEN NOT MATCHED BY Source THEN
                DELETE;
        """.trimIndent()

        val statement = SqlServerHelper.getStatement(sql)

        if (statement is MergeTable) {
            Assert.assertEquals(StatementType.MERGE, statement.statementType)
            Assert.assertEquals("TargetProducts", statement.targetTable.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(TableId("SourceProducts"), statement.inputTables.get(0))
        } else {
            Assert.fail()
        }
    }
}
