package com.example.services;

import com.example.dtos.ProductLossCreateDTO;
import com.example.dtos.ProductLossDTO;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;
import com.example.models.Extra;
import com.example.models.Ingredient;
import com.example.models.Order;
import com.example.models.OrderItem;
import com.example.models.Pizza;
import com.example.models.PizzaIngredient;
import com.example.models.ProductLoss;
import com.example.models.Size;
import com.example.models.StockMovement;
import com.example.repositories.ExtraRepository;
import com.example.repositories.IngredientRepository;
import com.example.repositories.OrderItemRepository;
import com.example.repositories.PizzaIngredientRepository;
import com.example.repositories.PizzaRepository;
import com.example.repositories.ProductLossRepository;
import com.example.repositories.SizeRepository;
import com.example.repositories.StockMovementRepository;
import com.example.repositories.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductLossService {

    private final ProductLossRepository productLossRepository;
    private final IngredientRepository ingredientRepository;
    private final ExtraRepository extraRepository;
    private final PizzaRepository pizzaRepository;
    private final PizzaIngredientRepository pizzaIngredientRepository;
    private final SizeRepository sizeRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventoryService inventoryService;

    public ProductLossService(ProductLossRepository productLossRepository,
                              IngredientRepository ingredientRepository,
                              ExtraRepository extraRepository,
                              PizzaRepository pizzaRepository,
                              PizzaIngredientRepository pizzaIngredientRepository,
                              SizeRepository sizeRepository,
                              OrderItemRepository orderItemRepository,
                              StockMovementRepository stockMovementRepository,
                              UsuarioRepository usuarioRepository,
                              InventoryService inventoryService) {
        this.productLossRepository = productLossRepository;
        this.ingredientRepository = ingredientRepository;
        this.extraRepository = extraRepository;
        this.pizzaRepository = pizzaRepository;
        this.pizzaIngredientRepository = pizzaIngredientRepository;
        this.sizeRepository = sizeRepository;
        this.orderItemRepository = orderItemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<ProductLossDTO> listLosses(LocalDate startDate, LocalDate endDate, String category, String itemType) {
        List<ProductLoss> losses;
        if (startDate != null && endDate != null) {
            losses = productLossRepository.findByLossDateBetweenOrderByLossDateDesc(
                    startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        } else {
            losses = productLossRepository.findAllByOrderByLossDateDesc();
        }

        Map<Integer, String> userMap = usuarioRepository.findAll().stream()
                .collect(Collectors.toMap(u -> u.getId().intValue(),
                        u -> u.getName() != null && !u.getName().isBlank() ? u.getName() : u.getEmail(),
                        (a, b) -> a));

        return losses.stream()
                .filter(l -> category == null || category.isBlank() || l.getCategory().name().equalsIgnoreCase(category))
                .filter(l -> itemType == null || itemType.isBlank() || l.getItemType().name().equalsIgnoreCase(itemType))
                .map(l -> {
                    ProductLossDTO dto = new ProductLossDTO(l);
                    if (l.getReportedByUserId() != null) {
                        dto.setReportedByName(userMap.getOrDefault(l.getReportedByUserId(), "Usuario #" + l.getReportedByUserId()));
                    }
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLossesSummary(LocalDate startDate, LocalDate endDate) {
        List<ProductLossDTO> list = listLosses(startDate, endDate, null, null);
        BigDecimal totalLossCost = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (ProductLoss.LossCategory cat : ProductLoss.LossCategory.values()) {
            byCategory.put(cat.name(), BigDecimal.ZERO);
        }
        Map<String, BigDecimal> byItemType = new LinkedHashMap<>();
        for (ProductLoss.ItemType type : ProductLoss.ItemType.values()) {
            byItemType.put(type.name(), BigDecimal.ZERO);
        }

        for (ProductLossDTO dto : list) {
            BigDecimal cost = dto.getTotalLossCost() != null ? dto.getTotalLossCost() : BigDecimal.ZERO;
            totalLossCost = totalLossCost.add(cost);
            if (dto.getCategory() != null) {
                byCategory.merge(dto.getCategory(), cost, BigDecimal::add);
            }
            if (dto.getItemType() != null) {
                byItemType.merge(dto.getItemType(), cost, BigDecimal::add);
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCount", list.size());
        summary.put("totalLossCost", totalLossCost.setScale(2, RoundingMode.HALF_UP));
        summary.put("byCategory", byCategory);
        summary.put("byItemType", byItemType);
        return summary;
    }

    public ProductLossDTO createLoss(ProductLossCreateDTO req, Integer userId) {
        if (req == null || req.getItemType() == null || req.getCategory() == null || req.getQuantity() == null) {
            throw new BadRequestException("Debe especificar itemType, category y quantity.");
        }
        if (req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("La cantidad de pérdida debe ser mayor a cero.");
        }

        ProductLoss.ItemType itemType = ProductLoss.ItemType.valueOf(req.getItemType().toUpperCase());
        ProductLoss.LossCategory category = ProductLoss.LossCategory.valueOf(req.getCategory().toUpperCase());
        boolean shouldDeduct = req.getDeductStock() == null || req.getDeductStock();

        ProductLoss loss = new ProductLoss();
        loss.setItemType(itemType);
        loss.setCategory(category);
        loss.setReason(req.getReason() != null ? req.getReason().trim() : null);
        loss.setReportedByUserId(userId);
        loss.setLossDate(req.getLossDate() != null ? req.getLossDate() : LocalDateTime.now());

        if (itemType == ProductLoss.ItemType.INGREDIENT) {
            if (req.getIngredientId() == null) {
                throw new BadRequestException("Debe especificar ingredientId para registrar merma de ingrediente.");
            }
            Ingredient ing = ingredientRepository.findById(req.getIngredientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado"));
            BigDecimal qty = req.getQuantity().setScale(2, RoundingMode.HALF_UP);
            BigDecimal uCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
            BigDecimal totalCost = qty.multiply(uCost).setScale(2, RoundingMode.HALF_UP);

            if (shouldDeduct) {
                BigDecimal prev = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
                if (prev.compareTo(qty) < 0) {
                    throw new BadRequestException(String.format(
                            "El stock actual de '%s' (%.2f %s) es menor que la merma reportada (%.2f %s).",
                            ing.getName(), prev, ing.getUnit(), qty, ing.getUnit()));
                }
                BigDecimal next = prev.subtract(qty).setScale(2, RoundingMode.HALF_UP);
                ing.setStockQuantity(next);
                ingredientRepository.save(ing);

                loss.setIngredientId(ing.getId());
                loss.setItemName(ing.getName());
                loss.setUnit(ing.getUnit() != null ? ing.getUnit() : "g");
                loss.setQuantity(qty);
                loss.setUnitCost(uCost);
                loss.setTotalLossCost(totalCost);
                ProductLoss saved = productLossRepository.save(loss);

                recordLossMovement(StockMovement.ItemType.INGREDIENT, ing.getId(), null, null, qty, prev, next, uCost, totalCost,
                        saved.getId(), loss.getReason(), userId);
                inventoryService.evictCatalogCaches();
                return enrichLoss(saved);
            } else {
                loss.setIngredientId(ing.getId());
                loss.setItemName(ing.getName());
                loss.setUnit(ing.getUnit() != null ? ing.getUnit() : "g");
                loss.setQuantity(qty);
                loss.setUnitCost(uCost);
                loss.setTotalLossCost(totalCost);
                return enrichLoss(productLossRepository.save(loss));
            }
        } else if (itemType == ProductLoss.ItemType.EXTRA) {
            if (req.getExtraId() == null) {
                throw new BadRequestException("Debe especificar extraId para registrar merma de extra.");
            }
            Extra ext = extraRepository.findById(req.getExtraId())
                    .orElseThrow(() -> new ResourceNotFoundException("Extra no encontrado"));
            int units = req.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue();
            if (units <= 0) {
                throw new BadRequestException("La cantidad para un extra debe ser al menos 1 unidad.");
            }
            BigDecimal uCost = ext.getUnitCost() != null ? ext.getUnitCost() : BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.valueOf(units).multiply(uCost).setScale(2, RoundingMode.HALF_UP);

            if (shouldDeduct) {
                int prev = ext.getStockQuantity() != null ? ext.getStockQuantity() : 0;
                if (prev < units) {
                    throw new BadRequestException(String.format(
                            "El stock actual de '%s' (%d und) es menor que la merma reportada (%d und).",
                            ext.getName(), prev, units));
                }
                int next = prev - units;
                ext.setStockQuantity(next);
                extraRepository.save(ext);

                loss.setExtraId(ext.getId());
                loss.setItemName(ext.getName());
                loss.setUnit("und");
                loss.setQuantity(BigDecimal.valueOf(units));
                loss.setUnitCost(uCost);
                loss.setTotalLossCost(totalCost);
                ProductLoss saved = productLossRepository.save(loss);

                recordLossMovement(StockMovement.ItemType.EXTRA, null, ext.getId(), null, BigDecimal.valueOf(units),
                        BigDecimal.valueOf(prev), BigDecimal.valueOf(next), uCost, totalCost,
                        saved.getId(), loss.getReason(), userId);
                inventoryService.evictCatalogCaches();
                return enrichLoss(saved);
            } else {
                loss.setExtraId(ext.getId());
                loss.setItemName(ext.getName());
                loss.setUnit("und");
                loss.setQuantity(BigDecimal.valueOf(units));
                loss.setUnitCost(uCost);
                loss.setTotalLossCost(totalCost);
                return enrichLoss(productLossRepository.save(loss));
            }
        } else if (itemType == ProductLoss.ItemType.SIZE) {
            if (req.getSizeId() == null) {
                throw new BadRequestException("Debe especificar sizeId para registrar merma de masa.");
            }
            Size size = sizeRepository.findById(req.getSizeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tamaño no encontrado"));
            int units = req.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue();
            if (units <= 0) {
                throw new BadRequestException("La cantidad para masa debe ser al menos 1 unidad.");
            }
            BigDecimal uCost = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;

            if (shouldDeduct) {
                int prev = size.getStockQuantity() != null ? size.getStockQuantity() : 0;
                if (prev < units) {
                    throw new BadRequestException(String.format(
                            "El stock actual de masa '%s' (%d und) es menor que la merma reportada (%d und).",
                            size.getName(), prev, units));
                }
                int next = prev - units;
                size.setStockQuantity(next);
                sizeRepository.save(size);

                loss.setSizeId(size.getId());
                loss.setItemName("Masa " + size.getName());
                loss.setUnit("und");
                loss.setQuantity(BigDecimal.valueOf(units));
                loss.setUnitCost(uCost);
                loss.setTotalLossCost(totalCost);
                ProductLoss saved = productLossRepository.save(loss);

                recordLossMovement(StockMovement.ItemType.SIZE, null, null, size.getId(), BigDecimal.valueOf(units),
                        BigDecimal.valueOf(prev), BigDecimal.valueOf(next), uCost, totalCost,
                        saved.getId(), loss.getReason(), userId);
                inventoryService.evictCatalogCaches();
                return enrichLoss(saved);
            } else {
                loss.setSizeId(size.getId());
                loss.setItemName("Masa " + size.getName());
                loss.setUnit("und");
                loss.setQuantity(BigDecimal.valueOf(units));
                loss.setUnitCost(uCost);
                loss.setTotalLossCost(totalCost);
                return enrichLoss(productLossRepository.save(loss));
            }
        } else {
            // PIZZA preparada perdida (ej. quemada en horno / caída)
            if (req.getPizzaId() == null) {
                throw new BadRequestException("Debe especificar pizzaId para registrar merma de pizza.");
            }
            Pizza pizza = pizzaRepository.findById(req.getPizzaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pizza no encontrada"));
            Size size = null;
            BigDecimal multiplier = BigDecimal.ONE;
            if (req.getSizeId() != null) {
                size = sizeRepository.findById(req.getSizeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tamaño no encontrado"));
                if (size.getRecipeMultiplier() != null && size.getRecipeMultiplier().compareTo(BigDecimal.ZERO) > 0) {
                    multiplier = size.getRecipeMultiplier();
                }
            }

            int units = req.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue();
            if (units <= 0) {
                throw new BadRequestException("La cantidad de pizzas debe ser al menos 1 unidad.");
            }
            BigDecimal unitsDec = BigDecimal.valueOf(units);

            List<PizzaIngredient> recipe = pizzaIngredientRepository.findByPizzaId(pizza.getId());
            BigDecimal unitPizzaCost = BigDecimal.ZERO;
            Map<Ingredient, BigDecimal> toDeduct = new LinkedHashMap<>();

            for (PizzaIngredient pi : recipe) {
                Integer ingId = pi.getId() != null ? pi.getId().getIngredientId() : null;
                if (ingId == null) continue;
                Ingredient ing = ingredientRepository.findById(ingId).orElse(null);
                if (ing == null) continue;
                BigDecimal perPizzaQty = (pi.getQuantity() != null ? pi.getQuantity() : new BigDecimal("50.00"))
                        .multiply(multiplier);
                BigDecimal uCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
                unitPizzaCost = unitPizzaCost.add(perPizzaQty.multiply(uCost));
                toDeduct.put(ing, perPizzaQty.multiply(unitsDec).setScale(2, RoundingMode.HALF_UP));
            }

            if (shouldDeduct) {
                for (Map.Entry<Ingredient, BigDecimal> entry : toDeduct.entrySet()) {
                    Ingredient ing = entry.getKey();
                    BigDecimal avail = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
                    if (avail.compareTo(entry.getValue()) < 0) {
                        throw new BadRequestException(String.format(
                                "Stock insuficiente de '%s' para descontar la receta de la merma (disponible %.2f %s, requerido %.2f %s).",
                                ing.getName(), avail, ing.getUnit(), entry.getValue(), ing.getUnit()));
                    }
                }
            }

            BigDecimal roundedUnitCost = unitPizzaCost.setScale(4, RoundingMode.HALF_UP);
            BigDecimal totalCost = roundedUnitCost.multiply(unitsDec).setScale(2, RoundingMode.HALF_UP);
            String fullName = size != null ? pizza.getName() + " (" + size.getName() + ")" : pizza.getName();

            loss.setPizzaId(pizza.getId());
            loss.setSizeId(size != null ? size.getId() : null);
            loss.setItemName(fullName);
            loss.setUnit("und");
            loss.setQuantity(unitsDec);
            loss.setUnitCost(roundedUnitCost);
            loss.setTotalLossCost(totalCost);
            ProductLoss saved = productLossRepository.save(loss);

            if (shouldDeduct) {
                for (Map.Entry<Ingredient, BigDecimal> entry : toDeduct.entrySet()) {
                    Ingredient ing = entry.getKey();
                    BigDecimal prev = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
                    BigDecimal qty = entry.getValue();
                    BigDecimal next = prev.subtract(qty).setScale(2, RoundingMode.HALF_UP);
                    ing.setStockQuantity(next);
                    ingredientRepository.save(ing);

                    BigDecimal uCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
                    recordLossMovement(StockMovement.ItemType.INGREDIENT, ing.getId(), null, null, qty, prev, next, uCost,
                            qty.multiply(uCost).setScale(2, RoundingMode.HALF_UP),
                            saved.getId(), "Merma de pizza: " + fullName, userId);
                }

                if (size != null) {
                    int prevSz = size.getStockQuantity() != null ? size.getStockQuantity() : 0;
                    int nextSz = Math.max(0, prevSz - units);
                    size.setStockQuantity(nextSz);
                    sizeRepository.save(size);

                    recordLossMovement(StockMovement.ItemType.SIZE, null, null, size.getId(), BigDecimal.valueOf(units),
                            BigDecimal.valueOf(prevSz), BigDecimal.valueOf(nextSz), BigDecimal.ZERO, BigDecimal.ZERO,
                            saved.getId(), "Merma de masa para pizza: " + fullName, userId);
                }

                inventoryService.evictCatalogCaches();
            }

            return enrichLoss(saved);
        }
    }

    public ProductLossDTO updateLoss(Integer id, ProductLossCreateDTO req) {
        if (id == null || req == null) {
            throw new BadRequestException("Datos de actualización inválidos.");
        }
        ProductLoss loss = productLossRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de pérdida no encontrado"));
        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            loss.setCategory(ProductLoss.LossCategory.valueOf(req.getCategory().toUpperCase()));
        }
        if (req.getReason() != null) {
            loss.setReason(req.getReason().trim());
        }
        if (req.getLossDate() != null) {
            loss.setLossDate(req.getLossDate());
        }
        return enrichLoss(productLossRepository.save(loss));
    }

    public void deleteLoss(Integer id, boolean restoreStock, Integer userId) {
        ProductLoss loss = productLossRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de pérdida no encontrado"));

        if (restoreStock && loss.getOrderId() == null) {
            if (loss.getItemType() == ProductLoss.ItemType.INGREDIENT && loss.getIngredientId() != null) {
                ingredientRepository.findById(loss.getIngredientId()).ifPresent(ing -> {
                    BigDecimal prev = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
                    BigDecimal add = loss.getQuantity() != null ? loss.getQuantity() : BigDecimal.ZERO;
                    BigDecimal next = prev.add(add).setScale(2, RoundingMode.HALF_UP);
                    ing.setStockQuantity(next);
                    ingredientRepository.save(ing);

                    BigDecimal uCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
                    StockMovement sm = new StockMovement();
                    sm.setItemType(StockMovement.ItemType.INGREDIENT);
                    sm.setIngredientId(ing.getId());
                    sm.setMovementType(StockMovement.MovementType.RETURN);
                    sm.setQuantity(add);
                    sm.setPreviousStock(prev);
                    sm.setNewStock(next);
                    sm.setUnitCost(uCost);
                    sm.setTotalCost(add.multiply(uCost).setScale(2, RoundingMode.HALF_UP));
                    sm.setReferenceType("LOSS_REVERT");
                    sm.setReferenceId(loss.getId());
                    sm.setReason("Reversión de registro de pérdida #" + loss.getId());
                    sm.setUserId(userId);
                    stockMovementRepository.save(sm);
                });
            } else if (loss.getItemType() == ProductLoss.ItemType.EXTRA && loss.getExtraId() != null) {
                extraRepository.findById(loss.getExtraId()).ifPresent(ext -> {
                    int prev = ext.getStockQuantity() != null ? ext.getStockQuantity() : 0;
                    int add = loss.getQuantity() != null ? loss.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue() : 0;
                    int next = prev + add;
                    ext.setStockQuantity(next);
                    extraRepository.save(ext);

                    BigDecimal uCost = ext.getUnitCost() != null ? ext.getUnitCost() : BigDecimal.ZERO;
                    StockMovement sm = new StockMovement();
                    sm.setItemType(StockMovement.ItemType.EXTRA);
                    sm.setExtraId(ext.getId());
                    sm.setMovementType(StockMovement.MovementType.RETURN);
                    sm.setQuantity(BigDecimal.valueOf(add));
                    sm.setPreviousStock(BigDecimal.valueOf(prev));
                    sm.setNewStock(BigDecimal.valueOf(next));
                    sm.setUnitCost(uCost);
                    sm.setTotalCost(BigDecimal.valueOf(add).multiply(uCost).setScale(2, RoundingMode.HALF_UP));
                    sm.setReferenceType("LOSS_REVERT");
                    sm.setReferenceId(loss.getId());
                    sm.setReason("Reversión de registro de pérdida #" + loss.getId());
                    sm.setUserId(userId);
                    stockMovementRepository.save(sm);
                });
            } else if (loss.getItemType() == ProductLoss.ItemType.SIZE && loss.getSizeId() != null) {
                sizeRepository.findById(loss.getSizeId()).ifPresent(sz -> {
                    int prev = sz.getStockQuantity() != null ? sz.getStockQuantity() : 0;
                    int add = loss.getQuantity() != null ? loss.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue() : 0;
                    int next = prev + add;
                    sz.setStockQuantity(next);
                    sizeRepository.save(sz);

                    StockMovement sm = new StockMovement();
                    sm.setItemType(StockMovement.ItemType.SIZE);
                    sm.setSizeId(sz.getId());
                    sm.setMovementType(StockMovement.MovementType.RETURN);
                    sm.setQuantity(BigDecimal.valueOf(add));
                    sm.setPreviousStock(BigDecimal.valueOf(prev));
                    sm.setNewStock(BigDecimal.valueOf(next));
                    sm.setUnitCost(BigDecimal.ZERO);
                    sm.setTotalCost(BigDecimal.ZERO);
                    sm.setReferenceType("LOSS_REVERT");
                    sm.setReferenceId(loss.getId());
                    sm.setReason("Reversión de registro de pérdida de masa #" + loss.getId());
                    sm.setUserId(userId);
                    stockMovementRepository.save(sm);
                });
            }
            inventoryService.evictCatalogCaches();
        }

        productLossRepository.delete(loss);
    }

    /**
     * Registra la pérdida financiera de un pedido que fue cancelado cuando ya estaba en PREPARING u OUT_FOR_DELIVERY.
     * No vuelve a descontar stock porque ya fue descontado al momento del checkout.
     */
    public List<ProductLoss> recordCancelledOrderLoss(Order order, Integer userId) {
        List<ProductLoss> created = new ArrayList<>();
        if (order == null || !"DEDUCTED".equalsIgnoreCase(order.getStockSettled())) {
            return created;
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            BigDecimal uCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
            BigDecimal lCost = item.getLineCost() != null && item.getLineCost().compareTo(BigDecimal.ZERO) > 0
                    ? item.getLineCost()
                    : uCost.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

            ProductLoss loss = new ProductLoss();
            loss.setOrderId(order.getId());
            loss.setCategory(ProductLoss.LossCategory.CANCELLED_ORDER);
            loss.setQuantity(BigDecimal.valueOf(qty));
            loss.setUnit("und");
            loss.setUnitCost(uCost);
            loss.setTotalLossCost(lCost);
            loss.setReason("Pedido #" + order.getId() + " cancelado durante preparación/reparto");
            loss.setReportedByUserId(userId);
            loss.setLossDate(LocalDateTime.now());

            if ("EXTRA".equalsIgnoreCase(item.getItemType()) && item.getExtraId() != null) {
                loss.setItemType(ProductLoss.ItemType.EXTRA);
                loss.setExtraId(item.getExtraId());
                String name = extraRepository.findById(item.getExtraId()).map(Extra::getName).orElse("Extra #" + item.getExtraId());
                loss.setItemName(name);
            } else {
                loss.setItemType(ProductLoss.ItemType.PIZZA);
                loss.setPizzaId(item.getPizzaId());
                loss.setSizeId(item.getSizeId());
                String pName = item.getPizzaId() != null
                        ? pizzaRepository.findById(item.getPizzaId()).map(Pizza::getName).orElse("Pizza #" + item.getPizzaId())
                        : "Pizza";
                String sName = item.getSizeId() != null
                        ? sizeRepository.findById(item.getSizeId()).map(Size::getName).orElse(null)
                        : null;
                loss.setItemName(sName != null ? pName + " (" + sName + ")" : pName);
            }
            created.add(productLossRepository.save(loss));
        }
        return created;
    }

    private void recordLossMovement(StockMovement.ItemType itemType, Integer ingId, Integer extId, Integer sizeId,
                                    BigDecimal qty, BigDecimal prev, BigDecimal next,
                                    BigDecimal uCost, BigDecimal totalCost,
                                    Integer lossId, String reason, Integer userId) {
        StockMovement sm = new StockMovement();
        sm.setItemType(itemType);
        sm.setIngredientId(ingId);
        sm.setExtraId(extId);
        sm.setSizeId(sizeId);
        sm.setMovementType(StockMovement.MovementType.LOSS);
        sm.setQuantity(qty.negate());
        sm.setPreviousStock(prev);
        sm.setNewStock(next);
        sm.setUnitCost(uCost);
        sm.setTotalCost(totalCost);
        sm.setReferenceType("LOSS");
        sm.setReferenceId(lossId);
        sm.setReason(reason != null && !reason.isBlank() ? reason : "Merma de producto #" + lossId);
        sm.setUserId(userId);
        stockMovementRepository.save(sm);
    }

    private ProductLossDTO enrichLoss(ProductLoss loss) {
        ProductLossDTO dto = new ProductLossDTO(loss);
        if (loss.getReportedByUserId() != null) {
            usuarioRepository.findById(loss.getReportedByUserId().longValue()).ifPresent(u ->
                    dto.setReportedByName(u.getName() != null && !u.getName().isBlank() ? u.getName() : u.getEmail()));
        }
        return dto;
    }
}

