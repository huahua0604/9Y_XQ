import { createApp } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import router from './router'
import App from './App.vue'
import { useAuthStore } from './store/auth'

import 'bootstrap/dist/css/bootstrap.min.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
setActivePinia(pinia)

const auth = useAuthStore()
auth.hydrate()

app.use(router)
app.mount('#app')
