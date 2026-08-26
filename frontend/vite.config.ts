import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "");
  const backendTarget =
    env.BOARDHUB_VITE_PROXY_TARGET ?? "http://localhost:8082";

  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
      proxy: {
        "/stats-api": {
          target:
            env.BOARDHUB_VITE_STATS_PROXY_TARGET ?? "http://localhost:8083",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/stats-api/, ""),
        },
        "/api": backendTarget,
        "/actuator": backendTarget,
      },
    },
  };
});
