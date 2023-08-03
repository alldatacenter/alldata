import json

TEMPERATURE_MEASUREMENT = "atmosphere temperature"
HUMIDITY_MEASUREMENT = "atmosphere humidity"


def check_regular_measurement_exception(measurement_name, measurement_value):
    exception_message = ""
    if measurement_name == TEMPERATURE_MEASUREMENT:
        if int(measurement_value) > 35:
            exception_message = "temperature is too high"
    elif measurement_name == HUMIDITY_MEASUREMENT:
        if int(measurement_value) > 60:
            exception_message = "humidity is too high"

    return exception_message


def humidity(raw_data):
    new_data = []
    # raw_loaded = json.load(raw_data)
    # translate the raw data to new data
    entities = raw_data["entity"]
    for i in range(len(entities)):
        new_data_entry = {"code": entities[i]["deviceId"],
                          "name": entities[i]["eName"],
                          "val": entities[i]["eValue"],
                          "unit": entities[i]["eUnit"],
                          "exception": check_regular_measurement_exception(entities[i]["eName"], entities[i]["eValue"])}
        new_data.append(new_data_entry)
    return new_data
