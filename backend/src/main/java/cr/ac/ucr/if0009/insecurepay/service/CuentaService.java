package cr.ac.ucr.if0009.insecurepay.service;

import cr.ac.ucr.if0009.insecurepay.dto.CuentaDto;
import cr.ac.ucr.if0009.insecurepay.model.CuentaBancaria;
import cr.ac.ucr.if0009.insecurepay.repository.CuentaRepository;
import cr.ac.ucr.if0009.insecurepay.exception.AccesoDenegadoException;
import cr.ac.ucr.if0009.insecurepay.exception.RecursoNoEncontradoException;
import cr.ac.ucr.if0009.insecurepay.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final JwtUtil jwtUtil;

    public CuentaService(CuentaRepository cuentaRepository, JwtUtil jwtUtil) {
        this.cuentaRepository = cuentaRepository;
        this.jwtUtil = jwtUtil;
    }

    public List<CuentaDto> listarTodas(String authHeader) {
        Claims identidad = identidadDesde(authHeader);
        List<CuentaBancaria> cuentas = esPersonalAutorizado(identidad)
                ? cuentaRepository.findAll()
                : cuentaRepository.findByUsuarioUsername(identidad.getSubject());
        return cuentas.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public CuentaDto obtenerPorId(Long id, String authHeader) {
        Claims identidad = identidadDesde(authHeader);
        CuentaBancaria cuenta = cuentaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria no encontrada"));
        validarPropiedad(cuenta, identidad);
        return convertirADto(cuenta);
    }

    public List<CuentaDto> obtenerPorUsuario(Long usuarioId, String authHeader) {
        Claims identidad = identidadDesde(authHeader);
        List<CuentaBancaria> cuentas;
        if (esPersonalAutorizado(identidad)) {
            cuentas = cuentaRepository.findByUsuarioId(usuarioId);
        } else {
            if (!cuentaRepository.findByUsuarioId(usuarioId).stream()
                    .allMatch(cuenta -> cuenta.getUsuario().getUsername().equalsIgnoreCase(identidad.getSubject()))) {
                throw new AccesoDenegadoException("No puede consultar cuentas de otro usuario");
            }
            cuentas = cuentaRepository.findByUsuarioId(usuarioId);
        }
        return cuentas.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> obtenerSaldo(Long id, String authHeader) {
        CuentaDto cuenta = obtenerPorId(id, authHeader);
        return Map.of(
                "cuentaId", cuenta.getId(),
                "numeroCuenta", cuenta.getNumeroCuenta(),
                "saldo", cuenta.getSaldo(),
                "moneda", cuenta.getMoneda()
        );
    }

    private void validarPropiedad(CuentaBancaria cuenta, Claims identidad) {
        if (!esPersonalAutorizado(identidad)
                && !cuenta.getUsuario().getUsername().equalsIgnoreCase(identidad.getSubject())) {
            throw new AccesoDenegadoException("Acceso no autorizado a cuenta ajena");
        }
    }

    private boolean esPersonalAutorizado(Claims identidad) {
        String rol = identidad.get("rol", String.class);
        return "ADMIN".equalsIgnoreCase(rol) || "AUDITOR".equalsIgnoreCase(rol);
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

    private CuentaDto convertirADto(CuentaBancaria cuenta) {
        CuentaDto dto = new CuentaDto();
        dto.setId(cuenta.getId());
        dto.setUsuarioId(cuenta.getUsuario().getId());
        dto.setTitular(cuenta.getUsuario().getNombreCompleto());
        dto.setNumeroCuenta(cuenta.getNumeroCuenta());
        dto.setTipoCuenta(cuenta.getTipoCuenta().name());
        dto.setSaldo(cuenta.getSaldo());
        dto.setMoneda(cuenta.getMoneda());
        dto.setActiva(cuenta.getActiva());
        return dto;
    }
}

