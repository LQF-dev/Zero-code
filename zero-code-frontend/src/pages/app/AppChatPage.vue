<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
        <a-tag v-if="appInfo?.codeGenType" color="blue" class="code-gen-type-tag">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
        <a-tag v-if="isWorkflowLocked" color="gold">工作流锁定</a-tag>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button
            type="primary"
            ghost
            @click="downloadCode"
            :loading="downloading"
            :disabled="!isOwner"
        >
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer" @scroll="handleMessagesScroll">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              加载更多历史消息
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" />
              </div>
              <div class="message-content">
                <div v-if="message.processEvents?.length" class="process-timeline">
                  <div
                    v-for="eventItem in message.processEvents"
                    :key="eventItem.id"
                    class="process-event"
                  >
                    <details v-if="eventItem.details" class="event-details">
                      <summary class="event-summary-line">
                        <span class="event-summary-text">{{ eventItem.summary || '过程' }}</span>
                        <span class="event-time">{{ eventItem.time }}</span>
                      </summary>
                      <pre class="event-plain">{{ eventItem.details }}</pre>
                    </details>
                    <div v-else class="event-summary-line event-summary-line-static">
                      <span class="event-summary-text">{{ eventItem.summary || '过程' }}</span>
                      <span class="event-time">{{ eventItem.time }}</span>
                    </div>
                  </div>
                </div>
                <div v-if="message.content" class="final-reply">
                  <div class="final-reply-title">最终回复</div>
                  <MarkdownRenderer :content="message.content" />
                </div>
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
            v-if="selectedElementInfo"
            class="selected-element-alert"
            type="info"
            closable
            @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  页面路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea
                  v-model:value="userInput"
                  :placeholder="getInputPlaceholder()"
                  :rows="4"
                  :maxlength="1000"
                  @keydown.enter.prevent="sendMessage"
                  :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
                v-else
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                  type="primary"
                  @click="sendMessage"
                  :loading="isGenerating"
                  :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>生成后的网页展示</h3>
          <div class="preview-actions">
            <a-button
                v-if="isOwner && previewUrl"
                type="link"
                :danger="isEditMode"
                @click="toggleEditMode"
                :class="{ 'edit-mode-active': isEditMode }"
                style="padding: 0; height: auto; margin-right: 12px"
            >
              <template #icon>
                <EditOutlined />
              </template>
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
            </a-button>
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <iframe
              v-else
              :src="previewUrl"
              class="preview-iframe"
              frameborder="0"
              @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
        v-model:open="appDetailVisible"
        :app="appInfo"
        :show-actions="isOwner || isAdmin"
        @edit="editApp"
        @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
        v-model:open="deployModalVisible"
        :deploy-url="deployUrl"
        @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
} from '@/api/appController'
import { listAppChatHistory, listTurnEvents } from '@/api/chatHistoryController'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import request from '@/request'
import {
  getGlobalChatGenMode,
  isAppWorkflowModeLocked,
  lockAppWorkflowMode,
  resolveAppChatGenMode,
} from '@/utils/chatGenMode'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.svg'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<any>()
const isWorkflowLocked = ref(false)

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
  turnId?: string
  processEvents?: ProcessEventItem[]
  streamBuffer?: string
}

interface ThinkingStreamChunk {
  type: 'thinking'
  data: string
}

interface ToolRequestStreamChunk {
  type: 'tool_request'
  id?: string
  name?: string
  arguments?: string
}

interface ToolExecutedStreamChunk {
  type: 'tool_executed'
  id?: string
  name?: string
  arguments?: string
  result?: string
}

type StructuredStreamChunk = ThinkingStreamChunk | ToolRequestStreamChunk | ToolExecutedStreamChunk

type ProcessEventType =
  | 'thinking'
  | 'tool'
  | 'system'
  | 'error'

interface ProcessEventItem {
  id: string
  type: ProcessEventType
  title: string
  summary: string
  details?: string
  time: string
}

