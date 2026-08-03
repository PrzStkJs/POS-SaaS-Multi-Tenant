/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Analítica y Dashboard
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de admin analitica en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de admin-analitica.js.

/* ============================================================
   MÓDULO DE ANALÍTICA IA (VERSIÓN FINAL)
   ============================================================ */

/**
 * Punto de entrada del módulo.
 */

const escaparAnalitica = (valor) => window.escapeHtml ? window.escapeHtml(valor) : String(valor ?? '');
const sanitizarAnalitica = (html) => window.sanitizeHtml ? window.sanitizeHtml(html) : escaparAnalitica(html);
window.initAnaliticaModule = function() {
    console.log("Módulo de Analítica IA Inicializado.");
    inicializarGraficoPrediccion();
    configurarChatIA();
    configurarReporteEjecutivo();
    cargarProyeccionDemanda();  // ← Se carga la tabla de proyección al iniciar
};

/* ============================================================
   INICIALIZAR GRÁFICO DE PREDICCIÓN
   ============================================================ */
function inicializarGraficoPrediccion() {
    const ctx = document.getElementById('graficoPrediccionVentas');
    if (!ctx) return;

    fetch('/api/ia/grafico-ventas')
        .then(response => response.json())
        .then(datos => {
            if (window.graficoIA) {
                window.graficoIA.destroy();
            }

            window.graficoIA = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: datos.labels,
                    datasets: [
                        {
                            label: 'Ventas Reales',
                            data: datos.ventas,
                            borderColor: '#212529',
                            borderWidth: 2,
                            tension: 0.3
                        },
                        {
                            label: 'Predicción IA',
                            data: datos.prediccion,
                            borderColor: '#198754',
                            borderDash: [5, 5],
                            borderWidth: 2,
                            tension: 0.3,
                            fill: true,
                            backgroundColor: 'rgba(25, 135, 84, 0.1)'
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'top' }
                    }
                }
            });

            const txtInsight = document.getElementById('textoInsightIA');
            if (txtInsight) txtInsight.textContent = datos.insight || '';

            const txtChat = document.getElementById('textoInicialChat');
            if (txtChat) txtChat.innerHTML = '<i class="bi bi-robot text-muted me-2"></i>' + escaparAnalitica(datos.mensajeInicial);
        })
        .catch(error => console.error("Error cargando el gráfico:", error));
}

/* ============================================================
   CONFIGURAR CHAT INTERACTIVO (CON MANEJO DE AMBOS ERRORES)
   ============================================================ */
