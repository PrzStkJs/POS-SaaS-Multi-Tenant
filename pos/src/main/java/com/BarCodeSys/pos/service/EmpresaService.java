/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Administración
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de empresa.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.service;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BarCodeSys.pos.dto.RegistroEmpresaDTO;
import com.BarCodeSys.pos.models.Empresa;
import com.BarCodeSys.pos.models.Usuario;
import com.BarCodeSys.pos.models.Rol;
import com.BarCodeSys.pos.repository.EmpresaRepository;
import com.BarCodeSys.pos.repository.UsuarioRepository;
import com.BarCodeSys.pos.security.SecurityUtils;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de EmpresaService.

@Service
@Transactional(readOnly = true) // PRO-TIP: Por defecto, todas las operaciones son de solo lectura (optimiza BD)
public class EmpresaService {

    // Dependencias inmutables (Inyección por constructor)
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeminiClient geminiClient;

    public EmpresaService(EmpresaRepository empresaRepository, 
                          UsuarioRepository usuarioRepository, 
                          PasswordEncoder passwordEncoder,
                          GeminiClient geminiClient) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.geminiClient = geminiClient;
    }

    // Este método suele ser para un SuperAdmin del sistema global
    @Transactional
    public List<Empresa> listarTodas() {
        List<Empresa> empresas = empresaRepository.findAll();
        empresas.forEach(geminiClient::normalizarCuotaDiaria);
        return empresas;
    }

    // Este método se queda igual porque se usa para REGISTRAR una empresa nueva 
    // (el usuario aún no tiene token ni sesión iniciada)
    @Transactional // Sobrescribe el readOnly = true de la clase para permitir escritura
    public Empresa guardarConAdmin(RegistroEmpresaDTO dto) {
        if (dto.getAdminUsername() != null && !dto.getAdminUsername().trim().isEmpty()) {
            if (usuarioRepository.findByUsername(dto.getAdminUsername().trim()).isPresent()) {
                throw new RuntimeException("El nombre de usuario '" + dto.getAdminUsername() + "' ya está en uso.");
            }
        }

        Empresa empresa = new Empresa();
        empresa.setNombreComercial(dto.getNombreComercial());
        empresa.setEmailContacto(dto.getEmailContacto());
        empresa.setLimiteUsuarios(dto.getLimiteUsuarios());
        empresa.setPasarelaActiva(dto.getPasarelaActiva());
        empresa.setMpAccessToken(dto.getMpAccessToken());
        empresa.setNiubizMerchantId(dto.getNiubizMerchantId());
        empresa.setNiubizApiUser(dto.getNiubizApiUser());
        empresa.setNiubizApiPassword(dto.getNiubizApiPassword());

        normalizarDatosPago(empresa);
        
        empresa = empresaRepository.save(empresa);

        if (dto.getAdminUsername() != null && !dto.getAdminUsername().trim().isEmpty() &&
            dto.getAdminPassword() != null && !dto.getAdminPassword().trim().isEmpty()) {
            
            Usuario adminInicial = new Usuario();
            adminInicial.setUsername(dto.getAdminUsername().trim());
            adminInicial.setPassword(passwordEncoder.encode(dto.getAdminPassword().trim()));
            adminInicial.setRol(Rol.ADMIN); 
            adminInicial.setEmpresa(empresa); 
            adminInicial.setActivo(true);
            
            usuarioRepository.save(adminInicial);
        }

        return empresa;
    }

    // BLINDADO: Ya no recibimos el 'Long id' como parámetro.
    // Una empresa solo puede actualizar SU PROPIA configuración.
    @Transactional
    public Empresa actualizar(RegistroEmpresaDTO datos) {
        // Obtenemos el ID de forma invisible y segura
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId(); 
        
        Empresa empresa = buscarPorId(idEmpresa);

        empresa.setNombreComercial(limpiar(datos.getNombreComercial()));
        empresa.setEmailContacto(limpiar(datos.getEmailContacto()));
        empresa.setLimiteUsuarios(datos.getLimiteUsuarios());
        empresa.setPasarelaActiva(datos.getPasarelaActiva());
        empresa.setMpAccessToken(datos.getMpAccessToken());
        empresa.setNiubizMerchantId(datos.getNiubizMerchantId());
        empresa.setNiubizApiUser(datos.getNiubizApiUser());
        empresa.setNiubizApiPassword(datos.getNiubizApiPassword());

        normalizarDatosPago(empresa);
        return empresaRepository.save(empresa);
    }

    // Lo mantenemos con el parámetro (Long id) porque este método es de uso interno
    // para que otros servicios (como UsuarioService) busquen la entidad en la base de datos.
    public Empresa buscarPorId(Long id) {
        return empresaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada con ID: " + id));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!empresaRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar. La empresa no existe.");
        }
        empresaRepository.deleteById(id);
    }

    // --- MÉTODOS PRIVADOS DE LIMPIEZA ---

    private void normalizarDatosPago(Empresa empresa) {
        empresa.setNombreComercial(limpiar(empresa.getNombreComercial()));
        empresa.setEmailContacto(limpiar(empresa.getEmailContacto()));
        empresa.setPasarelaActiva(normalizarPasarela(empresa.getPasarelaActiva()));
        empresa.setMpAccessToken(limpiar(empresa.getMpAccessToken()));
        empresa.setNiubizMerchantId(limpiar(empresa.getNiubizMerchantId()));
        empresa.setNiubizApiUser(limpiar(empresa.getNiubizApiUser()));
        empresa.setNiubizApiPassword(limpiar(empresa.getNiubizApiPassword()));

        if (empresa.getLimiteUsuarios() == null || empresa.getLimiteUsuarios() < 1) {
            empresa.setLimiteUsuarios(3);
        }
    }

    private String normalizarPasarela(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "MANUAL";
        }

        String normalizado = valor.trim().toUpperCase().replace(" ", "").replace("_", "");
        if ("NIUBIZ".equals(normalizado)) {
            return "NIUBIZ";
        }
        if ("MERCADOPAGO".equals(normalizado) || "MP".equals(normalizado)) {
            return "MERCADOPAGO";
        }
        return "MANUAL";
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}

// Fin de la definición principal de EmpresaService.