const TOOL_EVENT_TYPES = new Set(['tool_request', 'tool_executed', 'tool_result'])
const TOOL_NAME_LABEL_MAP: Record<string, string> = {
  writeFile: '写入文件',
  readFile: '读取文件',
  readDir: '读取目录',
  modifyFile: '修改文件',
  deleteFile: '删除文件',
  exit: '退出工具调用',
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
const shouldAutoScroll = ref(true)

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
  onError: (tip: string) => {
    message.warning(tip)
  },
})

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

const isVueProjectMode = computed(() => {
  return appInfo.value?.codeGenType === CodeGenTypeEnum.VUE_PROJECT
})

// 应用详情相关
const appDetailVisible = ref(false)

const getCurrentGenerationMode = () => {
  if (!appId.value) {
    return getGlobalChatGenMode()
  }
  return resolveAppChatGenMode(appId.value)
}

const parseStructuredStreamChunk = (chunk: unknown): StructuredStreamChunk | null => {
  if (typeof chunk !== 'string') {
    return null
  }
  const trimmed = chunk.trim()
  if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) {
    return null
  }
  try {
    const parsed = JSON.parse(
      trimmed
        .replace(/[“”]/g, '"')
        .replace(/[‘’]/g, "'")
    )
    if (
      parsed &&
      parsed.type === 'thinking' &&
      typeof parsed.data === 'string'
    ) {
      return parsed as ThinkingStreamChunk
    }
    if (parsed && parsed.type === 'tool_request') {
      return {
        type: 'tool_request',
        id: parsed.id,
        name: parsed.name,
        arguments: parsed.arguments,
      }
    }
    if (parsed && parsed.type === 'tool_executed') {
      return {
        type: 'tool_executed',
        id: parsed.id,
        name: parsed.name,
        arguments: parsed.arguments,
        result: parsed.result,
      }
    }
    return null
  } catch {
    return null
  }
}

