// Хук для избранного

import { useState, useEffect, useCallback, useRef } from 'react';
import { addFavorite, removeFavoriteApi, getMyFavorites } from '../api/favorites';
import { useToast } from '../components/ui/Toast';

const STORAGE_KEY = 'tramplin_favorites';

export type FavoriteType = 'OPPORTUNITY' | 'COMPANY';

export interface FavoriteItem {
  id: string;
  type: FavoriteType;
  addedAt: string;
}


function loadFromStorage(): FavoriteItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function saveToStorage(items: FavoriteItem[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch (err) {
    console.error('Не удалось сохранить избранное:', err);
  }
}

function isAuthenticated(): boolean {
  return !!localStorage.getItem('accessToken');
}


export function useFavorites() {
  const [favorites, setFavorites] = useState<FavoriteItem[]>(loadFromStorage);
  const { showToast } = useToast();
  const syncingRef = useRef(false);

  useEffect(() => {
    async function syncFromServer() {
      if (!isAuthenticated()) return;

      try {
        const serverFavs = await getMyFavorites();
        const items: FavoriteItem[] = serverFavs.map(f => ({
          id: f.opportunityId,
          type: 'OPPORTUNITY' as FavoriteType,
          addedAt: f.createdAt,
        }));

        const localFavs = loadFromStorage();
        const serverIds = new Set(items.map(i => i.id));
        const toSync = localFavs.filter(lf => !serverIds.has(lf.id));

        for (const item of toSync) {
          try {
            await addFavorite(item.id);
            items.push(item);
          } catch { /* дубликат пропускается*/ }
        }

        setFavorites(items);
        saveToStorage(items);
      } catch (err) {
        console.error('Ошибка загрузки избранного с сервера:', err);
      }
    }
    syncFromServer();
  }, []);

  useEffect(() => {
    function handleStorage(e: StorageEvent) {
      if (e.key === STORAGE_KEY) {
        setFavorites(loadFromStorage());
      }
    }
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  const isFavorite = useCallback(
    (id: string) => favorites.some((f) => f.id === id),
    [favorites]
  );

  const toggleFavorite = useCallback(
    async (id: string, type: FavoriteType = 'OPPORTUNITY') => {
      if (syncingRef.current) return;
      syncingRef.current = true;

      const exists = favorites.some((f) => f.id === id);

      const next = exists
        ? favorites.filter((f) => f.id !== id)
        : [...favorites, { id, type, addedAt: new Date().toISOString() }];
      setFavorites(next);
      saveToStorage(next);

      if (isAuthenticated()) {
        try {
          if (exists) {
            await removeFavoriteApi(id);
          } else {
            await addFavorite(id);
          }
          showToast(
            exists ? 'Удалено из избранного' : 'Добавлено в избранное ✔',
            exists ? 'info' : 'success'
          );
        } catch (err) {
          console.error('Ошибка синхронизации избранного:', err);
          setFavorites(favorites);
          saveToStorage(favorites);
          showToast('Не удалось обновить избранное', 'error');
        }
      } else {
        showToast(
          exists ? 'Удалено из избранного' : 'Добавлено в избранное ✔',
          exists ? 'info' : 'success'
        );
      }

      syncingRef.current = false;
    },
    [favorites, showToast]
  );

  const removeFavorite = useCallback(
    async (id: string) => {
      const next = favorites.filter((f) => f.id !== id);
      setFavorites(next);
      saveToStorage(next);
      showToast('Удалено из избранного', 'info');

      if (isAuthenticated()) {
        try {
          await removeFavoriteApi(id);
        } catch (err) {
          console.error('Ошибка удаления из избранного:', err);
        }
      }
    },
    [favorites, showToast]
  );

  const getFavoriteIds = useCallback(
    (type?: FavoriteType) => {
      const filtered = type ? favorites.filter((f) => f.type === type) : favorites;
      return filtered.map((f) => f.id);
    },
    [favorites]
  );

  return { favorites, isFavorite, toggleFavorite, removeFavorite, getFavoriteIds };
}
