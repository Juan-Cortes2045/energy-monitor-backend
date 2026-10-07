package com.energymonitor.home.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.energymonitor.security.infrastructure.JwtKeyedTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Regression test for the {@code home_type} seed.
 *
 * <p>{@code home-005-seed-home-type} once existed without being included from the module
 * changelog, so the catalog stayed empty in every database and no home could be created. This
 * reads the table Liquibase left behind and nothing more: it writes no row, so it cannot disturb
 * the data of the database it runs against.
 */
@SpringBootTest
class HomeTypeSeedTest extends JwtKeyedTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("the catalog holds exactly the six seeded home types")
    void catalogHoldsExactlyTheSeededTypes() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
                .createNativeQuery("SELECT id_home_type, name FROM home_type")
                .getResultList();

        assertThat(rows)
                .extracting(row -> row[0], row -> row[1])
                .containsExactlyInAnyOrder(
                        tuple("hous000001", "house"),
                        tuple("apar000001", "apartment"),
                        tuple("stud000001", "studio"),
                        tuple("coun000001", "country_house"),
                        tuple("cabi000001", "cabin"),
                        tuple("othe000001", "other"));
    }
}
