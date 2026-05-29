import 'zone.js'; //ESTO ME AYUDA A CAMBIAR LAS IMAGENES DEL CAROUSEL
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { appConfig } from './app/app.config';

bootstrapApplication(App, appConfig)
  .catch(err => console.error(err));