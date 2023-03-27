const pointer = require("json-pointer");

export const resolveRef = (obj, schema) => {
    return Object.fromEntries(
        Object.entries(obj).flatMap(([k, v]) => {
          if (k === "$ref" && typeof v === "string") {
            return Object.entries(
              pointer.get(schema, v.replace('#',''))
            );
          } else if (!v || typeof v !== "object" || Array.isArray(v)) {
            return [[k, v]];
          } else {
            return [[k, resolveRef(v, schema)]];
          }
        })
      );
  };

export const getObject = (obj, schema, value) =>{
  pointer.set(obj, schema, value)
}