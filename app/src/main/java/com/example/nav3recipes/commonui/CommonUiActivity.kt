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

package com.example.nav3recipes.commonui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.nav3recipes.content.ContentBlue
import com.example.nav3recipes.content.ContentGreen
import com.example.nav3recipes.content.ContentPurple
import com.example.nav3recipes.content.ContentRed
import com.example.nav3recipes.ui.setEdgeToEdgeConfig

// 定义顶层路由（Top-Level Routes）的接口，用于底部导航栏
// 只有实现此接口的页面才会出现在底部导航中
private sealed interface TopLevelRoute {
    val icon: ImageVector // 每个顶层页面对应的图标
}

// 首页：属于顶层路由
private data object Home : TopLevelRoute {
    override val icon = Icons.Default.Home
}

// 聊天列表：属于顶层路由
private data object ChatList : TopLevelRoute {
    override val icon = Icons.Default.Face
}

// 聊天详情页：**不是**顶层路由（不会出现在底部导航栏）
private data object ChatDetail

// 相机页：属于顶层路由
private data object Camera : TopLevelRoute {
    override val icon = Icons.Default.PlayArrow
}

// 所有需要显示在底部导航栏的顶层路由列表
private val TOP_LEVEL_ROUTES: List<TopLevelRoute> = listOf(Home, ChatList, Camera)

class CommonUiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)
        setContent {
            // 创建一个支持“多栈管理”的顶层导航栈（每个顶层页面有自己的子栈）
            val topLevelBackStack = remember { TopLevelBackStack<Any>(Home) }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        // 为每个顶层路由创建一个底部导航项
                        TOP_LEVEL_ROUTES.forEach { topLevelRoute ->
                            // 判断当前是否选中该路由（通过比较 topLevelKey）
                            val isSelected = topLevelRoute == topLevelBackStack.topLevelKey
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    // 点击时切换到对应的顶层路由（会激活其对应的子栈）
                                    topLevelBackStack.addTopLevel(topLevelRoute)
                                },
                                icon = {
                                    Icon(
                                        imageVector = topLevelRoute.icon,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            ) { _ ->
                // 使用 NavDisplay 渲染当前完整的后退栈（由所有顶层子栈拼接而成）
                NavDisplay(
                    backStack = topLevelBackStack.backStack,
                    onBack = { topLevelBackStack.removeLast() }, // 处理返回按钮
                    entryProvider = entryProvider {
                        // 为每种路由类型注册对应的 Composable 页面
                        entry<Home> {
                            ContentRed("Home screen")
                        }
                        entry<ChatList> {
                            ContentGreen("Chat list screen") {
                                Button(onClick = { topLevelBackStack.add(ChatDetail) }) {
                                    Text("Go to conversation")
                                }
                            }
                        }
                        entry<ChatDetail> {
                            ContentBlue("Chat detail screen")
                        }
                        entry<Camera> {
                            ContentPurple("Camera screen")
                        }
                    },
                )
            }
        }
    }
}

/**
自定义的“多栈导航管理器”：
说白了，TopLevelBackStack 就是一个 Map，每个 key（代表一个底部 Tab）对应一个自己的子栈（List）。
这种模式在带底部导航的 App 中几乎是标配，无论是微信、淘宝、还是 Instagram，背后都是类似的设计。
所以你理解得一点都没错：
一个 Map，一个 key 管一个子栈。 ✅
 */
class TopLevelBackStack<T : Any>(startKey: T) {

    // 为每个顶层路由维护一个独立的子栈（使用 LinkedHashMap 保持插入顺序）
    private var topLevelStacks: LinkedHashMap<T, SnapshotStateList<T>> = linkedMapOf(
        startKey to mutableStateListOf(startKey)
    )

    // 当前激活的顶层路由（用于底部导航高亮）
    var topLevelKey by mutableStateOf(startKey)
        private set

    // 暴露给 NavDisplay 的扁平化后退栈（将所有子栈按顺序拼接）
    val backStack = mutableStateListOf(startKey)

    // 更新 backStack：清空并重新拼接所有子栈的内容
    private fun updateBackStack() =
        backStack.apply {
            clear()
            addAll(topLevelStacks.flatMap { it.value })
        }

    /**
     * 切换到底层导航中的某个顶层页面
     * - 如果该页面首次访问，创建新栈
     * - 如果已存在，则将其移到 LinkedHashMap 末尾（模拟“最近使用”），并激活
     */
    fun addTopLevel(key: T) {
        if (topLevelStacks[key] == null) {
            // 首次进入：创建以 key 为根的新子栈
            topLevelStacks[key] = mutableStateListOf(key)
        } else {
            // 已存在：移除再重新插入，确保它在 LinkedHashMap 末尾（作为当前栈）
            topLevelStacks.apply {
                remove(key)?.let { stack ->
                    put(key, stack)
                }
            }
        }
        topLevelKey = key // 更新当前激活的顶层页面
        updateBackStack() // 重新生成扁平栈
    }

    /**
     * 向当前顶层页面的子栈中添加新页面（例如从 ChatList 进入 ChatDetail）
     */
    fun add(key: T) {
        topLevelStacks[topLevelKey]?.add(key)
        updateBackStack()
    }

    /**
     * 处理返回操作：
     * - 从当前顶层子栈弹出最后一个页面
     * - 如果弹出的是顶层页面本身（即子栈只剩根页面），则移除整个子栈
     * - 然后激活上一个顶层页面（LinkedHashMap 的倒数第二个 key）
     */
    fun removeLast() {
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        // 如果弹出的页面恰好是某个顶层路由的根页面，则清理该子栈
        if (removedKey != null && topLevelStacks[removedKey] != null) {
            topLevelStacks.remove(removedKey)
        }
        // 切换到上一个最近使用的顶层页面（LinkedHashMap 保持顺序）
        if (topLevelStacks.isNotEmpty()) {
            topLevelKey = topLevelStacks.keys.last()
        }
        updateBackStack()
    }
}