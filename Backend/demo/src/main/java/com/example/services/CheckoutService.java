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

    public CheckoutService(OrderService orderService, OrderItemService orderItemService, AddressService addressService,
                           PaymentService paymentService, PromotionService promotionService,
                           OrderItemExtraService orderItemExtraService, IngredientService ingredientService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.addressService = addressService;
        this.paymentService = paymentService;
        this.promotionService = promotionService;
        this.orderItemExtraService = orderItemExtraService;
        this.ingredientService = ingredientService;
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

        // 4. Process Items
        if (req.getItems() != null) {
            for (CheckoutRequestDTO.CheckoutItemDTO itemDTO : req.getItems()) {
                if (itemDTO.getQuantity() != null && itemDTO.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Cantidad inválida. Debe ser mayor a cero.");
                }
                OrderItem item = new OrderItem();
                item.setOrderId(savedOrder.getId());
                item.setItemType(itemDTO.getItemType().toUpperCase());
                item.setQuantity(itemDTO.getQuantity());
                item.setUnitPrice(itemDTO.getUnitPrice());
                item.setSizeExtra(BigDecimal.ZERO);
                item.setLineTotal(itemDTO.getLineTotal());

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
                            OrderItemExtra extra = new OrderItemExtra();
                            extra.setOrderItemId(savedItem.getId());
                            extra.setIngredientId(ingredient.getId());
                            extra.setIngredientName(ingredient.getName());
                            extra.setExtraCost(ingredient.getExtraCost() != null ? ingredient.getExtraCost() : BigDecimal.ZERO);
                            orderItemExtraService.crearOrderItemExtra(extra);
                        });
                    }
                }
            }
        }

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
