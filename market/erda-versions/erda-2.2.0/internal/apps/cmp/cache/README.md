CmpCache
------------

cache implement lru limited by memory usage. keys in CmpCache sorted by timestamp and lazy remove. 

- [`entry`](#entry)
  store value of cache data as Value type. data only support `Int` | `String` | `Float` | `Bool` | `UnsignedValue`

- `pair`
  contains key , value ,overdue timestamp

- `segment`
  cache contains 256 segments. key hash to uint64 and assigned to specific segment

- `store`
  contains segments , locks of each segment ,and a temporary slice for hash


- benchmark.

  | method-duration  | ns/op |
                    | ------------ | ---- |
  | BenchmarkLRU_Rand  | 306 |
  | BenchmarkLRU_Freq  | 278 |
  | BenchmarkLRU_FreqParallel-8 | 148 |


- Interface

    - ``Remove(key string) error``

    - ``WriteMulti(pairs map[string]Values) error``

    - ``Write(key string, value Values,overdueTimeStamp int64) error``

      Add key value pair in cache.
    - ``IncreaseSize(size int64)``
      
      Increase capacity of memory thar cache could use
    - ``DecrementSize(size int64) error``
      
      Decrease capacity of memory thar cache could use
    - ``Get(key string) (Values,bool, error)``
      
      Return value that cache stored . The second return value is key expired or not. 

