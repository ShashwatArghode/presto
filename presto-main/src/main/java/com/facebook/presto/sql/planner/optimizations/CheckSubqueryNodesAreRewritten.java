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

package com.facebook.presto.sql.planner.optimizations;

import com.facebook.presto.Session;
import com.facebook.presto.execution.warnings.WarningCollector;
import com.facebook.presto.spi.plan.PlanNodeIdAllocator;
import com.facebook.presto.spi.relation.VariableReferenceExpression;
import com.facebook.presto.sql.analyzer.SemanticException;
import com.facebook.presto.sql.planner.SymbolAllocator;
import com.facebook.presto.sql.planner.TypeProvider;
import com.facebook.presto.sql.planner.plan.ApplyNode;
import com.facebook.presto.sql.planner.plan.LateralJoinNode;
import com.facebook.presto.sql.planner.plan.PlanNode;

import java.util.List;

import static com.facebook.presto.sql.planner.optimizations.PlanNodeSearcher.searchFrom;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class CheckSubqueryNodesAreRewritten
        implements PlanOptimizer
{
    @Override
    public PlanNode optimize(PlanNode plan, Session session, TypeProvider types, SymbolAllocator symbolAllocator, PlanNodeIdAllocator idAllocator, WarningCollector warningCollector)
    {
        searchFrom(plan).where(ApplyNode.class::isInstance)
                .findFirst()
                .ifPresent(node -> {
                    ApplyNode applyNode = (ApplyNode) node;
                    throw error(applyNode.getCorrelation(), applyNode.getOriginSubqueryError());
                });

        searchFrom(plan).where(LateralJoinNode.class::isInstance)
                .findFirst()
                .ifPresent(node -> {
                    LateralJoinNode lateralJoinNode = (LateralJoinNode) node;
                    throw error(lateralJoinNode.getCorrelation(), lateralJoinNode.getOriginSubqueryError());
                });

        return plan;
    }

    private SemanticException error(List<VariableReferenceExpression> correlation, String originSubqueryError)
    {
        checkState(!correlation.isEmpty(), "All the non correlated subqueries should be rewritten at this point");
        throw new RuntimeException(format(originSubqueryError, "Given correlated subquery is not supported"));
    }
}
