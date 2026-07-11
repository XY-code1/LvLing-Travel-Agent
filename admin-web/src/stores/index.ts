import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import { adminLogin, adminLogout, getAdminInfo } from '../api/login';
import type { AdminInfoVO, AdminLoginDTO } from '../types';
import { clearToken, getToken, setToken } from '../utils';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken());
  const user = ref<AdminInfoVO | null>(null);
  const isLoggedIn = computed(() => Boolean(token.value));

  async function login(payload: AdminLoginDTO): Promise<void> {
    const data = await adminLogin(payload);
    token.value = data.token;
    user.value = data.adminInfo;
    setToken(data.token);
  }

  async function loadUser(): Promise<void> {
    if (!token.value) {
      return;
    }
    user.value = await getAdminInfo();
  }

  async function logout(): Promise<void> {
    try {
      if (token.value) {
        await adminLogout();
      }
    } finally {
      clearSession();
    }
  }

  function clearSession(): void {
    token.value = '';
    user.value = null;
    clearToken();
  }

  return {
    token,
    user,
    isLoggedIn,
    login,
    loadUser,
    logout,
    clearSession
  };
});
