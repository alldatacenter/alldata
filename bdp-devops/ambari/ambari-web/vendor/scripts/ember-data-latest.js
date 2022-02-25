(function() {
window.DS = Ember.Namespace.create({
  CURRENT_API_REVISION: 4
});

})();



(function() {
var get = Ember.get, set = Ember.set;

/**
  A record array is an array that contains records of a certain type. The record
  array materializes records as needed when they are retrieved for the first
  time. You should not create record arrays yourself. Instead, an instance of
  DS.RecordArray or its subclasses will be returned by your application's store
  in response to queries.
*/

DS.RecordArray = Ember.ArrayProxy.extend({

  /**
    The model type contained by this record array.

    @type DS.Model
  */
  type: null,

  // The array of client ids backing the record array. When a
  // record is requested from the record array, the record
  // for the client id at the same index is materialized, if
  // necessary, by the store.
  content: null,

  // The store that created this record array.
  store: null,

  objectAtContent: function(index) {
    var content = get(this, 'content'),
        clientId = content.objectAt(index),
        store = get(this, 'store');

    if (clientId !== undefined) {
      return store.findByClientId(get(this, 'type'), clientId);
    }
  }
});

})();



(function() {
var get = Ember.get;

DS.FilteredRecordArray = DS.RecordArray.extend({
  filterFunction: null,

  replace: function() {
    var type = get(this, 'type').toString();
    throw new Error("The result of a client-side filter (on " + type + ") is immutable.");
  },

  updateFilter: Ember.observer(function() {
    var store = get(this, 'store');
    store.updateRecordArrayFilter(this, get(this, 'type'), get(this, 'filterFunction'));
  }, 'filterFunction')
});

})();



(function() {
var get = Ember.get, set = Ember.set;

DS.AdapterPopulatedRecordArray = DS.RecordArray.extend({
  query: null,
  isLoaded: false,

  replace: function() {
    var type = get(this, 'type').toString();
    throw new Error("The result of a server query (on " + type + ") is immutable.");
  },

  load: function(array) {
    var store = get(this, 'store'), type = get(this, 'type');

    var clientIds = store.loadMany(type, array).clientIds;

    this.beginPropertyChanges();
    set(this, 'content', Ember.A(clientIds));
    set(this, 'isLoaded', true);
    this.endPropertyChanges();
  }
});


})();



(function() {
var get = Ember.get, set = Ember.set, guidFor = Ember.guidFor;

var Set = function() {
  this.hash = {};
  this.list = [];
};

Set.prototype = {
  add: function(item) {
    var hash = this.hash,
        guid = guidFor(item);

    if (hash.hasOwnProperty(guid)) { return; }

    hash[guid] = true;
    this.list.push(item);
  },

  remove: function(item) {
    var hash = this.hash,
        guid = guidFor(item);

    if (!hash.hasOwnProperty(guid)) { return; }

    delete hash[guid];
    var list = this.list,
        index = Ember.EnumerableUtils.indexOf(this, item);

    list.splice(index, 1);
  },

  isEmpty: function() {
    return this.list.length === 0;
  }
};

var LoadedState = Ember.State.extend({
  recordWasAdded: function(manager, record) {
    var dirty = manager.dirty, observer;
    dirty.add(record);

    observer = function() {
      if (!get(record, 'isDirty')) {
        record.removeObserver('isDirty', observer);
        manager.send('childWasSaved', record);
      }
    };

    record.addObserver('isDirty', observer);
  },

  recordWasRemoved: function(manager, record) {
    var dirty = manager.dirty, observer;
    dirty.add(record);

    observer = function() {
      record.removeObserver('isDirty', observer);
      if (!get(record, 'isDirty')) { manager.send('childWasSaved', record); }
    };

    record.addObserver('isDirty', observer);
  }
});

var states = {
  loading: Ember.State.create({
    isLoaded: false,
    isDirty: false,

    loadedRecords: function(manager, count) {
      manager.decrement(count);
    },

    becameLoaded: function(manager) {
      manager.transitionTo('clean');
    }
  }),

  clean: LoadedState.create({
    isLoaded: true,
    isDirty: false,

    recordWasAdded: function(manager, record) {
      this._super(manager, record);
      manager.goToState('dirty');
    },

    update: function(manager, clientIds) {
      var manyArray = manager.manyArray;
      set(manyArray, 'content', clientIds);
    }
  }),

  dirty: LoadedState.create({
    isLoaded: true,
    isDirty: true,

    childWasSaved: function(manager, child) {
      var dirty = manager.dirty;
      dirty.remove(child);

      if (dirty.isEmpty()) { manager.send('arrayBecameSaved'); }
    },

    arrayBecameSaved: function(manager) {
      manager.goToState('clean');
    }
  })
};

DS.ManyArrayStateManager = Ember.StateManager.extend({
  manyArray: null,
  initialState: 'loading',
  states: states,

  /**
   This number is used to keep track of the number of outstanding
   records that must be loaded before the array is considered
   loaded. As results stream in, this number is decremented until
   it becomes zero, at which case the `isLoaded` flag will be set
   to true
  */
  counter: 0,

  init: function() {
    this._super();
    this.dirty = new Set();
    this.counter = get(this, 'manyArray.length');
  },

  decrement: function(count) {
    var counter = this.counter = this.counter - count;

    Ember.assert("Somehow the ManyArray loaded counter went below 0. This is probably an ember-data bug. Please report it at https://github.com/emberjs/data/issues", counter >= 0);

    if (counter === 0) {
      this.send('becameLoaded');
    }
  }
});

})();



(function() {
var get = Ember.get, set = Ember.set;

DS.ManyArray = DS.RecordArray.extend({
  init: function() {
    set(this, 'stateManager', DS.ManyArrayStateManager.create({ manyArray: this }));

    return this._super();
  },

  parentRecord: null,

  isDirty: Ember.computed(function() {
    return get(this, 'stateManager.currentState.isDirty');
  }).property('stateManager.currentState').cacheable(),

  isLoaded: Ember.computed(function() {
    return get(this, 'stateManager.currentState.isLoaded');
  }).property('stateManager.currentState').cacheable(),

  send: function(event, context) {
    this.get('stateManager').send(event, context);
  },

  fetch: function() {
    var clientIds = get(this, 'content'),
        store = get(this, 'store'),
        type = get(this, 'type');

    store.fetchUnloadedClientIds(type, clientIds);
  },

  // Overrides Ember.Array's replace method to implement
  replaceContent: function(index, removed, added) {
    var parentRecord = get(this, 'parentRecord');
    var pendingParent = parentRecord && !get(parentRecord, 'id');
    var stateManager = get(this, 'stateManager');

    // Map the array of record objects into an array of  client ids.
    added = added.map(function(record) {
      Ember.assert("You can only add records of " + (get(this, 'type') && get(this, 'type').toString()) + " to this association.", !get(this, 'type') || (get(this, 'type') === record.constructor));

      // If the record to which this many array belongs does not yet
      // have an id, notify the newly-added record that it must wait
      // for the parent to receive an id before the child can be
      // saved.
      if (pendingParent) {
        record.send('waitingOn', parentRecord);
      }

      var oldParent = this.assignInverse(record, parentRecord);

      record.get('transaction')
        .relationshipBecameDirty(record, oldParent, parentRecord);

      stateManager.send('recordWasAdded', record);

      return record.get('clientId');
    }, this);

    var store = this.store;

    var len = index+removed, record;
    for (var i = index; i < len; i++) {
      // TODO: null out inverse FK
      record = this.objectAt(i);
      var oldParent = this.assignInverse(record, parentRecord, true);

      record.get('transaction')
        .relationshipBecameDirty(record, parentRecord, null);

      // If we put the child record into a pending state because
      // we were waiting on the parent record to get an id, we
      // can tell the child it no longer needs to wait.
      if (pendingParent) {
        record.send('doneWaitingOn', parentRecord);
      }

      stateManager.send('recordWasAdded', record);
    }

    this._super(index, removed, added);
  },

  assignInverse: function(record, parentRecord, remove) {
    var associationMap = get(record.constructor, 'associations'),
        possibleAssociations = associationMap.get(parentRecord.constructor),
        possible, actual, oldParent;

    if (!possibleAssociations) { return; }

    for (var i = 0, l = possibleAssociations.length; i < l; i++) {
      possible = possibleAssociations[i];

      if (possible.kind === 'belongsTo') {
        actual = possible;
        break;
      }
    }

    if (actual) {
      oldParent = get(record, actual.name);
      set(record, actual.name, remove ? null : parentRecord);
      return oldParent;
    }
  },

  // Create a child record within the parentRecord
  createRecord: function(hash, transaction) {
    var parentRecord = get(this, 'parentRecord'),
        store = get(parentRecord, 'store'),
        type = get(this, 'type'),
        record;

    transaction = transaction || get(parentRecord, 'transaction');

    record = store.createRecord.call(store, type, hash, transaction);
    this.pushObject(record);

    return record;
  }
});

})();



(function() {

})();



