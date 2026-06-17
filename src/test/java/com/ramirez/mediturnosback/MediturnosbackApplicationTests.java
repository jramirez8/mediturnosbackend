package com.ramirez.mediturnosback;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediturnosbackApplicationTests {

    @Test
    void existePuntoDeEntradaDeLaAplicacion() {
        assertThat(MediturnosbackApplication.class).isNotNull();
    }
}
