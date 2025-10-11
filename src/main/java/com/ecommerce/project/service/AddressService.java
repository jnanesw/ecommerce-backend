package com.ecommerce.project.service;

import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AddressService {
    AddressDTO createAddress(@Valid AddressDTO addressDTO);
    List<AddressDTO> getAddresses();
    AddressDTO getAddressById(Long addressId);
    List<AddressDTO> getUserAddresses(User user);
    AddressDTO updateAddress(Long addressId, AddressDTO addressDTO);
    String deleteAddress(Long addressId);
}
