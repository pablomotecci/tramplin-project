// экранирование HTML спецсимволов
export const escapeHtml = (value: unknown): string => {
  if (value == null) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
};


export const safeUrl = (value: unknown): string => {
  if (value == null) return '';
  const str = String(value).trim();
  if (str.startsWith('http://') || str.startsWith('https://') || str.startsWith('/')) {
    return escapeHtml(str);
  }
  return ''; // отбрасываем потенциально опасные схемы
};