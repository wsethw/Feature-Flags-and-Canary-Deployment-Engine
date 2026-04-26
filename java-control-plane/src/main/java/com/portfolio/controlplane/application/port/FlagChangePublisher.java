package com.portfolio.controlplane.application.port;

import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import org.eclipse.jdt.annotation.NonNull;

public interface FlagChangePublisher {

    void publish(@NonNull FlagChangedEvent event);
}
