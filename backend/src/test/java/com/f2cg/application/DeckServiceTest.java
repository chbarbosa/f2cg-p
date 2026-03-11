package com.f2cg.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeckServiceTest {

    @Test
    void contextLoads() {
        assertThat(new DeckService()).isNotNull();
    }
}