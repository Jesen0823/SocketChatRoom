package org.jesen.library.clink.core.schedule;

import org.jesen.library.clink.core.Connector;
import org.jesen.library.clink.core.SchedulerJob;

import java.util.concurrent.TimeUnit;

public class IdleTimeoutScheduleJob extends SchedulerJob {

    public IdleTimeoutScheduleJob(long idleTimeOut, TimeUnit unit, Connector connector) {
        super(idleTimeOut, unit, connector);
    }

    @Override
    public void run() {
        long lastActiveTime = connector.getLastActiveTime();
        long idleTimeoutMilliseconds = this.idleTimeoutMilliseconds;

        long nextDelay = idleTimeoutMilliseconds - (System.currentTimeMillis() - lastActiveTime);
        if (nextDelay <= 0) {
            // 已经超时
            schedule(idleTimeoutMilliseconds);

            try{
                connector.fireIdleTimeoutEvent();
            }catch (Throwable throwable){
                connector.fireExceptionCaught(throwable);
            }
        } else {
            schedule(nextDelay);
        }
    }
}
