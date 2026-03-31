<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NInput,
  NSpace,
  NText,
  type DataTableColumns,
} from 'naive-ui'
import { http } from '../shared/http'
import { useNotify } from '../shared/notify'

type DocumentSummary = {
  id: number
  title: string
  sourceType: string
  ingestStatus: string
  createdAt: string
}

type DocumentDetail = {
  id: number
  title: string
  sourceType: string
  ingestStatus: string
  ingestError: string | null
  content: string
  createdAt: string
}

type RagSource = {
  documentId: number
  documentTitle: string
  chunkId: number
  chunkIndex: number
  content: string
  score: number
}

type RagAskResponse = {
  answer: string
  sources: RagSource[]
}

const notify = useNotify()
const creating = ref(false)
const deletingId = ref<number | null>(null)
const asking = ref(false)
const loadingList = ref(false)
const loadingDetail = ref(false)
const error = ref<string | null>(null)
const docs = ref<DocumentSummary[]>([])
const selected = ref<DocumentDetail | null>(null)
const selectedDocIds = ref<number[]>([])
const question = ref('')
const ragAnswer = ref('')
const ragSources = ref<RagSource[]>([])

const form = reactive({
  title: '',
  content: '',
})

const columns: DataTableColumns<DocumentSummary> = [
  { type: 'selection' },
  { title: 'ID', key: 'id', width: 80 },
  { title: '제목', key: 'title' },
  { title: '타입', key: 'sourceType', width: 120 },
  { title: '인제스트', key: 'ingestStatus', width: 120 },
  { title: '생성일', key: 'createdAt', width: 220 },
  {
    title: '액션',
    key: 'actions',
    width: 120,
    render: (row) =>
      h(
        NButton,
        {
          size: 'small',
          type: 'error',
          tertiary: true,
          style: 'width: 56px; min-width: 56px; height: 28px;',
          disabled: deletingId.value !== null,
          onClick: (e: MouseEvent) => {
            e.stopPropagation()
            void deleteDocument(row.id)
          },
        },
        { default: () => '삭제' },
      ),
  },
]

async function fetchList() {
  loadingList.value = true
  error.value = null
  try {
    const { data } = await http.get<DocumentSummary[]>('/api/documents')
    docs.value = data
    selectedDocIds.value = selectedDocIds.value.filter((id) => data.some((d) => d.id === id))
  } catch (e: any) {
    const message = e?.response?.data?.message || e?.message || '문서 목록 조회 실패'
    error.value = String(message)
  } finally {
    loadingList.value = false
  }
}

async function fetchDetail(id: number) {
  loadingDetail.value = true
  error.value = null
  try {
    const { data } = await http.get<DocumentDetail>(`/api/documents/${id}`)
    selected.value = data
  } catch (e: any) {
    const message = e?.response?.data?.message || e?.message || '문서 상세 조회 실패'
    error.value = String(message)
    selected.value = null
  } finally {
    loadingDetail.value = false
  }
}

async function createDocument() {
  creating.value = true
  error.value = null
  try {
    const payload = {
      title: form.title.trim(),
      content: form.content,
    }
    const { data } = await http.post<DocumentDetail>('/api/documents', payload)
    notify.success('문서 생성 완료')
    form.title = ''
    form.content = ''
    await fetchList()
    selected.value = data
    selectedDocIds.value = Array.from(new Set([...selectedDocIds.value, data.id]))
  } catch (e: any) {
    const message = e?.response?.data?.message || e?.message || '문서 생성 실패'
    error.value = String(message)
    notify.error('문서 생성 실패')
  } finally {
    creating.value = false
  }
}

async function deleteDocument(id: number) {
  if (!window.confirm('이 문서를 삭제하시겠습니까?')) return
  deletingId.value = id
  error.value = null
  try {
    await http.delete(`/api/documents/${id}`)
    notify.success('문서 삭제 완료')
    if (selected.value?.id === id) {
      selected.value = null
    }
    selectedDocIds.value = selectedDocIds.value.filter((docId) => docId !== id)
    await fetchList()
  } catch (e: any) {
    const message = e?.response?.data?.message || e?.message || '문서 삭제 실패'
    error.value = String(message)
    notify.error('문서 삭제 실패')
  } finally {
    deletingId.value = null
  }
}

