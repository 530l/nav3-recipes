# Returning a Result (Event-Based)

This recipe demonstrates how to return a result from one screen to a previous screen using an event-based approach.

## How it works

This example uses a `ResultEventBus` to facilitate communication between the screens.

1.  **`ResultEventBus`**: A simple event bus is created and made available to the composables.
2.  **Sending the result**: The screen that produces the result calls `resultBus.sendResult(person)` to send the data back as a one-time event.
3.  **Receiving the result**: The screen that needs the result uses a `ResultEffect` composable to listen for results of a specific type. When a result is received, the effect's lambda is triggered.

This approach is useful for results that are transient and should be handled as one-time events.

返回结果（基于事件）
本示例演示如何使用基于事件的方法，将结果从一个屏幕返回到前一个屏幕。
工作原理
本示例使用 ResultEventBus 来促进屏幕之间的通信。
ResultEventBus：创建一个简单的事件总线，并将其提供给可组合函数（composables）。
发送结果：产生结果的屏幕调用 resultBus.sendResult(person)，将数据作为一次性事件发送回去。
接收结果：需要该结果的屏幕使用 ResultEffect 可组合项来监听特定类型的结果。当收到结果时，Effect 的 lambda 会被触发。
这种方法适用于那些瞬态的、应当作为一次性事件处理的结果。