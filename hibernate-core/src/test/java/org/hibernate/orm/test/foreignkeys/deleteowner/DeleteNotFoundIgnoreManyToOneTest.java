/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.foreignkeys.deleteowner;

import jakarta.persistence.*;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test to delete a parent entity but not the child `@ManyToOne` without a foreign key
 * Currencies have coins. The identifier remains known after deleting a Currency (is audited).
 * The coins are to be kept intact after deleting a currency, such that they can be used in native queries on the audit.
 * <dl>
 *     <dt>Currency: <b>cascade = {CascadeType.PERSIST, CascadeType.DETACH}</b></dt>
 *     <dd>Used to make sure the coins are not removed when deleting a Currency. But they will be detached and saved.</dd>
 *     <dt>Coin: <b>@ForeignKey(ConstraintMode.NO_CONSTRAINT)</b></dt>
 *     <dd>Do not create a foreign key relation between the tables, such that currencies may be deleted, while
 *     its identifier is still known for coins</dd>
 * </dl>
 * <p>
 * Test steps
 * <ol>
 *     <li>Given: A currency and accompanying coin</li>
 *     <li>When: Delete the currency</li>
 *     <li>Then: Currency is deleted and Coin remains the same</li>
 * </ol>
 *
 * @author Leon Schenk
 * @see Currency currency entity
 * @see Coin coin entity
 */
@DomainModel(annotatedClasses = {DeleteNotFoundIgnoreManyToOneTest.Coin.class, DeleteNotFoundIgnoreManyToOneTest.Currency.class})
@SessionFactory(useCollectingStatementInspector = true)
public class DeleteNotFoundIgnoreManyToOneTest {

    @Test
    public void testDeleteElement(SessionFactoryScope scope) {
        scope.inTransaction((session) -> {
            final Currency currency = session.byId(Currency.class).load(2);
            session.remove(currency);
        });

        scope.inTransaction(session -> {
            final Tuple singleResult = session.createQuery("select c.id as id, c.name as name, c.currency.id as currency_id from Coin c where c.id = 2", Tuple.class).getSingleResult();
            assertThat(singleResult.get("name"), is("Penny"));
            assertThat(singleResult.get("id"), is(2));
            assertThat(singleResult.get("currency_id"), is(2));

            final Integer currencyExists = session.createQuery("select 1 from Currency c where c.id = 2", Integer.class).getSingleResultOrNull();
            assertThat(currencyExists, is(nullValue()));
        });
    }

    @BeforeEach
    protected void prepareTestData(SessionFactoryScope scope) {
        scope.inTransaction((session) -> {
            Currency euro = new Currency(1, "Euro");
            Coin fiveC = new Coin(1, "Five cents", euro);
            session.persist(euro);
            session.persist(fiveC);

            Currency usd = new Currency(2, "USD");
            Coin penny = new Coin(2, "Penny", usd);
            session.persist(usd);
            session.persist(penny);
        });

        scope.inTransaction((session) -> {
            session.createMutationQuery("delete Currency where id = 1").executeUpdate();
        });
    }

    @AfterEach
    protected void dropTestData(SessionFactoryScope scope) throws Exception {
        scope.inTransaction((session) -> {
            session.createMutationQuery("delete Coin").executeUpdate();
            session.createMutationQuery("delete Currency").executeUpdate();
        });
    }

    @Entity(name = "Coin")
    public static class Coin {

        @Id
        private Integer id;

        private String name;

        @ManyToOne
        @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private Currency currency;

        @SuppressWarnings("unused")
        public Coin() {
        }

        public Coin(Integer id, String name, Currency currency) {
            this.id = id;
            this.name = name;
            this.currency = currency;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Currency getCurrency() {
            return currency;
        }

        public void setCurrency(Currency currency) {
            this.currency = currency;
        }
    }

    @Entity(name = "Currency")
    public static class Currency implements Serializable {

        @Id
        private Integer id;

        private String name;

        @OneToMany(fetch = FetchType.EAGER, mappedBy = "currency", cascade = {CascadeType.PERSIST, CascadeType.DETACH})
        private List<Coin> coins;

        public Currency() {
        }

        public Currency(Integer id, String name) {
            this.id = id;
            this.name = name;
            this.coins = new ArrayList<>();
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Coin> getCoins() {
            return this.coins;
        }

        @Override
        public String toString() {
            return "Currency{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }

    }

}
