package com.example.services;

import com.example.dtos.CheckoutRequestDTO;
import com.example.models.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        Integer addressId = processAddress(req);
        
        String promoCode = req.getOrder().getPromoCode();
        Promotion promotionAUsar = getValidPromotion(promoCode);
        BigDecimal discount = promotionAUsar != null ? 
            promotionService.calcularDescuento(promotionAUsar, req.getOrder().getSubtotal()) : BigDecimal.ZERO;
        
        Order savedOrder = initializeOrder(req, addressId, discount, promotionAUsar);
        
        BigDecimal realSubtotal = processItems(req, savedOrder.getId());
        
        savedOrder = finalizeOrderTotals(savedOrder, req, realSubtotal, promotionAUsar);
        
        processPayment(req, savedOrder);
        
        incrementPromotionUsage(promotionAUsar);

        return savedOrder;
    }

    private Integer processAddress(CheckoutRequestDTO req) {
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
        order.setDeliveryType(Order.DeliveryType.valueOf(req.getOrder().getDeliveryType().toUpperCase()));
        order.setPaymentMethodId(req.getOrder().getPaymentMethodId());
        order.setSubtotal(req.getOrder().getSubtotal());
        order.setDeliveryFee(req.getOrder().getDeliveryFee() != null ? req.getOrder().getDeliveryFee() : BigDecimal.ZERO);
        order.setDiscount(discount);
        order.setTotal(req.getOrder().getSubtotal().add(order.getDeliveryFee()).subtract(discount));
        order.setPromoCode(req.getOrder().getPromoCode());
        order.setPromotionId(promotionAUsar != null ? promotionAUsar.getId() : null);
        order.setNotes(req.getOrder().getNotes());
        order.setEstimatedDelivery(req.getOrder().getEstimatedDelivery() != null ? 
            req.getOrder().getEstimatedDelivery() : LocalDateTime.now().plusMinutes(45));

        return orderService.crearOrder(order);
    }

    private BigDecimal processItems(CheckoutRequestDTO req, Integer orderId) {
        BigDecimal realSubtotal = BigDecimal.ZERO;
        
        if (req.getItems() == null) return realSubtotal;

        for (CheckoutRequestDTO.CheckoutItemDTO itemDTO : req.getItems()) {
            if (itemDTO.getQuantity() != null && itemDTO.getQuantity() <= 0) {
                throw new IllegalArgumentException("Cantidad inválida. Debe ser mayor a cero.");
            }
            
            BigDecimal[] costs = calculateItemCosts(itemDTO);
            BigDecimal unitPrice = costs[0];
            BigDecimal sizeExtra = costs[1];
            BigDecimal extraIngredientsCost = costs[2];

            BigDecimal totalUnitPrice = unitPrice.add(sizeExtra).add(extraIngredientsCost);
            BigDecimal lineTotal = totalUnitPrice.multiply(new BigDecimal(itemDTO.getQuantity()));
            
            realSubtotal = realSubtotal.add(lineTotal);
            
            OrderItem savedItem = createOrderItem(orderId, itemDTO, totalUnitPrice, sizeExtra, lineTotal);
            
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
                        .orElseThrow(() -> new IllegalArgumentException("Tamaño no encontrado"));
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

    private OrderItem createOrderItem(Integer orderId, CheckoutRequestDTO.CheckoutItemDTO itemDTO, BigDecimal totalUnitPrice, BigDecimal sizeExtra, BigDecimal lineTotal) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
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

        return orderItemService.crearOrderItem(item);
    }

    private void processItemExtras(CheckoutRequestDTO.CheckoutItemDTO itemDTO, Integer savedItemId) {
        if (itemDTO.getExtraIngredientIds() != null && !itemDTO.getExtraIngredientIds().isEmpty()) {
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

    private void processPayment(CheckoutRequestDTO req, Order savedOrder) {
        if (req.getPayment() != null && req.getPayment().getPaymentMethodId() != null) {
            Payment payment = new Payment();
            payment.setOrderId(savedOrder.getId());
            payment.setAmount(req.getPayment().getAmount() != null ? req.getPayment().getAmount() : savedOrder.getTotal());
            payment.setPaymentMethodId(req.getPayment().getPaymentMethodId());
            payment.setStatus(Payment.PaymentStatus.valueOf(req.getPayment().getStatus() != null ? req.getPayment().getStatus().toUpperCase() : "PENDING"));
            payment.setTransactionId(req.getPayment().getTransactionId());
            paymentService.crearPayment(payment);
        }
    }

    private void incrementPromotionUsage(Promotion promotionAUsar) {
        if (promotionAUsar != null) {
            try {
                promotionService.incrementarUsoPromocion(promotionAUsar.getCode());
            } catch (Exception e) {
                logger.error("Error al incrementar uso de promoción", e);
            }
        }
    }
}
