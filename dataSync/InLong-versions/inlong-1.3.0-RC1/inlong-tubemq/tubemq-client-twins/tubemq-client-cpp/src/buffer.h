// Modified from muduo project http://github.com/chenshuo/muduo
// @see https://github.com/chenshuo/muduo/blob/master/muduo/net/Buffer.h and https://github.com/chenshuo/muduo/blob/master/muduo/net/Buffer.cc

// Modified from evpp
// @see https://github.com/Qihoo360/evpp/blob/master/evpp/buffer.h, commit version b2535d7

#ifndef _TUBEMQ_BUFFER_H_
#define _TUBEMQ_BUFFER_H_

#include <arpa/inet.h>
#include <assert.h>
#include <stdint.h>
#include <string.h>

#include <algorithm>
#include <memory>
#include <string>

namespace tubemq {

class Buffer;
using BufferPtr = std::shared_ptr<Buffer>;

class Buffer {
 public:
  static const size_t kCheapPrependSize = 0;
  static const size_t kInitialSize = 8192;

  explicit Buffer(size_t initial_size = kInitialSize,
                  size_t reserved_prepend_size = kCheapPrependSize)
      : capacity_(reserved_prepend_size + initial_size),
        read_index_(reserved_prepend_size),
        write_index_(reserved_prepend_size),
        reserved_prepend_size_(reserved_prepend_size) {
    buffer_ = new char[capacity_];
    has_mem_ = true;
    assert(length() == 0);
    assert(WritableBytes() == initial_size);
    assert(PrependableBytes() == reserved_prepend_size);
  }

  ~Buffer() {
    if (has_mem_) {
      delete[] buffer_;
    }
    buffer_ = nullptr;
    capacity_ = 0;
  }

  std::string String() {
    char buf[1024];
    snprintf(buf, sizeof(buf),
             "buffer:%p,capacity:%ld,readindex:%ld,writeindex:%ld,prependsize:%ld,hasmem:%d",
             buffer_, capacity_, read_index_, write_index_, reserved_prepend_size_, has_mem_);
    return buf;
  }

  BufferPtr Slice() {
    auto buff = std::make_shared<Buffer>(*this);
    buff->has_mem_ = false;
    return buff;
  }

  void Swap(Buffer& rhs) {
    std::swap(buffer_, rhs.buffer_);
    std::swap(capacity_, rhs.capacity_);
    std::swap(read_index_, rhs.read_index_);
    std::swap(write_index_, rhs.write_index_);
    std::swap(reserved_prepend_size_, rhs.reserved_prepend_size_);
  }

  // Skip advances the reading index of the buffer
  void Skip(size_t len) {
    if (len < length()) {
      read_index_ += len;
    } else {
      Reset();
    }
  }

  // Retrieve advances the reading index of the buffer
  // Retrieve it the same as Skip.
  void Retrieve(size_t len) { Skip(len); }

  // Truncate discards all but the first n unread bytes from the buffer
  // but continues to use the same allocated storage.
  // It does nothing if n is greater than the length of the buffer.
  void Truncate(size_t n) {
    if (n == 0) {
      read_index_ = reserved_prepend_size_;
      write_index_ = reserved_prepend_size_;
    } else if (write_index_ > read_index_ + n) {
      write_index_ = read_index_ + n;
    }
  }

  // Reset resets the buffer to be empty,
  // but it retains the underlying storage for use by future writes.
  // Reset is the same as Truncate(0).
  void Reset() { Truncate(0); }

  // Increase the capacity of the container to a value that's greater
  // or equal to len. If len is greater than the current capacity(),
  // new storage is allocated, otherwise the method does nothing.
  void Reserve(size_t len) {
    if (capacity_ >= len + reserved_prepend_size_) {
      return;
    }

    grow(len + reserved_prepend_size_);
  }

  // Make sure there is enough memory space to append more data with length len
  void EnsureWritableBytes(size_t len) {
    if (WritableBytes() < len) {
      grow(len);
    }

    assert(WritableBytes() >= len);
  }

  // ToText appends char '\0' to buffer to convert the underlying data to a c-style string text.
  // It will not change the length of buffer.
  void ToText() {
    AppendInt8('\0');
    UnwriteBytes(1);
  }

  // Write
 public:
  void Write(const void* /*restrict*/ d, size_t len) {
    EnsureWritableBytes(len);
    memcpy(WriteBegin(), d, len);
    assert(write_index_ + len <= capacity_);
    write_index_ += len;
  }

  void Append(const char* /*restrict*/ d, size_t len) { Write(d, len); }

  void Append(const void* /*restrict*/ d, size_t len) { Write(d, len); }

  void AppendInt32(int32_t x) {
    int32_t be32 = htonl(x);
    Write(&be32, sizeof be32);
  }

  void AppendInt16(int16_t x) {
    int16_t be16 = htons(x);
    Write(&be16, sizeof be16);
  }

  void AppendInt8(int8_t x) { Write(&x, sizeof x); }

  void PrependInt32(int32_t x) {
    int32_t be32 = htonl(x);
    Prepend(&be32, sizeof be32);
  }

