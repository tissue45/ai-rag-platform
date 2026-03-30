import type { MessageApiInjection } from 'naive-ui/es/message/src/MessageProvider'
import { useMessage } from 'naive-ui'

export function useNotify(): MessageApiInjection {
  return useMessage()
}

