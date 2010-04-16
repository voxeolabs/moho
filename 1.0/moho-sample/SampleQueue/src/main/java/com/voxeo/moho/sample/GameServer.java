package com.voxeo.moho.sample;

public interface GameServer {

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public void move(Direction direction);
}
