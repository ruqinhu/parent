package service.impl;

import service.Spi;

public class DefaultSpi implements Spi {

    public void say() {
        System.out.println("DefaultSpi");
    }
}
