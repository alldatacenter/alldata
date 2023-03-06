class SchemaComparator:
    def __init__(self, historic_schema, measured_schema):
        self.historic_schema = historic_schema
        self.measured_schema = measured_schema

        self.schema_column_deletions = []
        self.schema_column_additions = []
        self.schema_column_type_changes = {}
        self.schema_column_index_changes = {}

        self.__compute_schema_changes()

    def __compute_schema_changes(self):
        column_types = {column["name"]: column["type"] for column in self.measured_schema}
        column_indexes = {column["name"]: index for index, column in enumerate(self.measured_schema)}

        previous_column_types = {column["name"]: column["type"] for column in self.historic_schema}
        previous_column_indexes = {column["name"]: index for index, column in enumerate(self.historic_schema)}
        for previous_column in previous_column_types:
            if previous_column not in column_types:
                self.schema_column_deletions.append(previous_column)
        for column in column_types:
            if column not in previous_column_types:
                self.schema_column_additions.append(column)
        for column in column_types:
            if column in previous_column_types:
                type = column_types[column]
                previous_type = previous_column_types[column]
                if type != previous_type:
                    self.schema_column_type_changes[column] = {
                        "previous_type": previous_type,
                        "new_type": type,
                    }
                index = column_indexes[column]
                previous_index = previous_column_indexes[column]
                if index != previous_index:
                    self.schema_column_index_changes[column] = {
                        "previous_index": previous_index,
                        "new_index": index,
                    }
