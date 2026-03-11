package com.f2cg.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceTest {

    @Test
    void contextLoads() {
        assertThat(new GameService()).isNotNull();
    }
}