function configurarChatIA() {
    const formChat = document.getElementById('formChatIA');
    const inputPregunta = document.getElementById('inputPreguntaIA');
    const btnEnviar = formChat.querySelector('button[type="submit"]');
    const chatBody = document.getElementById('chatBody');

    if (!formChat) return;

    let bloqueadoPorLimite = false;

    formChat.addEventListener('submit', function(e) {
        e.preventDefault();
        if (bloqueadoPorLimite) return;

        const pregunta = inputPregunta.value.trim();
        if (pregunta === "") return;

        agregarMensajeUsuario(pregunta);
        inputPregunta.value = '';

        const idCarga = mostrarIndicadorCarga();

        fetch('/api/ia/consultar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pregunta: pregunta })
        })
        .then(response => {
            if (!response.ok) throw new Error('Error en la respuesta del servidor');
            return response.json();
        })
        .then(data => {
            const elCarga = document.getElementById(idCarga);
            if (elCarga) elCarga.remove();

            if (data.respuesta === "ERROR_LIMITE_API") {
                iniciarContador(40);
            } else if (data.respuesta === "ERROR_CUOTA_AGOTADA") {
                bloquearChatDefinitivo();
            } else {
                agregarMensajeIA(data.respuesta);
            }
        })
        .catch(error => {
            console.error("Error conectando con la IA:", error);
            const elCarga = document.getElementById(idCarga);
            if (elCarga) elCarga.remove();
            agregarMensajeIA("Lo siento, hubo un problema de conexión con el servidor. Revisa la consola.");
        });
    });

    function iniciarContador(segundos) {
        bloqueadoPorLimite = true;
        inputPregunta.disabled = true;
        if (btnEnviar) btnEnviar.disabled = true;

        const placeholderOriginal = inputPregunta.placeholder || "Escribe tu pregunta aquí...";
        inputPregunta.placeholder = "⏳ Respuestas gratuitas en espera...";

        const mensajeId = 'timer-' + Date.now();
        let tiempoRestante = segundos;

        const htmlContador = `
            <div id="${mensajeId}" class="d-flex mb-3">
                <div class="bg-warning bg-opacity-10 p-3 rounded-3 shadow-sm border-start border-3 border-danger" style="max-width: 85%;">
                    <p class="mb-0 text-dark small">
                        ⚠️ Límite de tokens alcanzado. Tus respuestas gratuitas están en espera. Por favor, aguarda <span id="segundos-${mensajeId}" style="font-weight: bold;">${tiempoRestante}</span>s...
                    </p>
                </div>
            </div>
        `;
        chatBody.insertAdjacentHTML('beforeend', htmlContador);
        chatBody.scrollTop = chatBody.scrollHeight;

        const intervalo = setInterval(() => {
            tiempoRestante--;
            const spanSegundos = document.getElementById(`segundos-${mensajeId}`);
            if (spanSegundos) spanSegundos.innerText = tiempoRestante;

            inputPregunta.placeholder = `⏳ Respuestas en espera (${tiempoRestante}s)...`;

            if (tiempoRestante <= 0) {
                clearInterval(intervalo);

                const mensajeDiv = document.getElementById(mensajeId);
                if (mensajeDiv) {
                    mensajeDiv.innerHTML = `
                        <div class="bg-success bg-opacity-10 p-3 rounded-3 shadow-sm border-start border-3 border-success" style="max-width: 85%;">
                            <p class="mb-0 text-dark small">✅ El límite se ha restablecido. Ya puedes volver a preguntar.</p>
                        </div>
                    `;
                }

                bloqueadoPorLimite = false;
                inputPregunta.disabled = false;
                inputPregunta.placeholder = placeholderOriginal;
                if (btnEnviar) btnEnviar.disabled = false;
                inputPregunta.focus();
            }
        }, 1000);
    }

    function bloquearChatDefinitivo() {
        bloqueadoPorLimite = true;
        inputPregunta.disabled = true;
        if (btnEnviar) btnEnviar.disabled = true;

        inputPregunta.placeholder = "❌ Tus respuestas gratuitas han terminado por hoy.";

        const htmlBloqueo = `
            <div class="d-flex mb-3">
                <div class="bg-danger bg-opacity-10 p-3 rounded-3 shadow-sm border-start border-3 border-danger" style="max-width: 85%;">
                    <p class="mb-0 text-dark small">
                        <strong>❌ Límite de consultas alcanzado:</strong> Se ha agotado la cuota de consultas gratuitas de IA por hoy. El chat permanecerá deshabilitado.
                    </p>
                </div>
            </div>
        `;
        chatBody.insertAdjacentHTML('beforeend', htmlBloqueo);
        chatBody.scrollTop = chatBody.scrollHeight;
    }

    function agregarMensajeUsuario(texto) {
        const html = `
            <div class="d-flex justify-content-end mb-3">
                <div class="bg-success text-white p-3 rounded-3 shadow-sm" style="max-width: 85%;">
                    <p class="mb-0 small">${escaparAnalitica(texto)}</p>
                </div>
            </div>
        `;
        chatBody.insertAdjacentHTML('beforeend', html);
        chatBody.scrollTop = chatBody.scrollHeight;
    }

    function agregarMensajeIA(texto) {
        let textoFormateado = escaparAnalitica(texto).replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        textoFormateado = textoFormateado.replace(/\n/g, '<br>');

        const html = `
            <div class="d-flex mb-3">
                <div class="bg-light p-3 rounded-3 shadow-sm border-start border-3 border-warning" style="max-width: 85%;">
                    <p class="mb-0 text-dark small"><i class="bi bi-robot text-muted me-2"></i>${textoFormateado}</p>
                </div>
            </div>
        `;
        chatBody.insertAdjacentHTML('beforeend', html);
        chatBody.scrollTop = chatBody.scrollHeight;
    }

    function mostrarIndicadorCarga() {
        const id = 'carga-' + Date.now();
        const html = `
            <div id="${id}" class="d-flex mb-3">
                <div class="bg-light p-3 rounded-3 shadow-sm" style="max-width: 85%;">
                    <div class="spinner-grow spinner-grow-sm text-success" role="status"></div>
                    <div class="spinner-grow spinner-grow-sm text-success" role="status"></div>
                    <div class="spinner-grow spinner-grow-sm text-success" role="status"></div>
                </div>
            </div>
        `;
        chatBody.insertAdjacentHTML('beforeend', html);
        chatBody.scrollTop = chatBody.scrollHeight;
        return id;
    }
}

/* ============================================================
   CONFIGURAR REPORTE EJECUTIVO IA (APERTURA DEL MODAL)
   ============================================================ */
function configurarReporteEjecutivo() {
    const modalReporte = document.getElementById('modalReporteIA');
    if (!modalReporte) return;

    modalReporte.addEventListener('show.bs.modal', function () {
        const loader = document.getElementById('loaderReporte');
        const contenido = document.getElementById('contenidoReporte');

        loader.classList.remove('d-none');
        contenido.classList.add('d-none');
        contenido.innerHTML = '';

        fetch('/api/ia/reporte-ejecutivo')
            .then(response => {
                if (!response.ok) throw new Error('Error en la red');
                return response.json();
            })
            .then(data => {
                loader.classList.add('d-none');
                contenido.classList.remove('d-none');

                if (data.reporte) {
                    contenido.innerHTML = sanitizarAnalitica(data.reporte);
                } else if (data.error) {
                    contenido.innerHTML = `
                        <div class="alert alert-warning border-warning shadow-sm" role="alert">
                            <i class="bi bi-exclamation-triangle-fill me-2 text-warning"></i> 
                            <strong>Aviso:</strong> ${escaparAnalitica(data.error)}
                        </div>`;
                }
            })
            .catch(error => {
                console.error("Error al generar el reporte:", error);
                loader.classList.add('d-none');
                contenido.classList.remove('d-none');
                contenido.innerHTML = `
                    <div class="alert alert-danger shadow-sm" role="alert">
                        <i class="bi bi-wifi-off me-2"></i> No se pudo conectar con el motor de IA. Intente de nuevo en unos minutos.
                    </div>`;
            });
    });
}

