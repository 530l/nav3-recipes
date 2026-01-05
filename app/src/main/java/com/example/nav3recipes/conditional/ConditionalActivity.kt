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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import com.example.nav3recipes.content.ContentBlue
import com.example.nav3recipes.content.ContentGreen
import com.example.nav3recipes.content.ContentYellow
import kotlinx.serialization.Serializable


/**
 * 示例：根据应用状态（例如用户是否登录）进行条件导航的演示。
 * 本文件包含：
 * - 一个 sealed `NavKey` 层次，描述目的地和访问限制
 * - 一个简单的 Activity，创建并显示 `NavBackStack` 的内容
 * - 一个重载的 `rememberNavBackStack` 帮助函数，使用序列化使回退栈在进程被回收后可恢复
 *
 * 该示例使用 KotlinX Serialization 来持久化导航键和 NavBackStack。
 */

/**
 * 表示应用中导航键的基类。
 *
 * 使用 sealed class 的原因是 KotlinX Serialization 能够自动处理密封类的多态序列化。
 * 每个键可以通过 `requiresLogin` 标志表示是否需要用户登录才能导航到该目的地。
 *
 * @param requiresLogin - 若为 true，则表示该目的地需要用户登录后才能访问。
 */
@Serializable
sealed class ConditionalNavKey(val requiresLogin: Boolean = false) : NavKey

/**
 * 表示主页的键。该键在示例中作为初始启动目的地使用。
 */
@Serializable
private data object Home : ConditionalNavKey()

/**
 * 表示用户资料页的键。该键将 `requiresLogin = true`，表示这是受保护的目的地，
 * 只有在用户已认证时才能访问。在本示例中，当尝试导航到该键而用户未登录时，
 * 示例中的 Navigator 会重定向到 `Login` 键。
 */
@Serializable
private data object Profile : ConditionalNavKey(requiresLogin = true)

/**
 * 表示登录页面的键。
 *
 * @param redirectToKey - 可选的重定向键，若提供则表示登录成功后应该导航到的目标。
 * 这允许在用户在未登录时尝试访问受保护页面时记住原始意图，并在登录后恢复。
 */
@Serializable
private data class Login(
    val redirectToKey: ConditionalNavKey? = null
) : ConditionalNavKey()


class ConditionalActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            // 创建一个存放 ConditionalNavKey 的 NavBackStack。我们使用下面的
            // `rememberNavBackStack` 帮助函数，该函数使用序列化器，使得回退栈
            // 在进程被系统回收后可以恢复。初始元素为 Home。
            val backStack = rememberNavBackStack<ConditionalNavKey>(Home)

            // 一个简单的布尔值，表示当前用户是否已登录。在真实应用中通常来自
            // ViewModel、仓库或鉴权管理器。这里使用 rememberSaveable，以在配置变更
            // 或尽可能在进程被回收后保留该状态。
            var isLoggedIn by rememberSaveable { mutableStateOf(false) }

            // 构建一个简单的 Navigator 实例。该 navigator 是一个用于执行导航操作的
            // 轻量抽象。它需要访问回退栈并包含一些应用特有逻辑：
            // - onNavigateToRestrictedKey：当目标受限时，返回应当展示的键（例如 Login）
            // - isLoggedIn：用于判定用户当前是否已认证的函数。
            //
            // 注意：此处引用的 `Navigator` 在示例中作为一个辅助类存在（本文件未包含其实现），
            // 关键在于示范条件导航：当用户尝试导航到受限目的地时，navigator 可以改为推入
            // 一个包含 `redirectToKey` 的 `Login` 键，以便在登录成功后还原原始意图。
            val navigator = remember {
                Navigator(
                    backStack = backStack,
                    onNavigateToRestrictedKey = { redirectToKey -> Login(redirectToKey) },
                    isLoggedIn = { isLoggedIn }
                )
            }

            // `NavDisplay` 是一个 UI 容器，会读取回退栈的顶层 entry 并显示对应的可组合项。
            // 我们通过 entryProvider 提供从 NavKey 到可组合内容的映射。每个 entry 处理器
            // 会接收具体类型的键并返回该目的地的 UI。
            NavDisplay(
                backStack = backStack,
                onBack = { navigator.goBack() },
                entryProvider = entryProvider {
                    // Home 页面：显示欢迎信息，并提供跳转到 Profile 或 Login 的按钮。
                    // 注意 Profile 按钮演示了条件导航 —— navigator 会在需要时重定向到 Login。
                    entry<Home> {
                        ContentGreen("Welcome to Nav3. Logged in? ${isLoggedIn}") {
                            Column {
                                Button(onClick = { navigator.navigate(Profile) }) {
                                    Text("Profile")
                                }
                                Button(onClick = { navigator.navigate(Login()) }) {
                                    Text("Login")
                                }
                            }
                        }
                    }

                    // Profile 页面：仅在 `isLoggedIn` 为 true 时可访问。
                    // 显示资料内容以及一个注销按钮，注销后更新 `isLoggedIn` 并导航回 Home。
                    entry<Profile> {
                        ContentBlue("Profile screen (only accessible once logged in)") {
                            Button(onClick = {
                                // 模拟注销并导航回 Home。真实应用中应清理鉴权 token
                                // 并通知应用其他部分状态变化。
                                isLoggedIn = false
                                navigator.navigate(Home)
                            }) {
                                Text("Logout")
                            }
                        }
                    }

                    // Login 页面：显示登录界面。用户登录成功后将 isLoggedIn 设为 true。
                    // 如果登录键包含 `redirectToKey`，则从回退栈中移除登录键并导航到目标，
                    // 使用户回到最初想访问的页面。
                    entry<Login> { key ->
                        ContentYellow("Login screen. Logged in? $isLoggedIn") {
                            Button(onClick = {
                                // 模拟登录成功。
                                isLoggedIn = true
                                // 如果存在待重定向目标，则导航到该目标。
                                key.redirectToKey?.let { targetKey ->
                                    // 从回退栈中移除 Login 键，然后导航到原始目标键。
                                    backStack.remove(key)
                                    navigator.navigate(targetKey)
                                }
                            }) {
                                Text("Login")
                            }
                        }
                    }
                }
            )
        }
    }
}


// 对 `rememberNavBackStack` 的一个泛型重载，返回子类型为 `NavKey` 的 NavBackStack。
// 该帮助函数使用 `NavBackStackSerializer` 和提供的 `NavKeySerializer`，使回退栈及其键
// 可以被 KotlinX Serialization 序列化与反序列化。这样在进程被系统回收后，导航状态
// 可以被恢复。参考示例议题以了解更多背景（例如 issue tracker）。
@Composable
fun <T : NavKey> rememberNavBackStack(vararg elements: T): NavBackStack<T> {
    return rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())
    ) {
        NavBackStack(*elements)
    }
}