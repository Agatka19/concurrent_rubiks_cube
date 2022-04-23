package concurrentcube;

import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;

public class Cube {

    private final int size;
    private BiConsumer<Integer, Integer> beforeRotation;
    private BiConsumer<Integer, Integer> afterRotation;
    private Runnable beforeShowing;
    private Runnable afterShowing;

    private int[][][] net;
    private int workingGroup = -1;
    private int workingProcessesNumber = 0;
    private int[] waitingProcesses = new int[4];
    private int waitingGroupsNumber = 0;

    private final Semaphore security = new Semaphore(1, true);
    private final Semaphore[] rest = new Semaphore[4]; //separate semaphore for every group
    private final Semaphore[] layers;


    public Cube(int size,
                BiConsumer<Integer, Integer> beforeRotation,
                BiConsumer<Integer, Integer> afterRotation,
                Runnable beforeShowing,
                Runnable afterShowing) {

        this.size = size;
        this.beforeRotation = beforeRotation;
        this.afterRotation = afterRotation;
        this.beforeShowing = beforeShowing;
        this.afterShowing = afterShowing;
        net = new int[6][size][size];
        this.layers = new Semaphore[size];

        //creating the cube
        for (int side = 0; side < 6; ++side) {
            for (int row = 0; row < size; ++row) {
                for (int column = 0; column < size; ++column) {
                    net[side][row][column] = side;
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            waitingProcesses[i] = 0;
        }
        for (int i = 0; i < 4; ++i) {
            rest[i] = new Semaphore(0, true);
        }
        for (int i = 0; i < layers.length; ++i) {
            layers[i] = new Semaphore(1, true);
        }

    }

    private void rotate_side_prim(int side) {
        // rotates the opposide side when layer = size - 1
        for (int i = 0; i < this.size / 2; i++) {
            for (int j = 0; j < (this.size + 1) / 2; j++) {
                // we can divide the cube into rectangles and rotate each rectangle
                int pom = net[side][i][j];
                net[side][i][j] = net[side][j][this.size - 1 - i];
                net[side][j][this.size - 1 - i] = net[side][this.size - 1 - i][this.size - 1 - j];
                net[side][this.size - 1 - i][this.size - 1 - j] = net[side][this.size - 1 - j][i];
                net[side][this.size - 1 - j][i] = pom;
            }
        }
    }

    private void rotate_adjacent_side(int side) {
        // rotates the side that is adjacent to the one we are rotating
        for (int i = 0; i < this.size / 2; i++) {
            for (int j = 0; j < (this.size + 1) / 2; j++) {
                int aux = this.net[side][i][j];
                net[side][i][j] = net[side][this.size - 1 - j][i];
                net[side][this.size - 1 - j][i] = net[side][this.size - 1 - i][this.size - 1 - j];
                net[side][this.size - 1 - i][this.size - 1 - j] = net[side][j][this.size - 1 - i];
                net[side][j][this.size - 1 - i] = aux;
            }
        }
    }

    private void rotate_left(int layer) {
        // rotating from the left side (the rest of the rotation functions will be named similarly)
        for (int j = 0; j < this.size; j++) {
            int pom = net[4][this.size - 1 - j][this.size - 1 - layer];
            net[4][this.size - 1 - j][this.size - 1 - layer] = net[5][j][layer];
            net[5][j][layer] = net[2][j][layer];
            net[2][j][layer] = net[0][j][layer];
            net[0][j][layer] = pom;
        }
        if (layer == 0) rotate_adjacent_side(1);
        if (layer == this.size - 1) rotate_side_prim(3);
    }

    private void rotate_right(int layer) {
        int i = this.size - 1 - layer;
        for (int j = 0; j < this.size; j++) {
            int pom = net[0][j][i];
            net[0][j][i] = net[2][j][i];
            net[2][j][i] = net[5][j][i];
            net[5][j][i] = net[4][this.size - 1 - j][this.size - 1 - i];
            net[4][this.size - 1 - j][this.size - 1 - i] = pom;
        }
        if (layer == 0) rotate_adjacent_side(3);
        if (layer == this.size - 1) rotate_side_prim(1);
    }

    private void rotate_up(int layer) {
        for (int j = 0; j < this.size; j++) {
            int pom = net[2][layer][j];
            net[2][layer][j] = net[3][layer][j];
            net[3][layer][j] = net[4][layer][j];
            net[4][layer][j] = net[1][layer][j];
            net[1][layer][j] = pom;
        }
        ;
        if (layer == 0) rotate_adjacent_side(0);
        if (layer == this.size - 1) rotate_side_prim(5);
    }

    private void rotate_down(int layer) {
        int i = this.size - 1 - layer;
        for (int j = 0; j < this.size; j++) {
            int pom = net[2][i][j];
            net[2][i][j] = net[1][i][j];
            net[1][i][j] = net[4][i][j];
            net[4][i][j] = net[3][i][j];
            net[3][i][j] = pom;
        }
        if (layer == 0) rotate_adjacent_side(5);
        if (layer == this.size - 1) rotate_side_prim(0);
    }

    private void rotate_front(int layer) {
        for (int j = 0; j < this.size; j++) {
            int pom = net[0][this.size - 1 - layer][j];
            // we change the indexes because we will be rotating rows on the upper and down sides, and columns on the rest
            net[0][this.size - 1 - layer][j] = net[1][this.size - 1 - j][this.size - 1 - layer];
            net[1][this.size - 1 - j][this.size - 1 - layer] = net[5][layer][this.size - 1 - j];
            net[5][layer][this.size - 1 - j] = net[3][j][layer];
            net[3][j][layer] = pom;
        }
        if (layer == 0) rotate_adjacent_side(2);
        if (layer == this.size - 1) rotate_side_prim(4);
    }

    private void rotate_back(int layer) {
        for (int j = 0; j < this.size; j++) {
            int pom = net[0][layer][j];
            net[0][layer][j] = net[3][j][this.size - 1 - layer];
            net[3][j][this.size - 1 - layer] = net[5][this.size - 1 - layer][this.size - 1 - j];
            net[5][this.size - 1 - layer][this.size - 1 - j] = net[1][this.size - 1 - j][layer];
            net[1][this.size - 1 - j][layer] = pom;
        }
        if (layer == 0) rotate_adjacent_side(4);
        if (layer == this.size - 1) rotate_side_prim(2);
    }

    private int checkGroup(int side) {
        if (side == 6) { //show
            return 3;
        } else if (side == 0 || side == 5) {
            return 0;
        } else if (side == 1 || side == 3) {
            return 1;
        } else {
            return 2;
        }
    }

    private void enterProtocol(int side) throws InterruptedException {
        int group = checkGroup(side);
        security.acquire();
        if (waitingGroupsNumber > 0 || (workingGroup != -1 && workingGroup != group)) {
            waitingProcesses[group]++;
            if (waitingProcesses[group] == 1) {
                waitingGroupsNumber++;
            }
            security.release();
            try {
                rest[group].acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                security.acquire();
                waitingProcesses[group]--;
                if (waitingProcesses[group] == 0) {
                    waitingGroupsNumber--;
                }
                security.release();
            }
            waitingProcesses[group]--;
            if (waitingProcesses[group] == 0)
                waitingGroupsNumber--;
        }
        workingGroup = group;
        workingProcessesNumber++;
        if (waitingProcesses[group] > 0) {
            rest[group].release();
        } else {
            security.release();
        }
    }

    private void exitProtocol(int side) throws InterruptedException {
        int group = checkGroup(side);
        security.acquireUninterruptibly();
        workingProcessesNumber--;
        if (workingProcessesNumber > 0) {
            security.release();
        } else {
            if (waitingGroupsNumber > 0) {
                for (int i = 0; i < 4; i++) {
                    if (waitingProcesses[(group + i) % 4] > 0) {
                        rest[(group + i) % 4].release();
                        break;
                    }
                }
            } else {
                workingGroup = -1;
                security.release();
            }
        }
    }


    public void rotate(int side, int layer) throws InterruptedException {
        if (layer < 0 || layer > this.size - 1) {
            throw new InterruptedException("Layer number is not correct");
        }
        enterProtocol(side);
        try {
            if (side == 0 || side == 1 || side == 2)
                layers[layer].acquire();
            else
                layers[this.size - 1 - layer].acquire();
            beforeRotation.accept(side, layer);
            switch (side) {
                case 0:
                    rotate_up(layer);
                    break;
                case 1:
                    rotate_left(layer);
                    break;
                case 2:
                    rotate_front(layer);
                    break;
                case 3:
                    rotate_right(layer);
                    break;
                case 4:
                    rotate_back(layer);
                    break;
                case 5:
                    rotate_down(layer);
                    break;
                default:
                    throw new InterruptedException("Side number is not correct");
            }
            afterRotation.accept(side, layer);
            if (side == 0 || side == 1 || side == 2)
                layers[layer].release();
            else
                layers[this.size - 1 - layer].release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            exitProtocol(side);
        }
    }

    public String show() throws InterruptedException {
        StringBuilder result = new StringBuilder();
        enterProtocol(6);
        beforeShowing.run();
        for (int side = 0; side < 6; ++side) {
            for (int row = 0; row < size; ++row) {
                for (int column = 0; column < size; ++column) {
                    result.append(net[side][row][column]);
                }
            }
        }
        afterShowing.run();
        exitProtocol(6);
        return result.toString();
    }

    public int getSize() {
        return this.size;
    }
}