/* ============================================================
   IMPRESIÓN LIMPIA DEL REPORTE
   ============================================================ */
window.imprimirReporteLimpio = function() {
    const contenidoHTML = document.getElementById('contenidoReporte').innerHTML;
    
    if (!contenidoHTML || contenidoHTML.trim() === '') return;

    const ventanaImpresion = window.open('', '_blank', 'width=900,height=650');
    
    ventanaImpresion.document.write(`
        <html>
            <head>
                <title>Reporte Estratégico Gerencial</title>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
                <style>
                    body { 
                        padding: 40px; 
                        font-family: Arial, sans-serif; 
                        color: #333;
                    }
                    @media print {
                        body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                        .card { border: 1px solid #ddd !important; box-shadow: none !important; }
                    }
                </style>
            </head>
            <body>
                <div class="d-flex justify-content-between align-items-center border-bottom pb-3 mb-4">
                    <div>
                        <h2 class="text-success mb-0 fw-bold">Reporte Estratégico Gerencial</h2>
                        <small class="text-muted">Generado por IA Analítica - BarCodeSys</small>
                    </div>
                    <div class="text-end">
                        <small class="fw-bold d-block">Fecha de Emisión:</small>
                        <small class="text-muted">${new Date().toLocaleDateString('es-PE', { day: '2-digit', month: 'long', year: 'numeric' })}</small>
                    </div>
                </div>

                ${contenidoHTML}
                
                <script>
                    setTimeout(() => {
                        window.print();
                        window.close();
                    }, 500);
                </script>
            </body>
        </html>
    `);
    
    ventanaImpresion.document.close();
};

/* ============================================================
   CARGAR PROYECCIÓN DE DEMANDA (CONECTADO AL BACKEND JAVA)
   ============================================================ */
   window.cargarProyeccionDemanda = function() {
       const tbody = document.getElementById('tbodyProyeccion');
       if (!tbody) return;

       tbody.innerHTML = `
           <tr>
               <td colspan="5" class="text-center py-3">
                   <div class="spinner-border text-success" role="status">
                       <span class="visually-hidden">Cargando...</span>
                   </div>
               </td>
           </tr>
       `;

       fetch('/api/ia/proyeccion-inventario')
           .then(response => {
               if (!response.ok) throw new Error('Error al obtener proyección de demanda');
               return response.json();
           })
           .then(data => {
               const dataLimpia = data.filter(prod => 
                   prod.nombreProducto !== 'Venta Rápida' && 
                   prod.nombreProducto !== 'VENTA RÁPIDA' &&
                   prod.codigoBarras !== 'GENERICO'
               );

               if (!dataLimpia || dataLimpia.length === 0) {
                   tbody.innerHTML = `
                       <tr>
                           <td colspan="5" class="text-center text-muted py-3">
                               No se encontraron productos para proyectar.
                           </td>
                       </tr>
                   `;
                   return;
               }

               let filasHTML = '';
               dataLimpia.forEach(prod => {
                   let badgeClass = 'bg-success';
                   
                   if (prod.estado === 'Comprar Urgente') {
                       badgeClass = 'bg-danger';
                   } else if (prod.estado === 'Stock Crítico') {
                       badgeClass = 'bg-warning text-dark';
                   } else if (prod.estado === 'Revisar Pronto') {
                       badgeClass = 'bg-info text-dark';
                   } else if (prod.estado === 'Sin rotación') {
                       badgeClass = 'bg-secondary';
                   }

                   let textoDias = prod.diasEstimados === 999 ? '∞' : `${prod.diasEstimados} días`;

                   filasHTML += `
                       <tr>
                           <td class="ps-4 fw-bold">${escaparAnalitica(prod.nombreProducto)}</td>
                           <td class="text-center">${prod.stockActual} und.</td>
                           <td class="text-center">${prod.consumoDiario} und/día</td>
                           <td class="text-center fw-bold ${prod.diasEstimados <= 7 ? 'text-danger' : 'text-success'}">${textoDias}</td>
                           <td class="text-center pe-4"><span class="badge ${badgeClass}">${escaparAnalitica(prod.estado)}</span></td>
                       </tr>
                   `;
               });

               tbody.innerHTML = filasHTML;
           })
           .catch(error => {
               console.error("Error al cargar proyección de demanda:", error);
               tbody.innerHTML = `
                   <tr>
                       <td colspan="5" class="text-center text-danger py-3">
                           <i class="bi bi-wifi-off me-2"></i> No se pudo cargar la proyección. Intente de nuevo.
                       </td>
                   </tr>
               `;
           });
   };

// Fin de la lógica principal de admin-analitica.js.
