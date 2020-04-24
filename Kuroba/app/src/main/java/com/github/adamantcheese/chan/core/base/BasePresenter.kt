package com.github.adamantcheese.chan.core.base

import androidx.annotation.CallSuper
import com.github.adamantcheese.common.ModularResult
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

abstract class BasePresenter<V> {
    private var view: V? = null
    private val initialized = AtomicBoolean(false)

    protected val scope = MainScope() + CoroutineName("BasePresenter")

    @CallSuper
    open fun onCreate(view: V) {
        if (!initialized.compareAndSet(false, true)) {
            throw RuntimeException("Attempt to double create")
        }

        this.view = view
    }

    @CallSuper
    open fun onDestroy() {
        if (!initialized.compareAndSet(true, false)) {
            throw RuntimeException("Attempt to destroy without creating first")
        }

        this.view = null
        scope.cancel()
    }

    fun withViewNormal(func: V.() -> Unit) {
        if (!initialized.get()) {
            throw RuntimeException("Not initialized!")
        }

        view?.let { v -> func(v) }
    }

    /**
     * This version of withView method does not block anything so you can call any other suspend
     * method from it without the fear of ANRing the app.
     *
     * We do not allow returning anything from these two methods on purpose because UI code should
     * be "stupidly simple". It should just do whatever it's told to do. It shouldn't return anything
     * back to presenter.
     * */
    suspend fun withView(func: suspend V.() -> Unit) {
        if (!initialized.get()) {
            throw RuntimeException("Not initialized!")
        }

        view?.let { v ->
            withContext(scope.coroutineContext) {
                val result = ModularResult.safeRun { func(v) }
                handleResult(result)
            }
        }
    }

    /**
     * Allows you to call withView function from a non-suspend method. This blocks the caller thread
     * so the code inside the lambda should be light-weight so the app does not ANR (like updating
     * the UI for example)
     * */
    @Suppress("ConstantConditionIf")
    fun withViewBlocking(func: suspend V.() -> Unit) {
        if (!initialized.get()) {
            throw RuntimeException("Not initialized!")
        }

        view?.let { v ->
            runBlocking {
                val result = ModularResult.safeRun { func(v) }
                handleResult(result)
            }
        }
    }

    private fun handleResult(result: ModularResult<Unit>) {
        if (result is ModularResult.Error) {
            when (result.error) {
                is InterruptedException -> {
                    // Restore the interrupt flag.
                    Thread.currentThread().interrupt()

                    // Fine, blocking code threw InterruptedException. Don't do anything.
                }
                is CancellationException -> {
                    // Fine, the coroutine was canceled. Don't do anything.
                }
                else -> throw result.error
            }
        }
    }

    companion object {
        private const val TAG = "BasePresenter"
    }
}