package com.example.services;

import com.example.dtos.CheckoutRequestDTO;
import com.example.models.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
public class CheckoutService {

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

    public CheckoutService(OrderService orderService, OrderItemService orderItemService, AddressService addressService,
                           PaymentService paymentService, PromotionService promotionService,
                           OrderItemExtraService orderItemExtraService, IngredientService ingredientService,
                           PizzaService pizzaService, ExtraService extraService, SizeService sizeService) {
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
    }

    public Order checkout(CheckoutRequestDTO req) {
        // 1. Process Address
        Integer addressId = req.getOrder().getAddressId();
        if (req.getAddress() != null && "DELIVERY".equalsIgnoreCase(req.getOrder().getDeliveryType())) {
            Address address = new Address();
            address.setUserId(req.getOrder().getUserId());
            address.setLine1(req.getAddress().getLine1());
            address.setCity(req.getAddress().getCity());
            address.setDistrict(req.getAddress().getDistrict());
            address.setReference(req.getAddress().getReference());
            address.setIsDefault(req.getAddress().getIsDefault() != null ? req.getAddress().getIsDefault() : false);
            Address savedAddress = addressService.crearAddress(address);
            addressId = savedAddress.getId();
        }

        // 2. Process Promotion
        BigDecimal discount = BigDecimal.ZERO;
        String promoCode = req.getOrder().getPromoCode();
        Integer promotionId = null;
        Promotion promotionAUsar = null;

        if (promoCode != null && !promoCode.trim().isEmpty()) {
            var promotionOpt = promotionService.obtenerPorCodigo(promoCode);
            if (promotionOpt.isPresent()) {
                promotionAUsar = promotionOpt.get();
                discount = promotionService.calcularDescuento(promotionAUsar, req.getOrder().getSubtotal());
                promotionId = promotionAUsar.getId();
            }
        }

        // 3. Process Order
        Order order = new Order();
        order.setUserId(req.getOrder().getUserId());
        order.setAddressId(addressId);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setDeliveryType(Order.DeliveryType.valueOf(req.getOrder().getDeliveryType().toUpperCase()));
        order.setPaymentMethodId(req.getOrder().getPaymentMethodId());
        order.setSubtotal(req.getOrder().getSubtotal());
        order.setDeliveryFee(req.getOrder().getDeliveryFee() != null ? req.getOrder().getDeliveryFee() : BigDecimal.ZERO);
        order.setDiscount(discount);
        order.setTotal(req.getOrder().getSubtotal().add(order.getDeliveryFee()).subtract(discount));
        order.setPromoCode(promoCode);
        order.setPromotionId(promotionId);
        order.setNotes(req.getOrder().getNotes());
        order.setEstimatedDelivery(req.getOrder().getEstimatedDelivery() != null ? 
            req.getOrder().getEstimatedDelivery() : LocalDateTime.now().plusMinutes(45));

        Order savedOrder = orderService.crearOrder(order);

        // 4. Process Items and Calculate Subtotal
        BigDecimal realSubtotal = BigDecimal.ZERO;
        
        if (req.getItems() != null) {
            for (CheckoutRequestDTO.CheckoutItemDTO itemDTO : req.getItems()) {
                if (itemDTO.getQuantity() != null && itemDTO.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Cantidad inválida. Debe ser mayor a cero.");
                }
                
                BigDecimal unitPrice = BigDecimal.ZERO;
                BigDecimal sizeExtra = BigDecimal.ZERO;
                BigDecimal extraIngredientsCost = BigDecimal.ZERO;
                
                if ("PIZZA".equalsIgnoreCase(itemDTO.getItemType())) {
                    Pizza pizza = pizzaService.obtenerPorId(itemDTO.getPizzaId())
                            .orElseThrow(() -> new IllegalArgumentException("Pizza no encontrada"));
                    unitPrice = pizza.getPrice();
                    
                    if (itemDTO.getSizeId() != null) {
                        Size size = sizeService.obtenerPorId(itemDTO.getSizeId())
                                .orElseThrow(() -> new IllegalArgumentException("Tamaño no encontrado"));
                        sizeExtra = size.getExtraCost(); // Asumiendo que es un extra fijo, o multiplicar. En este MVP parece sumarse
                    }
                } else if ("EXTRA".equalsIgnoreCase(itemDTO.getItemType())) {
                    Extra extra = extraService.obtenerPorId(itemDTO.getExtraId())
                            .orElseThrow(() -> new IllegalArgumentException("Extra no encontrado"));
                    unitPrice = extra.getPrice();
                }

                // Calcular costo de ingredientes extra
                if (itemDTO.getExtraIngredientIds() != null && !itemDTO.getExtraIngredientIds().isEmpty()) {
                    for (Integer ingId : itemDTO.getExtraIngredientIds()) {
                        Ingredient ingredient = ingredientService.obtenerPorId(ingId).orElse(null);
                        if (ingredient != null && ingredient.getExtraCost() != null) {
                            extraIngredientsCost = extraIngredientsCost.add(ingredient.getExtraCost());
                        }
                    }
                }

                // El precio total por unidad es (precioBase + extraPorTamaño + costoIngredientesExtra)
                BigDecimal totalUnitPrice = unitPrice.add(sizeExtra).add(extraIngredientsCost);
                BigDecimal lineTotal = totalUnitPrice.multiply(new BigDecimal(itemDTO.getQuantity()));
                
                realSubtotal = realSubtotal.add(lineTotal);
                
                OrderItem item = new OrderItem();
                item.setOrderId(savedOrder.getId());
                item.setItemType(itemDTO.getItemType().toUpperCase());
                item.setQuantity(itemDTO.getQuantity());
                item.setUnitPrice(totalUnitPrice);
                item.setSizeExtra(sizeExtra);
                item.setLineTotal(lineTotal);

                if ("PIZZA".equalsIgnoreCase(itemDTO.getItemType())) {
                    item.setPizzaId(itemDTO.getPizzaId());
                    item.setSizeId(itemDTO.getSizeId());
                } else if ("EXTRA".equalsIgnoreCase(itemDTO.getItemType())) {
                    item.setExtraId(itemDTO.getExtraId());
                }

                OrderItem savedItem = orderItemService.crearOrderItem(item);

                // Process Extras for Pizza
                if (itemDTO.getExtraIngredientIds() != null && !itemDTO.getExtraIngredientIds().isEmpty()) {
                    for (Integer ingId : itemDTO.getExtraIngredientIds()) {
                        ingredientService.obtenerPorId(ingId).ifPresent(ingredient -> {
                            OrderItemExtra orderItemExtra = new OrderItemExtra();
                            orderItemExtra.setOrderItemId(savedItem.getId());
                            orderItemExtra.setIngredientId(ingredient.getId());
                            orderItemExtra.setIngredientName(ingredient.getName());
                            orderItemExtra.setExtraCost(ingredient.getExtraCost() != null ? ingredient.getExtraCost() : BigDecimal.ZERO);
                            orderItemExtraService.crearOrderItemExtra(orderItemExtra);
                        });
                    }
                }
            }
        }
        
        // 5. Re-calculate Promos and Order Totals with REAL subtotal
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            if (promotionAUsar != null) {
                // Validación estricta de negocio: ¿Este usuario tiene derecho a usar este cupón?
                promotionService.verificarReglasDeUsuario(promotionAUsar, req.getOrder().getUserId());
                
                discount = promotionService.calcularDescuento(promotionAUsar, realSubtotal);
            }
        }
        
