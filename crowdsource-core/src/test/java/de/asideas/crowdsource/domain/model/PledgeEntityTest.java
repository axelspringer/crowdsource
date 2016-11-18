package de.asideas.crowdsource.domain.model;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PledgeEntityTest {

    @Test
    public void add_bothHavingSameProjectFinancingRoundAndUser() throws Exception {
        UserEntity user = user("some@email.com", "firstname", "lastname");
        FinancingRoundEntity financingRound = new FinancingRoundEntity();
        ProjectEntity project = new ProjectEntity();

        PledgeEntity p0 = pledgeEntity(BigDecimal.valueOf(7), 100L, project, user, financingRound);
        PledgeEntity p1 = pledgeEntity(BigDecimal.valueOf(3), 101L, project, user, financingRound);

        PledgeEntity res = p0.add(p1);

        assertThat(res.getAmount(), is(10));
        assertThat(res.getCreator(), is(user));
        assertThat(res.getCreatedDate(), is(nullValue()));
        assertThat(res.getId(), is(nullValue()));
        assertThat(res.getLastModifiedDate(), is(nullValue()));
        assertThat(res.getFinancingRound(), is(financingRound));
        assertThat(res.getProject(), is(project));
    }

    @Test
    public void add_bothHavingDifferentProjectFinancingRoundAndUser() throws Exception {
        UserEntity user = user("some@email.com", "firstname", "lastname");
        FinancingRoundEntity financingRound0 = new FinancingRoundEntity();
        financingRound0.setId(100L);
        FinancingRoundEntity financingRound1 = new FinancingRoundEntity();
        financingRound1.setId(101L);

        ProjectEntity project0 = new ProjectEntity();
        project0.setId(100L);
        ProjectEntity project1 = new ProjectEntity();
        project0.setId(101L);

        PledgeEntity p0 = pledgeEntity(BigDecimal.valueOf(7), 100L, project0, user, financingRound0);
        PledgeEntity p1 = pledgeEntity(BigDecimal.valueOf(3), 101L, project1, user, financingRound1);

        PledgeEntity res = p0.add(p1);

        assertThat(res.getAmount(), is(10));
        assertThat(res.getCreator(), is(user));
        assertThat(res.getCreatedDate(), is(nullValue()));
        assertThat(res.getLastModifiedDate(), is(nullValue()));
        assertThat(res.getFinancingRound(), is(nullValue()));
        assertThat(res.getProject(), is(nullValue()));

    }

    @Test
    public void add_otherIsNullShouldReturnThisCopy() throws Exception {
        UserEntity user = user("some@email.com", "firstname", "lastname");
        FinancingRoundEntity financingRound0 = new FinancingRoundEntity();
        financingRound0.setId(100L);
        ProjectEntity project0 = new ProjectEntity();
        PledgeEntity p0 = pledgeEntity(BigDecimal.valueOf(7), 100L, project0, user, financingRound0);

        PledgeEntity res = p0.add(null);

        assertThat(res.getAmount(), is(7));
        assertThat(res.getCreator(), is(user));
        assertThat(res.getCreatedDate(), is(nullValue()));
        assertThat(res.getLastModifiedDate(), is(nullValue()));
        assertThat(res.getFinancingRound(), is(financingRound0));
        assertThat(res.getProject(), is(project0));
    }

    @Test
    public void add_thisMembersAreNullOthersNotResultShouldContainOthers() throws Exception {
        UserEntity user = user("some@email.com", "firstname", "lastname");
        FinancingRoundEntity financingRound = new FinancingRoundEntity();
        ProjectEntity project = new ProjectEntity();

        PledgeEntity p0 = pledgeEntity(BigDecimal.valueOf(7), 100L, null, null, null);
        PledgeEntity p1 = pledgeEntity(BigDecimal.valueOf(3), 101L, project, user, financingRound);

        PledgeEntity res = p0.add(p1);

        assertThat(res.getAmount(), is(10));
        assertThat(res.getCreator(), is(user));
        assertThat(res.getCreatedDate(), is(nullValue()));
        assertThat(res.getId(), is(nullValue()));
        assertThat(res.getLastModifiedDate(), is(nullValue()));
        assertThat(res.getFinancingRound(), is(financingRound));
        assertThat(res.getProject(), is(project));
    }

    @Test
    public void add_thisMembersAreNotNullButOthersResultShouldContainNulls() throws Exception {
        UserEntity user = user("some@email.com", "firstname", "lastname");
        FinancingRoundEntity financingRound = new FinancingRoundEntity();
        ProjectEntity project = new ProjectEntity();

        PledgeEntity p0 = pledgeEntity(BigDecimal.valueOf(3), 100L, project, user, financingRound);
        PledgeEntity p1 = pledgeEntity(BigDecimal.valueOf(7), 101L, null, null, null);

        PledgeEntity res = p0.add(p1);

        assertThat(res.getAmount(), is(10));
        assertThat(res.getCreator(), is(nullValue()));
        assertThat(res.getCreatedDate(), is(nullValue()));
        assertThat(res.getId(), is(nullValue()));
        assertThat(res.getLastModifiedDate(), is(nullValue()));
        assertThat(res.getFinancingRound(), is(nullValue()));
        assertThat(res.getProject(), is(nullValue()));
    }

    private PledgeEntity pledgeEntity(BigDecimal amount, Long id, ProjectEntity project, UserEntity user, FinancingRoundEntity financingRound) {
        PledgeEntity res = new PledgeEntity();
        res.setAmount(amount);
        res.setFinancingRound(financingRound);
        res.setId(id);
        res.setProject(project);
        if(user != null){
            res.setCreator(user);
        }
        return res;
    }

    private UserEntity user(String email, String firstname, String lastname) {
        UserEntity userEntity = new UserEntity(email, firstname, lastname);
        userEntity.setId(Integer.valueOf(email.hashCode()).longValue());
        userEntity.setBudget(BigDecimal.ZERO);
        return userEntity;
    }
}