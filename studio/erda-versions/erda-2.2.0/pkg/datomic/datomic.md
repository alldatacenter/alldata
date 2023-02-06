datomic
=======

基于 etcd 提供分布式计数器

Usage
-----

    dint, err := New("key")
    // handle err..

    // use Store to reset value to 0
    succ, err := dint.Store(func(old uint64) bool {
        if old >= 10000 {
            return true
        }
        return false
    }, 0)
    // handle err..

    old, new, err := dint.Add(10)
    // handle err..

    err := dint.Clear()
    // handle err..
