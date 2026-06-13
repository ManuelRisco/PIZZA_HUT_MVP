import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ubicacion',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './ubicacion.html',
  styleUrl: './ubicacion.css'
})
export class Ubicacion {
  mapLoaded = false;

  onMapLoad() {
    this.mapLoaded = true;
  }
}