const formatEventTime = () => {
  return new Date().toLocaleTimeString('zh-CN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

const sanitizeEventLine = (line: string) => {
  return line
    .replace(/^\[(选择工具|工具调用|工具结果|执行结束)\]\s*/, '')
    .replace(/^(工具:|参数:|结果:)\s*/, '')
    .trim()
}

const buildEventPreview = (details: string, type: ProcessEventType) => {
  if (!details) {
    return type === 'error' ? '执行异常' : '过程'
  }
  const lines = details
    .split('\n')
    .map((line) => sanitizeEventLine(line))
    .filter(Boolean)
  if (!lines.length) {
    return type === 'error' ? '执行异常' : '过程'
  }
  const toolLine = lines.find((line) =>
    /(写入文件|读取文件|修改文件|删除文件|创建目录|执行命令|运行命令|search|read|write)/i.test(line)
  )
  const first = toolLine || lines[0]
  return first.length > 56 ? `${first.slice(0, 56)}...` : first
}

const ensureProcessEvents = (msg: Message) => {
  if (!msg.processEvents) {
    msg.processEvents = []
  }
  return msg.processEvents
}

const appendProcessEvent = (
  msg: Message,
  event: Omit<ProcessEventItem, 'id' | 'time'>,
  mergeIfSameType = false
) => {
  const events = ensureProcessEvents(msg)
  const last = events[events.length - 1]
  if (mergeIfSameType && last && last.type === event.type) {
    last.summary = event.summary || last.summary
    if (event.details) {
      last.details = last.details ? `${last.details}\n${event.details}` : event.details
    }
    return
  }
  events.push({
    id: `${Date.now()}_${events.length}_${Math.random().toString(36).slice(2, 8)}`,
    time: formatEventTime(),
    ...event,
  })
}

const upsertThinkingEvent = (msg: Message, thinking: string) => {
  const events = ensureProcessEvents(msg)
  for (let i = events.length - 1; i >= 0; i -= 1) {
    const item = events[i]
    if (item.type === 'thinking') {
      item.details = thinking
      item.summary = buildEventPreview(thinking, 'thinking')
      return
    }
  }
  events.push({
    id: `${Date.now()}_${events.length}_thinking`,
    type: 'thinking',
    title: 'thinking',
    summary: buildEventPreview(thinking, 'thinking'),
    details: thinking,
    time: formatEventTime(),
  })
}

const mergeStreamSnapshotOrDelta = (previous: string, incoming: string) => {
  if (!incoming) {
    return {
      next: previous,
      delta: '',
    }
  }
  if (!previous) {
    return {
      next: incoming,
      delta: incoming,
    }
  }
  if (incoming.startsWith(previous)) {
    return {
      next: incoming,
      delta: incoming.slice(previous.length),
    }
  }
  // 收到完全重复或更短前缀，视为无增量
  if (previous === incoming || previous.startsWith(incoming)) {
    return {
      next: previous,
      delta: '',
    }
  }
  // 常见流式增量：新分片刚好接在尾部
  if (incoming.startsWith(previous.slice(-Math.min(previous.length, 64)))) {
    return {
      next: previous + incoming,
      delta: incoming,
    }
  }
  // 兜底：当无法可靠判定为“快照”时，按增量追加，优先保证内容不丢失
  return {
    next: previous + incoming,
    delta: incoming,
  }
}

const sanitizeAssistantContent = (raw: string) => {
  if (!raw) return ''
  const normalizedRaw = raw
    // 兼容部分模型返回的中文引号 JSON 片段
    .replace(/[“”]/g, '"')
    .replace(/[‘’]/g, "'")
    // 将拼接在一起的 JSON 对象拆开，便于逐行过滤
    .replace(/}\s*{/g, '}\n{')
  const lines = normalizedRaw.split('\n')
  const filtered: string[] = []
  let suppressToolResultBlock = false
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue
    // 过滤结构化工具事件 JSON，避免被展示在“最终回复”
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      try {
        const obj = JSON.parse(trimmed)
        if (obj && typeof obj.type === 'string' && TOOL_EVENT_TYPES.has(obj.type)) {
          continue
        }
      } catch {
        // ignore JSON parse error
      }
    }
    if (/"type"\s*:\s*"(tool_request|tool_executed|tool_result)"/.test(trimmed)) {
      continue
    }
    if (/^\[(选择工具|工具调用|执行结束)\]/.test(trimmed)) {
      suppressToolResultBlock = false
      continue
    }
    if (/^\[工具结果\]/.test(trimmed) || /^结果:\s*/.test(trimmed)) {
      suppressToolResultBlock = true
      continue
    }
    if (/^(工具:|参数:)\s*/.test(trimmed)) continue
    if (/^(替换前|替换后)[:：]?$/.test(trimmed)) continue
    if (suppressToolResultBlock) continue
    filtered.push(line)
  }
  return filtered.join('\n').trim()
}

const appendToolDetail = (msg: Message, toolEventIndex: number, detail: string) => {
  const events = ensureProcessEvents(msg)
  const event = events[toolEventIndex]
  if (!event) return
  event.details = event.details ? `${event.details}\n${detail}` : detail
  event.summary = buildEventPreview(event.details || '', 'tool')
}

const findLatestToolEventIndexByCallId = (msg: Message, toolCallId?: string) => {
  if (!toolCallId) return null
  const events = ensureProcessEvents(msg)
  for (let i = events.length - 1; i >= 0; i -= 1) {
    const item = events[i]
    if (item.type === 'tool' && item.id.includes(`tool_${toolCallId}`)) {
      return i
    }
  }
  return null
}