(function() {
var get = Ember.get, set = Ember.set, fmt = Ember.String.fmt,
    removeObject = Ember.EnumerableUtils.removeObject;

/**
  A transaction allows you to collect multiple records into a unit of work
  that can be committed or rolled back as a group.

  For example, if a record has local modifications that have not yet
  been saved, calling `commit()` on its transaction will cause those
  modifications to be sent to the adapter to be saved. Calling
  `rollback()` on its transaction would cause all of the modifications to
  be discarded and the record to return to the last known state before
  changes were made.

  If a newly created record's transaction is rolled back, it will
  immediately transition to the deleted state.

  If you do not explicitly create a transaction, a record is assigned to
  an implicit transaction called the default transaction. In these cases,
  you can treat your application's instance of `DS.Store` as a transaction
  and call the `commit()` and `rollback()` methods on the store itself.

  Once a record has been successfully committed or rolled back, it will
  be moved back to the implicit transaction. Because it will now be in
  a clean state, it can be moved to a new transaction if you wish.

  ### Creating a Transaction

  To create a new transaction, call the `transaction()` method of your
  application's `DS.Store` instance:

      var transaction = App.store.transaction();

  This will return a new instance of `DS.Transaction` with no records
  yet assigned to it.

  ### Adding Existing Records

  Add records to a transaction using the `add()` method:

      record = App.store.find(Person, 1);
      transaction.add(record);

  Note that only records whose `isDirty` flag is `false` may be added
  to a transaction. Once modifications to a record have been made
  (its `isDirty` flag is `true`), it is not longer able to be added to
  a transaction.

  ### Creating New Records

  Because newly created records are dirty from the time they are created,
  and because dirty records can not be added to a transaction, you must
  use the `createRecord()` method to assign new records to a transaction.

  For example, instead of this:

    var transaction = store.transaction();
    var person = Person.createRecord({ name: "Steve" });

    // won't work because person is dirty
    transaction.add(person);

  Call `createRecord()` on the transaction directly:

    var transaction = store.transaction();
    transaction.createRecord(Person, { name: "Steve" });

  ### Asynchronous Commits

  Typically, all of the records in a transaction will be committed
  together. However, new records that have a dependency on other new
  records need to wait for their parent record to be saved and assigned an
  ID. In that case, the child record will continue to live in the
  transaction until its parent is saved, at which time the transaction will
  attempt to commit again.

  For this reason, you should not re-use transactions once you have committed
  them. Always make a new transaction and move the desired records to it before
  calling commit.
*/

DS.Transaction = Ember.Object.extend({
  /**
    @private

    Creates the bucket data structure used to segregate records by
    type.
  */
  init: function() {
    set(this, 'buckets', {
      clean:    Ember.Map.create(),
      created:  Ember.Map.create(),
      updated:  Ember.Map.create(),
      deleted:  Ember.Map.create(),
      inflight: Ember.Map.create()
    });

    this.dirtyRelationships = {
      byChild: Ember.Map.create(),
      byNewParent: Ember.Map.create(),
      byOldParent: Ember.Map.create()
    };
  },

  /**
    Creates a new record of the given type and assigns it to the transaction
    on which the method was called.

    This is useful as only clean records can be added to a transaction and
    new records created using other methods immediately become dirty.

    @param {DS.Model} type the model type to create
    @param {Object} hash the data hash to assign the new record
  */
  createRecord: function(type, hash) {
    var store = get(this, 'store');

    return store.createRecord(type, hash, this);
  },

  /**
    Adds an existing record to this transaction. Only records without
    modficiations (i.e., records whose `isDirty` property is `false`)
    can be added to a transaction.

    @param {DS.Model} record the record to add to the transaction
  */
  add: function(record) {
    // we could probably make this work if someone has a valid use case. Do you?
    Ember.assert("Once a record has changed, you cannot move it into a different transaction", !get(record, 'isDirty'));

    var recordTransaction = get(record, 'transaction'),
        defaultTransaction = get(this, 'store.defaultTransaction');

    Ember.assert("Models cannot belong to more than one transaction at a time.", recordTransaction === defaultTransaction);

    this.adoptRecord(record);
  },

  /**
    Commits the transaction, which causes all of the modified records that
    belong to the transaction to be sent to the adapter to be saved.

    Once you call `commit()` on a transaction, you should not re-use it.

    When a record is saved, it will be removed from this transaction and
    moved back to the store's default transaction.
  */
  commit: function() {
    var self = this,
        iterate;

    iterate = function(bucketType, fn, binding) {
      var dirty = self.bucketForType(bucketType);

      dirty.forEach(function(type, records) {
        if (records.isEmpty()) { return; }

        var array = [];

        records.forEach(function(record) {
          record.send('willCommit');

          if (get(record, 'isPending') === false) {
            array.push(record);
          }
        });

        fn.call(binding, type, array);
      });
    };

    var commitDetails = {
      updated: {
        eachType: function(fn, binding) { iterate('updated', fn, binding); }
      },

      created: {
        eachType: function(fn, binding) { iterate('created', fn, binding); }
      },

      deleted: {
        eachType: function(fn, binding) { iterate('deleted', fn, binding); }
      }
    };

    var store = get(this, 'store');
    var adapter = get(store, '_adapter');

    this.removeCleanRecords();

    if (adapter && adapter.commit) { adapter.commit(store, commitDetails); }
    else { throw fmt("Adapter is either null or does not implement `commit` method", this); }
  },

  /**
    Rolling back a transaction resets the records that belong to
    that transaction.

    Updated records have their properties reset to the last known
    value from the persistence layer. Deleted records are reverted
    to a clean, non-deleted state. Newly created records immediately
    become deleted, and are not sent to the adapter to be persisted.

    After the transaction is rolled back, any records that belong
    to it will return to the store's default transaction, and the
    current transaction should not be used again.
  */
  rollback: function() {
    var store = get(this, 'store'),
        dirty;

    // Loop through all of the records in each of the dirty states
    // and initiate a rollback on them. As a side effect of telling
    // the record to roll back, it should also move itself out of
    // the dirty bucket and into the clean bucket.
    ['created', 'updated', 'deleted', 'inflight'].forEach(function(bucketType) {
      dirty = this.bucketForType(bucketType);

      dirty.forEach(function(type, records) {
        records.forEach(function(record) {
          record.send('rollback');
        });
      });
    }, this);

    // Now that all records in the transaction are guaranteed to be
    // clean, migrate them all to the store's default transaction.
    this.removeCleanRecords();
  },

  /**
    @private

    Removes a record from this transaction and back to the store's
    default transaction.

    Note: This method is private for now, but should probably be exposed
    in the future once we have stricter error checking (for example, in the
    case of the record being dirty).

    @param {DS.Model} record
  */
  remove: function(record) {
    var defaultTransaction = get(this, 'store.defaultTransaction');
    defaultTransaction.adoptRecord(record);
  },

  /**
    @private

    Removes all of the records in the transaction's clean bucket.
  */
  removeCleanRecords: function() {
    var clean = this.bucketForType('clean'),
        self = this;

    clean.forEach(function(type, records) {
      records.forEach(function(record) {
        self.remove(record);
      });
    });
  },

  /**
    @private

    Returns the bucket for the given bucket type. For example, you might call
    `this.bucketForType('updated')` to get the `Ember.Map` that contains all
    of the records that have changes pending.

    @param {String} bucketType the type of bucket
    @returns Ember.Map
  */
  bucketForType: function(bucketType) {
    var buckets = get(this, 'buckets');

    return get(buckets, bucketType);
  },

  /**
    @private

    This method moves a record into a different transaction without the normal
    checks that ensure that the user is not doing something weird, like moving
    a dirty record into a new transaction.

    It is designed for internal use, such as when we are moving a clean record
    into a new transaction when the transaction is committed.

    This method must not be called unless the record is clean.

    @param {DS.Model} record
  */
  adoptRecord: function(record) {
    var oldTransaction = get(record, 'transaction');

    if (oldTransaction) {
      oldTransaction.removeFromBucket('clean', record);
    }

    this.addToBucket('clean', record);
    set(record, 'transaction', this);
  },

  /**
    @private

    Adds a record to the named bucket.

    @param {String} bucketType one of `clean`, `created`, `updated`, or `deleted`
  */
  addToBucket: function(bucketType, record) {
    var bucket = this.bucketForType(bucketType),
        type = record.constructor;

    var records = bucket.get(type);

    if (!records) {
      records = Ember.OrderedSet.create();
      bucket.set(type, records);
    }

    records.add(record);
  },

  /**
    @private

    Removes a record from the named bucket.

    @param {String} bucketType one of `clean`, `created`, `updated`, or `deleted`
  */
  removeFromBucket: function(bucketType, record) {
    var bucket = this.bucketForType(bucketType),
        type = record.constructor;

    var records = bucket.get(type);
    records.remove(record);
  },

  /**
    @private

    Called by a ManyArray when a new record is added to it. This
    method will index a relationship description by the child
    record, its old parent, and its new parent.

    The store will provide this description to the adapter's
    shouldCommit method, so it can determine whether any of
    the records is pending another record. The store will also
    provide a list of these descriptions to the adapter's commit
    method.

    @param {DS.Model} record the new child record
    @param {DS.Model} oldParent the parent that the child is
      moving from, or null
    @param {DS.Model} newParent the parent that the child is
      moving to, or null
  */
  relationshipBecameDirty: function(child, oldParent, newParent) {
    var relationships = this.dirtyRelationships, relationship;

    var relationshipsForChild = relationships.byChild.get(child),
        possibleRelationship,
        needsNewEntries = true;

    // If the child has any existing dirty relationships in this
    // transaction, we need to collapse the old relationship
    // into the new one. For example, if we change the parent of
    // a child record before saving, there is no need to save the
    // record that was its parent temporarily.
    if (relationshipsForChild) {

      // Loop through all of the relationships we know about that
      // contain the same child as the new relationship.
      for (var i=0, l=relationshipsForChild.length; i<l; i++) {
        relationship = relationshipsForChild[i];

        // If the parent of the child record has changed, there is
        // no need to update the old parent that had not yet been saved.
        //
        // This case is two changes in a record's parent:
        //
        //   A -> B
        //   B -> C
        //
        // In this case, there is no need to remember the A->B
        // change. We can collapse both changes into:
        //
        //   A -> C
        //
        // Another possible case is:
        //
        //   A -> B
        //   B -> A
        //
        // In this case, we don't need to do anything. We can
        // simply remove the original A->B change and call it
        // a day.
        if (relationship.newParent === oldParent) {
          oldParent = relationship.oldParent;
          this.removeRelationship(relationship);

          // This is the case of A->B followed by B->A.
          if (relationship.oldParent === newParent) {
            needsNewEntries = false;
          }
        }
      }
    }

    relationship = {
      child: child,
      oldParent: oldParent,
      newParent: newParent
    };

    // If we didn't go A->B and then B->A, add new dirty relationship
    // entries.
    if (needsNewEntries) {
      this.addRelationshipTo('byChild', child, relationship);
      this.addRelationshipTo('byOldParent', oldParent, relationship);
      this.addRelationshipTo('byNewParent', newParent, relationship);
    }
  },

  removeRelationship: function(relationship) {
    var relationships = this.dirtyRelationships;

    removeObject(relationships.byOldParent.get(relationship.oldParent), relationship);
    removeObject(relationships.byNewParent.get(relationship.newParent), relationship);
    removeObject(relationships.byChild.get(relationship.child), relationship);
  },

  addRelationshipTo: function(type, record, description) {
    var map = this.dirtyRelationships[type];

    var relationships = map.get(record);

    if (!relationships) {
      relationships = [ description ];
      map.set(record, relationships);
    } else {
      relationships.push(description);
    }
  },

  /**
    @private

    Called by a record's state manager to indicate that the record has entered
    a dirty state. The record will be moved from the `clean` bucket and into
    the appropriate dirty bucket.

    @param {String} bucketType one of `created`, `updated`, or `deleted`
  */
  recordBecameDirty: function(bucketType, record) {
    this.removeFromBucket('clean', record);
    this.addToBucket(bucketType, record);
  },

  /**
    @private

    Called by a record's state manager to indicate that the record has entered
    inflight state. The record will be moved from its current dirty bucket and into
    the `inflight` bucket.

    @param {String} bucketType one of `created`, `updated`, or `deleted`
  */
  recordBecameInFlight: function(kind, record) {
    this.removeFromBucket(kind, record);
    this.addToBucket('inflight', record);
  },

  /**
    @private

    Called by a record's state manager to indicate that the record has entered
    a clean state. The record will be moved from its current dirty or inflight bucket and into
    the `clean` bucket.

    @param {String} bucketType one of `created`, `updated`, or `deleted`
  */
  recordBecameClean: function(kind, record) {
    this.removeFromBucket(kind, record);

    this.remove(record);
  }
});

})();



