package com.trototn.boardinghouse.interaction;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RoomHoldExpirationJob {
    private final InteractionService interactionService;

    public RoomHoldExpirationJob(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void releaseExpiredHolds() {
        interactionService.expirePendingHolds();
    }
}
