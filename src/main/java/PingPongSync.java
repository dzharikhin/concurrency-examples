/**
 * Created by dzharikhin on 29.02.2016.
 */
public class PingPongSync {
    static class PingPongThread extends Thread {
        private static final Object lock = new Object();
        private final String msg;

        public PingPongThread(String msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            try {
                synchronized (lock) {
                    if (msg.equals("pong")) {
                        notifyAndWait();
                    }
                    while (true) {
                        System.out.println(msg);
                        Thread.sleep(1000);
                        notifyAndWait();
                    }
                }
            } catch (InterruptedException e) {

            }
        }

        private void notifyAndWait() throws InterruptedException {
            lock.notifyAll();
            lock.wait();
        }
    }

    public static void main(String[] args) {
        new PingPongThread("pong").start();
        new PingPongThread("ping").start();
    }
}
