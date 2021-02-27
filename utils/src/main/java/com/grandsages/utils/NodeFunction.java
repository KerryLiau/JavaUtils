package com.grandsages.utils;

@FunctionalInterface
public interface NodeFunction<T> {
    void invoke(T data);
}
