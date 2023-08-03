package com.linkedin.feathr.common.tensor;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Supported primitive types.
 *
 * They serve as basis for scalars - dimensions and values.
 *
 * Note: String types should only be used when the dimension is an actual text snippet.
 */
public enum Primitive implements Serializable, Representable {
    // A note to implementors:
    // if you find yourself using a switch on this enum, consider adding a new method to the enum instead.

    INT {
        @Override
        public int toInt(ReadableTuple tuple, int column) {
            return tuple.getInt(column);
        }

        @Override
        public long toLong(ReadableTuple tuple, int column) {
            return tuple.getInt(column);
        }

        @Override
        public float toFloat(ReadableTuple tuple, int column) {
            return tuple.getInt(column);
        }

        @Override
        public double toDouble(ReadableTuple tuple, int column) {
            return tuple.getInt(column);
        }

        @Override
        public boolean toBoolean(ReadableTuple tuple, int column) {
            return tuple.getInt(column) != 0;
        }

        @Override
        public String toString(ReadableTuple tuple, int column) {
            return String.valueOf(tuple.getInt(column));
        }

        @Override
        public Object toObject(ReadableTuple tuple, int column) {
            return tuple.getInt(column);
        }

        /**
         * Returns value serialized into a byte[] in BIG_ENDIAN byte order.
         * @param tuple Input tuple
         * @param column Column index
         * @return Value in byte[]
         */
        @Override
        public byte[] toBytes(ReadableTuple tuple, int column) {
            return ByteBuffer.allocate(Integer.BYTES).putInt(tuple.getInt(column)).array();
        }

        @Override
        public void from(Number value, WriteableTuple tuple, int column) {
            tuple.setInt(column, value.intValue());
        }

        @Override
        public void from(int value, WriteableTuple tuple, int column) {
            tuple.setInt(column, value);
        }

        @Override
        public void from(long value, WriteableTuple tuple, int column) {
            tuple.setInt(column, (int) value);
        }

        @Override
        public void from(float value, WriteableTuple tuple, int column) {
            tuple.setInt(column, (int) value);
        }

        @Override
        public void from(double value, WriteableTuple tuple, int column) {
            tuple.setInt(column, (int) value);
        }

        @Override
        public void from(boolean value, WriteableTuple tuple, int column) {
            tuple.setInt(column, value ? 1 : 0);
        }

        @Override
        public void from(String value, WriteableTuple tuple, int column) {
            tuple.setInt(column, Integer.parseInt(value));
        }

        /**
         * Stores value from its byte[] serialization in the input tuple. BIG_ENDIAN byte order
         * is expected in input byte[].
         * @param value Input value
         * @param tuple Input tuple to store the value
         * @param column Column index
         */
        @Override
        public void from(byte[] value, WriteableTuple tuple, int column) {
            tuple.setInt(column, ByteBuffer.wrap(value).getInt());
        }

        @Override
        public void copy(ReadableTuple fromTuple, int fromColumn, WriteableTuple toTuple, int toColumn) {
            toTuple.setInt(toColumn, fromTuple.getInt(fromColumn));
        }

        @Override
        public boolean equals(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return leftTuple.getInt(leftColumn) == rightTuple.getInt(rightColumn);
        }

        @Override
        public int compare(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return Integer.compare(leftTuple.getInt(leftColumn), rightTuple.getInt(rightColumn));
        }

        @Override
        public boolean canBeDenseDimension() {
            return true;
        }

        @Override
        public boolean canBeValue() {
            return true;
        }

        @Override
        public boolean isInstance(Object obj) {
            return obj instanceof Integer;
        }
    }, LONG {
        @Override
        public int toInt(ReadableTuple tuple, int column) {
            return (int) tuple.getLong(column);
        }

        @Override
        public long toLong(ReadableTuple tuple, int column) {
            return tuple.getLong(column);
        }

        @Override
        public float toFloat(ReadableTuple tuple, int column) {
            return tuple.getLong(column);
        }

        @Override
        public double toDouble(ReadableTuple tuple, int column) {
            return tuple.getLong(column);
        }

        @Override
        public boolean toBoolean(ReadableTuple tuple, int column) {
            return tuple.getLong(column) != 0L;
        }

        @Override
        public String toString(ReadableTuple tuple, int column) {
            return String.valueOf(tuple.getLong(column));
        }

        @Override
        public Object toObject(ReadableTuple tuple, int column) {
            return tuple.getLong(column);
        }

        /**
         * Returns value serialized into a byte[] in BIG_ENDIAN byte order.
         * @param tuple Input tuple
         * @param column Column index
         * @return Value in byte[]
         */
        @Override
        public byte[] toBytes(ReadableTuple tuple, int column) {
            return ByteBuffer.allocate(Long.BYTES).putLong(tuple.getLong(column)).array();
        }

        @Override
        public void from(Number value, WriteableTuple tuple, int column) {
            tuple.setLong(column, value.longValue());
        }

        @Override
        public void from(int value, WriteableTuple tuple, int column) {
            tuple.setLong(column, value);
        }

        @Override
        public void from(long value, WriteableTuple tuple, int column) {
            tuple.setLong(column, value);
        }

        @Override
        public void from(float value, WriteableTuple tuple, int column) {
            tuple.setLong(column, (long) value);
        }

        @Override
        public void from(double value, WriteableTuple tuple, int column) {
            tuple.setLong(column, (long) value);
        }

        @Override
        public void from(boolean value, WriteableTuple tuple, int column) {
            tuple.setLong(column, value ? 1L : 0L);
        }

        @Override
        public void from(String value, WriteableTuple tuple, int column) {
            tuple.setLong(column, Long.parseLong(value));
        }

        /**
         * Stores value from its byte[] serialization in the input tuple. BIG_ENDIAN byte order
         * is expected in input byte[].
         * @param value Input value
         * @param tuple Input tuple to store the value
         * @param column Column index
         */
        @Override
        public void from(byte[] value, WriteableTuple tuple, int column) {
            tuple.setLong(column, ByteBuffer.wrap(value).getLong());
        }

        @Override
        public void copy(ReadableTuple fromTuple, int fromColumn, WriteableTuple toTuple, int toColumn) {
            toTuple.setLong(toColumn, fromTuple.getLong(fromColumn));
        }

        @Override
        public boolean equals(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return leftTuple.getLong(leftColumn) == rightTuple.getLong(rightColumn);
        }

        @Override
        public int compare(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return Long.compare(leftTuple.getLong(leftColumn), rightTuple.getLong(rightColumn));
        }

        @Override
        public boolean canBeDenseDimension() {
            return true;
        }

        @Override
        public boolean canBeValue() {
            return true;
        }

        @Override
        public boolean isInstance(Object obj) {
            return obj instanceof Long;
        }
    }, FLOAT {
        @Override
        public int toInt(ReadableTuple tuple, int column) {
            return (int) tuple.getFloat(column);
        }

        @Override
        public long toLong(ReadableTuple tuple, int column) {
            return (long) tuple.getFloat(column);
        }

        @Override
        public float toFloat(ReadableTuple tuple, int column) {
            return tuple.getFloat(column);
        }

        @Override
        public double toDouble(ReadableTuple tuple, int column) {
            return tuple.getFloat(column);
        }

        @Override
        public boolean toBoolean(ReadableTuple tuple, int column) {
            return tuple.getFloat(column) != 0.0F;
        }

        @Override
        public String toString(ReadableTuple tuple, int column) {
            return String.valueOf(tuple.getFloat(column));
        }

        @Override
        public Object toObject(ReadableTuple tuple, int column) {
            return tuple.getFloat(column);
        }

        /**
         * Returns value serialized into a byte[] in BIG_ENDIAN byte order.
         * @param tuple Input tuple
         * @param column Column index
         * @return Value in byte[]
         */
        @Override
        public byte[] toBytes(ReadableTuple tuple, int column) {
            return ByteBuffer.allocate(Float.BYTES).putFloat(tuple.getFloat(column)).array();
        }

        @Override
        public void from(Number value, WriteableTuple tuple, int column) {
            tuple.setFloat(column, value.floatValue());
        }

        @Override
        public void from(int value, WriteableTuple tuple, int column) {
            tuple.setFloat(column, value);
        }

        @Override
        public void from(long value, WriteableTuple tuple, int column) {
            tuple.setFloat(column, value);
        }

        @Override
        public void from(float value, WriteableTuple tuple, int column) {
            tuple.setFloat(column, value);
        }

        @Override
        public void from(double value, WriteableTuple tuple, int column) {
            tuple.setFloat(column, (float) value);
        }

        @Override
        public void from(boolean value, WriteableTuple tuple, int column) {
            tuple.setFloat(column, value ? 1.0F : 0.0F);
        }

        @Override
        public void from(String value, WriteableTuple tuple, int column) {
            tuple.setFloat(column, Float.parseFloat(value));
        }

        /**
         * Stores value from its byte[] serialization in the input tuple. BIG_ENDIAN byte order
         * is expected in input byte[].
         * @param value Input value
         * @param tuple Input tuple to store the value
         * @param column Column index
         */
        @Override
        public void from(byte[] value, WriteableTuple tuple, int column) {
            tuple.setFloat(column, ByteBuffer.wrap(value).getFloat());
        }

        @Override
        public void copy(ReadableTuple fromTuple, int fromColumn, WriteableTuple toTuple, int toColumn) {
            toTuple.setFloat(toColumn, fromTuple.getFloat(fromColumn));
        }

        @Override
        public boolean equals(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return leftTuple.getFloat(leftColumn) == rightTuple.getFloat(rightColumn);
        }

        @Override
        public int compare(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return Float.compare(leftTuple.getFloat(leftColumn), rightTuple.getFloat(rightColumn));
        }

        @Override
        public boolean canBeDenseDimension() {
            return false;
        }

        @Override
        public boolean canBeValue() {
            return true;
        }

        @Override
        public boolean isInstance(Object obj) {
            return obj instanceof Float;
        }
    }, DOUBLE {
        @Override
        public int toInt(ReadableTuple tuple, int column) {
            return (int) tuple.getDouble(column);
        }

        @Override
        public long toLong(ReadableTuple tuple, int column) {
            return (long) tuple.getDouble(column);
        }

        @Override
        public float toFloat(ReadableTuple tuple, int column) {
            return (float) tuple.getDouble(column);
        }

        @Override
        public double toDouble(ReadableTuple tuple, int column) {
            return tuple.getDouble(column);
        }

        @Override
        public boolean toBoolean(ReadableTuple tuple, int column) {
            return tuple.getDouble(column) != 0.0D;
        }

        @Override
        public String toString(ReadableTuple tuple, int column) {
            return String.valueOf(tuple.getDouble(column));
        }

        @Override
        public Object toObject(ReadableTuple tuple, int column) {
            return tuple.getDouble(column);
        }

        /**
         * Returns value serialized into a byte[] in BIG_ENDIAN byte order.
         * @param tuple Input tuple
         * @param column Column index
         * @return Value in byte[]
         */
        @Override
        public byte[] toBytes(ReadableTuple tuple, int column) {
            return ByteBuffer.allocate(Double.BYTES).putDouble(tuple.getDouble(column)).array();
        }

        @Override
        public void from(Number value, WriteableTuple tuple, int column) {
            tuple.setDouble(column, value.doubleValue());
        }

        @Override
        public void from(int value, WriteableTuple tuple, int column) {
            tuple.setDouble(column, value);
        }

        @Override
        public void from(long value, WriteableTuple tuple, int column) {
            tuple.setDouble(column, value);
        }

        @Override
        public void from(float value, WriteableTuple tuple, int column) {
            tuple.setDouble(column, value);
        }

        @Override
        public void from(double value, WriteableTuple tuple, int column) {
            tuple.setDouble(column, value);
        }

        @Override
        public void from(boolean value, WriteableTuple tuple, int column) {
            tuple.setDouble(column, value ? 1.0D : 0.0D);
        }

        @Override
        public void from(String value, WriteableTuple tuple, int column) {
            tuple.setDouble(column, Double.parseDouble(value));
        }

        /**
         * Stores value from its byte[] serialization in the input tuple. BIG_ENDIAN byte order
         * is expected in input byte[].
         * @param value Input value
         * @param tuple Input tuple to store the value
         * @param column Column index
         */
        @Override
        public void from(byte[] value, WriteableTuple tuple, int column) {
            tuple.setDouble(column, ByteBuffer.wrap(value).getDouble());
        }

        @Override
        public void copy(ReadableTuple fromTuple, int fromColumn, WriteableTuple toTuple, int toColumn) {
            toTuple.setDouble(toColumn, fromTuple.getDouble(fromColumn));
        }

        @Override
        public boolean equals(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return leftTuple.getDouble(leftColumn) == rightTuple.getDouble(rightColumn);
        }

        @Override
        public int compare(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return Double.compare(leftTuple.getDouble(leftColumn), rightTuple.getDouble(rightColumn));
        }

        @Override
        public boolean canBeDenseDimension() {
            return false;
        }

        @Override
        public boolean canBeValue() {
            return true;
        }

        @Override
        public boolean isInstance(Object obj) {
            return obj instanceof Double;
        }
    }, BOOLEAN {
        @Override
        public int toInt(ReadableTuple tuple, int column) {
            return tuple.getBoolean(column) ? 1 : 0;
        }

        @Override
        public long toLong(ReadableTuple tuple, int column) {
            return tuple.getBoolean(column) ? 1L : 0L;
        }

        @Override
        public float toFloat(ReadableTuple tuple, int column) {
            return tuple.getBoolean(column) ? 1.0F : 0.0F;
        }

        @Override
        public double toDouble(ReadableTuple tuple, int column) {
            return tuple.getBoolean(column) ? 1.0D : 0.0D;
        }

        @Override
        public String toString(ReadableTuple tuple, int column) {
            return String.valueOf(tuple.getBoolean(column));
        }

        @Override
        public boolean toBoolean(ReadableTuple tuple, int column) {
            return tuple.getBoolean(column);
        }

        @Override
        public Object toObject(ReadableTuple tuple, int column) {
            return tuple.getBoolean(column);
        }

        /**
         * Returns value serialized into a byte[] with a single byte (0 for false, 1 for true).
         * @param tuple Input tuple
         * @param column Column index
         * @return Value in byte[]
         */
        @Override
        public byte[] toBytes(ReadableTuple tuple, int column) {
            return ByteBuffer.allocate(1).put((byte) (tuple.getBoolean(column) ? 1 : 0)).array();
        }

        @Override
        public void from(Number value, WriteableTuple tuple, int column) {
            tuple.setBoolean(column, value.intValue() != 0);
        }

        @Override
        public void from(int value, WriteableTuple tuple, int column) {
            tuple.setBoolean(column, value != 0);
        }

        @Override
        public void from(long value, WriteableTuple tuple, int column) {
            tuple.setBoolean(column, value != 0L);
        }

        @Override
        public void from(float value, WriteableTuple tuple, int column) {
            tuple.setBoolean(column, value != 0.0F);
        }

        @Override
        public void from(double value, WriteableTuple tuple, int column) {
            tuple.setBoolean(column, value != 0.0D);
        }

        @Override
        public void from(boolean value, WriteableTuple tuple, int column) {
            tuple.setBoolean(column, value);
        }

        @Override
        public void from(String value, WriteableTuple tuple, int column) {
            tuple.setBoolean(column, Boolean.parseBoolean(value));
        }

        /**
         * Stores value from its byte[] serialization in the input tuple. Only the leading byte
         * of the array is used; false if 0, true otherwise.
         * @param value Input value
         * @param tuple Input tuple to store the value
         * @param column Column index
         */
        @Override
        public void from(byte[] value, WriteableTuple tuple, int column) {
            tuple.setBoolean(column, (ByteBuffer.wrap(value).get() != (byte) 0));
        }

        @Override
        public void copy(ReadableTuple fromTuple, int fromColumn, WriteableTuple toTuple, int toColumn) {
            toTuple.setBoolean(toColumn, fromTuple.getBoolean(fromColumn));
        }

        @Override
        public boolean equals(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return leftTuple.getBoolean(leftColumn) == rightTuple.getBoolean(rightColumn);
        }

        @Override
        public int compare(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return Boolean.compare(leftTuple.getBoolean(leftColumn), rightTuple.getBoolean(rightColumn));
        }

        @Override
        public boolean canBeDenseDimension() {
            return true;
        }

        @Override
        public boolean canBeValue() {
            return true;
        }

        @Override
        public boolean isInstance(Object obj) {
            return obj instanceof Boolean;
        }
    }, STRING {
        @Override
        public int toInt(ReadableTuple tuple, int column) {
            return Integer.parseInt(tuple.getString(column));
        }

        @Override
        public long toLong(ReadableTuple tuple, int column) {
            return Long.parseLong(tuple.getString(column));
        }

        @Override
        public float toFloat(ReadableTuple tuple, int column) {
            return Float.parseFloat(tuple.getString(column));
        }

        @Override
        public double toDouble(ReadableTuple tuple, int column) {
            return Double.parseDouble(tuple.getString(column));
        }

        @Override
        public boolean toBoolean(ReadableTuple tuple, int column) {
            return Boolean.parseBoolean(tuple.getString(column));
        }

        @Override
        public String toString(ReadableTuple tuple, int column) {
            return tuple.getString(column);
        }

        @Override
        public Object toObject(ReadableTuple tuple, int column) {
            return tuple.getString(column);
        }

        /**
         * Returns value serialized into a byte[] using UTF-8 charset.
         * @param tuple Input tuple
         * @param column Column index
         * @return Value in byte[]
         */
        @Override
        public byte[] toBytes(ReadableTuple tuple, int column) {
            try {
                return tuple.getString(column).getBytes(DEFAULT_CHARSET);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding " + DEFAULT_CHARSET + " is not supported in Primitive.String", e);
            }
        }

        @Override
        public void from(Number value, WriteableTuple tuple, int column) {
            tuple.setString(column, value.toString());
        }

        @Override
        public void from(int value, WriteableTuple tuple, int column) {
            tuple.setString(column, String.valueOf(value));
        }

        @Override
        public void from(long value, WriteableTuple tuple, int column) {
            tuple.setString(column, String.valueOf(value));
        }

        @Override
        public void from(float value, WriteableTuple tuple, int column) {
            tuple.setString(column, String.valueOf(value));
        }

        @Override
        public void from(double value, WriteableTuple tuple, int column) {
            tuple.setString(column, String.valueOf(value));
        }

        @Override
        public void from(boolean value, WriteableTuple tuple, int column) {
            tuple.setString(column, String.valueOf(value));
        }

        @Override
        public void from(String value, WriteableTuple tuple, int column) {
            tuple.setString(column, value);
        }

        /**
         * Stores value from its byte[] serialization in the input tuple. UTF-8 charset is used to read bytes
         * into a String.
         * @param value Input value
         * @param tuple Input tuple to store the value
         * @param column Column index
         */
        @Override
        public void from(byte[] value, WriteableTuple tuple, int column) {
            try {
                tuple.setString(column, new String(value, DEFAULT_CHARSET));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding " + DEFAULT_CHARSET + " is not supported in Primitive.String", e);
            }
        }

        @Override
        public void copy(ReadableTuple fromTuple, int fromColumn, WriteableTuple toTuple, int toColumn) {
            toTuple.setString(toColumn, fromTuple.getString(fromColumn));
        }

        @Override
        public boolean equals(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return leftTuple.getString(leftColumn).equals(rightTuple.getString(rightColumn));
        }

        @Override
        public int compare(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return leftTuple.getString(leftColumn).compareTo(rightTuple.getString(rightColumn));
        }

        @Override
        public boolean canBeDenseDimension() {
            return false;
        }

        @Override
        public boolean canBeValue() {
            return true;
        }

        @Override
        public boolean isInstance(Object obj) {
            return obj instanceof String;
        }
    }, BYTES {
        @Override
        public int toInt(ReadableTuple tuple, int column) {
            return ByteBuffer.wrap(tuple.getBytes(column)).getInt();
        }

        @Override
        public long toLong(ReadableTuple tuple, int column) {
            return ByteBuffer.wrap(tuple.getBytes(column)).getLong();
        }

        @Override
        public float toFloat(ReadableTuple tuple, int column) {
            return ByteBuffer.wrap(tuple.getBytes(column)).getFloat();
        }

        @Override
        public double toDouble(ReadableTuple tuple, int column) {
            return ByteBuffer.wrap(tuple.getBytes(column)).getDouble();
        }

        /**
         * Checks if a tuple contains a byte array with a leading non-zero byte
         * @param tuple Input tuple
         * @param column Column index
         * @return true if tuple contains a byte array with a leading non-zero byte,
         *         false otherwise
         */
        @Override
        public boolean toBoolean(ReadableTuple tuple, int column) {
            return ByteBuffer.wrap(tuple.getBytes(column)).get() != (byte) 0;
        }

        @Override
        public String toString(ReadableTuple tuple, int column) {
            try {
                return new String(tuple.getBytes(column), DEFAULT_CHARSET);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding " + DEFAULT_CHARSET + " is not supported in Primitive.String", e);
            }
        }

        @Override
        public Object toObject(ReadableTuple tuple, int column) {
            return tuple.getBytes(column);
        }

        @Override
        public byte[] toBytes(ReadableTuple tuple, int column) {
            return tuple.getBytes(column);
        }

        @Override
        public void from(Number value, WriteableTuple tuple, int column) {
            tuple.setBytes(column, ByteBuffer.allocate(Double.BYTES).putDouble(value.doubleValue()).array());
        }

        @Override
        public void from(int value, WriteableTuple tuple, int column) {
            tuple.setBytes(column, ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
        }

        @Override
        public void from(long value, WriteableTuple tuple, int column) {
            tuple.setBytes(column, ByteBuffer.allocate(Long.BYTES).putLong(value).array());
        }

        @Override
        public void from(float value, WriteableTuple tuple, int column) {
            tuple.setBytes(column, ByteBuffer.allocate(Float.BYTES).putFloat(value).array());
        }

        @Override
        public void from(double value, WriteableTuple tuple, int column) {
            tuple.setBytes(column, ByteBuffer.allocate(Double.BYTES).putDouble(value).array());
        }

        @Override
        public void from(boolean value, WriteableTuple tuple, int column) {
            tuple.setBytes(column, new byte[]{(byte) (value ? 1 : 0)});
        }

        @Override
        public void from(String value, WriteableTuple tuple, int column) {
            try {
                tuple.setBytes(column, value.getBytes(DEFAULT_CHARSET));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding " + DEFAULT_CHARSET + " is not supported in Primitive.String", e);
            }
        }

        @Override
        public void from(byte[] value, WriteableTuple tuple, int column) {
            tuple.setBytes(column, value);
        }

        @Override
        public void copy(ReadableTuple fromTuple, int fromColumn, WriteableTuple toTuple, int toColumn) {
            toTuple.setBytes(toColumn, fromTuple.getBytes(fromColumn));
        }

        @Override
        public boolean equals(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            return Arrays.equals(leftTuple.getBytes(leftColumn), rightTuple.getBytes(rightColumn));
        }

        @Override
        public int compare(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn) {
            byte[] l = leftTuple.getBytes(leftColumn);
            byte[] r = rightTuple.getBytes(rightColumn);

            int i = 0;
            int j = 0;
            for (; i < l.length && j < r.length; i++, j++) {
                if (l[i] == r[j]) {
                    continue;
                }
                return Byte.compare(l[i], r[j]);
            }

            if (i == l.length && j == r.length) {
                return 0;
            } else if (j == r.length) {
                return 1;
            } else {
                return -1;
            }
        }

        @Override
        public boolean canBeDenseDimension() {
            return false;
        }

        @Override
        public boolean canBeValue() {
            return true;
        }

        @Override
        public boolean isInstance(Object obj) {
            return obj instanceof byte[];
        }
    };  // TBD: SHORT

    private static final String DEFAULT_CHARSET = "UTF-8";

    @Override
    public Primitive getRepresentation() {
        return this;
    }

    // Coercing methods: coerce from a value of this type stored in a column of a tuple to the requested one, avoiding boxing to Object.

    public abstract int toInt(ReadableTuple tuple, int column);

    public abstract long toLong(ReadableTuple tuple, int column);

    public abstract float toFloat(ReadableTuple tuple, int column);

    public abstract double toDouble(ReadableTuple tuple, int column);

    public abstract boolean toBoolean(ReadableTuple tuple, int column);

    public abstract String toString(ReadableTuple tuple, int column);

    /**
     * Returns value serialized into a byte[].
     * <p/>
     * For numeric types (int, long, float, double), BIG_ENDIAN byte order
     * is preserved.
     * <p/>
     * For string type, a UTF-8 charset is used to get bytes.
     * <p/>
     * For booleans, a single byte is returned in the array.
     * @param tuple Input tuple
     * @param column Column index
     * @return Value in byte[]
     */
    public abstract byte[] toBytes(ReadableTuple tuple, int column);

    public abstract Object toObject(ReadableTuple tuple, int column);

    public void from(Object value, WriteableTuple tuple, int column) {
        if (value instanceof Number) {
            from((Number) value, tuple, column);
        } else if (value instanceof String) {
            from((String) value, tuple, column);
        } else if (value instanceof Boolean) {
            from((boolean) value, tuple, column);
        } else if (value instanceof byte[]) {
            from((byte[]) value, tuple, column);
        } else {
            throw new IllegalArgumentException("Unsupported value class: " + value);
        }
    }

    // Coercing methods: coerce to a value of this type stored in a column of a tuple from the provided one, avoiding boxing to Object.

    public abstract void from(Number value, WriteableTuple tuple, int column);

    public abstract void from(int value, WriteableTuple tuple, int column);

    public abstract void from(long value, WriteableTuple tuple, int column);

    public abstract void from(float value, WriteableTuple tuple, int column);

    public abstract void from(double value, WriteableTuple tuple, int column);

    public abstract void from(boolean value, WriteableTuple tuple, int column);

    public abstract void from(String value, WriteableTuple tuple, int column);

    /**
     * Stores value from its byte[] serialization in the input tuple.
     * <p/>
     * For numeric types (int, long, float, double), BIG_ENDIAN byte order
     * is expected.
     * <p/>
     * For string type, a UTF-8 charset is used to construct the string.
     * <p/>
     * For booleans, only the leading byte of the array is checked.
     * @param value Input value
     * @param tuple Input tuple to store the value
     * @param column Column index
     */
    public abstract void from(byte[] value, WriteableTuple tuple, int column);

    /**
     * Copies data from fromTuple[fromColumn] to toTuple[toColumn]
     * Note: The representation of both fromColumn and toColumn is assumed to be the same as {@code this}
     * No validation on representation is performed
     * @param fromTuple {@link ReadableTuple} from which to copy
     * @param fromColumn column in fromTuple to copy
     * @param toTuple  {@link WriteableTuple} where the data would be copied
     * @param toColumn column in toTuple where the data would be copied
     */
    public abstract void copy(ReadableTuple fromTuple, int fromColumn, WriteableTuple toTuple, int toColumn);

    /**
     * Matches data at leftTuple[leftColumn] with data at rightTuple[rightColumn]
     * Note: The representation of both leftColumn and rightColumn is assumed to be the same as {@code this}
     * No validation on representation is performed
     * @param leftTuple left tuple from which a column will be matched
     * @param leftColumn column in leftTuple that will be matched
     * @param rightTuple right tuple from which a column will be matched
     * @param rightColumn column in right tuple that will be matched
     * @return true if data is equal, false otherwise
     */
    public abstract boolean equals(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn);

    /**
     * Compares data at leftTuple[leftColumn] with data at rightTuple[rightColumn]
     * Note: The representation of both leftColumn and rightColumn is assumed to be the same as {@code this}
     * No validation on representation is performed
     * @param leftTuple left tuple from which a column will be compared
     * @param leftColumn column in leftTuple that will be compared
     * @param rightTuple right tuple from which a column will be compared
     * @param rightColumn column in right tuple that will be compared
     * @return -1 if lefttuple[leftColumn] < rightTuple[rightColumn],
     *         0 if lefttuple[leftColumn] == rightTuple[rightColumn],
     *         1 if lefttuple[leftColumn] > rightTuple[rightColumn]
     */
    public abstract int compare(ReadableTuple leftTuple, int leftColumn, ReadableTuple rightTuple, int rightColumn);


    /**
     * @return if this primitive type can be used as a dimension in dense tensors.
     * In other words, is this type integral.
     * If true, the type MUST support {@link #toInt}, {@link #toLong}, and {@link #from} with both int and long value.
     */
    public abstract boolean canBeDenseDimension();

    /**
     * This method is called to check if the column Type of the value can be of the specified Primitive.
     * @return the Primitive of the column type
     */
    public abstract boolean canBeValue();

    /**
     * check whether the input object is of the primitive type without coercion.
     */
    public abstract boolean isInstance(Object obj);
}
