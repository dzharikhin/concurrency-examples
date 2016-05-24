import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Created by dzharikhin on 29.02.2016.
 */
public class PingPongBarrier {
    static class PingPong extends Thread {

        private final String msg;
        private Object ball;

        private CyclicBarrier barrier;

        public PingPong(String msg) {
            this.msg = msg;
        }

        public void setBall(Object ball) {
            this.ball = ball;
        }

        public void setBarrier(CyclicBarrier barrier) {
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (this.ball != null) {
                        System.out.println(msg);
                        Thread.sleep(1000);
                    }
                    barrier.await();
                }
            } catch (InterruptedException e) {

            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        final Object ball = new Object();
        PingPong p1 = new PingPong("ping");
        PingPong p2 = new PingPong("pong");
        final Runnable barrierAction = new Runnable() {
            @Override
            public void run() {
                if (p1.ball != null) {
                    p1.ball = null;
                    p2.ball = ball;
                } else {
                    p1.ball = ball;
                    p2.ball = null;
                }
            }
        };
        CyclicBarrier barrier = new CyclicBarrier(2, barrierAction);
        p1.setBarrier(barrier);
        p2.setBarrier(barrier);
        p1.setBall(ball);
        p1.start();
        p2.start();
    }
}