  void PrependInt16(int16_t x) {
    int16_t be16 = htons(x);
    Prepend(&be16, sizeof be16);
  }

  void PrependInt8(int8_t x) { Prepend(&x, sizeof x); }

  // Insert content, specified by the parameter, into the front of reading index
  void Prepend(const void* /*restrict*/ d, size_t len) {
    assert(len <= PrependableBytes());
    read_index_ -= len;
    const char* p = static_cast<const char*>(d);
    memcpy(begin() + read_index_, p, len);
  }

  void UnwriteBytes(size_t n) {
    assert(n <= length());
    write_index_ -= n;
  }

  void WriteBytes(size_t n) {
    assert(n <= WritableBytes());
    write_index_ += n;
  }

  // Read
 public:
  // Peek int32_t/int16_t/int8_t with network endian

  uint32_t ReadUint32() {
    uint32_t result = PeekUint32();
    Skip(sizeof result);
    return result;
  }

  int32_t ReadInt32() {
    int32_t result = PeekInt32();
    Skip(sizeof result);
    return result;
  }

  int16_t ReadInt16() {
    int16_t result = PeekInt16();
    Skip(sizeof result);
    return result;
  }

  int8_t ReadInt8() {
    int8_t result = PeekInt8();
    Skip(sizeof result);
    return result;
  }

  std::string ToString() const { return std::string(data(), length()); }

  // ReadByte reads and returns the next byte from the buffer.
  // If no byte is available, it returns '\0'.
  char ReadByte() {
    assert(length() >= 1);

    if (length() == 0) {
      return '\0';
    }

    return buffer_[read_index_++];
  }

  // UnreadBytes unreads the last n bytes returned
  // by the most recent read operation.
  void UnreadBytes(size_t n) {
    assert(n < read_index_);
    read_index_ -= n;
  }

  // Peek
 public:
  // Peek int64_t/int32_t/int16_t/int8_t with network endian

  uint32_t PeekUint32() const {
    assert(length() >= sizeof(uint32_t));
    uint32_t be32 = 0;
    ::memcpy(&be32, data(), sizeof be32);
    return ntohl(be32);
  }

  int32_t PeekInt32() const {
    assert(length() >= sizeof(int32_t));
    int32_t be32 = 0;
    ::memcpy(&be32, data(), sizeof be32);
    return ntohl(be32);
  }

  int16_t PeekInt16() const {
    assert(length() >= sizeof(int16_t));
    int16_t be16 = 0;
    ::memcpy(&be16, data(), sizeof be16);
    return ntohs(be16);
  }

  int8_t PeekInt8() const {
    assert(length() >= sizeof(int8_t));
    int8_t x = *data();
    return x;
  }

 public:
  // data returns a pointer of length Buffer.length() holding the unread portion of the buffer.
  // The data is valid for use only until the next buffer modification (that is,
  // only until the next call to a method like Read, Write, Reset, or Truncate).
  // The data aliases the buffer content at least until the next buffer modification,
  // so immediate changes to the slice will affect the result of future reads.
  const char* data() const { return buffer_ + read_index_; }

  char* WriteBegin() { return begin() + write_index_; }

  const char* WriteBegin() const { return begin() + write_index_; }

  // length returns the number of bytes of the unread portion of the buffer
  size_t length() const {
    assert(write_index_ >= read_index_);
    return write_index_ - read_index_;
  }

  // size returns the number of bytes of the unread portion of the buffer.
  // It is the same as length().
  size_t size() const { return length(); }

  // capacity returns the capacity of the buffer's underlying byte slice, that is, the
  // total space allocated for the buffer's data.
  size_t capacity() const { return capacity_; }

  size_t WritableBytes() const {
    assert(capacity_ >= write_index_);
    return capacity_ - write_index_;
  }

  size_t PrependableBytes() const { return read_index_; }

 public:
  char* begin() { return buffer_; }

  const char* begin() const { return buffer_; }

  void grow(size_t len) {
    if (!has_mem_) {
      return;
    }
    if (WritableBytes() + PrependableBytes() < len + reserved_prepend_size_) {
      // grow the capacity
      size_t n = (capacity_ << 1) + len;
      size_t m = length();
      char* d = new char[n];
      memcpy(d + reserved_prepend_size_, begin() + read_index_, m);
      write_index_ = m + reserved_prepend_size_;
      read_index_ = reserved_prepend_size_;
      capacity_ = n;
      delete[] buffer_;
      buffer_ = d;
    } else {
      // move readable data to the front, make space inside buffer
      assert(reserved_prepend_size_ < read_index_);
      size_t readable = length();
      memmove(begin() + reserved_prepend_size_, begin() + read_index_, length());
      read_index_ = reserved_prepend_size_;
      write_index_ = read_index_ + readable;
      assert(readable == length());
      assert(WritableBytes() >= len);
    }
  }

 private:
  char* buffer_;
  size_t capacity_;
  size_t read_index_;
  size_t write_index_;
  size_t reserved_prepend_size_;
  bool has_mem_{true};
};

}  // namespace tubemq
#endif /* _TUBEMQ_BUFFER_H_ */
