export interface Usuario {
  email: string;
}

export interface Ficha {
  material: string;
  color: string;
  ancho: number | null;
  largo: number | null;
  ubicacion: string;
}

export interface Retazo extends Ficha {
  id: number;
  ancho: number;
  largo: number;
  girado: boolean;
}

export class ErrorApi extends Error {
  constructor(readonly estado: number) {
    super('La petición al taller falló.');
  }
}

export async function api<T>(ruta: string, metodo = 'GET', datos?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (metodo !== 'GET') headers['X-Retazo'] = '1';
  if (datos !== undefined) headers['Content-Type'] = 'application/json';
  const respuesta = await fetch('/api/' + ruta, {
    method: metodo,
    credentials: 'same-origin',
    headers,
    body: datos === undefined ? undefined : JSON.stringify(datos)
  });
  if (!respuesta.ok) throw new ErrorApi(respuesta.status);
  if (respuesta.status === 204) return undefined as T;
  const contenido = await respuesta.text();
  return contenido ? JSON.parse(contenido) as T : undefined as T;
}

export function medidaValida(valor: number | null): boolean {
  return valor !== null && Number.isInteger(valor) && valor >= 1 && valor <= 10000;
}
