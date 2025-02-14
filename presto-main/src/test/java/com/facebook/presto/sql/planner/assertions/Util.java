/*
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
package com.facebook.presto.sql.planner.assertions;

import com.facebook.presto.Session;
import com.facebook.presto.metadata.Metadata;
import com.facebook.presto.metadata.TableHandle;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.predicate.Domain;
import com.facebook.presto.spi.predicate.TupleDomain;
import com.facebook.presto.sql.planner.OrderingScheme;
import com.facebook.presto.sql.planner.Symbol;
import com.facebook.presto.sql.planner.assertions.PlanMatchPattern.Ordering;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class Util
{
    private Util() {}

    /**
     * @param expectedDomains if empty, the actualConstraint's domains must also be empty.
     */
    static boolean domainsMatch(
            Optional<Map<String, Domain>> expectedDomains,
            TupleDomain<ColumnHandle> actualConstraint,
            TableHandle tableHandle,
            Session session,
            Metadata metadata)
    {
        Optional<Map<ColumnHandle, Domain>> actualDomains = actualConstraint.getDomains();

        if (expectedDomains.isPresent() != actualDomains.isPresent()) {
            return false;
        }

        if (!expectedDomains.isPresent()) {
            return true;
        }

        Map<String, ColumnHandle> columnHandles = metadata.getColumnHandles(session, tableHandle);
        for (Map.Entry<String, Domain> expectedColumnConstraint : expectedDomains.get().entrySet()) {
            if (!columnHandles.containsKey(expectedColumnConstraint.getKey())) {
                return false;
            }
            ColumnHandle columnHandle = columnHandles.get(expectedColumnConstraint.getKey());
            if (!actualDomains.get().containsKey(columnHandle)) {
                return false;
            }
            if (!expectedColumnConstraint.getValue().contains(actualDomains.get().get(columnHandle))) {
                return false;
            }
        }

        return true;
    }

    static boolean orderingSchemeMatches(List<Ordering> expectedOrderBy, OrderingScheme orderingScheme, SymbolAliases symbolAliases)
    {
        if (expectedOrderBy.size() != orderingScheme.getOrderBy().size()) {
            return false;
        }

        for (int i = 0; i < expectedOrderBy.size(); ++i) {
            Ordering ordering = expectedOrderBy.get(i);
            Symbol symbol = Symbol.from(symbolAliases.get(ordering.getField()));
            if (!symbol.equals(new Symbol(orderingScheme.getOrderBy().get(i).getName()))) {
                return false;
            }
            if (!ordering.getSortOrder().equals(orderingScheme.getOrdering(symbol))) {
                return false;
            }
        }

        return true;
    }
}
