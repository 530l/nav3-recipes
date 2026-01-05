# Returning a Result (State-Based)

This recipe demonstrates how to return a result from one screen to a previous screen using a state-based approach.

## How it works

This example uses a `ResultStore` to manage the result as state.

1.  **`ResultStore`**: A `ResultStore` is created and made available to the composables. This store holds the results.
2.  **Setting the result**: The screen that produces the result calls `resultStore.setResult(person)` to save the data in the store.
3.  **Observing the result**: The screen that needs the result calls `resultStore.getResultState<Person?>()` to get a `State` object representing the result. The UI then observes this state and recomposes whenever the result changes.

This approach is suitable when the result should be treated as persistent state that survives recomposition and configuration changes.


返回结果（基于状态）
本示例演示如何使用基于状态的方法，将结果从一个屏幕返回到前一个屏幕。
工作原理
此示例使用 ResultStore 将结果作为状态进行管理。
ResultStore：创建一个 ResultStore 并将其提供给可组合函数（composables）。该存储用于保存结果。
设置结果：产生结果的屏幕调用 resultStore.setResult(person)，将数据保存在存储中。
观察结果：需要该结果的屏幕调用 resultStore.getResultState<Person?>() 来获取表示结果的 State 对象。
UI 会监听该状态并在结果变化时重新组合（recompose）。

当结果需要被视为持久状态并在重组或配置更改后仍然保留时，适合使用这种方法。