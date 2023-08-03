package com.platform.antlr.parser.common

enum class SqlType(val desc: String) {
    DML("Data Manipulation Language"),
    DDL("Data Definition Language"),
    DQL("Data Query Language"),
    DCL("Data Control Language"),
    TCL("Transaction Control Language")
}