(function() {
/*globals Ember*/
var get = Ember.get, set = Ember.set, fmt = Ember.String.fmt;

var DATA_PROXY = {
  get: function(name) {
    return this.savedData[name];
  }
};

// These values are used in the data cache when clientIds are
// needed but the underlying data has not yet been loaded by
// the server.
var UNLOADED = 'unloaded';
var LOADING = 'loading';

// Implementors Note:
//
//   The variables in this file are consistently named according to the following
//   scheme:
//
//   * +id+ means an identifier managed by an external source, provided inside the
//     data hash provided by that source.
//   * +clientId+ means a transient numerical identifier generated at runtime by
//     the data store. It is important primarily because newly created objects may
//     not yet have an externally generated id.
//   * +type+ means a subclass of DS.Model.

/**
  The store contains all of the hashes for records loaded from the server.
  It is also responsible for creating instances of DS.Model when you request one
  of these data hashes, so that they can be bound to in your Handlebars templates.

  Create a new store like this:

       MyApp.store = DS.Store.create();

  You can retrieve DS.Model instances from the store in several ways. To retrieve
  a record for a specific id, use the `find()` method:

       var record = MyApp.store.find(MyApp.Contact, 123);

   By default, the store will talk to your backend using a standard REST mechanism.
   You can customize how the store talks to your backend by specifying a custom adapter:

       MyApp.store = DS.Store.create({
         adapter: 'MyApp.CustomAdapter'
       });

    You can learn more about writing a custom adapter by reading the `DS.Adapter`
    documentation.
*/
DS.Store = Ember.Object.extend({

  /**
    Many methods can be invoked without specifying which store should be used.
    In those cases, the first store created will be used as the default. If
    an application has multiple stores, it should specify which store to use
    when performing actions, such as finding records by id.

    The init method registers this store as the default if none is specified.
  */
  init: function() {
    // Enforce API revisioning. See BREAKING_CHANGES.md for more.
    var revision = get(this, 'revision');

    if (revision !== DS.CURRENT_API_REVISION && !Ember.ENV.TESTING) {
      throw new Error("Error: The Ember Data library has had breaking API changes since the last time you updated the library. Please review the list of breaking changes at https://github.com/emberjs/data/blob/master/BREAKING_CHANGES.md, then update your store's `revision` property to " + DS.CURRENT_API_REVISION);
    }

    if (!get(DS, 'defaultStore') || get(this, 'isDefaultStore')) {
      set(DS, 'defaultStore', this);
    }

    // internal bookkeeping; not observable
    this.typeMaps = {};
    this.recordCache = [];
    this.clientIdToId = {};
    this.recordArraysByClientId = {};

    // Internally, we maintain a map of all unloaded IDs requested by
    // a ManyArray. As the adapter loads hashes into the store, the
    // store notifies any interested ManyArrays. When the ManyArray's
    // total number of loading records drops to zero, it becomes
    // `isLoaded` and fires a `didLoad` event.
    this.loadingRecordArrays = {};

    set(this, 'defaultTransaction', this.transaction());

    return this._super();
  },

  /**
    Returns a new transaction scoped to this store.

    @see {DS.Transaction}
    @returns DS.Transaction
  */
  transaction: function() {
    return DS.Transaction.create({ store: this });
  },

  /**
    @private

    This is used only by the record's DataProxy. Do not use this directly.
  */
  dataForRecord: function(record) {
    var type = record.constructor,
        clientId = get(record, 'clientId'),
        typeMap = this.typeMapFor(type);

    return typeMap.cidToHash[clientId];
  },

  /**
    The adapter to use to communicate to a backend server or other persistence layer.

    This can be specified as an instance, a class, or a property path that specifies
    where the adapter can be located.

    @property {DS.Adapter|String}
  */
  adapter: null,

  /**
    @private

    This property returns the adapter, after resolving a possible String.

    @returns DS.Adapter
  */
  _adapter: Ember.computed(function() {
    var adapter = get(this, 'adapter');
    if (typeof adapter === 'string') {
      return get(this, adapter, false) || get(window, adapter);
    }
    return adapter;
  }).property('adapter').cacheable(),

  // A monotonically increasing number to be used to uniquely identify
  // data hashes and records.
  clientIdCounter: 1,

  // .....................
  // . CREATE NEW RECORD .
  // .....................

  /**
    Create a new record in the current store. The properties passed
    to this method are set on the newly created record.

    @param {subclass of DS.Model} type
    @param {Object} properties a hash of properties to set on the
      newly created record.
    @returns DS.Model
  */
  createRecord: function(type, properties, transaction) {
    properties = properties || {};

    // Create a new instance of the model `type` and put it
    // into the specified `transaction`. If no transaction is
    // specified, the default transaction will be used.
    //
    // NOTE: A `transaction` is specified when the
    // `transaction.createRecord` API is used.
    var record = type._create({
      store: this
    });

    transaction = transaction || get(this, 'defaultTransaction');
    transaction.adoptRecord(record);

    // Extract the primary key from the `properties` hash,
    // based on the `primaryKey` for the model type.
    var primaryKey = get(record, 'primaryKey'),
        id = properties[primaryKey] || null;

    // If the passed properties do not include a primary key,
    // give the adapter an opportunity to generate one.
    var adapter;
    if (Ember.none(id)) {
      adapter = get(this, 'adapter');
      if (adapter && adapter.generateIdForRecord) {
        id = adapter.generateIdForRecord(this, record);
        properties.id = id;
      }
    }

    var hash = {}, clientId;

    // Push the hash into the store. If present, associate the
    // extracted `id` with the hash.
    clientId = this.pushHash(hash, id, type);

    record.send('didChangeData');

    var recordCache = get(this, 'recordCache');

    // Now that we have a clientId, attach it to the record we
    // just created.
    set(record, 'clientId', clientId);

    // Store the record we just created in the record cache for
    // this clientId.
    recordCache[clientId] = record;

    // Set the properties specified on the record.
    record.setProperties(properties);

    this.updateRecordArrays(type, clientId, get(record, 'data'));

    return record;
  },

  // .................
  // . DELETE RECORD .
  // .................

  /**
    For symmetry, a record can be deleted via the store.

    @param {DS.Model} record
  */
  deleteRecord: function(record) {
    record.send('deleteRecord');
  },

  // ................
  // . FIND RECORDS .
  // ................

  /**
    This is the main entry point into finding records. The first
    parameter to this method is always a subclass of `DS.Model`.

    You can use the `find` method on a subclass of `DS.Model`
    directly if your application only has one store. For
    example, instead of `store.find(App.Person, 1)`, you could
    say `App.Person.find(1)`.

    ---

    To find a record by ID, pass the `id` as the second parameter:

        store.find(App.Person, 1);
        App.Person.find(1);

    If the record with that `id` had not previously been loaded,
    the store will return an empty record immediately and ask
    the adapter to find the data by calling the adapter's `find`
    method.

    The `find` method will always return the same object for a
    given type and `id`. To check whether the adapter has populated
    a record, you can check its `isLoaded` property.

    ---

    To find all records for a type, call `find` with no additional
    parameters:

        store.find(App.Person);
        App.Person.find();

    This will return a `RecordArray` representing all known records
    for the given type and kick off a request to the adapter's
    `findAll` method to load any additional records for the type.

    The `RecordArray` returned by `find()` is live. If any more
    records for the type are added at a later time through any
    mechanism, it will automatically update to reflect the change.

    ---

    To find a record by a query, call `find` with a hash as the
    second parameter:

        store.find(App.Person, { page: 1 });
        App.Person.find({ page: 1 });

    This will return a `RecordArray` immediately, but it will always
    be an empty `RecordArray` at first. It will call the adapter's
    `findQuery` method, which will populate the `RecordArray` once
    the server has returned results.

    You can check whether a query results `RecordArray` has loaded
    by checking its `isLoaded` property.
  */
  find: function(type, id, query) {
    if (id === undefined) {
      return this.findAll(type);
    }

    if (query !== undefined) {
      return this.findMany(type, id, query);
    } else if (Ember.typeOf(id) === 'object') {
      return this.findQuery(type, id);
    }

    if (Ember.isArray(id)) {
      return this.findMany(type, id);
    }

    var clientId = this.typeMapFor(type).idToCid[id];

    return this.findByClientId(type, clientId, id);
  },

  findByClientId: function(type, clientId, id) {
    var recordCache = get(this, 'recordCache'),
        dataCache, record;

    // If there is already a clientId assigned for this
    // type/id combination, try to find an existing
    // record for that id and return. Otherwise,
    // materialize a new record and set its data to the
    // value we already have.
    if (clientId !== undefined) {
      record = recordCache[clientId];

      if (!record) {
        // create a new instance of the model type in the
        // 'isLoading' state
        record = this.materializeRecord(type, clientId);

        dataCache = this.typeMapFor(type).cidToHash;

        if (typeof dataCache[clientId] === 'object') {
          record.send('didChangeData');
        }
      }
    } else {
      clientId = this.pushHash(LOADING, id, type);

      // create a new instance of the model type in the
      // 'isLoading' state
      record = this.materializeRecord(type, clientId, id);

      // let the adapter set the data, possibly async
      var adapter = get(this, '_adapter');
      if (adapter && adapter.find) { adapter.find(this, type, id); }
      else { throw fmt("Adapter is either null or does not implement `find` method", this); }
    }

    return record;
  },

  /**
    @private

    Given a type and array of `clientId`s, determines which of those
    `clientId`s has not yet been loaded.

    In preparation for loading, this method also marks any unloaded
    `clientId`s as loading.
  */
  neededClientIds: function(type, clientIds) {
    var neededClientIds = [],
        typeMap = this.typeMapFor(type),
        dataCache = typeMap.cidToHash,
        clientId;

    for (var i=0, l=clientIds.length; i<l; i++) {
      clientId = clientIds[i];
      if (dataCache[clientId] === UNLOADED) {
        neededClientIds.push(clientId);
        dataCache[clientId] = LOADING;
      }
    }

    return neededClientIds;
  },

  /**
    @private

    This method is the entry point that associations use to update
    themselves when their underlying data changes.

    First, it determines which of its `clientId`s are still unloaded,
    then converts the needed `clientId`s to IDs and invokes `findMany`
    on the adapter.
  */
  fetchUnloadedClientIds: function(type, clientIds) {
    var neededClientIds = this.neededClientIds(type, clientIds);
    this.fetchMany(type, neededClientIds);
  },

  /**
    @private

    This method takes a type and list of `clientId`s, converts the
    `clientId`s into IDs, and then invokes the adapter's `findMany`
    method.

    It is used both by a brand new association (via the `findMany`
    method) or when the data underlying an existing association
    changes (via the `fetchUnloadedClientIds` method).
  */
  fetchMany: function(type, clientIds) {
    var clientIdToId = this.clientIdToId;

    var neededIds = Ember.EnumerableUtils.map(clientIds, function(clientId) {
      return clientIdToId[clientId];
    });

    if (!neededIds.length) { return; }

    var adapter = get(this, '_adapter');
    if (adapter && adapter.findMany) { adapter.findMany(this, type, neededIds); }
    else { throw fmt("Adapter is either null or does not implement `findMany` method", this); }
  },

  /**
    @private

    `findMany` is the entry point that associations use to generate a
    new `ManyArray` for the list of IDs specified by the server for
    the association.

    Its responsibilities are:

    * convert the IDs into clientIds
    * determine which of the clientIds still need to be loaded
    * create a new ManyArray whose content is *all* of the clientIds
    * notify the ManyArray of the number of its elements that are
      already loaded
    * insert the unloaded clientIds into the `loadingRecordArrays`
      bookkeeping structure, which will allow the `ManyArray` to know
      when all of its loading elements are loaded from the server.
    * ask the adapter to load the unloaded elements, by invoking
      findMany with the still-unloaded IDs.
  */
  findMany: function(type, ids) {
    // 1. Convert ids to client ids
    // 2. Determine which of the client ids need to be loaded
    // 3. Create a new ManyArray whose content is ALL of the clientIds
    // 4. Decrement the ManyArray's counter by the number of loaded clientIds
    // 5. Put the ManyArray into our bookkeeping data structure, keyed on
    //    the needed clientIds
    // 6. Ask the adapter to load the records for the unloaded clientIds (but
    //    convert them back to ids)

    var clientIds = this.clientIdsForIds(type, ids);

    var neededClientIds = this.neededClientIds(type, clientIds),
        manyArray = this.createManyArray(type, Ember.A(clientIds)),
        loadedCount = clientIds.length - neededClientIds.length,
        loadingRecordArrays = this.loadingRecordArrays,
        clientId, i, l;

    manyArray.send('loadedRecords', loadedCount);

    if (neededClientIds.length) {
      for (i=0, l=neededClientIds.length; i<l; i++) {
        clientId = neededClientIds[i];
        if (loadingRecordArrays[clientId]) {
          loadingRecordArrays[clientId].push(manyArray);
        } else {
          this.loadingRecordArrays[clientId] = [ manyArray ];
        }
      }

      this.fetchMany(type, neededClientIds);
    }

    return manyArray;
  },

  findQuery: function(type, query) {
    var array = DS.AdapterPopulatedRecordArray.create({ type: type, content: Ember.A([]), store: this });
    var adapter = get(this, '_adapter');
    if (adapter && adapter.findQuery) { adapter.findQuery(this, type, query, array); }
    else { throw fmt("Adapter is either null or does not implement `findQuery` method", this); }
    return array;
  },

  findAll: function(type) {

    var typeMap = this.typeMapFor(type),
        findAllCache = typeMap.findAllCache;

    if (findAllCache) { return findAllCache; }

    var array = DS.RecordArray.create({ type: type, content: Ember.A([]), store: this });
    this.registerRecordArray(array, type);

    var adapter = get(this, '_adapter');
    if (adapter && adapter.findAll) { adapter.findAll(this, type); }

    typeMap.findAllCache = array;
    return array;
  },

  filter: function(type, query, filter) {
    // allow an optional server query
    if (arguments.length === 3) {
      this.findQuery(type, query);
    } else if (arguments.length === 2) {
      filter = query;
    }

    var array = DS.FilteredRecordArray.create({ type: type, content: Ember.A([]), store: this, filterFunction: filter });

    this.registerRecordArray(array, type, filter);

    return array;
  },

  recordIsLoaded: function(type, id) {
    return !Ember.none(this.typeMapFor(type).idToCid[id]);
  },

  // ............
  // . UPDATING .
  // ............

  hashWasUpdated: function(type, clientId, record) {
    // Because hash updates are invoked at the end of the run loop,
    // it is possible that a record might be deleted after its hash
    // has been modified and this method was scheduled to be called.
    //
    // If that's the case, the record would have already been removed
    // from all record arrays; calling updateRecordArrays would just
    // add it back. If the record is deleted, just bail. It shouldn't
    // give us any more trouble after this.

    if (get(record, 'isDeleted')) { return; }
    this.updateRecordArrays(type, clientId, get(record, 'data'));
  },

  // ..............
  // . PERSISTING .
  // ..............

  commit: function() {
    var defaultTransaction = get(this, 'defaultTransaction');
    set(this, 'defaultTransaction', this.transaction());

    defaultTransaction.commit();
  },

  didUpdateRecords: function(array, hashes) {
    if (hashes) {
      array.forEach(function(record, idx) {
        this.didUpdateRecord(record, hashes[idx]);
      }, this);
    } else {
      array.forEach(function(record) {
        this.didUpdateRecord(record);
      }, this);
    }
  },

  didUpdateRecord: function(record, hash) {
    if (hash) {
      var clientId = get(record, 'clientId'),
          dataCache = this.typeMapFor(record.constructor).cidToHash;

      dataCache[clientId] = hash;
      record.send('didChangeData');
      record.hashWasUpdated();
    } else {
      record.send('didSaveData');
    }

    record.send('didCommit');
  },

  didDeleteRecords: function(array) {
    array.forEach(function(record) {
      record.send('didCommit');
    });
  },

  didDeleteRecord: function(record) {
    record.send('didCommit');
  },

  _didCreateRecord: function(record, hash, typeMap, clientId, primaryKey) {
    var recordData = get(record, 'data'), id, changes;

    if (hash) {
      typeMap.cidToHash[clientId] = hash;

      // If the server returns a hash, we assume that the server's version
      // of the data supercedes the local changes.
      record.beginPropertyChanges();
      record.send('didChangeData');
      recordData.adapterDidUpdate();
      record.hashWasUpdated();
      record.endPropertyChanges();

      id = hash[primaryKey];

      typeMap.idToCid[id] = clientId;
      this.clientIdToId[clientId] = id;
    } else {
      recordData.commit();
    }

    record.send('didCommit');
  },


  didCreateRecords: function(type, array, hashes) {
    var primaryKey = type.proto().primaryKey,
        typeMap = this.typeMapFor(type),
        clientId;

    for (var i=0, l=get(array, 'length'); i<l; i++) {
      var record = array[i], hash = hashes[i];
      clientId = get(record, 'clientId');

      this._didCreateRecord(record, hash, typeMap, clientId, primaryKey);
    }
  },

  didCreateRecord: function(record, hash) {
    var type = record.constructor,
        typeMap = this.typeMapFor(type),
        clientId, primaryKey;

    // The hash is optional, but if it is not provided, the client must have
    // provided a primary key.

    primaryKey = type.proto().primaryKey;

    // TODO: Make Ember.assert more flexible
    if (hash) {
      Ember.assert("The server must provide a primary key: " + primaryKey, get(hash, primaryKey));
    } else {
      Ember.assert("The server did not return data, and you did not create a primary key (" + primaryKey + ") on the client", get(get(record, 'data'), primaryKey));
    }

    clientId = get(record, 'clientId');

    this._didCreateRecord(record, hash, typeMap, clientId, primaryKey);
  },

  recordWasInvalid: function(record, errors) {
    record.send('becameInvalid', errors);
  },

  // .................
  // . RECORD ARRAYS .
  // .................

  registerRecordArray: function(array, type, filter) {
    var recordArrays = this.typeMapFor(type).recordArrays;

    recordArrays.push(array);

    this.updateRecordArrayFilter(array, type, filter);
  },

  createManyArray: function(type, clientIds) {
    var array = DS.ManyArray.create({ type: type, content: clientIds, store: this });

    clientIds.forEach(function(clientId) {
      var recordArrays = this.recordArraysForClientId(clientId);
      recordArrays.add(array);
    }, this);

    return array;
  },

  updateRecordArrayFilter: function(array, type, filter) {
    var typeMap = this.typeMapFor(type),
        dataCache = typeMap.cidToHash,
        clientIds = typeMap.clientIds,
        clientId, hash, proxy;

    var recordCache = get(this, 'recordCache'),
        foundRecord,
        record;

    for (var i=0, l=clientIds.length; i<l; i++) {
      clientId = clientIds[i];
      foundRecord = false;

      hash = dataCache[clientId];
      if (typeof hash === 'object') {
        if (record = recordCache[clientId]) {
          if (!get(record, 'isDeleted')) {
            proxy = get(record, 'data');
            foundRecord = true;
          }
        } else {
          DATA_PROXY.savedData = hash;
          proxy = DATA_PROXY;
          foundRecord = true;
        }

        if (foundRecord) { this.updateRecordArray(array, filter, type, clientId, proxy); }
      }
    }
  },

  updateRecordArrays: function(type, clientId, dataProxy) {
    var recordArrays = this.typeMapFor(type).recordArrays,
        filter;

    recordArrays.forEach(function(array) {
      filter = get(array, 'filterFunction');
      this.updateRecordArray(array, filter, type, clientId, dataProxy);
    }, this);

    // loop through all manyArrays containing an unloaded copy of this
    // clientId and notify them that the record was loaded.
    var manyArrays = this.loadingRecordArrays[clientId], manyArray;

    if (manyArrays) {
      for (var i=0, l=manyArrays.length; i<l; i++) {
        manyArrays[i].send('loadedRecords', 1);
      }

      this.loadingRecordArrays[clientId] = null;
    }
  },

  updateRecordArray: function(array, filter, type, clientId, dataProxy) {
    var shouldBeInArray;

    if (!filter) {
      shouldBeInArray = true;
    } else {
      shouldBeInArray = filter(dataProxy);
    }

    var content = get(array, 'content');
    var alreadyInArray = content.indexOf(clientId) !== -1;

    var recordArrays = this.recordArraysForClientId(clientId);

    if (shouldBeInArray && !alreadyInArray) {
      recordArrays.add(array);
      content.pushObject(clientId);
    } else if (!shouldBeInArray && alreadyInArray) {
      recordArrays.remove(array);
      content.removeObject(clientId);
    }
  },

  removeFromRecordArrays: function(record) {
    var clientId = get(record, 'clientId');
    var recordArrays = this.recordArraysForClientId(clientId);

    recordArrays.forEach(function(array) {
      var content = get(array, 'content');
      content.removeObject(clientId);
    });
  },

  // ............
  // . INDEXING .
  // ............

  recordArraysForClientId: function(clientId) {
    var recordArrays = get(this, 'recordArraysByClientId');
    var ret = recordArrays[clientId];

    if (!ret) {
      ret = recordArrays[clientId] = Ember.OrderedSet.create();
    }

    return ret;
  },

  typeMapFor: function(type) {
    var typeMaps = get(this, 'typeMaps');
    var guidForType = Ember.guidFor(type);

    var typeMap = typeMaps[guidForType];

    if (typeMap) {
      return typeMap;
    } else {
      return (typeMaps[guidForType] =
        {
          idToCid: {},
          clientIds: [],
          cidToHash: {},
          recordArrays: []
      });
    }
  },

  /** @private

    For a given type and id combination, returns the client id used by the store.
    If no client id has been assigned yet, one will be created and returned.

    @param {DS.Model} type
    @param {String|Number} id
  */
  clientIdForId: function(type, id) {
    var clientId = this.typeMapFor(type).idToCid[id];

    if (clientId !== undefined) { return clientId; }

    return this.pushHash(UNLOADED, id, type);
  },

  /**
    @private

    This method works exactly like `clientIdForId`, but does not
    require looking up the `typeMap` for every `clientId` and
    invoking a method per `clientId`.
  */
  clientIdsForIds: function(type, ids) {
    var typeMap = this.typeMapFor(type),
        idToClientIdMap = typeMap.idToCid;

    return Ember.EnumerableUtils.map(ids, function(id) {
      var clientId = idToClientIdMap[id];
      if (clientId) { return clientId; }
      return this.pushHash(UNLOADED, id, type);
    }, this);
  },

  // ................
  // . LOADING DATA .
  // ................

  /**
    Load a new data hash into the store for a given id and type combination.
    If data for that record had been loaded previously, the new information
    overwrites the old.

    If the record you are loading data for has outstanding changes that have not
    yet been saved, an exception will be thrown.

    @param {DS.Model} type
    @param {String|Number} id
    @param {Object} hash the data hash to load
  */
  load: function(type, id, hash) {
    if (hash === undefined) {
      hash = id;
      var primaryKey = type.proto().primaryKey;
      Ember.assert("A data hash was loaded for a record of type " + type.toString() + " but no primary key '" + primaryKey + "' was provided.", primaryKey in hash);
      id = hash[primaryKey];
    }

    var typeMap = this.typeMapFor(type),
        dataCache = typeMap.cidToHash,
        clientId = typeMap.idToCid[id],
        recordCache = get(this, 'recordCache');

    if (clientId !== undefined) {
      dataCache[clientId] = hash;

      var record = recordCache[clientId];
      if (record) {
        record.send('didChangeData');
      }
    } else {
      clientId = this.pushHash(hash, id, type);
    }

    DATA_PROXY.savedData = hash;
    this.updateRecordArrays(type, clientId, DATA_PROXY);

    return { id: id, clientId: clientId };
  },

  loadMany: function(type, ids, hashes) {
    var clientIds = Ember.A([]);

    if (hashes === undefined) {
      hashes = ids;
      ids = [];
      var primaryKey = type.proto().primaryKey;

      ids = Ember.EnumerableUtils.map(hashes, function(hash) {
        return hash[primaryKey];
      });
    }

    for (var i=0, l=get(ids, 'length'); i<l; i++) {
      var loaded = this.load(type, ids[i], hashes[i]);
      clientIds.pushObject(loaded.clientId);
    }

    return { clientIds: clientIds, ids: ids };
  },

  /** @private

    Stores a data hash for the specified type and id combination and returns
    the client id.

    @param {Object} hash
    @param {String|Number} id
    @param {DS.Model} type
    @returns {Number}
  */
  pushHash: function(hash, id, type) {
    var typeMap = this.typeMapFor(type);

    var idToClientIdMap = typeMap.idToCid,
        clientIdToIdMap = this.clientIdToId,
        clientIds = typeMap.clientIds,
        dataCache = typeMap.cidToHash;

    var clientId = ++this.clientIdCounter;

    dataCache[clientId] = hash;

    // if we're creating an item, this process will be done
    // later, once the object has been persisted.
    if (id) {
      idToClientIdMap[id] = clientId;
      clientIdToIdMap[clientId] = id;
    }

    clientIds.push(clientId);

    return clientId;
  },

  // ..........................
  // . RECORD MATERIALIZATION .
  // ..........................

  materializeRecord: function(type, clientId, id) {
    var record;

    get(this, 'recordCache')[clientId] = record = type._create({
      store: this,
      clientId: clientId,
      _id: id
    });

    get(this, 'defaultTransaction').adoptRecord(record);

    record.send('loadingData');
    return record;
  },

  destroy: function() {
    if (get(DS, 'defaultStore') === this) {
      set(DS, 'defaultStore', null);
    }

    return this._super();
  }
});

})();



