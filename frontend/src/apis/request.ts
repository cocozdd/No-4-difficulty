import axios from 'axios';
import { useUserStore } from '../stores/userStore';
import { ElMessage } from 'element-plus';

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
});

request.interceptors.request.use((config) => {
  const userStore = useUserStore();
  if (userStore.token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${userStore.token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || '网络异常，请稍后重试';
    if (error.response?.status === 401) {
      const userStore = useUserStore();
      userStore.logout();
    }
    ElMessage.error(message);
    return Promise.reject(error);
  }
);

export default request;