const appendStructuredToolEvent = (msg: Message, chunk: ToolRequestStreamChunk | ToolExecutedStreamChunk) => {
  const toolDisplayName = TOOL_NAME_LABEL_MAP[chunk.name || ''] || chunk.name || '工具'
  if (chunk.type === 'tool_request') {
    const events = ensureProcessEvents(msg)
    events.push({
      id: `tool_${chunk.id || `${Date.now()}_${events.length}`}`,
      type: 'tool',
      title: 'tool',
      summary: toolDisplayName,
      details: chunk.arguments ? `参数: ${chunk.arguments}` : toolDisplayName,
      time: formatEventTime(),
    })
    return
  }
  const targetIndex = findLatestToolEventIndexByCallId(msg, chunk.id)
  const detailParts: string[] = []
  detailParts.push(`工具: ${toolDisplayName}`)
  if (chunk.arguments) detailParts.push(`参数: ${chunk.arguments}`)
  if (chunk.result) detailParts.push(`结果: ${chunk.result}`)
  const mergedDetails = detailParts.join('\n')
  if (targetIndex !== null) {
    appendToolDetail(msg, targetIndex, mergedDetails)
    return
  }
  const events = ensureProcessEvents(msg)
  events.push({
    id: `tool_${chunk.id || `${Date.now()}_${events.length}`}`,
    type: 'tool',
    title: 'tool',
    summary: toolDisplayName,
    details: mergedDetails,
    time: formatEventTime(),
  })
}

const startToolEvent = (msg: Message) => {
  const events = ensureProcessEvents(msg)
  events.push({
    id: `${Date.now()}_${events.length}_tool`,
    type: 'tool',
    title: 'tool',
    summary: '工具调用',
    details: '',
    time: formatEventTime(),
  })
  return events.length - 1
}

const appendStreamChunkEvents = (
  msg: Message,
  chunk: string,
  toolState: { activeIndex: number | null; collectingResult: boolean }
) => {
  msg.streamBuffer = (msg.streamBuffer || '') + chunk
  const lines = msg.streamBuffer.split('\n')
  msg.streamBuffer = lines.pop() || ''
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue
    if (trimmed.startsWith('[选择工具]')) {
      // 仅作为中间态提示，不单独展示，避免出现“写入文件”这类无效卡片
      continue
    }
    if (trimmed.startsWith('[工具调用]')) {
      // 每次工具调用都单独生成一条事件，避免多次调用混在同一块中
      toolState.activeIndex = startToolEvent(msg)
      toolState.collectingResult = false
      appendToolDetail(
        msg,
        toolState.activeIndex,
        trimmed.replace(/^\[工具调用\]\s*/, '')
      )
      continue
    }
    if (trimmed.startsWith('[工具结果]')) {
      if (toolState.activeIndex === null) {
        toolState.activeIndex = startToolEvent(msg)
      }
      const events = ensureProcessEvents(msg)
      const current = events[toolState.activeIndex]
      if (current) {
        current.title = 'tool'
      }
      toolState.collectingResult = true
      appendToolDetail(
        msg,
        toolState.activeIndex,
        trimmed.replace(/^\[工具结果\]\s*/, '')
      )
      continue
    }
    if (trimmed.startsWith('[执行结束]')) {
      toolState.activeIndex = null
      toolState.collectingResult = false
      continue
    }
    if (
      toolState.activeIndex !== null &&
      !/^\[(选择工具|工具调用|工具结果|执行结束)\]/.test(trimmed)
    ) {
      appendToolDetail(msg, toolState.activeIndex, line)
    }
  }
}

const flushStreamBufferEvents = (
  msg: Message,
  toolState: { activeIndex: number | null; collectingResult: boolean }
) => {
  const rest = (msg.streamBuffer || '').trim()
  if (rest) {
    appendStreamChunkEvents(msg, `${rest}\n`, toolState)
  }
  msg.streamBuffer = ''
}

const toMultilineEventContent = (eventItem: API.ChatEventLog) => {
  const lines: string[] = []
  if (eventItem.toolName) lines.push(`工具: ${eventItem.toolName}`)
  if (eventItem.toolArguments) lines.push(`参数: ${eventItem.toolArguments}`)
  if (eventItem.toolResult) lines.push(`结果: ${eventItem.toolResult}`)
  if (eventItem.reasoningContent) lines.push(eventItem.reasoningContent)
  if (eventItem.content) lines.push(eventItem.content)
  return lines.join('\n')
}

