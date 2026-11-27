import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="not-found-container">
      <div class="not-found-content">
        <div class="eco-logo-badge">
          <img src="/logo.webp" alt="Logo Eco Pizza" />
        </div>
        <h1>¡Oops! Página no encontrada</h1>
        <h2>Error 404</h2>
        <p>La página que buscas no existe, pero en Eco Pizza tenemos deliciosas pizzas esperándote.</p>

        <div class="action-buttons">
          <button type="button" class="btn-home" (click)="navegarA('/')">
            <i class="fas fa-home" aria-hidden="true"></i> Ir al Inicio
          </button>
          <button type="button" class="btn-menu" (click)="navegarA('/menu')">
            <i class="fas fa-pizza-slice" aria-hidden="true"></i> Ver Menú
          </button>
        </div>

        <div class="suggested-links">
          <h3>¿Quizás buscabas?</h3>
          <ul>
            <li><a routerLink="/menu">Menú</a></li>
            <li><a routerLink="/extras">Extras</a></li>
            <li><a routerLink="/promociones">Promociones</a></li>
            <li><a routerLink="/cart">Carrito</a></li>
            <li><a routerLink="/ubicacion">Ubicación</a></li>
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
      background: #f6fbf7;
    }

    .not-found-content {
      text-align: center;
      background: white;
      padding: 40px;
      border-radius: 20px;
      border-top: 4px solid #039340;
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
      max-width: 500px;
      width: 100%;
    }

    .eco-logo-badge {
      width: 96px;
      height: 96px;
      margin: 0 auto 20px;
      padding: 8px;
      border-radius: 20px;
      background: #ffffff;
      border: 2px solid #039340;
      box-shadow: 0 10px 24px rgba(3, 147, 64, 0.16);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .eco-logo-badge img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }

    h1 {
      color: var(--text-primary);
      margin-bottom: 8px;
      font-size: 26px;
      font-weight: 700;
    }

    h2 {
      color: var(--eco-brand);
      margin-bottom: 18px;
      font-size: 46px;
      font-weight: 700;
    }

    p {
      color: var(--text-secondary);
      margin-bottom: 28px;
      line-height: 1.6;
    }

    .action-buttons {
      display: flex;
      gap: 14px;
      justify-content: center;
      margin-bottom: 28px;
      flex-wrap: wrap;
    }

    .btn-home, .btn-menu {
      padding: 12px 24px;
      border: none;
      border-radius: 12px;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.25s ease;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .btn-home {
      background: #d9251b;
      color: white;
    }

    .btn-home:hover {
      background: #b31d15;
      transform: translateY(-2px);
    }

    .btn-menu {
      background: #039340;
      color: white;
    }

    .btn-menu:hover {
      background: #027a35;
      transform: translateY(-2px);
    }

    .suggested-links {
      border-top: 1px solid #e5e7eb;
      padding-top: 20px;
    }

    .suggested-links h3 {
      color: #161616;
      margin-bottom: 14px;
      font-size: 16px;
      font-weight: 700;
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
      color: #039340;
      text-decoration: none;
      font-weight: 600;
      cursor: pointer;
    }

    .suggested-links a:hover {
      color: #d9251b;
      text-decoration: underline;
    }

    @media (max-width: 768px) {
      .not-found-content {
        padding: 30px 20px;
        width: 90%;
      }

      h1 { font-size: 22px; }
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
  `]
})
export class NotFoundComponent {

  constructor(private readonly router: Router) {}

  navegarA(ruta: string): void {
    this.router.navigate([ruta]);
  }
}
