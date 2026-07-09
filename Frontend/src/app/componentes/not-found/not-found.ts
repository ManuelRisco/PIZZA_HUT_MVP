import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="not-found-container">
      <div class="not-found-content">
        <div class="pizza-icon">ðŸ•</div>
        <h1>¡Oops! Página no encontrada</h1>
        <h2>Error 404</h2>
        <p>La página que buscas no existe, pero tenemos deliciosas pizzas esperándote.</p>

        <div class="action-buttons">
          <button class="btn-home" (click)="navegarA('/')">
            <i class="fas fa-home"></i> Ir al Inicio
          </button>
          <button class="btn-menu" (click)="navegarA('/menu')">
            <i class="fas fa-pizza-slice"></i> Ver Menú
          </button>
        </div>

        <div class="suggested-links">
          <h3>¿Quizás buscabas?</h3>
          <ul>
            <li><a (click)="navegarA('/menu')" href="javascript:void(0)">Menú</a></li>
            <li><a (click)="navegarA('/extras')" href="javascript:void(0)">Extras</a></li>
            <li><a (click)="navegarA('/promociones')" href="javascript:void(0)">Promociones</a></li>
            <li><a (click)="navegarA('/cart')" href="javascript:void(0)">Carrito</a></li>
            <li><a (click)="navegarA('/ubicacion')" href="javascript:void(0)">Ubicación</a></li>
          </ul>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .not-found-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }

    .not-found-content {
      text-align: center;
      background: white;
      padding: 40px;
      border-radius: 20px;
      box-shadow: 0 20px 40px rgba(0,0,0,0.1);
      max-width: 500px;
      width: 100%;
    }

    .pizza-icon {
      font-size: 80px;
      margin-bottom: 20px;
      display: inline-block;
      animation: spin 3s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    h1 {
      color: #333;
      margin-bottom: 10px;
      font-size: 28px;
    }

    h2 {
      color: #ff6b6b;
      margin-bottom: 20px;
      font-size: 48px;
      font-weight: bold;
    }

    p {
      color: #666;
      margin-bottom: 30px;
      line-height: 1.6;
    }

    .action-buttons {
      display: flex;
      gap: 15px;
      justify-content: center;
      margin-bottom: 30px;
      flex-wrap: wrap;
    }

    .btn-home, .btn-menu {
      padding: 12px 24px;
      border: none;
      border-radius: 25px;
      font-weight: bold;
      cursor: pointer;
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .btn-home {
      background: #ff6b6b;
      color: white;
    }

    .btn-home:hover {
      background: #ff5252;
      transform: translateY(-2px);
    }

    .btn-menu {
      background: #feca57;
      color: #333;
    }

    .btn-menu:hover {
      background: #ffb142;
      transform: translateY(-2px);
    }

    .suggested-links {
      border-top: 1px solid #eee;
      padding-top: 20px;
    }

    .suggested-links h3 {
      color: #333;
      margin-bottom: 15px;
      font-size: 18px;
    }

    .suggested-links ul {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .suggested-links li {
      margin-bottom: 8px;
    }

    .suggested-links a {
      color: #ff6b6b;
      text-decoration: none;
      font-weight: 500;
      cursor: pointer;
    }

    .suggested-links a:hover {
      text-decoration: underline;
    }

    /* Responsive */
    @media (max-width: 768px) {
      .not-found-content {
        padding: 30px 20px;
        width: 90%;
      }

      h1 { font-size: 24px; }
      h2 { font-size: 36px; }

      .action-buttons {
        flex-direction: column;
        align-items: stretch;
      }

      .btn-home, .btn-menu {
        width: 100%;
        max-width: none;
        justify-content: center;
      }
    }

    /* Alto Contraste */
    :host-context(html[data-contraste="alto"]) .not-found-content {
      background: #000;
      border: 2px solid #ffff00;
      box-shadow: none;
    }

    :host-context(html[data-contraste="alto"]) h1,
    :host-context(html[data-contraste="alto"]) h2,
    :host-context(html[data-contraste="alto"]) p,
    :host-context(html[data-contraste="alto"]) h3,
    :host-context(html[data-contraste="alto"]) .suggested-links a {
      color: #ffff00 !important;
      border: none !important;
    }

    :host-context(html[data-contraste="alto"]) .suggested-links {
      border-top-color: #ffff00;
    }

    :host-context(html[data-contraste="alto"]) .btn-home,
    :host-context(html[data-contraste="alto"]) .btn-menu {
      background: #000 !important;
      color: #ffff00 !important;
      border: 2px solid #ffff00 !important;
    }

    :host-context(html[data-contraste="alto"]) .btn-home:hover,
    :host-context(html[data-contraste="alto"]) .btn-menu:hover {
      background: #ffff00 !important;
      color: #000 !important;
    }
  `]
})
export class NotFoundComponent {

  constructor(private readonly router: Router) {}

  navegarA(ruta: string): void {
    this.router.navigate([ruta]);
  }
}

