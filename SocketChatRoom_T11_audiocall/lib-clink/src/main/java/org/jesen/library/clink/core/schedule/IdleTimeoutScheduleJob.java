package org.jesen.library.clink.core.schedule;

import org.jesen.library.clink.core.Connector;
import org.jesen.library.clink.core.ScheduleJob;

import java.util.concurrent.TimeUnit;

public class IdleTimeoutScheduleJob extends ScheduleJob {

    public IdleTimeoutScheduleJob(long idleTime, TimeUnit unit, Connector connector) {
        super(idleTime, unit, connector);
    }

    @Override
    public void run() {
        long lastActiveTime = connector.getLastActiveTime();
        long idleTimeoutMilliseconds = this.idleTimeoutMilliseconds;

        long nextDelay = idleTimeoutMilliseconds - (System.currentTimeMillis() - lastActiveTime);
        if (nextDelay <=0){
            // 时间到了
            realSchedule(idleTimeoutMilliseconds);
            try {
                connector.fireIdleTimeoutEvent();
            }catch (Throwable throwable){
                connector.fireExceptionCaught(throwable);
            }
        }else {
            realSchedule(nextDelay);
        }
    }
}
