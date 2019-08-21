package service.impl;

import service.Spi;

public class DubboSpi implements Spi {

    public void say() {
        System.out.println("DubboSpi");
    }
}
