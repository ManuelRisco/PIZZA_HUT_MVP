import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PanelAdmin } from './panel-admin';

describe('PanelAdmin', () => {
  let component: PanelAdmin;
  let fixture: ComponentFixture<PanelAdmin>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
      imports: [PanelAdmin]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PanelAdmin);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});


