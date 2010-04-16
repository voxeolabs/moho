package com.voxeo.utils;

import java.util.concurrent.Executor;

public final class SynchronousExecutor implements Executor {

    private static final SynchronousExecutor singleton = new SynchronousExecutor();

    public static final Executor get() {
        return singleton;
    }
    
    public void execute(Runnable command) {
        command.run();
    }

}
