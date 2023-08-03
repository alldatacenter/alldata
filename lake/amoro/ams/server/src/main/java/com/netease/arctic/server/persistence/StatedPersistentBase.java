package com.netease.arctic.server.persistence;

import com.clearspring.analytics.util.Lists;
import com.google.common.collect.Maps;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class StatedPersistentBase extends PersistentBase {

  private static final Map<Class<? extends PersistentBase>, List<State>> stateMetaCache = Maps.newConcurrentMap();
  private Lock stateLock = new ReentrantLock();
  private List<State> states = Lists.newArrayList();

  protected StatedPersistentBase() {
    initStateFields();
  }

  protected final void invokeConsisitency(Runnable runnable) {
    stateLock.lock();
    try {
      retainStates();
      runnable.run();
    } catch (Throwable throwable) {
      restoreStates();
      throw throwable;
    } finally {
      stateLock.unlock();
    }
  }

  protected final <T> T invokeConsisitency(Supplier<T> supplier) {
    stateLock.lock();
    try {
      retainStates();
      return supplier.get();
    } catch (Throwable throwable) {
      restoreStates();
      throw throwable;
    } finally {
      stateLock.unlock();
    }
  }

  protected final void invokeInStateLock(Runnable runnable) {
    stateLock.lock();
    try {
      runnable.run();
    } catch (Throwable throwable) {
      throw throwable;
    } finally {
      stateLock.unlock();
    }
  }

  protected final <T> T invokeInStateLock(Supplier<T> supplier) {
    stateLock.lock();
    try {
      return supplier.get();
    } catch (Throwable throwable) {
      throw throwable;
    } finally {
      stateLock.unlock();
    }
  }

  private void initStateFields() {
    states = stateMetaCache.computeIfAbsent(getClass(), clz -> {
      List<State> states = new ArrayList<>();
      while (clz != PersistentBase.class) {
        for (Field field : clz.getDeclaredFields()) {
          if (field.isAnnotationPresent(StateField.class)) {
            states.add(new State(field));
          }
        }
        clz = clz.getSuperclass().asSubclass(PersistentBase.class);
      }
      return states;
    }).stream()
        .map(state -> new State(state.field))
        .collect(Collectors.toList());
  }

  private void retainStates() {
    states.forEach(State::retain);
  }

  private void restoreStates() {
    states.forEach(State::restore);
  }

  private class State {
    private Object value;
    private Field field;

    State(Field field) {
      this.field = field;
    }

    void retain() {
      try {
        field.setAccessible(true);
        value = field.get(StatedPersistentBase.this);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
    }

    void restore() {
      try {
        field.set(StatedPersistentBase.this, value);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface StateField {
  }
}

