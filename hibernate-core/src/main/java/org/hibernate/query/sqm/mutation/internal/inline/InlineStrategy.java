/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import java.util.function.Function;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * Support for multi-table SQM mutation operations which select the matching id values from the database back into
 * the VM and uses that list of values to produce a restriction for the mutations.  The exact form of that
 * restriction is based on the {@link MatchingIdRestrictionProducer} implementation used
 *
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class InlineStrategy implements SqmMultiTableMutationStrategy {
	private final Function<SqmDeleteOrUpdateStatement,MatchingIdRestrictionProducer> matchingIdsStrategy;

	public InlineStrategy(Dialect dialect) {
		this( determinePredicateProducer( dialect ) );
	}

	private static Function<SqmDeleteOrUpdateStatement,MatchingIdRestrictionProducer> determinePredicateProducer(Dialect dialect) {
		throw new NotYetImplementedFor6Exception();
	}

	public InlineStrategy(Function<SqmDeleteOrUpdateStatement,MatchingIdRestrictionProducer> matchingIdsStrategy) {
		this.matchingIdsStrategy = matchingIdsStrategy;
	}

	@Override
	public int executeUpdate(
			SqmUpdateStatement sqmUpdate,
			DomainParameterXref domainParameterXref,
			ExecutionContext context) {
		final InlineUpdateHandler handler = new InlineUpdateHandler(
				matchingIdsStrategy.apply( sqmUpdate ),
				sqmUpdate,
				domainParameterXref,
				context
		);
		return handler.execute( context );
	}

	@Override
	public int executeDelete(
			SqmDeleteStatement sqmDelete,
			DomainParameterXref domainParameterXref,
			ExecutionContext context) {
		final InlineDeleteHandler deleteHandler = new InlineDeleteHandler(
				matchingIdsStrategy.apply( sqmDelete ),
				sqmDelete,
				domainParameterXref,
				context
		);

		return deleteHandler.execute( context );
	}
}
