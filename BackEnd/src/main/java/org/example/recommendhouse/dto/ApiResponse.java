package org.example.recommendhouse.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.recommendhouse.entity.RentalHousing;

import java.util.List;

// ApiResponse.java
@Getter
@Setter
@NoArgsConstructor
public class ApiResponse {
    private List<RentalHousing> data;
    private int currentCount;
    private int matchCount;
    private int page;
    private int perPage;
    private int totalCount;
}