(function() {
var get = Ember.get, set = Ember.set, guidFor = Ember.guidFor;

/**
  This file encapsulates the various states that a record can transition
  through during its lifecycle.

  ### State Manager

  A record's state manager explicitly tracks what state a record is in
  at any given time. For instance, if a record is newly created and has
  not yet been sent to the adapter to be saved, it would be in the
  `created.uncommitted` state.  If a record has had local modifications
  made to it that are in the process of being saved, the record would be
  in the `updated.inFlight` state. (These state paths will be explained
  in more detail below.)

  Events are sent by the record or its store to the record's state manager.
  How the state manager reacts to these events is dependent on which state
  it is in. In some states, certain events will be invalid and will cause
  an exception to be raised.

  States are hierarchical. For example, a record can be in the
  `deleted.start` state, then transition into the `deleted.inFlight` state.
  If a child state does not implement an event handler, the state manager
  will attempt to invoke the event on all parent states until the root state is
  reached. The state hierarchy of a record is described in terms of a path
  string. You can determine a record's current state by getting its manager's
  current state path:

        record.get('stateManager.currentState.path');
        //=> "created.uncommitted"

  The `DS.Model` states are themselves stateless. What we mean is that,
  though each instance of a record also has a unique instance of a
  `DS.StateManager`, the hierarchical states that each of *those* points
  to is a shared data structure. For performance reasons, instead of each
  record getting its own copy of the hierarchy of states, each state
  manager points to this global, immutable shared instance. How does a
  state know which record it should be acting on?  We pass a reference to
  the current state manager as the first parameter to every method invoked
  on a state.

  The state manager passed as the first parameter is where you should stash
  state about the record if needed; you should never store data on the state
  object itself. If you need access to the record being acted on, you can
  retrieve the state manager's `record` property. For example, if you had
  an event handler `myEvent`:

      myEvent: function(manager) {
        var record = manager.get('record');
        record.doSomething();
      }

  For more information about state managers in general, see the Ember.js
  documentation on `Ember.StateManager`.

  ### Events, Flags, and Transitions

  A state may implement zero or more events, flags, or transitions.

  #### Events

  Events are named functions that are invoked when sent to a record. The
  state manager will first look for a method with the given name on the
  current state. If no method is found, it will search the current state's
  parent, and then its grandparent, and so on until reaching the top of
  the hierarchy. If the root is reached without an event handler being found,
  an exception will be raised. This can be very helpful when debugging new
  features.

  Here's an example implementation of a state with a `myEvent` event handler:

      aState: DS.State.create({
        myEvent: function(manager, param) {
          console.log("Received myEvent with "+param);
        }
      })

  To trigger this event:

      record.send('myEvent', 'foo');
      //=> "Received myEvent with foo"

  Note that an optional parameter can be sent to a record's `send()` method,
  which will be passed as the second parameter to the event handler.

  Events should transition to a different state if appropriate. This can be
  done by calling the state manager's `goToState()` method with a path to the
  desired state. The state manager will attempt to resolve the state path
  relative to the current state. If no state is found at that path, it will
  attempt to resolve it relative to the current state's parent, and then its
  parent, and so on until the root is reached. For example, imagine a hierarchy
  like this:

      * created
        * start <-- currentState
        * inFlight
      * updated
        * inFlight

  If we are currently in the `start` state, calling
  `goToState('inFlight')` would transition to the `created.inFlight` state,
  while calling `goToState('updated.inFlight')` would transition to
  the `updated.inFlight` state.

  Remember that *only events* should ever cause a state transition. You should
  never call `goToState()` from outside a state's event handler. If you are
  tempted to do so, create a new event and send that to the state manager.

  #### Flags

  Flags are Boolean values that can be used to introspect a record's current
  state in a more user-friendly way than examining its state path. For example,
  instead of doing this:

      var statePath = record.get('stateManager.currentState.path');
      if (statePath === 'created.inFlight') {
        doSomething();
      }

  You can say:

      if (record.get('isNew') && record.get('isSaving')) {
        doSomething();
      }

  If your state does not set a value for a given flag, the value will
  be inherited from its parent (or the first place in the state hierarchy
  where it is defined).

  The current set of flags are defined below. If you want to add a new flag,
  in addition to the area below, you will also need to declare it in the
  `DS.Model` class.

  #### Transitions

  Transitions are like event handlers but are called automatically upon
  entering or exiting a state. To implement a transition, just call a method
  either `enter` or `exit`:

      myState: DS.State.create({
        // Gets called automatically when entering
        // this state.
        enter: function(manager) {
          console.log("Entered myState");
        }
      })

   Note that enter and exit events are called once per transition. If the
   current state changes, but changes to another child state of the parent,
   the transition event on the parent will not be triggered.
*/

var stateProperty = Ember.computed(function(key) {
  var parent = get(this, 'parentState');
  if (parent) {
    return get(parent, key);
  }
}).property();

var isEmptyObject = function(object) {
  for (var name in object) {
    if (object.hasOwnProperty(name)) { return false; }
  }

  return true;
};

var hasDefinedProperties = function(object) {
  for (var name in object) {
    if (object.hasOwnProperty(name) && object[name]) { return true; }
  }

  return false;
};

DS.State = Ember.State.extend({
  isLoaded: stateProperty,
  isDirty: stateProperty,
  isSaving: stateProperty,
  isDeleted: stateProperty,
  isError: stateProperty,
  isNew: stateProperty,
  isValid: stateProperty,
  isPending: stateProperty,

  // For states that are substates of a
  // DirtyState (updated or created), it is
  // useful to be able to determine which
  // type of dirty state it is.
  dirtyType: stateProperty
});

var setProperty = function(manager, context) {
  var key = context.key, value = context.value;

  var record = get(manager, 'record'),
      data = get(record, 'data');

  set(data, key, value);
};

var setAssociation = function(manager, context) {
  var key = context.key, value = context.value;

  var record = get(manager, 'record'),
      data = get(record, 'data');

  data.setAssociation(key, value);
};

var didChangeData = function(manager) {
  var record = get(manager, 'record'),
      data = get(record, 'data');

  data._savedData = null;
  record.notifyPropertyChange('data');
};

// The waitingOn event shares common functionality
// between the different dirty states, but each is
// treated slightly differently. This method is exposed
// so that each implementation can invoke the common
// behavior, and then implement the behavior specific
// to the state.
var waitingOn = function(manager, object) {
  var record = get(manager, 'record'),
      pendingQueue = get(record, 'pendingQueue'),
      objectGuid = guidFor(object);

  var observer = function() {
    if (get(object, 'id')) {
      manager.send('doneWaitingOn', object);
      Ember.removeObserver(object, 'id', observer);
    }
  };

  pendingQueue[objectGuid] = [object, observer];
  Ember.addObserver(object, 'id', observer);
};

// Implementation notes:
//
// Each state has a boolean value for all of the following flags:
//
// * isLoaded: The record has a populated `data` property. When a
//   record is loaded via `store.find`, `isLoaded` is false
//   until the adapter sets it. When a record is created locally,
//   its `isLoaded` property is always true.
// * isDirty: The record has local changes that have not yet been
//   saved by the adapter. This includes records that have been
//   created (but not yet saved) or deleted.
// * isSaving: The record's transaction has been committed, but
//   the adapter has not yet acknowledged that the changes have
//   been persisted to the backend.
// * isDeleted: The record was marked for deletion. When `isDeleted`
//   is true and `isDirty` is true, the record is deleted locally
//   but the deletion was not yet persisted. When `isSaving` is
//   true, the change is in-flight. When both `isDirty` and
//   `isSaving` are false, the change has persisted.
// * isError: The adapter reported that it was unable to save
//   local changes to the backend. This may also result in the
//   record having its `isValid` property become false if the
//   adapter reported that server-side validations failed.
// * isNew: The record was created on the client and the adapter
//   did not yet report that it was successfully saved.
// * isValid: No client-side validations have failed and the
//   adapter did not report any server-side validation failures.
// * isPending: A record `isPending` when it belongs to an
//   association on another record and that record has not been
//   saved. A record in this state cannot be saved because it
//   lacks a "foreign key" that will be supplied by its parent
//   association when the parent record has been created. When
//   the adapter reports that the parent has saved, the
//   `isPending` property on all children will become `false`
//   and the transaction will try to commit the records.

// This mixin is mixed into various uncommitted states. Make
// sure to mix it in *after* the class definition, so its
// super points to the class definition.
var Uncommitted = Ember.Mixin.create({
  setProperty: setProperty,
  setAssociation: setAssociation
});

// These mixins are mixed into substates of the concrete
// subclasses of DirtyState.

var CreatedUncommitted = Ember.Mixin.create({
  deleteRecord: function(manager) {
    var record = get(manager, 'record');
    this._super(manager);

    record.withTransaction(function(t) {
      t.recordBecameClean('created', record);
    });
    manager.goToState('deleted.saved');
  }
});

var UpdatedUncommitted = Ember.Mixin.create({
  deleteRecord: function(manager) {
    this._super(manager);

    var record = get(manager, 'record');

    record.withTransaction(function(t) {
      t.recordBecameClean('updated', record);
    });

    manager.goToState('deleted');
  }
});

// The dirty state is a abstract state whose functionality is
// shared between the `created` and `updated` states.
//
// The deleted state shares the `isDirty` flag with the
// subclasses of `DirtyState`, but with a very different
// implementation.
var DirtyState = DS.State.extend({
  initialState: 'uncommitted',

  // FLAGS
  isDirty: true,

  // SUBSTATES

  // When a record first becomes dirty, it is `uncommitted`.
  // This means that there are local pending changes,
  // but they have not yet begun to be saved.
  uncommitted: DS.State.extend({
    // TRANSITIONS
    enter: function(manager) {
      var dirtyType = get(this, 'dirtyType'),
          record = get(manager, 'record');

      record.withTransaction(function (t) {
        t.recordBecameDirty(dirtyType, record);
      });
    },

    // EVENTS
    deleteRecord: Ember.K,

    waitingOn: function(manager, object) {
      waitingOn(manager, object);
      manager.goToState('pending');
    },

    willCommit: function(manager) {
      manager.goToState('inFlight');
    },

    becameInvalid: function(manager) {
      var dirtyType = get(this, 'dirtyType'),
          record = get(manager, 'record');

      record.withTransaction(function (t) {
        t.recordBecameInFlight(dirtyType, record);
      });

      manager.goToState('invalid');
    },

    rollback: function(manager) {
      var record = get(manager, 'record'),
          dirtyType = get(this, 'dirtyType'),
          data = get(record, 'data');

      data.rollback();

      record.withTransaction(function(t) {
        t.recordBecameClean(dirtyType, record);
      });

      manager.goToState('saved');
    }
  }, Uncommitted),

  // Once a record has been handed off to the adapter to be
  // saved, it is in the 'in flight' state. Changes to the
  // record cannot be made during this window.
  inFlight: DS.State.extend({
    // FLAGS
    isSaving: true,

    // TRANSITIONS
    enter: function(manager) {
      var dirtyType = get(this, 'dirtyType'),
          record = get(manager, 'record');

      record.withTransaction(function (t) {
        t.recordBecameInFlight(dirtyType, record);
      });
    },

    // EVENTS
    didCommit: function(manager) {
      var dirtyType = get(this, 'dirtyType'),
          record = get(manager, 'record');

      record.withTransaction(function(t) {
        t.recordBecameClean('inflight', record);
      });

      manager.goToState('saved');
      manager.send('invokeLifecycleCallbacks', dirtyType);
    },

    becameInvalid: function(manager, errors) {
      var record = get(manager, 'record');

      set(record, 'errors', errors);

      manager.goToState('invalid');
      manager.send('invokeLifecycleCallbacks');
    },

    becameError: function(manager) {
      manager.goToState('error');
      manager.send('invokeLifecycleCallbacks');
    },

    didChangeData: didChangeData
  }),

  // If a record becomes associated with a newly created
  // parent record, it will be `pending` until the parent
  // record has successfully persisted. Once this happens,
  // this record can use the parent's primary key as its
  // foreign key.
  //
  // If the record's transaction had already started to
  // commit, the record will transition to the `inFlight`
  // state. If it had not, the record will transition to
  // the `uncommitted` state.
  pending: DS.State.extend({
    initialState: 'uncommitted',

    // FLAGS
    isPending: true,

    // SUBSTATES

    // A pending record whose transaction has not yet
    // started to commit is in this state.
    uncommitted: DS.State.extend({
      // EVENTS
      deleteRecord: function(manager) {
        var record = get(manager, 'record'),
            pendingQueue = get(record, 'pendingQueue'),
            tuple;

        // since we are leaving the pending state, remove any
        // observers we have registered on other records.
        for (var prop in pendingQueue) {
          if (!pendingQueue.hasOwnProperty(prop)) { continue; }

          tuple = pendingQueue[prop];
          Ember.removeObserver(tuple[0], 'id', tuple[1]);
        }
      },

      willCommit: function(manager) {
        manager.goToState('committing');
      },

      doneWaitingOn: function(manager, object) {
        var record = get(manager, 'record'),
            pendingQueue = get(record, 'pendingQueue'),
            objectGuid = guidFor(object);

        delete pendingQueue[objectGuid];

        if (isEmptyObject(pendingQueue)) {
          manager.send('doneWaiting');
        }
      },

      doneWaiting: function(manager) {
        var dirtyType = get(this, 'dirtyType');
        manager.goToState(dirtyType + '.uncommitted');
      }
    }, Uncommitted),

    // A pending record whose transaction has started
    // to commit is in this state. Since it has not yet
    // been sent to the adapter, it is not `inFlight`
    // until all of its dependencies have been committed.
    committing: DS.State.extend({
      // FLAGS
      isSaving: true,

      // EVENTS
      doneWaitingOn: function(manager, object) {
        var record = get(manager, 'record'),
            pendingQueue = get(record, 'pendingQueue'),
            objectGuid = guidFor(object);

        delete pendingQueue[objectGuid];

        if (isEmptyObject(pendingQueue)) {
          manager.send('doneWaiting');
        }
      },

      doneWaiting: function(manager) {
        var record = get(manager, 'record'),
            transaction = get(record, 'transaction');

        // Now that the record is no longer pending, schedule
        // the transaction to commit.
        Ember.run.once(transaction, transaction.commit);
      },

      willCommit: function(manager) {
        var record = get(manager, 'record'),
            pendingQueue = get(record, 'pendingQueue');

        if (isEmptyObject(pendingQueue)) {
          var dirtyType = get(this, 'dirtyType');
          manager.goToState(dirtyType + '.inFlight');
        }
      }
    })
  }),

  // A record is in the `invalid` state when its client-side
  // invalidations have failed, or if the adapter has indicated
  // the the record failed server-side invalidations.
  invalid: DS.State.extend({
    // FLAGS
    isValid: false,

    exit: function(manager) {
      var record = get(manager, 'record');

      record.withTransaction(function (t) {
        t.recordBecameClean('inflight', record);
      });
    },

    // EVENTS
    deleteRecord: function(manager) {
      manager.goToState('deleted');
    },

    setAssociation: setAssociation,

    setProperty: function(manager, context) {
      setProperty(manager, context);

      var record = get(manager, 'record'),
          errors = get(record, 'errors'),
          key = context.key;

      set(errors, key, null);

      if (!hasDefinedProperties(errors)) {
        manager.send('becameValid');
      }
    },

    rollback: function(manager) {
      manager.send('becameValid');
      manager.send('rollback');
    },

    becameValid: function(manager) {
      manager.goToState('uncommitted');
    },

    invokeLifecycleCallbacks: function(manager) {
      var record = get(manager, 'record');
      record.trigger('becameInvalid', record);
    }
  })
});

// The created and updated states are created outside the state
// chart so we can reopen their substates and add mixins as
// necessary.

var createdState = DirtyState.create({
  dirtyType: 'created',

  // FLAGS
  isNew: true
});

var updatedState = DirtyState.create({
  dirtyType: 'updated'
});

// The created.uncommitted state and created.pending.uncommitted share
// some logic defined in CreatedUncommitted.
createdState.states.uncommitted.reopen(CreatedUncommitted);
createdState.states.pending.states.uncommitted.reopen(CreatedUncommitted);

// The created.uncommitted state needs to immediately transition to the
// deleted state if it is rolled back.
createdState.states.uncommitted.reopen({
  rollback: function(manager) {
    this._super(manager);
    manager.goToState('deleted.saved');
  }
});

// The updated.uncommitted state and updated.pending.uncommitted share
// some logic defined in UpdatedUncommitted.
updatedState.states.uncommitted.reopen(UpdatedUncommitted);
updatedState.states.pending.states.uncommitted.reopen(UpdatedUncommitted);
updatedState.states.inFlight.reopen({
  didSaveData: function(manager) {
    var record = get(manager, 'record'),
        data = get(record, 'data');

    data.saveData();
    data.adapterDidUpdate();
  }
});

var states = {
  rootState: Ember.State.create({
    // FLAGS
    isLoaded: false,
    isDirty: false,
    isSaving: false,
    isDeleted: false,
    isError: false,
    isNew: false,
    isValid: true,
    isPending: false,

    // SUBSTATES

    // A record begins its lifecycle in the `empty` state.
    // If its data will come from the adapter, it will
    // transition into the `loading` state. Otherwise, if
    // the record is being created on the client, it will
    // transition into the `created` state.
    empty: DS.State.create({
      // EVENTS
      loadingData: function(manager) {
        manager.goToState('loading');
      },

      didChangeData: function(manager) {
        didChangeData(manager);

        manager.goToState('loaded.created');
      }
    }),

    // A record enters this state when the store askes
    // the adapter for its data. It remains in this state
    // until the adapter provides the requested data.
    //
    // Usually, this process is asynchronous, using an
    // XHR to retrieve the data.
    loading: DS.State.create({
      // TRANSITIONS
      exit: function(manager) {
        var record = get(manager, 'record');
        record.trigger('didLoad');
      },

      // EVENTS
      didChangeData: function(manager, data) {
        didChangeData(manager);
        manager.send('loadedData');
      },

      loadedData: function(manager) {
        manager.goToState('loaded');
      }
    }),

    // A record enters this state when its data is populated.
    // Most of a record's lifecycle is spent inside substates
    // of the `loaded` state.
    loaded: DS.State.create({
      initialState: 'saved',

      // FLAGS
      isLoaded: true,

      // SUBSTATES

      // If there are no local changes to a record, it remains
      // in the `saved` state.
      saved: DS.State.create({

        // EVENTS
        setProperty: function(manager, context) {
          setProperty(manager, context);
          manager.goToState('updated');
        },

        setAssociation: function(manager, context) {
          setAssociation(manager, context);
          manager.goToState('updated');
        },

        didChangeData: didChangeData,

        deleteRecord: function(manager) {
          manager.goToState('deleted');
        },

        waitingOn: function(manager, object) {
          waitingOn(manager, object);
          manager.goToState('updated.pending');
        },

        invokeLifecycleCallbacks: function(manager, dirtyType) {
          var record = get(manager, 'record');
          if (dirtyType === 'created') {
            record.trigger('didCreate', record);
          } else {
            record.trigger('didUpdate', record);
          }
        }
      }),

      // A record is in this state after it has been locally
      // created but before the adapter has indicated that
      // it has been saved.
      created: createdState,

      // A record is in this state if it has already been
      // saved to the server, but there are new local changes
      // that have not yet been saved.
      updated: updatedState
    }),

    // A record is in this state if it was deleted from the store.
    deleted: DS.State.create({
      // FLAGS
      isDeleted: true,
      isLoaded: true,
      isDirty: true,

      // TRANSITIONS
      enter: function(manager) {
        var record = get(manager, 'record'),
            store = get(record, 'store');

        store.removeFromRecordArrays(record);
      },

      // SUBSTATES

      // When a record is deleted, it enters the `start`
      // state. It will exit this state when the record's
      // transaction starts to commit.
      start: DS.State.create({
        // TRANSITIONS
        enter: function(manager) {
          var record = get(manager, 'record');

          record.withTransaction(function(t) {
            t.recordBecameDirty('deleted', record);
          });
        },

        // EVENTS
        willCommit: function(manager) {
          manager.goToState('inFlight');
        },

        rollback: function(manager) {
          var record = get(manager, 'record'),
              data = get(record, 'data');

          data.rollback();
          record.withTransaction(function(t) {
            t.recordBecameClean('deleted', record);
          });
          manager.goToState('loaded');
        }
      }),

      // After a record's transaction is committing, but
      // before the adapter indicates that the deletion
      // has saved to the server, a record is in the
      // `inFlight` substate of `deleted`.
      inFlight: DS.State.create({
        // FLAGS
        isSaving: true,

        // TRANSITIONS
        enter: function(manager) {
          var record = get(manager, 'record');

          record.withTransaction(function (t) {
            t.recordBecameInFlight('deleted', record);
          });
        },

        // EVENTS
        didCommit: function(manager) {
          var record = get(manager, 'record');

          record.withTransaction(function(t) {
            t.recordBecameClean('inflight', record);
          });

          manager.goToState('saved');

          manager.send('invokeLifecycleCallbacks');
        }
      }),

      // Once the adapter indicates that the deletion has
      // been saved, the record enters the `saved` substate
      // of `deleted`.
      saved: DS.State.create({
        // FLAGS
        isDirty: false,

        invokeLifecycleCallbacks: function(manager) {
          var record = get(manager, 'record');
          record.trigger('didDelete', record);
        }
      })
    }),

    // If the adapter indicates that there was an unknown
    // error saving a record, the record enters the `error`
    // state.
    error: DS.State.create({
      isError: true,

      // EVENTS

      invokeLifecycleCallbacks: function(manager) {
        var record = get(manager, 'record');
        record.trigger('becameError', record);
      }
    })
  })
};

DS.StateManager = Ember.StateManager.extend({
  record: null,
  initialState: 'rootState',
  states: states
});

})();



