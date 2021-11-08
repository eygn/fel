package com.greenpineyu.fel.util;
public class Pair<T1, T2> {
    private T1 object1;
    private T2 object2;

    private Pair(T1 object1, T2 object2) {
        this.object1 = object1;
        this.object2 = object2;
    }

    public static <T1,T2> Pair<T1, T2> of(T1 object1, T2 object2){
        return new Pair<>(object1,object2);
    }

    public T1 getLeft() {
        return object1;
    }

    public void setLeft(T1 object1) {
        this.object1 = object1;
    }
    public T2 getRight() {
        return object2;
    }

    public void setRight(T2 object2) {
        this.object2 = object2;
    }
}
