package com.winten.greenlight.admin.domain.actionevent;

import com.winten.greenlight.admin.domain.actionevent.dto.ActionEvent;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ActionEventQueue {
    private final BlockingQueue<ActionEvent> queue = new LinkedBlockingQueue<>();

    public boolean offer(ActionEvent event) {
        return queue.offer(event);
    }

    public int drainTo(Collection<? super ActionEvent> sink, int maxElements) {
        return queue.drainTo(sink, maxElements);
    }

    public int size() {
        return queue.size();
    }
}