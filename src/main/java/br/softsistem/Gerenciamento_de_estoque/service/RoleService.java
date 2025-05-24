package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    // Injeção via construtor
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    // Retorna todas as roles
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    // Retorna uma role por ID
    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    // Cria uma nova role
    public Role createRole(Role role) {
        if (roleRepository.existsByNome(role.getNome())) {
            throw new IllegalArgumentException("Role com o nome já existe.");
        }
        return roleRepository.save(role);
    }

    // Atualiza uma role existente
    public Optional<Role> updateRole(Long id, Role roleDetails) {
        Optional<Role> existingRole = roleRepository.findById(id);
        if (existingRole.isPresent()) {
            Role updatedRole = existingRole.get();
            updatedRole.setNome(roleDetails.getNome());
            updatedRole.setOrg(roleDetails.getOrg());
            return Optional.of(roleRepository.save(updatedRole));
        }
        return Optional.empty();
    }

    // Exclui uma role
    public boolean deleteRole(Long id) {
        if (roleRepository.existsById(id)) {
            roleRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
