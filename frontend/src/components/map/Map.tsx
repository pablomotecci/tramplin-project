import { useState, useCallback, useRef, useEffect, useMemo } from 'react';
import { YMaps, Map as YMap, Clusterer, Placemark } from '@pbe/react-yandex-maps';
import { getOpportunitiesForMap } from '../../api/opportunities';
import type { OpportunityMapCard } from '../../types';
import styles from './Map.module.css';
import { useFavorites } from '../../hooks/useFavorites';
import { escapeHtml, safeUrl } from '../../utils/html';


const MARKER_PRESETS: Record<string, string> = {
  VACANCY: 'islands#blueCircleDotIcon',
  INTERNSHIP: 'islands#blueCircleDotIcon',
  EVENT: 'islands#greenCircleDotIcon',
  MENTORSHIP: 'islands#violetCircleDotIcon',
};

const TYPE_LABELS: Record<string, string> = {
  VACANCY: 'Вакансия',
  INTERNSHIP: 'Стажировка',
  MENTORSHIP: 'Менторство',
  EVENT: 'Мероприятие',
};

const FORMAT_ICONS: Record<string, string> = {
  OFFICE: 'apartment',
  HYBRID: 'sync_alt',
  REMOTE: 'home',
};

const FORMAT_LABELS: Record<string, string> = {
  OFFICE: 'Офис',
  HYBRID: 'Гибрид',
  REMOTE: 'Удалённо',
};

function formatSalary(min: number | null, max: number | null): string {
  if (min && max) return `${min.toLocaleString('ru-RU')} – ${max.toLocaleString('ru-RU')} ₽`;
  if (min) return `от ${min.toLocaleString('ru-RU')} ₽`;
  if (max) return `до ${max.toLocaleString('ru-RU')} ₽`;
  return 'По договорённости';
}

function typeColors(type: string): { bg: string; color: string } {
  if (type === 'VACANCY') return { bg: '#dbeafe', color: '#1d4ed8' };
  if (type === 'INTERNSHIP') return { bg: '#fff3ed', color: '#E8622C' };
  if (type === 'EVENT') return { bg: '#d1fae5', color: '#059669' };
  return { bg: '#ede9fe', color: '#7c3aed' };
}

// Русская плюрализация: 1 вакансия, 2 вакансии, 5 вакансий
function pluralizeVacancies(n: number): string {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return 'вакансия';
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) return 'вакансии';
  return 'вакансий';
}

function favoriteButton(oppId: string, isFav: boolean): string {
  return `
    <button
      type="button"
      class="tramplin-balloon__fav-btn${isFav ? ' tramplin-balloon__fav-btn--active' : ''}"
      data-tramplin-action="toggle-fav"
      data-tramplin-opp-id="${escapeHtml(oppId)}"
      title="${isFav ? 'Убрать из избранного' : 'Добавить в избранное'}"
    >
      <span class="material-symbols-rounded" style="font-size:15px;">favorite</span>
      ${isFav ? 'В избранном' : 'В избранное'}
    </button>
  `;
}

