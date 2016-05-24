import java.util.concurrent.Exchanger;

/**
 * Created by dzharikhin on 29.02.2016.
 */
public class PingPongExchanger {

    static class PingPong extends Thread {

        private final Exchanger<Object> exchanger;
        private Object ball;

        public PingPong(Exchanger<Object> exchanger, String name) {
            super(name);
            this.exchanger = exchanger;
        }

        public PingPong(Exchanger<Object> exchanger, String name, Object ball) {
            super(name);
            this.exchanger = exchanger;
            this.ball = ball;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (this.ball != null) {
                        System.out.println(getName());
                        Thread.sleep(1000);
                    }
                        this.ball = exchanger.exchange(this.ball);
                    }
            } catch (InterruptedException e) {

            }
        }
    }

    public static void main(String[] args) {
        final Exchanger<Object> table = new Exchanger<>();
        final Object ball = new Object();

        new PingPong(table, "pong").start();
        new PingPong(table, "ping", ball).start();
    }
}
