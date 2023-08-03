package com.linkedin.feathr.common.tensorbuilder;

import com.linkedin.feathr.common.tensor.Primitive;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.linkedin.feathr.common.tensor.Primitive.*;

public class BufferUtils {
    // The charset to be used for strings.
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private BufferUtils() {
    }

    public static ByteBuffer encodeString(String string) {
        return BufferUtils.CHARSET.encode(string);
    }

    public static String decodeString(byte[] bytes) {
        return new String(bytes, CHARSET);
    }

    public static String decodeString(ByteBuffer buffer) {
        return CHARSET.decode(buffer).toString();
    }

    public static int compareBytes(byte[] left, byte[] right) {
        int i = 0;
        int j = 0;
        for (; i < left.length && j < right.length; i++, j++) {
            if (left[i] == right[j]) {
                continue;
            }
            return Byte.compare(left[i], right[j]);
        }

        if (i == left.length && j == right.length) {
            return 0;
        } else if (j == right.length) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Return size of primitive types in terms of bytes
     * @param valueType type for which size is required
     * @return size of the valueType in terms of bytes
     */
    public static int primitiveSize(Primitive valueType) {
        switch (valueType) {
            case INT:
                return Integer.BYTES;
            case DOUBLE:
                return Double.BYTES;
            case LONG:
                return Long.BYTES;
            case FLOAT:
                return Float.BYTES;
            case BOOLEAN:
                return 1;
            default:
                throw new IllegalArgumentException("Cannot create ByteBuffer with value type: " + valueType);
        }
    }
    /**
     * Creates a byteBuffer of specified valueType from a collection of numbers or strings.
     * @param values a collection of values; Currently only {@link Number} and {@link String} values are supported
     * @param valueType numbers are coerced to this type before being added to bytebuffer
     * @return ByteBuffer containing all numbers coerced into specified valueType
     */
    public static ByteBuffer createByteBuffer(List<?> values, Primitive valueType) {
        int count = values.size();
        boolean hasVariableSize = valueType == STRING || valueType == BYTES;
        // Cannot allocate the buffer for string-like objects at this moment, as the calculation of the size is non-trivial.
        // It is done later in the corresponding case of the switch.
        ByteBuffer byteBuffer = hasVariableSize ? null : ByteBuffer.allocate(count * primitiveSize(valueType));
        switch (valueType) {
            case INT:
                for (Number v : (List<Number>) values) {
                    byteBuffer.putInt(v.intValue());
                }
                break;
            case DOUBLE:
                for (Number v : (List<Number>) values) {
                    byteBuffer.putDouble(v.doubleValue());
                }
                break;
            case LONG:
                for (Number v : (List<Number>) values) {
                    byteBuffer.putLong(v.longValue());
                }
                break;
            case FLOAT:
                for (Number v : (List<Number>) values) {
                    byteBuffer.putFloat(v.floatValue());
                }
                break;
            case BOOLEAN:
                for (Object v : values) {
                    boolean aBoolean;
                    if (v instanceof Boolean) {
                        aBoolean = (Boolean) v;
                    } else if (v instanceof Number) {
                        aBoolean = ((Number) v).doubleValue() != 0.0D;
                    } else {
                        throw new IllegalArgumentException("Cannot create BOOLEAN buffer from value element type " + v.getClass());
                    }
                    byteBuffer.put(aBoolean ? (byte) 1 : (byte) 0);
                }
                break;
            case STRING:
                if (count == 0) {
                    byteBuffer = ByteBuffer.allocate(0);
                } else {
                    // Hack, the cleaner way is for this method to only work with strings, but this creates even more overhead for deserializer.

                    Object first = values.get(0);
                    if (first instanceof ByteBuffer) {
                        byteBuffer = combineByteBuffers((List<ByteBuffer>) values);
                    } else if (first instanceof String) {
                        List<String> sources = (List<String>) values;
                        List<ByteBuffer> buffers = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            buffers.add(BufferUtils.encodeString(sources.get(i)));
                        }
                        byteBuffer = combineByteBuffers(buffers);
                    } else {
                        throw new IllegalArgumentException("Cannot create ByteBuffer from value class: " + first.getClass());
                    }
                }
                break;
            case BYTES:
                if (count == 0) {
                    byteBuffer = ByteBuffer.allocate(0);
                } else {
                    Object first = values.get(0);
                    if (first instanceof ByteBuffer) {
                        byteBuffer = combineByteBuffers((List<ByteBuffer>) values);
                    } else if (first instanceof byte[]) {
                        List<byte[]> sources = (List<byte[]>) values;
                        List<ByteBuffer> buffers = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            buffers.add(ByteBuffer.wrap(sources.get(i)));
                        }
                        byteBuffer = combineByteBuffers(buffers);
                    } else {
                        throw new IllegalArgumentException("Cannot create ByteBuffer from value class: " + first.getClass());
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Cannot create ByteBuffer with value type: " + valueType);
        }
        return byteBuffer;
    }

    /**
     * Creates a ByteBuffer of specified valueType from an array of floats
     * @param floats an array of floats
     * @param valueType floats are coerced to this type before being added to ByteBuffer
     * @return ByteBuffer containing all floats coerced into specified valueType
     */
    public static ByteBuffer createByteBuffer(float[] floats, Primitive valueType) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(floats.length * primitiveSize(valueType));
        switch (valueType) {
            case INT:
                for (float aFloat : floats) {
                    byteBuffer.putInt((int) aFloat);
                }
                break;
            case DOUBLE:
                for (float aFloat : floats) {
                    byteBuffer.putDouble(aFloat);
                }
                break;
            case LONG:
                for (float aFloat : floats) {
                    byteBuffer.putLong((long) aFloat);
                }
                break;
            case FLOAT:
                byteBuffer.asFloatBuffer().put(floats);
                break;
            case BOOLEAN:
                for (float aFloat : floats) {
                    byteBuffer.put((aFloat == 0.0f) ? (byte) 0 : (byte) 1);
                }
                break;
            default:
                throw new IllegalArgumentException("Cannot create ByteBuffer with value type: " + valueType);
        }
        return byteBuffer;
    }

