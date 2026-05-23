import { defineComponent } from 'vue'

export default defineComponent({
  name: 'App',
  setup() {
    return () => (
      <div>
        <div>
          <h1>欢迎来到 Vue 3 + TSX 页面</h1>
        </div>
        <div>
          <p>这是一个使用 TSX 语法的 Vue 组件示例。</p>
        </div>
      </div>
    )
  },
})
