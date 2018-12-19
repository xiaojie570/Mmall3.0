package com.mmall.test;

import org.junit.Test;

import java.math.BigDecimal;

/**
 * Created by lenovo on 2018/10/10.
 */
public class BigDecimalTest {
    @Test
    public void test1() {
        // 浮点型会出现一些故障
        System.out.println(0.05 + 0.01);
        System.out.println(1.0 - 0.42);
        System.out.println(4.015 * 100);
        System.out.println(123.3 / 100);
    }

    @Test
    public void test2() {
        // 这个也有问题。输出：0.06000000000000000298372437868010820238851010799407958984375
        BigDecimal b1 = new BigDecimal(0.05);
        BigDecimal b2 = new BigDecimal(0.01);
        System.out.println(b1.add(b2));
    }

    @Test
    public void test3() {
        // 在商业中，计算价格的时候一定要用Bigdecimal的string类型构造器
        // 这个是正确的。输出：0.06
        BigDecimal b1 = new BigDecimal("0.05");
        BigDecimal b2 = new BigDecimal("0.01");
        System.out.println(b1.add(b2));
    }

    @Test
    public void test4() {

        int matrix[][] = {{1,0,2},{2,4,3},{6,4,9}};
        boolean flag = false;
        for(int row=0;row<3;row++)
        {
            int a=0;
            for(int i=0;i<3;i++)
            {
                if(matrix[row][a]<matrix[row][i])
                {
                    flag = true;
                    a=i;

                }else {
                    flag = false;

                }
            }
            if(flag) {
                int temp = 1;
                for(int j=0;j<3;j++)
                {
                    if(matrix[row][a]>=matrix[j][a] && row != j)
                    {
                        temp = 0;
                        break;
                    }else if(matrix[row][a]<matrix[j][a]){
                        temp ++;
                    }
                    if(temp == 3) {
                        System.out.println(matrix[row][a]);
                        break;
                    }
                }
            }
        }
    }
}
