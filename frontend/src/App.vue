<template>
  <div>
    <NavBar />
    <main style="max-width: 980px; margin: 20px auto; padding: 0 12px;">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import NavBar from './components/NavBar.vue'
</script>

<style>
:root { --brand:#2a7ade; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif; margin:0; }
a { color: var(--brand); text-decoration: none; }
.btn { background: var(--brand); color:#fff; border:none; padding:8px 14px; border-radius:6px; cursor:pointer; }
.btn[disabled] { opacity:.6; cursor:not-allowed; }
.btn.outline { background:#fff; color:var(--brand); border:1px solid var(--brand); }
.input, select, textarea { width:100%; padding:8px 10px; border:1px solid #d0d7de; border-radius:6px; box-sizing: border-box; }
.card { border:1px solid #e5e7eb; border-radius:10px; padding:16px; box-shadow:0 1px 2px rgba(0,0,0,.04); background:#fff; }
.table { width:100%; border-collapse: collapse; }
.table th,.table td{ border-bottom:1px solid #eee; padding:10px; text-align:left; }
.badge { display:inline-block; padding:2px 8px; border-radius:999px; background:#eef2ff; color:#334155; font-size:12px; }
.header { display:flex; justify-content:space-between; align-items:center; margin: 12px 0 18px; }
</style>