        savedOrder.setSubtotal(realSubtotal);
        savedOrder.setDiscount(discount);
        BigDecimal calculatedTotal = realSubtotal.add(savedOrder.getDeliveryFee()).subtract(discount);
        savedOrder.setTotal(calculatedTotal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : calculatedTotal);
        orderService.actualizarOrder(savedOrder.getId(), savedOrder);

        // 5. Process Payment
        if (req.getPayment() != null && req.getPayment().getPaymentMethodId() != null) {
            Payment payment = new Payment();
            payment.setOrderId(savedOrder.getId());
            payment.setAmount(req.getPayment().getAmount() != null ? req.getPayment().getAmount() : savedOrder.getTotal());
            payment.setPaymentMethodId(req.getPayment().getPaymentMethodId());
            payment.setStatus(Payment.PaymentStatus.valueOf(req.getPayment().getStatus() != null ? req.getPayment().getStatus().toUpperCase() : "PENDING"));
            payment.setTransactionId(req.getPayment().getTransactionId());
            paymentService.crearPayment(payment);
        }

        // 6. Increment Promotion Usage
        if (promotionAUsar != null) {
            try {
                promotionService.incrementarUsoPromocion(promotionAUsar);
            } catch (Exception e) {
                System.err.println("Error al incrementar uso de promoción: " + e.getMessage());
            }
        }

        return savedOrder;
    }
}
