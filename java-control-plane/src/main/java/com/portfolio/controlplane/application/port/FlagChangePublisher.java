package com.portfolio.controlplane.application.port;

import com.portfolio.controlplane.domain.event.FlagChangedEvent;

public interface FlagChangePublisher {

    void publish(FlagChangedEvent event);
}

