export async function api(path, { method = 'GET', body, token } = {}) {
  const res = await fetch('/api' + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && token) { localStorage.clear(); location.reload(); }
    throw new Error(data?.message || `Request failed (${res.status})`);
  }
  return data;
}
