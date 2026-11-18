package cr.ac.ucr.if0009.insecurepay.service;

import cr.ac.ucr.if0009.insecurepay.dto.TransaccionDto;
import cr.ac.ucr.if0009.insecurepay.dto.TransferenciaDto;
import cr.ac.ucr.if0009.insecurepay.model.CuentaBancaria;
import cr.ac.ucr.if0009.insecurepay.model.Transaccion;
import cr.ac.ucr.if0009.insecurepay.repository.CuentaRepository;
import cr.ac.ucr.if0009.insecurepay.repository.TransaccionRepository;
import cr.ac.ucr.if0009.insecurepay.exception.AccesoDenegadoException;
import cr.ac.ucr.if0009.insecurepay.exception.RecursoNoEncontradoException;
import cr.ac.ucr.if0009.insecurepay.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final CuentaRepository cuentaRepository;
    private final JwtUtil jwtUtil;

    public TransaccionService(TransaccionRepository transaccionRepository, CuentaRepository cuentaRepository,
                              JwtUtil jwtUtil) {
        this.transaccionRepository = transaccionRepository;
        this.cuentaRepository = cuentaRepository;
        this.jwtUtil = jwtUtil;
    }

    public List<TransaccionDto> listarTodas() {
        return transaccionRepository.findAll().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public TransaccionDto obtenerPorId(Long id) {
        Transaccion transaccion = transaccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaccion no encontrada con ID: " + id));
        return convertirADto(transaccion);
    }

    public List<TransaccionDto> obtenerPorCuenta(Long cuentaId) {
        return transaccionRepository.findByCuentaOrigenIdOrCuentaDestinoIdOrderByFechaDesc(cuentaId, cuentaId).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public List<TransaccionDto> buscarPorConcepto(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return List.of();
        }
        return transaccionRepository.buscarPorTerminoSeguro(termino.trim()).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransaccionDto transferir(TransferenciaDto dto, String authHeader) {
        if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto a transferir debe ser mayor a cero.");
        }

        CuentaBancaria cuentaOrigen = cuentaRepository.findById(dto.getCuentaOrigenId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta de origen no encontrada"));

        CuentaBancaria cuentaDestino = cuentaRepository.findByNumeroCuenta(dto.getNumeroCuentaDestino())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta de destino no encontrada"));

        Claims identidad = identidadDesde(authHeader);
        String rol = identidad.get("rol", String.class);
        boolean esPersonalAutorizado = "ADMIN".equalsIgnoreCase(rol) || "AUDITOR".equalsIgnoreCase(rol);
        if (!esPersonalAutorizado
                && !cuentaOrigen.getUsuario().getUsername().equalsIgnoreCase(identidad.getSubject())) {
            throw new AccesoDenegadoException("No puede debitar fondos de una cuenta ajena");
        }

        if (cuentaOrigen.getSaldo().compareTo(dto.getMonto()) < 0) {
            throw new RuntimeException("Fondos insuficientes en la cuenta de origen seleccionada.");
        }

        // Efectuar debito y credito
        cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(dto.getMonto()));
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(dto.getMonto()));
        cuentaRepository.save(cuentaOrigen);
        cuentaRepository.save(cuentaDestino);

        Transaccion transaccion = new Transaccion();
        transaccion.setCuentaOrigen(cuentaOrigen);
        transaccion.setCuentaDestino(cuentaDestino);
        transaccion.setMonto(dto.getMonto());
        transaccion.setConceptoHtml(sanitizarTexto(dto.getConceptoHtml()));
        transaccion.setEstado("COMPLETADA");

        Transaccion guardada = transaccionRepository.save(transaccion);
        return convertirADto(guardada);
    }

    private String sanitizarTexto(String entrada) {
        if (entrada == null) {
            return "";
        }
        return entrada
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private Claims identidadDesde(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")
                || authHeader.length() <= 7) {
            throw new AccesoDenegadoException("Cabecera de autorizacion invalida");
        }
        try {
            return jwtUtil.getClaims(authHeader.substring(7));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AccesoDenegadoException("Token de autorizacion invalido");
        }
    }

    private TransaccionDto convertirADto(Transaccion t) {
        TransaccionDto dto = new TransaccionDto();
        dto.setId(t.getId());
        dto.setCuentaOrigenId(t.getCuentaOrigen().getId());
        dto.setNumeroCuentaOrigen(t.getCuentaOrigen().getNumeroCuenta());
        dto.setTitularOrigen(t.getCuentaOrigen().getUsuario().getNombreCompleto());
        dto.setCuentaDestinoId(t.getCuentaDestino().getId());
        dto.setNumeroCuentaDestino(t.getCuentaDestino().getNumeroCuenta());
        dto.setTitularDestino(t.getCuentaDestino().getUsuario().getNombreCompleto());
        dto.setMonto(t.getMonto());
        dto.setConceptoHtml(t.getConceptoHtml());
        dto.setFecha(t.getFecha());
        dto.setEstado(t.getEstado());
        return dto;
    }
}

