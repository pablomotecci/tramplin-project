import { useState, useCallback, useRef, useEffect } from 'react';
import { YMaps, Map, Clusterer, Placemark } from '@pbe/react-yandex-maps';
import { getOpportunitiesForMap } from '../../api/opportunities';
import type { OpportunityMapCard } from '../../types';
import styles from './Map.module.css';
import { useFavorites } from '../../hooks/useFavorites';

/*
  Компонент Яндекс карты с маркерами вакансий
  При клике на маркер — показывает модульную карточку.
*/

// Цвета маркеров по типу возможности (из плана 04)
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

// Формируем HTML для маркера (модульная карточка при клике на маркер)
function buildBalloonContent(opp: OpportunityMapCard): string {
  const typeBg = opp.type === 'VACANCY' ? '#dbeafe' : opp.type === 'INTERNSHIP' ? '#fff3ed' : opp.type === 'EVENT' ? '#d1fae5' : '#ede9fe';
  const typeColor = opp.type === 'VACANCY' ? '#1d4ed8' : opp.type === 'INTERNSHIP' ? '#E8622C' : opp.type === 'EVENT' ? '#059669' : '#7c3aed';

  return `
    <div class="tramplin-balloon">
      <!-- Шапка: тип + формат -->
      <div class="tramplin-balloon__header">
        <span class="tramplin-balloon__type" style="background:${typeBg};color:${typeColor};">
          ${TYPE_LABELS[opp.type] || opp.type}
        </span>
        <span class="tramplin-balloon__format">
          <span class="material-symbols-rounded" style="font-size:14px;">${FORMAT_ICONS[opp.workFormat] || 'work'}</span>
          ${FORMAT_LABELS[opp.workFormat] || opp.workFormat}
        </span>
      </div>

      <!-- Название -->
      <div class="tramplin-balloon__title">${opp.title}</div>

      <!-- Компания -->
      <div class="tramplin-balloon__company">
        ${opp.logoUrl
          ? `<img src="${opp.logoUrl}" alt="" class="tramplin-balloon__logo" />`
          : `<div class="tramplin-balloon__logo-placeholder">${opp.companyName.charAt(0)}</div>`
        }
        <span>${opp.companyName}</span>
      </div>

      <!-- Теги -->
      ${opp.tags && opp.tags.length > 0 ? `
        <div class="tramplin-balloon__tags">
          ${opp.tags.slice(0, 4).map(tag => `<span class="tramplin-balloon__tag">${tag}</span>`).join('')}
        </div>
      ` : ''}

      <!-- Подвал: город + зарплата -->
      <div class="tramplin-balloon__footer">
        <span class="tramplin-balloon__city">
          <span class="material-symbols-rounded" style="font-size:14px;">location_on</span>
          ${opp.city}
        </span>
        <span class="tramplin-balloon__salary">${formatSalary(opp.salaryMin, opp.salaryMax)}</span>
      </div>

      <!-- Кнопка -->
      <a href="/opportunities/${opp.id}" class="tramplin-balloon__link">
        Подробнее
        <span class="material-symbols-rounded" style="font-size:16px;">arrow_forward</span>
      </a>
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
  const { isFavorite } = useFavorites();

  // Загрузка маркеров при изменении видимой области карты
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

  // При смене тегов — перезагружаем маркеры с текущими bounds
  useEffect(() => {
    if (mapRef.current) {
      loadMarkers(mapRef.current);
    }
  }, [tagIds, loadMarkers]);



  
  return (
    <div className={styles.mapContainer}>
      {loading && <div className={styles.mapLoader}>Загрузка маркеров...</div>}
      <YMaps query={{ apikey: import.meta.env.VITE_YANDEX_MAPS_API_KEY || '', lang: 'ru_RU', load: 'package.full' }}>
        <Map
          defaultState={{
            center: [55.7558, 37.6176], // Москва
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
            {markers.map(opp => (
              <Placemark
                key={opp.id}
                geometry={[opp.latitude!, opp.longitude!]}
                options={{
                  preset: isFavorite(opp.id)
                  ? 'islands#orangeCircleDotIcon'
                  : (MARKER_PRESETS[opp.type] || 'islands#blueCircleDotIcon'),
                }}
                properties={{
                  balloonContentBody: buildBalloonContent(opp),
                  hintContent: opp.title,
                }}
              />
            ))}
          </Clusterer>
        </Map>
      </YMaps>
    </div>
  );
}