// Балун для одиночной вакансии (старый формат + индикатор избранного)
function buildBalloonContent(opp: OpportunityMapCard, isFav: boolean): string {
  const { bg: typeBg, color: typeColor } = typeColors(opp.type);
  const logoUrl = safeUrl(opp.logoUrl);
  const favBtn = favoriteButton(opp.id, isFav);

  return `
    <div class="tramplin-balloon">
      <div class="tramplin-balloon__header">
        <span class="tramplin-balloon__type" style="background:${typeBg};color:${typeColor};">
          ${escapeHtml(TYPE_LABELS[opp.type] || opp.type)}
        </span>
        <span class="tramplin-balloon__format">
          <span class="material-symbols-rounded" style="font-size:14px;">${escapeHtml(FORMAT_ICONS[opp.workFormat] || 'work')}</span>
          ${escapeHtml(FORMAT_LABELS[opp.workFormat] || opp.workFormat)}
        </span>
        ${favBtn}
      </div>

      <div class="tramplin-balloon__title">${escapeHtml(opp.title)}</div>

      <div class="tramplin-balloon__company">
        ${logoUrl
          ? `<img src="${logoUrl}" alt="" class="tramplin-balloon__logo" />`
          : `<div class="tramplin-balloon__logo-placeholder">${escapeHtml(opp.companyName.charAt(0))}</div>`
        }
        <span>${escapeHtml(opp.companyName)}</span>
      </div>

      ${opp.tags && opp.tags.length > 0 ? `
        <div class="tramplin-balloon__tags">
          ${opp.tags.slice(0, 4).map(tag => `<span class="tramplin-balloon__tag">${escapeHtml(tag)}</span>`).join('')}
        </div>
      ` : ''}

      <div class="tramplin-balloon__footer">
        <span class="tramplin-balloon__city">
          <span class="material-symbols-rounded" style="font-size:14px;">location_on</span>
          ${escapeHtml(opp.city)}
        </span>
        <span class="tramplin-balloon__salary">${formatSalary(opp.salaryMin, opp.salaryMax)}</span>
      </div>

      <a href="/opportunities/${encodeURIComponent(opp.id)}" class="tramplin-balloon__link">
        Подробнее
        <span class="material-symbols-rounded" style="font-size:16px;">arrow_forward</span>
      </a>
    </div>
  `;
}


function buildMultiBalloonContent(group: OpportunityMapCard[], favIds: Set<string>): string {
  const companies = new Set(group.map(o => o.companyName));
  const header = companies.size === 1
    ? `${group.length} ${pluralizeVacancies(group.length)} · ${escapeHtml(group[0].companyName)}`
    : `${group.length} ${pluralizeVacancies(group.length)} по этому адресу`;

  const sorted = [...group].sort((a, b) => {
    const aFav = favIds.has(a.id) ? 1 : 0;
    const bFav = favIds.has(b.id) ? 1 : 0;
    return bFav - aFav;
  });

  const cards = sorted.map(opp => {
    const isFav = favIds.has(opp.id);
    const { bg: typeBg, color: typeColor } = typeColors(opp.type);

    return `
      <div class="tramplin-balloon-multi__card${isFav ? ' tramplin-balloon-multi__card--fav' : ''}">
        <div class="tramplin-balloon-multi__card-row">
          <span class="tramplin-balloon__type" style="background:${typeBg};color:${typeColor};">
            ${escapeHtml(TYPE_LABELS[opp.type] || opp.type)}
          </span>
          ${favoriteButton(opp.id, isFav)}
        </div>
        <div class="tramplin-balloon-multi__card-title">${escapeHtml(opp.title)}</div>
        <div class="tramplin-balloon-multi__card-meta">
          <span>${formatSalary(opp.salaryMin, opp.salaryMax)}</span>
          <a href="/opportunities/${encodeURIComponent(opp.id)}" class="tramplin-balloon-multi__card-link">
            Подробнее
            <span class="material-symbols-rounded" style="font-size:14px;vertical-align:-3px;">arrow_forward</span>
          </a>
        </div>
      </div>
    `;
  }).join('');

  return `
    <div class="tramplin-balloon-multi">
      <div class="tramplin-balloon-multi__header">
        <span class="material-symbols-rounded" style="font-size:18px;color:#6b7280;">location_on</span>
        ${header}
      </div>
      <div class="tramplin-balloon-multi__list">
        ${cards}
      </div>
    </div>
  `;
}

interface MapViewProps {
  onMarkersLoaded?: (count: number) => void;
  tagIds?: string[];
}

