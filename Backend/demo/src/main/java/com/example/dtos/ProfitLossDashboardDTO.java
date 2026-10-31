package com.example.dtos;

import java.math.BigDecimal;
import java.util.List;

public class ProfitLossDashboardDTO {
    private String startDate;
    private String endDate;
    private String groupBy;

    private int validOrdersCount;
    private int cancelledOrdersCount;
    private int lossesCount;

    private BigDecimal grossSalesSubtotal;
    private BigDecimal deliveryFeesCollected;
    private BigDecimal totalDiscounts;
    private BigDecimal netRevenue;
    private BigDecimal costOfGoodsSold;
    private BigDecimal grossProfit;
    private BigDecimal grossMarginPercent;
    private BigDecimal totalProductLosses;
    private BigDecimal netProfit;
    private BigDecimal netMarginPercent;
    private BigDecimal averageTicket;

    private List<TimelinePointDTO> timeline;
    private List<ProductProfitabilityDTO> productRanking;
    private List<LossCategoryBreakdownDTO> lossesByCategory;

    public static class TimelinePointDTO {
        private String period;
        private int ordersCount;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal cogs = BigDecimal.ZERO;
        private BigDecimal grossProfit = BigDecimal.ZERO;
        private BigDecimal losses = BigDecimal.ZERO;
        private BigDecimal netProfit = BigDecimal.ZERO;

        public TimelinePointDTO() {}

        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }

        public int getOrdersCount() { return ordersCount; }
        public void setOrdersCount(int ordersCount) { this.ordersCount = ordersCount; }

        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

        public BigDecimal getCogs() { return cogs; }
        public void setCogs(BigDecimal cogs) { this.cogs = cogs; }

        public BigDecimal getGrossProfit() { return grossProfit; }
        public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }

        public BigDecimal getLosses() { return losses; }
        public void setLosses(BigDecimal losses) { this.losses = losses; }

        public BigDecimal getNetProfit() { return netProfit; }
        public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    }

    public static class ProductProfitabilityDTO {
        private String itemType;
        private String itemName;
        private int unitsSold;
        private BigDecimal revenue;
        private BigDecimal cogs;
        private BigDecimal profit;
        private BigDecimal marginPercent;

        public ProductProfitabilityDTO() {}

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public int getUnitsSold() { return unitsSold; }
        public void setUnitsSold(int unitsSold) { this.unitsSold = unitsSold; }

        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

        public BigDecimal getCogs() { return cogs; }
        public void setCogs(BigDecimal cogs) { this.cogs = cogs; }

        public BigDecimal getProfit() { return profit; }
        public void setProfit(BigDecimal profit) { this.profit = profit; }

        public BigDecimal getMarginPercent() { return marginPercent; }
        public void setMarginPercent(BigDecimal marginPercent) { this.marginPercent = marginPercent; }
    }

    public static class LossCategoryBreakdownDTO {
        private String category;
        private String label;
        private int count;
        private BigDecimal totalCost;
        private BigDecimal percentage;

        public LossCategoryBreakdownDTO() {}

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

        public BigDecimal getPercentage() { return percentage; }
        public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    }

    public ProfitLossDashboardDTO() {}

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getGroupBy() { return groupBy; }
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }

    public int getValidOrdersCount() { return validOrdersCount; }
    public void setValidOrdersCount(int validOrdersCount) { this.validOrdersCount = validOrdersCount; }

    public int getCancelledOrdersCount() { return cancelledOrdersCount; }
    public void setCancelledOrdersCount(int cancelledOrdersCount) { this.cancelledOrdersCount = cancelledOrdersCount; }

    public int getLossesCount() { return lossesCount; }
    public void setLossesCount(int lossesCount) { this.lossesCount = lossesCount; }

    public BigDecimal getGrossSalesSubtotal() { return grossSalesSubtotal; }
    public void setGrossSalesSubtotal(BigDecimal grossSalesSubtotal) { this.grossSalesSubtotal = grossSalesSubtotal; }

    public BigDecimal getDeliveryFeesCollected() { return deliveryFeesCollected; }
    public void setDeliveryFeesCollected(BigDecimal deliveryFeesCollected) { this.deliveryFeesCollected = deliveryFeesCollected; }

    public BigDecimal getTotalDiscounts() { return totalDiscounts; }
    public void setTotalDiscounts(BigDecimal totalDiscounts) { this.totalDiscounts = totalDiscounts; }

    public BigDecimal getNetRevenue() { return netRevenue; }
    public void setNetRevenue(BigDecimal netRevenue) { this.netRevenue = netRevenue; }

    public BigDecimal getCostOfGoodsSold() { return costOfGoodsSold; }
    public void setCostOfGoodsSold(BigDecimal costOfGoodsSold) { this.costOfGoodsSold = costOfGoodsSold; }

    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }

    public BigDecimal getGrossMarginPercent() { return grossMarginPercent; }
    public void setGrossMarginPercent(BigDecimal grossMarginPercent) { this.grossMarginPercent = grossMarginPercent; }

    public BigDecimal getTotalProductLosses() { return totalProductLosses; }
    public void setTotalProductLosses(BigDecimal totalProductLosses) { this.totalProductLosses = totalProductLosses; }

    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }

    public BigDecimal getNetMarginPercent() { return netMarginPercent; }
    public void setNetMarginPercent(BigDecimal netMarginPercent) { this.netMarginPercent = netMarginPercent; }

    public BigDecimal getAverageTicket() { return averageTicket; }
    public void setAverageTicket(BigDecimal averageTicket) { this.averageTicket = averageTicket; }

    public List<TimelinePointDTO> getTimeline() { return timeline; }
    public void setTimeline(List<TimelinePointDTO> timeline) { this.timeline = timeline; }

    public List<ProductProfitabilityDTO> getProductRanking() { return productRanking; }
    public void setProductRanking(List<ProductProfitabilityDTO> productRanking) { this.productRanking = productRanking; }

    public List<LossCategoryBreakdownDTO> getLossesByCategory() { return lossesByCategory; }
    public void setLossesByCategory(List<LossCategoryBreakdownDTO> lossesByCategory) { this.lossesByCategory = lossesByCategory; }
}

