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

package hibernate.jakarta.persistence.tools.provider;

import hibernate.jakarta.persistence.tools.HibernateNamedObjectParser;
import persistence.tools.NamedObjectParser;
import persistence.tools.spi.NamedObjectParserFactory;

import java.util.List;

/**
 * {@link NamedObjectParserFactory} implementation for the Jakarta Persistence
 * namespace ({@code "jakarta"}).
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader}
 * provider and creates {@link HibernateNamedObjectParser} instances backed by
 * Hibernate ORM&nbsp;6.</p>
 *
 * @since 1.0.0
 * @see HibernateNamedObjectParser
 */
public class HibernateNamedObjectParserFactory extends HibernatePersistenceLinterFactory implements NamedObjectParserFactory {

    /** Creates a new {@code HibernateNamedObjectParserFactory}. */
    public HibernateNamedObjectParserFactory() {}

    /** {@inheritDoc} */
    @Override
    public NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames) {
        return HibernateNamedObjectParser.from(discoveredClassNames);
    }

}
