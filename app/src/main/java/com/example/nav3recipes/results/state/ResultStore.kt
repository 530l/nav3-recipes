/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nav3recipes.results.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Local for storing results in a [ResultStore]
 *
 * 用于在 Compose 树中提供和访问 ResultStore 的 CompositionLocal。
 */
object LocalResultStore {
    // 定义一个可被提供的 CompositionLocal，初始值为 null
    private val LocalResultStore: ProvidableCompositionLocal<ResultStore?> =
        compositionLocalOf { null }

    /**
     * The current [ResultStore]
     *
     * 获取当前作用域中的 ResultStore 实例。
     * 如果没有通过 CompositionLocalProvider 提供，则抛出错误。
     */
    val current: ResultStore
        @Composable
        get() = LocalResultStore.current ?: error("No ResultStore has been provided")

    /**
     * Provides a [ResultStore] to the composition
     *
     * 提供一个 ResultStore 给子 Composable 使用（使用 infix 语法糖）。
     */
    infix fun provides(
        store: ResultStore
    ): ProvidedValue<ResultStore?> {
        return LocalResultStore.provides(store)
    }
}

/**
 * Provides a [ResultStore] that will be remembered across configuration changes.
 *
 * 创建一个能在配置变更（如屏幕旋转）甚至进程死亡后恢复的 ResultStore。
 */
@Composable
fun rememberResultStore() : ResultStore {
    // 使用 rememberSaveable 确保 ResultStore 的状态可被保存和恢复
    return rememberSaveable(saver = ResultStoreSaver()) {
        ResultStore()
    }
}

/**
 * A store for passing results between multiple sets of screens.
 *
 * It provides a solution for state based results.
 *
 * 一个用于在多个屏幕之间传递结果的状态存储器。
 * 适用于需要“持久化状态”的场景（如表单草稿、搜索条件等）。
 */
class ResultStore {

    /**
     * Map from the result key to a mutable state of the result.
     *
     * 内部存储：每个 resultKey 对应一个 MutableState<Any?>，
     * 使得结果变化时能触发 Compose 重组。
     */
    val resultStateMap: MutableMap<String, MutableState<Any?>> = mutableMapOf()

    /**
     * Retrieves the current result of the given resultKey.
     *
     * 获取指定 key 的当前结果值，并尝试强转为类型 T。
     * 注意：如果类型不匹配，会抛出 ClassCastException。
     */
    inline fun <reified T> getResultState(resultKey: String = T::class.toString()) =
        resultStateMap[resultKey]?.value as T

    /**
     * Sets the result for the given resultKey.
     *
     * 设置指定 key 的结果值。
     * 每次调用都会创建一个新的 MutableState（覆盖旧值）。
     */
    inline fun <reified T> setResult(resultKey: String = T::class.toString(), result: T) {
        resultStateMap[resultKey] = mutableStateOf(result)
    }

    /**
     * Removes all results associated with the given key from the store.
     *
     * 从存储中移除指定 key 的结果，释放内存。
     */
    inline fun <reified T> removeResult(resultKey: String = T::class.toString()) {
        resultStateMap.remove(resultKey)
    }
}

/**
 * Saver to save and restore the NavController across config change and process death.
 *
 * 注意：此处注释原文写的是 "NavController"，但实际是用于 ResultStore。
 * 这可能是复制粘贴错误。正确用途是：保存和恢复 ResultStore 的状态。
 */
private fun ResultStoreSaver(): Saver<ResultStore, *> =
    Saver(
        // 保存时：直接保存整个 resultStateMap（要求其中的 value 是可保存类型）
        save = { it.resultStateMap },
        // 恢复时：新建 ResultStore 并将保存的 map 全部 put 进去
        restore = { ResultStore().apply { resultStateMap.putAll(it) } },
    )