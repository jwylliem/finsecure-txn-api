package com.finsecure.txn;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
// controlled failure for the demo, delete after, dont anyhow leave it inside
class ControlledFailureTest {
    @Test
    void deliberatelyFailing() {
        assertThat(1 + 1).isEqualTo(3);
    }
}
