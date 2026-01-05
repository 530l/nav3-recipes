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

package com.example.nav3recipes.conditional

import androidx.navigation3.runtime.NavBackStack

/**
 * 中文：提供带条件访问控制的导航帮助类。如果用户尝试导航到一个需要登录的目标（
 * 即 [ConditionalNavKey.requiresLogin] 为 true），但当前未登录，
 * Navigator 会通过 `onNavigateToRestrictedKey` 将用户重定向到登录页对应的 Key。
 *
 * 属性说明：
 * @property backStack 由此类修改的导航回退栈（NavBackStack），用于添加/移除导航 key。
 * @property onNavigateToRestrictedKey 当尝试导航到受限（需要登录）的 key 时调用的 lambda。
 *   该 lambda 应返回代表登录页面的 key，参数为用户原始目标 key，方便登录后重定向回原目标。
 * @property isLoggedIn 返回当前用户是否已登录的 lambda，用于判断是否需要重定向到登录页。
 */
class Navigator(
    private val backStack: NavBackStack<ConditionalNavKey>,
    private val onNavigateToRestrictedKey: (targetKey: ConditionalNavKey?) -> ConditionalNavKey,
    private val isLoggedIn: () -> Boolean,
) {
    // 导航到指定的 key。如果目标需要登录且用户未登录，则通过 onNavigateToRestrictedKey 获取登录页 key 并导航到登录页；否则直接导航到目标 key。
    fun navigate(key: ConditionalNavKey) {
        if (key.requiresLogin && !isLoggedIn()) {
            val loginKey = onNavigateToRestrictedKey(key)
            backStack.add(loginKey)
        } else {
            backStack.add(key)
        }
    }

    // 返回上一个 entry（从 backStack 中移除最后一个），如果为空则返回 null。
    fun goBack() = backStack.removeLastOrNull()
}