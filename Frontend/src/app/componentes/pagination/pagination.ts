import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pagination.html',
  styleUrls: ['./pagination.css']
})
export class PaginationComponent {
  @Input() currentPage: number = 1;
  @Input() totalItems: number = 0;
  @Input() itemsPerPage: number = 10;
  @Output() pageChange = new EventEmitter<number>();

  // Exponer Math para usarlo en el template
  Math = Math;

  get totalPages(): number {
    return Math.ceil(this.totalItems / this.itemsPerPage);
  }

  get pages(): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    
    if (this.totalPages <= maxPagesToShow) {
      for (let i = 1; i <= this.totalPages; i++) {
        pages.push(i);
      }
    } else if (this.currentPage <= 3) {
      for (let i = 1; i <= 4; i++) {
        pages.push(i);
      }
      pages.push(-1); // Ellipsis
      pages.push(this.totalPages);
    } else if (this.currentPage >= this.totalPages - 2) {
      pages.push(1);
      pages.push(-1); // Ellipsis
      for (let i = this.totalPages - 3; i <= this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      pages.push(1);
      pages.push(-1); // Ellipsis
      pages.push(this.currentPage - 1);
      pages.push(this.currentPage);
      pages.push(this.currentPage + 1);
      pages.push(-1); // Ellipsis
      pages.push(this.totalPages);
    }
    
    return pages;
  }

  changePage(page: number): void {
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
    if (this.currentPage < this.totalPages) {
      this.changePage(this.currentPage + 1);
    }
  }
}
