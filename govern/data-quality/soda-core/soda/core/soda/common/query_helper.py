from __future__ import annotations

import re

import sqlparse


def parse_columns_from_query(query: str) -> list[str]:
    # Handle non-standard sql syntax caveats before parsing.
    # Remove sqlserver TOP from select statement.
    query = re.sub(r"(?i)top(\(.*\)|\s+\d+)", "", query)

    columns = []
    statements = sqlparse.split(query)

    for statement in statements:
        column_lines = None
        start_looking_for_columns = False
        parsed = sqlparse.parse(statement)[0]
        for token in parsed.tokens:
            if token.ttype == sqlparse.tokens.Keyword.DML and token.value.lower() == "select":
                start_looking_for_columns = True

            # This is now after a "select", look for a) no type which is list of columns or b) wildcard which is "*"
            if start_looking_for_columns and (token.ttype == None or token.ttype == sqlparse.tokens.Token.Wildcard):
                column_lines = token.value
                break
        # Remove newlines, double whitespaces and split by colon.
        column_lines = " ".join(column_lines.replace("\n", "").split()).split(",")

        # Strip each "column statement" of whitespace, remove everything after the first whitespace and keep everything after first dot.
        for column in column_lines:
            column, _, _ = column.strip().partition(" ")
            if "." in column:
                _, _, column = column.strip().partition(".")
            columns.append(column)

    return columns
