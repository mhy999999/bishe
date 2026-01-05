<template>
  <div class="app-container">
    <el-card shadow="never">
      <div style="display:flex; align-items:center; justify-content:space-between; gap:12px;">
        <div style="min-width:0;">
          <div style="font-size:16px; font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
            {{ title }}
          </div>
          <div style="color:#909399; font-size:12px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
            {{ resolvedUrl }}
          </div>
        </div>
        <div style="display:flex; gap:10px; flex-shrink:0;">
          <el-button @click="goBack">返回</el-button>
          <el-button type="primary" @click="openInNewTab">新窗口打开</el-button>
          <el-button @click="downloadFile">下载</el-button>
        </div>
      </div>
    </el-card>

    <div style="height: calc(100vh - 170px); margin-top: 12px;">
      <el-card v-if="loading" shadow="never" style="height: 100%;">
        <el-skeleton :rows="12" animated />
      </el-card>

      <el-card v-else-if="error" shadow="never" style="height: 100%;">
        <el-result icon="error" title="预览失败" :sub-title="error" />
      </el-card>

      <el-card v-else shadow="never" style="height: 100%;">
        <template v-if="viewerType === 'text' || viewerType === 'doc'">
          <pre style="white-space: pre-wrap; word-break: break-word; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace; font-size: 13px; line-height: 1.6; margin: 0;">{{ textContent }}</pre>
        </template>
        <template v-else-if="viewerType === 'markdown'">
          <div style="height:100%; overflow:auto;">
            <div v-html="markdownHtml" style="padding: 2px 2px 8px 2px;" />
          </div>
        </template>
        <template v-else-if="viewerType === 'image'">
          <div style="width:100%; height:100%; overflow:auto; text-align:center;">
            <img :src="resolvedUrl" style="max-width:100%; height:auto;" />
          </div>
        </template>
        <template v-else-if="viewerType === 'pdf'">
          <iframe :src="resolvedUrl" style="width:100%; height:100%; border:0;" />
        </template>
        <template v-else-if="viewerType === 'docx'">
          <div style="height:100%; overflow:auto;">
            <div v-html="docxHtml" style="padding: 2px 2px 8px 2px;" />
          </div>
        </template>
        <template v-else-if="viewerType === 'video'">
          <div style="width:100%; height:100%; display:flex; align-items:center; justify-content:center;">
            <video :src="resolvedUrl" controls style="max-width:100%; max-height:100%;" />
          </div>
        </template>
        <template v-else-if="viewerType === 'audio'">
          <div style="width:100%; height:100%; display:flex; align-items:center; justify-content:center;">
            <audio :src="resolvedUrl" controls style="width: 100%; max-width: 720px;" />
          </div>
        </template>
        <template v-else>
          <el-result icon="info" title="该文件类型暂不支持在线预览">
            <template #extra>
              <el-button type="primary" @click="downloadFile">下载文件</el-button>
              <el-button @click="openInNewTab">尝试直接打开</el-button>
            </template>
          </el-result>
        </template>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const contentCache = new Map()
let activeController = null

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const error = ref('')
const textContent = ref('')
const markdownHtml = ref('')
const docxHtml = ref('')

const rawUrl = computed(() => {
  const q = route.query?.url
  return typeof q === 'string' ? q : ''
})

const rawFrom = computed(() => {
  const q = route.query?.from
  return typeof q === 'string' ? q : ''
})

const resolvedUrl = computed(() => {
  const url = (rawUrl.value || '').trim()
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url

  const apiBase = (import.meta.env.VITE_APP_BASE_API || '/api').replace(/\/$/, '')
  if (url.startsWith('/api/')) return url
  if (url.startsWith('/files/')) return `${apiBase}${url}`
  if (url.startsWith('files/')) return `${apiBase}/${url}`
  if (url.startsWith('/')) return url
  return `${apiBase}/${url}`
})

const filename = computed(() => {
  const u = resolvedUrl.value || ''
  if (!u) return ''
  const pathPart = u.split('?')[0]
  const lastSlash = pathPart.lastIndexOf('/')
  return lastSlash >= 0 ? pathPart.slice(lastSlash + 1) : pathPart
})

const ext = computed(() => {
  const name = filename.value
  const dot = name.lastIndexOf('.')
  return dot >= 0 ? name.slice(dot + 1).toLowerCase() : ''
})

