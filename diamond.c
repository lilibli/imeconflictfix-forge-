
#include <stdio.h>

int main() {
    int n = 5;  // 上半部分行数（菱形 "半径"）

    // ===== 上半部分 =====
    for (int i = 1; i <= n; i++) {
        // 打印空格
        for (int j = 1; j <= n - i; j++) {
            printf("  ");
        }
        // 打印星星
        for (int j = 1; j <= 2 * i - 1; j++) {
            printf("* ");
        }
        printf("\n");
    }

    // ===== 下半部分 =====
    for (int i = n - 1; i >= 1; i--) {
        // 打印空格
        for (int j = 1; j <= n - i; j++) {
            printf("  ");
        }
        // 打印星星
        for (int j = 1; j <= 2 * i - 1; j++) {
            printf("* ");
        }
        printf("\n");
    }

    return 0;
}