export default function MapView({ onMarkersLoaded, tagIds }: MapViewProps) {
  const [markers, setMarkers] = useState<OpportunityMapCard[]>([]);
  const [loading, setLoading] = useState(false);
  const mapRef = useRef<any>(null);
  const { isFavorite, toggleFavorite } = useFavorites();

  // Храним toggleFavorite в ref, чтобы listener не пересоздавался каждый рендер
  const toggleFavRef = useRef(toggleFavorite);
  useEffect(() => {
    toggleFavRef.current = toggleFavorite;
  }, [toggleFavorite]);

  // Делегирование кликов из метки - ловим кнопки 
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      const target = e.target as HTMLElement | null;
      if (!target) return;
      const btn = target.closest('[data-tramplin-action="toggle-fav"]') as HTMLElement | null;
      if (!btn) return;

      e.preventDefault();
      e.stopPropagation();

      const oppId = btn.getAttribute('data-tramplin-opp-id');
      if (oppId) {
        toggleFavRef.current(oppId);
      }
    }

    document.addEventListener('click', handleClick);
    return () => document.removeEventListener('click', handleClick);
  }, []);

  const grouped = useMemo(() => {
    const map = new Map<string, OpportunityMapCard[]>();
    for (const m of markers) {
      if (m.latitude == null || m.longitude == null) continue;
      const key = `${m.latitude.toFixed(5)},${m.longitude.toFixed(5)}`;
      const arr = map.get(key) ?? [];
      arr.push(m);
      map.set(key, arr);
    }
    return map;
  }, [markers]);

  const loadMarkers = useCallback(async (map: any) => {
    try {
      const bounds = map.getBounds();
      const swLat = bounds[0][0];
      const swLng = bounds[0][1];
      const neLat = bounds[1][0];
      const neLng = bounds[1][1];

      setLoading(true);
      const data = await getOpportunitiesForMap({ swLat, swLng, neLat, neLng }, tagIds);
      setMarkers(data);
      onMarkersLoaded?.(data.length);
    } catch (err) {
      console.error('Ошибка загрузки маркеров:', err);
    } finally {
      setLoading(false);
    }
  }, [onMarkersLoaded, tagIds]);

  const handleBoundsChange = useCallback((e: any) => {
    const map = e.get('target');
    mapRef.current = map;
    loadMarkers(map);
  }, [loadMarkers]);

  useEffect(() => {
    if (mapRef.current) {
      loadMarkers(mapRef.current);
    }
  }, [tagIds, loadMarkers]);

  return (
    <div className={styles.mapContainer}>
      {loading && <div className={styles.mapLoader}>Загрузка маркеров...</div>}
      <YMaps query={{ apikey: import.meta.env.VITE_YANDEX_MAPS_API_KEY || '', lang: 'ru_RU', load: 'package.full' }}>
        <YMap
          defaultState={{
            center: [55.7558, 37.6176],
            zoom: 10,
            controls: ['zoomControl', 'fullscreenControl'],
          }}
          width="100%"
          height="100%"
          onBoundsChange={handleBoundsChange}
          modules={['geoObject.addon.balloon', 'geoObject.addon.hint']}
        >
          <Clusterer
            options={{
              preset: 'islands#invertedBlueClusterIcons',
              groupByCoordinates: false,
              clusterDisableClickZoom: false,
              clusterBalloonContentLayout: 'cluster#balloonCarousel',
            }}
          >
            {Array.from(grouped.entries()).map(([coordKey, group]) => {
              const first = group[0];
              const favIds = new Set(group.filter(opp => isFavorite(opp.id)).map(opp => opp.id));
              const hasFavorite = favIds.size > 0;
              const preset = hasFavorite
                ? 'islands#orangeCircleDotIcon'
                : (MARKER_PRESETS[first.type] || 'islands#blueCircleDotIcon');

              const balloonContent = group.length === 1
                ? buildBalloonContent(first, hasFavorite)
                : buildMultiBalloonContent(group, favIds);

              const hint = group.length === 1
                ? first.title
                : `${group.length} ${pluralizeVacancies(group.length)} · ${first.companyName}`;

              // Включаем флаг избранного в key, чтобы Яндекс пересоздал Placemark при изменении
              return (
                <Placemark
                  key={`${coordKey}-${hasFavorite}-${group.length}`}
                  geometry={[first.latitude!, first.longitude!]}
                  options={{ preset }}
                  properties={{
                    balloonContentBody: balloonContent,
                    hintContent: hint,
                  }}
                />
              );
            })}
          </Clusterer>
        </YMap>
      </YMaps>
    </div>
  );
}