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

package com.example.nav3recipes.results.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * CompositionLocal 对象，用于在 Compose 树中提供和访问 ResultEventBus。
 */
object LocalResultEventBus {

    // 定义一个可提供的 CompositionLocal，默认值为 null
    private val LocalResultEventBus: ProvidableCompositionLocal<ResultEventBus?> =
        compositionLocalOf { null }

    /**
     * 获取当前作用域中的 ResultEventBus 实例。
     * 如果没有提供，则抛出错误。
     */
    val current: ResultEventBus
        @Composable
        get() = LocalResultEventBus.current ?: error("No ResultEventBus has been provided")

    /**
     * 提供一个 ResultEventBus 实例给子 Composable 使用。
     * 用法：LocalResultEventBus provides myBus
     */
    infix fun provides(
        bus: ResultEventBus
    ): ProvidedValue<ResultEventBus?> {
        return LocalResultEventBus.provides(bus)
    }
}

/**
 * 一个基于 Channel 的事件总线，用于在不同屏幕/组件之间传递一次性结果。
 *
 * 设计目标：替代传统的 startActivityForResult，适用于 Compose 导航场景。
 */
class ResultEventBus {

    /**
     * 内部存储：每个 resultKey 对应一个 Channel，用于发送/接收结果。
     * 注意：Channel 是线程安全的，适合跨协程通信。
     */
    val channelMap: MutableMap<String, Channel<Any?>> = mutableMapOf()

    /**
     * 获取指定 key 的结果流（Flow）。
     * 使用 reified T 实现类型推断，但实际返回的是 Any?，需外部强转（由 ResultEffect 处理）。
     */
    inline fun <reified T> getResultFlow(resultKey: String = T::class.toString()) =
        channelMap[resultKey]?.receiveAsFlow()

    /**
     * 向指定 key 的 Channel 发送一个结果。
     * 如果该 key 尚未创建 Channel，则先创建一个（容量为 BUFFERED，溢出时挂起）。
     */
    inline fun <reified T> sendResult(resultKey: String = T::class.toString(), result: T) {
        if (!channelMap.contains(resultKey)) {
            // 创建新 Channel：缓冲模式，溢出时 SUSPEND（避免丢失事件）
            channelMap[resultKey] = Channel(
                capacity = BUFFERED,
                onBufferOverflow = BufferOverflow.SUSPEND
            )
        }
        // 尝试发送结果（非阻塞）
        channelMap[resultKey]?.trySend(result)
    }

    /**
     * 清理指定 key 的 Channel，释放资源。
     * 通常在不再需要监听结果时调用（可选，防止内存泄漏）。
     */
    inline fun <reified T> removeResult(resultKey: String = T::class.toString()) {
        channelMap.remove(resultKey)
    }
}