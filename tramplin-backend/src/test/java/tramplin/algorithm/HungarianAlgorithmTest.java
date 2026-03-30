package tramplin.algorithm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для HungarianAlgorithm (LAPJV).
 *
 * Тестируем корректность оптимального назначения:
 * алгоритм должен максимизировать суммарный score.
 * Контекст Spring не требуется — тестируем чистый алгоритм.
 */
@DisplayName("HungarianAlgorithm — оптимальное назначение (LAPJV)")
class HungarianAlgorithmTest {

    @Test
    @DisplayName("solve — квадратная матрица 3×3 → оптимальное назначение с макс. score")
    void solve_whenSquareMatrix_thenOptimalAssignment() {
        // Arrange — матрица совместимости 3 соискателя × 3 вакансии
        // Оптимум: [0]→[0]=0.9, [1]→[1]=0.8, [2]→[2]=0.7 → total = 2.4
        double[][] matrix = {
            {0.9, 0.2, 0.1},
            {0.1, 0.8, 0.3},
            {0.3, 0.1, 0.7}
        };

        HungarianAlgorithm algorithm = new HungarianAlgorithm(matrix);
        HungarianAlgorithm.AssignmentResult result = algorithm.solve();

        assertThat(result).isNotNull();
        assertThat(result.getPairs().length).isEqualTo(3);
        assertThat(result.getTotalScore()).isGreaterThanOrEqualTo(2.4);
    }

    @Test
    @DisplayName("solve — прямоугольная матрица (больше строк) → назначение по меньшему измерению")
    void solve_whenMoreRowsThanColumns_thenAssignsByMinDimension() {
        // Arrange — 3 соискателя, 2 вакансии → назначить можно только 2
        double[][] matrix = {
            {0.9, 0.1},
            {0.2, 0.8},
            {0.5, 0.3}
        };

        HungarianAlgorithm algorithm = new HungarianAlgorithm(matrix);
        HungarianAlgorithm.AssignmentResult result = algorithm.solve();

        // Assert — только 2 пары (по min(3,2))
        assertThat(result).isNotNull();
        assertThat(result.getPairs().length).isEqualTo(2);
        assertThat(result.getTotalScore()).isGreaterThanOrEqualTo(1.7);
    }

    @Test
    @DisplayName("solve — матрица 1×1 → единственная пара")
    void solve_whenSingleElement_thenSinglePair() {
        double[][] matrix = {{0.75}};

        HungarianAlgorithm algorithm = new HungarianAlgorithm(matrix);
        HungarianAlgorithm.AssignmentResult result = algorithm.solve();

        assertThat(result.getPairs().length).isEqualTo(1);
        assertThat(result.getTotalScore()).isEqualTo(0.75);
    }

    @Test
    @DisplayName("solve — нулевая матрица → score = 0.0")
    void solve_whenAllZeros_thenTotalScoreIsZero() {
        // Arrange — нет совместимости ни у кого
        double[][] matrix = {
            {0.0, 0.0},
            {0.0, 0.0}
        };

        HungarianAlgorithm algorithm = new HungarianAlgorithm(matrix);
        HungarianAlgorithm.AssignmentResult result = algorithm.solve();

        assertThat(result.getTotalScore()).isEqualTo(0.0);
    }
}