(function() {
var get = Ember.get, set = Ember.set;

//  When a record is changed on the client, it is considered "dirty"--there are
//  pending changes that need to be saved to a persistence layer, such as a
//  server.
//
//  If the record is rolled back, it re-enters a clean state, any changes are
//  discarded, and its attributes are reset back to the last known good copy
//  of the data that came from the server.
//
//  If the record is committed, the changes are sent to the server to be saved,
//  and once the server confirms that they are valid, the record's "canonical"
//  data becomes the original canonical data plus the changes merged in.
//
//  A DataProxy is an object that encapsulates this change tracking. It
//  contains three buckets:
//
//  * `savedData` - the last-known copy of the data from the server
//  * `unsavedData` - a hash that contains any changes that have not yet
//     been committed
//  * `associations` - this is similar to `savedData`, but holds the client
//    ids of associated records
//
//  When setting a property on the object, the value is placed into the
//  `unsavedData` bucket:
//
//      proxy.set('key', 'value');
//
//      // unsavedData:
//      {
//        key: "value"
//      }
//
//  When retrieving a property from the object, it first looks to see
//  if that value exists in the `unsavedData` bucket, and returns it if so.
//  Otherwise, it returns the value from the `savedData` bucket.
//
//  When the adapter notifies a record that it has been saved, it merges the
//  `unsavedData` bucket into the `savedData` bucket. If the record's
//  transaction is rolled back, the `unsavedData` hash is simply discarded.
//
//  This object is a regular JS object for performance. It is only
//  used internally for bookkeeping purposes.

var DataProxy = DS._DataProxy = function(record) {
  this.record = record;

  this.unsavedData = {};

  this.associations = {};
};

DataProxy.prototype = {
  get: function(key) { return Ember.get(this, key); },
  set: function(key, value) { return Ember.set(this, key, value); },

  setAssociation: function(key, value) {
    this.associations[key] = value;
  },

  savedData: function() {
    var savedData = this._savedData;
    if (savedData) { return savedData; }

    var record = this.record,
        clientId = get(record, 'clientId'),
        store = get(record, 'store');

    if (store) {
      savedData = store.dataForRecord(record);
      this._savedData = savedData;
      return savedData;
    }
  },

  unknownProperty: function(key) {
    var unsavedData = this.unsavedData,
        associations = this.associations,
        savedData = this.savedData(),
        store;

    var value = unsavedData[key], association;

    // if this is a belongsTo association, this will
    // be a clientId.
    association = associations[key];

    if (association !== undefined) {
      store = get(this.record, 'store');
      return store.clientIdToId[association];
    }

    if (savedData && value === undefined) {
      value = savedData[key];
    }

    return value;
  },

  setUnknownProperty: function(key, value) {
    var record = this.record,
        unsavedData = this.unsavedData;

    unsavedData[key] = value;

    record.hashWasUpdated();

    return value;
  },

  commit: function() {
    this.saveData();

    this.record.notifyPropertyChange('data');
  },

  rollback: function() {
    this.unsavedData = {};

    this.record.notifyPropertyChange('data');
  },

  saveData: function() {
    var record = this.record;

    var unsavedData = this.unsavedData;
    var savedData = this.savedData();

    for (var prop in unsavedData) {
      if (unsavedData.hasOwnProperty(prop)) {
        savedData[prop] = unsavedData[prop];
        delete unsavedData[prop];
      }
    }
  },

  adapterDidUpdate: function() {
    this.unsavedData = {};
  }
};

})();



