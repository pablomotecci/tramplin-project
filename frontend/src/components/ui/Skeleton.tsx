import styles from './Skeleton.module.css';

interface SkeletonProps {
  width?: string;
  height?: string;
  radius?: string;
  style?: React.CSSProperties;
}

export function Skeleton({ width = '100%', height = '1rem', radius = '8px', style }: SkeletonProps) {
  return (
    <div
      className={styles.skeleton}
      style={{ width, height, borderRadius: radius, ...style }}
    />
  );
}

export function SkeletonCard() {
  return (
    <div className={styles.card}>
      <div className={styles.cardRow}>
        <Skeleton width="70px" height="20px" radius="12px" />
        <Skeleton width="50px" height="16px" />
      </div>
      <Skeleton width="85%" height="1.1rem" />
      <div className={styles.cardRow}>
        <Skeleton width="28px" height="28px" radius="6px" />
        <Skeleton width="120px" height="14px" />
      </div>
      <div className={styles.cardRow}>
        <Skeleton width="50px" height="20px" radius="10px" />
        <Skeleton width="60px" height="20px" radius="10px" />
        <Skeleton width="45px" height="20px" radius="10px" />
      </div>
      <div className={styles.cardRow} style={{ justifyContent: 'space-between' }}>
        <Skeleton width="80px" height="14px" />
        <Skeleton width="100px" height="14px" />
      </div>
    </div>
  );
}

export function SkeletonProfile() {
  return (
    <div className={styles.profile}>
      <div className={styles.cardRow}>
        <Skeleton width="56px" height="56px" radius="50%" />
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
          <Skeleton width="180px" height="1.1rem" />
          <Skeleton width="220px" height="0.85rem" />
        </div>
      </div>
      <Skeleton width="100%" height="1rem" style={{ marginTop: '1rem' }} />
      <Skeleton width="60%" height="1rem" />
    </div>
  );
}