const mapTurnEventType = (eventType?: string) => {
  const map: Record<string, { type: ProcessEventType; title: string }> = {
    THINKING_PARTIAL: { type: 'thinking', title: '思考中' },
    THINKING_FINAL: { type: 'thinking', title: '思考中' },
    TOOL_REQUEST: { type: 'tool', title: '调用工具中' },
    TOOL_RESULT: { type: 'tool', title: '工具调用完毕' },
    ERROR: { type: 'error', title: '错误' },
  }
  return map[eventType || ''] || null
}

const hydrateTurnEventsForMessage = async (msg: Message) => {
  if (!msg.turnId || !isVueProjectMode.value) return
  try {
    const res = await listTurnEvents({ turnId: msg.turnId })
    if (res.data.code !== 0 || !res.data.data?.length) return
    const sorted = [...res.data.data].sort((a, b) => (a.seq || 0) - (b.seq || 0))
    const events: ProcessEventItem[] = []
    let activeToolIndex: number | null = null

    sorted.forEach((item, idx) => {
      const mapped = mapTurnEventType(item.eventType)
      if (!mapped) return
      const detail = toMultilineEventContent(item)
      const time = item.createTime || formatEventTime()
      if (item.eventType === 'TOOL_REQUEST') {
        events.push({
          id: `${msg.turnId}_${item.id || idx}`,
          type: 'tool',
          title: 'tool',
          summary: buildEventPreview(detail, 'tool'),
          details: detail,
          time,
        })
        activeToolIndex = events.length - 1
        return
      }
      if (item.eventType === 'TOOL_RESULT') {
        if (activeToolIndex !== null && events[activeToolIndex]) {
          events[activeToolIndex].title = 'tool'
          if (detail) {
            events[activeToolIndex].details = events[activeToolIndex].details
              ? `${events[activeToolIndex].details}\n${detail}`
              : detail
            events[activeToolIndex].summary = buildEventPreview(events[activeToolIndex].details || '', 'tool')
          }
          activeToolIndex = null
        } else {
          events.push({
            id: `${msg.turnId}_${item.id || idx}`,
            type: 'tool',
            title: 'tool',
            summary: buildEventPreview(detail, 'tool'),
            details: detail,
            time,
          })
        }
        return
      }
      if (mapped.type === 'thinking') {
        const thinkingDetails = detail || item.reasoningContent || ''
        events.push({
          id: `${msg.turnId}_${item.id || idx}`,
          type: 'thinking',
          title: 'thinking',
          summary: buildEventPreview(thinkingDetails, 'thinking'),
          details: thinkingDetails,
          time,
        })
        return
      }
      if (mapped.type === 'error') {
        events.push({
          id: `${msg.turnId}_${item.id || idx}`,
          type: 'error',
          title: 'error',
          summary: buildEventPreview(detail, 'error'),
          details: detail,
          time,
        })
      }
    })

    msg.processEvents = events
  } catch (e) {
    console.error('加载轮次事件失败:', e)
  }
}

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
            .map((chat) => ({
              type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
              content: chat.messageType === 'assistant' ? sanitizeAssistantContent(chat.message || '') : (chat.message || ''),
              turnId: chat.turnId,
              createTime: chat.createTime,
              processEvents:
                chat.messageType === 'assistant' && chat.reasoningContent
                  ? [
                      {
                        id: `${chat.turnId || chat.createTime || 'history'}_thinking`,
                        type: 'thinking' as const,
                        title: 'thinking',
                        summary: buildEventPreview(chat.reasoningContent, 'thinking'),
                        details: chat.reasoningContent,
                        time: chat.createTime || formatEventTime(),
                      },
                    ]
                  : [],
            }))
            .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
        for (const msg of historyMessages) {
          if (msg.type === 'ai' && msg.turnId) {
            // 优先展示数据库中的完整执行事件
            await hydrateTurnEventsForMessage(msg)
          }
        }
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id
  isWorkflowLocked.value = isAppWorkflowModeLocked(id)

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      // 如果有至少2条对话记录，展示对应的网站
      if (messages.value.length >= 2) {
        updatePreview()
      }
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
          appInfo.value.initPrompt &&
          isOwner.value &&
          messages.value.length === 0 &&
          historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom(true)

  // 开始生成
  isGenerating.value = true
  const clientRequestId = `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
  await generateCode(prompt, aiMessageIndex, clientRequestId)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  let message = userInput.value.trim()
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom(true)

  // 开始生成
  isGenerating.value = true
  const clientRequestId = `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
  await generateCode(message, aiMessageIndex, clientRequestId)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number, clientRequestId: string) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
      mode: getCurrentGenerationMode(),
      clientRequestId,
    })
    if (appId.value && getCurrentGenerationMode() === 'workflow') {
      lockAppWorkflowMode(appId.value)
      isWorkflowLocked.value = true
    }

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 创建 EventSource 连接
    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ''
    let fullThinking = ''
    const toolState: { activeIndex: number | null; collectingResult: boolean } = {
      activeIndex: null,
      collectingResult: false,
    }

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        if (content === undefined || content === null) {
          return
        }
        // 结构化分片（thinking/tool）
        const structuredChunk = parseStructuredStreamChunk(content)
        if (structuredChunk?.type === 'thinking') {
          const thinkingUpdate = mergeStreamSnapshotOrDelta(fullThinking, structuredChunk.data)
          fullThinking = thinkingUpdate.next
          upsertThinkingEvent(messages.value[aiMessageIndex], fullThinking)
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
          return
        }
        if (
          structuredChunk?.type === 'tool_request' ||
          structuredChunk?.type === 'tool_executed'
        ) {
          appendStructuredToolEvent(messages.value[aiMessageIndex], structuredChunk)
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
          return
        }
        // 普通文本分片（AI响应/工具输出）
        const chunkText = typeof content === 'string' ? content : String(content)
        const contentUpdate = mergeStreamSnapshotOrDelta(fullContent, chunkText)
        fullContent = contentUpdate.next
        // 流式阶段保留原始内容，确保工具调用标记可实时展示
        messages.value[aiMessageIndex].content = fullContent
        if (contentUpdate.delta) {
          appendStreamChunkEvents(messages.value[aiMessageIndex], contentUpdate.delta, toolState)
        }
        messages.value[aiMessageIndex].loading = false
        scrollToBottom()
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理done事件
    eventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      flushStreamBufferEvents(messages.value[aiMessageIndex], toolState)
      messages.value[aiMessageIndex].content = sanitizeAssistantContent(fullContent)
      isGenerating.value = false
      eventSource?.close()

      // 延迟更新预览，确保后端已完成处理
      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    // 处理business-error事件（后端限流等错误）
    eventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        console.error('SSE业务错误事件:', errorData)

        // 显示具体的错误信息
        const errorMessage = errorData.message || '生成过程中出现错误'
        messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
        appendProcessEvent(messages.value[aiMessageIndex], {
          type: 'error',
          title: '业务错误',
          summary: '处理过程中出现业务错误',
          details: errorMessage,
        })
        messages.value[aiMessageIndex].loading = false
        message.error(errorMessage)

        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()
      } catch (parseError) {
        console.error('解析错误事件失败:', parseError, '原始数据:', event.data)
        handleError(new Error('服务器返回错误'), aiMessageIndex)
      }
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 避免浏览器 EventSource 自动重连导致同一请求被反复触发
      eventSource?.close()
      handleError(new Error('SSE连接中断'), aiMessageIndex)
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  const errorText = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].content = errorText
  appendProcessEvent(messages.value[aiMessageIndex], {
    type: 'error',
    title: '连接异常',
    summary: '网络连接或流式传输异常',
    details: errorText,
  })
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