(function() {
var get = Ember.get, set = Ember.set, none = Ember.none;

var retrieveFromCurrentState = Ember.computed(function(key) {
  return get(get(this, 'stateManager.currentState'), key);
}).property('stateManager.currentState').cacheable();

DS.Model = Ember.Object.extend(Ember.Evented, {
  isLoaded: retrieveFromCurrentState,
  isDirty: retrieveFromCurrentState,
  isSaving: retrieveFromCurrentState,
  isDeleted: retrieveFromCurrentState,
  isError: retrieveFromCurrentState,
  isNew: retrieveFromCurrentState,
  isPending: retrieveFromCurrentState,
  isValid: retrieveFromCurrentState,

  clientId: null,
  transaction: null,
  stateManager: null,
  pendingQueue: null,
  errors: null,

  // because unknownProperty is used, any internal property
  // must be initialized here.
  primaryKey: 'id',
  id: Ember.computed(function(key, value) {
    var primaryKey = get(this, 'primaryKey'),
        data = get(this, 'data');

    if (arguments.length === 2) {
      set(data, primaryKey, value);
      return value;
    }

    var id = get(data, primaryKey);
    return id ? id : this._id;
  }).property('primaryKey', 'data'),

  // The following methods are callbacks invoked by `toJSON`. You
  // can override one of the callbacks to override specific behavior,
  // or toJSON itself.
  //
  // If you override toJSON, you can invoke these callbacks manually
  // to get the default behavior.

  /**
    Add the record's primary key to the JSON hash.

    The default implementation uses the record's specified `primaryKey`
    and the `id` computed property, which are passed in as parameters.

    @param {Object} json the JSON hash being built
    @param {Number|String} id the record's id
    @param {String} key the primaryKey for the record
  */
  addIdToJSON: function(json, id, key) {
    if (id) { json[key] = id; }
  },

  /**
    Add the attributes' current values to the JSON hash.

    The default implementation gets the current value of each
    attribute from the `data`, and uses a `defaultValue` if
    specified in the `DS.attr` definition.

    @param {Object} json the JSON hash being build
    @param {Ember.Map} attributes a Map of attributes
    @param {DataProxy} data the record's data, accessed with `get` and `set`.
  */
  addAttributesToJSON: function(json, attributes, data) {
    attributes.forEach(function(name, meta) {
      var key = meta.key(this.constructor),
          value = get(data, key);

      if (value === undefined) {
        value = meta.options.defaultValue;
      }

      json[key] = value;
    }, this);
  },

  /**
    Add the value of a `hasMany` association to the JSON hash.

    The default implementation honors the `embedded` option
    passed to `DS.hasMany`. If embedded, `toJSON` is recursively
    called on the child records. If not, the `id` of each
    record is added.

    Note that if a record is not embedded and does not
    yet have an `id` (usually provided by the server), it
    will not be included in the output.

    @param {Object} json the JSON hash being built
    @param {DataProxy} data the record's data, accessed with `get` and `set`.
    @param {Object} meta information about the association
    @param {Object} options options passed to `toJSON`
  */
  addHasManyToJSON: function(json, data, meta, options) {
    var key = meta.key,
        manyArray = get(this, key),
        records = [], i, l,
        clientId, id;

    if (meta.options.embedded) {
      // TODO: Avoid materializing embedded hashes if possible
      manyArray.forEach(function(record) {
        records.push(record.toJSON(options));
      });
    } else {
      var clientIds = get(manyArray, 'content');

      for (i=0, l=clientIds.length; i<l; i++) {
        clientId = clientIds[i];
        id = get(this, 'store').clientIdToId[clientId];

        if (id !== undefined) {
          records.push(id);
        }
      }
    }

    key = meta.options.key || get(this, 'namingConvention').keyToJSONKey(key);
    json[key] = records;
  },

  /**
    Add the value of a `belongsTo` association to the JSON hash.

    The default implementation always includes the `id`.

    @param {Object} json the JSON hash being built
    @param {DataProxy} data the record's data, accessed with `get` and `set`.
    @param {Object} meta information about the association
    @param {Object} options options passed to `toJSON`
  */
  addBelongsToToJSON: function(json, data, meta, options) {
    var key = meta.key, value, id;

    if (meta.options.embedded) {
      key = meta.options.key || get(this, 'namingConvention').keyToJSONKey(key);
      value = get(data.record, key);
      json[key] = value ? value.toJSON(options) : null;
    } else {
      key = meta.options.key || get(this, 'namingConvention').foreignKey(key);
      id = data.get(key);
      json[key] = none(id) ? null : id;
    }
  },
  /**
    Create a JSON representation of the record, including its `id`,
    attributes and associations. Honor any settings defined on the
    attributes or associations (such as `embedded` or `key`).
  */
  toJSON: function(options) {
    var data = get(this, 'data'),
        result = {},
        type = this.constructor,
        attributes = get(type, 'attributes'),
        primaryKey = get(this, 'primaryKey'),
        id = get(this, 'id'),
        store = get(this, 'store'),
        associations;

    options = options || {};

    // delegate to `addIdToJSON` callback
    this.addIdToJSON(result, id, primaryKey);

    // delegate to `addAttributesToJSON` callback
    this.addAttributesToJSON(result, attributes, data);

    associations = get(type, 'associationsByName');

    // add associations, delegating to `addHasManyToJSON` and
    // `addBelongsToToJSON`.
    associations.forEach(function(key, meta) {
      if (options.associations && meta.kind === 'hasMany') {
        this.addHasManyToJSON(result, data, meta, options);
      } else if (meta.kind === 'belongsTo') {
        this.addBelongsToToJSON(result, data, meta, options);
      }
    }, this);

    return result;
  },

  data: Ember.computed(function() {
    return new DS._DataProxy(this);
  }).cacheable(),

  didLoad: Ember.K,
  didUpdate: Ember.K,
  didCreate: Ember.K,
  didDelete: Ember.K,
  becameInvalid: Ember.K,
  becameError: Ember.K,

  init: function() {
    var stateManager = DS.StateManager.create({
      record: this
    });

    set(this, 'pendingQueue', {});

    set(this, 'stateManager', stateManager);
    stateManager.goToState('empty');
  },

  destroy: function() {
    if (!get(this, 'isDeleted')) {
      this.deleteRecord();
    }
    this._super();
  },

  send: function(name, context) {
    return get(this, 'stateManager').send(name, context);
  },

  withTransaction: function(fn) {
    var transaction = get(this, 'transaction');
    if (transaction) { fn(transaction); }
  },

  setProperty: function(key, value) {
    this.send('setProperty', { key: key, value: value });
  },

  deleteRecord: function() {
    this.send('deleteRecord');
  },

  waitingOn: function(record) {
    this.send('waitingOn', record);
  },

  notifyHashWasUpdated: function() {
    var store = get(this, 'store');
    if (store) {
      store.hashWasUpdated(this.constructor, get(this, 'clientId'), this);
    }
  },

  unknownProperty: function(key) {
    var data = get(this, 'data');

    if (data && key in data) {
      Ember.assert("You attempted to access the " + key + " property on a record without defining an attribute.", false);
    }
  },

  setUnknownProperty: function(key, value) {
    var data = get(this, 'data');

    if (data && key in data) {
      Ember.assert("You attempted to set the " + key + " property on a record without defining an attribute.", false);
    } else {
      return this._super(key, value);
    }
  },

  namingConvention: {
    keyToJSONKey: function(key) {
      // TODO: Strip off `is` from the front. Example: `isHipster` becomes `hipster`
      return Ember.String.decamelize(key);
    },

    foreignKey: function(key) {
      return Ember.String.decamelize(key) + '_id';
    }
  },

  /** @private */
  hashWasUpdated: function() {
    // At the end of the run loop, notify record arrays that
    // this record has changed so they can re-evaluate its contents
    // to determine membership.
    Ember.run.once(this, this.notifyHashWasUpdated);
  },

  dataDidChange: Ember.observer(function() {
    var associations = get(this.constructor, 'associationsByName'),
        data = get(this, 'data'), store = get(this, 'store'),
        idToClientId = store.idToClientId,
        cachedValue;

    associations.forEach(function(name, association) {
      if (association.kind === 'hasMany') {
        cachedValue = this.cacheFor(name);

        if (cachedValue) {
          var key = association.options.key || get(this, 'namingConvention').keyToJSONKey(name),
              ids = data.get(key) || [];
          
          var clientIds;   
          if(association.options.embedded) {
            clientIds = store.loadMany(association.type, ids).clientIds;
          } else {
            clientIds = Ember.EnumerableUtils.map(ids, function(id) {
              return store.clientIdForId(association.type, id);
            });
          }
          
          set(cachedValue, 'content', Ember.A(clientIds));
          cachedValue.fetch();
        }
      }
    }, this);
  }, 'data'),

  /**
    @private

    Override the default event firing from Ember.Evented to
    also call methods with the given name.
  */
  trigger: function(name) {
    Ember.tryInvoke(this, name, [].slice.call(arguments, 1));
    this._super.apply(this, arguments);
  }
});

// Helper function to generate store aliases.
// This returns a function that invokes the named alias
// on the default store, but injects the class as the
// first parameter.
var storeAlias = function(methodName) {
  return function() {
    var store = get(DS, 'defaultStore'),
        args = [].slice.call(arguments);

    args.unshift(this);
    return store[methodName].apply(store, args);
  };
};

DS.Model.reopenClass({
  isLoaded: storeAlias('recordIsLoaded'),
  find: storeAlias('find'),
  filter: storeAlias('filter'),

  _create: DS.Model.create,

  create: function() {
    throw new Ember.Error("You should not call `create` on a model. Instead, call `createRecord` with the attributes you would like to set.");
  },

  createRecord: storeAlias('createRecord')
});

})();



