package com.example.services;

import com.example.dtos.CheckoutRequestDTO;
import com.example.dtos.InventorySummaryDTO;
import com.example.dtos.StockMovementDTO;
import com.example.dtos.StockOperationRequestDTO;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.InsufficientStockException;
import com.example.exceptions.ResourceNotFoundException;
import com.example.models.Extra;
import com.example.models.Ingredient;
import com.example.models.Order;
import com.example.models.OrderItem;
import com.example.models.OrderItemExtra;
import com.example.models.Pizza;
import com.example.models.PizzaIngredient;
import com.example.models.Size;
import com.example.models.StockMovement;
import com.example.repositories.ExtraRepository;
import com.example.repositories.IngredientRepository;
import com.example.repositories.OrderItemExtraRepository;
import com.example.repositories.OrderItemRepository;
import com.example.repositories.PizzaIngredientRepository;
import com.example.repositories.PizzaRepository;
import com.example.repositories.SizeRepository;
import com.example.repositories.StockMovementRepository;
import com.example.repositories.UsuarioRepository;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private final IngredientRepository ingredientRepository;
    private final ExtraRepository extraRepository;
    private final PizzaRepository pizzaRepository;
    private final PizzaIngredientRepository pizzaIngredientRepository;
    private final SizeRepository sizeRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemExtraRepository orderItemExtraRepository;
    private final UsuarioRepository usuarioRepository;
    private final CacheManager cacheManager;

    public InventoryService(IngredientRepository ingredientRepository,
                            ExtraRepository extraRepository,
                            PizzaRepository pizzaRepository,
                            PizzaIngredientRepository pizzaIngredientRepository,
                            SizeRepository sizeRepository,
                            StockMovementRepository stockMovementRepository,
                            OrderItemRepository orderItemRepository,
                            OrderItemExtraRepository orderItemExtraRepository,
                            UsuarioRepository usuarioRepository,
                            CacheManager cacheManager) {
        this.ingredientRepository = ingredientRepository;
        this.extraRepository = extraRepository;
        this.pizzaRepository = pizzaRepository;
        this.pizzaIngredientRepository = pizzaIngredientRepository;
        this.sizeRepository = sizeRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderItemExtraRepository = orderItemExtraRepository;
        this.usuarioRepository = usuarioRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional(readOnly = true)
    public InventorySummaryDTO getSummary() {
        List<Ingredient> ingredients = ingredientRepository.findByDeletedAtIsNull();
        List<Extra> extras = extraRepository.findByDeletedAtIsNullOrderByDisplayOrderAsc();

        BigDecimal ingValue = BigDecimal.ZERO;
        BigDecimal extValue = BigDecimal.ZERO;
        int lowStock = 0;
        int outOfStock = 0;
        List<InventorySummaryDTO.AlertItemDTO> alerts = new ArrayList<>();

        for (Ingredient ing : ingredients) {
            BigDecimal stock = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
            BigDecimal min = ing.getMinStock() != null ? ing.getMinStock() : BigDecimal.ZERO;
            BigDecimal cost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
            ingValue = ingValue.add(stock.multiply(cost));

            if (stock.compareTo(BigDecimal.ZERO) <= 0) {
                outOfStock++;
                alerts.add(buildAlert("INGREDIENT", ing.getId(), ing.getName(), ing.getUnit(), stock, min, cost, "OUT_OF_STOCK"));
            } else if (stock.compareTo(min) <= 0) {
                lowStock++;
                alerts.add(buildAlert("INGREDIENT", ing.getId(), ing.getName(), ing.getUnit(), stock, min, cost, "LOW_STOCK"));
            }
        }

        for (Extra ext : extras) {
            int stockInt = ext.getStockQuantity() != null ? ext.getStockQuantity() : 0;
            int minInt = ext.getMinStock() != null ? ext.getMinStock() : 5;
            BigDecimal stock = BigDecimal.valueOf(stockInt);
            BigDecimal min = BigDecimal.valueOf(minInt);
            BigDecimal cost = ext.getUnitCost() != null ? ext.getUnitCost() : BigDecimal.ZERO;
            extValue = extValue.add(stock.multiply(cost));

            if (stockInt <= 0) {
                outOfStock++;
                alerts.add(buildAlert("EXTRA", ext.getId(), ext.getName(), "und", stock, min, cost, "OUT_OF_STOCK"));
            } else if (stockInt <= minInt) {
                lowStock++;
                alerts.add(buildAlert("EXTRA", ext.getId(), ext.getName(), "und", stock, min, cost, "LOW_STOCK"));
            }
        }

        List<Size> sizes = sizeRepository.findByDeletedAtIsNull();
        for (Size sz : sizes) {
            int stockInt = sz.getStockQuantity() != null ? sz.getStockQuantity() : 0;
            int minInt = sz.getMinStock() != null ? sz.getMinStock() : 10;
            BigDecimal stock = BigDecimal.valueOf(stockInt);
            BigDecimal min = BigDecimal.valueOf(minInt);

            if (stockInt <= 0) {
                outOfStock++;
                alerts.add(buildAlert("SIZE", sz.getId(), "Masa " + sz.getName(), "und", stock, min, BigDecimal.ZERO, "OUT_OF_STOCK"));
            } else if (stockInt <= minInt) {
                lowStock++;
                alerts.add(buildAlert("SIZE", sz.getId(), "Masa " + sz.getName(), "und", stock, min, BigDecimal.ZERO, "LOW_STOCK"));
            }
        }

        alerts.sort(Comparator.comparing((InventorySummaryDTO.AlertItemDTO a) -> !"OUT_OF_STOCK".equals(a.getStatus()))
                .thenComparing(InventorySummaryDTO.AlertItemDTO::getName));

        InventorySummaryDTO summary = new InventorySummaryDTO();
        summary.setIngredientsInventoryValue(ingValue.setScale(2, RoundingMode.HALF_UP));
        summary.setExtrasInventoryValue(extValue.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalInventoryValue(ingValue.add(extValue).setScale(2, RoundingMode.HALF_UP));
        summary.setTotalIngredients(ingredients.size());
        summary.setTotalExtras(extras.size());
        summary.setLowStockCount(lowStock);
        summary.setOutOfStockCount(outOfStock);
        summary.setAlerts(alerts);
        return summary;
    }

    @Transactional(readOnly = true)
    public List<InventorySummaryDTO.AlertItemDTO> getAlerts() {
        return getSummary().getAlerts();
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getMovements(String itemType, String movementType, Integer limit) {
        List<StockMovement> movements = stockMovementRepository.findTop200ByOrderByCreatedAtDesc();

        Map<Integer, Ingredient> ingMap = ingredientRepository.findAll().stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity(), (a, b) -> a));
        Map<Integer, Extra> extMap = extraRepository.findAll().stream()
                .collect(Collectors.toMap(Extra::getId, Function.identity(), (a, b) -> a));
        Map<Integer, Size> sizeMap = sizeRepository.findAll().stream()
                .collect(Collectors.toMap(Size::getId, Function.identity(), (a, b) -> a));
        Map<Integer, String> userMap = usuarioRepository.findAll().stream()
                .collect(Collectors.toMap(u -> u.getId().intValue(),
                        u -> u.getName() != null && !u.getName().isBlank() ? u.getName() : u.getEmail(),
                        (a, b) -> a));

        int maxRows = (limit != null && limit > 0) ? Math.min(limit, 500) : 200;

        return movements.stream()
                .filter(m -> itemType == null || itemType.isBlank() || m.getItemType().name().equalsIgnoreCase(itemType))
                .filter(m -> movementType == null || movementType.isBlank() || m.getMovementType().name().equalsIgnoreCase(movementType))
                .limit(maxRows)
                .map(m -> toMovementDto(m, ingMap, extMap, sizeMap, userMap))
                .toList();
    }

    public StockMovementDTO restock(StockOperationRequestDTO req, Integer userId) {
        validateOperationRequest(req);
        if (req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("La cantidad a reabastecer debe ser mayor a cero.");
        }

        StockMovement sm = new StockMovement();
        sm.setMovementType(StockMovement.MovementType.PURCHASE);
        sm.setReferenceType("MANUAL_RESTOCK");
        sm.setReason(req.getReason() != null && !req.getReason().isBlank() ? req.getReason().trim() : "Reabastecimiento de stock");
        sm.setUserId(userId);

        if ("INGREDIENT".equalsIgnoreCase(req.getItemType())) {
            Ingredient ing = ingredientRepository.findById(req.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado"));
            BigDecimal prev = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
            BigDecimal next = prev.add(req.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            ing.setStockQuantity(next);
            if (req.getUnitCost() != null && req.getUnitCost().compareTo(BigDecimal.ZERO) >= 0) {
                ing.setUnitCost(req.getUnitCost());
            }
            if (req.getMinStock() != null && req.getMinStock().compareTo(BigDecimal.ZERO) >= 0) {
                ing.setMinStock(req.getMinStock());
            }
            ingredientRepository.save(ing);

            BigDecimal uCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
            sm.setItemType(StockMovement.ItemType.INGREDIENT);
            sm.setIngredientId(ing.getId());
            sm.setQuantity(req.getQuantity().setScale(2, RoundingMode.HALF_UP));
            sm.setPreviousStock(prev);
            sm.setNewStock(next);
            sm.setUnitCost(uCost);
            sm.setTotalCost(req.getQuantity().multiply(uCost).setScale(2, RoundingMode.HALF_UP));
        } else if ("EXTRA".equalsIgnoreCase(req.getItemType())) {
            Extra ext = extraRepository.findById(req.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Extra no encontrado"));
            int addUnits = req.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue();
            if (addUnits <= 0) {
                throw new BadRequestException("La cantidad para extras debe ser al menos 1 unidad entera.");
            }
            int prevInt = ext.getStockQuantity() != null ? ext.getStockQuantity() : 0;
            int nextInt = prevInt + addUnits;
            ext.setStockQuantity(nextInt);
            if (req.getUnitCost() != null && req.getUnitCost().compareTo(BigDecimal.ZERO) >= 0) {
                ext.setUnitCost(req.getUnitCost().setScale(2, RoundingMode.HALF_UP));
            }
            if (req.getMinStock() != null && req.getMinStock().compareTo(BigDecimal.ZERO) >= 0) {
                ext.setMinStock(req.getMinStock().setScale(0, RoundingMode.HALF_UP).intValue());
            }
            extraRepository.save(ext);

            BigDecimal uCost = ext.getUnitCost() != null ? ext.getUnitCost() : BigDecimal.ZERO;
            sm.setItemType(StockMovement.ItemType.EXTRA);
            sm.setExtraId(ext.getId());
            sm.setQuantity(BigDecimal.valueOf(addUnits));
            sm.setPreviousStock(BigDecimal.valueOf(prevInt));
            sm.setNewStock(BigDecimal.valueOf(nextInt));
            sm.setUnitCost(uCost);
            sm.setTotalCost(BigDecimal.valueOf(addUnits).multiply(uCost).setScale(2, RoundingMode.HALF_UP));
        } else if ("SIZE".equalsIgnoreCase(req.getItemType())) {
            Size sz = sizeRepository.findById(req.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tamaño no encontrado"));
            int addUnits = req.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue();
            if (addUnits <= 0) {
                throw new BadRequestException("La cantidad para masa/tamaño debe ser al menos 1 unidad entera.");
            }
            int prevInt = sz.getStockQuantity() != null ? sz.getStockQuantity() : 0;
            int nextInt = prevInt + addUnits;
            sz.setStockQuantity(nextInt);
            if (req.getMinStock() != null && req.getMinStock().compareTo(BigDecimal.ZERO) >= 0) {
                sz.setMinStock(req.getMinStock().setScale(0, RoundingMode.HALF_UP).intValue());
            }
            sizeRepository.save(sz);

            sm.setItemType(StockMovement.ItemType.SIZE);
            sm.setSizeId(sz.getId());
            sm.setQuantity(BigDecimal.valueOf(addUnits));
            sm.setPreviousStock(BigDecimal.valueOf(prevInt));
            sm.setNewStock(BigDecimal.valueOf(nextInt));
            sm.setUnitCost(BigDecimal.ZERO);
            sm.setTotalCost(BigDecimal.ZERO);
        } else {
            throw new BadRequestException("Tipo de ítem inválido. Use INGREDIENT, EXTRA o SIZE.");
        }

        StockMovement saved = stockMovementRepository.save(sm);
        evictCatalogCaches();
        return enrichSingleMovement(saved);
    }

    public StockMovementDTO adjustStock(StockOperationRequestDTO req, Integer userId) {
        validateOperationRequest(req);
        if (req.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El nuevo stock físico no puede ser negativo.");
        }

        StockMovement sm = new StockMovement();
        sm.setMovementType(StockMovement.MovementType.ADJUSTMENT);
        sm.setReferenceType("MANUAL_ADJUST");
        sm.setReason(req.getReason() != null && !req.getReason().isBlank() ? req.getReason().trim() : "Ajuste físico de inventario");
        sm.setUserId(userId);

        if ("INGREDIENT".equalsIgnoreCase(req.getItemType())) {
            Ingredient ing = ingredientRepository.findById(req.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado"));
            BigDecimal prev = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
            BigDecimal next = req.getQuantity().setScale(2, RoundingMode.HALF_UP);
            BigDecimal diff = next.subtract(prev);
            ing.setStockQuantity(next);
            if (req.getUnitCost() != null && req.getUnitCost().compareTo(BigDecimal.ZERO) >= 0) {
                ing.setUnitCost(req.getUnitCost());
            }
            if (req.getMinStock() != null && req.getMinStock().compareTo(BigDecimal.ZERO) >= 0) {
                ing.setMinStock(req.getMinStock());
            }
            ingredientRepository.save(ing);

            BigDecimal uCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
            sm.setItemType(StockMovement.ItemType.INGREDIENT);
            sm.setIngredientId(ing.getId());
            sm.setQuantity(diff);
            sm.setPreviousStock(prev);
            sm.setNewStock(next);
            sm.setUnitCost(uCost);
            sm.setTotalCost(diff.abs().multiply(uCost).setScale(2, RoundingMode.HALF_UP));
        } else if ("EXTRA".equalsIgnoreCase(req.getItemType())) {
            Extra ext = extraRepository.findById(req.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Extra no encontrado"));
            int prevInt = ext.getStockQuantity() != null ? ext.getStockQuantity() : 0;
            int nextInt = req.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue();
            int diffInt = nextInt - prevInt;
            ext.setStockQuantity(nextInt);
            if (req.getUnitCost() != null && req.getUnitCost().compareTo(BigDecimal.ZERO) >= 0) {
                ext.setUnitCost(req.getUnitCost().setScale(2, RoundingMode.HALF_UP));
            }
            if (req.getMinStock() != null && req.getMinStock().compareTo(BigDecimal.ZERO) >= 0) {
                ext.setMinStock(req.getMinStock().setScale(0, RoundingMode.HALF_UP).intValue());
            }
            extraRepository.save(ext);

            BigDecimal uCost = ext.getUnitCost() != null ? ext.getUnitCost() : BigDecimal.ZERO;
            sm.setItemType(StockMovement.ItemType.EXTRA);
            sm.setExtraId(ext.getId());
            sm.setQuantity(BigDecimal.valueOf(diffInt));
            sm.setPreviousStock(BigDecimal.valueOf(prevInt));
            sm.setNewStock(BigDecimal.valueOf(nextInt));
            sm.setUnitCost(uCost);
            sm.setTotalCost(BigDecimal.valueOf(Math.abs(diffInt)).multiply(uCost).setScale(2, RoundingMode.HALF_UP));
        } else if ("SIZE".equalsIgnoreCase(req.getItemType())) {
            Size sz = sizeRepository.findById(req.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tamaño no encontrado"));
            int prevInt = sz.getStockQuantity() != null ? sz.getStockQuantity() : 0;
            int nextInt = req.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue();
            int diffInt = nextInt - prevInt;
            sz.setStockQuantity(nextInt);
            if (req.getMinStock() != null && req.getMinStock().compareTo(BigDecimal.ZERO) >= 0) {
                sz.setMinStock(req.getMinStock().setScale(0, RoundingMode.HALF_UP).intValue());
            }
            sizeRepository.save(sz);

            sm.setItemType(StockMovement.ItemType.SIZE);
            sm.setSizeId(sz.getId());
            sm.setQuantity(BigDecimal.valueOf(diffInt));
            sm.setPreviousStock(BigDecimal.valueOf(prevInt));
            sm.setNewStock(BigDecimal.valueOf(nextInt));
            sm.setUnitCost(BigDecimal.ZERO);
            sm.setTotalCost(BigDecimal.ZERO);
        } else {
            throw new BadRequestException("Tipo de ítem inválido. Use INGREDIENT, EXTRA o SIZE.");
        }

        StockMovement saved = stockMovementRepository.save(sm);
        evictCatalogCaches();
        return enrichSingleMovement(saved);
    }

    /**
     * Valida la disponibilidad acumulada de stock de todos los ítems del pedido,
     * descuenta stock de ingredientes y extras, registra movimientos SALE en el Kardex
     * y retorna la lista de costos unitarios directos (unitCost) por cada CheckoutItemDTO.
     */
    public List<BigDecimal> deductCheckoutStockAndCalculateUnitCosts(
            List<CheckoutRequestDTO.CheckoutItemDTO> items,
            Integer orderId,
            Integer userId) {

        List<BigDecimal> unitCosts = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return unitCosts;
        }

        Map<Integer, BigDecimal> requiredIngredients = new LinkedHashMap<>();
        Map<Integer, Integer> requiredExtras = new LinkedHashMap<>();
        Map<Integer, Integer> requiredSizes = new LinkedHashMap<>();
        Map<Integer, Ingredient> loadedIngredients = new HashMap<>();
        Map<Integer, Extra> loadedExtras = new HashMap<>();
        Map<Integer, Size> loadedSizes = new HashMap<>();

        for (CheckoutRequestDTO.CheckoutItemDTO item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Cantidad inválida. Debe ser mayor a cero.");
            }
            int qty = item.getQuantity();
            BigDecimal qtyDec = BigDecimal.valueOf(qty);

            if ("PIZZA".equalsIgnoreCase(item.getItemType())) {
                Pizza pizza = pizzaRepository.findById(item.getPizzaId())
                        .orElseThrow(() -> new IllegalArgumentException("Pizza no encontrada"));
                if (!pizza.getIsAvailable() || pizza.getDeletedAt() != null) {
                    throw new IllegalStateException("La pizza '" + pizza.getName() + "' no está disponible actualmente.");
                }

                BigDecimal multiplier = BigDecimal.ONE;
                if (item.getSizeId() != null) {
                    Size size = loadedSizes.computeIfAbsent(item.getSizeId(), id -> {
                        Optional<Size> locked = sizeRepository.findByIdWithLock(id);
                        return (locked != null && locked.isPresent())
                                ? locked.get()
                                : sizeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tamaño no encontrado"));
                    });
                    if (size.getRecipeMultiplier() != null && size.getRecipeMultiplier().compareTo(BigDecimal.ZERO) > 0) {
                        multiplier = size.getRecipeMultiplier();
                    }
                    requiredSizes.merge(size.getId(), qty, Integer::sum);
                } else {
                    Size defaultSize = loadedSizes.computeIfAbsent(-1, k ->
                            sizeRepository.findByDeletedAtIsNull().stream()
                                    .min(Comparator.comparing(s -> s.getDisplayOrder() != null ? s.getDisplayOrder() : s.getId()))
                                    .orElse(null));
                    if (defaultSize != null) {
                        loadedSizes.put(defaultSize.getId(), defaultSize);
                        requiredSizes.merge(defaultSize.getId(), qty, Integer::sum);
                    }
                }

                List<PizzaIngredient> recipe = pizzaIngredientRepository.findByPizzaId(pizza.getId());
                BigDecimal unitDirectCost = BigDecimal.ZERO;

                for (PizzaIngredient pi : recipe) {
                    Integer ingId = pi.getId() != null ? pi.getId().getIngredientId() : null;
                    if (ingId == null) continue;
                    Ingredient ing = loadedIngredients.computeIfAbsent(ingId, id -> {
                        Optional<Ingredient> locked = ingredientRepository.findByIdWithLock(id);
                        return (locked != null && locked.isPresent())
                                ? locked.get()
                                : ingredientRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Ingrediente de receta no encontrado"));
                    });
                    BigDecimal perPizzaQty = (pi.getQuantity() != null ? pi.getQuantity() : new BigDecimal("50.00"))
                            .multiply(multiplier);
                    BigDecimal totalNeeded = perPizzaQty.multiply(qtyDec);
                    requiredIngredients.merge(ingId, totalNeeded, BigDecimal::add);

                    BigDecimal ingCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
                    unitDirectCost = unitDirectCost.add(perPizzaQty.multiply(ingCost));
                }

                if (item.getExtraIngredientIds() != null && !item.getExtraIngredientIds().isEmpty()) {
                    for (Integer extraIngId : item.getExtraIngredientIds()) {
                        Ingredient ing = loadedIngredients.computeIfAbsent(extraIngId, id -> {
                            Optional<Ingredient> locked = ingredientRepository.findByIdWithLock(id);
                            return (locked != null && locked.isPresent())
                                    ? locked.get()
                                    : ingredientRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Topping extra no encontrado"));
                        });
                        BigDecimal portion = ing.getExtraPortionQuantity() != null && ing.getExtraPortionQuantity().compareTo(BigDecimal.ZERO) > 0
                                ? ing.getExtraPortionQuantity()
                                : new BigDecimal("50.00");
                        requiredIngredients.merge(extraIngId, portion.multiply(qtyDec), BigDecimal::add);

                        BigDecimal ingCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
                        unitDirectCost = unitDirectCost.add(portion.multiply(ingCost));
                    }
                }

                unitCosts.add(unitDirectCost.setScale(2, RoundingMode.HALF_UP));

            } else if ("EXTRA".equalsIgnoreCase(item.getItemType())) {
                Extra extra = loadedExtras.computeIfAbsent(item.getExtraId(), id -> {
                    Optional<Extra> locked = extraRepository.findByIdWithLock(id);
                    return (locked != null && locked.isPresent())
                            ? locked.get()
                            : extraRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Extra no encontrado"));
                });
                if (extra.getIsAvailable() == null || !extra.getIsAvailable() || extra.getDeletedAt() != null) {
                    throw new IllegalStateException("El producto '" + extra.getName() + "' no está disponible.");
                }
                requiredExtras.merge(extra.getId(), qty, Integer::sum);
                BigDecimal extUnitCost = extra.getUnitCost() != null ? extra.getUnitCost() : BigDecimal.ZERO;
                unitCosts.add(extUnitCost.setScale(2, RoundingMode.HALF_UP));
            } else {
                unitCosts.add(BigDecimal.ZERO);
            }
        }

        // Validar disponibilidad de masa para todos los tamaños requeridos (stock compartido)
        for (Map.Entry<Integer, Integer> entry : requiredSizes.entrySet()) {
            Size sz = loadedSizes.get(entry.getKey());
            int available = sz.getStockQuantity() != null ? sz.getStockQuantity() : 0;
            int needed = entry.getValue();
            if (available < needed) {
                throw new InsufficientStockException(String.format(
                        "Stock insuficiente de masa para el tamaño '%s': disponible %d masas, solicitado %d.",
                        sz.getName(), available, needed));
            }
        }

        // Validar disponibilidad de todos los ingredientes requeridos
        for (Map.Entry<Integer, BigDecimal> entry : requiredIngredients.entrySet()) {
            Ingredient ing = loadedIngredients.get(entry.getKey());
            BigDecimal available = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
            BigDecimal needed = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            if (!ing.isAvailable() || available.compareTo(needed) < 0) {
                throw new InsufficientStockException(String.format(
                        "Stock insuficiente del insumo '%s': disponible %.2f %s, requerido %.2f %s.",
                        ing.getName(), available, ing.getUnit(), needed, ing.getUnit()));
            }
        }

        // Validar disponibilidad de todos los extras requeridos
        for (Map.Entry<Integer, Integer> entry : requiredExtras.entrySet()) {
            Extra extra = loadedExtras.get(entry.getKey());
            int available = extra.getStockQuantity() != null ? extra.getStockQuantity() : 0;
            int needed = entry.getValue();
            if (available < needed) {
                throw new InsufficientStockException(String.format(
                        "Stock insuficiente para '%s': disponible %d und, solicitado %d und.",
                        extra.getName(), available, needed));
            }
        }

        // Descontar masa compartida por tamaño y registrar movimientos SALE
        for (Map.Entry<Integer, Integer> entry : requiredSizes.entrySet()) {
            Size sz = loadedSizes.get(entry.getKey());
            int prev = sz.getStockQuantity() != null ? sz.getStockQuantity() : 0;
            int deduct = entry.getValue();
            int next = prev - deduct;
            if (next < 0) {
                throw new InsufficientStockException(String.format(
                        "Stock insuficiente de masa para el tamaño '%s': disponible %d masas, solicitado %d.",
                        sz.getName(), prev, deduct));
            }
            sz.setStockQuantity(next);
            sizeRepository.save(sz);

            StockMovement sm = new StockMovement();
            sm.setItemType(StockMovement.ItemType.SIZE);
            sm.setSizeId(sz.getId());
            sm.setMovementType(StockMovement.MovementType.SALE);
            sm.setQuantity(BigDecimal.valueOf(-deduct));
            sm.setPreviousStock(BigDecimal.valueOf(prev));
            sm.setNewStock(BigDecimal.valueOf(next));
            sm.setUnitCost(BigDecimal.ZERO);
            sm.setTotalCost(BigDecimal.ZERO);
            sm.setReferenceType("ORDER");
            sm.setReferenceId(orderId);
            sm.setReason("Consumo de masa para venta pedido #" + orderId);
            sm.setUserId(userId);
            stockMovementRepository.save(sm);
        }

        // Descontar stock de ingredientes y registrar movimientos SALE
        for (Map.Entry<Integer, BigDecimal> entry : requiredIngredients.entrySet()) {
            Ingredient ing = loadedIngredients.get(entry.getKey());
            BigDecimal prev = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
            BigDecimal deduct = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            BigDecimal next = prev.subtract(deduct).setScale(2, RoundingMode.HALF_UP);
            if (next.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientStockException(String.format(
                        "Stock insuficiente del insumo '%s': disponible %.2f %s, requerido %.2f %s.",
                        ing.getName(), prev, ing.getUnit(), deduct, ing.getUnit()));
            }
            ing.setStockQuantity(next);
            ingredientRepository.save(ing);

            BigDecimal uCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
            StockMovement sm = new StockMovement();
            sm.setItemType(StockMovement.ItemType.INGREDIENT);
            sm.setIngredientId(ing.getId());
            sm.setMovementType(StockMovement.MovementType.SALE);
            sm.setQuantity(deduct.negate());
            sm.setPreviousStock(prev);
            sm.setNewStock(next);
            sm.setUnitCost(uCost);
            sm.setTotalCost(deduct.multiply(uCost).setScale(2, RoundingMode.HALF_UP));
            sm.setReferenceType("ORDER");
            sm.setReferenceId(orderId);
            sm.setReason("Venta pedido #" + orderId);
            sm.setUserId(userId);
            stockMovementRepository.save(sm);
        }

        for (Map.Entry<Integer, Integer> entry : requiredExtras.entrySet()) {
            Extra extra = loadedExtras.get(entry.getKey());
            int prev = extra.getStockQuantity() != null ? extra.getStockQuantity() : 0;
            int deduct = entry.getValue();
            int next = prev - deduct;
            if (next < 0) {
                throw new InsufficientStockException(String.format(
                        "Stock insuficiente para '%s': disponible %d und, solicitado %d und.",
                        extra.getName(), prev, deduct));
            }
            extra.setStockQuantity(next);
            extraRepository.save(extra);

            BigDecimal uCost = extra.getUnitCost() != null ? extra.getUnitCost() : BigDecimal.ZERO;
            StockMovement sm = new StockMovement();
            sm.setItemType(StockMovement.ItemType.EXTRA);
            sm.setExtraId(extra.getId());
            sm.setMovementType(StockMovement.MovementType.SALE);
            sm.setQuantity(BigDecimal.valueOf(-deduct));
            sm.setPreviousStock(BigDecimal.valueOf(prev));
            sm.setNewStock(BigDecimal.valueOf(next));
            sm.setUnitCost(uCost);
            sm.setTotalCost(BigDecimal.valueOf(deduct).multiply(uCost).setScale(2, RoundingMode.HALF_UP));
            sm.setReferenceType("ORDER");
            sm.setReferenceId(orderId);
            sm.setReason("Venta pedido #" + orderId);
            sm.setUserId(userId);
            stockMovementRepository.save(sm);
        }

        evictCatalogCaches();
        return unitCosts;
    }

    /**
     * Devuelve el stock de un pedido cancelado antes de entrar a preparación (PENDING o CONFIRMED).
     */
    public void returnOrderStock(Order order, Integer userId) {
        if (order == null || !"DEDUCTED".equalsIgnoreCase(order.getStockSettled())) {
            return;
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        Map<Integer, BigDecimal> returnIngredients = new LinkedHashMap<>();
        Map<Integer, Integer> returnExtras = new LinkedHashMap<>();
        Map<Integer, Integer> returnSizes = new LinkedHashMap<>();

        for (OrderItem item : items) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            BigDecimal qtyDec = BigDecimal.valueOf(qty);

            if ("EXTRA".equalsIgnoreCase(item.getItemType()) && item.getExtraId() != null) {
                returnExtras.merge(item.getExtraId(), qty, Integer::sum);
            } else if (item.getPizzaId() != null) {
                BigDecimal multiplier = BigDecimal.ONE;
                if (item.getSizeId() != null) {
                    returnSizes.merge(item.getSizeId(), qty, Integer::sum);
                    multiplier = sizeRepository.findById(item.getSizeId())
                            .map(Size::getRecipeMultiplier)
                            .filter(m -> m != null && m.compareTo(BigDecimal.ZERO) > 0)
                            .orElse(BigDecimal.ONE);
                } else {
                    sizeRepository.findByDeletedAtIsNull().stream()
                            .min(Comparator.comparing(s -> s.getDisplayOrder() != null ? s.getDisplayOrder() : s.getId()))
                            .ifPresent(defaultSize -> returnSizes.merge(defaultSize.getId(), qty, Integer::sum));
                }
                List<PizzaIngredient> recipe = pizzaIngredientRepository.findByPizzaId(item.getPizzaId());
                for (PizzaIngredient pi : recipe) {
                    Integer ingId = pi.getId() != null ? pi.getId().getIngredientId() : null;
                    if (ingId == null) continue;
                    BigDecimal perPizza = (pi.getQuantity() != null ? pi.getQuantity() : new BigDecimal("50.00"))
                            .multiply(multiplier);
                    returnIngredients.merge(ingId, perPizza.multiply(qtyDec), BigDecimal::add);
                }

                List<OrderItemExtra> itemExtras = orderItemExtraRepository.findByOrderItemId(item.getId());
                for (OrderItemExtra oie : itemExtras) {
                    if (oie.getIngredientId() == null) continue;
                    Ingredient ing = ingredientRepository.findById(oie.getIngredientId()).orElse(null);
                    BigDecimal portion = (ing != null && ing.getExtraPortionQuantity() != null)
                            ? ing.getExtraPortionQuantity()
                            : new BigDecimal("50.00");
                    returnIngredients.merge(oie.getIngredientId(), portion.multiply(qtyDec), BigDecimal::add);
                }
            }
        }

        for (Map.Entry<Integer, BigDecimal> entry : returnIngredients.entrySet()) {
            ingredientRepository.findById(entry.getKey()).ifPresent(ing -> {
                BigDecimal prev = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
                BigDecimal addQty = entry.getValue().setScale(2, RoundingMode.HALF_UP);
                BigDecimal next = prev.add(addQty).setScale(2, RoundingMode.HALF_UP);
                ing.setStockQuantity(next);
                ingredientRepository.save(ing);

                BigDecimal uCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
                StockMovement sm = new StockMovement();
                sm.setItemType(StockMovement.ItemType.INGREDIENT);
                sm.setIngredientId(ing.getId());
                sm.setMovementType(StockMovement.MovementType.RETURN);
                sm.setQuantity(addQty);
                sm.setPreviousStock(prev);
                sm.setNewStock(next);
                sm.setUnitCost(uCost);
                sm.setTotalCost(addQty.multiply(uCost).setScale(2, RoundingMode.HALF_UP));
                sm.setReferenceType("ORDER_CANCEL");
                sm.setReferenceId(order.getId());
                sm.setReason("Devolución de insumos por cancelación de pedido #" + order.getId());
                sm.setUserId(userId);
                stockMovementRepository.save(sm);
            });
        }

        for (Map.Entry<Integer, Integer> entry : returnExtras.entrySet()) {
            extraRepository.findById(entry.getKey()).ifPresent(ext -> {
                int prev = ext.getStockQuantity() != null ? ext.getStockQuantity() : 0;
                int addQty = entry.getValue();
                int next = prev + addQty;
                ext.setStockQuantity(next);
                extraRepository.save(ext);

                BigDecimal uCost = ext.getUnitCost() != null ? ext.getUnitCost() : BigDecimal.ZERO;
                StockMovement sm = new StockMovement();
                sm.setItemType(StockMovement.ItemType.EXTRA);
                sm.setExtraId(ext.getId());
                sm.setMovementType(StockMovement.MovementType.RETURN);
                sm.setQuantity(BigDecimal.valueOf(addQty));
                sm.setPreviousStock(BigDecimal.valueOf(prev));
                sm.setNewStock(BigDecimal.valueOf(next));
                sm.setUnitCost(uCost);
                sm.setTotalCost(BigDecimal.valueOf(addQty).multiply(uCost).setScale(2, RoundingMode.HALF_UP));
                sm.setReferenceType("ORDER_CANCEL");
                sm.setReferenceId(order.getId());
                sm.setReason("Devolución de producto por cancelación de pedido #" + order.getId());
                sm.setUserId(userId);
                stockMovementRepository.save(sm);
            });
        }

        for (Map.Entry<Integer, Integer> entry : returnSizes.entrySet()) {
            sizeRepository.findById(entry.getKey()).ifPresent(sz -> {
                int prev = sz.getStockQuantity() != null ? sz.getStockQuantity() : 0;
                int addQty = entry.getValue();
                int next = prev + addQty;
                sz.setStockQuantity(next);
                sizeRepository.save(sz);

                StockMovement sm = new StockMovement();
                sm.setItemType(StockMovement.ItemType.SIZE);
                sm.setSizeId(sz.getId());
                sm.setMovementType(StockMovement.MovementType.RETURN);
                sm.setQuantity(BigDecimal.valueOf(addQty));
                sm.setPreviousStock(BigDecimal.valueOf(prev));
                sm.setNewStock(BigDecimal.valueOf(next));
                sm.setUnitCost(BigDecimal.ZERO);
                sm.setTotalCost(BigDecimal.ZERO);
                sm.setReferenceType("ORDER_CANCEL");
                sm.setReferenceId(order.getId());
                sm.setReason("Devolución de masa por cancelación de pedido #" + order.getId());
                sm.setUserId(userId);
                stockMovementRepository.save(sm);
            });
        }

        evictCatalogCaches();
    }

    public void evictCatalogCaches() {
        if (cacheManager == null) return;
        for (String name : List.of("pizzas", "ingredients", "extras", "extrasDisponibles", "sizes")) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private void validateOperationRequest(StockOperationRequestDTO req) {
        if (req == null || req.getItemType() == null || req.getItemId() == null || req.getQuantity() == null) {
            throw new BadRequestException("Debe especificar itemType, itemId y quantity.");
        }
    }

    private InventorySummaryDTO.AlertItemDTO buildAlert(String itemType, Integer itemId, String name, String unit,
                                                        BigDecimal stock, BigDecimal min, BigDecimal cost, String status) {
        InventorySummaryDTO.AlertItemDTO alert = new InventorySummaryDTO.AlertItemDTO();
        alert.setItemType(itemType);
        alert.setItemId(itemId);
        alert.setName(name);
        alert.setUnit(unit != null ? unit : "g");
        alert.setStockQuantity(stock);
        alert.setMinStock(min);
        alert.setUnitCost(cost);
        alert.setStatus(status);
        return alert;
    }

    private StockMovementDTO enrichSingleMovement(StockMovement sm) {
        StockMovementDTO dto = new StockMovementDTO(sm);
        if (sm.getIngredientId() != null) {
            ingredientRepository.findById(sm.getIngredientId()).ifPresent(ing -> {
                dto.setItemName(ing.getName());
                dto.setUnit(ing.getUnit());
            });
        } else if (sm.getExtraId() != null) {
            extraRepository.findById(sm.getExtraId()).ifPresent(ext -> {
                dto.setItemName(ext.getName());
                dto.setUnit("und");
            });
        } else if (sm.getSizeId() != null) {
            sizeRepository.findById(sm.getSizeId()).ifPresent(sz -> {
                dto.setItemName("Masa " + sz.getName());
                dto.setUnit("und");
            });
        }
        if (sm.getUserId() != null) {
            usuarioRepository.findById(sm.getUserId().longValue()).ifPresent(u ->
                    dto.setUserName(u.getName() != null && !u.getName().isBlank() ? u.getName() : u.getEmail()));
        }
        return dto;
    }

    private StockMovementDTO toMovementDto(StockMovement sm,
                                           Map<Integer, Ingredient> ingMap,
                                           Map<Integer, Extra> extMap,
                                           Map<Integer, Size> sizeMap,
                                           Map<Integer, String> userMap) {
        StockMovementDTO dto = new StockMovementDTO(sm);
        if (sm.getIngredientId() != null) {
            Ingredient ing = ingMap.get(sm.getIngredientId());
            dto.setItemName(ing != null ? ing.getName() : "Ingrediente #" + sm.getIngredientId());
            dto.setUnit(ing != null ? ing.getUnit() : "g");
        } else if (sm.getExtraId() != null) {
            Extra ext = extMap.get(sm.getExtraId());
            dto.setItemName(ext != null ? ext.getName() : "Extra #" + sm.getExtraId());
            dto.setUnit("und");
        } else if (sm.getSizeId() != null) {
            Size sz = sizeMap != null ? sizeMap.get(sm.getSizeId()) : null;
            dto.setItemName(sz != null ? "Masa " + sz.getName() : "Tamaño #" + sm.getSizeId());
            dto.setUnit("und");
        }
        if (sm.getUserId() != null) {
            dto.setUserName(userMap.getOrDefault(sm.getUserId(), "Usuario #" + sm.getUserId()));
        }
        return dto;
    }
}

