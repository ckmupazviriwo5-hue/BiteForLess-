package com.example.biteforlessfoodbridge.data.remote;

/** Async result delivered on the Android main thread. */
public interface SupaResult<T> {
    void onSuccess(T value);
    void onError(String message);
}
