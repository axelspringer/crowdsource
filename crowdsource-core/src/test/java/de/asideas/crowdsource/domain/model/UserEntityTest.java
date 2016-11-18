package de.asideas.crowdsource.domain.model;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UserEntityTest {

    private UserEntity userEntity;

    @Before
    public void setUp() {
        userEntity = new UserEntity();
        userEntity.setBudget(BigDecimal.valueOf(10));
    }

    @Test
    public void accountPledge() throws Exception {
        userEntity.accountPledge(BigDecimal.valueOf(10));
        assertThat(userEntity.getBudget(), is(BigDecimal.ZERO));

    }

    @Test(expected = IllegalArgumentException.class)
    public void accountPledge_exceed() throws Exception {
        userEntity.accountPledge(BigDecimal.valueOf(11));
        assertThat(userEntity.getBudget(), is(BigDecimal.valueOf(10)));
    }

    @Test
    public void accountPledge_takeBack() throws Exception {
        userEntity.accountPledge(BigDecimal.valueOf(-11));
        assertThat(userEntity.getBudget(), is(BigDecimal.valueOf(21)));
    }
}