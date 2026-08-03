/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Seguridad y Usuarios
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de security en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de security.js.

(function () {
    const unsafeMethods = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);
    const nativeFetch = window.fetch.bind(window);

    function readCookie(name) {
        return document.cookie
            .split(';')
            .map(part => part.trim())
            .find(part => part.startsWith(`${name}=`))
            ?.substring(name.length + 1) || '';
    }

    function readMeta(name) {
        return document.querySelector(`meta[name="${name}"]`)?.getAttribute('content') || '';
    }

    function obtenerCsrfToken() {
        const token = readMeta('_csrf') || readCookie('XSRF-TOKEN');
        return token ? decodeURIComponent(token) : '';
    }

    function obtenerCsrfHeaderName() {
        return readMeta('_csrf_header') || 'X-XSRF-TOKEN';
    }

    function isSameOrigin(input) {
        const url = input instanceof Request ? input.url : String(input);
        return new URL(url, window.location.href).origin === window.location.origin;
    }

    function resolveMethod(input, init) {
        return String(init?.method || (input instanceof Request ? input.method : 'GET')).toUpperCase();
    }

    window.fetch = function (input, init = {}) {
        const method = resolveMethod(input, init);

        if (!unsafeMethods.has(method) || !isSameOrigin(input)) {
            return nativeFetch(input, init);
        }

        const token = obtenerCsrfToken();
        if (!token) {
            return nativeFetch(input, init);
        }

        const headers = new Headers(init.headers || (input instanceof Request ? input.headers : undefined));
        const headerName = obtenerCsrfHeaderName();
        if (!headers.has(headerName)) {
            headers.set(headerName, token);
        }

        return nativeFetch(input, {
            ...init,
            headers,
            credentials: init.credentials || 'same-origin'
        });
    };

    window.obtenerCsrfToken = obtenerCsrfToken;
    window.obtenerCsrfHeaderName = obtenerCsrfHeaderName;

    window.escapeHtml = function (value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    };

    window.sanitizeHtml = function (html) {
        const template = document.createElement('template');
        template.innerHTML = String(html ?? '');

        template.content
            .querySelectorAll('script, style, iframe, object, embed, form, input, button, meta, link, base')
            .forEach(element => element.remove());

        template.content.querySelectorAll('*').forEach(element => {
            Array.from(element.attributes).forEach(attribute => {
                const name = attribute.name.toLowerCase();
                const value = attribute.value.trim().toLowerCase();

                if (name.startsWith('on')) {
                    element.removeAttribute(attribute.name);
                }

                if ((name === 'href' || name === 'src' || name === 'xlink:href')
                        && (value.startsWith('javascript:') || value.startsWith('data:text/html'))) {
                    element.removeAttribute(attribute.name);
                }
            });
        });

        return template.innerHTML;
    };
})();

// Fin de la lógica principal de security.js.
