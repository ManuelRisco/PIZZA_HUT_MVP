package com.example.services;

import com.example.dtos.ProfitLossDashboardDTO;
import com.example.models.Extra;
import com.example.models.Order;
import com.example.models.OrderItem;
import com.example.models.Pizza;
import com.example.models.ProductLoss;
import com.example.models.Size;
import com.example.repositories.ExtraRepository;
import com.example.repositories.OrderItemRepository;
import com.example.repositories.OrderRepository;
import com.example.repositories.PizzaRepository;
import com.example.repositories.ProductLossRepository;
import com.example.repositories.SizeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FinancialDashboardService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductLossRepository productLossRepository;
    private final PizzaRepository pizzaRepository;
    private final ExtraRepository extraRepository;
    private final SizeRepository sizeRepository;

    public FinancialDashboardService(OrderRepository orderRepository,
                                     OrderItemRepository orderItemRepository,
                                     ProductLossRepository productLossRepository,
                                     PizzaRepository pizzaRepository,
                                     ExtraRepository extraRepository,
                                     SizeRepository sizeRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productLossRepository = productLossRepository;
        this.pizzaRepository = pizzaRepository;
        this.extraRepository = extraRepository;
        this.sizeRepository = sizeRepository;
    }

    public ProfitLossDashboardDTO getProfitLossReport(LocalDate startDate,
                                                      LocalDate endDate,
                                                      String groupBy,
                                                      boolean onlyDelivered) {
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        boolean byMonth = "MONTH".equalsIgnoreCase(groupBy);
        DateTimeFormatter periodFormatter = byMonth ? MONTH_FMT : DAY_FMT;

        List<Order> allOrders = orderRepository.findAll().stream()
                .filter(o -> isWithinRange(o.getCreatedAt(), startDt, endDt))
                .toList();

        List<ProductLoss> allLosses = productLossRepository.findAll().stream()
                .filter(l -> isWithinRange(l.getLossDate(), startDt, endDt))
                .toList();

        int validOrdersCount = 0;
        int cancelledOrdersCount = 0;
        BigDecimal grossSalesSubtotal = BigDecimal.ZERO;
        BigDecimal deliveryFeesCollected = BigDecimal.ZERO;
        BigDecimal totalDiscounts = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;

        Map<Integer, Order> validOrderMap = new HashMap<>();
        Map<String, ProfitLossDashboardDTO.TimelinePointDTO> timelineMap = new TreeMap<>();

        for (Order o : allOrders) {
            if (o.getStatus() == Order.OrderStatus.CANCELLED) {
                cancelledOrdersCount++;
                continue;
            }
            if (onlyDelivered && o.getStatus() != Order.OrderStatus.DELIVERED) {
                continue;
            }
            validOrdersCount++;
            validOrderMap.put(o.getId(), o);

            BigDecimal sub = o.getSubtotal() != null ? o.getSubtotal() : BigDecimal.ZERO;
            BigDecimal fee = o.getDeliveryFee() != null ? o.getDeliveryFee() : BigDecimal.ZERO;
            BigDecimal disc = o.getDiscount() != null ? o.getDiscount() : BigDecimal.ZERO;
            BigDecimal tot = o.getTotal() != null ? o.getTotal() : sub.add(fee).subtract(disc);

            grossSalesSubtotal = grossSalesSubtotal.add(sub);
            deliveryFeesCollected = deliveryFeesCollected.add(fee);
            totalDiscounts = totalDiscounts.add(disc);
            netRevenue = netRevenue.add(tot);

            String periodKey = formatPeriod(o.getCreatedAt(), periodFormatter);
            ProfitLossDashboardDTO.TimelinePointDTO tp = timelineMap.computeIfAbsent(periodKey, k -> {
                ProfitLossDashboardDTO.TimelinePointDTO p = new ProfitLossDashboardDTO.TimelinePointDTO();
                p.setPeriod(k);
                return p;
            });
            tp.setOrdersCount(tp.getOrdersCount() + 1);
            tp.setRevenue(tp.getRevenue().add(tot));
        }

        Set<Integer> validOrderIds = validOrderMap.keySet();
        Map<Integer, Pizza> pizzaMap = pizzaRepository.findAll().stream()
                .collect(Collectors.toMap(Pizza::getId, Function.identity(), (a, b) -> a));
        Map<Integer, Extra> extraMap = extraRepository.findAll().stream()
                .collect(Collectors.toMap(Extra::getId, Function.identity(), (a, b) -> a));
        Map<Integer, Size> sizeMap = sizeRepository.findAll().stream()
                .collect(Collectors.toMap(Size::getId, Function.identity(), (a, b) -> a));

        BigDecimal totalCogs = BigDecimal.ZERO;
        Map<String, ProfitLossDashboardDTO.ProductProfitabilityDTO> productRankingMap = new HashMap<>();

        if (!validOrderIds.isEmpty()) {
            for (OrderItem item : orderItemRepository.findAll()) {
                if (item.getOrderId() == null || !validOrderIds.contains(item.getOrderId())) {
                    continue;
                }
                Order parentOrder = validOrderMap.get(item.getOrderId());
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                BigDecimal lineRevenue = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
                BigDecimal lineCost = item.getLineCost() != null ? item.getLineCost() : BigDecimal.ZERO;

                totalCogs = totalCogs.add(lineCost);

                String periodKey = formatPeriod(parentOrder.getCreatedAt(), periodFormatter);
                ProfitLossDashboardDTO.TimelinePointDTO tp = timelineMap.get(periodKey);
                if (tp != null) {
                    tp.setCogs(tp.getCogs().add(lineCost));
                }

                String itemType = "EXTRA".equalsIgnoreCase(item.getItemType()) ? "EXTRA" : "PIZZA";
                String prodKey;
                String prodName;
                if ("EXTRA".equals(itemType)) {
                    prodKey = "EXTRA_" + item.getExtraId();
                    Extra ext = item.getExtraId() != null ? extraMap.get(item.getExtraId()) : null;
                    prodName = ext != null ? ext.getName() : "Extra #" + item.getExtraId();
                } else {
                    prodKey = "PIZZA_" + item.getPizzaId() + "_" + item.getSizeId();
                    Pizza piz = item.getPizzaId() != null ? pizzaMap.get(item.getPizzaId()) : null;
                    Size sz = item.getSizeId() != null ? sizeMap.get(item.getSizeId()) : null;
                    String pName = piz != null ? piz.getName() : "Pizza #" + item.getPizzaId();
                    prodName = sz != null ? pName + " (" + sz.getName() + ")" : pName;
                }

                ProfitLossDashboardDTO.ProductProfitabilityDTO rank = productRankingMap.computeIfAbsent(prodKey, k -> {
                    ProfitLossDashboardDTO.ProductProfitabilityDTO r = new ProfitLossDashboardDTO.ProductProfitabilityDTO();
                    r.setItemType(itemType);
                    r.setItemName(prodName);
                    r.setUnitsSold(0);
                    r.setRevenue(BigDecimal.ZERO);
                    r.setCogs(BigDecimal.ZERO);
                    r.setProfit(BigDecimal.ZERO);
                    r.setMarginPercent(BigDecimal.ZERO);
                    return r;
                });
                rank.setUnitsSold(rank.getUnitsSold() + qty);
                rank.setRevenue(rank.getRevenue().add(lineRevenue));
                rank.setCogs(rank.getCogs().add(lineCost));
            }
        }

        // Procesar Pérdidas / Mermas
        BigDecimal totalLosses = BigDecimal.ZERO;
        Map<ProductLoss.LossCategory, Integer> lossCountByCat = new LinkedHashMap<>();
        Map<ProductLoss.LossCategory, BigDecimal> lossCostByCat = new LinkedHashMap<>();
        for (ProductLoss.LossCategory cat : ProductLoss.LossCategory.values()) {
            lossCountByCat.put(cat, 0);
            lossCostByCat.put(cat, BigDecimal.ZERO);
        }

        for (ProductLoss loss : allLosses) {
            BigDecimal cost = loss.getTotalLossCost() != null ? loss.getTotalLossCost() : BigDecimal.ZERO;
            totalLosses = totalLosses.add(cost);
            if (loss.getCategory() != null) {
                lossCountByCat.merge(loss.getCategory(), 1, Integer::sum);
                lossCostByCat.merge(loss.getCategory(), cost, BigDecimal::add);
            }

            String periodKey = formatPeriod(loss.getLossDate(), periodFormatter);
            ProfitLossDashboardDTO.TimelinePointDTO tp = timelineMap.computeIfAbsent(periodKey, k -> {
                ProfitLossDashboardDTO.TimelinePointDTO p = new ProfitLossDashboardDTO.TimelinePointDTO();
                p.setPeriod(k);
                return p;
            });
            tp.setLosses(tp.getLosses().add(cost));
        }

        // Calcular utilidad bruta y neta por punto en línea de tiempo
        List<ProfitLossDashboardDTO.TimelinePointDTO> timeline = new ArrayList<>();
        for (ProfitLossDashboardDTO.TimelinePointDTO tp : timelineMap.values()) {
            BigDecimal rev = tp.getRevenue().setScale(2, RoundingMode.HALF_UP);
            BigDecimal cg = tp.getCogs().setScale(2, RoundingMode.HALF_UP);
            BigDecimal ls = tp.getLosses().setScale(2, RoundingMode.HALF_UP);
            BigDecimal gp = rev.subtract(cg).setScale(2, RoundingMode.HALF_UP);
            BigDecimal np = gp.subtract(ls).setScale(2, RoundingMode.HALF_UP);
            tp.setRevenue(rev);
            tp.setCogs(cg);
            tp.setLosses(ls);
            tp.setGrossProfit(gp);
            tp.setNetProfit(np);
            timeline.add(tp);
        }

        // Ranking de productos por rentabilidad
        List<ProfitLossDashboardDTO.ProductProfitabilityDTO> productRanking = new ArrayList<>(productRankingMap.values());
        for (ProfitLossDashboardDTO.ProductProfitabilityDTO r : productRanking) {
            BigDecimal rev = r.getRevenue().setScale(2, RoundingMode.HALF_UP);
            BigDecimal cg = r.getCogs().setScale(2, RoundingMode.HALF_UP);
            BigDecimal pf = rev.subtract(cg).setScale(2, RoundingMode.HALF_UP);
            BigDecimal mg = rev.compareTo(BigDecimal.ZERO) > 0
                    ? pf.multiply(new BigDecimal("100")).divide(rev, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            r.setRevenue(rev);
            r.setCogs(cg);
            r.setProfit(pf);
            r.setMarginPercent(mg);
        }
        productRanking.sort(Comparator.comparing(ProfitLossDashboardDTO.ProductProfitabilityDTO::getProfit).reversed());

        // Desglose de pérdidas por categoría
        List<ProfitLossDashboardDTO.LossCategoryBreakdownDTO> lossesByCategory = new ArrayList<>();
        for (ProductLoss.LossCategory cat : ProductLoss.LossCategory.values()) {
            BigDecimal catCost = lossCostByCat.get(cat).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pct = totalLosses.compareTo(BigDecimal.ZERO) > 0
                    ? catCost.multiply(new BigDecimal("100")).divide(totalLosses, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            ProfitLossDashboardDTO.LossCategoryBreakdownDTO b = new ProfitLossDashboardDTO.LossCategoryBreakdownDTO();
            b.setCategory(cat.name());
            b.setLabel(getCategorySpanishLabel(cat));
            b.setCount(lossCountByCat.get(cat));
            b.setTotalCost(catCost);
            b.setPercentage(pct);
            lossesByCategory.add(b);
        }

        BigDecimal roundedNetRevenue = netRevenue.setScale(2, RoundingMode.HALF_UP);
        BigDecimal roundedCogs = totalCogs.setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossProfit = roundedNetRevenue.subtract(roundedCogs).setScale(2, RoundingMode.HALF_UP);
        BigDecimal roundedLosses = totalLosses.setScale(2, RoundingMode.HALF_UP);
        BigDecimal netProfit = grossProfit.subtract(roundedLosses).setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossMarginPct = roundedNetRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.multiply(new BigDecimal("100")).divide(roundedNetRevenue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal netMarginPct = roundedNetRevenue.compareTo(BigDecimal.ZERO) > 0
                ? netProfit.multiply(new BigDecimal("100")).divide(roundedNetRevenue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgTicket = validOrdersCount > 0
                ? roundedNetRevenue.divide(BigDecimal.valueOf(validOrdersCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ProfitLossDashboardDTO dto = new ProfitLossDashboardDTO();
        dto.setStartDate(startDate != null ? startDate.toString() : null);
        dto.setEndDate(endDate != null ? endDate.toString() : null);
        dto.setGroupBy(byMonth ? "MONTH" : "DAY");
        dto.setValidOrdersCount(validOrdersCount);
        dto.setCancelledOrdersCount(cancelledOrdersCount);
        dto.setLossesCount(allLosses.size());
        dto.setGrossSalesSubtotal(grossSalesSubtotal.setScale(2, RoundingMode.HALF_UP));
        dto.setDeliveryFeesCollected(deliveryFeesCollected.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalDiscounts(totalDiscounts.setScale(2, RoundingMode.HALF_UP));
        dto.setNetRevenue(roundedNetRevenue);
        dto.setCostOfGoodsSold(roundedCogs);
        dto.setGrossProfit(grossProfit);
        dto.setGrossMarginPercent(grossMarginPct);
        dto.setTotalProductLosses(roundedLosses);
        dto.setNetProfit(netProfit);
        dto.setNetMarginPercent(netMarginPct);
        dto.setAverageTicket(avgTicket);
        dto.setTimeline(timeline);
        dto.setProductRanking(productRanking);
        dto.setLossesByCategory(lossesByCategory);
        return dto;
    }

    private boolean isWithinRange(LocalDateTime dt, LocalDateTime start, LocalDateTime end) {
        if (dt == null) return false;
        if (start != null && dt.isBefore(start)) return false;
        if (end != null && dt.isAfter(end)) return false;
        return true;
    }

    private String formatPeriod(LocalDateTime dt, DateTimeFormatter formatter) {
        if (dt == null) return LocalDate.now().format(formatter);
        return dt.format(formatter);
    }

    private String getCategorySpanishLabel(ProductLoss.LossCategory category) {
        return switch (category) {
            case EXPIRED -> "Vencimiento / Caducidad";
            case DAMAGED -> "Dañado / Deteriorado";
            case KITCHEN_ERROR -> "Error en Cocina / Horno";
            case CANCELLED_ORDER -> "Pedido Cancelado en Preparación";
            case OTHER -> "Otras Mermas";
        };
    }
}