(function() {
var get = Ember.get;
DS.Model.reopenClass({
  attributes: Ember.computed(function() {
    var map = Ember.Map.create();

    this.eachComputedProperty(function(name, meta) {
      if (meta.isAttribute) { map.set(name, meta); }
    });

    return map;
  }).cacheable(),

  processAttributeKeys: function() {
    if (this.processedAttributeKeys) { return; }

    var namingConvention = this.proto().namingConvention;

    this.eachComputedProperty(function(name, meta) {
      if (meta.isAttribute && !meta.options.key) {
        meta.options.key = namingConvention.keyToJSONKey(name, this);
      }
    }, this);
  }
});

function getAttr(record, options, key) {
  var data = get(record, 'data');
  var value = get(data, key);

  if (value === undefined) {
    value = options.defaultValue;
  }

  return value;
}

DS.attr = function(type, options) {
  var transform = DS.attr.transforms[type];
  Ember.assert("Could not find model attribute of type " + type, !!transform);

  var transformFrom = transform.from;
  var transformTo = transform.to;

  options = options || {};

  var meta = {
    type: type,
    isAttribute: true,
    options: options,

    // this will ensure that the key always takes naming
    // conventions into consideration.
    key: function(recordType) {
      recordType.processAttributeKeys();
      return options.key;
    }
  };

  return Ember.computed(function(key, value) {
    var data;

    key = meta.key(this.constructor);

    if (arguments.length === 2) {
      value = transformTo(value);

      if (value !== getAttr(this, options, key)) {
        this.setProperty(key, value);
      }
    } else {
      value = getAttr(this, options, key);
    }

    return transformFrom(value);
  // `data` is never set directly. However, it may be
  // invalidated from the state manager's setData
  // event.
  }).property('data').cacheable().meta(meta);
};

DS.attr.transforms = {
  string: {
    from: function(serialized) {
      return Ember.none(serialized) ? null : String(serialized);
    },

    to: function(deserialized) {
      return Ember.none(deserialized) ? null : String(deserialized);
    }
  },

  number: {
    from: function(serialized) {
      return Ember.none(serialized) ? null : Number(serialized);
    },

    to: function(deserialized) {
      return Ember.none(deserialized) ? null : Number(deserialized);
    }
  },

  'boolean': {
    from: function(serialized) {
      return Boolean(serialized);
    },

    to: function(deserialized) {
      return Boolean(deserialized);
    }
  },

  date: {
    from: function(serialized) {
      var type = typeof serialized;

      if (type === "string" || type === "number") {
        return new Date(serialized);
      } else if (serialized === null || serialized === undefined) {
        // if the value is not present in the data,
        // return undefined, not null.
        return serialized;
      } else {
        return null;
      }
    },

    to: function(date) {
      if (date instanceof Date) {
        var days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
        var months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

        var pad = function(num) {
          return num < 10 ? "0"+num : ""+num;
        };

        var utcYear = date.getUTCFullYear(),
            utcMonth = date.getUTCMonth(),
            utcDayOfMonth = date.getUTCDate(),
            utcDay = date.getUTCDay(),
            utcHours = date.getUTCHours(),
            utcMinutes = date.getUTCMinutes(),
            utcSeconds = date.getUTCSeconds();


        var dayOfWeek = days[utcDay];
        var dayOfMonth = pad(utcDayOfMonth);
        var month = months[utcMonth];

        return dayOfWeek + ", " + dayOfMonth + " " + month + " " + utcYear + " " +
               pad(utcHours) + ":" + pad(utcMinutes) + ":" + pad(utcSeconds) + " GMT";
      } else if (date === undefined) {
        return undefined;
      } else {
        return null;
      }
    }
  }
};


})();



(function() {

})();



(function() {
var get = Ember.get, set = Ember.set,
    none = Ember.none;

var embeddedFindRecord = function(store, type, data, key, one) {
  var association = get(data, key);
  return none(association) ? undefined : store.load(type, association).id;
};

var referencedFindRecord = function(store, type, data, key, one) {
  return get(data, key);
};

var hasAssociation = function(type, options, one) {
  options = options || {};

  var embedded = options.embedded,
      findRecord = embedded ? embeddedFindRecord : referencedFindRecord;

  var meta = { type: type, isAssociation: true, options: options, kind: 'belongsTo' };

  return Ember.computed(function(key, value) {
    var data = get(this, 'data'), ids, id, association,
        store = get(this, 'store');

    if (typeof type === 'string') {
      type = get(this, type, false) || get(window, type);
    }

    if (arguments.length === 2) {
      key = options.key || get(this, 'namingConvention').foreignKey(key);
      this.send('setAssociation', { key: key, value: Ember.none(value) ? null : get(value, 'clientId') });
      //data.setAssociation(key, get(value, 'clientId'));
      // put the client id in `key` in the data hash
      return value;
    } else {
      // Embedded belongsTo associations should not look for
      // a foreign key.
      if (embedded) {
        key = options.key || get(this, 'namingConvention').keyToJSONKey(key);

      // Non-embedded associations should look for a foreign key.
      // For example, instead of person, we might look for person_id
      } else {
        key = options.key || get(this, 'namingConvention').foreignKey(key);
      }
      id = findRecord(store, type, data, key, true);
      association = id ? store.find(type, id) : null;
    }

    return association;
  }).property('data').cacheable().meta(meta);
};

DS.belongsTo = function(type, options) {
  Ember.assert("The type passed to DS.belongsTo must be defined", !!type);
  return hasAssociation(type, options);
};

})();



(function() {
var get = Ember.get, set = Ember.set;
var embeddedFindRecord = function(store, type, data, key) {
  var association = get(data, key);
  return association ? store.loadMany(type, association).ids : [];
};

var referencedFindRecord = function(store, type, data, key, one) {
  return get(data, key);
};

var hasAssociation = function(type, options) {
  options = options || {};

  var embedded = options.embedded,
      findRecord = embedded ? embeddedFindRecord : referencedFindRecord;

  var meta = { type: type, isAssociation: true, options: options, kind: 'hasMany' };

  return Ember.computed(function(key, value) {
    var data = get(this, 'data'),
        store = get(this, 'store'),
        ids, id, association;

    if (typeof type === 'string') {
      type = get(this, type, false) || get(window, type);
    }

    key = options.key || get(this, 'namingConvention').keyToJSONKey(key);
    ids = findRecord(store, type, data, key);
    association = store.findMany(type, ids || []);
    set(association, 'parentRecord', this);

    return association;
  }).property().cacheable().meta(meta);
};

DS.hasMany = function(type, options) {
  Ember.assert("The type passed to DS.hasMany must be defined", !!type);
  return hasAssociation(type, options);
};

})();



(function() {
var get = Ember.get;

DS.Model.reopenClass({
  typeForAssociation: function(name) {
    var association = get(this, 'associationsByName').get(name);
    return association && association.type;
  },

  associations: Ember.computed(function() {
    var map = Ember.Map.create();

    this.eachComputedProperty(function(name, meta) {
      if (meta.isAssociation) {
        var type = meta.type,
            typeList = map.get(type);

        if (typeof type === 'string') {
          type = get(this, type, false) || get(window, type);
          meta.type = type;
        }

        if (!typeList) {
          typeList = [];
          map.set(type, typeList);
        }

        typeList.push({ name: name, kind: meta.kind });
      }
    });

    return map;
  }).cacheable(),

  associationsByName: Ember.computed(function() {
    var map = Ember.Map.create(), type;

    this.eachComputedProperty(function(name, meta) {
      if (meta.isAssociation) {
        meta.key = name;
        type = meta.type;

        if (typeof type === 'string') {
          type = get(this, type, false) || get(window, type);
          meta.type = type;
        }

        map.set(name, meta);
      }
    });

    return map;
  }).cacheable()
});

})();



(function() {

})();



(function() {
/**
  An adapter is an object that receives requests from a store and
  translates them into the appropriate action to take against your
  persistence layer. The persistence layer is usually an HTTP API, but may
  be anything, such as the browser's local storage.

  ### Creating an Adapter

  First, create a new subclass of `DS.Adapter`:

      App.MyAdapter = DS.Adapter.extend({
        // ...your code here
      });

  To tell your store which adapter to use, set its `adapter` property:

      App.store = DS.Store.create({
        revision: 3,
        adapter: App.MyAdapter.create()
      });

  `DS.Adapter` is an abstract base class that you should override in your
  application to customize it for your backend. The minimum set of methods
  that you should implement is:

    * `find()`
    * `createRecord()`
    * `updateRecord()`
    * `deleteRecord()`

   To improve the network performance of your application, you can optimize
   your adapter by overriding these lower-level methods:

    * `findMany()`
    * `createRecords()`
    * `updateRecords()`
    * `deleteRecords()`
    * `commit()`

   For more information about the adapter API, please see `README.md`.
*/

DS.Adapter = Ember.Object.extend({
  /**
    The `find()` method is invoked when the store is asked for a record that
    has not previously been loaded. In response to `find()` being called, you
    should query your persistence layer for a record with the given ID. Once
    found, you can asynchronously call the store's `load()` method to load
    the record.

    Here is an example `find` implementation:

      find: function(store, type, id) {
        var url = type.url;
        url = url.fmt(id);

        jQuery.getJSON(url, function(data) {
            // data is a Hash of key/value pairs. If your server returns a
            // root, simply do something like:
            // store.load(type, id, data.person)
            store.load(type, id, data);
        });
      }
  */
  find: null,

  /**
    If the globally unique IDs for your records should be generated on the client,
    implement the `generateIdForRecord()` method. This method will be invoked
    each time you create a new record, and the value returned from it will be
    assigned to the record's `primaryKey`.

    Most traditional REST-like HTTP APIs will not use this method. Instead, the ID
    of the record will be set by the server, and your adapter will update the store
    with the new ID when it calls `didCreateRecord()`. Only implement this method if
    you intend to generate record IDs on the client-side.

    The `generateIdForRecord()` method will be invoked with the requesting store as
    the first parameter and the newly created record as the second parameter:

        generateIdForRecord: function(store, record) {
          var uuid = App.generateUUIDWithStatisticallyLowOddsOfCollision();
          return uuid;
        }
  */
  generateIdForRecord: null,

  commit: function(store, commitDetails) {
    commitDetails.updated.eachType(function(type, array) {
      this.updateRecords(store, type, array.slice());
    }, this);

    commitDetails.created.eachType(function(type, array) {
      this.createRecords(store, type, array.slice());
    }, this);

    commitDetails.deleted.eachType(function(type, array) {
      this.deleteRecords(store, type, array.slice());
    }, this);
  },

  createRecords: function(store, type, records) {
    records.forEach(function(record) {
      this.createRecord(store, type, record);
    }, this);
  },

  updateRecords: function(store, type, records) {
    records.forEach(function(record) {
      this.updateRecord(store, type, record);
    }, this);
  },

  deleteRecords: function(store, type, records) {
    records.forEach(function(record) {
      this.deleteRecord(store, type, record);
    }, this);
  },

  findMany: function(store, type, ids) {
    ids.forEach(function(id) {
      this.find(store, type, id);
    }, this);
  }
});

})();



