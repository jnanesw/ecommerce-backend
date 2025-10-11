package com.ecommerce.project.repo;

import com.ecommerce.project.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepo extends JpaRepository<Address, Long> {
    @Query("SELECT a from Address a WHERE a.user.id=?1 AND a.id=?2")
    Address getAddressByUserIdAndAddressId(Long userId, Long addressId);
}
