package cr.ac.ucr.if0009.insecurepay.controller;

import cr.ac.ucr.if0009.insecurepay.dto.CuentaDto;
import cr.ac.ucr.if0009.insecurepay.service.CuentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public ResponseEntity<List<CuentaDto>> listarTodas(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(cuentaService.listarTodas(authorization));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaDto> obtenerPorId(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(cuentaService.obtenerPorId(id, authorization));
    }

    @GetMapping("/{id}/saldo")
    public ResponseEntity<Map<String, Object>> obtenerSaldo(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(cuentaService.obtenerSaldo(id, authorization));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<CuentaDto>> obtenerPorUsuario(@PathVariable Long usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(cuentaService.obtenerPorUsuario(usuarioId, authorization));
    }
}

