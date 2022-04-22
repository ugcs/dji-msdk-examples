// Sometimes not all functions of the toolset are in use
@file:Suppress("unused")

package com.example.ugcssample.drone

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.ugcssample.DJIErrorException
import com.example.ugcssample.DjiUnknownException
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks
import dji.common.util.CommonCallbacks.CompletionCallbackWith
import dji.keysdk.callback.SetCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java8.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

suspend inline fun <T1, T2> suspendCoroutine(f: KFunction3<T1, T2, SetCallback?, Unit>, p1: T1, p2: T2) {
    return kotlin.coroutines.suspendCoroutine {continuation ->
        f(p1, p2, object : SetCallback {
            override fun onSuccess() {
                continuation.resume(Unit)
            }

            override fun onFailure(djiError: DJIError) {
                continuation.resumeWithException(
                        DJIErrorException(djiError)
                )
            }
        })
    }
}

suspend inline fun <T1, T2> suspendCoroutine(p1: T1, p2: T2, f: KFunction3<T1, T2, SetCallback?, Unit>) {
    return kotlin.coroutines.suspendCoroutine {continuation ->
        f(p1, p2, object : SetCallback {
            override fun onSuccess() {
                continuation.resume(Unit)
            }

            override fun onFailure(djiError: DJIError) {
                continuation.resumeWithException(
                        DJIErrorException(djiError)
                )
            }
        })
    }
}

suspend inline fun <T1> suspendCoroutine(p1: T1, f: KFunction2<T1, SetCallback?, Unit>) {
    return kotlin.coroutines.suspendCoroutine {continuation ->
        f(p1, object : SetCallback {
            override fun onSuccess() {
                continuation.resume(Unit)
            }

            override fun onFailure(djiError: DJIError) {
                continuation.resumeWithException(
                        DJIErrorException(djiError)
                )
            }
        })
    }
}

suspend inline fun <T1> suspendCoroutineCompletion(f: KFunction2<T1, CommonCallbacks.CompletionCallback<DJIError>?, Unit>, p0: T1) {
    return kotlin.coroutines.suspendCoroutine {continuation ->
        f(p0){ djiError ->
            if (djiError == null) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(
                        DJIErrorException(djiError)
                )
            }
        }
    }
}

suspend inline fun <R> suspendCoroutine(f: KFunction1<CompletionCallbackWith<R>, Unit>): R {
    return kotlin.coroutines.suspendCoroutine {continuation ->
        f(object : CompletionCallbackWith<R> {
            override fun onSuccess(result: R) {
                continuation.resume(result)
            }

            override fun onFailure(djiError: DJIError?) {
                if (djiError != null){
                    continuation.resumeWithException(
                            DJIErrorException(djiError)
                    )
                } else {
                    continuation.resumeWithException(DjiUnknownException())
                }
            }
        })
    }
}

suspend inline fun <R1,R2> suspendCoroutineTwo(f: KFunction1<CommonCallbacks.CompletionCallbackWithTwoParam<R1,R2>, Unit>): Pair<R1,R2> {
    return kotlin.coroutines.suspendCoroutine {continuation ->
        f(object : CommonCallbacks.CompletionCallbackWithTwoParam<R1,R2> {
            override fun onSuccess(res0: R1, res1 : R2) {
                continuation.resume(Pair(res0, res1))
            }

            override fun onFailure(djiError: DJIError?) {
                if (djiError != null){
                    continuation.resumeWithException(
                            DJIErrorException(djiError)
                    )
                } else {
                    continuation.resumeWithException(DjiUnknownException())
                }
            }
        })
    }
}

/**
 * @see com.ugcs.android.tools.ToFutureAdapter
 */
class DjiToFutureAdapter {

    @FunctionalInterface
    interface AsyncFunction<T> {
        fun execute(callback: T)
    }

    @FunctionalInterface
    interface AsyncFunction2<T1, T2> {
        fun execute(p1: T1, callback: T2)
    }

    companion object {

        @RequiresApi(Build.VERSION_CODES.N)
        @JvmStatic
        fun <R> getFuture(f: AsyncFunction<CompletionCallbackWith<R>>): CompletableFuture<R> {
            val future = CompletableFuture<R>()
            f.execute(object : CompletionCallbackWith<R> {
                override fun onSuccess(result: R?) {
                    future.complete(result)
                }

                override fun onFailure(djiError: DJIError?) {
                    if (djiError != null) {
                        future.completeExceptionally(
                                DJIErrorException(djiError)
                        )
                    } else {
                        future.completeExceptionally(DjiUnknownException())
                    }
                }
            })

            return future
        }
        @RequiresApi(Build.VERSION_CODES.N)
        @JvmStatic
        fun <R1,R2> getFutureTwoParam(f: AsyncFunction<CommonCallbacks.CompletionCallbackWithTwoParam<R1,R2>>): CompletableFuture<Pair<R1?, R2?>> {
            val future = CompletableFuture<Pair<R1?,R2?>>()
            f.execute(object : CommonCallbacks.CompletionCallbackWithTwoParam<R1,R2> {
                override fun onSuccess(res1: R1?, res2 : R2?) {
                    future.complete(Pair(res1,res2))
                }

                override fun onFailure(djiError: DJIError?) {
                    if (djiError != null) {
                        future.completeExceptionally(
                                DJIErrorException(djiError)
                        )
                    } else {
                        future.completeExceptionally(DjiUnknownException())
                    }
                }
            })

            return future
        }

        @RequiresApi(Build.VERSION_CODES.N)
        @JvmStatic
        fun <T> getFuture(f: AsyncFunction2<T, CommonCallbacks.CompletionCallback<DJIError>>, value: T): CompletableFuture<Void> {
            val future = CompletableFuture<Void>()
            f.execute(value, CommonCallbacks.CompletionCallback { djiError ->
                if (djiError == null)
                    future.complete(null)
                else
                    future.completeExceptionally(DJIErrorException(djiError))
            })

            return future
        }

        @RequiresApi(Build.VERSION_CODES.N)
        @JvmStatic
        fun <T1, T2> getFuture(f: KFunction3<T1, T2, SetCallback?, Unit>, p1: T1, p2: T2): CompletableFuture<Void> {
            val future = CompletableFuture<Void>()
            GlobalScope.launch {
                f(p1, p2, object : SetCallback {
                    override fun onSuccess() {
                        future.complete(null)
                    }

                    override fun onFailure(djiError: DJIError) {
                        future.completeExceptionally(
                                DJIErrorException(djiError)
                        )
                    }
                })
            }

            return future
        }
    }
}