export type ChatGenMode = 'classic' | 'workflow'

const GLOBAL_CHAT_GEN_MODE_KEY = 'zc_global_chat_gen_mode'
const APP_MODE_LOCK_PREFIX = 'zc_app_chat_mode_lock_'

export const getGlobalChatGenMode = (): ChatGenMode => {
  const saved = localStorage.getItem(GLOBAL_CHAT_GEN_MODE_KEY)
  return saved === 'workflow' ? 'workflow' : 'classic'
}

export const setGlobalChatGenMode = (mode: ChatGenMode) => {
  localStorage.setItem(GLOBAL_CHAT_GEN_MODE_KEY, mode)
}

const getAppModeLockKey = (appId: string | number) => `${APP_MODE_LOCK_PREFIX}${appId}`

export const lockAppWorkflowMode = (appId: string | number) => {
  localStorage.setItem(getAppModeLockKey(appId), 'workflow')
}

export const isAppWorkflowModeLocked = (appId: string | number) => {
  return localStorage.getItem(getAppModeLockKey(appId)) === 'workflow'
}

export const resolveAppChatGenMode = (appId: string | number): ChatGenMode => {
  if (isAppWorkflowModeLocked(appId)) {
    return 'workflow'
  }
  return getGlobalChatGenMode()
}

