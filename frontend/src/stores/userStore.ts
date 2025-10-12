import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { login as loginApi, register as registerApi, fetchProfile } from '../apis/auth';

const TOKEN_KEY = 'campus-market-token';

export const useUserStore = defineStore('userStore', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY));
  const nickname = ref<string>('');
  const role = ref<string>('');
  const userId = ref<number | null>(null);
  const phone = ref<string>('');

  const isAuthenticated = computed(() => Boolean(token.value));

  const setToken = (value: string | null) => {
    token.value = value;
    if (value) {
      localStorage.setItem(TOKEN_KEY, value);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  };

  const login = async (username: string, password: string) => {
    const { data } = await loginApi({ username, password });
    setToken(data.token);
    role.value = data.role;
    nickname.value = data.nickname;
    await loadProfile();
  };

  const register = async (username: string, password: string, nicknameValue: string, phone: string) => {
    const { data } = await registerApi({ username, password, nickname: nicknameValue, phone });
    setToken(data.token);
    role.value = data.role;
    nickname.value = data.nickname;
    await loadProfile();
  };

  const loadProfile = async () => {
    if (!token.value) {
      return;
    }
    const { data } = await fetchProfile();
    userId.value = data.id;
    nickname.value = data.nickname;
    role.value = data.role;
    phone.value = data.phone || '';
  };

  const logout = () => {
    setToken(null);
    role.value = '';
    nickname.value = '';
    userId.value = null;
    phone.value = '';
  };

  if (token.value) {
    loadProfile().catch(() => logout());
  }

  return {
    token,
    nickname,
    role,
    userId,
    phone,
    isAuthenticated,
    login,
    register,
    loadProfile,
    logout
  };
});
