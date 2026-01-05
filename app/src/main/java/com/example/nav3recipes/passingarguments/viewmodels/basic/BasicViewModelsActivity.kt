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
 *
 * ------------------------------------------------------------
 * 中文说明：
 * Apache 2.0 开源协议，允许自由使用、修改和分发，
 * 但需保留版权声明和许可证说明。
 * ------------------------------------------------------------
 */

package com.example.nav3recipes.passingarguments.viewmodels.basic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.nav3recipes.content.ContentBlue
import com.example.nav3recipes.content.ContentGreen
import com.example.nav3recipes.ui.setEdgeToEdgeConfig

/**
 * RouteA：无参数的导航路由
 *
 * 使用 data object 表示一个单例路由
 */
data object RouteA

/**
 * RouteB：带参数的导航路由
 *
 * @param id 用于区分不同 RouteB 实例的参数
 */
data class RouteB(val id: String)

class BasicViewModelsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)
        setContent {

            // 当前导航回退栈
            // 使用 Any 以支持不同类型的路由对象
            val backStack = remember { mutableStateListOf<Any>(RouteA) }

            NavDisplay(
                backStack = backStack,

                // 系统返回键回调
                // 从回退栈中移除最后一个页面
                onBack = { backStack.removeLastOrNull() },

                // In order to add the `ViewModelStoreNavEntryDecorator` (see comment below for why)
                // we also need to add the default `NavEntryDecorator`s as well. These provide
                // extra information to the entry's content to enable it to display correctly
                // and save its state.
                //
                // 中文说明：
                // 为了使用 ViewModelStoreNavEntryDecorator（用于 ViewModel 作用域管理），
                // 必须同时添加默认的 NavEntryDecorator：
                // 1. rememberSaveableStateHolderNavEntryDecorator：保存 Compose UI 状态
                // 2. rememberViewModelStoreNavEntryDecorator：为每个 NavEntry 提供独立的 ViewModelStore
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),

                // 路由与页面内容的映射关系
                entryProvider = entryProvider {

                    // RouteA 页面定义
                    entry<RouteA> {
                        ContentGreen("Welcome to Nav3") {
                            LazyColumn {
                                items(10) { i ->
                                    Button(
                                        onClick = {
                                            // 点击后跳转到 RouteB
                                            // 并传入不同的 id 参数
                                            backStack.add(RouteB("$i"))
                                        }
                                    ) {
                                        Text("$i")
                                    }
                                }
                            }
                        }
                    }

                    // RouteB 页面定义
                    entry<RouteB> { key ->

                        // Note: We need a new ViewModel for every new RouteB instance. Usually
                        // we would need to supply a `key` String that is unique to the
                        // instance, however, the ViewModelStoreNavEntryDecorator (supplied
                        // above) does this for us, using `NavEntry.contentKey` to uniquely
                        // identify the viewModel.
                        //
                        // tl;dr: Make sure you use rememberViewModelStoreNavEntryDecorator()
                        // if you want a new ViewModel for each new navigation key instance.
                        //
                        // 中文说明：
                        // 每一个 RouteB 实例都需要一个全新的 ViewModel。
                        // 在传统 ViewModel 中，通常需要手动传入唯一的 key。
                        // 但在 Nav3 中，ViewModelStoreNavEntryDecorator
                        // 会自动使用 NavEntry.contentKey 来区分 ViewModel。
                        //
                        // 结论：
                        // 👉 如果你希望「每个路由实例都有独立 ViewModel」，
                        // 👉 一定要使用 rememberViewModelStoreNavEntryDecorator()
                        ScreenB(
                            viewModel = viewModel(
                                factory = RouteBViewModel.Factory(key)
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun ScreenB(viewModel: RouteBViewModel = viewModel()) {
    // 显示当前 RouteB 中携带的参数 id
    ContentBlue("Route id: ${viewModel.key.id} ")
}

/**
 * RouteB 对应的 ViewModel
 *
 * @property key 当前 RouteB 路由对象
 */
class RouteBViewModel(
    val key: RouteB
) : ViewModel() {

    /**
     * 自定义 ViewModel Factory
     *
     * 用于将 RouteB 参数传入 ViewModel
     */
    class Factory(
        private val key: RouteB,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            // 创建并返回带参数的 ViewModel 实例
            return RouteBViewModel(key) as T
        }
    }
}
