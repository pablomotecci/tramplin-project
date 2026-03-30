import { useState, useRef } from 'react';
import { uploadFile } from '../../api/upload';

interface FileUploadProps {
  label: string;
  accept?: string;
  currentUrl?: string | null;
  onUploaded: (url: string) => void;
}

export function FileUpload({ label, accept = 'image/*', currentUrl, onUploaded }: FileUploadProps) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  async function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      setError('Файл не должен превышать 5 МБ');
      return;
    }

    setError(null);
    setUploading(true);
    try {
      const result = await uploadFile(file);
      onUploaded(result.url);
    } catch {
      setError('Ошибка загрузки файла');
    } finally {
      setUploading(false);
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
      <label style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--color-text-primary)' }}>
        {label}
      </label>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        {currentUrl && (
          <img
            src={currentUrl}
            alt=""
            style={{
              width: 48, height: 48, borderRadius: '10px', objectFit: 'cover',
              border: '1px solid var(--color-border)',
            }}
          />
        )}
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={uploading}
          style={{
            padding: '0.4rem 1rem', borderRadius: '8px',
            border: '1px solid var(--color-border)', background: 'var(--color-surface)',
            fontSize: '0.85rem', fontWeight: 500, cursor: 'pointer',
            color: 'var(--color-text)', transition: 'border-color 0.2s',
          }}
        >
          {uploading ? 'Загрузка...' : currentUrl ? 'Заменить' : 'Загрузить'}
        </button>
        <input
          ref={inputRef}
          type="file"
          accept={accept}
          onChange={handleChange}
          style={{ display: 'none' }}
        />
      </div>
      {error && (
        <span style={{ fontSize: '0.8rem', color: 'var(--color-error, #dc2626)' }}>{error}</span>
      )}
    </div>
  );
}
