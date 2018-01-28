package org.janusgraph.usage.api;

/**
 * Created by IBM on 2018/1/27.
 */
public class Test {
    public static void main(String[] args) {
        System.out.println((1<<4) | 9);

        System.out.println((1L <<63) | 25);

        System.out.println((16 << 5) | 13);


        System.out.println((525 & ((1L << 3) - 1)) == 2);
    }
}
