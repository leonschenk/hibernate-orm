/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		LazyOneToOneRemoveFlushAccessTest.ContainingEntity.class,
		LazyOneToOneRemoveFlushAccessTest.ContainedEntity.class
} )
@SessionFactory
@BytecodeEnhanced( runNotEnhancedAsWell = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18212" )
public class LazyOneToOneRemoveFlushAccessTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ContainingEntity containing = session.find( ContainingEntity.class, 2 );
			final ContainedEntity containedEntity = containing.getContained();

			session.remove( containedEntity );
			session.flush();

			final ContainingEntity parent = containing.getParent();
			// Lazy loading will load the child based on parent, which triggers the NPE of HHH-18212
			final ContainingEntity child = parent.getChild();
			assertThat( child.getId() ).isEqualTo( 2 );
			assertThat( child ).isSameAs( containing );
			// child.getContained() is not null here as the state for ContainingEntity#2 is not refreshed
			// assertThat( child.getContained() ).isNull();
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ContainingEntity entity1 = new ContainingEntity();
			entity1.setId( 1 );

			final ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );

			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			final ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );

			session.persist( containingEntity1 );
			session.persist( entity1 );
			session.persist( containedEntity );

			containingEntity1.setContained( containedEntity );
			containedEntity.setContaining( containingEntity1 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ContainingEntity entity1 = session.find( ContainingEntity.class, 1 );
			final ContainingEntity entity2 = session.find( ContainingEntity.class, 2 );
			session.remove( entity1 );
			session.remove( entity2 );
		} );
	}

	@Entity( name = "ContainingEntity" )
	static class ContainingEntity {
		@Id
		private Integer id;

		@OneToOne( fetch = FetchType.LAZY )
		private ContainingEntity parent;

		@OneToOne( mappedBy = "parent", fetch = FetchType.LAZY )
		private ContainingEntity child;

		@OneToOne( mappedBy = "containing" )
		private ContainedEntity contained;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getParent() {
			return parent;
		}

		public void setParent(ContainingEntity parent) {
			this.parent = parent;
		}

		public ContainingEntity getChild() {
			return child;
		}

		public void setChild(ContainingEntity child) {
			this.child = child;
		}

		public ContainedEntity getContained() {
			return contained;
		}

		public void setContained(ContainedEntity contained) {
			this.contained = contained;
		}

	}

	@Entity( name = "ContainedEntity" )
	static class ContainedEntity {
		@Id
		private Integer id;

		@OneToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "containing" )
		private ContainingEntity containing;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getContaining() {
			return containing;
		}

		public void setContaining(ContainingEntity containing) {
			this.containing = containing;
		}
	}
}