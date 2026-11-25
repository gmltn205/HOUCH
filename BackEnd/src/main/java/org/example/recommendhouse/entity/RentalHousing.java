package org.example.recommendhouse.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rental_housing_favorites")
public class RentalHousing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("HOUSE_MANAGE_NO")
    @Column(unique = true)
    private String houseManageNo;

    @JsonProperty("HOUSE_NM")
    private String houseName;

    @JsonProperty("HSSPLY_ADRES")
    private String address;

    @JsonProperty("TOT_SUPLY_HSHLDCO")
    private Integer totalSupplyCount;

    @JsonProperty("RCRIT_PBLANC_DE")
    private String recruitDate;

    @JsonProperty("SUBSCRPT_RCEPT_BGNDE")
    private String subscriptionStartDate;

    @JsonProperty("SUBSCRPT_RCEPT_ENDDE")
    private String subscriptionEndDate;

    @JsonProperty("PRZWNER_PRESNATN_DE")
    private String winnerAnnouncementDate;

    @JsonProperty("HMPG_ADRES")
    private String homepageUrl;

    @JsonProperty("BSNS_MBY_NM")
    private String businessName;

    @JsonProperty("MDHS_TELNO")
    private String contactNumber;

    // 찜하기 관련 필드
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private boolean favorited = false;
}