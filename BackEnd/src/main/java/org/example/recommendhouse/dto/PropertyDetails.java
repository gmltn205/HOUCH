package org.example.recommendhouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDetails {
    private String propertyName;
    private float unitCount;  //
    private String propertyType;
    private float area;
    private float deposit;    //
    private float monthlyRent; //
}