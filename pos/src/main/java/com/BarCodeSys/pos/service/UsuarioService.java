/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Seguridad y Usuarios
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de usuario.
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

import com.BarCodeSys.pos.dto.UsuarioResponseDTO;
import com.BarCodeSys.pos.exceptions.RecursoNoEncontradoException;
import com.BarCodeSys.pos.models.Empresa;
import com.BarCodeSys.pos.models.Rol;
import com.BarCodeSys.pos.models.Usuario;
import com.BarCodeSys.pos.repository.EmpresaRepository;
import com.BarCodeSys.pos.repository.UsuarioRepository;
import com.BarCodeSys.pos.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de UsuarioService.

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponseDTO> listarPorEmpresa() {
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();
        return usuarioRepository.findByEmpresaId(idEmpresa)
                .stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    public List<UsuarioResponseDTO> listarPorEmpresaEspecifica(Long idEmpresa) {
        return usuarioRepository.findByEmpresaId(idEmpresa)
                .stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioResponseDTO guardar(Usuario usuario) {
        usuario.setId(null);

        Long idEmpresaDestino;
        if (SecurityUtils.isSuperAdmin()) {
            if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
                throw new RuntimeException("Como Super Admin, debes especificar el ID de la empresa del nuevo usuario.");
            }
            idEmpresaDestino = usuario.getEmpresa().getId();
        } else {
            if (usuario.getRol() != null && usuario.getRol() != Rol.CAJERO) {
                throw new RuntimeException("Acceso denegado: solo puedes crear usuarios cajeros.");
            }
            usuario.setRol(Rol.CAJERO);
            idEmpresaDestino = SecurityUtils.getCurrentEmpresaId();
        }

        Empresa empresaTarget = empresaRepository.findById(idEmpresaDestino)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada."));

        long totalUsuarios = usuarioRepository.countByEmpresaId(idEmpresaDestino);
        if (totalUsuarios >= empresaTarget.getLimiteUsuarios()) {
            throw new RuntimeException("Limite de plan alcanzado: la empresa ya tiene "
                    + empresaTarget.getLimiteUsuarios() + " usuarios permitidos.");
        }

        if (usuario.getPassword() == null || usuario.getPassword().trim().isEmpty()) {
            throw new RuntimeException("La clave del usuario es obligatoria.");
        }

        usuario.setEmpresa(empresaTarget);
        usuario.setEmail(limpiar(usuario.getEmail()));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        return mapearADTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Usuario usuario = buscarUsuarioEditable(id, "cambiar el estado de");

        if (!SecurityUtils.isSuperAdmin() && usuario.getId().equals(SecurityUtils.getCurrentUsuarioId())) {
            throw new RuntimeException("Accion denegada: no puedes desactivar tu propio usuario.");
        }

        usuario.setActivo(!Boolean.TRUE.equals(usuario.getActivo()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void resetPassword(Long id, String nuevaClave) {
        Usuario usuario = buscarUsuarioEditable(id, "restablecer la clave de");

        if (nuevaClave == null || nuevaClave.trim().isEmpty()) {
            throw new RuntimeException("La nueva clave no puede estar vacia.");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaClave));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioResponseDTO actualizar(Long id, Usuario usuarioActualizado) {
        Usuario usuarioExistente = buscarUsuarioEditable(id, "editar");

        if (!SecurityUtils.isSuperAdmin()
                && usuarioActualizado.getRol() != null
                && usuarioActualizado.getRol() != Rol.CAJERO) {
            throw new RuntimeException("Acceso denegado: solo puedes asignar el rol de cajero.");
        }

        usuarioExistente.setUsername(usuarioActualizado.getUsername());
        usuarioExistente.setEmail(limpiar(usuarioActualizado.getEmail()));

        if (usuarioActualizado.getRol() != null) {
            usuarioExistente.setRol(usuarioActualizado.getRol());
        }

        if (usuarioActualizado.getPassword() != null && !usuarioActualizado.getPassword().trim().isEmpty()) {
            usuarioExistente.setPassword(passwordEncoder.encode(usuarioActualizado.getPassword()));
        }

        if (usuarioActualizado.getActivo() != null) {
            usuarioExistente.setActivo(usuarioActualizado.getActivo());
        }

        return mapearADTO(usuarioRepository.save(usuarioExistente));
    }

    private Usuario buscarUsuarioEditable(Long id, String accion) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        if (!SecurityUtils.isSuperAdmin()) {
            Long idEmpresaToken = SecurityUtils.getCurrentEmpresaId();
            if (usuario.getEmpresa() == null || !usuario.getEmpresa().getId().equals(idEmpresaToken)) {
                throw new RuntimeException("Acceso denegado: este usuario no pertenece a tu empresa.");
            }
            if (usuario.getRol() != Rol.CAJERO) {
                throw new RuntimeException("Acceso denegado: solo puedes " + accion + " usuarios cajeros.");
            }
        }

        return usuario;
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private UsuarioResponseDTO mapearADTO(Usuario usuario) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(usuario.getId());
        dto.setUsername(usuario.getUsername());
        dto.setEmail(usuario.getEmail());
        dto.setRol(usuario.getRol());
        dto.setActivo(usuario.getActivo());

        if (usuario.getEmpresa() != null) {
            dto.setEmpresaId(usuario.getEmpresa().getId());
            dto.setEmpresaNombre(usuario.getEmpresa().getNombreComercial());
        }

        return dto;
    }
}

// Fin de la definición principal de UsuarioService.
