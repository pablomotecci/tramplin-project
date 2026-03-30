import { Link } from 'react-router-dom';
import styles from './Footer.module.css';

export function Footer() {
  return (
    <footer className={styles.footer}>
      <div className={styles.container}>

        <div className={styles.top}>

          <div className={styles.brand}>
            <Link to="/" className={styles.logo}>
              <span className="material-symbols-rounded" style={{ fontSize: '28px' }}>footprint</span>
              <span className={styles.logoText}>Трамплин</span>
            </Link>
            <p className={styles.brandDesc}>
              Карьерная платформа для студентов и&nbsp;выпускников в&nbsp;сфере IT.
              Стажировки, вакансии, менторы.
            </p>
          </div>

          <div className={styles.column}>
            <h4 className={styles.columnTitle}>Платформа</h4>
            <Link to="/" className={styles.link}>Карта вакансий</Link>
            <Link to="/" className={styles.link}>О платформе</Link>
          </div>

          <div className={styles.column}>
            <h4 className={styles.columnTitle}>Соискателям</h4>
            <Link to="/profile" className={styles.link}>Мой профиль</Link>
            <Link to="/" className={styles.link}>Поиск вакансий</Link>
          </div>

          <div className={styles.column}>
            <h4 className={styles.columnTitle}>Работодателям</h4>
            <Link to="/company" className={styles.link}>Кабинет компании</Link>
            <Link to="/company/opportunities/new" className={styles.link}>Создать вакансию</Link>
          </div>
        </div>

        <div className={styles.divider} />

        <div className={styles.bottom}>
          <span className={styles.copyright}>
             2026 Трамплин · ДетиДедлайна · IT-Планета
          </span>
          <div className={styles.socials}>
            <a href="https://t.me/+h2iv-THiq_oxOTZi" target="_blank" rel="noopener noreferrer" className={styles.socialLink} title="Telegram">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69.01-.03.01-.14-.07-.2-.08-.06-.19-.04-.27-.02-.12.03-1.97 1.25-5.55 3.67-.53.36-1.01.54-1.43.53-.47-.01-1.38-.27-2.05-.49-.83-.27-1.49-.42-1.43-.88.03-.24.37-.49 1.02-.74 3.98-1.73 6.64-2.87 7.97-3.44 3.8-1.58 4.59-1.86 5.1-1.87.11 0 .37.03.53.17.14.12.18.28.2.45-.01.06.01.24 0 .38z"/></svg>
            </a>
            <a href="https://vk.me/join/bb1akpHkvJJgsNkOBd/jatO5RI/fHQ567S4=" target="_blank" rel="noopener noreferrer" className={styles.socialLink} title="VK">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm3.97 13.5h-1.23c-.47 0-.61-.37-1.45-1.22-.73-.7-1.05-.8-1.23-.8-.25 0-.32.07-.32.42v1.12c0 .3-.09.48-1.54.48-1.54 0-3.05-.93-4.17-2.66C4.8 10.97 4.5 9.42 4.5 9.08c0-.18.07-.35.42-.35h1.23c.31 0 .43.14.55.49.61 1.76 1.62 3.3 2.04 3.3.15 0 .22-.07.22-.46v-1.82c-.05-.79-.46-.86-.46-1.14 0-.14.12-.28.31-.28h1.93c.26 0 .35.14.35.47v2.45c0 .26.12.35.19.35.15 0 .28-.09.57-.38.88-.99 1.51-2.52 1.51-2.52.08-.18.22-.35.53-.35h1.23c.37 0 .45.19.37.47-.15.7-1.64 2.81-1.64 2.81-.13.21-.18.31 0 .55.13.17.55.55.84.88.53.61.93 1.12 1.04 1.48.12.35-.06.53-.42.53z"/></svg>
            </a>
            <a href="https://github.com/" target="_blank" rel="noopener noreferrer" className={styles.socialLink} title="GitHub">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.477 2 2 6.477 2 12c0 4.42 2.865 8.17 6.839 9.49.5.092.682-.217.682-.482 0-.237-.008-.866-.013-1.7-2.782.604-3.369-1.34-3.369-1.34-.454-1.156-1.11-1.464-1.11-1.464-.908-.62.069-.608.069-.608 1.003.07 1.531 1.03 1.531 1.03.892 1.529 2.341 1.087 2.91.831.092-.646.35-1.086.636-1.336-2.22-.253-4.555-1.11-4.555-4.943 0-1.091.39-1.984 1.029-2.683-.103-.253-.446-1.27.098-2.647 0 0 .84-.269 2.75 1.025A9.578 9.578 0 0112 6.836c.85.004 1.705.115 2.504.337 1.909-1.294 2.747-1.025 2.747-1.025.546 1.377.203 2.394.1 2.647.64.699 1.028 1.592 1.028 2.683 0 3.842-2.339 4.687-4.566 4.935.359.309.678.919.678 1.852 0 1.336-.012 2.415-.012 2.743 0 .267.18.578.688.48C19.138 20.167 22 16.418 22 12c0-5.523-4.477-10-10-10z"/></svg>
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}