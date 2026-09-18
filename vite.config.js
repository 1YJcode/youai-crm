import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const legacyAssets = [
  ["logo-youai.png", "logo-youai.png"]
];

function emitLegacyAssets() {
  return {
    name: "emit-legacy-assets",
    apply: "build",
    async generateBundle() {
      await Promise.all(legacyAssets.map(async ([source, fileName]) => {
        const sourcePath = resolve(process.cwd(), source);
        const sourceBuffer = await readFile(sourcePath);
        this.emitFile({ type: "asset", fileName, source: sourceBuffer });
      }));
    }
  };
}

export default defineConfig({
  plugins: [vue(), emitLegacyAssets()],
  build: {
    target: "es2020",
    manifest: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ["vue"]
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      "/api": "http://127.0.0.1:8080"
    }
  }
});
