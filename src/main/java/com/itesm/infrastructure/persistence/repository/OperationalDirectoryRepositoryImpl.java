package com.itesm.infrastructure.persistence.repository;

import com.itesm.domain.models.HospitalInventoryMovement;
import com.itesm.domain.models.HospitalOperationalContact;
import com.itesm.domain.models.HospitalOperationalGroup;
import com.itesm.domain.repository.OperationalDirectoryRepository;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.HospitalInventoryItemEntity;
import com.itesm.infrastructure.persistence.entity.HospitalInventoryMovementEntity;
import com.itesm.infrastructure.persistence.entity.HospitalOperationalContactEntity;
import com.itesm.infrastructure.persistence.entity.HospitalOperationalGroupEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class OperationalDirectoryRepositoryImpl
        implements OperationalDirectoryRepository,
                   PanacheRepositoryBase<HospitalOperationalContactEntity, UUID> {

    @Inject
    EntityManager em;

    @Override
    public List<HospitalOperationalContact> findContactsByHospitalId(UUID hospitalId, Boolean assignable, Boolean notifiable, String departmentCode) {
        return em.createQuery("""
                select c
                from HospitalOperationalContactEntity c
                where c.hospital.id = :hospitalId
                  and (:assignable is null or c.assignable = :assignable)
                  and (:notifiable is null or c.notifiable = :notifiable)
                  and (:departmentCode is null or c.departmentCode = :departmentCode)
                order by c.displayName asc
                """, HospitalOperationalContactEntity.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("assignable", assignable)
                .setParameter("notifiable", notifiable)
                .setParameter("departmentCode", departmentCode)
                .getResultList()
                .stream().map(this::contactToDomain).collect(Collectors.toList());
    }

    @Override
    public List<HospitalOperationalContact> findActiveEmailContactsByDepartment(UUID hospitalId, String departmentCode, boolean notifiableOnly) {
        return em.createQuery("""
                select c
                from HospitalOperationalContactEntity c
                where c.hospital.id = :hospitalId
                  and c.departmentCode = :departmentCode
                  and c.contactChannel = 'EMAIL'
                  and c.contactValue is not null
                  and upper(coalesce(c.availabilityStatus, 'ACTIVE')) <> 'INACTIVE'
                  and (:notifiableOnly = false or c.notifiable = true)
                order by c.displayName asc
                """, HospitalOperationalContactEntity.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("departmentCode", departmentCode)
                .setParameter("notifiableOnly", notifiableOnly)
                .getResultList()
                .stream().map(this::contactToDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<HospitalOperationalContact> findContactById(UUID contactId) {
        return Optional.ofNullable(em.find(HospitalOperationalContactEntity.class, contactId)).map(this::contactToDomain);
    }

    @Override
    @Transactional
    public HospitalOperationalContact createContact(HospitalOperationalContact contact) {
        HospitalOperationalContactEntity entity = new HospitalOperationalContactEntity();
        entity.setId(contact.getId() != null ? contact.getId() : UUID.randomUUID());
        entity.setHospital(em.getReference(HospitalEntity.class, contact.getHospitalId()));
        copyContactFields(contact, entity);
        entity.setUpdatedAt(LocalDateTime.now());
        em.persist(entity);
        return contactToDomain(entity);
    }

    @Override
    @Transactional
    public HospitalOperationalContact updateContact(HospitalOperationalContact contact) {
        HospitalOperationalContactEntity entity = em.find(HospitalOperationalContactEntity.class, contact.getId());
        copyContactFields(contact, entity);
        entity.setUpdatedAt(LocalDateTime.now());
        return contactToDomain(entity);
    }

    @Override
    public List<HospitalOperationalGroup> findGroupsByHospitalId(UUID hospitalId, Boolean assignable, Boolean notifiable, String departmentCode) {
        return em.createQuery("""
                select g
                from HospitalOperationalGroupEntity g
                where g.hospital.id = :hospitalId
                  and (:assignable is null or g.assignable = :assignable)
                  and (:notifiable is null or g.notifiable = :notifiable)
                  and (:departmentCode is null or g.departmentCode = :departmentCode)
                order by g.groupName asc
                """, HospitalOperationalGroupEntity.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("assignable", assignable)
                .setParameter("notifiable", notifiable)
                .setParameter("departmentCode", departmentCode)
                .getResultList()
                .stream().map(this::groupToDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<HospitalOperationalGroup> findGroupById(UUID groupId) {
        return Optional.ofNullable(em.find(HospitalOperationalGroupEntity.class, groupId)).map(this::groupToDomain);
    }

    @Override
    public List<HospitalInventoryMovement> findInventoryMovementsByItemId(UUID hospitalId, UUID inventoryItemId) {
        return em.createQuery("""
                select m
                from HospitalInventoryMovementEntity m
                where m.hospital.id = :hospitalId
                  and m.inventoryItem.id = :inventoryItemId
                order by m.createdAt desc
                """, HospitalInventoryMovementEntity.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("inventoryItemId", inventoryItemId)
                .getResultList()
                .stream().map(this::movementToDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void appendInventoryMovement(HospitalInventoryMovement movement) {
        HospitalInventoryMovementEntity entity = new HospitalInventoryMovementEntity();
        entity.setId(movement.getId() != null ? movement.getId() : UUID.randomUUID());
        entity.setHospital(em.getReference(HospitalEntity.class, movement.getHospitalId()));
        entity.setInventoryItem(em.getReference(HospitalInventoryItemEntity.class, movement.getInventoryItemId()));
        entity.setMovementType(movement.getMovementType());
        entity.setQuantityDelta(movement.getQuantityDelta());
        entity.setUnit(movement.getUnit());
        entity.setNotes(movement.getNotes());
        entity.setRelatedSupplyRequestId(movement.getRelatedSupplyRequestId());
        entity.setCreatedAt(movement.getCreatedAt() != null ? movement.getCreatedAt() : LocalDateTime.now());
        em.persist(entity);
    }

    private HospitalOperationalContact contactToDomain(HospitalOperationalContactEntity entity) {
        HospitalOperationalContact contact = new HospitalOperationalContact();
        contact.setId(entity.getId());
        contact.setHospitalId(entity.getHospital().getId());
        if (entity.getUser() != null) {
            contact.setUserId(entity.getUser().getId());
        }
        contact.setDisplayName(entity.getDisplayName());
        contact.setRoleLabel(entity.getRoleLabel());
        contact.setDepartmentCode(entity.getDepartmentCode());
        contact.setContactChannel(entity.getContactChannel());
        contact.setContactValue(entity.getContactValue());
        contact.setAvailabilityStatus(entity.getAvailabilityStatus());
        contact.setAssignable(entity.isAssignable());
        contact.setNotifiable(entity.isNotifiable());
        contact.setUpdatedAt(entity.getUpdatedAt());
        return contact;
    }

    private void copyContactFields(HospitalOperationalContact contact, HospitalOperationalContactEntity entity) {
        entity.setDisplayName(contact.getDisplayName());
        entity.setRoleLabel(contact.getRoleLabel());
        entity.setDepartmentCode(contact.getDepartmentCode());
        entity.setContactChannel(contact.getContactChannel());
        entity.setContactValue(contact.getContactValue());
        entity.setAvailabilityStatus(contact.getAvailabilityStatus());
        entity.setAssignable(contact.isAssignable());
        entity.setNotifiable(contact.isNotifiable());
    }

    private HospitalOperationalGroup groupToDomain(HospitalOperationalGroupEntity entity) {
        HospitalOperationalGroup group = new HospitalOperationalGroup();
        group.setId(entity.getId());
        group.setHospitalId(entity.getHospital().getId());
        group.setGroupCode(entity.getGroupCode());
        group.setGroupName(entity.getGroupName());
        group.setGroupType(entity.getGroupType());
        group.setDepartmentCode(entity.getDepartmentCode());
        group.setAssignable(entity.isAssignable());
        group.setNotifiable(entity.isNotifiable());
        group.setMemberCount(countGroupMembers(entity.getId()));
        group.setUpdatedAt(entity.getUpdatedAt());
        return group;
    }

    private int countGroupMembers(UUID groupId) {
        return em.createQuery("""
                select count(m)
                from HospitalOperationalGroupMemberEntity m
                where m.group.id = :groupId
                """, Long.class)
                .setParameter("groupId", groupId)
                .getSingleResult()
                .intValue();
    }

    private HospitalInventoryMovement movementToDomain(HospitalInventoryMovementEntity entity) {
        HospitalInventoryMovement movement = new HospitalInventoryMovement();
        movement.setId(entity.getId());
        movement.setHospitalId(entity.getHospital().getId());
        movement.setInventoryItemId(entity.getInventoryItem().getId());
        movement.setMovementType(entity.getMovementType());
        movement.setQuantityDelta(entity.getQuantityDelta());
        movement.setUnit(entity.getUnit());
        movement.setNotes(entity.getNotes());
        movement.setRelatedSupplyRequestId(entity.getRelatedSupplyRequestId());
        movement.setCreatedAt(entity.getCreatedAt());
        return movement;
    }
}
