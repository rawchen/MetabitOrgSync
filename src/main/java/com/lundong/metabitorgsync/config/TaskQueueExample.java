package com.lundong.metabitorgsync.config;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author shuangquan.chen
 * @date 2024-01-17 15:20
 */
@Slf4j
public class TaskQueueExample {
    private BlockingQueue<Runnable> queue;
    Runnable active;

    public TaskQueueExample(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public void submitTask(final Runnable task) throws InterruptedException {
        if (!queue.offer(task)) {
            throw new IllegalStateException("Failed to add the task to the queue.");
        }
    }

    public void startProcessingTasks() {
        Thread thread = new Thread(() -> {
            log.info("任务处理队列线程启动");
            try {
                while (true) {
                    Runnable task = queue.take();

                    execute(task);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public synchronized void execute(final Runnable r) {
        try {
            r.run();
        } finally {
            scheduleNext();
        }
    }

    protected void scheduleNext() {
        if ((active = queue.poll()) != null) {
            this.execute(active);
        }
    }

}