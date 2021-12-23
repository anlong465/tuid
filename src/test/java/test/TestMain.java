package test;

import org.sunrise.tuid.Byte62;
import org.sunrise.tuid.TUID;

import java.util.Date;
import java.util.UUID;

public class TestMain {
    public final static void main(String[] args) {
        System.out.println(new Date(Byte62.toTimemills("SsNSOrr")));

        for(int i = 0; i < 10000; i++) {
            System.out.println(TUID.next());
        }

        test(30000000);
    }

    private static void test(int number) {
        new Tester("TUID", number, () -> { TUID.next(); } ).run();
        new Tester("UUID", number, () -> { UUID.randomUUID().toString(); } ).run();
    }

    private static class Tester {
        String testName;
        int number;
        TestRunner runner;
        public Tester(String testName, int number, TestRunner runner) {
            this.testName = testName;
            this.number = number;
            this.runner = runner;
        }

        public void run() {
            runner.exec();

            long startMs = System.nanoTime();
            for(int i = 0; i < number; i++) {
                runner.exec();
            }
            long usedTime = System.nanoTime() - startMs;
            System.out.println( "[" + testName + "] avg time: " + (usedTime/number) + "ns");
        }
    }

    private static interface TestRunner {
        public void exec();
    }
}
