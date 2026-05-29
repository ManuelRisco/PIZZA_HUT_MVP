package com.example.application.users.creacionales;

import com.example.domain.model.Usuario;

/**
 * Patrón Factory - Crea diferentes tipos de usuarios
 */
public class UsuarioFactory {

    /**
     * Crea un usuario cliente con configuración por defecto
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
     * Crea un usuario con información completa
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
