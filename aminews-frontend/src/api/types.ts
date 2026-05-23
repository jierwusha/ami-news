// API响应通用格式
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T | null
}

// 认证相关类型
export interface LoginRequest {
  usernameOrEmail: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  code: string
}

export interface LoginResponse {
  token: string
  user: {
    id: string
    username: string
    email: string
  }
}

// RSS源数据类型
export interface RssSource {
  id: number
  title: string
  link: string
  atomLink: string
  description: string
  icon: string | null
  createTime: string
  updateTime: string
}
// HomeView 新闻相关类型
export interface HomeViewResponse {
  /**
   * 状态码
   */
  code: number
  /**
   * 文件夹数据
   */
  data: FolderData[]
  /**
   * 附加信息
   */
  message: string
}

export interface FolderData {
  /**
   * 频道列表
   */
  channels: ChannelData[]
  /**
   * 文件夹ID
   */
  id: string
  /**
   * 文件夹名称
   */
  name: string
}

export interface ChannelData {
  /**
   * 描述，频道的一句话描述
   */
  description: string
  /**
   * 图标，频道图标
   */
  icon: string | null
  /**
   * 频道ID
   */
  id: number
  /**
   * 链接，频道本身的URL
   */
  link: string
  /**
   * 名称，频道名称
   */
  name: string
  /**
   * 最新5条新闻
   */
  top5news: NewsItemData[]
}

export interface NewsItemData {
  /**
   * 新闻ID
   */
  id: string
  /**
   * 更新时间
   */
  pubDate: string
  /**
   * 标题
   */
  title: string
}

// HomeView 组件使用的转换后类型（与现有模拟数据结构兼容）
export interface HomeViewFolder {
  id: string
  name: string
  icon: string
  color: string
  channels: HomeViewChannel[]
  unreadCount: number
}

export interface HomeViewChannel {
  id: string
  name: string
  icon: string
  description: string
  link: string // 频道本身的URL
  unreadCount: number
  lastUpdate: string
  recentNews: HomeViewNewsItem[]
}

export interface HomeViewNewsItem {
  id: number
  title: string
  source: string
  time: string
  image: string
  sourceIcon: string
  category: string
}

// 新闻详情相关类型
export interface NewsDetailResponse {
  code: number
  message: string
  data: NewsDetailData
}

export interface NewsDetailData {
  id: number
  channelId: number
  title: string
  description: string
  link: string
  aiDescription: string | null
  pubDate: string
  createTime: string
  updateTime: string
}

// 新闻详情页面使用的增强类型（包含频道信息）
export interface NewsDetailWithChannel extends NewsDetailData {
  channelInfo?: {
    id: string
    name: string
    icon: string
    description: string
  }
}

// 频道新闻列表相关类型
export interface ChannelItemsRequest {
  channelId?: string
  pageNum?: number
  pageSize?: number
}

// 文件夹新闻列表相关类型
export interface FolderItemsRequest {
  folderId?: string
  pageNum?: number
  pageSize?: number
}

// 热词相关类型
export interface HotWordItem {
  /**
   * 热词ID
   */
  id: number
  /**
   * 热词内容
   */
  word: string
  /**
   * 热词出现次数
   */
  count: number
}

// 随机频道数据类型（包含订阅状态）
export interface RandomChannelData {
  /**
   * 描述，频道的一句话描述
   */
  description: string
  /**
   * 图标，频道图标
   */
  icon: string | null
  /**
   * 频道ID
   */
  id: number
  /**
   * 链接，频道本身的URL
   */
  link: string
  /**
   * 名称，频道名称
   */
  title: string
  /**
   * RSS源URL
   */
  url: string
  /**
   * 是否已订阅
   */
  subscribed: boolean
  /**
   * 更新时间
   */
  updateTime?: string
}

// 热词新闻相关类型
export interface HotWordItemsRequest {
  hotWordId: number
  pageNum?: number
  pageSize?: number
}

export interface HotWordItemsResponse {
  code: number
  data: HotWordItemDatum[]
  message: string
}

export interface HotWordItemsResponse {
  code: number
  data: HotWordItemDatum[]
  message: string
}

export interface HotWordItemDatum {
  aiDescription: string | null
  channelIcon: string
  channelTitle: string
  description: string
  id: number
  imageUrl: string
  link: string
  pubDate: string
  title: string
}

// 频道新闻数据项类型
export interface ChannelItemDatum {
  id: number
  title: string
  link: string
  pubDate: string
  imageUrl: string | null
  description: string
  aiDescription: string | null
  channelTitle?: string
  channelIcon?: string | null
}

// 文件夹新闻数据项类型
export interface FolderItemDatum {
  id: number
  title: string
  link: string
  pubDate: string
  imageUrl: string | null
  description: string
  aiDescription: string | null
  channelTitle: string
  channelIcon: string | null
}

// 分页新闻数据结构 - 适配新接口
export interface PaginatedNewsResponse<T> {
  items: T[]
  pageNum: number
  pageSize: number
  hasNext: boolean
}

// 频道新闻API响应类型 - 更新为新的数据结构
export interface ChannelItemsResponse {
  code: number
  data: PaginatedNewsResponse<ChannelItemDatum>
  message: string
}

// 文件夹新闻API响应类型 - 更新为新的数据结构
export interface FolderItemsResponse {
  code: number
  data: PaginatedNewsResponse<FolderItemDatum>
  message: string
}
