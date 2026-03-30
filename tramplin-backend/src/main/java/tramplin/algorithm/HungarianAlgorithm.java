package tramplin.algorithm;

/**
 * Венгерский алгоритм (Kuhn-Munkres) с LAPJV-эвристикой (Jonker-Volgenant).
 * Решает Linear Assignment Problem: N×M → максимальная суммарная совместимость.
 * Сложность: O(n³), где n = max(rows, cols).
 */
public class HungarianAlgorithm {

    private static final int UNASSIGNED = -1;

    private final double[][] costMatrix;
    private final int rows;
    private final int cols;
    private final int n;

    private final double[] u;
    private final double[] v;

    private final int[] xy;
    private final int[] yx;

    private final double[] slack;
    private final int[] slackx;
    private final int[] prev;

    private final int[] visitedLeft;
    private final int[] visitedRight;
    private int generation;

    public HungarianAlgorithm(double[][] costMatrix) {
        this.rows = costMatrix.length;
        this.cols = costMatrix[0].length;
        this.n = Math.max(rows, cols);
        this.costMatrix = costMatrix;

        u = new double[n];
        v = new double[n];
        xy = new int[n];
        yx = new int[n];
        slack = new double[n];
        slackx = new int[n];
        prev = new int[n];
        visitedLeft = new int[n];
        visitedRight = new int[n];
        generation = 0;

        java.util.Arrays.fill(xy, UNASSIGNED);
        java.util.Arrays.fill(yx, UNASSIGNED);
    }

    private double cost(int row, int col, double maxWeight) {
        if (row < rows && col < cols) {
            return maxWeight - costMatrix[row][col];
        }
        return 0.0;
    }

    private void clearForStep() {
        generation++;
        java.util.Arrays.fill(slack, Double.MAX_VALUE);
        java.util.Arrays.fill(prev, UNASSIGNED);
    }

    public AssignmentResult solve() {
        if (n == 0) {
            return new AssignmentResult(new int[0][], 0.0);
        }

        double maxWeight = 0.0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (costMatrix[i][j] > maxWeight) {
                    maxWeight = costMatrix[i][j];
                }
            }
        }

        lapjvInitialization(maxWeight);
        computeInitialMatching(maxWeight);
        for (int i = 0; i < n; i++) {
            if (xy[i] == UNASSIGNED) {
                augment(i, maxWeight);
            }
        }

        return buildResult();
    }

    private void lapjvInitialization(double maxWeight) {
        for (int j = 0; j < n; j++) {
            double minVal = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                double c = cost(i, j, maxWeight);
                if (c < minVal) {
                    minVal = c;
                }
            }
            v[j] = minVal;
        }

        for (int i = 0; i < n; i++) {
            double min1 = Double.MAX_VALUE;
            double min2 = Double.MAX_VALUE;
            int j1 = UNASSIGNED;

            for (int j = 0; j < n; j++) {
                double reducedCost = cost(i, j, maxWeight) - v[j];
                if (reducedCost < min1) {
                    min2 = min1;
                    min1 = reducedCost;
                    j1 = j;
                } else if (reducedCost < min2) {
                    min2 = reducedCost;
                }
            }

            if (n > 1) {
                u[i] = min2;
                if (min1 < min2 && j1 != UNASSIGNED) {
                    double delta = min2 - min1;
                    v[j1] -= delta;
                }
            } else {
                u[i] = min1;
            }
        }
    }

    private void computeInitialMatching(double maxWeight) {
        for (int i = 0; i < n; i++) {
            double uI = u[i];
            for (int j = 0; j < n; j++) {
                if (xy[i] == UNASSIGNED && yx[j] == UNASSIGNED) {
                    if (Math.abs(cost(i, j, maxWeight) - uI - v[j]) < 1e-10) {
                        xy[i] = j;
                        yx[j] = i;
                        break;
                    }
                }
            }
        }
    }

    private void augment(int root, double maxWeight) {
        clearForStep();

        int currentRow = root;
        visitedLeft[currentRow] = generation;

        double uI = u[currentRow];
        for (int j = 0; j < n; j++) {
            slack[j] = cost(currentRow, j, maxWeight) - uI - v[j];
            slackx[j] = currentRow;
        }

        int endCol;

        while (true) {
            double minSlack = Double.MAX_VALUE;
            int j0 = UNASSIGNED;

            for (int j = 0; j < n; j++) {
                if (visitedRight[j] != generation && slack[j] < minSlack) {
                    minSlack = slack[j];
                    j0 = j;
                }
            }

            if (minSlack > 1e-10) {
                for (int i = 0; i < n; i++) {
                    if (visitedLeft[i] == generation) {
                        u[i] += minSlack;
                    }
                }
                for (int j = 0; j < n; j++) {
                    if (visitedRight[j] == generation) {
                        v[j] -= minSlack;
                    } else {
                        slack[j] -= minSlack;
                    }
                }
            }

            visitedRight[j0] = generation;
            prev[j0] = slackx[j0];

            if (yx[j0] == UNASSIGNED) {
                endCol = j0;
                break;
            }

            currentRow = yx[j0];
            visitedLeft[currentRow] = generation;
            uI = u[currentRow];

            for (int j = 0; j < n; j++) {
                if (visitedRight[j] != generation) {
                    double reducedCost = cost(currentRow, j, maxWeight) - uI - v[j];
                    if (reducedCost < slack[j]) {
                        slack[j] = reducedCost;
                        slackx[j] = currentRow;
                    }
                }
            }
        }

        int j = endCol;
        while (j != UNASSIGNED) {
            int i = prev[j];
            int nextJ = xy[i];
            yx[j] = i;
            xy[i] = j;
            j = nextJ;
        }
    }

    private AssignmentResult buildResult() {
        int pairCount = Math.min(rows, cols);
        int[][] pairs = new int[pairCount][2];
        double totalScore = 0.0;
        int idx = 0;

        for (int i = 0; i < rows; i++) {
            int j = xy[i];
            if (j < cols) {
                pairs[idx][0] = i;
                pairs[idx][1] = j;
                totalScore += costMatrix[i][j];
                idx++;
            }
        }

        if (idx < pairCount) {
            pairs = java.util.Arrays.copyOf(pairs, idx);
        }

        return new AssignmentResult(pairs, totalScore);
    }

    public static class AssignmentResult {
        private final int[][] pairs;
        private final double totalScore;

        public AssignmentResult(int[][] pairs, double totalScore) {
            this.pairs = pairs;
            this.totalScore = totalScore;
        }

        public int[][] getPairs() { return pairs; }
        public double getTotalScore() { return totalScore; }
    }
}