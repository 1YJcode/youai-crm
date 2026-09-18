<script setup>
import { onMounted, onUnmounted, ref } from "vue";

const status = ref("初始化 Vue 3 运行时");
const error = ref("");
const ready = ref(false);
let observer;

function reloadPage() {
  window.location.reload();
}

function loadLegacyApp() {
  return import("../app.js");
}

function hideWhenLegacyIsReady() {
  const sync = () => {
    if (!document.body.classList.contains("app-booting")) {
      ready.value = true;
      observer?.disconnect();
    }
  };
  observer = new MutationObserver(sync);
  observer.observe(document.body, { attributes: true, attributeFilter: ["class"] });
  sync();
}

onMounted(async () => {
  try {
    status.value = "加载业务模块";
    await loadLegacyApp();
    hideWhenLegacyIsReady();
  } catch (loadError) {
    error.value = loadError.message || "前端启动失败";
  }
});

onUnmounted(() => observer?.disconnect());
</script>

<template>
  <main v-if="!ready" class="vue-boot-shell">
    <section class="vue-boot-card" role="status">
      <img src="/logo-youai.png" alt="优爱 YOUAI" class="vue-boot-logo">
      <div class="vue-boot-copy">
        <p class="vue-boot-eyebrow">YOUAI CRM</p>
        <h1>{{ error ? "工作台启动失败" : "正在加载工作台" }}</h1>
        <p>{{ error || status }}</p>
        <button v-if="error" type="button" class="vue-boot-retry" @click="reloadPage">重新加载</button>
      </div>
      <span v-if="!error" class="vue-boot-spinner" aria-hidden="true"></span>
    </section>
  </main>
</template>
