export type ScanStage =
  | 'CLONING'
  | 'SCANNING'
  | 'RECOMMENDING'
  | 'VALIDATING'
  | 'REPORTING'
  | 'COMPLETED'

export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'

export interface RepositoryScan {
  id: string
  gitHubUrl: string
  repositoryName: string
  clonedPath?: string
  createdAt: string
  updatedAt: string
  currentStage: ScanStage
  statusMessage?: string
  retryCount: number
  vulnerabilities: Vulnerability[]
  testResult?: TestResult
  scanReport?: ScanReport
}

export interface Vulnerability {
  id: string
  scanId: string
  filePath: string
  severity: Severity
  type: string
  description: string
  cveId?: string
  source: 'OWASP' | 'SNYK'
  aiRecommendation?: AIRecommendation
}

export interface AIRecommendation {
  id: string
  vulnerabilityId: string
  suggestion: string
  estimatedEffort?: string
  appliedManually: boolean
  createdAt: string
}

export interface TestResult {
  id: string
  scanId: string
  buildSuccess: boolean
  testsPassed: number
  testsFailed: number
  logs?: string
  createdAt: string
}

export interface ScanReport {
  id: string
  scanId: string
  htmlReport: string
  jsonReport: string
  createdAt: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
}
