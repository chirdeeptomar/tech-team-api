package com.tech.teams.hooks;

import java.util.Optional;

import com.tech.teams.entity.Profile;
import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SaveProfileHook implements LifeCycleHook<Profile> {

    @Override
    public void execute(Operation operation, TransactionPhase phase, Profile elideEntity, RequestScope requestScope,
            Optional<ChangeSpec> changes) {
        log.info("Saved profile {}", elideEntity);
    }

}
