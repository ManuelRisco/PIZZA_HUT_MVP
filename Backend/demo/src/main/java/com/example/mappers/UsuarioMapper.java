package com.example.mappers;

import com.example.dtos.UsuarioCreateDTO;
import com.example.dtos.UsuarioDTO;
import com.example.models.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UsuarioMapper {

    UsuarioDTO toDto(Usuario usuario);

    @org.mapstruct.Mapping(source = "password", target = "passwordHash")
    Usuario toEntity(UsuarioCreateDTO usuarioDTO);

    List<UsuarioDTO> toDtoList(List<Usuario> usuarios);

    @org.mapstruct.Mapping(source = "password", target = "passwordHash")
    void updateEntityFromDto(UsuarioCreateDTO usuarioDTO, @MappingTarget Usuario usuario);
}