const isNearBottom = () => {
  if (!messagesContainer.value) {
    return true
  }
  const container = messagesContainer.value
  const threshold = 80
  return container.scrollHeight - (container.scrollTop + container.clientHeight) <= threshold
}

const handleMessagesScroll = () => {
  shouldAutoScroll.value = isNearBottom()
}

// 滚动到底部
const scrollToBottom = (force = false) => {
  if (!force && !shouldAutoScroll.value) {
    return
  }
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    if (force) {
      shouldAutoScroll.value = true
    }
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  // EventSource 会在组件卸载时自动清理
})
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: #fdfdfd;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-gen-type-tag {
  font-size: 12px;
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 8px;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.messages-container {
  flex: 0.9;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 12px;
  min-width: 0;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.message-content {
  max-width: min(70%, 920px);
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
  white-space: pre-wrap;
}

.user-message .message-content {
  background: #1890ff;
  color: white;
}

.ai-message .message-content {
  background: #f5f5f5;
  color: #1a1a1a;
  padding: 8px 12px;
}

.process-timeline {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.process-event {
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ececec;
  overflow: hidden;
}

.event-time {
  font-size: 11px;
  color: #8c8c8c;
}

.event-summary-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 10px;
  user-select: none;
}

.event-details {
  margin: 0;
}

.event-summary-line::-webkit-details-marker {
  display: none;
}

.event-details > summary {
  list-style: none;
  cursor: pointer;
}

.event-summary-line-static {
  cursor: default;
}

.event-summary-text {
  font-size: 12px;
  color: #1f1f1f;
  font-weight: 500;
  line-height: 1.4;
  text-align: left;
}

.event-plain {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.5;
  font-family: 'JetBrains Mono', 'Fira Code', Menlo, Monaco, Consolas, monospace;
  background: #f7f7f7;
  border-top: 1px solid #ececec;
  padding: 10px;
}

.final-reply {
  margin-top: 8px;
  padding: 10px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
}

.final-reply-title {
  font-size: 12px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 6px;
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}


/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

/* 输入区域 */
.input-container {
  padding: 16px;
  background: white;
}

.input-wrapper {
  position: relative;
}

.input-wrapper .ant-input {
  padding-right: 50px;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

/* 右侧预览区域 */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.preview-loading p {
  margin-top: 16px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.selected-element-alert {
  margin: 0 16px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 12px 16px;
  }

  .app-name {
    font-size: 16px;
  }

  .main-content {
    padding: 8px;
    gap: 8px;
  }

  .message-content {
    max-width: 85%;
  }

  /* 选中元素信息样式 */
  .selected-element-alert {
    margin: 0 16px;
  }

  .selected-element-info {
    line-height: 1.4;
  }

  .element-header {
    margin-bottom: 8px;
  }

  .element-details {
    margin-top: 8px;
  }

  .element-item {
    margin-bottom: 4px;
    font-size: 13px;
  }

  .element-item:last-child {
    margin-bottom: 0;
  }

  .element-tag {
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 14px;
    font-weight: 600;
    color: #007bff;
  }

  .element-id {
    color: #28a745;
    margin-left: 4px;
  }

  .element-class {
    color: #ffc107;
    margin-left: 4px;
  }

  .element-selector-code {
    font-family: 'Monaco', 'Menlo', monospace;
    background: #f6f8fa;
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 12px;
    color: #d73a49;
    border: 1px solid #e1e4e8;
  }

  /* 编辑模式按钮样式 */
  .edit-mode-active {
    background-color: #52c41a !important;
    border-color: #52c41a !important;
    color: white !important;
  }

  .edit-mode-active:hover {
    background-color: #73d13d !important;
    border-color: #73d13d !important;
  }
}
</style>
