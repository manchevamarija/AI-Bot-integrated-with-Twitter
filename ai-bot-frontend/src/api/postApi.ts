import axiosInstance from '../axios/axios.ts';
import type { PageResponse, PostFilter, PostResponse, SessionStatistics } from './types/post.ts';

const postApi = {
  findAll: async (filter: PostFilter, page: number, size: number) => {
    return await axiosInstance.get<PageResponse<PostResponse>>('/posts', {
      params: { ...filter, page, size }
    });
  },
  findById: async (id: string) => {
    return await axiosInstance.get<PostResponse>(`/posts/${id}`);
  },
  loadVideo: async (postId: number, mediaId: number) => {
    return await axiosInstance.get<Blob>(`/posts/${postId}/media/${mediaId}`, {
      responseType: 'blob',
    });
  },
  delete: async (id: string) => {
    return await axiosInstance.delete<PostResponse>(`/posts/${id}/delete`);
  },
  statistics: async (sessionId: string) => {
    return await axiosInstance.get<SessionStatistics>(`/posts/session/${sessionId}/statistics`);
  },
  export: async (sessionId: string, format: 'json' | 'csv') => {
    return await axiosInstance.get<Blob>(`/posts/export/${format}`, {
      params: { sessionId },
      responseType: 'blob',
    });
  },
};

export default postApi;
