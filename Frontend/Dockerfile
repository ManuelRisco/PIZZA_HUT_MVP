FROM node:20 AS build
WORKDIR /app

# Instalar dependencias
COPY package*.json ./
RUN npm ci

# Copiar el código y construir la app Angular
COPY . .
RUN npm run build -- --configuration development

# Etapa de ejecución con Nginx
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar los archivos compilados de Angular a Nginx
# Nota: Ajusta 'pizzahut/browser' si tu carpeta dist tiene otra estructura
COPY --from=build /app/dist/pizzahut/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
