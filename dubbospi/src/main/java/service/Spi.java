package service;

import annotation.SPI;

@SPI("defaultSpi")
public interface Spi {

    void say();

}
