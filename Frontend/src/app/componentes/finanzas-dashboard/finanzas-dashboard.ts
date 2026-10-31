import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { saveAs } from 'file-saver';
import { FinancialDashboardService } from '../../services/financial-dashboard.service';
import { ToastService } from '../../services/toast.service';
import { PaginationComponent } from '../pagination/pagination';
import {
  ProfitLossDashboardDTO,
  ProfitLossTimelinePointDTO
} from '../../models/admin.interface';

type PeriodPreset = 'ALL' | 'TODAY' | '7D' | '30D' | 'MONTH' | 'YEAR' | 'CUSTOM';
type ProductRankingRow = ProfitLossDashboardDTO['productRanking'][number];
type LossCategoryBreakdownRow = ProfitLossDashboardDTO['lossesByCategory'][number];

@Component({
  selector: 'app-finanzas-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './finanzas-dashboard.html',
  styleUrls: ['./finanzas-dashboard.css']
})
export class FinanzasDashboardComponent implements OnInit {
  loading = false;
  exporting = false;
  errorMessage = '';
  hasLoadedOnce = false;

  selectedPreset: PeriodPreset = 'ALL';
  customStartDate = '';
  customEndDate = '';
  groupBy: 'DAY' | 'MONTH' = 'DAY';
  onlyDelivered = false;

  // Métricas precalculadas para evitar O(N²) durante change detection
  maxTimelineMagnitude = 1;
  cogsPercentOfRevenue = 0;
  lossesPercentOfRevenue = 0;

  // Filtro y paginación de Ranking de Rentabilidad por Producto
  rankingSearch = '';
  rankingPage = 1;
  rankingPerPage = 10;
  filteredProductRanking: ProductRankingRow[] = [];
  paginatedProductRanking: ProductRankingRow[] = [];

  report: ProfitLossDashboardDTO = {
    startDate: '',
    endDate: '',
    groupBy: 'DAY',
    validOrdersCount: 0,
    cancelledOrdersCount: 0,
    lossesCount: 0,
    grossSalesSubtotal: 0,
    deliveryFeesCollected: 0,
    totalDiscounts: 0,
    netRevenue: 0,
    costOfGoodsSold: 0,
    grossProfit: 0,
    grossMarginPercent: 0,
    totalProductLosses: 0,
    netProfit: 0,
    netMarginPercent: 0,
    averageTicket: 0,
    timeline: [],
    productRanking: [],
    lossesByCategory: []
  };

  constructor(
    private readonly financialService: FinancialDashboardService,
    private readonly toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.cargarReporte();
  }

