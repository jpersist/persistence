/*
 * Copyright 2024-2026 JPersist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hibernate.jakarta.persistence.tools.internal;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A no-op {@link ConnectionProvider} for offline build-time metadata and query
 * validation where no live database connection is available or required.
 *
 * <p>Configuring this provider suppresses {@code HHH000181} startup logs while
 * preventing accidental JDBC connection attempts during offline linting.</p>
 *
 * @since 2.1.0
 */
public final class NoOpConnectionProvider implements ConnectionProvider {

    /**
     * Public no-arg constructor required for reflective instantiation by Hibernate.
     */
    public NoOpConnectionProvider() {
    }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection() throws SQLException {
        throw new UnsupportedOperationException("Offline linting does not supply JDBC connections");
    }

    /** {@inheritDoc} */
    @Override
    public void closeConnection(Connection conn) throws SQLException {
        // No-op
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return ConnectionProvider.class.equals(unwrapType)
            || NoOpConnectionProvider.class.isAssignableFrom(unwrapType);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        throw new IllegalArgumentException("Cannot unwrap to " + unwrapType.getName());
    }

}
