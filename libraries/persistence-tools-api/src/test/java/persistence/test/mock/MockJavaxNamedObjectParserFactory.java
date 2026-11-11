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

package persistence.test.mock;

import persistence.tools.NamedObjectDescriptor;
import persistence.tools.NamedObjectParser;
import persistence.tools.spi.NamedObjectParserFactory;

import java.util.List;
import java.util.Set;

public class MockJavaxNamedObjectParserFactory extends MockJavaxPersistenceServiceProvider implements NamedObjectParserFactory {

    public MockJavaxNamedObjectParserFactory() {
    }

    @Override
    public NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames) {
        return new NamedObjectParser() {
            @Override
            public NamedObjectDescriptor parseQuery(String name, String queryString) {
                return null;
            }

            @Override
            public NamedObjectDescriptor parseStoredProcedure(String name, String procedureName, Set<String> parameterNames) {
                return null;
            }

            @Override
            public void close() {
            }
        };
    }
}