  private toLocalIsoDate(date: Date): string {
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 10);
  }

  seleccionarPreset(preset: PeriodPreset): void {
    this.selectedPreset = preset;
    const now = new Date();

    if (preset === 'ALL') {
      this.customStartDate = '';
      this.customEndDate = '';
      this.cargarReporte();
      return;
    }

    if (preset === 'CUSTOM') {
      if (!this.customEndDate) {
        this.customEndDate = this.toLocalIsoDate(now);
      }
      if (!this.customStartDate) {
        const start = new Date(now);
        start.setDate(now.getDate() - 30);
        this.customStartDate = this.toLocalIsoDate(start);
      }
      this.cargarReporte();
      return;
    }

    const endStr = this.toLocalIsoDate(now);
    let startStr = endStr;

    if (preset === 'TODAY') {
      startStr = endStr;
      this.groupBy = 'DAY';
    } else if (preset === '7D') {
      const d = new Date(now);
      d.setDate(now.getDate() - 6);
      startStr = this.toLocalIsoDate(d);
      this.groupBy = 'DAY';
    } else if (preset === '30D') {
      const d = new Date(now);
      d.setDate(now.getDate() - 29);
      startStr = this.toLocalIsoDate(d);
      this.groupBy = 'DAY';
    } else if (preset === 'MONTH') {
      const d = new Date(now.getFullYear(), now.getMonth(), 1);
      startStr = this.toLocalIsoDate(d);
      this.groupBy = 'DAY';
    } else if (preset === 'YEAR') {
      const d = new Date(now.getFullYear(), 0, 1);
      startStr = this.toLocalIsoDate(d);
      this.groupBy = 'MONTH';
    }

    this.customStartDate = startStr;
    this.customEndDate = endStr;
    this.cargarReporte();
  }

  cambiarAgrupacion(mode: 'DAY' | 'MONTH'): void {
    this.groupBy = mode;
    this.cargarReporte();
  }

  reintentarCarga(): void {
    this.cargarReporte();
  }

  cargarReporte(): void {
    this.loading = true;
    this.errorMessage = '';
    const startDate =
      this.selectedPreset === 'ALL' ? undefined : this.customStartDate || undefined;
    const endDate =
      this.selectedPreset === 'ALL' ? undefined : this.customEndDate || undefined;

    this.financialService
      .obtenerEstadoResultados({
        startDate,
        endDate,
        groupBy: this.groupBy,
        onlyDelivered: this.onlyDelivered
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (data) => {
          this.report = data;
          this.hasLoadedOnce = true;
          this.rankingPage = 1;
          this.recalcularMetricasReporte();
        },
        error: (err) => {
          this.errorMessage =
            err?.status === 0
              ? 'No hay conexión con el servidor (Backend desconectado). Verifique que el servicio esté activo.'
              : err?.error?.message || 'Error al cargar el estado de pérdidas y ganancias.';
        }
      });
  }

  private recalcularMetricasReporte(): void {
    const timeline = this.report?.timeline || [];
    let maxVal = 1;
    for (const pt of timeline) {
      maxVal = Math.max(
        maxVal,
        Math.abs(Number(pt.revenue ?? 0)),
        Math.abs(Number(pt.cogs ?? 0)),
        Math.abs(Number(pt.losses ?? 0)),
        Math.abs(Number(pt.netProfit ?? 0))
      );
    }
    this.maxTimelineMagnitude = maxVal;

    const netRev = Number(this.report?.netRevenue ?? 0);
    this.cogsPercentOfRevenue =
      netRev > 0 ? (Number(this.report?.costOfGoodsSold ?? 0) / netRev) * 100 : 0;
    this.lossesPercentOfRevenue =
      netRev > 0 ? (Number(this.report?.totalProductLosses ?? 0) / netRev) * 100 : 0;

    this.recalcularRanking();
  }

  recalcularRanking(): void {
    const source = this.report?.productRanking || [];
    const term = this.rankingSearch.trim().toLowerCase();
    const filtered = term
      ? source.filter(
          (prod) =>
            (prod.itemName || '').toLowerCase().includes(term) ||
            (prod.itemType || '').toLowerCase().includes(term)
        )
      : [...source];

    this.filteredProductRanking = filtered;

    const totalPages = Math.max(1, Math.ceil(filtered.length / this.rankingPerPage));
    if (this.rankingPage > totalPages) {
      this.rankingPage = totalPages;
    }
    const start = (this.rankingPage - 1) * this.rankingPerPage;
    this.paginatedProductRanking = filtered.slice(start, start + this.rankingPerPage);
  }

  onRankingSearchChange(): void {
    this.rankingPage = 1;
    this.recalcularRanking();
  }

  onRankingPageChange(page: number): void {
    this.rankingPage = page;
    this.recalcularRanking();
  }

  onRankingPerPageChange(size: number): void {
    this.rankingPerPage = size;
    this.rankingPage = 1;
    this.recalcularRanking();
  }

  getBarHeight(value: number): number {
    const abs = Math.abs(Number(value ?? 0));
    if (abs <= 0) return 4;
    return Math.max(6, Math.min(100, (abs / this.maxTimelineMagnitude) * 100));
  }

  formatTimelinePeriod(pt: ProfitLossTimelinePointDTO): string {
    if (!pt.period) return '';
    if (pt.period.length === 7) {
      // YYYY-MM
      const [y, m] = pt.period.split('-');
      return `${m}/${y}`;
    }
    if (pt.period.length === 10) {
      // YYYY-MM-DD
      const [, m, d] = pt.period.split('-');
      return `${d}/${m}`;
    }
    return pt.period;
  }

  trackByTimeline(index: number, pt: ProfitLossTimelinePointDTO): string {
    return pt.period || String(index);
  }

  trackByProductRanking(index: number, prod: ProductRankingRow): string {
    return `${prod.itemType}-${prod.itemName}-${index}`;
  }

  trackByLossCategory(index: number, cat: LossCategoryBreakdownRow): string {
    return cat.category || cat.label || String(index);
  }

  async exportarExcel(): Promise<void> {
    return this.exportToExcel();
  }

  async exportToExcel(): Promise<void> {
    if (this.exporting) return;
    try {
      this.exporting = true;
      const ExcelJS = await import('exceljs');
      const WorkbookCtor =
        ExcelJS.Workbook ??
        (ExcelJS as unknown as { default: { Workbook: typeof ExcelJS.Workbook } }).default.Workbook;
      const workbook = new WorkbookCtor();
      workbook.creator = 'Pizza Hut MVP - Finanzas';
      workbook.created = new Date();

      // ==================== HOJA 1: ESTADO DE RESULTADOS (P&L) ====================
      const wsPl = workbook.addWorksheet('Estado de Resultados');
      wsPl.mergeCells('A1:C1');
      const title = wsPl.getCell('A1');
      title.value = 'ESTADO DE PÉRDIDAS Y GANANCIAS (P&L) - PIZZA HUT';
      title.font = { name: 'Arial', size: 14, bold: true, color: { argb: 'FFFFFFFF' } };
      title.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFC8102E' } };
      title.alignment = { vertical: 'middle', horizontal: 'center' };
      wsPl.getRow(1).height = 28;

      wsPl.mergeCells('A2:C2');
      const sub = wsPl.getCell('A2');
      const periodoTxt =
        this.selectedPreset === 'ALL'
          ? 'Todo el historial'
          : `${this.report.startDate || this.customStartDate} al ${this.report.endDate || this.customEndDate}`;
      sub.value = `Período: ${periodoTxt} | Pedidos válidos: ${this.report.validOrdersCount} | Generado: ${new Date().toLocaleString()}`;
      sub.font = { name: 'Arial', size: 10, italic: true, color: { argb: 'FF555555' } };
      sub.alignment = { vertical: 'middle', horizontal: 'center' };

      wsPl.columns = [
        { key: 'concepto', width: 48 },
        { key: 'monto', width: 22 },
        { key: 'porcentaje', width: 18 }
      ];

      const hdr = wsPl.getRow(4);
      hdr.values = ['Concepto Financiero', 'Monto (S/)', '% sobre Ventas'];
      hdr.font = { bold: true, color: { argb: 'FFFFFFFF' } };
      hdr.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF111827' } };

      const rev = Number(this.report.netRevenue || 0);
      const pct = (val: number) => (rev > 0 ? `${((val / rev) * 100).toFixed(2)}%` : '0.00%');

      const plRows: Array<[string, number, string, boolean]> = [
        ['(+) Subtotal Ventas Brutas', Number(this.report.grossSalesSubtotal), pct(Number(this.report.grossSalesSubtotal)), false],
        ['(+) Tarifas de Delivery Cobradas', Number(this.report.deliveryFeesCollected), pct(Number(this.report.deliveryFeesCollected)), false],
        ['(-) Descuentos y Promociones Aplicadas', -Number(this.report.totalDiscounts), pct(-Number(this.report.totalDiscounts)), false],
        ['(=) INGRESOS NETOS POR VENTAS', Number(this.report.netRevenue), '100.00%', true],
        ['(-) Costo Directo de lo Vendido (COGS)', -Number(this.report.costOfGoodsSold), pct(-Number(this.report.costOfGoodsSold)), false],
        ['(=) UTILIDAD / GANANCIA BRUTA', Number(this.report.grossProfit), `${Number(this.report.grossMarginPercent).toFixed(2)}%`, true],
        ['(-) Pérdidas de Productos y Mermas', -Number(this.report.totalProductLosses), pct(-Number(this.report.totalProductLosses)), false],
        ['(=) RESULTADO NETO DEL PERÍODO (GANANCIA / PÉRDIDA)', Number(this.report.netProfit), `${Number(this.report.netMarginPercent).toFixed(2)}%`, true]
      ];

      plRows.forEach(([concepto, monto, porcentaje, highlight]) => {
        const r = wsPl.addRow({ concepto, monto, porcentaje });
        r.getCell(2).numFmt = '"S/" #,##0.00;[Red]-"S/" #,##0.00';
        r.getCell(3).alignment = { horizontal: 'right' };
        if (highlight) {
          r.font = { bold: true };
          r.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF3F4F6' } };
        }
      });

      // ==================== HOJA 2: EVOLUCIÓN TEMPORAL ====================
      const wsTime = workbook.addWorksheet('Evolucion Temporal');
      wsTime.columns = [
        { header: 'Período', key: 'period', width: 16 },
        { header: 'Pedidos', key: 'ordersCount', width: 12 },
        { header: 'Ingresos Netos (S/)', key: 'revenue', width: 20 },
        { header: 'Costo Directo COGS (S/)', key: 'cogs', width: 22 },
        { header: 'Ganancia Bruta (S/)', key: 'grossProfit', width: 20 },
        { header: 'Pérdidas / Mermas (S/)', key: 'losses', width: 22 },
        { header: 'Resultado Neto (S/)', key: 'netProfit', width: 20 }
      ];
      wsTime.getRow(1).font = { bold: true };
      (this.report.timeline || []).forEach((pt) => {
        const r = wsTime.addRow(pt);
        [3, 4, 5, 6, 7].forEach((c) => (r.getCell(c).numFmt = '"S/" #,##0.00;[Red]-"S/" #,##0.00'));
      });

      // ==================== HOJA 3: RENTABILIDAD POR PRODUCTO ====================
      const wsProd = workbook.addWorksheet('Rentabilidad Productos');
      wsProd.columns = [
        { header: 'Tipo', key: 'itemType', width: 14 },
        { header: 'Producto', key: 'itemName', width: 36 },
        { header: 'Unidades Vendidas', key: 'unitsSold', width: 18 },
        { header: 'Ingresos (S/)', key: 'revenue', width: 18 },
        { header: 'Costo Directo (S/)', key: 'cogs', width: 18 },
        { header: 'Ganancia (S/)', key: 'profit', width: 18 },
        { header: 'Margen (%)', key: 'marginPercent', width: 14 }
      ];
      wsProd.getRow(1).font = { bold: true };
      (this.report.productRanking || []).forEach((pr) => {
        const r = wsProd.addRow(pr);
        [4, 5, 6].forEach((c) => (r.getCell(c).numFmt = '"S/" #,##0.00;[Red]-"S/" #,##0.00'));
      });

      // ==================== HOJA 4: DESGLOSE DE PÉRDIDAS ====================
      const wsLoss = workbook.addWorksheet('Desglose Perdidas');
      wsLoss.columns = [
        { header: 'Código', key: 'category', width: 20 },
        { header: 'Categoría de Merma', key: 'label', width: 34 },
        { header: 'Incidentes', key: 'count', width: 14 },
        { header: 'Costo Perdido (S/)', key: 'totalCost', width: 20 },
        { header: 'Participación (%)', key: 'percentage', width: 18 }
      ];
      wsLoss.getRow(1).font = { bold: true };
      (this.report.lossesByCategory || []).forEach((lc) => {
        const r = wsLoss.addRow(lc);
        r.getCell(4).numFmt = '"S/" #,##0.00';
      });

      const buffer = await workbook.xlsx.writeBuffer();
      const blob = new Blob([buffer], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      });
      saveAs(blob, `Estado_Perdidas_Ganancias_PizzaHut_${Date.now()}.xlsx`);
      this.toastService.showSuccess('Estado de Pérdidas y Ganancias exportado a Excel');
    } catch {
      this.toastService.showError('No se pudo generar el archivo Excel.');
    } finally {
      this.exporting = false;
    }
  }
}
