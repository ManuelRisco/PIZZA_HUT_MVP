import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Categorias } from './categorias';
import { CategoriaService } from '../../services/categoria.service';
import { PizzaService } from '../../services/pizza.service';

describe('Categorias', () => {
  let component: Categorias;
  let fixture: ComponentFixture<Categorias>;
  const categoriaServiceMock = {
    listarCategorias: jasmine.createSpy('listarCategorias').and.returnValue(of([])),
    crearCategoria: jasmine.createSpy('crearCategoria'),
    actualizarCategoria: jasmine.createSpy('actualizarCategoria'),
    eliminarCategoria: jasmine.createSpy('eliminarCategoria')
  };
  const pizzaServiceMock = {
    listarPizzas: jasmine.createSpy('listarPizzas').and.returnValue(of([]))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Categorias],
      providers: [provideHttpClient(), provideRouter([]), 
        { provide: CategoriaService, useValue: categoriaServiceMock },
        { provide: PizzaService, useValue: pizzaServiceMock }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Categorias);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});


