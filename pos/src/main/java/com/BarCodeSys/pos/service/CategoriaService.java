/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de categoria.
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
import org.springframework.stereotype.Service;

import com.BarCodeSys.pos.exceptions.RecursoNoEncontradoException;
import com.BarCodeSys.pos.models.Categoria;
import com.BarCodeSys.pos.models.Empresa;
import com.BarCodeSys.pos.repository.CategoriaRepository;
import com.BarCodeSys.pos.security.SecurityUtils;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de CategoriaService.

@Service
public class CategoriaService {

    // 1. Declarado como 'final' garantiza que el repositorio no se modifique por error en tiempo de ejecución
    private final CategoriaRepository categoriaRepository;

    // 2. Inyección por constructor: Estándar moderno de Spring Boot (reemplaza al @Autowired en la variable)
    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listarTodas() {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        return categoriaRepository.findByEmpresaId(empresaId);
    }

    public Categoria guardar(Categoria categoria) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        categoria.setEmpresa(empresa);
        
        return categoriaRepository.save(categoria);
    }

    public Categoria buscarPorId(Long id) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        // 3. Uso de la excepción personalizada para retornar un 404 limpio
        return categoriaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada o no pertenece a tu empresa. ID: " + id));
    }

    public void eliminar(Long id) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        
        if (!categoriaRepository.existsByIdAndEmpresaId(id, empresaId)) {
            // 3. Uso de la excepción personalizada
            throw new RecursoNoEncontradoException("No se puede eliminar. La categoría no existe o no tienes permisos. ID: " + id);
        }
        
        categoriaRepository.deleteById(id);
    }
}

// Fin de la definición principal de CategoriaService.
