import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-sobre-nosotros',
  imports: [CommonModule],
  templateUrl: './sobre-nosotros.html',
  styleUrl: './sobre-nosotros.css'
})
export class SobreNosotros implements OnInit, OnDestroy {
  isSwapped = false;
  private intervalId: any;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.intervalId = setInterval(() => {
        this.isSwapped = !this.isSwapped;
      }, 4000);
    }
  }

  ngOnDestroy() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }
}
