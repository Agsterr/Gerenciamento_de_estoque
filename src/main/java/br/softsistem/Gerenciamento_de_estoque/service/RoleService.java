package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    @Transactional
    public Role createRole(Role role) {
        if (roleRepository.existsByNome(role.getNome())) {
            throw new IllegalArgumentException("Role com o nome já existe.");
        }
        return roleRepository.save(role);
    }

    @Transactional
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

    @Transactional
    public boolean deleteRole(Long id) {
        if (roleRepository.existsById(id)) {
            roleRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
