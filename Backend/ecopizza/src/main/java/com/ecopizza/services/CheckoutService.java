package com.ecopizza.services;

import com.ecopizza.dtos.CheckoutRequestDTO;
import com.ecopizza.dtos.MercadoPagoChargeResultDTO;
import com.ecopizza.models.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final AddressService addressService;
    private final PaymentService paymentService;
    private final PromotionService promotionService;
    private final OrderItemExtraService orderItemExtraService;
    private final IngredientService ingredientService;
    private final PizzaService pizzaService;
    private final ExtraService extraService;
    private final SizeService sizeService;
    private final InventoryService inventoryService;
    private final SunatService sunatService;
    private final MercadoPagoService mercadoPagoService;
    private final StoreSettingsService storeSettingsService;

    public CheckoutService(OrderService orderService, OrderItemService orderItemService, AddressService addressService,
                           PaymentService paymentService, PromotionService promotionService,
                           OrderItemExtraService orderItemExtraService, IngredientService ingredientService,
                           PizzaService pizzaService, ExtraService extraService, SizeService sizeService,
                           InventoryService inventoryService,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) SunatService sunatService,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) MercadoPagoService mercadoPagoService,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) StoreSettingsService storeSettingsService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.addressService = addressService;
        this.paymentService = paymentService;
        this.promotionService = promotionService;
        this.orderItemExtraService = orderItemExtraService;
        this.ingredientService = ingredientService;
        this.pizzaService = pizzaService;
        this.extraService = extraService;
        this.sizeService = sizeService;
        this.inventoryService = inventoryService;
        this.sunatService = sunatService;
        this.mercadoPagoService = mercadoPagoService;
        this.storeSettingsService = storeSettingsService;
    }

    public Order checkout(CheckoutRequestDTO req) {
        Integer addressId = processAddress(req);

        String promoCode = req.getOrder().getPromoCode();
        Promotion promotionAUsar = getValidPromotion(promoCode);
        BigDecimal discount = promotionAUsar != null ?
            promotionService.calcularDescuento(promotionAUsar, req.getOrder().getSubtotal()) : BigDecimal.ZERO;

        Order savedOrder = initializeOrder(req, addressId, discount, promotionAUsar);

        BigDecimal realSubtotal = processItems(req, savedOrder.getId());

        savedOrder = finalizeOrderTotals(savedOrder, req, realSubtotal, promotionAUsar);

        Payment.PaymentStatus resolvedPaymentStatus = processPayment(req, savedOrder);

        incrementPromotionUsage(promotionAUsar);

        boolean wantsBoleta = Boolean.TRUE.equals(req.getWantsBoleta())
                || (req.getOrder() != null && Boolean.TRUE.equals(req.getOrder().getWantsBoleta()));
        if (wantsBoleta && sunatService != null) {
            boolean isFactura = Integer.valueOf(1).equals(req.getTipoComprobante())
                    || (req.getOrder() != null && Integer.valueOf(1).equals(req.getOrder().getBoletaTipoComprobante()))
                    || (req.getClienteRuc() != null && !req.getClienteRuc().isBlank());
            String reqNombre = req.getClienteNombre() != null && !req.getClienteNombre().isBlank()
                    ? req.getClienteNombre().trim()
                    : (req.getOrder() != null ? req.getOrder().getBoletaClienteNombre() : null);

            if (isFactura) {
                String reqRuc = req.getClienteRuc() != null && !req.getClienteRuc().isBlank()
                        ? req.getClienteRuc().trim()
                        : (req.getClienteDni() != null && !req.getClienteDni().isBlank()
                            ? req.getClienteDni().trim()
                            : (req.getOrder() != null ? req.getOrder().getBoletaClienteDni() : null));
                String reqDireccion = req.getClienteDireccion() != null && !req.getClienteDireccion().isBlank()
                        ? req.getClienteDireccion().trim()
                        : (req.getOrder() != null ? req.getOrder().getBoletaClienteDireccion() : null);
                Boolean rucVerified = Boolean.TRUE.equals(req.getRucVerified());
                if (resolvedPaymentStatus == Payment.PaymentStatus.PAID) {
                    sunatService.emitirComprobanteParaPedido(
                            savedOrder.getId(), 1, reqRuc, reqNombre, reqDireccion, rucVerified);
                } else {
                    sunatService.registrarSolicitudComprobantePendiente(
                            savedOrder.getId(), 1, reqRuc, reqNombre, reqDireccion, rucVerified);
                }
            } else {
                String reqDni = req.getClienteDni() != null && !req.getClienteDni().isBlank()
                        ? req.getClienteDni().trim()
                        : (req.getOrder() != null ? req.getOrder().getBoletaClienteDni() : null);
                if (resolvedPaymentStatus == Payment.PaymentStatus.PAID) {
                    sunatService.emitirBoletaParaPedido(savedOrder.getId(), reqDni, reqNombre);
                } else {
                    sunatService.registrarSolicitudBoletaPendiente(savedOrder.getId(), reqDni, reqNombre);
                }
            }
            savedOrder = orderService.obtenerPorId(savedOrder.getId()).orElse(savedOrder);
        }

        return savedOrder;
    }

    private Integer processAddress(CheckoutRequestDTO req) {
        if (!"DELIVERY".equalsIgnoreCase(req.getOrder().getDeliveryType())) {
            return null;
        }
        Integer addressId = req.getOrder().getAddressId();
        Integer userId = req.getOrder().getUserId();

        // Si el cliente seleccionó un addressId existente que le pertenece, reutilizarlo sin crear filas duplicadas
        if (addressId != null) {
            var existingOpt = addressService.obtenerPorId(addressId);
            if (existingOpt.isPresent()) {
                Address existing = existingOpt.get();
                if (userId == null || userId.equals(existing.getUserId())) {
                    if (req.getAddress() != null && req.getAddress().getReference() != null
                            && !req.getAddress().getReference().isBlank()
                            && !req.getAddress().getReference().trim().equals(existing.getReference())) {
                        existing.setReference(req.getAddress().getReference().trim());
                        addressService.actualizarAddress(existing.getId(), existing);
                    }
                    return existing.getId();
                }
            }
        }

        if (req.getAddress() != null) {
            Address address = new Address();
            address.setUserId(userId);
            address.setLine1(req.getAddress().getLine1());
            address.setCity(req.getAddress().getCity());
            address.setDistrict(req.getAddress().getDistrict());
            address.setReference(req.getAddress().getReference());
            address.setIsDefault(req.getAddress().getIsDefault() != null ? req.getAddress().getIsDefault() : false);
            // crearAddress ahora deduplica automáticamente por (userId, line1, city, district)
            Address savedAddress = addressService.crearAddress(address);
            return savedAddress.getId();
        }
        return addressId;
    }

    private Promotion getValidPromotion(String promoCode) {
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            return promotionService.obtenerPorCodigo(promoCode).orElse(null);
        }
        return null;
    }

    private Order initializeOrder(CheckoutRequestDTO req, Integer addressId, BigDecimal discount, Promotion promotionAUsar) {
        Order order = new Order();
        order.setUserId(req.getOrder().getUserId());
        order.setAddressId(addressId);
        order.setStatus(Order.OrderStatus.PENDING);
        Order.DeliveryType deliveryType = Order.DeliveryType.valueOf(req.getOrder().getDeliveryType().toUpperCase());
        order.setDeliveryType(deliveryType);
        order.setPaymentMethodId(req.getOrder().getPaymentMethodId());
        order.setSubtotal(req.getOrder().getSubtotal());
        BigDecimal resolvedDeliveryFee;
        if (deliveryType == Order.DeliveryType.PICKUP) {
            resolvedDeliveryFee = BigDecimal.ZERO;
        } else if (storeSettingsService != null) {
            resolvedDeliveryFee = storeSettingsService.getDeliveryFee();
        } else {
            resolvedDeliveryFee = req.getOrder().getDeliveryFee() != null ? req.getOrder().getDeliveryFee() : BigDecimal.ZERO;
        }
        order.setDeliveryFee(resolvedDeliveryFee);
        order.setDiscount(discount);
        order.setTotal(req.getOrder().getSubtotal().add(order.getDeliveryFee()).subtract(discount));
        order.setPromoCode(req.getOrder().getPromoCode());
        order.setPromotionId(promotionAUsar != null ? promotionAUsar.getId() : null);
        order.setNotes(req.getOrder().getNotes());
        order.setStockSettled("DEDUCTED");
        order.setEstimatedDelivery(req.getOrder().getEstimatedDelivery() != null ?
            req.getOrder().getEstimatedDelivery() : LocalDateTime.now().plusMinutes(45));

        return orderService.crearOrder(order);
    }

    private BigDecimal processItems(CheckoutRequestDTO req, Integer orderId) {
        BigDecimal realSubtotal = BigDecimal.ZERO;

        if (req.getItems() == null) return realSubtotal;

        for (CheckoutRequestDTO.CheckoutItemDTO itemDTO : req.getItems()) {
            if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                throw new IllegalArgumentException("Cantidad inv\u00e1lida. Debe ser mayor a cero.");
            }
        }

        List<BigDecimal> unitDirectCosts = inventoryService != null
                ? inventoryService.deductCheckoutStockAndCalculateUnitCosts(
                        req.getItems(), orderId, req.getOrder() != null ? req.getOrder().getUserId() : null)
                : Collections.emptyList();

        for (int idx = 0; idx < req.getItems().size(); idx++) {
            CheckoutRequestDTO.CheckoutItemDTO itemDTO = req.getItems().get(idx);

            BigDecimal[] costs = calculateItemCosts(itemDTO);
            BigDecimal unitPrice = costs[0];
            BigDecimal sizeExtra = costs[1];
            BigDecimal extraIngredientsCost = costs[2];

            BigDecimal totalUnitPrice = unitPrice.add(sizeExtra).add(extraIngredientsCost);
            BigDecimal lineTotal = totalUnitPrice.multiply(new BigDecimal(itemDTO.getQuantity()));

            BigDecimal directUnitCost = (unitDirectCosts != null && idx < unitDirectCosts.size() && unitDirectCosts.get(idx) != null)
                    ? unitDirectCosts.get(idx)
                    : BigDecimal.ZERO;
            BigDecimal directLineCost = directUnitCost.multiply(BigDecimal.valueOf(itemDTO.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            realSubtotal = realSubtotal.add(lineTotal);

            OrderItem savedItem = createOrderItem(orderId, itemDTO, totalUnitPrice, sizeExtra, lineTotal, directUnitCost, directLineCost);

            processItemExtras(itemDTO, savedItem.getId());
        }
        return realSubtotal;
    }

    private BigDecimal[] calculateItemCosts(CheckoutRequestDTO.CheckoutItemDTO itemDTO) {
        BigDecimal unitPrice = BigDecimal.ZERO;
        BigDecimal sizeExtra = BigDecimal.ZERO;
        BigDecimal extraIngredientsCost = BigDecimal.ZERO;

        if ("PIZZA".equalsIgnoreCase(itemDTO.getItemType())) {
            Pizza pizza = pizzaService.obtenerPorId(itemDTO.getPizzaId())
                    .orElseThrow(() -> new IllegalArgumentException("Pizza no encontrada"));
            unitPrice = pizza.getPrice();

            if (itemDTO.getSizeId() != null) {
                Size size = sizeService.obtenerPorId(itemDTO.getSizeId())
                        .orElseThrow(() -> new IllegalArgumentException("Tama\u00f1o no encontrado"));
                sizeExtra = size.getExtraCost();
            }
        } else if ("EXTRA".equalsIgnoreCase(itemDTO.getItemType())) {
            Extra extra = extraService.obtenerPorId(itemDTO.getExtraId())
                    .orElseThrow(() -> new IllegalArgumentException("Extra no encontrado"));
            unitPrice = extra.getPrice();
        }

        if (itemDTO.getExtraIngredientIds() != null && !itemDTO.getExtraIngredientIds().isEmpty()) {
            for (Integer ingId : itemDTO.getExtraIngredientIds()) {
                Ingredient ingredient = ingredientService.obtenerPorId(ingId).orElse(null);
                if (ingredient != null && ingredient.getExtraCost() != null) {
                    extraIngredientsCost = extraIngredientsCost.add(ingredient.getExtraCost());
                }
            }
        }
        return new BigDecimal[]{unitPrice, sizeExtra, extraIngredientsCost};
    }

    private OrderItem createOrderItem(Integer orderId, CheckoutRequestDTO.CheckoutItemDTO itemDTO,
                                      BigDecimal totalUnitPrice, BigDecimal sizeExtra, BigDecimal lineTotal,
                                      BigDecimal directUnitCost, BigDecimal directLineCost) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setItemType(itemDTO.getItemType().toUpperCase());
        item.setQuantity(itemDTO.getQuantity());
        item.setUnitPrice(totalUnitPrice);
        item.setSizeExtra(sizeExtra);
        item.setLineTotal(lineTotal);
        item.setUnitCost(directUnitCost);
        item.setLineCost(directLineCost);

        if ("PIZZA".equalsIgnoreCase(itemDTO.getItemType())) {
            item.setPizzaId(itemDTO.getPizzaId());
            item.setSizeId(itemDTO.getSizeId());
        } else if ("EXTRA".equalsIgnoreCase(itemDTO.getItemType())) {
            item.setExtraId(itemDTO.getExtraId());
        }

        return orderItemService.crearOrderItem(item);
    }

    private void processItemExtras(CheckoutRequestDTO.CheckoutItemDTO itemDTO, Integer savedItemId) {
        if (reqHasExtras(itemDTO)) {
            for (Integer ingId : itemDTO.getExtraIngredientIds()) {
                ingredientService.obtenerPorId(ingId).ifPresent(ingredient -> {
                    OrderItemExtra orderItemExtra = new OrderItemExtra();
                    orderItemExtra.setOrderItemId(savedItemId);
                    orderItemExtra.setIngredientId(ingredient.getId());
                    orderItemExtra.setIngredientName(ingredient.getName());
                    orderItemExtra.setExtraCost(ingredient.getExtraCost() != null ? ingredient.getExtraCost() : BigDecimal.ZERO);
                    orderItemExtraService.crearOrderItemExtra(orderItemExtra);
                });
            }
        }
    }

    private boolean reqHasExtras(CheckoutRequestDTO.CheckoutItemDTO itemDTO) {
        return itemDTO.getExtraIngredientIds() != null && !itemDTO.getExtraIngredientIds().isEmpty();
    }

    private Order finalizeOrderTotals(Order savedOrder, CheckoutRequestDTO req, BigDecimal realSubtotal, Promotion promotionAUsar) {
        BigDecimal discount = BigDecimal.ZERO;
        String promoCode = req.getOrder().getPromoCode();
        if (promoCode != null && !promoCode.trim().isEmpty() && promotionAUsar != null) {
            promotionService.verificarReglasDeUsuario(promotionAUsar, req.getOrder().getUserId());
            discount = promotionService.calcularDescuento(promotionAUsar, realSubtotal);
        }

        savedOrder.setSubtotal(realSubtotal);
        savedOrder.setDiscount(discount);
        BigDecimal calculatedTotal = realSubtotal.add(savedOrder.getDeliveryFee()).subtract(discount);
        savedOrder.setTotal(calculatedTotal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : calculatedTotal);
        return orderService.actualizarOrder(savedOrder.getId(), savedOrder);
    }

    private Payment.PaymentStatus processPayment(CheckoutRequestDTO req, Order savedOrder) {
        if (req.getPayment() != null && req.getPayment().getPaymentMethodId() != null) {
            CheckoutRequestDTO.PaymentDTO payReq = req.getPayment();
            Payment.PaymentStatus resolvedStatus = Payment.PaymentStatus.valueOf(
                    payReq.getStatus() != null ? payReq.getStatus().toUpperCase() : "PENDING");
            String resolvedTxId = payReq.getTransactionId();

            // Si Mercado Pago está configurado y el medio elegido es Tarjeta o Yape, procesar cobro real contra Mercado Pago.
            // Si Mercado Pago rechaza el cobro, lanza BadRequestException y @Transactional revierte pedido + stock (y no emite boleta SUNAT).
            if (mercadoPagoService != null
                    && mercadoPagoService.requiresGatewayProcessing(payReq.getPaymentMethodId(), payReq)) {
                MercadoPagoChargeResultDTO mpResult = mercadoPagoService.procesarPago(savedOrder, payReq);
                if (mpResult.processedViaMercadoPago()) {
                    resolvedStatus = Payment.PaymentStatus.PAID;
                    resolvedTxId = mpResult.transactionId();
                    savedOrder.setStatus(Order.OrderStatus.CONFIRMED);
                    orderService.actualizarOrder(savedOrder.getId(), savedOrder);
                }
            }

            Payment payment = new Payment();
            payment.setOrderId(savedOrder.getId());
            payment.setAmount(savedOrder.getTotal());
            payment.setPaymentMethodId(payReq.getPaymentMethodId());
            payment.setStatus(resolvedStatus);
            payment.setTransactionId(resolvedTxId);
            paymentService.crearPayment(payment);
            return resolvedStatus;
        }
        return Payment.PaymentStatus.PENDING;
    }

    private void incrementPromotionUsage(Promotion promotionAUsar) {
        if (promotionAUsar != null) {
            try {
                promotionService.incrementarUsoPromocion(promotionAUsar.getCode());
            } catch (Exception ex) {
                logger.error("Error al incrementar uso de promoci\u00f3n", ex);
            }
        }
    }
}

