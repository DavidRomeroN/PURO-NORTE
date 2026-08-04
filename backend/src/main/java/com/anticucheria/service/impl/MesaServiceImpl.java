package com.anticucheria.service.impl;

import com.anticucheria.dto.mapper.CatalogoMapper;
import com.anticucheria.dto.request.MesaRequest;
import com.anticucheria.dto.response.MesaResponse;
import com.anticucheria.exception.ReglaNegocioException;
import com.anticucheria.exception.ResourceNotFoundException;
import com.anticucheria.model.Mesa;
import com.anticucheria.model.enums.EstadoMesa;
import com.anticucheria.repository.MesaRepository;
import com.anticucheria.service.MesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MesaServiceImpl implements MesaService {

    private final MesaRepository mesaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MesaResponse> listar() {
        return mesaRepository.findAllByOrderByNumeroAsc().stream()
                .map(CatalogoMapper::toMesaResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MesaResponse obtener(Long id) {
        return CatalogoMapper.toMesaResponse(buscar(id));
    }

    @Override
    @Transactional
    public MesaResponse crear(MesaRequest request) {
        if (mesaRepository.existsByNumero(request.getNumero())) {
            throw new ReglaNegocioException("Ya existe la mesa " + request.getNumero());
        }
        Mesa mesa = Mesa.builder()
                .numero(request.getNumero())
                .estado(EstadoMesa.LIBRE)
                .build();
        return CatalogoMapper.toMesaResponse(mesaRepository.save(mesa));
    }

    @Override
    @Transactional
    public MesaResponse actualizar(Long id, MesaRequest request) {
        Mesa mesa = buscar(id);
        if (!mesa.getNumero().equals(request.getNumero()) && mesaRepository.existsByNumero(request.getNumero())) {
            throw new ReglaNegocioException("Ya existe la mesa " + request.getNumero());
        }
        mesa.setNumero(request.getNumero());
        return CatalogoMapper.toMesaResponse(mesaRepository.save(mesa));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Mesa mesa = buscar(id);
        if (mesa.getEstado() == EstadoMesa.OCUPADA) {
            throw new ReglaNegocioException("No se puede eliminar la mesa " + mesa.getNumero() + " porque está ocupada");
        }
        mesaRepository.delete(mesa);
    }

    private Mesa buscar(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + id));
    }
}
