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
import androidx.compose.runtime.LaunchedEffect

/**
 * 一个 Compose Effect，用于监听并接收跨页面的一次性结果。
 *
 * 它通过 [ResultEventBus] 监听指定 key 的结果流，并在收到结果时调用 [onResult]。
 *
 * @param resultEventBus 结果事件总线，默认从 LocalResultEventBus 获取
 * @param resultKey 用于标识该结果的唯一键。默认使用泛型 T 的类名作为 key
 * @param onResult 当收到类型为 T 的结果时触发的回调（suspend 函数）
 */
@Composable
inline fun <reified T> ResultEffect(
    resultEventBus: ResultEventBus = LocalResultEventBus.current,
    resultKey: String = T::class.toString(), // 默认用类型名作 key，也可自定义
    crossinline onResult: suspend (T) -> Unit
) {
    // 当 resultKey 或对应 channel 变化时，重新启动协程
    LaunchedEffect(resultKey, resultEventBus.channelMap[resultKey]) {
        // 获取该 key 对应的结果 Flow（可能是 null）
        resultEventBus.getResultFlow<T>(resultKey)?.collect { result ->
            // 收到结果后，调用用户传入的处理逻辑
            onResult.invoke(result as T)
        }
    }
}