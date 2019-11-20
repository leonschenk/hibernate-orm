/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.mutation.multitable;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.orm.test.metamodel.mapping.SecondaryTableTests;
import org.hibernate.orm.test.metamodel.mapping.inheritance.joined.JoinedInheritanceTest;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.MatchingIdSelectionHelper;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Tests for selecting matching ids related to SQM update/select statements.
 *
 * Matching-id-selection is used in CTE- and inline-based strategies.
 *
 * A "functional correctness" test for {@link MatchingIdSelectionHelper#selectMatchingIds}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT,
		annotatedClasses = {
				SecondaryTableTests.SimpleEntityWithSecondaryTables.class,
				JoinedInheritanceTest.Customer.class,
				JoinedInheritanceTest.DomesticCustomer.class,
				JoinedInheritanceTest.ForeignCustomer.class
		}
)
@ServiceRegistry
@SessionFactory( exportSchema = true )
public class IdSelectionTests {

	@Test
	public void testSecondaryTableRestrictedOnRootTable(SessionFactoryScope scope) {
		final SqmDeleteStatement sqm = (SqmDeleteStatement) scope.getSessionFactory()
				.getQueryEngine()
				.getHqlTranslator()
				.translate( "delete SimpleEntityWithSecondaryTables where name = :n" );

		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqm );
		final ParameterMetadataImpl parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		final QueryParameterBindingsImpl domainParamBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				scope.getSessionFactory()
		);
		domainParamBindings.getBinding( "n" ).setBindValue( "abc" );

		scope.inTransaction(
				session -> {
					final ExecutionContext executionContext = new TestExecutionContext( session, domainParamBindings );

					MatchingIdSelectionHelper.selectMatchingIds( sqm, domainParameterXref, executionContext );
				}
		);
	}

	@Test
	public void testSecondaryTableRestrictedOnNonRootTable(SessionFactoryScope scope) {
		final SqmDeleteStatement sqm = (SqmDeleteStatement) scope.getSessionFactory()
				.getQueryEngine()
				.getHqlTranslator()
				.translate( "delete SimpleEntityWithSecondaryTables where data = :d" );

		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqm );
		final ParameterMetadataImpl parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		final QueryParameterBindingsImpl domainParamBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				scope.getSessionFactory()
		);
		domainParamBindings.getBinding( "d" ).setBindValue( "123" );

		scope.inTransaction(
				session -> {
					final ExecutionContext executionContext = new TestExecutionContext( session, domainParamBindings );

					MatchingIdSelectionHelper.selectMatchingIds( sqm, domainParameterXref, executionContext );
				}
		);
	}

	@Test
	public void testJoinedSubclassRestrictedOnRootTable(SessionFactoryScope scope) {
		final SqmDeleteStatement sqm = (SqmDeleteStatement) scope.getSessionFactory()
				.getQueryEngine()
				.getHqlTranslator()
				.translate( "delete Customer where name = :n" );

		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqm );
		final ParameterMetadataImpl parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		final QueryParameterBindingsImpl domainParamBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				scope.getSessionFactory()
		);
		domainParamBindings.getBinding( "n" ).setBindValue( "Acme" );

		scope.inTransaction(
				session -> {
					final ExecutionContext executionContext = new TestExecutionContext( session, domainParamBindings );

					MatchingIdSelectionHelper.selectMatchingIds( sqm, domainParameterXref, executionContext );
				}
		);
	}

	@Test
	public void testJoinedSubclassRestrictedOnNonPrimaryRootTable(SessionFactoryScope scope) {
		final SqmDeleteStatement sqm = (SqmDeleteStatement) scope.getSessionFactory()
				.getQueryEngine()
				.getHqlTranslator()
				.translate( "delete ForeignCustomer where name = :n" );

		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqm );
		final ParameterMetadataImpl parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		final QueryParameterBindingsImpl domainParamBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				scope.getSessionFactory()
		);
		domainParamBindings.getBinding( "n" ).setBindValue( "Acme" );

		scope.inTransaction(
				session -> {
					final ExecutionContext executionContext = new TestExecutionContext( session, domainParamBindings );

					MatchingIdSelectionHelper.selectMatchingIds( sqm, domainParameterXref, executionContext );
				}
		);
	}

	@Test
	public void testJoinedSubclassRestrictedOnPrimaryNonRootTable(SessionFactoryScope scope) {
		final SqmDeleteStatement sqm = (SqmDeleteStatement) scope.getSessionFactory()
				.getQueryEngine()
				.getHqlTranslator()
				.translate( "delete ForeignCustomer where vat = :v" );

		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqm );
		final ParameterMetadataImpl parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		final QueryParameterBindingsImpl domainParamBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				scope.getSessionFactory()
		);
		domainParamBindings.getBinding( "v" ).setBindValue( "123" );

		scope.inTransaction(
				session -> {
					final ExecutionContext executionContext = new TestExecutionContext( session, domainParamBindings );

					MatchingIdSelectionHelper.selectMatchingIds( sqm, domainParameterXref, executionContext );
				}
		);
	}

	private static class TestExecutionContext implements ExecutionContext {
		private final SessionImplementor session;
		private final QueryParameterBindingsImpl domainParamBindings;

		public TestExecutionContext(SessionImplementor session, QueryParameterBindingsImpl domainParamBindings) {
			this.session = session;
			this.domainParamBindings = domainParamBindings;
		}

		@Override
		public SharedSessionContractImplementor getSession() {
			return session;
		}

		@Override
		public QueryOptions getQueryOptions() {
			return QueryOptions.NONE;
		}

		@Override
		public QueryParameterBindings getQueryParameterBindings() {
			return domainParamBindings;
		}

		@Override
		public Callback getCallback() {
			return afterLoadAction -> {
			};
		}
	}
}
