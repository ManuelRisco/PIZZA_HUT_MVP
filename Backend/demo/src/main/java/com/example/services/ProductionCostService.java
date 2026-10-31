package com.example.services;

import com.example.dtos.ProductionCatalogCostDTO;
import com.example.dtos.ProductionEstimateRequestDTO;
import com.example.dtos.ProductionEstimateResponseDTO;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;
import com.example.models.Extra;
import com.example.models.Ingredient;
import com.example.models.Pizza;
import com.example.models.PizzaIngredient;
import com.example.models.Size;
import com.example.repositories.ExtraRepository;
import com.example.repositories.IngredientRepository;
import com.example.repositories.PizzaIngredientRepository;
import com.example.repositories.PizzaRepository;
import com.example.repositories.SizeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductionCostService {

    private final PizzaRepository pizzaRepository;
    private final PizzaIngredientRepository pizzaIngredientRepository;
    private final IngredientRepository ingredientRepository;
    private final SizeRepository sizeRepository;
    private final ExtraRepository extraRepository;

    public ProductionCostService(PizzaRepository pizzaRepository,
                                 PizzaIngredientRepository pizzaIngredientRepository,
                                 IngredientRepository ingredientRepository,
                                 SizeRepository sizeRepository,
                                 ExtraRepository extraRepository) {
        this.pizzaRepository = pizzaRepository;
        this.pizzaIngredientRepository = pizzaIngredientRepository;
        this.ingredientRepository = ingredientRepository;
        this.sizeRepository = sizeRepository;
        this.extraRepository = extraRepository;
    }

    public ProductionCatalogCostDTO getCatalogCosts() {
        List<Pizza> pizzas = pizzaRepository.findByDeletedAtIsNull();
        List<Size> sizes = sizeRepository.findByDeletedAtIsNull().stream()
                .sorted(Comparator.comparing(s -> s.getDisplayOrder() != null ? s.getDisplayOrder() : s.getId()))
                .toList();
        List<Extra> extras = extraRepository.findByDeletedAtIsNullOrderByDisplayOrderAsc();
        Map<Integer, Ingredient> ingMap = ingredientRepository.findByDeletedAtIsNull().stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity(), (a, b) -> a));

        Map<Integer, List<PizzaIngredient>> recipesByPizza = new HashMap<>();
        for (PizzaIngredient pi : pizzaIngredientRepository.findAll()) {
            Integer pId = pi.getId() != null ? pi.getId().getPizzaId() : null;
            if (pId != null) {
                recipesByPizza.computeIfAbsent(pId, k -> new ArrayList<>()).add(pi);
            }
        }

        List<ProductionCatalogCostDTO.PizzaSizeCostDTO> pizzaCosts = new ArrayList<>();
        for (Pizza pizza : pizzas) {
            List<PizzaIngredient> recipe = recipesByPizza.getOrDefault(pizza.getId(), Collections.emptyList());

            // Stock base producible de la pizza: unificado e idéntico para todos los tamaños
            int pizzaBaseStock = recipe.isEmpty() ? 0 : Integer.MAX_VALUE;
            for (PizzaIngredient pi : recipe) {
                Integer ingId = pi.getId() != null ? pi.getId().getIngredientId() : null;
                Ingredient ing = ingId != null ? ingMap.get(ingId) : null;
                BigDecimal baseQty = pi.getQuantity() != null ? pi.getQuantity() : new BigDecimal("50.00");
                if (ing == null || !ing.isAvailable() || ing.getDeletedAt() != null || baseQty.compareTo(BigDecimal.ZERO) <= 0) {
                    pizzaBaseStock = 0;
                } else {
                    BigDecimal stock = ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO;
                    int possible = stock.divide(baseQty, 0, RoundingMode.FLOOR).intValue();
                    pizzaBaseStock = Math.min(pizzaBaseStock, Math.max(0, possible));
                }
            }
            if (pizzaBaseStock == Integer.MAX_VALUE) {
                pizzaBaseStock = 0;
            }

            for (Size size : sizes) {
                BigDecimal multiplier = size.getRecipeMultiplier() != null && size.getRecipeMultiplier().compareTo(BigDecimal.ZERO) > 0
                        ? size.getRecipeMultiplier()
                        : BigDecimal.ONE;
                BigDecimal sizeExtra = size.getExtraCost() != null ? size.getExtraCost() : BigDecimal.ZERO;
                BigDecimal basePrice = pizza.getPrice() != null ? pizza.getPrice() : BigDecimal.ZERO;
                BigDecimal salePrice = basePrice.add(sizeExtra).setScale(2, RoundingMode.HALF_UP);

                BigDecimal directCost = BigDecimal.ZERO;
                List<ProductionCatalogCostDTO.RecipeLineCostDTO> lines = new ArrayList<>();

                for (PizzaIngredient pi : recipe) {
                    Integer ingId = pi.getId() != null ? pi.getId().getIngredientId() : null;
                    Ingredient ing = ingId != null ? ingMap.get(ingId) : null;
                    BigDecimal baseQty = pi.getQuantity() != null ? pi.getQuantity() : new BigDecimal("50.00");
                    BigDecimal scaledQty = baseQty.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal uCost = (ing != null && ing.getUnitCost() != null) ? ing.getUnitCost() : BigDecimal.ZERO;
                    BigDecimal lineCost = scaledQty.multiply(uCost).setScale(2, RoundingMode.HALF_UP);
                    directCost = directCost.add(scaledQty.multiply(uCost));

                    ProductionCatalogCostDTO.RecipeLineCostDTO lineDto = new ProductionCatalogCostDTO.RecipeLineCostDTO();
                    lineDto.setIngredientId(ingId);
                    lineDto.setIngredientName(ing != null ? ing.getName() : "Ingrediente #" + ingId);
                    lineDto.setUnit(ing != null && ing.getUnit() != null ? ing.getUnit() : "g");
                    lineDto.setQuantityPerUnit(scaledQty);
                    lineDto.setUnitCost(uCost);
                    lineDto.setLineCost(lineCost);
                    lines.add(lineDto);
                }

                BigDecimal roundedDirect = directCost.setScale(2, RoundingMode.HALF_UP);
                BigDecimal unitProfit = salePrice.subtract(roundedDirect).setScale(2, RoundingMode.HALF_UP);
                BigDecimal margin = salePrice.compareTo(BigDecimal.ZERO) > 0
                        ? unitProfit.multiply(new BigDecimal("100")).divide(salePrice, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                int sharedDoughStock = size.getStockQuantity() != null ? size.getStockQuantity() : 0;
                boolean isPizzaAvailable = Boolean.TRUE.equals(pizza.getIsAvailable());
                boolean hasIngredients = recipe.isEmpty() || pizzaBaseStock > 0;
                int maxProducible = (isPizzaAvailable && hasIngredients) ? sharedDoughStock : 0;

                ProductionCatalogCostDTO.PizzaSizeCostDTO row = new ProductionCatalogCostDTO.PizzaSizeCostDTO();
                row.setPizzaId(pizza.getId());
                row.setPizzaName(pizza.getName());
                row.setCategoryName(pizza.getCategory() != null ? pizza.getCategory().getName() : "Sin categoría");
                row.setImageUrl(pizza.getImageUrl());
                row.setAvailable(pizza.getIsAvailable());
                row.setSizeId(size.getId());
                row.setSizeName(size.getName());
                row.setRecipeMultiplier(multiplier);
                row.setSalePrice(salePrice);
                row.setDirectCost(roundedDirect);
                row.setUnitProfit(unitProfit);
                row.setProfitMargin(margin);
                row.setMaxProducibleUnits(maxProducible); // Stock de masa compartido por todas las pizzas para este tamaño
                row.setIngredients(lines);
                pizzaCosts.add(row);
            }
        }

        List<ProductionCatalogCostDTO.ExtraCostDTO> extraCosts = new ArrayList<>();
        for (Extra ext : extras) {
            BigDecimal salePrice = ext.getPrice() != null ? ext.getPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal directCost = ext.getUnitCost() != null ? ext.getUnitCost().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal unitProfit = salePrice.subtract(directCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal margin = salePrice.compareTo(BigDecimal.ZERO) > 0
                    ? unitProfit.multiply(new BigDecimal("100")).divide(salePrice, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            ProductionCatalogCostDTO.ExtraCostDTO row = new ProductionCatalogCostDTO.ExtraCostDTO();
            row.setExtraId(ext.getId());
            row.setName(ext.getName());
            row.setCategory(ext.getCategory() != null ? ext.getCategory().name() : null);
            row.setImageUrl(ext.getImageUrl());
            row.setAvailable(Boolean.TRUE.equals(ext.getIsAvailable()));
            row.setSalePrice(salePrice);
            row.setDirectCost(directCost);
            row.setUnitProfit(unitProfit);
            row.setProfitMargin(margin);
            row.setAvailableStock(ext.getStockQuantity() != null ? ext.getStockQuantity() : 0);
            extraCosts.add(row);
        }

        ProductionCatalogCostDTO response = new ProductionCatalogCostDTO();
        response.setPizzaCosts(pizzaCosts);
        response.setExtraCosts(extraCosts);
        return response;
    }

    public ProductionEstimateResponseDTO estimateBatch(ProductionEstimateRequestDTO req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new BadRequestException("Debe agregar al menos un producto para estimar el lote de producción.");
        }

        Map<Integer, Ingredient> ingMap = ingredientRepository.findAll().stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity(), (a, b) -> a));
        Map<Integer, BigDecimal> requiredIngredients = new LinkedHashMap<>();
        Map<Integer, Integer> requiredExtras = new LinkedHashMap<>();
        Map<Integer, Integer> requiredSizes = new LinkedHashMap<>();
        Map<Integer, Extra> loadedExtras = new HashMap<>();
        Map<Integer, Size> loadedSizes = new HashMap<>();

        List<ProductionEstimateResponseDTO.BatchLineEstimateDTO> lines = new ArrayList<>();
        int totalUnits = 0;
        BigDecimal totalDirectCost = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (ProductionEstimateRequestDTO.BatchItemRequest item : req.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BadRequestException("La cantidad de cada ítem del lote debe ser mayor a cero.");
            }
            int qty = item.getQuantity();
            BigDecimal qtyDec = BigDecimal.valueOf(qty);
            totalUnits += qty;

            if ("PIZZA".equalsIgnoreCase(item.getItemType())) {
                Pizza pizza = pizzaRepository.findById(item.getPizzaId())
                        .orElseThrow(() -> new ResourceNotFoundException("Pizza no encontrada: " + item.getPizzaId()));
                Size size = item.getSizeId() != null
                        ? loadedSizes.computeIfAbsent(item.getSizeId(), id -> sizeRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Tamaño no encontrado: " + id)))
                        : null;

                if (size != null) {
                    requiredSizes.merge(size.getId(), qty, Integer::sum);
                }

                BigDecimal multiplier = (size != null && size.getRecipeMultiplier() != null && size.getRecipeMultiplier().compareTo(BigDecimal.ZERO) > 0)
                        ? size.getRecipeMultiplier()
                        : BigDecimal.ONE;
                BigDecimal sizeExtra = (size != null && size.getExtraCost() != null) ? size.getExtraCost() : BigDecimal.ZERO;
                BigDecimal basePrice = pizza.getPrice() != null ? pizza.getPrice() : BigDecimal.ZERO;
                BigDecimal unitSalePrice = basePrice.add(sizeExtra);
                BigDecimal unitCost = BigDecimal.ZERO;

                List<PizzaIngredient> recipe = pizzaIngredientRepository.findByPizzaId(pizza.getId());
                for (PizzaIngredient pi : recipe) {
                    Integer ingId = pi.getId() != null ? pi.getId().getIngredientId() : null;
                    if (ingId == null) continue;
                    Ingredient ing = ingMap.get(ingId);
                    if (ing == null) continue;
                    BigDecimal perUnitQty = (pi.getQuantity() != null ? pi.getQuantity() : new BigDecimal("50.00"))
                            .multiply(multiplier);
                    requiredIngredients.merge(ingId, perUnitQty.multiply(qtyDec), BigDecimal::add);
                    BigDecimal ingCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
                    unitCost = unitCost.add(perUnitQty.multiply(ingCost));
                }

                if (item.getExtraIngredientIds() != null) {
                    for (Integer extraIngId : item.getExtraIngredientIds()) {
                        Ingredient ing = ingMap.get(extraIngId);
                        if (ing == null) continue;
                        BigDecimal portion = ing.getExtraPortionQuantity() != null ? ing.getExtraPortionQuantity() : new BigDecimal("50.00");
                        requiredIngredients.merge(extraIngId, portion.multiply(qtyDec), BigDecimal::add);
                        BigDecimal ingCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
                        unitCost = unitCost.add(portion.multiply(ingCost));
                        if (ing.getExtraCost() != null) {
                            unitSalePrice = unitSalePrice.add(ing.getExtraCost());
                        }
                    }
                }

                BigDecimal roundedUnitCost = unitCost.setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineCost = roundedUnitCost.multiply(qtyDec).setScale(2, RoundingMode.HALF_UP);
                BigDecimal roundedSale = unitSalePrice.setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineRevenue = roundedSale.multiply(qtyDec).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineProfit = lineRevenue.subtract(lineCost).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineMargin = lineRevenue.compareTo(BigDecimal.ZERO) > 0
                        ? lineProfit.multiply(new BigDecimal("100")).divide(lineRevenue, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                ProductionEstimateResponseDTO.BatchLineEstimateDTO lineDto = new ProductionEstimateResponseDTO.BatchLineEstimateDTO();
                lineDto.setItemType("PIZZA");
                lineDto.setItemName(size != null ? pizza.getName() + " (" + size.getName() + ")" : pizza.getName());
                lineDto.setQuantity(qty);
                lineDto.setUnitDirectCost(roundedUnitCost);
                lineDto.setTotalDirectCost(lineCost);
                lineDto.setUnitSalePrice(roundedSale);
                lineDto.setProjectedRevenue(lineRevenue);
                lineDto.setProjectedGrossProfit(lineProfit);
                lineDto.setProfitMargin(lineMargin);
                lines.add(lineDto);

                totalDirectCost = totalDirectCost.add(lineCost);
                totalRevenue = totalRevenue.add(lineRevenue);

            } else if ("EXTRA".equalsIgnoreCase(item.getItemType())) {
                Extra extra = loadedExtras.computeIfAbsent(item.getExtraId(), id ->
                        extraRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Extra no encontrado: " + id)));
                requiredExtras.merge(extra.getId(), qty, Integer::sum);

                BigDecimal roundedUnitCost = (extra.getUnitCost() != null ? extra.getUnitCost() : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineCost = roundedUnitCost.multiply(qtyDec).setScale(2, RoundingMode.HALF_UP);
                BigDecimal roundedSale = (extra.getPrice() != null ? extra.getPrice() : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineRevenue = roundedSale.multiply(qtyDec).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineProfit = lineRevenue.subtract(lineCost).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineMargin = lineRevenue.compareTo(BigDecimal.ZERO) > 0
                        ? lineProfit.multiply(new BigDecimal("100")).divide(lineRevenue, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                ProductionEstimateResponseDTO.BatchLineEstimateDTO lineDto = new ProductionEstimateResponseDTO.BatchLineEstimateDTO();
                lineDto.setItemType("EXTRA");
                lineDto.setItemName(extra.getName());
                lineDto.setQuantity(qty);
                lineDto.setUnitDirectCost(roundedUnitCost);
                lineDto.setTotalDirectCost(lineCost);
                lineDto.setUnitSalePrice(roundedSale);
                lineDto.setProjectedRevenue(lineRevenue);
                lineDto.setProjectedGrossProfit(lineProfit);
                lineDto.setProfitMargin(lineMargin);
                lines.add(lineDto);

                totalDirectCost = totalDirectCost.add(lineCost);
                totalRevenue = totalRevenue.add(lineRevenue);
            } else {
                throw new BadRequestException("itemType inválido en lote: " + item.getItemType());
            }
        }

        List<ProductionEstimateResponseDTO.MaterialRequirementDTO> materials = new ArrayList<>();
        BigDecimal totalMissingCost = BigDecimal.ZERO;
        boolean allSufficient = true;

        for (Map.Entry<Integer, BigDecimal> entry : requiredIngredients.entrySet()) {
            Ingredient ing = ingMap.get(entry.getKey());
            if (ing == null) continue;
            BigDecimal reqQty = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            BigDecimal avail = (ing.getStockQuantity() != null ? ing.getStockQuantity() : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal missing = reqQty.compareTo(avail) > 0 ? reqQty.subtract(avail) : BigDecimal.ZERO;
            BigDecimal uCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;
            BigDecimal directCost = reqQty.multiply(uCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal missingCost = missing.multiply(uCost).setScale(2, RoundingMode.HALF_UP);
            boolean sufficient = missing.compareTo(BigDecimal.ZERO) == 0 && ing.isAvailable();
            if (!sufficient) allSufficient = false;
            totalMissingCost = totalMissingCost.add(missingCost);

            ProductionEstimateResponseDTO.MaterialRequirementDTO mat = new ProductionEstimateResponseDTO.MaterialRequirementDTO();
            mat.setItemType("INGREDIENT");
            mat.setItemId(ing.getId());
            mat.setName(ing.getName());
            mat.setUnit(ing.getUnit() != null ? ing.getUnit() : "g");
            mat.setUnitCost(uCost);
            mat.setRequiredQuantity(reqQty);
            mat.setAvailableStock(avail);
            mat.setMissingQuantity(missing);
            mat.setTotalDirectCost(directCost);
            mat.setMissingPurchaseCost(missingCost);
            mat.setSufficient(sufficient);
            materials.add(mat);
        }

        for (Map.Entry<Integer, Integer> entry : requiredExtras.entrySet()) {
            Extra ext = loadedExtras.get(entry.getKey());
            if (ext == null) continue;
            BigDecimal reqQty = BigDecimal.valueOf(entry.getValue());
            BigDecimal avail = BigDecimal.valueOf(ext.getStockQuantity() != null ? ext.getStockQuantity() : 0);
            BigDecimal missing = reqQty.compareTo(avail) > 0 ? reqQty.subtract(avail) : BigDecimal.ZERO;
            BigDecimal uCost = ext.getUnitCost() != null ? ext.getUnitCost() : BigDecimal.ZERO;
            BigDecimal directCost = reqQty.multiply(uCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal missingCost = missing.multiply(uCost).setScale(2, RoundingMode.HALF_UP);
            boolean sufficient = missing.compareTo(BigDecimal.ZERO) == 0 && Boolean.TRUE.equals(ext.getIsAvailable());
            if (!sufficient) allSufficient = false;
            totalMissingCost = totalMissingCost.add(missingCost);

            ProductionEstimateResponseDTO.MaterialRequirementDTO mat = new ProductionEstimateResponseDTO.MaterialRequirementDTO();
            mat.setItemType("EXTRA");
            mat.setItemId(ext.getId());
            mat.setName(ext.getName());
            mat.setUnit("und");
            mat.setUnitCost(uCost);
            mat.setRequiredQuantity(reqQty);
            mat.setAvailableStock(avail);
            mat.setMissingQuantity(missing);
            mat.setTotalDirectCost(directCost);
            mat.setMissingPurchaseCost(missingCost);
            mat.setSufficient(sufficient);
            materials.add(mat);
        }

        for (Map.Entry<Integer, Integer> entry : requiredSizes.entrySet()) {
            Size sz = loadedSizes.get(entry.getKey());
            if (sz == null) continue;
            BigDecimal reqQty = BigDecimal.valueOf(entry.getValue());
            BigDecimal avail = BigDecimal.valueOf(sz.getStockQuantity() != null ? sz.getStockQuantity() : 0);
            BigDecimal missing = reqQty.compareTo(avail) > 0 ? reqQty.subtract(avail) : BigDecimal.ZERO;
            boolean sufficient = missing.compareTo(BigDecimal.ZERO) == 0;
            if (!sufficient) allSufficient = false;

            ProductionEstimateResponseDTO.MaterialRequirementDTO mat = new ProductionEstimateResponseDTO.MaterialRequirementDTO();
            mat.setItemType("SIZE");
            mat.setItemId(sz.getId());
            mat.setName("Masa " + sz.getName());
            mat.setUnit("und");
            mat.setUnitCost(BigDecimal.ZERO);
            mat.setRequiredQuantity(reqQty);
            mat.setAvailableStock(avail);
            mat.setMissingQuantity(missing);
            mat.setTotalDirectCost(BigDecimal.ZERO);
            mat.setMissingPurchaseCost(BigDecimal.ZERO);
            mat.setSufficient(sufficient);
            materials.add(mat);
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalDirectCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal marginPercent = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.multiply(new BigDecimal("100")).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ProductionEstimateResponseDTO res = new ProductionEstimateResponseDTO();
        res.setTotalUnits(totalUnits);
        res.setTotalProductionCost(totalDirectCost.setScale(2, RoundingMode.HALF_UP));
        res.setProjectedRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP));
        res.setProjectedGrossProfit(grossProfit);
        res.setProjectedMarginPercent(marginPercent);
        res.setTotalMissingPurchaseCost(totalMissingCost.setScale(2, RoundingMode.HALF_UP));
        res.setCanProduceWithCurrentStock(allSufficient);
        res.setLines(lines);
        res.setMaterials(materials);
        return res;
    }
}

