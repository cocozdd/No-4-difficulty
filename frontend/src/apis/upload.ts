import request from './request';

const createFormData = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return formData;
};

export const uploadGoodsImage = (file: File) =>
  request.post<{ url: string }>('/upload/goods-image', createFormData(file), {
    headers: { 'Content-Type': 'multipart/form-data' }
  });

export const uploadChatImage = (file: File) =>
  request.post<{ url: string }>('/upload/chat-image', createFormData(file), {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
