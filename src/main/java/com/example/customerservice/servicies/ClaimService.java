package com.example.customerservice.servicies;

import com.example.customerservice.data.ClaimDto;
import com.example.customerservice.data.ClaimEntry;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClaimService {

    @Autowired
    private ModelMapper modelMapper;

    public ClaimDto convertToDto(final ClaimEntry claimEntry) {
        final ClaimDto claimDto = modelMapper.map(claimEntry, ClaimDto.class);
        claimDto.setUsername(claimEntry.getUser().getName());
        return claimDto;
    }
}
