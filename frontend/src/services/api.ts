import axios, { AxiosInstance } from 'axios'
import { RepositoryScan, PaginatedResponse } from '../types'

const apiClient: AxiosInstance = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

export const scanAPI = {
  createScan: async (gitHubUrl: string): Promise<RepositoryScan> => {
    const response = await apiClient.post<RepositoryScan>('/scans', { gitHubUrl })
    return response.data
  },

  getScan: async (scanId: string): Promise<RepositoryScan> => {
    const response = await apiClient.get<RepositoryScan>(`/scans/${scanId}`)
    return response.data
  },

  listScans: async (page: number = 0, size: number = 20): Promise<PaginatedResponse<RepositoryScan>> => {
    const response = await apiClient.get<PaginatedResponse<RepositoryScan>>('/scans', {
      params: { page, size },
    })
    return response.data
  },

  deleteScan: async (scanId: string): Promise<void> => {
    await apiClient.delete(`/scans/${scanId}`)
  },

  retryScan: async (scanId: string): Promise<RepositoryScan> => {
    const response = await apiClient.post<RepositoryScan>(`/scans/${scanId}/retry`)
    return response.data
  },

  getHtmlReport: async (scanId: string): Promise<string> => {
    const response = await apiClient.get<string>(`/scans/${scanId}/report`, {
      headers: { Accept: 'text/html' },
    })
    return response.data
  },

  getJsonReport: async (scanId: string) => {
    const response = await apiClient.get(`/scans/${scanId}/report`, {
      headers: { Accept: 'application/json' },
    })
    return response.data
  },

  downloadReport: async (scanId: string, format: 'html' | 'json' = 'html'): Promise<void> => {
    const response = await apiClient.get(`/scans/${scanId}/report/download`, {
      params: { format },
      responseType: 'blob',
    })

    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `remedify-report-${scanId}.${format}`)
    document.body.appendChild(link)
    link.click()
    link.parentNode?.removeChild(link)
  },

  subscribeToProgress: (scanId: string, onUpdate: (data: any) => void): (() => void) => {
    const eventSource = new EventSource(`/api/scans/${scanId}/sse`)

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        onUpdate(data)
      } catch (e) {
        console.error('Failed to parse SSE message', e)
      }
    }

    eventSource.onerror = () => {
      eventSource.close()
    }

    return () => eventSource.close()
  },
}

export default apiClient