(function() {
var set = Ember.set;

Ember.onLoad('application', function(app) {
  app.registerInjection({
    name: "store",
    before: "controllers",

    injection: function(app, stateManager, property) {
      if (property === 'Store') {
        set(stateManager, 'store', app[property].create());
      }
    }
  });

  app.registerInjection({
    name: "giveStoreToControllers",

    injection: function(app, stateManager, property) {
      if (property.match(/Controller$/)) {
        var controllerName = property.charAt(0).toLowerCase() + property.substr(1);
        var store = stateManager.get('store');
        var controller = stateManager.get(controllerName);

        controller.set('store', store);
      }
    }
  });
});

})();



(function() {
var get = Ember.get;

DS.FixtureAdapter = DS.Adapter.extend({

  simulateRemoteResponse: true,

  latency: 50,

  /*
    Implement this method in order to provide data associated with a type
  */
  fixturesForType: function(type) {
    return type.FIXTURES ? Ember.A(type.FIXTURES) : null;
  },

  /*
    Implement this method in order to query fixtures data
  */
  queryFixtures: function(fixtures, query) {
    return fixtures;
  },

  /*
    Implement this method in order to provide provide json for CRUD methods
  */
  mockJSON: function(type, record) {
    return record.toJSON({associations: true});
  },

  /*
    Adapter methods
  */
  generateIdForRecord: function(store, record) {
    return Ember.guidFor(record);
  },

  find: function(store, type, id) {
    var fixtures = this.fixturesForType(type);

    Ember.assert("Unable to find fixtures for model type "+type.toString(), !!fixtures);

    if (fixtures) {
      fixtures = fixtures.findProperty('id', id);
    }

    if (fixtures) {
      this.simulateRemoteCall(function() {
        store.load(type, fixtures);
      }, store, type);
    }
  },

  findMany: function(store, type, ids) {
    var fixtures = this.fixturesForType(type);

    Ember.assert("Unable to find fixtures for model type "+type.toString(), !!fixtures);

    if (fixtures) {
      fixtures = fixtures.filter(function(item) {
        return ids.indexOf(item.id) !== -1;
      });
    }
  
    if (fixtures) {
      this.simulateRemoteCall(function() {
        store.loadMany(type, fixtures);
      }, store, type);
    }
  },

  findAll: function(store, type) {
    var fixtures = this.fixturesForType(type);

    Ember.assert("Unable to find fixtures for model type "+type.toString(), !!fixtures);

    this.simulateRemoteCall(function() {
      store.loadMany(type, fixtures);
    }, store, type);
  },

  findQuery: function(store, type, query, array) {
    var fixtures = this.fixturesForType(type);
    
    Ember.assert("Unable to find fixtures for model type "+type.toString(), !!fixtures);

    fixtures = this.queryFixtures(fixtures, query);

    if (fixtures) {
      this.simulateRemoteCall(function() {
        array.load(fixtures);
      }, store, type);
    }
  },

  createRecord: function(store, type, record) {
    var fixture = this.mockJSON(type, record);

    fixture.id = this.generateIdForRecord(store, record);

    this.simulateRemoteCall(function() {
      store.didCreateRecord(record, fixture);
    }, store, type, record);
  },

  updateRecord: function(store, type, record) {
    var fixture = this.mockJSON(type, record);

    this.simulateRemoteCall(function() {
      store.didUpdateRecord(record, fixture);
    }, store, type, record);
  },

  deleteRecord: function(store, type, record) {
    this.simulateRemoteCall(function() {
      store.didDeleteRecord(record);
    }, store, type, record);
  },

  /*
    @private
  */
  simulateRemoteCall: function(callback, store, type, record) {
    if (get(this, 'simulateRemoteResponse')) {
      setTimeout(callback, get(this, 'latency'));
    } else {
      callback();
    }
  }
});

DS.fixtureAdapter = DS.FixtureAdapter.create();

})();



(function() {
/*global jQuery*/

var get = Ember.get, set = Ember.set;

DS.RESTAdapter = DS.Adapter.extend({
  bulkCommit: false,
	
  createRecord: function(store, type, record) {
    var root = this.rootForType(type);

    var data = {};
    data[root] = record.toJSON();

    this.ajax(this.buildURL(root), "POST", {
      data: data,
      context: this,
      success: function(json) {
        this.didCreateRecord(store, type, record, json);
      }
    });
  },

  didCreateRecord: function(store, type, record, json) {
    var root = this.rootForType(type);

    this.sideload(store, type, json, root);
    store.didCreateRecord(record, json[root]);
  },

  createRecords: function(store, type, records) {
    if (get(this, 'bulkCommit') === false) {
      return this._super(store, type, records);
    }

    var root = this.rootForType(type),
        plural = this.pluralize(root);

    var data = {};
    data[plural] = records.map(function(record) {
      return record.toJSON();
    });

    this.ajax(this.buildURL(root), "POST", {
      data: data,
      context: this,
      success: function(json) {
        this.didCreateRecords(store, type, records, json);
      }
    });
  },

  didCreateRecords: function(store, type, records, json) {
    var root = this.pluralize(this.rootForType(type));

    this.sideload(store, type, json, root);
    store.didCreateRecords(type, records, json[root]);
  },

  updateRecord: function(store, type, record) {
    var id = get(record, 'id');
    var root = this.rootForType(type);

    var data = {};
    data[root] = record.toJSON();

    this.ajax(this.buildURL(root, id), "PUT", {
      data: data,
      context: this,
      success: function(json) {
        this.didUpdateRecord(store, type, record, json);
      }
    });
  },

  didUpdateRecord: function(store, type, record, json) {
    var root = this.rootForType(type);

    this.sideload(store, type, json, root);
    store.didUpdateRecord(record, json && json[root]);
  },

  updateRecords: function(store, type, records) {
    if (get(this, 'bulkCommit') === false) {
      return this._super(store, type, records);
    }

    var root = this.rootForType(type),
        plural = this.pluralize(root);

    var data = {};
    data[plural] = records.map(function(record) {
      return record.toJSON();
    });

    this.ajax(this.buildURL(root, "bulk"), "PUT", {
      data: data,
      context: this,
      success: function(json) {
        this.didUpdateRecords(store, type, records, json);
      }
    });
  },

  didUpdateRecords: function(store, type, records, json) {
    var root = this.pluralize(this.rootForType(type));

    this.sideload(store, type, json, root);
    store.didUpdateRecords(records, json[root]);
  },

  deleteRecord: function(store, type, record) {
    var id = get(record, 'id');
    var root = this.rootForType(type);

    this.ajax(this.buildURL(root, id), "DELETE", {
      context: this,
      success: function(json) {
        this.didDeleteRecord(store, type, record, json);
      }
    });
  },

  didDeleteRecord: function(store, type, record, json) {
    if (json) { this.sideload(store, type, json); }
    store.didDeleteRecord(record);
  },

  deleteRecords: function(store, type, records) {
    if (get(this, 'bulkCommit') === false) {
      return this._super(store, type, records);
    }

    var root = this.rootForType(type),
        plural = this.pluralize(root);

    var data = {};
    data[plural] = records.map(function(record) {
      return get(record, 'id');
    });

    this.ajax(this.buildURL(root, 'bulk'), "DELETE", {
      data: data,
      context: this,
      success: function(json) {
        this.didDeleteRecords(store, type, records, json);
      }
    });
  },

  didDeleteRecords: function(store, type, records, json) {
    if (json) { this.sideload(store, type, json); }
    store.didDeleteRecords(records);
  },

  find: function(store, type, id) {
    var root = this.rootForType(type);

    this.ajax(this.buildURL(root, id), "GET", {
      success: function(json) {
        this.sideload(store, type, json, root);
        store.load(type, json[root]);
      }
    });
  },

  findMany: function(store, type, ids) {
    var root = this.rootForType(type), plural = this.pluralize(root);

    this.ajax(this.buildURL(root), "GET", {
      data: { ids: ids },
      success: function(json) {
        this.sideload(store, type, json, plural);
        store.loadMany(type, json[plural]);
      }
    });
  },

  findAll: function(store, type) {
    var root = this.rootForType(type), plural = this.pluralize(root);

    this.ajax(this.buildURL(root), "GET", {
      success: function(json) {
        this.sideload(store, type, json, plural);
        store.loadMany(type, json[plural]);
      }
    });
  },

  findQuery: function(store, type, query, recordArray) {
    var root = this.rootForType(type), plural = this.pluralize(root);

    this.ajax(this.buildURL(root), "GET", {
      data: query,
      success: function(json) {
        this.sideload(store, type, json, plural);
        recordArray.load(json[plural]);
      }
    });
  },

  // HELPERS

  plurals: {},

  // define a plurals hash in your subclass to define
  // special-case pluralization
  pluralize: function(name) {
    return this.plurals[name] || name + "s";
  },

  rootForType: function(type) {
    if (type.url) { return type.url; }

    // use the last part of the name as the URL
    var parts = type.toString().split(".");
    var name = parts[parts.length - 1];
    return name.replace(/([A-Z])/g, '_$1').toLowerCase().slice(1);
  },

  ajax: function(url, type, hash) {
    hash.url = url;
    hash.type = type;
    hash.dataType = 'json';
    hash.contentType = 'application/json; charset=utf-8';
    hash.context = this;

    if (hash.data && type !== 'GET') {
      hash.data = JSON.stringify(hash.data);
    }

    jQuery.ajax(hash);
  },

  sideload: function(store, type, json, root) {
    var sideloadedType, mappings, loaded = {};

    loaded[root] = true;

    for (var prop in json) {
      if (!json.hasOwnProperty(prop)) { continue; }
      if (prop === root) { continue; }

      sideloadedType = type.typeForAssociation(prop);

      if (!sideloadedType) {
        mappings = get(this, 'mappings');
        Ember.assert("Your server returned a hash with the key " + prop + " but you have no mappings", !!mappings);

        sideloadedType = get(mappings, prop);

        if (typeof sideloadedType === 'string') {
          sideloadedType = get(window, sideloadedType);
        }

        Ember.assert("Your server returned a hash with the key " + prop + " but you have no mapping for it", !!sideloadedType);
      }

      this.sideloadAssociations(store, sideloadedType, json, prop, loaded);
    }
  },

  sideloadAssociations: function(store, type, json, prop, loaded) {
    loaded[prop] = true;

    get(type, 'associationsByName').forEach(function(key, meta) {
      key = meta.key || key;
      if (meta.kind === 'belongsTo') {
        key = this.pluralize(key);
      }
      if (json[key] && !loaded[key]) {
        this.sideloadAssociations(store, meta.type, json, key, loaded);
      }
    }, this);

    this.loadValue(store, type, json[prop]);
  },

  loadValue: function(store, type, value) {
    if (value instanceof Array) {
      store.loadMany(type, value);
    } else {
      store.load(type, value);
    }
  },

  buildURL: function(record, suffix) {
    var url = [""];

    Ember.assert("Namespace URL (" + this.namespace + ") must not start with slash", !this.namespace || this.namespace.toString().charAt(0) !== "/");
    Ember.assert("Record URL (" + record + ") must not start with slash", !record || record.toString().charAt(0) !== "/");
    Ember.assert("URL suffix (" + suffix + ") must not start with slash", !suffix || suffix.toString().charAt(0) !== "/");

    if (this.namespace !== undefined) {
      url.push(this.namespace);
    }

    url.push(this.pluralize(record));
    if (suffix !== undefined) {
      url.push(suffix);
    }

    return url.join("/");
  }
});


})();



(function() {
//Copyright (C) 2011 by Living Social, Inc.

//Permission is hereby granted, free of charge, to any person obtaining a copy of
//this software and associated documentation files (the "Software"), to deal in
//the Software without restriction, including without limitation the rights to
//use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
//of the Software, and to permit persons to whom the Software is furnished to do
//so, subject to the following conditions:

//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.

//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

})();

