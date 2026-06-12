package com.trototn.boardinghouse.interaction;

import com.trototn.boardinghouse.interaction.repository.RentalContractRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RentalContractRepositoryTest {
    @Test
    void contractDownloadQueryFetchesAllDocumentRelations() throws Exception {
        EntityGraph graph = RentalContractRepository.class
                .getMethod("findByRentalRequestId", Long.class)
                .getAnnotation(EntityGraph.class);

        assertNotNull(graph);
        assertEquals(
                Set.of("room", "tenant", "landlord", "physicalRoom"),
                Set.copyOf(Arrays.asList(graph.attributePaths())));
    }
}