const viewerType = computed(() => {
  const e = ext.value
  if (['md', 'markdown'].includes(e)) return 'markdown'
  if (['txt', 'json', 'log', 'csv', 'xml', 'yml', 'yaml', 'ini', 'conf'].includes(e)) return 'text'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(e)) return 'image'
  if (e === 'pdf') return 'pdf'
  if (e === 'docx') return 'docx'
  if (e === 'doc') return 'doc'
  if (['mp4', 'webm', 'ogg', 'mov', 'm4v'].includes(e)) return 'video'
  if (['mp3', 'wav', 'aac', 'flac', 'm4a', 'oga', 'opus'].includes(e)) return 'audio'
  return 'other'
})

const title = computed(() => filename.value || '材料文件预览')

const apiBase = computed(() => (import.meta.env.VITE_APP_BASE_API || '/api').replace(/\/$/, ''))

const docPreviewUrl = computed(() => {
  const url = resolvedUrl.value
  if (!url) return ''
  const lower = url.toLowerCase()
  const endpoint = lower.includes('/files/maintenance/')
    ? '/battery/maintenance/material/preview/doc'
    : '/battery/sales/material/preview/doc'
  return `${apiBase.value}${endpoint}?url=${encodeURIComponent(url)}`
})

const goBack = () => {
  const from = (rawFrom.value || '').trim()
  if (from) {
    router.push(from)
    return
  }
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push({ path: '/battery/sales/list' })
}

const clearViewerState = () => {
  error.value = ''
  textContent.value = ''
  markdownHtml.value = ''
  docxHtml.value = ''
}

const openInNewTab = () => {
  if (!resolvedUrl.value) return
  window.open(resolvedUrl.value, '_blank')
}

const downloadFile = () => {
  if (!resolvedUrl.value) return
  const a = document.createElement('a')
  a.href = resolvedUrl.value
  a.download = filename.value || 'download'
  a.target = '_blank'
  document.body.appendChild(a)
  a.click()
  a.remove()
}

const loadContent = async () => {
  clearViewerState()
  const url = resolvedUrl.value
  const type = viewerType.value
  if (!url) {
    error.value = '缺少文件地址参数'
    return
  }
  if (!['text', 'markdown', 'docx', 'doc'].includes(type)) return

  const fetchUrl = type === 'doc' ? docPreviewUrl.value : url
  if (type === 'doc' && !fetchUrl) {
    error.value = '缺少文件地址参数'
    return
  }

  const cached = contentCache.get(`${type}:${fetchUrl}`)
  if (cached) {
    if (type === 'markdown') {
      markdownHtml.value = cached
    } else if (type === 'docx') {
      docxHtml.value = cached
    } else {
      textContent.value = cached
    }
    return
  }

  if (activeController) {
    activeController.abort()
  }
  activeController = new AbortController()

  loading.value = true
  try {
    const headers = {}
    if (userStore?.token) {
      headers.Authorization = 'Bearer ' + userStore.token
    }
    const res = await fetch(fetchUrl, {
      headers,
      credentials: 'include',
      cache: 'force-cache',
      signal: activeController.signal
    })
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`)
    }
    if (type === 'markdown') {
      const body = await res.text()
      const [{ marked }, domPurifyModule] = await Promise.all([
        import('marked'),
        import('dompurify')
      ])
      const DOMPurify = domPurifyModule?.default || domPurifyModule
      const html = marked.parse(body, { gfm: true, breaks: true })
      const safe = DOMPurify.sanitize(String(html || ''))
      markdownHtml.value = safe
      contentCache.set(`${type}:${fetchUrl}`, safe)
    } else if (type === 'docx') {
      const buffer = await res.arrayBuffer()
      const [mammothModule, domPurifyModule] = await Promise.all([
        import('mammoth'),
        import('dompurify')
      ])
      const mammoth = mammothModule?.default || mammothModule
      const DOMPurify = domPurifyModule?.default || domPurifyModule
      const result = await mammoth.convertToHtml({ arrayBuffer: buffer })
      const safe = DOMPurify.sanitize(String(result?.value || ''))
      docxHtml.value = safe
      contentCache.set(`${type}:${fetchUrl}`, safe)
    } else if (type === 'doc') {
      const json = await res.json()
      if (json?.code !== 200) {
        throw new Error(json?.message || '预览解析失败')
      }
      const body = String(json?.data ?? '')
      textContent.value = body
      contentCache.set(`${type}:${fetchUrl}`, body)
    } else {
      const body = await res.text()
      textContent.value = body
      contentCache.set(`${type}:${fetchUrl}`, body)
    }
  } catch (e) {
    if (e?.name === 'AbortError') {
      return
    }
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

watch([resolvedUrl, viewerType], () => {
  loadContent()
}, { immediate: true })
</script>
