package com.example.patterns.users.creacionales;

import com.example.models.Usuario;

/**
 * Patr\u00f3n Builder - Facilita la creaci\u00f3n de objetos Usuario de forma fluida
 */
public class UsuarioBuilder {
    private Usuario usuario;

    public UsuarioBuilder() {
        this.usuario = new Usuario();
    }

    public UsuarioBuilder withEmail(String email) {
        this.usuario.setEmail(email);
        return this;
    }

    public UsuarioBuilder withPassword(String password) {
        this.usuario.setPasswordHash(password);
        return this;
    }

    public UsuarioBuilder withName(String name) {
        this.usuario.setName(name);
        return this;
    }

    public UsuarioBuilder withPhone(String phone) {
        this.usuario.setPhone(phone);
        return this;
    }

    public UsuarioBuilder withRole(Usuario.Role role) {
        this.usuario.setRole(role);
        return this;
    }

    public Usuario build() {
        return this.usuario;
    }

    // M\u00e9todo est\u00e1tico para iniciar el builder
    public static UsuarioBuilder builder() {
        return new UsuarioBuilder();
    }
}
