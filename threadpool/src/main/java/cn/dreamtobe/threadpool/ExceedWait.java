/*
 * Copyright (C) 2015-2016 Jacksgong(blog.dreamtobe.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.dreamtobe.threadpool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Components of exceed wait thread pool.
 */

public final class ExceedWait {
    public static class RejectedHandler implements RejectedExecutionHandler {
        private final static String TAG = "ExceedWaitRejectedHandler";

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                ThreadPoolLog.d(TAG, "put the rejected command to the exceed queue" +
                        " in the rejectedExecution method: %s", r);
                ((Queue) executor.getQueue()).putExceed(r);
            }
        }
    }

    public static class Queue extends SynchronousQueue<Runnable> {

        private final static String TAG = "ExceedWaitQueue";
        private final LinkedBlockingQueue<Runnable> mExceedQueue = new LinkedBlockingQueue<>();

        Queue() {
            super(true);
        }

        @Override
        public boolean offer(Runnable runnable) {
            ThreadPoolLog.d(TAG, "offer() called with: runnable = [%s]", runnable);
            boolean result = super.offer(runnable);
            ThreadPoolLog.d(TAG, "offer() returned: %B", result);
            return result;
        }

        @Override
        public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
            ThreadPoolLog.d(TAG, "poll() called with: timeout = [%d], unit = [%s]", timeout, unit);
            Runnable result = super.poll(timeout, unit);
            if (mExceedQueue.size() > 0 && result == null) {
                result = mExceedQueue.poll(timeout, unit);
            }
            ThreadPoolLog.d(TAG, "poll() returned: %s", result);
            return result;
        }

        @Override
        public Runnable take() throws InterruptedException {
            ThreadPoolLog.d(TAG, "take() called");
            Runnable result = super.take();
            if (mExceedQueue.size() > 0 && result == null) {
                result = mExceedQueue.take();
            }
            ThreadPoolLog.d(TAG, "take() returned: %s", result);
            return result;
        }

        @Override
        public boolean remove(Object o) {
            ThreadPoolLog.d(TAG, "remove() called with: o = [%s]", o);
            return mExceedQueue.remove(o);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            ThreadPoolLog.d(TAG, "removeAll() called with: c = [%s]", c);
            return mExceedQueue.removeAll(c);
        }

        public int exceedSize() {
            return mExceedQueue.size();
        }

        public void putExceed(Runnable e) {
            mExceedQueue.offer(e);
        }

        public List<Runnable> drainExceedQueue() {
            BlockingQueue<Runnable> q = mExceedQueue;
            ArrayList<Runnable> taskList = new ArrayList<>();
            q.drainTo(taskList);
            if (!q.isEmpty()) {
                for (Runnable r : q.toArray(new Runnable[0])) {
                    if (q.remove(r))
                        taskList.add(r);
                }
            }
            return taskList;
        }
    }
}
