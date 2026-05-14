import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import { visualizer } from 'rollup-plugin-visualizer'

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  const isBuild = command === 'build'
  
  return {
    plugins: [
      // uni-app 插件
      uni(),
      
      // Vue 插件（如果需要单独使用）
      vue(),
      
      // SVG 图标插件
      createSvgIconsPlugin({
        iconDirs: [path.resolve(process.cwd(), 'src/assets/icons')],
        symbolId: 'icon-[dir]-[name]',
        inject: 'body-last',
        customDomId: '__svg__icons__dom__'
      }),
      
      // 自动导入插件
      AutoImport({
        imports: ['vue', 'uni-app'],
        dirs: [
          './src/composables',
          './src/utils',
          './src/stores'
        ],
        dts: './src/auto-imports.d.ts',
        eslintrc: {
          enabled: true,
          filepath: './.eslintrc-auto-import.json',
          globalsPropValue: true
        }
      }),
      
      // 组件自动导入
      Components({
        resolvers: [NaiveUiResolver()],
        dirs: ['src/components'],
        extensions: ['vue'],
        deep: true,
        dts: './src/components.d.ts'
      }),
      
      // 打包分析
      mode === 'analyze' ? visualizer({
        open: true,
        gzipSize: true,
        brotliSize: true
      }) : null
    ].filter(Boolean),
    
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
        '@assets': path.resolve(__dirname, 'src/assets'),
        '@components': path.resolve(__dirname, 'src/components'),
        '@pages': path.resolve(__dirname, 'src/pages'),
        '@utils': path.resolve(__dirname, 'src/utils'),
        '@api': path.resolve(__dirname, 'src/api'),
        '@stores': path.resolve(__dirname, 'src/stores'),
        '@styles': path.resolve(__dirname, 'src/styles'),
        '@config': path.resolve(__dirname, 'src/config')
      }
    },
    
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
            @import "@/styles/variables.scss";
            @import "@/styles/mixins.scss";
          `
        }
      }
    },
    
    server: {
      host: '0.0.0.0',
      port: 5173,
      open: true,
      proxy: {
        '/api': {
          target: 'http://localhost:8080', // 后端服务地址
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        }
      }
    },
    
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: false,
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: true, // 移除 console
          drop_debugger: true // 移除 debugger
        }
      },
      rollupOptions: {
        output: {
          chunkFileNames: 'js/[name]-[hash].js',
          entryFileNames: 'js/[name]-[hash].js',
          assetFileNames: (assetInfo) => {
            if (assetInfo.name.endsWith('.css')) {
              return 'css/[name]-[hash].[ext]'
            }
            if (assetInfo.name.endsWith('.png') || 
                assetInfo.name.endsWith('.jpg') || 
                assetInfo.name.endsWith('.jpeg') || 
                assetInfo.name.endsWith('.gif') ||
                assetInfo.name.endsWith('.svg')) {
              return 'images/[name]-[hash].[ext]'
            }
            return 'assets/[name]-[hash].[ext]'
          },
          manualChunks: {
            vendor: ['vue', 'uni-app'],
            utils: ['@/utils/index.js'],
            api: ['@/api/index.js']
          }
        }
      }
    },
    
    define: {
      __UNI_PLATFORM__: JSON.stringify(process.env.UNI_PLATFORM),
      'process.env.NODE_ENV': JSON.stringify(mode)
    }
  }
})