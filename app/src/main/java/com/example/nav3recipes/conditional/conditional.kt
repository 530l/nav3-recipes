package com.example.nav3recipes.conditional

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 定义一个可序列化的导航键基类，用于标识每个页面，并标记是否需要登录
@Serializable
sealed class ConditionalNavRouterKey(val requiresLogin: Boolean = false) : NavKey

// 首页：不需要登录
@Serializable
private data object HomeRouterKey : ConditionalNavRouterKey()

// 个人中心：需要登录才能访问
@Serializable
private data object ProfileRouterKey : ConditionalNavRouterKey(requiresLogin = true)

// 登录页面：可携带“登录成功后要跳转的目标页面”信息
@Serializable
private data class LoginRouterKey(
    val redirectToKey: ConditionalNavRouterKey? = null
) : ConditionalNavRouterKey()


class Navigator(
    // 导航回退栈
    private val backStack: NavBackStack<ConditionalNavRouterKey>,
    // 获取重定向到登录页的 Key
    private val onNavigateToRestrictedKey: (targetKey: ConditionalNavRouterKey?) -> ConditionalNavRouterKey,
    // 获取当前登录状态
    private val isLoggedIn: () -> Boolean,
) {
    /**
     *  @param key : 目标导航 Key
     */
    fun navigate(key: ConditionalNavRouterKey) {// key = {ProfileRouterKey@31458} ProfileRouterKey
        //如果当前需要登录，并且 用户未登录，则重定向到登录页
        if (key.requiresLogin && !isLoggedIn()) {
            //onNavigateToRestrictedKey 方法：参数为 用户原本想去的 key，返回值为 登录页对应的 key
            val loginKey = onNavigateToRestrictedKey(key)
            backStack.add(loginKey)
        } else {
            // 否则直接跳转
            backStack.add(key)
        }
    }

    // 返回上一页
    fun goBack() = backStack.removeLastOrNull()
}

// 提供一个可 remember + 可保存状态的 NavBackStack（支持进程杀死后恢复）
//rememberNavBackStack 不仅是“更好”的选择，而是唯一正确的选择。
//它与 NavDisplay、entryProvider、NavKey 构成了完整的导航生态，而 mutableStateListOf 无法融入这个体系。
@Composable
fun <T : NavKey> rememberNavBackStack(vararg elements: T): NavBackStack<T> {
    return rememberSerializable(serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())) {
        NavBackStack(*elements)
    }
}

/**
 * 💡 核心逻辑总结（便于理解）：
 *    条件导航：通过 requiresLogin 标记页面是否需要登录。
 *    拦截跳转：当用户未登录却尝试访问 Profile 时，自动跳转到 Login 页面，并记住原本想去的地方。
 *    登录后重定向：登录成功后，自动跳回最初想访问的页面（如 Profile），并从栈中移除 Login 页面，避免“返回又回到登录页”。
 *    状态持久化：使用 rememberSaveable 保存登录状态，即使 Activity 重建也不会丢失。
 *    这套设计非常适合需要权限控制的 Compose Navigation 场景。
 */
@Composable
fun ConditionalNav3() {

    // 初始化导航栈，默认显示 Home
    val backStack = rememberNavBackStack<ConditionalNavRouterKey>(HomeRouterKey)

    // 用户登录状态（使用 rememberSaveable 保证配置变更或进程重建后仍保留）
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }

    // 创建导航器实例
    val navigator = remember {
        Navigator(
            // 导航回退栈
            backStack = backStack,
            // 提供一个重定向的 Key 到登录页，并携带原始目标 Key
            onNavigateToRestrictedKey = { redirectToKey -> LoginRouterKey(redirectToKey) },
            // 提供当前登录状态
            isLoggedIn = { isLoggedIn }
        )
    }
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.goBack() },
        entryProvider = entryProvider {
            entry<HomeRouterKey> {
                Home(
                    isLoggedIn = isLoggedIn,
                    onNavigateToProfile = {
                        // 点击跳转 Profile（会触发登录检查）
                        navigator.navigate(ProfileRouterKey)
                    },
                    onNavigateToLogin = {
                        // 直接跳转 Login
                        navigator.navigate(LoginRouterKey())
                    }
                )
            }

            entry<ProfileRouterKey> {
                Profile(
                    onLogout = {
                        // 模拟注销 清空登录状态，并跳回首页 ,真实应用中应清理鉴权 token
                        isLoggedIn = false
                        navigator.navigate(HomeRouterKey)
                    })
            }

            entry<LoginRouterKey> { key ->
                Login(
                    redirectToKey = key.redirectToKey,
                    onLoginSuccess = { redirectKey ->

                        isLoggedIn = true

                        // 从栈中移除当前的 Login 页面
                        backStack.remove(key)

                        if (redirectKey != null) {
                            // 有原始目标：跳回去
                            navigator.navigate(redirectKey)
                        } else {
                            // 没有原始目标（用户主动进登录页）：跳回首页
                            navigator.navigate(HomeRouterKey)
                        }
                    }
                )
            }
        }
    )
}

@Composable
fun Home(
    isLoggedIn: Boolean,
    onNavigateToProfile: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Home", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Logged in: $isLoggedIn",
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "Home。使用示例导航器前往个人资料或登录页面。",
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(onClick = onNavigateToProfile) {
            Text("前往个人资料")
        }
        Button(onClick = onNavigateToLogin) {
            Text("前往登录")
        }
    }
}

@Composable
fun Profile(onLogout: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall)
        Text(
            "登录用户的个人资料详情将显示在此处。",
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(onClick = onLogout) {
            Text("退出登录")
        }
    }
}

// 登录页面 UI 组件
// 接收两个参数：
// - redirectToKey：登录成功后应跳转回的目标页面（例如用户原本想访问 Profile，但被拦截到登录页）
// - onLoginSuccess：登录成功时的回调，通知上层导航逻辑进行后续跳转
@Composable
fun Login(
    redirectToKey: ConditionalNavRouterKey?,      // 可能为 null，表示没有特定重定向目标（比如用户主动点“去登录”）
    onLoginSuccess: (ConditionalNavRouterKey?) -> Unit  // 调用此回调来触发登录成功后的导航
) {
    // 本地状态：模拟是否已点击登录（仅用于 UI 反馈，非真实认证）
    var accepted by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Login", style = MaterialTheme.typography.headlineSmall)
        Text(
            if (accepted) "已登录成功" else "当前跳转${redirectToKey ?: run { "登录页面" }} 需要登录",
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = {
                accepted = true  // 更新 UI 显示“已登录”
                // 触发登录成功回调，并传入原始目标页面（可能为 null）
                onLoginSuccess(redirectToKey)
            },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Login")
        }
    }
}