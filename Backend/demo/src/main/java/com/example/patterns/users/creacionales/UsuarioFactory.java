package com.example.patterns.users.creacionales;

import com.example.models.Usuario;

/**
 * Patr\u00f3n Factory - Crea diferentes tipos de usuarios
 */
public class UsuarioFactory {

    private UsuarioFactory() {
        throw new IllegalStateException("Utility class");
    }


    /**
     * Crea un usuario cliente con configuraci\u00f3n por defecto
     */
    public static Usuario crearCliente(String email, String password, String name) {
        return UsuarioBuilder.builder()
                .withEmail(email)
                .withPassword(password)
                .withName(name)
                .withRole(Usuario.Role.CUSTOMER)
                .build();
    }

    /**
     * Crea un usuario administrador
     */
    public static Usuario crearAdministrador(String email, String password, String name) {
        return UsuarioBuilder.builder()
                .withEmail(email)
                .withPassword(password)
                .withName(name)
                .withRole(Usuario.Role.ADMIN)
                .build();
    }

    /**
     * Crea un usuario con informaci\u00f3n completa
     */
    public static Usuario crearUsuarioCompleto(String email, String password, String name, 
                                               String phone, Usuario.Role role) {
        return UsuarioBuilder.builder()
                .withEmail(email)
                .withPassword(password)
                .withName(name)
                .withPhone(phone)
                .withRole(role)
                .build();
    }
}