    /**
     * Creates a ByteBuffer of specified valueType from an array of longs
     * @param longs an array of longs
     * @param valueType longs are coerced to this type before being added to ByteBuffer
     * @return ByteBuffer containing all longs coerced into specified valueType
     */
    public static ByteBuffer createByteBuffer(long[] longs, Primitive valueType) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(longs.length * primitiveSize(valueType));
        switch (valueType) {
            case INT:
                for (long aLong : longs) {
                    byteBuffer.putInt((int) aLong);
                }
                break;
            case DOUBLE:
                for (long aLong : longs) {
                    byteBuffer.putDouble(aLong);
                }
                break;
            case LONG:
                byteBuffer.asLongBuffer().put(longs);
                break;
            case FLOAT:
                for (long aLong : longs) {
                    byteBuffer.putFloat(aLong);
                }
                break;
            case BOOLEAN:
                for (long aLong : longs) {
                    byteBuffer.put((aLong == 0L) ? (byte) 0 : (byte) 1);
                }
                break;
            default:
                throw new IllegalArgumentException("Cannot create ByteBuffer with value type: " + valueType);
        }
        return byteBuffer;
    }

    /**
     * Creates a ByteBuffer of specified valueType from an array of ints
     * @param ints an array of ints
     * @param valueType ints are coerced to this type before being added to ByteBuffer
     * @return ByteBuffer containing all ints coerced into specified valueType
     */
    public static ByteBuffer createByteBuffer(int[] ints, Primitive valueType) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * primitiveSize(valueType));
        switch (valueType) {
            case INT:
                byteBuffer.asIntBuffer().put(ints);
                break;
            case DOUBLE:
                for (int anInt : ints) {
                    byteBuffer.putDouble(anInt);
                }
                break;
            case LONG:
                for (int anInt : ints) {
                    byteBuffer.putLong(anInt);
                }
                break;
            case FLOAT:
                for (int anInt : ints) {
                    byteBuffer.putFloat(anInt);
                }
                break;
            case BOOLEAN:
                for (float anInt : ints) {
                    byteBuffer.put((anInt == 0) ? (byte) 0 : (byte) 1);
                }
                break;
            default:
                throw new IllegalArgumentException("Cannot create ByteBuffer with value type: " + valueType);
        }
        return byteBuffer;
    }

    /**
     * Creates a ByteBuffer of specified valueType from an array of doubles
     * @param doubles an array of doubles
     * @param valueType doubles are coerced to this type before being added to ByteBuffer
     * @return ByteBuffer containing all doubles coerced into specified valueType
     */
    public static ByteBuffer createByteBuffer(double[] doubles, Primitive valueType) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(doubles.length * primitiveSize(valueType));
        switch (valueType) {
            case INT:
                for (double aDouble : doubles) {
                    byteBuffer.putInt((int) aDouble);
                }
                break;
            case DOUBLE:
                byteBuffer.asDoubleBuffer().put(doubles);
                break;
            case LONG:
                for (double aDouble : doubles) {
                    byteBuffer.putLong((long) aDouble);
                }
                break;
            case FLOAT:
                for (double aDouble : doubles) {
                    byteBuffer.putFloat((float) aDouble);
                }
                break;
            case BOOLEAN:
                for (double aDouble : doubles) {
                    byteBuffer.put((aDouble == 0.0D) ? (byte) 0 : (byte) 1);
                }
                break;
            default:
                throw new IllegalArgumentException("Cannot create ByteBuffer with value type: " + valueType);
        }
        return byteBuffer;
    }

    public static ByteBuffer createByteBuffer(boolean[] booleans, Primitive valueType) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(booleans.length * primitiveSize(valueType));
        switch (valueType) {
            case INT:
                for (boolean aBoolean : booleans) {
                    byteBuffer.putInt(aBoolean ? 1 : 0);
                }
                break;
            case LONG:
                for (boolean aBoolean : booleans) {
                    byteBuffer.putLong(aBoolean ? 1L : 0L);
                }
                break;
            case FLOAT:
                for (boolean aBoolean : booleans) {
                    byteBuffer.putFloat(aBoolean ? 1.0F : 0.0F);
                }
                break;
            case DOUBLE:
                for (boolean aBoolean : booleans) {
                    byteBuffer.putDouble(aBoolean ? 1.0D : 0.0D);
                }
                break;
            case BOOLEAN:
                for (boolean aBoolean : booleans) {
                    byteBuffer.put(aBoolean ? (byte) 1 : (byte) 0);
                }
                break;
            default:
                throw new IllegalArgumentException("Cannot create ByteBuffer for boolean[] coercing to value type " + valueType);
        }
        return byteBuffer;
    }

    private static ByteBuffer combineByteBuffers(List<ByteBuffer> sources) {
        int count = sources.size();
        // Calculate the size of the buffer by adding:
        // 1. sizes of varint encodings of lengths
        // 2. lengths themselves
        int size = Long.BYTES * count;
        for (int i = 0; i < count; i++) {
            int length = sources.get(i).remaining();
            size += varintLength(length);
            size += length;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(size);

        int offset = Long.BYTES * count;
        for (int i = 0; i < count; i++) {
            byteBuffer.putLong(offset);
            ByteBuffer source = sources.get(i);
            int length = source.remaining();
            offset += varintLength(length);
            offset += length;
        }

        for (int i = 0; i < count; i++) {
            ByteBuffer source = sources.get(i);
            int length = source.remaining();
            encodeVarint(byteBuffer, length);
            // A view avoids modifying the state of the source buffer when copying.
            ByteBuffer view = source.asReadOnlyBuffer();
            byteBuffer.put(view);
        }
        return byteBuffer;
    }

    /**
     * Based on VarintLength in TensorFlow:
     * https://github.com/tensorflow/tensorflow/blob/master/tensorflow/core/lib/core/coding.cc#L127
     *
     * Note that Java int is 32 bit, so this code is not fully identical.
     */
    public static int varintLength(int value) {
        int len = 1;
        int v = value;
        while (v >= 0x80) {
            v >>= 7;
            len++;
        }
        return len;
    }
    /**
     * Based on EncodeVarint64 in TensorFlow:
     * https://github.com/tensorflow/tensorflow/blob/master/tensorflow/core/lib/core/coding.cc#L110
     *
     * Note that Java int is 32 bit, so this code is not fully identical.
     * @return the length of the encoding.
     */
    public static int encodeVarint(ByteBuffer buffer, int value) {
        int len = 1;
        int v = value;
        while (v >= 0x80) {
            buffer.put((byte) ((v & 0x7F) | 0x80));
            v >>= 7;
            len++;
        }
        buffer.put((byte) v);
        return len;
    }

    public static int decodeVarint(ByteBuffer buffer) {
        int v = 0;
        int shift = 0;
        while (true) {
            byte b = buffer.get();
            v |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return v;
            }
            shift += 7;
        }
    }
}
