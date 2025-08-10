package com.winten.greenlight.prototype.admin.domain.actionevent;

import com.influxdb.client.write.Point;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ActionEventQueue {
    private final BlockingQueue<Point> queue = new LinkedBlockingQueue<>();

    public boolean offer(Point point) {
        return queue.offer(point);
    }

    public int drainTo(Collection<? super Point> sink, int maxElements) {
        return queue.drainTo(sink, maxElements);
    }

    public int size() {
        return queue.size();
    }
}