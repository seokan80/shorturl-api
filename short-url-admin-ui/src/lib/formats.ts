export const formatDateTime = (value?: string | null): string =>
  value ? new Date(value).toLocaleString() : "-";

export const toInputDateTime = (value?: string | null): string => {
  if (!value) return "";
  return value.length >= 16 ? value.slice(0, 16) : value;
};

export const toServerDateTime = (value: string): string =>
  value.length === 16 ? `${value}:00` : value;

export const isExpired = (value?: string | null): boolean =>
  value ? new Date(value).getTime() < Date.now() : false;