async function askRag() {
  const q = question.value.trim()
  if (!q) {
    notify.error('질문을 입력해 주세요.')
    return
  }
  if (selectedDocIds.value.length === 0) {
    notify.error('질의 대상 문서를 1개 이상 선택해 주세요.')
    return
  }

  asking.value = true
  error.value = null
  try {
    const payload = {
      question: q,
      documentIds: selectedDocIds.value,
    }
    const { data } = await http.post<RagAskResponse>('/api/rag/ask', payload)
    ragAnswer.value = data.answer
    ragSources.value = data.sources
    notify.success('질의 완료')
  } catch (e: any) {
    const message = e?.response?.data?.message || e?.message || '질의 실패'
    error.value = String(message)
    notify.error('질의 실패')
  } finally {
    asking.value = false
  }
}

onMounted(fetchList)
</script>

<template>
  <NSpace vertical :size="16">
    <NCard title="문서 생성" size="medium">
      <NForm label-placement="top" @submit.prevent="createDocument">
        <NFormItem label="제목">
          <NInput v-model:value="form.title" placeholder="문서 제목" />
        </NFormItem>
        <NFormItem label="내용 (TEXT)">
          <NInput v-model:value="form.content" type="textarea" :rows="6" placeholder="문서 내용을 입력하세요." />
        </NFormItem>
        <NButton type="primary" attr-type="submit" :loading="creating">문서 저장</NButton>
      </NForm>
    </NCard>

    <NCard title="내 문서 목록" size="medium">
      <NDataTable
        :columns="columns"
        :data="docs"
        :row-key="(row) => row.id"
        v-model:checked-row-keys="selectedDocIds"
        :loading="loadingList"
        :single-line="false"
        :row-props="
          (row) => ({
            style: 'cursor: pointer',
            onClick: () => fetchDetail(row.id),
          })
        "
      />
    </NCard>

    <NCard title="질의 (RAG)" size="medium">
      <NSpace vertical :size="12">
        <NText depth="3">선택 문서 수: {{ selectedDocIds.length }}</NText>
        <NInput
          v-model:value="question"
          type="textarea"
          :rows="3"
          placeholder="선택한 문서를 기준으로 질문을 입력하세요."
        />
        <NButton type="primary" :loading="asking" @click="askRag">질문하기</NButton>
        <template v-if="ragAnswer">
          <NText><b>답변</b></NText>
          <NInput :value="ragAnswer" type="textarea" :rows="6" readonly />
          <NText><b>근거(sources)</b></NText>
          <NDataTable
            :columns="[
              { title: '문서', key: 'documentTitle', width: 180 },
              { title: '청크', key: 'chunkIndex', width: 80 },
              { title: '점수', key: 'score', width: 100 },
              { title: '내용', key: 'content' },
            ]"
            :data="ragSources"
            :single-line="false"
          />
        </template>
      </NSpace>
    </NCard>

    <NCard title="문서 상세" size="medium">
      <NText v-if="loadingDetail">불러오는 중...</NText>
      <template v-else-if="selected">
        <NSpace vertical :size="8">
          <NText><b>ID:</b> {{ selected.id }}</NText>
          <NText><b>제목:</b> {{ selected.title }}</NText>
          <NText><b>타입:</b> {{ selected.sourceType }}</NText>
          <NText><b>인제스트:</b> {{ selected.ingestStatus }}</NText>
          <NText v-if="selected.ingestError"><b>인제스트 에러:</b> {{ selected.ingestError }}</NText>
          <NText><b>생성일:</b> {{ selected.createdAt }}</NText>
          <NText depth="3">내용</NText>
          <NInput :value="selected.content" type="textarea" :rows="8" readonly />
        </NSpace>
      </template>
      <NText v-else depth="3">목록에서 문서를 선택하세요.</NText>
    </NCard>

    <NAlert v-if="error" type="error" :show-icon="false">{{ error }}</NAlert>
  </NSpace>
</template>

