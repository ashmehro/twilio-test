package twilio;

import java.util.concurrent.atomic.AtomicInteger;

public class UserIdGenerator {

    static AtomicInteger generator = new AtomicInteger(1000);
    public static int getUserId() {
        return generator.getAndIncrement();
    }
}
