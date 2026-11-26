package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.dtos.BoletaEmisionDTO;
import com.example.dtos.ConfigurarDniRequestDTO;
import com.example.dtos.DniLookupDTO;
import com.example.dtos.EmitirBoletaRequestDTO;
import com.example.dtos.UsuarioDTO;
import com.example.exceptions.BadRequestException;
import com.example.mappers.UsuarioMapper;
import com.example.models.Order;
import com.example.models.Usuario;
import com.example.security.SecurityUtils;
import com.example.services.EmailService;
import com.example.services.OrderService;
import com.example.services.ReniecService;
import com.example.services.SunatService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping("/api/sunat")
public class SunatReniecController {

    private final SunatService sunatService;
    private final ReniecService reniecService;
    private final OrderService orderService;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;
    private final UsuarioMapper usuarioMapper;

    public SunatReniecController(SunatService sunatService,
                                 ReniecService reniecService,
                                 OrderService orderService,
                                 EmailService emailService,
                                 SecurityUtils securityUtils,
                                 UsuarioMapper usuarioMapper) {
        this.sunatService = sunatService;
        this.reniecService = reniecService;
        this.orderService = orderService;
        this.emailService = emailService;
        this.securityUtils = securityUtils;
        this.usuarioMapper = usuarioMapper;
    }

    /**
     * Endpoint público para consultar datos de un DNI de 8 dígitos en RENIEC
     * (accesible desde Registro público, Login, Carrito y Panel de Administración).
     */
    @GetMapping("/dni/{dni}")
    public ResponseEntity<ApiResponse<DniLookupDTO>> consultarDni(@PathVariable("dni") String dni) {
        DniLookupDTO resultado = reniecService.consultarDni(dni);
        return ResponseEntity.ok(ApiResponse.success(resultado, "Consulta RENIEC exitosa"));
    }

    /**
     * Guarda el DNI y la declaración de mayoría de edad (+18) y titularidad en el usuario autenticado.
     */
    @PostMapping("/configurar-dni")
    public ResponseEntity<ApiResponse<UsuarioDTO>> configurarDni(@RequestBody ConfigurarDniRequestDTO body) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Debe iniciar sesión para vincular su DNI."));
        }
        String dni = body != null && body.dni() != null ? body.dni().trim() : null;
        Boolean ageVerified = body != null && Boolean.TRUE.equals(body.ageVerified());
        String name = body != null && body.name() != null ? body.name().trim() : null;

        Usuario actualizado = reniecService.configurarDniUsuario(currentUserId.longValue(), dni, ageVerified, name);
        return ResponseEntity.ok(ApiResponse.success(
                usuarioMapper.toDto(actualizado),
                "DNI verificado y vinculado a tu cuenta para futuras Boletas Electrónicas."
        ));
    }

    /**
     * Emite (o consulta si ya existe) la Boleta de Venta Electrónica de un pedido y opcionalmente
     * la envía adjunta en PDF por correo electrónico al cliente.
     */
    @PostMapping("/boleta/order/{orderId}/emitir")
    public ResponseEntity<ApiResponse<BoletaEmisionDTO>> emitirBoletaPedido(
            @PathVariable("orderId") Integer orderId,
            @RequestBody(required = false) EmitirBoletaRequestDTO body) {

        Optional<Order> orderOpt = orderService.obtenerPorId(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Pedido no encontrado"));
        }
        Order order = orderOpt.get();
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !order.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("No tienes permiso para emitir la boleta de este pedido"));
            }
        }

        String dni = body != null && body.dni() != null ? body.dni().trim() : null;
        String nombre = body != null && body.nombre() != null ? body.nombre().trim() : null;
        boolean enviarCorreo = body == null || !Boolean.FALSE.equals(body.enviarCorreo());

        BoletaEmisionDTO result = sunatService.emitirBoletaParaPedido(orderId, dni, nombre);

        if (enviarCorreo && emailService != null) {
            try {
                emailService.sendBoletaEmailForOrder(orderId);
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok(ApiResponse.success(
                result,
                "Boleta de Venta Electrónica " + result.comprobante() + " emitida correctamente."
        ));
    }

    /**
     * Descarga o visualiza en el navegador el archivo PDF de la Boleta de Venta Electrónica de un pedido.
     */
    @GetMapping("/boleta/order/{orderId}/pdf")
    public ResponseEntity<byte[]> descargarPdfBoleta(
            @PathVariable("orderId") Integer orderId,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {

        Optional<Order> orderOpt = orderService.obtenerPorId(orderId);
        if (orderOpt.isEmpty()) {
            throw new BadRequestException("Pedido no encontrado");
        }
        Order order = orderOpt.get();
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !order.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        byte[] pdfBytes = sunatService.obtenerPdfBoleta(orderId);
        Order refreshed = orderService.obtenerPorId(orderId).orElse(order);
        String serie = refreshed.getBoletaSerie() != null ? refreshed.getBoletaSerie() : "BBB1";
        int num = refreshed.getBoletaNumero() != null ? refreshed.getBoletaNumero() : orderId;
        String filename = String.format("Boleta_%s-%06d.pdf", serie, num);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        headers.setContentDisposition(disposition);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
