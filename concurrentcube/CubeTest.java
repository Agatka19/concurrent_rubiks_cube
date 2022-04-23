package concurrentcube;

import org.junit.Test;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class CubeTest {
    private Cube cube, cubeConcurrent;
    int size = 3;
    Thread[] threads = new Thread[10];

    private static class cubeRunClass implements Runnable {
        int side, layer;
        Cube cubeConcurrent;

        public cubeRunClass(int side, int layer, Cube cubeConcurrent) {
            this.side = side;
            this.layer = layer;
            this.cubeConcurrent = cubeConcurrent;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < 10; ++i) {
                    cubeConcurrent.rotate(side, layer);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                for (int i = 0; i < 10; ++i) {
                    cubeConcurrent.show();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @BeforeEach
    public void setup() {
        var count = new Object() {
            final AtomicInteger value = new AtomicInteger(0);
        };
        cubeConcurrent = new Cube(size, (x, y) -> {
            try {
                Thread.sleep(100);
                count.value.incrementAndGet();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, (x, y) -> {
            try {
                Thread.sleep(100);
                count.value.incrementAndGet();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, () -> {
        }, () -> {
        });

        cube = new Cube(size,
                (x, y) -> count.value.incrementAndGet(),
                (x, y) -> count.value.incrementAndGet(),
                count.value::incrementAndGet,
                count.value::incrementAndGet
        );
    }

    @Test
    public void testCorrectnesRotations() {
        setup();
        Cube cube2 = cube;
        assertDoesNotThrow(() -> {
            for (int side = 0; side < 6; side++) {
                for (int layer = 0; layer < size; layer++) {
                    cube.rotate(side, layer);
                    int opposite = size - 1 - layer;
                    switch (side) {
                        case 0:
                            cube.rotate(5, opposite);
                            break;
                        case 1:
                            cube.rotate(3, opposite);
                            break;
                        case 2:
                            cube.rotate(4, opposite);
                            break;
                        case 3:
                            cube.rotate(1, opposite);
                            break;
                        case 4:
                            cube.rotate(2, opposite);
                            break;
                        case 5:
                            cube.rotate(0, opposite);
                            break;
                    }
                }
            }
            assertEquals(cube.show(), cube2.show());
        });
    }

    @Test
    public void testConcurrency() {
        setup();
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(new cubeRunClass(2, (i % 3), cubeConcurrent));
            threads[i] = thread;
            thread.start();
        }
        for (int i = 0; i < 10; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
