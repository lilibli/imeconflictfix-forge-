
#include <stdio.h>

int main() {
    // ====== 例1: 方块 ======
    printf("=== 例1: 3x4方块 ===\n");
    printf("外层i: 行号 | 内层j: 列号\n");
    for (int i = 1; i <= 3; i++) {
        printf("i=%d: ", i);
        for (int j = 1; j <= 4; j++) {
            printf("[%d,%d] ", i, j);
        }
        printf("\n");
    }

    // ====== 例2: 三角形 ======
    printf("\n=== 例2: 三角形 ===\n");
    for (int i = 1; i <= 5; i++) {
        for (int j = 1; j <= i; j++) {
            printf("*");
        }
        printf("\n");
    }

    // ====== 例3: 冒泡排序 ======
    printf("\n=== 例3: 冒泡排序 ===\n");
    int arr[] = {5, 2, 8, 1, 9};
    int n = 5;
    
    for (int i = 0; i < n-1; i++) {
        printf("第%d趟: ", i+1);
        for (int j = 0; j < n-1-i; j++) {
            if (arr[j] > arr[j+1]) {
                int tmp = arr[j];
                arr[j] = arr[j+1];
                arr[j+1] = tmp;
            }
        }
        for (int k = 0; k < n; k++) printf("%d ", arr[k]);
        printf("\n");
    }
    
    return 0;
}
