import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pagination.html',
  styleUrls: ['./pagination.css']
})
export class PaginationComponent implements OnChanges {
  @Input() currentPage: number = 1;
  @Input() totalItems: number = 0;
  @Input() itemsPerPage: number = 10;
  @Input() pageSizeOptions: number[] = [10, 25, 50];
  @Input() showPageSizeSelector: boolean = true;

  @Output() pageChange = new EventEmitter<number>();
  @Output() itemsPerPageChange = new EventEmitter<number>();

  totalPages: number = 0;
  pages: number[] = [];
  rangeStart: number = 0;
  rangeEnd: number = 0;
  effectivePageSizeOptions: number[] = [10, 25, 50];

  ngOnChanges(_changes?: SimpleChanges): void {
    this.recompute();
  }

  get canChangePageSize(): boolean {
    return this.showPageSizeSelector && this.itemsPerPageChange.observed;
  }

  private recompute(): void {
    const safePerPage = this.itemsPerPage > 0 ? this.itemsPerPage : 10;
    const safeTotal = Math.max(0, this.totalItems || 0);

    this.totalPages = safeTotal > 0 ? Math.ceil(safeTotal / safePerPage) : 0;
    const clampedPage = this.totalPages > 0 ? Math.min(Math.max(1, this.currentPage), this.totalPages) : 1;

    this.rangeStart = safeTotal === 0 ? 0 : (clampedPage - 1) * safePerPage + 1;
    this.rangeEnd = safeTotal === 0 ? 0 : Math.min(clampedPage * safePerPage, safeTotal);

    const baseOpts = Array.isArray(this.pageSizeOptions) && this.pageSizeOptions.length > 0
      ? this.pageSizeOptions
      : [10, 25, 50];
    this.effectivePageSizeOptions = baseOpts.includes(safePerPage)
      ? baseOpts
      : [...baseOpts, safePerPage].sort((a, b) => a - b);

    this.pages = this.buildPages(clampedPage, this.totalPages);
  }

  private buildPages(current: number, total: number): number[] {
    if (total <= 1) return total === 1 ? [1] : [];
    const result: number[] = [];
    const maxPagesToShow = 5;

    if (total <= maxPagesToShow) {
      for (let i = 1; i <= total; i++) {
        result.push(i);
      }
    } else if (current <= 3) {
      for (let i = 1; i <= 4; i++) {
        result.push(i);
      }
      result.push(-1);
      result.push(total);
    } else if (current >= total - 2) {
      result.push(1);
      result.push(-1);
      for (let i = total - 3; i <= total; i++) {
        result.push(i);
      }
    } else {
      result.push(1);
      result.push(-1);
      result.push(current - 1);
      result.push(current);
      result.push(current + 1);
      result.push(-1);
      result.push(total);
    }

    return result;
  }

  changePage(page: number): void {
    if (this.totalPages === 0) this.recompute();
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.changePage(this.currentPage - 1);
    }
  }

  nextPage(): void {
    if (this.totalPages === 0) this.recompute();
    if (this.currentPage < this.totalPages) {
      this.changePage(this.currentPage + 1);
    }
  }

  onPageSizeSelect(newSize: number | string): void {
    const parsed = Number(newSize);
    if (!Number.isNaN(parsed) && parsed > 0 && parsed !== this.itemsPerPage) {
      this.itemsPerPageChange.emit(parsed);
    }
  }

  trackByPage(index: number, page: number): string {
    return page === -1 ? `ellipsis-${index}` : `page-${page}`;
  }
}
